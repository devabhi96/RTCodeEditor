export const DOCUMENT_ID_PATTERN = /^[A-Za-z0-9_-]{1,128}$/;

export const isValidDocumentId = (documentId) => (
  typeof documentId === 'string' && DOCUMENT_ID_PATTERN.test(documentId)
);
