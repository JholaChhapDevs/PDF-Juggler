<h1 align="center">PDF Juggler</h1>
<p align="center">
    <em>A powerful desktop PDF reader and editor built with Kotlin Multiplatform and Jetpack Compose</em>
</p>

## Introduction:
**PDF Juggler** is a comprehensive desktop PDF management application that provides an intuitive interface for reading, editing, and manipulating PDF documents. Built using **Kotlin Multiplatform** and **Jetpack Compose Desktop**, it offers advanced features like AI-powered document analysis, text-to-speech capabilities, and seamless PDF editing tools.

## Table of Contents
- [Technology Stack](#technologies-used)
- [Features](#features)
- [Installation & Setup](#installation--setup)
  - [Prerequisites](#prerequisites)
  - [Steps to Run](#steps-to-run)
- [Project Structure](#project-structure)
- [Building for Distribution](#building-for-distribution)
- [Contributing](#contributing)

## Technologies Used

- **Kotlin Multiplatform**
  - Jetpack Compose Desktop
  - Coroutines
- **PDF Processing**
  - Apache PDFBox
- **AI Integration**
  - Google Gemini API
- **Text-to-Speech**
  - MaryTTS
- **Networking**
  - Ktor Client
- **UI Framework**
  - Compose Multiplatform
  - Material 3 Design
  - Voyager Navigation

## Features

- **PDF Viewing & Navigation**: High-quality PDF rendering with smooth navigation and zoom controls
- **Multi-Tab Support**: Open and manage multiple PDF documents simultaneously
- **AI-Powered Analysis**: Generate table of contents and document summaries using Google Gemini AI
- **Text-to-Speech**: Convert PDF text to speech with MaryTTS integration
- **Search & Highlight**: Advanced text search with highlighting capabilities
- **Bookmark Management**: Create and manage bookmarks for quick navigation
- **Page Management**: Reorder, extract, and manipulate PDF pages
- **Print Support**: Advanced printing options with custom settings
- **Encryption/Decryption**: Secure document handling with encryption capabilities
- **Auto-Updates**: Built-in update mechanism for seamless application updates
- **Cross-Platform**: Desktop application targeting Windows, macOS, and Linux

## Installation & Setup

### Prerequisites
- JDK 17 or higher
- Gradle 8.0+
- An IDE with Kotlin support (IntelliJ IDEA recommended)

### Steps to Run

1. **Clone the Repository**
   ```sh
   git clone https://github.com/your-username/PDF-Juggler.git
   cd PDF-Juggler
   ```

2. **Configure Environment Variables** (Required for AI features)
   - Copy `sample_Env.kt` to `Env.kt` in the same directory:
     ```shell
     copy "composeApp\src\jvmMain\kotlin\com\jholachhapdevs\pdfjuggler\core\util\sample_Env.kt" "composeApp\src\jvmMain\kotlin\com\jholachhapdevs\pdfjuggler\core\util\Env.kt"
     ```
   - Edit the `Env.kt` file and replace placeholder values:
     - `GEMINI_API_KEY`: Your Google Gemini API key for AI features
       - Get your API key from [Google AI Studio](https://makersuite.google.com/app/apikey)
       - Create a new project and enable the Gemini API
     - `PREFS_KEY`: A custom encryption key for preferences (16+ characters)
   
   **Note**: The `Env.kt` file is already added to `.gitignore` to keep your API keys secure.

3. **Build and Run the Application**
   - On Windows:
     ```shell
     .\gradlew.bat :composeApp:run
     ```
   - On macOS/Linux:
     ```shell
     ./gradlew :composeApp:run
     ```

4. **Start Using PDF Juggler!**
   - The application will launch with a modern desktop interface
   - Open PDF files through the file menu or drag and drop
   - Explore the various features through the intuitive UI

## Project Structure

This is a Kotlin Multiplatform project with the following structure:

- **[/composeApp/src/commonMain](./composeApp/src/commonMain/kotlin)** - Shared code across platforms
- **[/composeApp/src/jvmMain](./composeApp/src/jvmMain/kotlin)** - Desktop-specific implementations
  - `feature/pdf/` - PDF viewing and manipulation
  - `feature/ai/` - AI integration with Gemini
  - `feature/tts/` - Text-to-speech functionality
  - `feature/update/` - Application update management
  - `core/` - Core utilities and UI components

## Building for Distribution

To create distributable packages:

```shell
# Create MSI installer (Windows)
.\gradlew.bat :composeApp:packageMsi

# Create DMG installer (macOS)
./gradlew :composeApp:packageDmg

# Create DEB package (Linux)
./gradlew :composeApp:packageDeb
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
