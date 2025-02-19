#!/bin/bash

# Make a GET request to localhost:9000/ping and store the HTTP status code
STATUS_CODE=$(curl -o /dev/null -s -w "%{http_code}\n" http://product-inventory:9000/admin/ping)

# Check if the status code is 200
if [ "$STATUS_CODE" -eq 200 ]; then
  echo "Healthcheck passed: HTTP 200"
  exit 0
else
  echo "Healthcheck failed: HTTP $STATUS_CODE"
  exit 1
fi