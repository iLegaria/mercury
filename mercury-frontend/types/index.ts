export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface Collection {
  id: string;
  userId: string;
  name: string;
  description?: string;
  documentCount: number;
  createdAt: string;
}

export type DocumentStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
export type DocumentSourceType = 'PDF' | 'DOCX' | 'TXT' | 'HTML' | 'MARKDOWN' | 'OTHER';

export interface Document {
  id: string;
  userId: string;
  title: string;
  sourceType: DocumentSourceType;
  status: DocumentStatus;
  createdAt: string;
  collectionId?: string;
  collectionName?: string;
  extractedText?: string;
}

export interface DocumentChunk {
  chunkId: string;
  chunkIndex: number;
  content: string;
  tokenCount: number | null;
}

export interface ChunkSearchResult {
  chunkId: string;
  documentId: string;
  documentTitle?: string;
  content: string;
  chunkIndex: number;
  similarity: number;
}

export interface RAGResponse {
  answer: string;
  chunks: ChunkSearchResult[];
}

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  sources?: ChunkSearchResult[];
  timestamp: string;
}

export interface QuizQuestion {
  questionId: string;
  questionText: string;
  questionIndex: number;
  totalQuestions: number;
}

export interface QuizSession {
  sessionId: string;
  totalQuestions: number;
  correctAnswers: number;
  status: 'ACTIVE' | 'COMPLETED';
  mode: string;
  currentQuestion?: QuizQuestion;
}

export type QuizVerdict = 'CORRECT' | 'PARTIALLY_CORRECT' | 'INCORRECT';

export interface AnswerFeedback {
  isCorrect: boolean;
  verdict: QuizVerdict;
  feedback: string;
  sessionComplete: boolean;
  finalScore?: number;
  totalQuestions?: number;
  nextQuestion?: QuizQuestion;
}

export interface FlashcardDeck {
  id: string;
  userId: string;
  name: string;
  description?: string;
  cardCount: number;
  dueCount: number;
  createdAt: string;
  whatsappReminderEnabled: boolean;
}

export interface FlashcardCard {
  id: string;
  deckId: string;
  question: string;
  answer: string;
  cardIndex: number;
  nextReviewAt: string;
  intervalDays: number;
  repetitions: number;
  easeFactor: number;
}

export interface ReviewResult {
  nextReviewAt: string;
  intervalDays: number;
}

export interface Snippet {
  id: string;
  userId: string;
  content: string;
  sourceUrl?: string;
  sourceTitle?: string;
  createdAt: string;
}

export interface DocumentStudyStats {
  chunkCount: number;
  quizAttempts: number;
  completedQuizzes: number;
  avgScore: number | null;
  deckCount: number;
  totalCards: number;
}
