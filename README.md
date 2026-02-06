Multilingual Chat Assistant for Android

This project is a privacy-focused multilingual Android chat assistant designed to improve cross-language communication by helping users better understand tone and intent in messages. The application supports multilingual message detection and translation, tone- and intent-aware reply suggestions, speech-to-text input, a floating chat bubble overlay, contextual GIF suggestions, and persistent local message history.

Mobile messaging often loses emotional context when messages are translated literally, leading to misunderstandings in everyday conversations. Many existing AI assistants rely on cloud-based processing, which raises privacy concerns and introduces latency. This project addresses these issues by performing core natural language processing directly on the device while remaining compliant with modern Android system constraints.

The application uses a rule-based natural language processing approach for tone and intent detection. This method was intentionally chosen to ensure predictable behaviour, low computational overhead, and improved privacy through on-device processing. Slang normalisation is applied to incoming text to improve consistency, and reply suggestions are generated deterministically to maintain reliability. Translation and GIF retrieval are only used when required.

From a mobile systems perspective, the project demonstrates advanced Android concepts including lifecycle-aware development, foreground service enforcement, overlay window management, background execution limits, secure permission handling, and offline-capable local data persistence using the Room database. The floating chat bubble is implemented using a foreground service to ensure stability under Android 14 restrictions.

Testing focused on realistic mobile usage scenarios such as background and foreground transitions, permission denial and recovery, overlay stability, offline operation, and speech recognition behaviour. The final implementation demonstrated stable performance, low response latency, and reliable message history persistence across typical Android usage conditions.

The project has some limitations. Rule-based NLP cannot fully capture complex language patterns such as sarcasm or cultural nuance, and translation quality depends on third-party services. The overlay bubble may also be intrusive on smaller screens for some users. These limitations were acknowledged as part of the project’s critical reflection.

Future improvements could include integrating lightweight on-device pretrained language models to enhance semantic understanding, adding adaptive tone personalisation, expanding language support, and exploring offline translation models. These enhancements would improve accuracy and flexibility while preserving the project’s privacy-first design.

This application was developed using Java in Android Studio as part of an Advanced Mobile Systems module. It is intended for academic and educational purposes and demonstrates practical application of modern Android system design principles.
