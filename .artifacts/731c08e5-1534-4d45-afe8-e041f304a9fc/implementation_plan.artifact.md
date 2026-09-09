# Implementation Plan - Final Fix for Services (403 & Categories)

This plan resolves the 403 Forbidden error and restores service categories by aligning the app with the patient-authorized `bookable-services` endpoint and removing redundant logic.

## Proposed Changes

### [Component Name] API Layer

#### [MODIFY] [AppointmentApiService.kt](file:///C:/Users/Ashley Toledo/AndroidStudioProjects/Clinexus_App_Latest/BITS/app/src/main/java/com/example/clinexusapp/api/AppointmentApiService.kt)
Revert the services endpoint to the patient-authorized version and remove the restricted categories endpoint.
- **Services**: `@GET("api/bookable-services")`
- **Categories**: Remove `getServiceCategories` (Unnecessary and likely restricted).

### [Component Name] ViewModel Layer

#### [MODIFY] [BookingViewModel.kt](file:///C:/Users/Ashley Toledo/AndroidStudioProjects/Clinexus_App_Latest/BITS/app/src/main/java/com/example/clinexusapp/viewmodel/BookingViewModel.kt)
- **Simplify Fetching**: Fetch only dentists and bookable services.
- **Remove Filters**: Delete the `isBookableOnline == 1` local check. Since the server pre-filters this route, the app will now correctly show the results.
- **State Cleanup**: Revert `servicesState` to a standard `StateFlow` containing the list from the repository.

### [Component Name] Model Layer

#### [MODIFY] [AppointmentModels.kt](file:///C:/Users/Ashley Toledo/AndroidStudioProjects/Clinexus_App_Latest/BITS/app/src/main/java/com/example/clinexusapp/model/AppointmentModels.kt)
Ensure `BookableServiceDTO` correctly maps `service_category_name` from your backend SQL.

## Verification Plan

### Automated Tests
- Build project.

### Manual Verification
- Open Booking Screen.
- Verify Services load without 403 (No "Forbidden" error).
- Verify each service shows its category badge (e.g., "GENERAL") and price.
- Verify timeslots appear after selection.
