export type UploadedDocument = {
  documentId: string;
  fileName: string;
  status: string;
};

export async function uploadDocument(file: File): Promise<UploadedDocument> {
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch("/api/documents", {
    method: "POST",
    body: formData
  });

  if (!response.ok) {
    throw new Error("Document upload failed");
  }

  return response.json() as Promise<UploadedDocument>;
}
