# Swapify Barter App

Welcome to the Swapify Barter App – your premier platform for modern bartering and trading. Step into a realm of smart exchanges, tailor-made for individuals who value the art of swapping goods and services. Discover a dynamic marketplace where you can redefine the way you acquire what you desire. Elevate your personal interactions and create meaningful connections through our meticulously designed bartering platform. Whether you're seeking unique items or offering your specialized skills, the Swapify Barter App is your gateway to a new world of exchange.

## Key Features
- Sign in With Google
- Firebase Authentication
- Firebase Cloud Firestore
- Firebase Realtime Database
- Firebase Storage

## Integrating Firebase Services for Android Studio Project
Follow these steps to seamlessly integrate Firebase services, including Google Authentication, into your Android Studio project.


1. **Create a Firebase Project:**

   If you haven't already, go to the [Firebase Console](https://console.firebase.google.com/) and create a new project.

2. **Add your Android App to the Firebase Project:**

   - In the Firebase Console, click on "Add app" and select Android.
   - Enter your app's **Package name** (usually in the format `com.example.myapp`) and click **Register app**.
   - Paste the SHA-1 fingerprint from your project in the appropriate field.
   - Download the `google-services.json` configuration file.

3. **Add `google-services.json` File:**

   - Copy the downloaded `google-services.json` file to your `app` module's root directory.

4. **Enable Google Sign-In**

   - In the Firebase Console, navigate to the "Authentication" section.
   - Click on the "Sign-in method" tab.
   - Enable the "Google" sign-in provider and save your changes.


5. **Enable Firestore Database:**

   - In the Firebase Console, navigate to the "Firestore" section.
   - Set up your database rules and configure the database according to your app's needs.

    ```firebase
    rules_version = '2';
    service cloud.firestore {
      match /databases/{database}/documents {
        match /{document=**} {
          allow read, write: if request.auth != null;
        }
      }
    }
    ```

6. **Enable Real-Time Database:**

   - In the Firebase Console, navigate to the "Database" section.
   - Set up your database rules and configure the database according to your app's needs.

    ```json
    {
      "rules": {
        ".read": true,
        ".write": true
      }
    }
    ```

7. **Enable Storage:**

   - In the Firebase Console, navigate to the "Storage" section.
   - Set up your database rules and configure the database according to your app's needs.

    ```firebase
    rules_version = '2';
    service firebase.storage {
      match /b/{bucket}/o {
        match /{allPaths=**} {
          allow read, write: if request.auth != null;
        }
      }
    }
    ```

---

Remember to follow these steps carefully to ensure a smooth integration of your Android app with Firebase services.