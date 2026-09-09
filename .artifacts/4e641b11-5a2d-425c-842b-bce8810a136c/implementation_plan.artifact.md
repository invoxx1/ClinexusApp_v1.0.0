# Implementation Plan - Profile Picture Options

Add functionality to the profile picture in `ProfileScreen` so that clicking it provides options to "View Profile" or "Edit Profile".

## User Review Required

> [!IMPORTANT]
> I will implement a `ModalBottomSheet` that appears when the profile picture is clicked.
> - **View Profile**: I'll assume this means viewing the profile picture in a full-screen/large dialog.
> - **Edit Profile**: This will navigate to the `PersonalInformationScreen` where the user can update their full profile details.

## Proposed Changes

### [Profile Screen]

#### [MODIFY] [ProfileScreen.kt](file:///C:/Users/Ashley Toledo/AndroidStudioProjects/Clinexus_App_Latest/Clinexus_Mobile_Application_Latest/Appointments/app/src/main/java/com/example/clinexusapp/ui/screens/profile/ProfileScreen.kt)
- Add state variable `showOptions` to control the visibility of the options menu.
- Add state variable `showFullImage` to control the visibility of the full-screen image dialog.
- Add a `ModalBottomSheet` (or `DropdownMenu`) to show the "View Profile" and "Edit Profile" options.
- Wrap the profile picture `Surface` in a `Box` with a `clickable` modifier.
- Implement a `Dialog` to show the profile picture in a larger size when "View Profile" is selected.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to ensure no build regressions.

### Manual Verification
1. Navigate to the Profile Screen.
2. Click on the profile picture.
3. Verify that a bottom sheet or menu appears with "View Profile" and "Edit Profile".
4. Select "View Profile" and verify a large image dialog appears.
5. Select "Edit Profile" and verify it navigates to the Personal Information screen.
