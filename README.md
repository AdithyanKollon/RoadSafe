# RoadSafe
### Smart Pothole Detection, Reporting and Analysis System

---

## Overview

RoadSafe is an Android application built to make pothole reporting fast, accurate, and useful. Instead of relying on manual surveys or reactive maintenance, the app lets users report potholes in real time — complete with photos, GPS coordinates, severity ratings, and timestamps.

What sets it apart from a basic reporting tool is the layer of business logic on top: impact scores, risk classification, and dashboard analytics that help prioritize which roads need attention most. Data is stored locally with SQLite for offline use and synced to Firebase Firestore when connectivity is available.

---

## Features

- Capture pothole images directly from the mobile camera
- Automatic GPS tagging with latitude and longitude
- Interactive map view with pothole markers
- Dashboard showing total reports and average severity
- Risk classification — Low, Moderate, or High — based on aggregated data
- Impact score calculation per report
- Offline storage via SQLite with cloud sync to Firebase Firestore
- User authentication with Firebase Auth
- Analytics page showing severity distribution across reports
- Points system to encourage user participation

---

## Business Logic

**Impact Score**
```
Impact Score = Severity × User Rating
```

**Severity Levels**

| Label  | Value |
|--------|-------|
| Low    | 1     |
| Medium | 2     |
| High   | 3     |

**Risk Classification (based on average severity)**

| Average Severity | Risk Level    |
|-----------------|---------------|
| >= 2.5          | High Risk     |
| 1.5 – 2.49      | Moderate Risk |
| < 1.5           | Low Risk      |

**Points System**

| Action            | Points |
|------------------|--------|
| Report a pothole  | +10    |
| Resolve a pothole | +5     |

---

## Tech Stack

| Layer             | Technology                |
|------------------|---------------------------|
| Frontend          | Android (Java, XML)       |
| Cloud Backend     | Firebase Firestore        |
| Authentication    | Firebase Auth             |
| Local Storage     | SQLite                    |
| Maps              | osmdroid                  |
| Location Services | Google Play Services      |

---

## Installation & Setup

**1. Clone the repository**
```bash
git clone https://github.com/your-username/RoadSafe.git
```

**2. Open in Android Studio**
- Open Android Studio
- Click **Open Project** and select the cloned folder

**3. Configure Firebase**
- Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
- Register your Android app and download `google-services.json`
- Place the file inside the `app/` directory
- Enable **Authentication** and **Firestore Database** in the Firebase console

**4. Run the app**
- Connect a physical Android device or start an emulator
- Click **Run** in Android Studio

---

## Usage

1. Register or log in with your account
2. Tap to capture a photo of the pothole
3. Select severity level and provide a rating
4. Save the report — it stores locally and syncs when online
5. View all reported potholes on the map
6. Check the dashboard and analytics for insights

---

## Project Structure

```
app/
├── java/com/example/roadsafe/
│   ├── HomeActivity.java
│   ├── CaptureActivity.java
│   ├── MapActivity.java
│   ├── StatsActivity.java
│   ├── PotholeProvider.java
│   └── DatabaseHelper.java
│
├── res/
│   ├── layout/
│   ├── drawable/
│   └── values/
│
└── AndroidManifest.xml
```

---

## Future Enhancements

- AI-based pothole detection using image classification
- Real-time GPS tracking for continuous road monitoring
- Route safety recommendations based on reported data
- Push notifications for potholes near the user's location
- Heatmap visualization of pothole density by area

---

## Conclusion

RoadSafe goes beyond a simple reporting app by adding intelligent prioritization and analytics that can genuinely inform road maintenance decisions. The combination of offline-first storage, geospatial visualization, and a clear impact scoring system makes it a practical tool for both everyday users and the authorities responsible for road upkeep.

---

## Author

K Adithyan
