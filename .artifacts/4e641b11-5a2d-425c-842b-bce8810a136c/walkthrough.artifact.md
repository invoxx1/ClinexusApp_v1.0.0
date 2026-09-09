# Walkthrough - Profile Picture Options

I have implemented a context menu for the profile picture in the `ProfileScreen`, allowing users to either view their profile picture in full size or navigate to the edit profile screen.

## Changes Made

### Profile Screen
- **[ProfileScreen.kt](file:///C:/Users/Ashley Toledo/AndroidStudioProjects/Clinexus_App_Latest/Clinexus_Mobile_Application_Latest/Appointments/app/src/main/java/com/example/clinexusapp/ui/screens/profile/ProfileScreen.kt)**:
    - Made the profile picture clickable.
    - Added a `ModalBottomSheet` that appears when the profile picture is clicked, showing "View Profile Picture" and "Edit Profile" options.
    - Added a `Dialog` to display the profile picture in full size when "View Profile Picture" is selected.
    - Linked "Edit Profile" to the existing personal information navigation.
    - Implemented a new `ProfileOptionItem` composable for consistent styling in the bottom sheet.

## Verification Results

### Automated Tests
- Executed `./gradlew app:assembleDebug`.
- **Status**: SUCCESS. The project builds without errors.

### Manual Verification Path
1. Open the **Profile** screen.
2. Tap on the **Profile Picture**.
3. A bottom sheet should slide up with two options:
    - **View Profile Picture**: Opens a full-screen view of the image.
    - **Edit Profile**: Navigates to the Personal Information screen.
