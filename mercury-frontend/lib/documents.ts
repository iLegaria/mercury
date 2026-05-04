import { api } from './api';
import type { Document, Page } from '@/types';

export const documentsApi = {
  list: async (userId: string, collectionId?: string): Promise<Document[]> => {
    const qs = collectionId ? `?collectionId=${collectionId}` : '';
    const page = await api.get<Page<Document>>(`/api/v1/documents/user/${userId}${qs}`);
    return page.content;
  },
  get: (id: string) =>
    api.get<Document>(`/api/v1/documents/${id}`),
  upload: (userId: string, title: string, file: File, collectionId?: string) => {
    const form = new FormData();
    form.append('userId', userId);
    form.append('title', title);
    form.append('file', file);
    if (collectionId) form.append('collectionId', collectionId);
    return api.postForm<Document>('/api/v1/documents/upload', form);
  },
  delete: (id: string, userId: string) =>
    api.delete<void>(`/api/v1/documents/${id}?userId=${userId}`),
  assignCollection: (id: string, userId: string, collectionId: string | null) =>
    api.patch<Document>(`/api/v1/documents/${id}/collection`, { userId, collectionId }),
  updateTitle: (id: string, userId: string, title: string) =>
    api.patch<Document>(`/api/v1/documents/${id}/title`, { userId, title }),
  updateContent: (id: string, userId: string, content: string) =>
    api.patch<Document>(`/api/v1/documents/${id}/content`, { userId, content }),
  createFromText: (userId: string, title: string, content: string, collectionId?: string) =>
    api.post<Document>('/api/v1/documents/create-text', { userId, title, content, collectionId }),
};
