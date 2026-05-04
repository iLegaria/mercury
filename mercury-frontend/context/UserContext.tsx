'use client';

import { createContext, useContext } from 'react';

export interface UserContextValue {
  userId: string;
  userName: string;
  logout: () => void;
}

export const UserContext = createContext<UserContextValue>({
  userId: '',
  userName: '',
  logout: () => {},
});

export function useUser() {
  return useContext(UserContext);
}
