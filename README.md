## <img src="assets/logo.webp" width="100" align="absmiddle" /> RailNav

RailNav is an enterprise-grade Android application designed to assist commuters in navigating complex, multi-level railway station layouts. It provides offline-capable, graph-based walking directions to facilities such as platforms, exits, ticket counters, and lifts. By leveraging advanced pathfinding algorithms and interactive mapping, the application solves the critical challenge of locating specific amenities within high-density transit hubs, particularly in GNSS-denied environments like transit stations roofed with reinforced concrete.

## Features

* **Accessibility-Aware Pathfinding:** Utilizes dynamic A* algorithm models to compute optimal routes, adapting edge costs for specific pedestrian profiles such as wheelchair or heavy luggage users.


* **Offline-First Capabilities:** Designed to maintain full routing capabilities during network blackouts by relying on an edge-native paradigm and localized data caching.


* **Interactive Station Mapping:** Visualizes the user's current location, station facilities, and the calculated route using OpenStreetMap data.
* **Contextual Search & Discovery:** Allows users to rapidly locate facilities by name or type with real-time filtering and distance estimations.
* **Real-Time Navigation:** Integrates Android Location Services to detect user position and suggest the nearest starting node.
* **Turn-by-Turn Directions:** Generates step-by-step textual instructions derived from accurate path geometry.
* **Live Transit Integration:** Seamlessly integrates real-time transit dashboards directly alongside the navigation overlays.

## Tech Stack

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (Material3)
* **Map Engine:** OSMDroid (OpenStreetMap for Android)
* **Architecture:** MVVM (Model-View-ViewModel) with Unidirectional Data Flow
* **Data Format:** GeoJSON (parsed via Kotlinx Serialization)
* **Build System:** Gradle with Version Catalogs (`libs.versions.toml`)

## Getting Started

Follow these instructions to set up the project locally for development and testing.

### Prerequisites

Ensure your development environment meets the following requirements:

* **Android Studio:** Latest stable version recommended.
* **Java Development Kit (JDK):** Version 11.
* **Android SDK:** API Level 36 (Baklava) is required for compilation.
* **Min SDK:** API Level 24 (Android 7.0).

### Installation

1. **Clone the repository:**
```bash
git clone https://github.com/railnav-org/app.git
cd app

```


2. **Open in Android Studio:**
Open Android Studio and select **File > Open**, then navigate to the cloned directory.
3. **Sync Gradle:**
Allow Android Studio to download dependencies and sync the project. This may take a few minutes.
4. **Build and Run:**
Connect an Android device (or use an emulator) and execute the run command:
```bash
./gradlew installDebug

```


*Note: A physical device with GPS capabilities is recommended to test location-based features effectively.*

## Permissions

The application requires the following permissions to function correctly:

* `ACCESS_FINE_LOCATION`: To determine the user's precise position within the station.
* `INTERNET`: Required by OSMDroid to fetch map tiles and live transit data.
* `WRITE_EXTERNAL_STORAGE`: Used for caching map tiles (max SDK 32).
