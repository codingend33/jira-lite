#!/bin/bash
# Build Lambda deployment package

set -e

cd "$(dirname "$0")"

echo "📦 Building Lambda deployment package..."

# Clean previous build
rm -rf package lambda.zip

# Create package directory
mkdir -p package

# Install dependencies
echo "Installing Python dependencies..."
pip install -r requirements.txt -t package/

# Copy function code
cp index.py package/

# Create ZIP file
echo "Creating ZIP archive..."
cd package
zip -r ../lambda.zip . -q
cd ..

# Clean up
rm -rf package

echo "✅ Lambda package created: lambda.zip ($(du -h lambda.zip | cut -f1))"
