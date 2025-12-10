# Multilingual Chat Assistant (Android)

This is my final year project – a multilingual chat assistant built using Android (Java).  
The main goal of this project is to help users understand incoming messages in different languages and generate appropriate replies with tone, intent, and even GIF support.

The app works mainly **offline** and focuses on **user privacy**, meaning messages are not sent to external servers for processing (except for GIF fetching).

---

## ✅ Features

- Translate incoming messages into the user’s preferred language
- Detect the tone and intent of messages
- Generate smart replies based on context
- Manual tone selection (friendly, formal, casual, etc.)
- Speech-to-text for both incoming messages and replies
- GIF generation based on message meaning and emotion
- Copy reply and GIF link options
- Message history stored locally using Room Database
- Floating chat bubble overlay
- Works fully offline for NLP and translation

---

## ✅ How the System Works (Simple Explanation)

1. The user pastes or speaks an incoming message.
2. The app detects the language automatically.
3. The message is translated into the user’s reading language.
4. The system detects:
   - Intent (greeting, question, thanks, etc.)
   - Tone (friendly, formal, casual, etc.)
5. The user types a reply or speaks it.
6. The system styles the reply and translates it back into the original sender’s language.
7. A suitable GIF is generated based on intent and tone.
8. Everything is saved into local history.

---

##  Technologies Used

- Java
- Android Studio
- XML (UI Design)
- Room Database
- Retrofit
- Giphy API
- Google ML Kit (Offline Translation)
- Hybrid NLP System (Rule-based + ML-ready)
- Git & GitHub

---

##  NLP & Hybrid Design (In Simple Words)

The app uses a **hybrid NLP system**:
- At the moment, it mainly uses **rule-based logic** for detecting intent and tone.
- However, the system is **ready for machine learning integration** using ONNX models in the future.
- This allows the app to work fully offline while being future-proof.

---

##  Project Structure

```text
app/
 ├── data/        → Database files (Room)
 ├── network/     → API handling (GIF)
 ├── nlp/         → NLP processing
 ├── overlay/     → Floating bubble
 ├── ui/          → Activities & UI logic
 ├── util/        → Helper classes
 └── res/         → Layouts, images, values
