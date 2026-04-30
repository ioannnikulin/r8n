#!/bin/sh
set -eu

# Wait for database to be ready before starting the JVM.
if [ -f /bin/bash ]; then
  until /bin/bash -c "echo > /dev/tcp/database/${DATABASE_CONTAINER_PORT:-5432}" 2>/dev/null; do
    echo "Waiting for database (via bash /dev/tcp)..."
    sleep 2
  done
else
  echo "Bash not found, skipping wait and hoping for the best..."
fi

exec "$@"
