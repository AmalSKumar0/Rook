# Rook: Bubbly API Tester 🚀

**Rook** is a powerful, mobile-first API testing and management platform built with a unique "Soft-Brutalist" design aesthetic. It transforms the complex world of API debugging into a friendly, tactile, and highly efficient experience.

![App Logo](app/src/main/res/drawable/ic_rook_logo.xml)

---

## ✨ Features

- **The Workbench (API Lab):** A playful yet precise environment for crafting and testing REST API requests (GET, POST, PUT, DELETE, etc.).
- **Workspace Organization:** Group your APIs into **Projects** and **Collections** for seamless workflow management.
- **Visual Feedback:** Instant status codes, latency metrics, and "bubbly" badges to categorize method types.
- **Historical Reports:** Track API performance over time with a dedicated analytics view.
- **Cloud Identity:** Secure authentication via Firebase to keep your workspaces synchronized.
- **Soft-Brutalist UI:** A distinct visual identity featuring thick strokes, pastel palettes, and chunky geometry.

---

## 🎨 Design System: "Playful Precision"

Rook is built on the **Bubbly API System**, which combines high-contrast line art with a physical metaphor of "squishiness."

- **Defined Outlines:** Every card and button is anchored by a 2px carbon-black stroke.
- **Pastel Palette:**
  - 🌿 **Mint Green (Primary):** Success and primary actions.
  - 🔮 **Pale Lavender (Secondary):** Utility and info.
  - 🍑 **Light Peach (Tertiary):** Warnings and focus.
- **Chunky Geometry:** 16px to 24px corner radii for a friendly, approachable feel.

For a deep dive into the code structure, see [ARCHITECTURE.md](ARCHITECTURE.md).

---

## 🛠️ Tech Stack

- **Platform:** Native Android (Java/Kotlin)
- **Networking:** [OkHttp](https://square.github.io/okhttp/)
- **Database:** SQLite (Room-ready architecture)
- **Auth:** [Firebase Authentication](https://firebase.google.com/docs/auth)
- **UI:** [Material 3](https://m3.material.io/) + Custom Bubbly Styling
- **Async:** Custom Executor Service for background I/O

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- JDK 11+
- Android SDK 34+

### Setup
1. **Clone the repository:**
   ```bash
   git clone https://github.com/yourusername/rook.git
   ```
2. **Firebase Configuration:**
   - Create a project in the [Firebase Console](https://console.firebase.google.com/).
   - Add an Android App with package name `com.example.rook`.
   - Download `google-services.json` and place it in the `app/` directory.
3. **Build & Run:**
   - Open the project in Android Studio.
   - Sync Gradle.
   - Run on an emulator or physical device.

---

## 📂 Project Structure

- `app/src/main/java`: Core logic and activities.
- `app/src/main/res`: Design system implementation (layouts, colors, themes).
- `app/src/main/assets`: Design specifications and documentation.

---

## 📜 License

Distributed under the MIT License. See `LICENSE` for more information.

---

*Built with ❤️ for developers who love clean APIs and bold design.*
