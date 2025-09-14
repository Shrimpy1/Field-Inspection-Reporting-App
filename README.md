[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/zR1IQ8nU)

Field Inspection Reporting App - App Submission
==================================

## STUDENT DETAILS

- Student ID: S3979077
- Student Name: Nguyen Dung Tri

- Student ID: S3978724
- Student Name: Nguyen Ich Kiet

## DEMO VIDEO LINK

- Link: https://www.youtube.com/watch?v=e1BWWTzdVnI

## APP FUNCTIONALITY

This application is a modern, offline-first mobile solution for field technicians at Phuong Hai JSC
to conduct and manage equipment and site inspections.

Its core features include:

- **User Authentication** using Firebase Auth, allowing technicians to sign up, sign in, and manage
  their sessions securely.

- **Offline-First Inspection Reporting**: CRUD operations available entirely offline by using Room
  local database.

- **Automated Background Syncing**: use WorkManager to automatically push all local and pull all
  remote when connection is stable.

- **Sync State Indicator**: warns user upon losing/regaining connection. Inspections have remote or
  local indicators

- **Media Attachment & Cloud Storage**: users can attach multiple images to each report. These
  images
  are uploaded to Supabase Storage.

- **GPS and Timestamp Validation**: every report is automatically tagged with the precise GPS
  coordinates and a timestamp.

- **Map View of Inspection Sites**: includes a feature to display all inspection sites visually on
  an integrated Google Map.

- **Logs and Reports**: all inspecting actions are recorded and available to be exported as CSV.

- **User-friendly UI**: comprehensive navigation navbar and buttons. Filter options and search bar
  on Inspection list.

- **User Preferences**: use Preferences DataStore to save user-specific settings or
  session
  information locally on the device.

- **Comprehensive Testing**: The project includes a robust testing suite with unit tests (using
  MockK
  for mocking) and asynchronous flow testing (using Turbine), ensuring the reliability of the core
  business logic.

Additional features:

- **Image Quality Checker**: upon uploading images, the app automatically check its brightness and
  clarity to warn user about any flaws.

- **One-tap share inspection**: user can share details of inspections as PDF file and send via
  Email, Messenger, etc.

## TECHNOLOGY USED

- Language: Kotlin

- User Interface (UI): Jetpack Compose, including Material 3 and extended Material Icons.

- Architecture: Clean MVVM pattern using Hilt for dependencies injection.

- Offline Storage: Room for the local database and Preferences DataStore for key-value storage.

- Background Processing: Hilt WorkManager to handle background syncing tasks.

- Networking & Backend:

    - Authentication: Firebase Authentication.

    - Data & File Storage: FireStore for data documents and Supabase Storage for images.

- Location & Maps: Google Play Services for location and Maps Compose SDK for displaying maps.

- Permissions: Accompanist Permissions library.

- Image Loading: Coil for asynchronously loading and displaying images.

- Navigation: Jetpack Navigation for Compose.

- Testing: JUnit, MockK (for mocking objects), and Turbine (for testing Kotlin Flows).