# Danielle Perfume

<div align="justify"> Introducing my sophisticated men's perfume app, the ultimate companion for distinguished gentlemen who appreciate the art of fragrance. Explore a world of captivating scents, curated exclusively for the modern man. Elevate your personal style and leave a lasting impression with my meticulously crafted collection of premium men's perfumes. Discover the perfect scent for every occasion, from alluring and seductive to fresh and invigorating. </div>

<br>

[![Perfume Banner](https://nichestory.eu/wp-content/uploads/2023/06/zousz-banner-01.jpg)]()

## Key Features
- Firebase Authentication
- Signin With Google

---

### Follow these steps to connect your Android Studio project to Google Authentication using Firebase.

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

---   
Remember to follow these steps carefully to ensure a smooth integration of your Android app with Firebase services.