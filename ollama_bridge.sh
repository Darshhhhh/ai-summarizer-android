#!/data/data/com.termux/files/usr/bin/bash

APP_DIR="/sdcard/Ollama"
mkdir -p "$APP_DIR"

while true; do
  if [ -f "$APP_DIR/input.txt" ]; then
    prompt=$(cat "$APP_DIR/input.txt")
    rm "$APP_DIR/input.txt"

    # Escape JSON safely
    safe_prompt=$(printf '%s' "$prompt" | sed ':a;N;$!ba;s/\\/\\\\/g; s/\"/\\\"/g; s/\n/\\n/g')

    # Call Ollama
    curl -s http://127.0.0.1:11434/api/generate       -H "Content-Type: application/json"       -d "{\"model\": \"llama3.2:1b\", \"prompt\": \"$safe_prompt\", \"stream\": false}"       > "$APP_DIR/output_raw.json"

    # Extract response text if possible
    if jq -e '.response' "$APP_DIR/output_raw.json" >/dev/null 2>&1; then
      jq -r '.response' "$APP_DIR/output_raw.json" > "$APP_DIR/output.txt"
    else
      cp "$APP_DIR/output_raw.json" "$APP_DIR/output.txt"
    fi
  fi
  sleep 1
done