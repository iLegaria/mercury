import { api } from './api';

export interface UserResponse {
  id: string;
  email: string;
  name: string;
  createdAt: string;
}

export const usersApi = {
  create: (email: string, name: string) =>
    api.post<UserResponse>('/api/v1/users', { email, name }),
  get: (id: string) =>
    api.get<UserResponse>(`/api/v1/users/${id}`),
};
