#!/usr/bin/env bash
set -euo pipefail

# This script generates a 2048-bit RSA key pair for JWT signing and verification.
# It updates the specified environment file by removing existing JWT_PRIVATE_KEY and JWT_PUBLIC_KEY
# and appending the new ones as quoted strings.

# The first argument should be the environment name (e.g., "local", "docker").
env_name="${1:-local}"

# Determine paths relative to the project root (assuming script is in deployment/scripts/)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
ENV_DIR="$ROOT_DIR/deployment/config"
ENV_FILE="$ENV_DIR/${env_name}.env"

if [ ! -d "$ENV_DIR" ]; then
  mkdir -p "$ENV_DIR"
fi

if [ ! -f "$ENV_FILE" ]; then
  echo "Creating $ENV_FILE"
  touch "$ENV_FILE"
fi

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT INT TERM

echo "Generating RSA key pair..."
# Generate private key (RSA 2048)
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$tmp_dir/private.pem" 2>/dev/null
# Convert to PKCS#8 format
openssl pkcs8 -topk8 -nocrypt -in "$tmp_dir/private.pem" -out "$tmp_dir/private_pkcs8.pem" 2>/dev/null
# Extract public key
openssl rsa -pubout -in "$tmp_dir/private.pem" -out "$tmp_dir/public.pem" 2>/dev/null

# Filter out existing JWT keys and lines that look like parts of old keys (starting with MII or ending with ===)
# This is a bit aggressive but helps clean up corrupted files.
# More safely, we can just remove everything after the first JWT_PRIVATE_KEY or similar, 
# but let's try to just match lines that are definitely NOT keys.
# Actually, the best way to clean up the corrupted file is to just match the known keys.
grep -v -E '^(JWT_PRIVATE_KEY|JWT_PUBLIC_KEY)=' "$ENV_FILE" | grep -v "\-----BEGIN" | grep -v "\-----END" | grep -vE '^[A-Za-z0-9+/]{64,}$' | grep -vE '^[A-Za-z0-9+/]+=*$' > "$tmp_dir/env.filtered" 2>/dev/null || true

# Format the keys into the environment file as single-line strings (with \n escaped if necessary)
# But Spring Boot handles multi-line values in .env files if they are quoted.
# The issue with $(cat ...) is that it strips trailing newlines, which is fine for PEM.

PRIVATE_KEY_CONTENT=$(cat "$tmp_dir/private_pkcs8.pem")
PUBLIC_KEY_CONTENT=$(cat "$tmp_dir/public.pem")

{
  cat "$tmp_dir/env.filtered"
  echo "JWT_PRIVATE_KEY=\"$PRIVATE_KEY_CONTENT\""
  echo "JWT_PUBLIC_KEY=\"$PUBLIC_KEY_CONTENT\""
} > "$ENV_FILE"

echo "Updated $ENV_FILE with new JWT key pair"
