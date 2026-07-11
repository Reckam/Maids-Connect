# MaidsConnect — Smart Matching Application for Domestic Workers and Employers

An offline-first, highly reliable, and securely verified mobile marketplace connecting households with professional domestic workers (housekeepers, cooks, caregivers, and infant nurses) across Uganda.


## Problem & Solution

### The Problem
Finding trustworthy, reliable, and vetted domestic help in Uganda remains a highly fragmented, informal, and risky endeavor. 
* **Lack of Vetting & Verification:** Standard hiring processes rely on word-of-mouth recommendations, leading to high security risks regarding National Identification Numbers (NIN) and local village clearances (LC1 letters).
* **Communication & Matching Gaps:** Employers struggle to find domestic workers with specialized skills (e.g., infant nursing, elderly care, specific local languages) matching their household needs.
* **Connectivity & Trust Barriers:** Due to intermittent internet connections, fully online applications are slow or completely fail, and physical transaction tracking is prone to error or disputes.

### The Solution
**MaidsConnect** bridges this trust and accessibility gap by digitalizing and securing the domestic worker hiring lifecycle with a powerful Android Jetpack Compose application tailored for the East African ecosystem.
* **Dual-Engine Syncing Architecture:** Powered by a high-speed local Room Database and a real-time Cloud Firestore integration. Users can search and make hiring requests completely offline, with immediate, bidirectional synchronization once a connection is detected.
* **Verified Vetting System:** Profiles require official National Identification Numbers (NIN) and sub-county/village registration verification before being designated as "Approved" by system security administrators.
* **Integrated Booking & Invoicing:** Includes structural transparent booking terms, automatic cost estimation in Ugandan Shillings (UGX), built-in placement fee tracking, and dispute management support.
* **Granular District Mapping:** Built-in geographic localization targeting major districts (Kampala, Wakiso, Mbarara, etc.), counties, subcounties, and villages to narrow down localized domestic workers easily.

---

## Setup Instructions

This is a modern Android application built using **Kotlin** and **Jetpack Compose**. Follow these steps to set up, configure, and run the project locally.

### Prerequisites
Before starting, ensure you have the following installed on your machine:
* **Android Studio** (Koala | 2024.1.1 or newer recommended)
* **Java Development Kit (JDK) 17** (bundled automatically with modern Android Studio versions)
* **Git** (for version control and pushing)

---

### Step 1: Clone the Repository
Clone the codebase to your local machine:
```bash
git clone https://github.com/Reckam/Maids-Connect.git
cd Maids-Connect
```

---

### Step 2: Open in Android Studio
1. Launch **Android Studio**.
2. Click on **File > Open** (or **Open an Existing Project** on the welcome screen).
3. Select the root folder (`Maids-Connect`) and click **OK**.
4. Allow Android Studio to complete importing and synchronizing the Gradle files.

---

### Step 3: Build and Run
1. Connect an Android Device via USB (with **USB Debugging** enabled in Developer Options) or start an Android Virtual Device (AVD) Emulator in Android Studio.
2. Ensure the active build configuration is set to **app** and the active build variant is `debug`.
3. Click the green **Run** button (or press `Shift + F10`) to compile, install, and launch the application on your device or emulator.

