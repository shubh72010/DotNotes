# honest message from the developer: im too lazy to delete log :P

# DotNotes 📝

DotNotes is a modern, privacy-focused, and feature-rich note-taking application for Android. Built with Jetpack Compose and modern Android architecture, it provides a seamless experience for capturing your thoughts, managing tasks, and keeping your sensitive information secure.

## ✨ Features

- **🗺️ Visual Note Maps**: Organize your thoughts spatially by linking notes together in infinite map views. Maps act as dynamic, linked folders for a modern approach to organization.
- **🤖 Built-in AI Assistant**: Utilize advanced AI (Gemini/OpenRouter) to fix grammar, adjust tone, summarize content, or answer questions directly within your notes.
- **✍️ Powerful Markdown Engine**: Our custom block-based markdown renderer natively supports recursive styles (bold, italic, strikethrough), code blocks, complex tables, quotes, and interactive checklists.
- **🔒 Secret Notes**: Protect your most sensitive information with biometrically secured private notes.
- **✅ Dynamic Checklists**: Easily create and reorder checklist items with smooth drag-and-drop support.
- **🖼️ Image Integration**: Attach local images and even snap photos with your camera in-app for better visual context.
- **📌 Note Pinning & Backlinks**: Keep important notes at the top and seamlessly track related content through bidirectional wiki-links (`[[Note Name]]`).
- **🎨 Customization**: Support for Dark, Light, and System themes, along with full UI animation controls.
- **🔍 Fast Search & Tagging**: Find exactly what you're looking for via real-time search queries and `#tag` filtering.
- **🔐 Privacy First**: All your data is stored locally on your device with intuitive folder-based backup/restore support.

## 🚀 Getting Started

### Prerequisites

- Android Studio
- Android SDK 24+
- Kotlin 2.0+

### Building from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/shubh72010/DotNotes.git
   ```
2. Open the project in Android Studio.
3. Build the project using Gradle:
   ```bash
   ./gradlew assembleDebug
   ```

## 🛠️ Tech Stack

- **UI**: Jetpack Compose
- **Database**: Room Persistence Library
- **Architecture**: MVVM (Model-View-ViewModel) utilizing Kotlin Coroutines & Flows
- **Network**: Retrofit (for AI Integrations)
- **Dependency Management**: Version Catalogs (libs.versions.toml)
- **Security**: Android Biometric Prompt

## 📄 License

This project is licensed under the **GNU General Public License v3.0**. See the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

Contributions are welcome! If you'd like to improve DotNotes, please feel free to:
1. Fork the repository.
2. Create a feature branch.
3. Submit a Pull Request.

---

Made with ❤️ by [flakesofsmth](https://github.com/shubh72010)
