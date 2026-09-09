# Fix Chat Blinking and Alignment

Resolve the persistent image blinking during polling and fix the alignment of sent attachments so they appear on the right side of the screen.

## User Review Required

> [!IMPORTANT]
> I will modify the `ChatViewModel` to prevent the UI from refreshing every 3 seconds if the message data is identical. This is the most likely cause of the "blinking".

> [!NOTE]
> I will fix the attachment alignment by ensuring the container size is correctly constrained, allowing the parent layout to pull sent messages to the right side of the screen as intended.

## Proposed Changes

### ViewModel & Data Stability

#### [MODIFY] [ChatViewModel.kt](file:///C:/Users/Ashley Toledo/AndroidStudioProjects/Clinexus_App_Latest/Clinexus_Mobile_Application_Latest/Appointments/app/src/main/java/com/example/clinexusapp/viewmodel/ChatViewModel.kt)
- Update `startPollingMessages` to compare the new state with the current state.
- Only update `_conversationMessagesState.value` if the data has actually changed. This prevents the entire message list from recomposing every 3 seconds.

### UI Components

#### [MODIFY] [ChatScreen.kt](file:///C:/Users/Ashley Toledo/AndroidStudioProjects/Clinexus_App_Latest/Clinexus_Mobile_Application_Latest/Appointments/app/src/main/java/com/example/clinexusapp/ui/screens/chat/ChatScreen.kt)
- **Attachment Alignment**:
    - Update `AttachmentBox` to strictly use `wrapContentSize()` for standalone images.
    - Ensure the `Row` inside `AttachmentBox` doesn't inadvertently fill the width, which was pushing it to the left.
- **Blinking Fix (UI Side)**:
    - Use `SubcomposeAsyncImage` which handles loading states more smoothly and can prevent flickering by keeping the previous success result visible while checking the cache.
    - Double-check list keys in `LazyColumn` to ensure they are unique and stable.

## Verification Plan

### Automated Tests
- Build the project to ensure no compilation errors.

### Manual Verification
- Deploy to a device.
- Send an image and verify it aligns to the **right**.
- Observe the chat for several minutes to ensure images remain stable and do not blink during the 3-second poll cycles.
