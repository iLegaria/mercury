import { api } from './api';
import type { DocumentChunk } from '@/types';

export const chunksApi = {
  list: (documentId: string, userId: string) =>
    api.get<DocumentChunk[]>(`/api/v1/documents/${documentId}/chunks?userId=${userId}`),
  update: (chunkId: string, userId: string, content: string) =>
    api.patch<DocumentChunk>(`/api/v1/chunks/${chunkId}`, { userId, content }),
};
