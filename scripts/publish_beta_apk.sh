#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."

# env
: "${TELEGRAM_BUILDS_BOT_TOKEN:?Set TELEGRAM_BUILDS_BOT_TOKEN in ~/.zshrc}"
: "${TELEGRAM_BUILDS_CHAT_ID:?Set TELEGRAM_BUILDS_CHAT_ID in ~/.zshrc}"

# build
BRANCH=$(git rev-parse --abbrev-ref HEAD)
START=$(date +%s)
./gradlew assembleBeta -q

# duration
DURATION=$(( $(date +%s) - START ))
if (( DURATION >= 60 )); then
    TIME="$(( DURATION / 60 ))m $(( DURATION % 60 ))s"
else
    TIME="${DURATION}s"
fi

# apk
APK=$(find app/build/outputs/apk/beta -name '*.apk' | head -1)
[[ -n "$APK" ]] || { echo "No APK found"; exit 1; }

# send
RESPONSE=$(curl -s -F document=@"$APK" \
    -F caption="\`beta | ${BRANCH} | ${TIME}\`" \
    -F parse_mode="MarkdownV2" \
    "https://api.telegram.org/bot${TELEGRAM_BUILDS_BOT_TOKEN}/sendDocument?chat_id=${TELEGRAM_BUILDS_CHAT_ID}")
[[ "$RESPONSE" == *'"ok":true'* ]] && echo "Sent." || { echo "Failed: $RESPONSE"; exit 1; }
