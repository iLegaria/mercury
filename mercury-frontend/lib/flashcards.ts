import { api } from './api';
import type { FlashcardDeck, FlashcardCard, ReviewResult, Page } from '@/types';

export const flashcardsApi = {
  listDecks: async (userId: string): Promise<FlashcardDeck[]> => {
    const page = await api.get<Page<FlashcardDeck>>(`/api/v1/flashcards/decks/user/${userId}`);
    return page.content;
  },
  createDeck: (userId: string, name: string, description?: string) =>
    api.post<FlashcardDeck>('/api/v1/flashcards/decks', { userId, name, description }),
  deleteDeck: (deckId: string, userId: string) =>
    api.delete<void>(`/api/v1/flashcards/decks/${deckId}?userId=${userId}`),
  getCards: (deckId: string, userId: string) =>
    api.get<FlashcardCard[]>(`/api/v1/flashcards/decks/${deckId}/cards?userId=${userId}`),
  getDueCards: (deckId: string, userId: string) =>
    api.get<FlashcardCard[]>(`/api/v1/flashcards/decks/${deckId}/cards/due?userId=${userId}`),
  reviewCard: (cardId: string, userId: string, quality: number) =>
    api.post<ReviewResult>(`/api/v1/flashcards/cards/${cardId}/review`, { userId, quality }),
  importCsv: (deckId: string, userId: string, file: File) => {
    const form = new FormData();
    form.append('userId', userId);
    form.append('file', file);
    return api.postForm<void>(`/api/v1/flashcards/decks/${deckId}/import`, form);
  },
  setWhatsappReminder: (deckId: string, userId: string, enabled: boolean) =>
    api.patch<void>(`/api/v1/flashcards/decks/${deckId}/whatsapp-reminder`, { userId, enabled }),
  addCard: (deckId: string, userId: string, question: string, answer: string) =>
    api.post<FlashcardCard>('/api/v1/flashcards/cards', { deckId, userId, question, answer }),
  updateCard: (cardId: string, userId: string, question: string, answer: string) =>
    api.patch<FlashcardCard>(`/api/v1/flashcards/cards/${cardId}`, { userId, question, answer }),
  deleteCard: (cardId: string, userId: string) =>
    api.delete<void>(`/api/v1/flashcards/cards/${cardId}?userId=${userId}`),
};
