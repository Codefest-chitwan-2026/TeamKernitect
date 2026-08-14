# Sahara

**The rescue app that helps you in emergencies.**

Sahara is an offline-first emergency communication platform being developed by **Team Kernitect** for the **CodeFest 2026 Provincial Phase — Chitwan, Bagmati Province, Nepal**.

The project is designed for disaster situations where normal communication infrastructure may be unavailable, damaged, overloaded, or unreliable.

Sahara enables small emergency SOS packets to move between nearby participating devices using **Bluetooth Low Energy (BLE)** and a **store-and-forward relay mechanism**, helping emergency information travel toward rescuers even when internet or cellular connectivity is unavailable.

---

## The Problem

During emergencies such as:

* Earthquakes
* Floods
* Landslides
* Heavy rainfall
* Fires
* Medical emergencies
* Infrastructure failures

traditional communication systems may become unavailable or unreliable.

However, many people may still have smartphones with functioning:

* Bluetooth
* GPS
* Local storage
* Wi-Fi hardware

Sahara uses these capabilities to create an offline emergency communication system.

---

## How Sahara Works

A victim can create an SOS with a single action.

```text
Victim Phone
     │
     │ BLE
     ▼
Relay Phone
     │
     │ BLE
     ▼
Relay Phone
     │
     │ BLE
     ▼
Rescue Node
     │
     ▼
Sahara Command Center
```

The SOS is stored locally and automatically forwarded whenever another Sahara-compatible device becomes available.

Users do not need to manually forward emergency packets.

---

## One-Tap SOS

Emergency situations are stressful, and victims may be injured, trapped, or unable to type.

Sahara therefore follows a simple principle:

> **Sending an emergency alert should require as little interaction as possible.**

Pressing the SOS button can immediately create an emergency packet containing information such as:

```json
{
  "id": "SOS-A72F1",
  "type": "EMERGENCY",
  "latitude": 27.68,
  "longitude": 84.43,
  "timestamp": "...",
  "priority": "CRITICAL",
  "hopCount": 0,
  "maxHops": 10
}
```

Additional information can be added afterward if the victim is able to provide it.

Examples include:

* Disaster type
* Short description
* Number of people affected
* Additional observations

Providing these details must not delay the initial SOS.

---

## Store-and-Forward Communication

Sahara is designed as an opportunistic communication system.

If no other Sahara device is currently nearby:

```text
SOS Created
     │
     ▼
Stored Locally
     │
     ▼
Device Continues Searching
     │
     ▼
Another Sahara Device Appears
     │
     ▼
SOS Automatically Relayed
```

This allows emergency packets to continue moving as participating devices encounter one another.

Sahara does **not** claim guaranteed communication when no participating device is within communication range.

---

## BLE Communication

Sahara currently uses **Bluetooth Low Energy (BLE)** for nearby device communication.

The Android application will support:

* BLE advertising
* BLE scanning
* Device discovery
* GATT communication
* Small packet exchange
* Automatic packet relay
* Duplicate detection
* Hop counting
* Message expiration
* Store-and-forward delivery

The BLE transport layer and Sahara routing logic are intentionally separated.

```text
BLE Layer
    │
    ▼
Packet Decoder
    │
    ▼
Mesh / Relay Logic
    │
    ▼
Local Database
    │
    ▼
Next Available Peer
```

BLE determines **how bytes move between devices**.

Sahara's relay system determines **what happens to those bytes after they arrive**.

---

## Duplicate Prevention

Every SOS contains a unique message ID.

Example:

```text
SOS-A72F1
```

When a phone receives a packet, Sahara checks whether that message has already been processed.

```text
Receive SOS
    │
    ▼
Check Message ID
    │
    ├── Already Seen → Ignore
    │
    └── New Message
             │
             ▼
           Store
             │
             ▼
        Increment Hop
             │
             ▼
           Relay
```

This prevents loops such as:

```text
A → B → C → B → A → C
```

---

## Citizen Application

The citizen side is the first development priority.

### Home

The home screen will provide:

* Sahara branding
* Emergency alerts
* GPS status
* BLE / relay status
* Nearby device information
* Recent Sahara activity
* Notifications

### SOS

The central SOS button is intentionally the most prominent action in the application.

The initial emergency packet is created immediately.

Victims can optionally add:

* Flood
* Earthquake
* Landslide
* Medical
* Trapped
* Fire
* Other

and a short description afterward.

### Nearby SOS

A citizen phone may also operate as a relay.

When another person's SOS is received:

* It is stored locally.
* It is automatically prepared for forwarding.
* The user may receive a notification.
* The user may optionally provide an observation.

Relay users cannot modify the victim's original report.

### Map

The citizen map can display:

* The user's location
* Nearby emergency locations
* Relevant SOS information

Offline map support is part of the planned MVP.

---

## Rescue Mode

Sahara will use a single Android application with different operational roles.

After the Citizen MVP is stable, **Rescue Mode** will be implemented.

Rescue Mode will provide:

* Active SOS list
* Emergency type
* Priority
* Coordinates
* Timestamp
* Hop count
* Victim description
* Offline map
* New / Responding / Resolved status
* Gateway communication with the Sahara Command Center

---

## Sahara Command Center

The rescue-side laptop system will provide a dashboard for emergency teams.

The planned command-center architecture is:

```text
Rescue / Gateway Device
        │
        ▼
Gateway
        │
        ▼
FastAPI
        │
        ├── SQLite
        │
        └── WebSocket
                │
                ▼
        React Dashboard
```

Planned technologies:

* **Python**
* **FastAPI**
* **SQLite**
* **WebSockets**
* **React**
* **TypeScript**
* **Vite**

The command center will be developed only after the offline Android communication flow is working reliably.

---

## Android Technology Stack

| Component            | Technology                      |
| -------------------- | ------------------------------- |
| Language             | Kotlin                          |
| IDE                  | Android Studio                  |
| UI                   | Jetpack Compose                 |
| Design System        | Material 3                      |
| Architecture         | MVVM / Unidirectional Data Flow |
| Nearby Communication | Bluetooth Low Energy            |
| Local Database       | Room / SQLite                   |
| Location             | Android Location APIs           |
| Async Operations     | Kotlin Coroutines / Flow        |
| Packet Format        | Structured lightweight packets  |
| Map                  | Offline-capable map solution    |

---

