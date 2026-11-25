#!/usr/bin/env bash
set -euo pipefail

CONFIG_FILE="configs/application.properties"

if [[ ! -f "$CONFIG_FILE" ]]; then
    echo "Error: $CONFIG_FILE not found!" >&2
    exit 1
fi

# 1. Generate a cryptographically secure 256-bit (32-byte) Base64 key
NEW_SECRET=$(openssl rand -base64 32)

# 2. Encrypt the key via Config Server's /encrypt endpoint
CIPHER=$(curl -s -f -u config:config -H "Content-Type: text/plain" http://localhost:8888/encrypt --data-raw "$NEW_SECRET")

if [[ -z "$CIPHER" ]]; then
    echo "Error: Failed to encrypt secret. Is Config Server running on http://localhost:8888?" >&2
    exit 1
fi

# 3. Update token.secret in configs/application.properties (cross-platform: works on both Linux & macOS)
TMP_FILE=$(mktemp)
awk -v new_line="token.secret={cipher}${CIPHER}" '
    /^[[:space:]]*token\.secret[[:space:]]*=/ { print new_line; updated=1; next }
    { print }
    END { if (!updated) print new_line }
' "$CONFIG_FILE" > "$TMP_FILE" && mv "$TMP_FILE" "$CONFIG_FILE"

echo "Successfully updated token.secret in $CONFIG_FILE."
echo

# 4. Trigger /actuator/refresh on all running services
for service in "${SERVICES[@]}"; do
    echo "Refreshing ${service}/actuator/refresh ..."
    if response=$(curl -s -f -X POST "${service}/actuator/refresh"); then
        echo "  -> Refreshed keys: $response"
    else
        echo "  -> [WARN] Could not refresh $service (service unreachable or refresh endpoint disabled)" >&2
    fi
done