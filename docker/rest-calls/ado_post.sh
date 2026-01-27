#!/bin/bash

# Script: cloud_post.sh
# Purpose: Test AWS CloudWatch webhook endpoint
# Usage: ./cloud_post.sh [log-message]
# Example: ./cloud_post.sh "Test error message"

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Construct URL
URL="http://localhost:8081/api/webhooks/ado/project"

# Create JSON payload for ADO Logs
# This simulates a ADO Logs subscription filter message
PAYLOAD='{"subscriptionId":"00000000-0000-0000-0000-000000000000","notificationId":1,"id":"03c164c2-8912-4d5e-8009-3707d5f83734","eventType":"git.push","publisherId":"tfs","message":{"text":"David pushed updates to qodo-code:trunk.","html":"David pushed updates to qodo-code:trunk.","markdown":"David pushed updates to `qodo-code`:`trunk`."},"detailedMessage":{"text":"David pushed a commit to qodo-code:trunk.\n - Fixed bug in web.config file 33b55f7c","html":"David pushed a commit to <a href=\"https://davidp0219@dev.azure.com/davidp0219/qodo-code/_git/qodo-code\">qodo-code</a>:<a href=\"https://davidp0219@dev.azure.com/davidp0219/qodo-code/_git/qodo-code#version=GBtrunk\">trunk</a>.\n<ul>\n<li>Fixed bug in web.config file <a href=\"https://davidp0219@dev.azure.com/davidp0219/qodo-code/_git/qodo-code/commit/33b55f7cb7e7e245323987634f960cf4a6e6bc74\">33b55f7c</a>\n</ul>","markdown":"David pushed a commit to [qodo-code](https://davidp0219@dev.azure.com/davidp0219/qodo-code/_git/qodo-code):[trunk](https://davidp0219@dev.azure.com/davidp0219/qodo-code/_git/qodo-code#version=GBtrunk).\n* Fixed bug in web.config file [33b55f7c](https://davidp0219@dev.azure.com/davidp0219/qodo-code/_git/qodo-code/commit/33b55f7cb7e7e245323987634f960cf4a6e6bc74)"},"resource":{"commits":[{"commitId":"33b55f7cb7e7e245323987634f960cf4a6e6bc74","author":{"name":"David","email":"david@example.com","date":"2015-02-25T19:01:00Z"},"committer":{"name":"David","email":"david@example.com","date":"2015-02-25T19:01:00Z"},"comment":"Fixed bug in web.config file","url":"https://davidp0219@dev.azure.com/davidp0219/qodo-code/_git/qodo-code/commit/33b55f7cb7e7e245323987634f960cf4a6e6bc74"}],"refUpdates":[{"name":"refs/heads/trunk","oldObjectId":"aad331d8d3b131fa9ae03cf5e53965b51942618a","newObjectId":"33b55f7cb7e7e245323987634f960cf4a6e6bc74"}],"repository":{"id":"278d5cd2-584d-4b63-824a-2ba458937249","name":"qodo-code","url":"https://davidp0219@dev.azure.com/davidp0219/qodo-code/_apis/git/repositories/278d5cd2-584d-4b63-824a-2ba458937249","project":{"id":"6ce954b1-ce1f-45d1-b94d-e6bf2464ba2c","name":"qodo-code","url":"https://davidp0219@dev.azure.com/davidp0219/qodo-code/_apis/projects/6ce954b1-ce1f-45d1-b94d-e6bf2464ba2c","state":"wellFormed","visibility":"unchanged","lastUpdateTime":"0001-01-01T00:00:00"},"defaultBranch":"refs/heads/trunk","remoteUrl":"https://davidp0219@dev.azure.com/davidp0219/qodo-code/_git/qodo-code"},"pushedBy":{"displayName":"David","id":"00067FFED5C7AF52@Live.com","uniqueName":"david@example.com"},"pushId":14,"date":"2014-05-02T19:17:13.3309587Z","url":"https://davidp0219@dev.azure.com/davidp0219/qodo-code/_apis/git/repositories/278d5cd2-584d-4b63-824a-2ba458937249/pushes/14"},"resourceVersion":"1.0","resourceContainers":{"collection":{"id":"c12d0eb8-e382-443b-9f9c-c52cba5014c2"},"account":{"id":"f844ec47-a9db-4511-8281-8b63f4eaf94e"},"project":{"id":"be9b3917-87e6-42a4-a549-2bc06a7a878f"}},"createdDate":"2026-01-23T19:33:29.5724811Z"}'


# Display request information
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}Testing AWS CloudWatch Webhook${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}Log Message:${NC} $LOG_MESSAGE"
echo -e "${YELLOW}URL:${NC} $URL"
echo -e "${YELLOW}Method:${NC} POST"
echo -e "${YELLOW}Content-Type:${NC} application/json"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Execute curl and capture response
echo -e "${YELLOW}Sending request...${NC}"
HTTP_CODE=$(curl -s -o /tmp/webhook_response.txt -w "%{http_code}" \
     -X POST \
     -H "Content-Type: application/json" \
     -H "X-ADO-Project-Signature:IzUpKCpmaGdXUkVFQCN3aG93b3VkQURPSUxPVkVUSElTSEVBREVSCg==" \
     -d "$PAYLOAD" \
     "$URL")

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}Response${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# Check response and display results
if [ "$HTTP_CODE" -ge 200 ] && [ "$HTTP_CODE" -lt 300 ]; then
    echo -e "${GREEN}✓ Success (HTTP $HTTP_CODE)${NC}"
    echo ""
    echo -e "${YELLOW}Response Body:${NC}"
    cat /tmp/webhook_response.txt
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    rm -f /tmp/webhook_response.txt
    exit 0
else
    echo -e "${RED}✗ Failed (HTTP $HTTP_CODE)${NC}"
    echo ""
    echo -e "${YELLOW}Response Body:${NC}"
    cat /tmp/webhook_response.txt
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    rm -f /tmp/webhook_response.txt
    exit 1
fi
