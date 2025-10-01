# 📘 AI Summarizer Android App  

An Android app built with Kotlin that summarizes text using a locally running [Ollama](https://ollama.ai) LLM (tested with `llama3.2:1b`).  

The app communicates with Ollama via a Termux bridge script that reads/writes from shared storage.  

---

## 🚀 Features
- Minimal, clean UI with Material Design  
- Word counter + summary style selector  
- Extra options (tone, numbers, action items, etc.)  
- Copy & Share buttons for easy usage  
- Works fully offline with Ollama — no external APIs needed  

---

## 📂 Project Structure
```
ai_summarizer_android/
 ├── app/src/main/java/com/example/ai_summarizer_android/
 │     └── MainActivity.kt
 ├── app/src/main/res/layout/
 │     └── activity_main.xml
 ├── app/src/main/res/values/
 │     └── themes.xml
 ├── README.md
 └── ollama_bridge.sh   (Termux bridge script)
```

---

## ⚡ Setup Guide

### 1. Prerequisites
- Android device with [Termux](https://f-droid.org/packages/com.termux/) installed  
- [Ollama](https://ollama.ai) installed in Termux  
- Model pulled (example: `ollama pull llama3.2:1b`)  
- Storage permissions enabled in Termux:  
  ```bash
  termux-setup-storage
  ```

---

### 2. Shared Folder Setup
Create the folder that both the app and Termux will use:

```bash
mkdir -p /sdcard/Ollama
```

The app will:  
- Write prompts to `/sdcard/Ollama/input.txt`  
- Expect summaries from `/sdcard/Ollama/output.txt`  

---

### 3. The Bridge Script (`ollama_bridge.sh`)

Save this file in Termux home (e.g. `~/ollama_bridge.sh`) and make it executable:

```bash
chmod +x ~/ollama_bridge.sh
```

```bash
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
```

---

### 4. Running the System  

Open **three Termux tabs**:

#### 📌 Tab 1: Start the Ollama server + model
```bash
ollama serve &
ollama run llama3.2:1b
```

#### 📌 Tab 2: Run the bridge script
```bash
bash ~/ollama_bridge.sh
```

#### 📌 Tab 3: (optional) Monitor output for debugging
```bash
cat /sdcard/Ollama/output_raw.json
```

---

### 5. Running the Android App
- Build and install the app on your Android device.  
- Paste text → choose style → tap **Summarize**.  
- The bridge script will handle communication with Ollama.  
- Summarized text appears inside the app.  

---

## 🛠 Troubleshooting
- **App shows “No response from Ollama”** → Check Termux Tab 2, see if `output.txt` is being created.  
- **Permission denied** → Run `termux-setup-storage` and grant storage permissions to both Termux and the app.  
- **Empty summary** → Inspect `/sdcard/Ollama/output_raw.json` in Tab 3 to confirm Ollama’s response.  

---

## 📸 Screenshots
Add your screenshots here:  

```
![App Input Screen](screenshots/input.png)
![App Output Screen](screenshots/output.png)
```

---

## 📜 License
MIT — use freely, modify freely.  
