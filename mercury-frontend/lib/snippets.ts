import { api } from './api';
import type { Snippet, Document, Page } from '@/types';

export const snippetsApi = {
  list: async (userId: string): Promise<Snippet[]> => {
    const page = await api.get<Page<Snippet>>(`/api/v1/snippets/user/${userId}`);
    return page.content;
  },

  create: (userId: string, content: string, sourceUrl?: string, sourceTitle?: string) =>
    api.post<Snippet>('/api/v1/snippets', { userId, content, sourceUrl, sourceTitle }),

  delete: (id: string, userId: string) =>
    api.delete<void>(`/api/v1/snippets/${id}?userId=${userId}`),

  compile: (userId: string, snippetIds: string[], documentTitle: string) =>
    api.post<Document>('/api/v1/snippets/compile', { userId, snippetIds, documentTitle }),

  appendToDocument: (userId: string, snippetIds: string[], documentId: string) =>
    api.post<{ count: number }>('/api/v1/snippets/append', { userId, snippetIds, documentId }),

  createFlashcards: (userId: string, snippetIds: string[], deckId: string) =>
    api.post<{ count: number }>('/api/v1/snippets/flashcards', { userId, snippetIds, deckId }),
};
