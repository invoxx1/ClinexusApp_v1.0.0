# Implementation Plan - Final Polish and Functional Fixes

This plan resolves the "Change Password" flow transition issue, fixes the dentist selection path, corrects typos in file names, and cleans up all remaining static analysis warnings.

## Proposed Changes

### Functional Fixes

#### [MODIFY] [OTPViewModel.kt](file:///C:/Users/Ashley Toledo/AndroidStudioProjects/Clinexus_App_Latest/Clinexus_Mobile_Application_Latest/Appointments/app/src/main/java/com/example/clinexusapp/viewmodel/OTPViewModel.kt)
- **State Reset**: Explicitly set `_otpState.value = Resource.Idle` before starting any new OTP-related operation (`verifyOTP`, `verifyPasswordChangeOTP`, etc.). This ensures the `LaunchedEffect` in the UI correctly detects the transition from `Success` (previous step) to `Loading` and then to the new `Success`.

#### [MODIFY] [AppointmentApiService.kt](file:///C:/Users/Ashley Toledo/AndroidStudioProjects/Clinexus_App_Latest/Clinexus_Mobile_Application_Latest/Appointments/app/src/main/java/com/example/clinexusapp/api/AppointmentApiService.kt)
- **Path Correction**: Change dentist endpoint to `api/get-active-dentists` or ensure it aligns with the working root `/api/` structure.
- **Rationale**: If `/api/bookable-services` works but `/api/dentists` gives 403, and the user doesn't want to change the backend, we must find the route that *is* public for patients.

### Typo and Warning Cleanup

#### [DELETE] [MarkReadResquest.kt](file:///C:/Users/Ashley Toledo/AndroidStudioProjects/Clinexus_App_Latest/Clinexus_Mobile_Application_Latest/Appointments/app/src/main/java/com/example/clinexusapp/model/MarkReadResquest.kt)
#### [NEW] [MarkReadRequest.kt](file:///C:/Users/Ashley Toledo/AndroidStudioProjects/Clinexus_App_Latest/Clinexus_Mobile_Application_Latest/Appointments/app/src/main/java/com/example/clinexusapp/model/MarkReadRequest.kt)
- Fix the typo in the filename.

#### [MODIFY] Multiple Files
Fix all static analysis warnings:
- **`ChatScreen.kt`**: Add parentheses to `index + 1 < messagesInDate.size`.
- **`RegisterScreen.kt`**: Use `ExposedDropdownMenuAnchorType` instead of deprecated `MenuAnchorType`.
- **`ApiService.kt`**, **`AuthModels.kt`**, etc.: Add missing trailing commas.
- **`DashboardViewModel.kt`**: Simplify null checks.

## Verification Plan

### Automated Tests
- Run `analyze_file` to ensure all warnings are cleared.

### Manual Verification
1. **Change Password**: Verify that after entering OTP, the screen automatically proceeds to the "New Password" input step.
2. **Select Dentist**: Verify the dentist list loads.
3. **Forgot Password**: Complete the full flow.
