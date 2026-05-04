import { api } from './api';
import type { DocumentStudyStats, FlashcardDeck, QuizSession } from '@/types';

export const studyApi = {
  generateFlashcards: (
    documentId: string,
    userId: string,
    mode: 'EXTRACT' | 'GENERATE',
    numCards = 10,
  ) =>
    api.post<FlashcardDeck>(`/api/v1/documents/${documentId}/study/flashcards`, {
      userId,
      mode,
      numCards,
    }),

  startQuiz: (
    documentId: string,
    userId: string,
    generationMode: 'EXTRACT' | 'GENERATE',
    numQuestions = 5,
  ) =>
    api.post<QuizSession>(`/api/v1/documents/${documentId}/study/quiz`, {
      userId,
      generationMode,
      numQuestions,
    }),

  getStats: (documentId: string, userId: string) =>
    api.get<DocumentStudyStats>(
      `/api/v1/documents/${documentId}/study/stats?userId=${userId}`,
    ),
};
