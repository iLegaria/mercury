import { api, BASE_URL } from './api';
import type { QuizSession, AnswerFeedback, QuizVerdict } from '@/types';

interface StreamCallbacks {
  onToken: (token: string) => void;
  onComplete: (feedback: AnswerFeedback) => void;
  onError: (err: Error) => void;
}

export const quizApi = {
  start: (userId: string, numQuestions: number, mode?: string, collectionId?: string) =>
    api.post<QuizSession>('/api/v1/quiz/start', { userId, numQuestions, collectionId: collectionId || null }),
  getSession: (sessionId: string) =>
    api.get<QuizSession>(`/api/v1/quiz/sessions/${sessionId}`),
  submitAnswer: (sessionId: string, userId: string, answer: string) =>
    api.post<AnswerFeedback>(`/api/v1/quiz/sessions/${sessionId}/answer`, { userId, answer }),

  submitAnswerStream: (
    sessionId: string,
    userId: string,
    answer: string,
    callbacks: StreamCallbacks,
  ): (() => void) => {
    const controller = new AbortController();

    (async () => {
      try {
        const res = await fetch(`${BASE_URL}/api/v1/quiz/sessions/${sessionId}/answer`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'Accept': 'text/event-stream' },
          body: JSON.stringify({ userId, answer }),
          signal: controller.signal,
        });

        if (!res.ok || !res.body) {
          callbacks.onError(new Error(`HTTP ${res.status}`));
          return;
        }

        const reader = res.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split('\n');
          buffer = lines.pop() ?? '';

          for (const line of lines) {
            if (!line.startsWith('data:')) continue;
            const json = line.slice(5).trim();
            if (!json) continue;
            try {
              const event = JSON.parse(json);
              if (event.type === 'token') {
                callbacks.onToken(event.text as string);
              } else if (event.type === 'complete') {
                callbacks.onComplete({
                  isCorrect: event.isCorrect as boolean,
                  verdict: event.verdict as QuizVerdict,
                  feedback: '',   // already streamed — FeedbackCard won't re-render it
                  sessionComplete: event.sessionComplete as boolean,
                  finalScore: event.finalScore ?? undefined,
                  totalQuestions: event.totalQuestions ?? undefined,
                  nextQuestion: event.nextQuestion ?? undefined,
                });
              }
            } catch { /* malformed event */ }
          }
        }
      } catch (err) {
        if (!controller.signal.aborted) {
          callbacks.onError(err instanceof Error ? err : new Error(String(err)));
        }
      }
    })();

    return () => controller.abort();
  },
};
