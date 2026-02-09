Act as a Lead Android Architect and DevOps Engineer. I need a production-ready code structure for a gamified discovery app using OpenStreetMap (OSMdroid).

### 1. Architecture & Permissions
- Use MVVM architecture with Jetpack Compose.
- Ensure 'AndroidManifest.xml' includes: ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, ACCESS_BACKGROUND_LOCATION, POST_NOTIFICATIONS, and INTERNET.
- Implement a PermissionHandler to request these at runtime.

### 2. Dynamic City Discovery (Overpass API)
- Create a 'Repository' that fetches cities/towns/villages using the Overpass API.
- Query Logic: Use a 15km 'around' radius based on the user's current lat/lng.
- PERFORMANCE: Implement a "Move Threshold." Only trigger a new API fetch if the user has moved more than 2km from the last fetch location.

### 3. Background Discovery (Geofencing Logic)
- Use 'GeofencingClient' to register 'Dwell' or 'Enter' transitions for city coordinates.
- When a Geofence is triggered: Save city ID to Room DB, trigger a Push Notification, and play the discovery sound.

### 4. Custom Audio Configuration (Scoped Storage)
- Use 'GetContent()' to pick a .mp3 file.
- Implement 'Scoped Storage': Copy the file to the app's internal 'filesDir' so the app retains access permanently.
- Use 'MediaPlayer' with 'AudioAttributes.USAGE_GAME'.

### 5. Automated Build Pipeline (GitHub Actions)
- Provide a YAML workflow file (`.github/workflows/android.yml`).
- The workflow must:
    1. Trigger on every push to the 'main' branch.
    2. Set up JDK 17.
    3. Grant execute permissions to gradlew.
    4. Build the debug APK.
    5. Upload the resulting .apk as a GitHub Build Artifact so it can be downloaded directly from the 'Actions' tab.

### Deliverables:
- Detailed 'build.gradle.kts' with all dependencies.
- The 'OverpassService' and Room Database classes.
- The 'GeofenceBroadcastReceiver'.
- The complete 'android.yml' GitHub Action configuration.
