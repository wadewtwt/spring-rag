# Document Upload Design

## Goal

Add a document upload area to the existing chat page so users can upload a file into the knowledge base and immediately continue asking questions on the same screen.

## Scope

- Keep the app as a single page.
- Add upload UI to the existing chat panel.
- Call the existing backend endpoint `POST /api/documents`.
- Show upload progress, success feedback, failure feedback, and a list of uploaded file names for the current browser session.
- Do not add document management, delete, or list-from-server features.

## UI Placement

The upload area lives at the top of the left chat panel, above the message list and composer. This keeps the primary flow on one side: upload first, then ask questions.

## Components

### ChatWindow

`frontend/src/components/ChatWindow.tsx` remains the page-level component. It gains:

- selected file state
- upload pending state
- upload success message
- upload error message
- uploaded file name list

### Document Service

Create `frontend/src/services/document.ts` to isolate the multipart upload request from UI logic.

## Data Flow

1. User selects a file.
2. User clicks upload.
3. Frontend sends `multipart/form-data` to `/api/documents` with field name `file`.
4. On success:
   - clear any upload error
   - show a success message with the file name
   - append the file name to the uploaded list
   - clear the selected file
5. On failure:
   - show an error message
   - keep the rest of the chat UI untouched

## Error Handling

- Upload button stays disabled when no file is selected or upload is already in progress.
- Network or backend failure shows a user-facing error message.
- Upload failures do not clear chat history or source references.

## Testing

Frontend tests should cover:

1. Upload controls render on the chat page.
2. Successful upload shows a success message and adds the file to the uploaded list.
3. Failed upload shows an error message and leaves chat UI available.
