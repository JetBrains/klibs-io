#!/bin/bash
# ------------------------------------------------------------------------------
# This script automates the process of syncing a remote PostgreSQL database with
# a local PostgreSQL instance running in a Docker container. The main steps 
# include:
# 
# 1. Switching Kubernetes namespace context. 
# 2. Retrieving database connection details from Kubernetes secrets.
# 3. Creating a database dump of the remote database using the retrieved details.
# 4. Clearing the data in the local PostgreSQL database.
# 5. Applying the dump to the local database for synchronization.
#
# It ensures clean-up of temporary files and uses error handling to alert on 
# any failures, making it robust and easy to use.
#
# PARAMETERS:
# -K <context>: Kubernetes namespace context (e.g., klibs-prod or klibs-test).
# -C <postgres_container>: Name or ID of the running PostgreSQL container.
# -L <local_user>: Username of the local database.
# -D <local_db>: Name of the local database.
#
# DEPENDENCIES:
# - Docker must be installed and running for local database management.
# - kubectl must be configured correctly to access the Kubernetes API.
# - The script assumes that database secrets are stored in a Kubernetes secret
#   named "db-secrets".
#
# EXAMPLES:
# ./copy_prod_db_to_local.sh -K klibs-prod -C klibs-io -L postgres -D postgres
#   - Syncs the "klibs-prod" remote database with "postgres" in a Docker container
#     named "klibs-io" using the credentials provided.
#
# CLEANUP:
# Upon script exit (success or failure), any generated temporary files are 
# removed automatically.
#
# ------------------------------------------------------------------------------

# Capture the start time of the script
SCRIPT_START_TIME=$(date +%s)

# Define temporary directory for dump files
TEMP_DIR="$(pwd)/tmp_db_sync"
DUMP_FILE_NAME="db.dump"
DUMP_FILE_PATH="$TEMP_DIR/$DUMP_FILE_NAME"

# Function to create a temporary directory
create_temp_dir() {
  if [[ ! -d "$TEMP_DIR" ]]; then
    echo "### Creating temporary directory at $TEMP_DIR..."
    mkdir -p "$TEMP_DIR"
    if [[ $? -ne 0 ]]; then
      echo "Error: Failed to create temporary directory at $TEMP_DIR."
      exit 1
    fi
    DIR_CREATED_BY_SCRIPT=true
  else
    echo "### Using existing temporary directory: $TEMP_DIR"
    DIR_CREATED_BY_SCRIPT=false
  fi
}

# Cleanup function to remove the dump file and temporary directory
cleanup() {
  echo "### Cleaning up temporary files..."
  if [[ -f "$DUMP_FILE_PATH" ]]; then
    rm -f "$DUMP_FILE_PATH"
    echo "### Temporary dump file removed."
  fi
  if [[ "$DIR_CREATED_BY_SCRIPT" == true && -d "$TEMP_DIR" ]]; then
    rmdir "$TEMP_DIR"
    echo "### Temporary directory removed."
  fi

  # Finished
  echo "### All steps completed successfully."

  # At the end of the script, calculate and display the execution time
  SCRIPT_END_TIME=$(date +%s)
  EXECUTION_TIME=$((SCRIPT_END_TIME - SCRIPT_START_TIME))
  echo "### Script execution completed in $EXECUTION_TIME seconds."
}

# Register the cleanup function to run on script exit
trap cleanup EXIT

usage() {
  echo "Usage: $0 -K <context> -L <local_user> -D <local_db> -C <postgres_container>"
  echo "  -K   Kubernetes context (e.g., klibs-prod or klibs-test)"
  echo "  -C   Local PostgreSQL container name or ID"
  echo "  -L   Local database user"
  echo "  -D   Local database name"
  exit 1
}

# Parse command-line arguments
while getopts "K:L:D:C:" opt; do
  case "$opt" in
    K) CONTEXT="$OPTARG" ;;
    L) LOCAL_USER="$OPTARG" ;;
    D) LOCAL_DB="$OPTARG" ;;
    C) POSTGRES_CONTAINER="$OPTARG" ;;
    *) usage ;;
  esac
done

# Ensure required parameters are provided
if [[ -z "$CONTEXT" || -z "$LOCAL_USER" || -z "$LOCAL_DB" || -z "$POSTGRES_CONTAINER" ]]; then
  usage
fi

# Create temporary directory for the dump file
create_temp_dir

# Step 1: Switch the Kubernetes context
echo "### Switching to Kubernetes context: $CONTEXT..."
kubectl config set-context --current --namespace="$CONTEXT"

if [[ $? -ne 0 ]]; then
  echo "Error: Failed to switch to the context '$CONTEXT'"
  exit 1
fi

# Step 2: Validate and fetch secret values from 'db-secrets'
echo "### Verifying and fetching database secrets..."
if kubectl get secret db-secrets &>/dev/null; then
  echo "Secrets found. Decoding values..."
  REMOTE_HOST=$(kubectl get secret db-secrets -o jsonpath="{.data.host}" | base64 -d)
  REMOTE_PORT=$(kubectl get secret db-secrets -o jsonpath="{.data.port}" | base64 -d)
  REMOTE_USER=$(kubectl get secret db-secrets -o jsonpath="{.data.username}" | base64 -d)
  REMOTE_PASSWORD=$(kubectl get secret db-secrets -o jsonpath="{.data.password}" | base64 -d)
  REMOTE_DB=$(kubectl get secret db-secrets -o jsonpath="{.data.database}" | base64 -d)
  echo "Secrets successfully decoded."
else
  echo "Warning: 'db-secrets' not found. Ensure that your database details are correct."
  exit 1
fi

# Step 3: Create a dump of the remote database
echo "### Creating a dump of the remote database..."
docker run --rm \
  -e PGPASSWORD="$REMOTE_PASSWORD" \
  -v "$TEMP_DIR":/backup \
  postgres \
  pg_dump -h "$REMOTE_HOST" -U "$REMOTE_USER" -d "$REMOTE_DB" -f /backup/"$DUMP_FILE_NAME"

if [[ $? -ne 0 ]]; then
  echo "Error: Failed to create database dump."
  exit 1
fi
echo "Remote database dump created at $DUMP_FILE_PATH."

# Step 4: Clear the local database
echo "### Clearing the local database: $LOCAL_DB in container: $POSTGRES_CONTAINER"
docker exec -i "$POSTGRES_CONTAINER" \
  psql -U "$LOCAL_USER" -d "$LOCAL_DB" -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"

if [[ $? -ne 0 ]]; then
  echo "Error: Failed to clear the local database."
  exit 1
fi
echo "Local database cleared successfully."

# Step 5: Apply the dump to the local database in the existing container
echo "### Applying dump to the local database in container: $POSTGRES_CONTAINER"
docker exec -i "$POSTGRES_CONTAINER" \
  psql -U "$LOCAL_USER" -d "$LOCAL_DB" < "$DUMP_FILE_PATH"

if [[ $? -ne 0 ]]; then
  echo "Error: Failed to apply the dump to the local database."
  exit 1
fi

echo "Dump successfully applied to the local database ($LOCAL_DB)."