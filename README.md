# HempsmyerDon_CE02 - Media Player

A robust Android Media Player application that demonstrates background audio playback using a Foreground Service, MediaSession integration, and a fragment-based UI.

## Features

- **Background Playback**: Audio continues to play even when the app is in the background or the screen is off.
- **Foreground Service**: Uses `AudioPlaybackService` to ensure the system doesn't kill the playback.
- **Media Controls**: Play, Pause, Stop, Skip Next, and Skip Previous functionality.
- **Notification Integration**: Control playback directly from the Android notification drawer.
- **Dynamic UI**: Responsive UI using Fragments that syncs with the service state.
- **Resource Management**: Properly handles audio focus and service lifecycle.

## Technical Details

- **Language**: Java
- **Architecture**: Fragment-based UI with a Bound and Started Service.
- **Components**:
    - `MainActivity`: Orchestrates service binding and fragment management.
    - `PlayerFragment`: Handles user interactions and UI updates.
    - `AudioPlaybackService`: Core logic for `MediaPlayer` and `MediaSession` management.
- **Libraries**:
    - AndroidX AppCompat
    - AndroidX ConstraintLayout
    - AndroidX Media
    - Google Material Components

## Setup Instructions

1. Clone the repository.
2. Open the project in Android Studio (Ladybug or newer recommended).
3. Sync Gradle files.
4. Run the `app` module on an Android device or emulator (API 21+).

## License

Copyright (c) 2024 Don Hempsmyer. All rights reserved.
