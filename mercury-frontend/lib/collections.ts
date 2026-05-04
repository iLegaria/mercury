import { api } from './api';
import type { Collection, Page } from '@/types';

export const collectionsApi = {
  list: async (userId: string): Promise<Collection[]> => {
    const page = await api.get<Page<Collection>>(`/api/v1/collections/user/${userId}`);
    return page.content;
  },
  create: (userId: string, name: string, description?: string) =>
    api.post<Collection>('/api/v1/collections', { userId, name, description }),
  delete: (id: string, userId: string) =>
    api.delete<void>(`/api/v1/collections/${id}?userId=${userId}`),
};
