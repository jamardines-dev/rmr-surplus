#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
ENV_FILE="$ROOT_DIR/.env"

if [ ! -f "$ENV_FILE" ]; then
  echo "Missing .env file. Create one with DB_URL, DB_USERNAME, and DB_PASSWORD before packaging." >&2
  exit 1
fi

set -a
. "$ENV_FILE"
set +a

: "${DB_URL:?DB_URL is required in .env}"
: "${DB_USERNAME:?DB_USERNAME is required in .env}"
: "${DB_PASSWORD:?DB_PASSWORD is required in .env}"

BUILD_ID="$(date +%Y%m%d-%H%M%S)"
APP_NAME="RMR Inventory"
INPUT_DIR="/private/tmp/rmr-jpackage-input-$BUILD_ID"
DEST_DIR="$ROOT_DIR/dist/macos-$BUILD_ID"
ZIP_FILE="$ROOT_DIR/dist/RMR-Inventory-macos-$BUILD_ID.zip"

cd "$ROOT_DIR"

echo "Building desktop app..."
./mvnw -pl rmr package -DskipTests

echo "Preparing packaging input..."
mkdir -p "$INPUT_DIR" "$DEST_DIR" "$ROOT_DIR/dist"
cp rmr/target/inventory-desktop-0.0.1-SNAPSHOT.jar "$INPUT_DIR/"
cp -R rmr/target/libs "$INPUT_DIR/libs"

echo "Creating macOS app bundle..."
jpackage \
  --type app-image \
  --name "$APP_NAME" \
  --input "$INPUT_DIR" \
  --main-jar inventory-desktop-0.0.1-SNAPSHOT.jar \
  --dest "$DEST_DIR" \
  --java-options "-DDB_URL=$DB_URL" \
  --java-options "-DDB_USERNAME=$DB_USERNAME" \
  --java-options "-DDB_PASSWORD=$DB_PASSWORD"

echo "Creating distributable zip..."
ditto -c -k --sequesterRsrc --keepParent "$DEST_DIR/$APP_NAME.app" "$ZIP_FILE"

echo
echo "App bundle: $DEST_DIR/$APP_NAME.app"
echo "Zip file:   $ZIP_FILE"
