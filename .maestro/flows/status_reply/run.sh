#!/usr/bin/env bash
# Orchestrates the 2-step cross-device "reply to status" flow (see ../../README.md "Flujo
# multi-dispositivo (Responder a Estado)"). Requires two devices/emulators already running and a
# fresh build of THIS feature branch installed on both (this feature isn't in develop yet — see
# that README section for why "already installed" can't be assumed).
#
# Only covers steps 1-2 (post + reply + navigation + quote content + reopen-viewer) — the optional
# 3rd step (expired-quote rendering) needs a real, manual, out-of-band SQL write against the linked
# Supabase project first and is intentionally NOT part of this script; run
# 03_expired_quote_verify.yaml directly afterwards if you want that coverage too (see that file's
# header for the exact SQL and why it isn't automated here).
#
# PRECONDITION: the two QA accounts must already have a 1:1 conversation between them (so the
# replier's ConversationListScreen/StatusViewModel already has the poster as a contactId — see
# StatusRepositoryImpl.observeActiveStatuses' contactIds filter). Already true for
# @claudeqa/@claudeqa2 (used throughout this whole suite), so nothing extra to do for that pair.
#
# Usage:
#   bash run.sh <poster_device_id> <poster_email> <poster_password> \
#               <replier_device_id> <replier_email> <replier_password> <poster_display_name>
#
# Example (from the repo root, with the two real QA accounts):
#   set -a && source .maestro/.env && set +a
#   bash .maestro/flows/status_reply/run.sh \
#     emulator-5554 "$QA_EMAIL" "$QA_PASSWORD" \
#     emulator-5556 claude.qa2.chatapp@gmail.com "$QA_PASSWORD" claudeqa
set -euo pipefail

POSTER_DEVICE=$1; POSTER_EMAIL=$2; POSTER_PASSWORD=$3
REPLIER_DEVICE=$4; REPLIER_EMAIL=$5; REPLIER_PASSWORD=$6; POSTER_DISPLAY_NAME=$7

# Shared, verifiably-unique tokens: generated once here so the poster's status and the replier's
# reply/assertions all agree on exact values, and a leftover status/reply from an earlier run (no
# cleanup step exists — see 01_poster_post.yaml) can never match by coincidence.
TS="$(date +%s)"
STATUS_TOKEN="Maestro status-reply target $TS"
REPLY_TEXT="Maestro status-reply $TS"

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "== Step 1/2: poster ($POSTER_DEVICE) posts a status with token: $STATUS_TOKEN =="
maestro test "$DIR/01_poster_post.yaml" --device "$POSTER_DEVICE" \
  -e LOGIN_EMAIL="$POSTER_EMAIL" -e LOGIN_PASSWORD="$POSTER_PASSWORD" -e STATUS_TOKEN="$STATUS_TOKEN"

echo "== Step 2/2: replier ($REPLIER_DEVICE) opens it and replies: $REPLY_TEXT =="
maestro test "$DIR/02_replier_reply.yaml" --device "$REPLIER_DEVICE" \
  -e LOGIN_EMAIL="$REPLIER_EMAIL" -e LOGIN_PASSWORD="$REPLIER_PASSWORD" \
  -e POSTER_DISPLAY_NAME="$POSTER_DISPLAY_NAME" \
  -e STATUS_TOKEN="$STATUS_TOKEN" -e REPLY_TEXT="$REPLY_TEXT"

echo "Status reply round trip OK."
echo "(Optional) to also verify the expired-quote rendering, run against the linked Supabase project:"
echo "  update public.messages set reply_to_status_expires_at = now() - interval '1 hour' where content = '$REPLY_TEXT';"
echo "then: maestro test $DIR/03_expired_quote_verify.yaml --device $REPLIER_DEVICE -e POSTER_DISPLAY_NAME=$POSTER_DISPLAY_NAME -e REPLY_TEXT=\"$REPLY_TEXT\""
