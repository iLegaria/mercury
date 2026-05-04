'use client';

import { useState, useEffect } from 'react';

const KEY = 'mercury_user_id';

function getOrCreateUserId(): string {
  if (typeof window === 'undefined') return '';
  let id = localStorage.getItem(KEY);
  if (!id) {
    id = crypto.randomUUID();
    localStorage.setItem(KEY, id);
  }
  return id;
}

export function useLocalUser() {
  const [userId, setUserId] = useState('');

  useEffect(() => {
    setUserId(getOrCreateUserId());
  }, []);

  return { userId, ready: userId !== '' };
}
