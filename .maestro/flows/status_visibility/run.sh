#!/usr/bin/env bash
# Orchestrates the 3-step cross-device Status visibility flow (see
# ../../README.md "Flujo multi-dispositivo (Estado / Status)"). Requires
# two devices/emulators already running and the app installed on both.
#
# PRECONDITION: the two QA accounts must already have a 1:1 conversation
# between them — see 03_viewer_verify_live.yaml's comment for why. Already
# true for @claudeqa/@claudeqa2 (used throughout this whole suite), so
# nothing extra to do for that pair.
#
# Usage:
#   bash run.sh <viewer_device_id> <viewer_email> <viewer_password> \
#               <poster_device_id> <poster_email> <poster_password> <poster_display_name>
#
# Example (from the repo root, with the two real QA accounts):
#   set -a && source .maestro/.env && set +a
#   bash .maestro/flows/status_visibility/run.sh \
#     emulator-5556 claude.qa2.chatapp@gmail.com "$QA_PASSWORD" \
#     emulator-5554 "$QA_EMAIL" "$QA_PASSWORD" claudeqa
set -euo pipefail

VIEWER_DEVICE=$1; VIEWER_EMAIL=$2; VIEWER_PASSWORD=$3
POSTER_DEVICE=$4; POSTER_EMAIL=$5; POSTER_PASSWORD=$6; POSTER_DISPLAY_NAME=$7

# Shared, verifiably-unique token: generated once here so both steps agree
# on the exact same value, and a leftover status from an earlier run (no
# cleanup step exists — see 02_poster_post.yaml) can never match it by
# coincidence.
STATUS_TOKEN="Maestro status $(date +%s)"

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "== Step 1/3: viewer ($VIEWER_DEVICE) opens the conversation list and waits =="
maestro test "$DIR/01_viewer_wait.yaml" --device "$VIEWER_DEVICE" \
  -e LOGIN_EMAIL="$VIEWER_EMAIL" -e LOGIN_PASSWORD="$VIEWER_PASSWORD"

echo "== Step 2/3: poster ($POSTER_DEVICE) posts a status with token: $STATUS_TOKEN =="
maestro test "$DIR/02_poster_post.yaml" --device "$POSTER_DEVICE" \
  -e LOGIN_EMAIL="$POSTER_EMAIL" -e LOGIN_PASSWORD="$POSTER_PASSWORD" -e STATUS_TOKEN="$STATUS_TOKEN"

echo "== Step 3/3: viewer ($VIEWER_DEVICE) confirms LIVE delivery, without relaunching =="
maestro test "$DIR/03_viewer_verify_live.yaml" --device "$VIEWER_DEVICE" \
  -e POSTER_DISPLAY_NAME="$POSTER_DISPLAY_NAME"

echo "Status visibility (live) round trip OK."
