"""
Lambda Pre Token Generation Function
Injects org_id, email, and cognito:groups into Cognito JWT claims from database lookup
"""

import json
import os
import psycopg2
from psycopg2.extras import RealDictCursor

# Database connection parameters from environment
DB_HOST = os.environ['DB_HOST']
DB_NAME = os.environ['DB_NAME']
DB_USER = os.environ['DB_USER']
DB_PASSWORD = os.environ['DB_PASSWORD']

# Connection pool (reused across invocations)
conn = None

def get_connection():
    """Get or create database connection"""
    global conn
    if conn is None or conn.closed:
        conn = psycopg2.connect(
            host=DB_HOST,
            database=DB_NAME,
            user=DB_USER,
            password=DB_PASSWORD,
            connect_timeout=5
        )
    return conn

def query_user_membership(user_id):
    """
    Query user's org_id and role from database.
    Uses user_id (Cognito sub) directly as org_memberships.user_id.
    
    Returns:
        dict with 'org_id' and 'role', or None if not found
    """
    try:
        connection = get_connection()
        with connection.cursor(cursor_factory=RealDictCursor) as cursor:
            # Find first ACTIVE organization membership for user
            # Note: user_id in org_memberships IS the Cognito sub (UUID)
            cursor.execute("""
                SELECT om.org_id::text as org_id, om.role
                FROM org_memberships om
                WHERE om.user_id = %s::uuid
                  AND om.status = 'ACTIVE'
                ORDER BY om.created_at ASC
                LIMIT 1
            """, (user_id,))
            
            result = cursor.fetchone()
            if result:
                return {
                    'org_id': result['org_id'],
                    'role': result['role']
                }
            return None
            
    except Exception as e:
        print(json.dumps({
            "level": "ERROR",
            "message": "Database query error",
            "error": str(e),
            "user_id": user_id
        }))
        return None

def handler(event, context):
    """
    Lambda handler for Cognito Pre Token Generation trigger
    
    Injects the following claims:
    - email: user's email address (always, for backend compatibility)
    - org_id: organization ID (for backend TenantContextFilter)
    - custom:org_id: organization ID (for Cognito custom attribute compatibility)
    - cognito:groups: user's role as group (ADMIN/MEMBER) for RBAC
    
    Args:
        event: Cognito event object containing user attributes
        context: Lambda context object
        
    Returns:
        Modified event with claims added
    """
    user_id = None
    email = None
    
    try:
        # Extract user info from Cognito event
        user_attributes = event['request']['userAttributes']
        user_id = user_attributes['sub']
        email = user_attributes.get('email')
        
        print(json.dumps({
            "level": "INFO",
            "message": "Processing token generation",
            "user_id": user_id,
            "email": email
        }))
        
        # Initialize response structure
        if 'response' not in event:
            event['response'] = {}
        
        claims_to_add = {}
        groups_to_add = []
        
        # ALWAYS inject email if present (backend requires it)
        if email:
            claims_to_add['email'] = email
        
        # Query org and role from database
        membership = query_user_membership(user_id)
        
        if membership:
            org_id = membership['org_id']
            role = membership['role']
            
            # Inject org_id with BOTH keys for compatibility
            # - 'org_id': for backend TenantContextFilter (default config)
            # - 'custom:org_id': for Cognito custom attribute pattern
            claims_to_add['org_id'] = org_id
            claims_to_add['custom:org_id'] = org_id
            
            # Inject role into cognito:groups for RBAC
            # SecurityConfig.cognitoGroupsConverter() expects ADMIN or MEMBER
            if role in ('ADMIN', 'MEMBER'):
                groups_to_add.append(role)
            
            print(json.dumps({
                "level": "INFO",
                "message": "Membership found",
                "user_id": user_id,
                "org_id": org_id,
                "role": role
            }))
        else:
            print(json.dumps({
                "level": "WARN",
                "message": "No active membership found",
                "user_id": user_id,
                "hint": "User may be new or needs to create/join an organization"
            }))
        
        # Build response
        event['response']['claimsOverrideDetails'] = {}
        
        if claims_to_add:
            event['response']['claimsOverrideDetails']['claimsToAddOrOverride'] = claims_to_add
        
        if groups_to_add:
            event['response']['claimsOverrideDetails']['groupOverrideDetails'] = {
                'groupsToOverride': groups_to_add
            }
        
        print(json.dumps({
            "level": "INFO",
            "message": "Token generation complete",
            "user_id": user_id,
            "claims_added": list(claims_to_add.keys()),
            "groups_added": groups_to_add
        }))
        
        return event
        
    except Exception as e:
        print(json.dumps({
            "level": "ERROR",
            "message": "Lambda handler error",
            "error": str(e),
            "user_id": user_id
        }))
        # Return event unmodified on error - allows authentication to proceed
        return event
