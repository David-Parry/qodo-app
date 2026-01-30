#!/bin/bash

# Ensure PATH includes common binary locations
export PATH="/usr/local/bin:/usr/bin:/bin:$PATH"

echo "Starting Spring Boot application..."
echo "========================================="
echo "PATH: $PATH"
echo "Snyk location: $(which snyk || echo 'not found')"
 echo "Azure CLI location: $(which az || echo 'not found')"
echo ""

# Azure DevOps Login using Service Principal
# Required environment variables:
#   AZURE_TENANT_ID     - Azure AD tenant ID
#   AZURE_CLIENT_ID     - Service Principal (App Registration) client ID
#   AZURE_CLIENT_SECRET - Service Principal client secret
#   AZURE_DEVOPS_ORG    - (Optional) Azure DevOps organization URL for default config
#
# Alternative: Use AZURE_DEVOPS_EXT_PAT for Personal Access Token authentication
#   AZURE_DEVOPS_EXT_PAT - Azure DevOps Personal Access Token

if [ -n "$AZURE_DEVOPS_EXT_PAT" ]; then
    echo "Azure DevOps: Logging in with Personal Access Token..."
    echo "$AZURE_DEVOPS_EXT_PAT" | az devops login
    echo "Azure DevOps: PAT login configured"
    
    # Set default organization if provided
    if [ -n "$AZURE_DEVOPS_ORG" ]; then
        az devops configure --defaults organization="$AZURE_DEVOPS_ORG"
        echo "Azure DevOps: Default organization set to $AZURE_DEVOPS_ORG"
    fi
elif [ -n "$AZURE_TENANT_ID" ] && [ -n "$AZURE_CLIENT_ID" ] && [ -n "$AZURE_CLIENT_SECRET" ]; then
    echo "Azure: Logging in with Service Principal..."
    az login --service-principal \
        --tenant "$AZURE_TENANT_ID" \
        --username "$AZURE_CLIENT_ID" \
        --password "$AZURE_CLIENT_SECRET" \
        --allow-no-subscriptions
    
    if [ $? -eq 0 ]; then
        echo "Azure: Service Principal login successful"
        
        # Set default organization if provided
        if [ -n "$AZURE_DEVOPS_ORG" ]; then
            az devops configure --defaults organization="$AZURE_DEVOPS_ORG"
            echo "Azure DevOps: Default organization set to $AZURE_DEVOPS_ORG"
        fi
    else
        echo "Azure: Service Principal login failed"
    fi
else
    echo "Azure: No credentials provided. Skipping Azure login."
    echo "  To enable Azure login, set one of the following:"
    echo "    - AZURE_DEVOPS_EXT_PAT (Personal Access Token)"
    echo "    - AZURE_TENANT_ID, AZURE_CLIENT_ID, AZURE_CLIENT_SECRET (Service Principal)"
fi

echo ""

# Start Spring Boot application
exec java $JAVA_OPTS -jar app.jar