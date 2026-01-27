"""
Lambda Pre Token Generation Function
Injects org_id into Cognito JWT claims from database lookup
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

def query_org_id(cognito_sub):
    """Query user's default org_id from database"""
    try:
        connection = get_connection()
        with connection.cursor(cursor_factory=RealDictCursor) as cursor:
            # Find first ACTIVE organization membership for user
            cursor.execute("""
                SELECT om.org_id
                FROM org_memberships om
                JOIN users u ON om.user_id = u.id
                WHERE u.cognito_sub = %s
                  AND om.status = 'ACTIVE'
                ORDER BY om.created_at ASC
                LIMIT 1
            """, (cognito_sub,))
            
            result = cursor.fetchone()
            return result['org_id'] if result else None
            
    except Exception as e:
        print(f"Database query error: {str(e)}")
        # Return None on error - user will still authenticate but without org_id
        return None

def handler(event, context):
    """
    Lambda handler for Cognito Pre Token Generation trigger
    
    Args:
        event: Cognito event object containing user attributes
        context: Lambda context object
        
    Returns:
        Modified event with org_id claim added
    """
    try:
        # Extract cognito_sub from user attributes
        cognito_sub = event['request']['userAttributes']['sub']
        print(f"Processing token generation for user: {cognito_sub}")
        
        # Query org_id from database
        org_id = query_org_id(cognito_sub)
        
        if org_id:
            # Inject org_id into JWT claims
            if 'response' not in event:
                event['response'] = {}
            
            event['response']['claimsOverrideDetails'] = {
                'claimsToAddOrOverride': {
                    'custom:org_id': org_id
                }
            }
            print(f"Injected custom:org_id: {org_id}")
        else:
            print(f"Warning: No active org found for user {cognito_sub}")
        
        return event
        
    except Exception as e:
        print(f"Error in Lambda handler: {str(e)}")
        # Return event unmodified on error - allows authentication to proceed
        return event
