#!/bin/bash
# ------------------------------------------------------------------------------
# Automates the process of dumping a primary database and restoring it into a
# secondary database using Kubernetes contexts and Docker.

# Capture the start time of the script
SCRIPT_START_TIME=$(date +%s)

# Temporary directory for database dump
TEMP_DIR="./tmp_db_sync"
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

# Cleanup function to remove all temporary files and directory
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

  SCRIPT_END_TIME=$(date +%s)
  EXECUTION_TIME=$((SCRIPT_END_TIME - SCRIPT_START_TIME))
  echo "### Script execution completed in $EXECUTION_TIME seconds."
}

# Register the cleanup function to run on script exit
trap cleanup EXIT

usage() {
  echo "Usage: $0 -S <source_context> -R <target_context>"
  echo "  -S   Kubernetes context (e.g., klibs-prod) for the primary database"
  echo "  -R   Kubernetes context (e.g., klibs-test) for the secondary database"
  exit 1
}

# Parse command-line arguments
while getopts "S:R:" opt; do
  case "$opt" in
    S) CONTEXT="$OPTARG" ;;
    R) SECOND_CONTEXT="$OPTARG" ;;
    *) usage ;;
  esac
done

# Ensure required parameters are provided
if [[ -z "$CONTEXT" || -z "$SECOND_CONTEXT" ]]; then
  usage
fi

# Step 0: Create temporary directory for the dump file
create_temp_dir

# Step 1: Switch to the primary Kubernetes context
echo "### Switching to Kubernetes context: $CONTEXT..."
kubectl config set-context --current --namespace="$CONTEXT"

if [[ $? -ne 0 ]]; then
  echo "Error: Failed to switch to the context '$CONTEXT'."
  exit 1
fi

# Step 2: Fetch primary database connection details
echo "### Retrieving primary database secrets..."
if kubectl get secret db-secrets &>/dev/null; then
  REMOTE_HOST=$(kubectl get secret db-secrets -o jsonpath="{.data.host}" | base64 -d)
  REMOTE_PORT=$(kubectl get secret db-secrets -o jsonpath="{.data.port}" | base64 -d)
  REMOTE_USER=$(kubectl get secret db-secrets -o jsonpath="{.data.username}" | base64 -d)
  REMOTE_PASSWORD=$(kubectl get secret db-secrets -o jsonpath="{.data.password}" | base64 -d)
  REMOTE_DB=$(kubectl get secret db-secrets -o jsonpath="{.data.name}" | base64 -d)
  echo "Primary database secrets retrieved successfully."
else
  echo "Error: 'db-secrets' not found in context $CONTEXT."
  exit 1
fi

# Step 3: Create a dump of the primary database
echo "### Creating a dump of the primary database..."
docker run --rm \
  -e PGPASSWORD="$REMOTE_PASSWORD" \
  -v "$TEMP_DIR":/backup \
  postgres \
  pg_dump -h "$REMOTE_HOST" -U "$REMOTE_USER" -d "$REMOTE_DB" -f /backup/"$DUMP_FILE_NAME"

if [[ $? -ne 0 ]]; then
  echo "Error: Failed to create database dump."
  exit 1
fi
echo "Primary database dump created at $DUMP_FILE_PATH."

# Step 4: Switch to the second Kubernetes context
echo "### Switching to Kubernetes context: $SECOND_CONTEXT..."
kubectl config set-context --current --namespace="$SECOND_CONTEXT"

if [[ $? -ne 0 ]]; then
  echo "Error: Failed to switch to the context '$SECOND_CONTEXT'."
  exit 1
fi

# Step 5: Fetch secondary database connection details
echo "### Retrieving secondary database secrets..."
if kubectl get secret db-secrets &>/dev/null; then
  SECOND_HOST=$(kubectl get secret db-secrets -o jsonpath="{.data.host}" | base64 -d)
  SECOND_PORT=$(kubectl get secret db-secrets -o jsonpath="{.data.port}" | base64 -d)
  SECOND_USER=$(kubectl get secret db-secrets -o jsonpath="{.data.username}" | base64 -d)
  SECOND_PASSWORD=$(kubectl get secret db-secrets -o jsonpath="{.data.password}" | base64 -d)
  SECOND_DB=$(kubectl get secret db-secrets -o jsonpath="{.data.name}" | base64 -d)
  echo "Secondary database secrets retrieved successfully."
else
  echo "Error: 'db-secrets' not found in context $SECOND_CONTEXT."
  exit 1
fi

# Step 6: Apply the dump to the secondary database
echo "### Applying dump to the secondary database..."
docker run --rm \
  -e PGPASSWORD="$SECOND_PASSWORD" \
  -v "$TEMP_DIR":/backup \
  postgres \
  psql -h "$SECOND_HOST" -U "$SECOND_USER" -d "$SECOND_DB" -f /backup/"$DUMP_FILE_NAME"

if [[ $? -ne 0 ]]; then
  echo "Error: Failed to apply the dump to the secondary database."
  exit 1
fi
echo "Dump successfully applied to the secondary database ($SECOND_DB)."