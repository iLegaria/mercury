import { api, BASE_URL } from './api';
import type { RAGResponse, ChunkSearchResult } from '@/types';

interface StreamCallbacks {
  onToken: (token: string) => void;
  onSources: (chunks: ChunkSearchResult[]) => void;
  onDone: () => void;
  onError: (err: Error) => void;
}

export const searchApi = {
  ask: (userId: string, question: string, collectionId?: string) =>
    api.post<RAGResponse>('/api/v1/search/ask', { userId, question, collectionId: collectionId || null }),

  search: (userId: string, query: string, limit = 5, collectionId?: string) =>
    api.post<ChunkSearchResult[]>('/api/v1/search', { userId, query, limit, collectionId: collectionId || null }),

  askStream: (
    userId: string,
    question: string,
    collectionId: string | undefined,
    callbacks: StreamCallbacks,
  ): (() => void) => {
    const controller = new AbortController();

    (async () => {
      try {
        const res = await fetch(`${BASE_URL}/api/v1/search/ask`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'Accept': 'text/event-stream' },
          body: JSON.stringify({ userId, question, collectionId: collectionId || null }),
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
              if (event.type === 'token') callbacks.onToken(event.text);
              else if (event.type === 'sources') callbacks.onSources(event.chunks);
              else if (event.type === 'done') callbacks.onDone();
            } catch { /* malformed event — skip */ }
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
