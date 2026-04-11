#!/bin/sh
set -e

alembic upgrade head

echo "Starting: $@"
exec "$@"