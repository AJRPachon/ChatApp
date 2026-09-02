#!/usr/bin/env bash
# Orchestrates the 4-step cross-device Realtime flow (see ../../README.md
# "Flujo multi-dispositivo (Realtime)"). Requires two devices/emulators
# already running and the app installed on both.
#
# Usage:
#   bash run.sh <sender_device_id> <sender_email> <sender_password> <sender_contact_username> \
#               <recipient_device_id> <recipient_email> <recipient_password> <recipient_contact_username>
#
# Example (from the repo root):
#   bash .maestro/flows/realtime/run.sh \
#     emulator-5554 claude.qa.chatapp@gmail.com "$QA_PASSWORD" claudeqa2 \
#     emulator-5556 claude.qa2.chatapp@gmail.com "$QA_PASSWORD" claudeqa
set -euo pipefail

SENDER_DEVICE=$1; SENDER_EMAIL=$2; SENDER_PASSWORD=$3; SENDER_TARGET=$4
RECIPIENT_DEVICE=$5; RECIPIENT_EMAIL=$6; RECIPIENT_PASSWORD=$7; RECIPIENT_TARGET=$8

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "== Step 1/4: recipient ($RECIPIENT_DEVICE) opens the conversation and waits =="
maestro test "$DIR/01_recipient_wait.yaml" --device "$RECIPIENT_DEVICE" \
  -e LOGIN_EMAIL="$RECIPIENT_EMAIL" -e LOGIN_PASSWORD="$RECIPIENT_PASSWORD" -e CONTACT_NAME="$RECIPIENT_TARGET"

echo "== Step 2/4: sender ($SENDER_DEVICE) sends the ping =="
maestro test "$DIR/02_sender_send.yaml" --device "$SENDER_DEVICE" \
  -e LOGIN_EMAIL="$SENDER_EMAIL" -e LOGIN_PASSWORD="$SENDER_PASSWORD" -e CONTACT_NAME="$SENDER_TARGET"

echo "== Step 3/4: recipient ($RECIPIENT_DEVICE) confirms live delivery =="
maestro test "$DIR/03_recipient_verify.yaml" --device "$RECIPIENT_DEVICE"

echo "== Step 4/4: sender ($SENDER_DEVICE) cleans up the ping =="
maestro test "$DIR/04_sender_cleanup.yaml" --device "$SENDER_DEVICE"

echo "Realtime round trip OK."
