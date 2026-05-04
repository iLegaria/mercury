'use client';

import { useEffect, useRef, useState, useCallback } from 'react';
import { Send } from 'lucide-react';
import { useUser } from '@/context/UserContext';
import { collectionsApi } from '@/lib/collections';
import { searchApi } from '@/lib/search';
import ChatMessage from '@/components/ask/ChatMessage';
import type { ChatMessage as ChatMsg, Collection, ChunkSearchResult } from '@/types';

function ThinkingIndicator() {
  return (
    <div style={{ display: 'flex', justifyContent: 'flex-start', marginBottom: '16px' }}>
      <div style={{ maxWidth: '80%' }}>
        <div style={{ fontSize: '9px', letterSpacing: '0.18em', textTransform: 'uppercase', color: 'var(--mercury-accent)', marginBottom: '6px' }}>
          Mercury
        </div>
        <div style={{
          background: 'var(--card)', border: '1px solid #121420', borderLeft: '2px solid var(--mercury-accent)',
          padding: '14px 18px', borderRadius: '0 2px 2px 2px',
          display: 'flex', alignItems: 'center', gap: '10px',
        }}>
          <div style={{ display: 'flex', gap: '4px', alignItems: 'center' }}>
            <span className="thinking-dot" />
            <span className="thinking-dot" />
            <span className="thinking-dot" />
          </div>
          <span style={{ fontSize: '11px', color: 'var(--text-muted-col)', letterSpacing: '0.04em' }}>
            Mercury is thinking...
          </span>
        </div>
      </div>
    </div>
  );
}

function EmptyState() {
  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '40px', textAlign: 'center' }}>
      <div style={{ fontSize: '9px', letterSpacing: '0.24em', textTransform: 'uppercase', color: 'var(--mercury-accent)', marginBottom: '16px' }}>
        Retrieval-Augmented Generation
      </div>
      <h2 style={{ fontFamily: 'var(--font-cinzel), Cinzel, serif', fontSize: '22px', fontWeight: 600, letterSpacing: '0.04em', color: 'var(--foreground)', marginBottom: '16px', lineHeight: 1.4 }}>
        The stars are listening.
      </h2>
      <p style={{ fontSize: '12px', color: 'var(--text-muted-col)', maxWidth: '360px', lineHeight: 1.8 }}>
        Ask anything about your uploaded documents. Mercury will search your knowledge base and answer from your own content.
      </p>
      <div style={{ marginTop: '32px', display: 'flex', gap: '8px', flexWrap: 'wrap', justifyContent: 'center' }}>
        {['What are the main topics?', 'Summarize the key points', 'What conclusions were reached?'].map(hint => (
          <span key={hint} style={{
            fontSize: '10px', padding: '5px 12px', border: '1px solid #1c1912',
            color: 'var(--text-muted-col)', borderRadius: '2px', letterSpacing: '0.02em',
          }}>
            {hint}
          </span>
        ))}
      </div>
    </div>
  );
}

export default function AskPage() {
  const { userId } = useUser();
  const [collections, setCollections] = useState<Collection[]>([]);
  const [collectionId, setCollectionId] = useState('');
  const [messages, setMessages] = useState<ChatMsg[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const abortStream = useRef<(() => void) | null>(null);

  useEffect(() => {
    collectionsApi.list(userId).then(setCollections).catch(() => {});
  }, [userId]);

  useEffect(() => {
    return () => { abortStream.current?.(); };
  }, []);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  const send = useCallback(() => {
    const q = input.trim();
    if (!q || loading) return;

    abortStream.current?.();

    const userMsg: ChatMsg = { role: 'user', content: q, timestamp: new Date().toISOString() };
    const assistantMsg: ChatMsg = { role: 'assistant', content: '', sources: [], timestamp: new Date().toISOString() };
    setMessages(prev => [...prev, userMsg, assistantMsg]);
    setInput('');
    setLoading(true);
    setError('');

    if (textareaRef.current) textareaRef.current.style.height = 'auto';

    abortStream.current = searchApi.askStream(userId, q, collectionId || undefined, {
      onToken: (token) => {
        setMessages(prev => {
          const msgs = [...prev];
          const last = msgs[msgs.length - 1];
          if (last?.role === 'assistant') msgs[msgs.length - 1] = { ...last, content: last.content + token };
          return msgs;
        });
      },
      onSources: (chunks: ChunkSearchResult[]) => {
        setMessages(prev => {
          const msgs = [...prev];
          const last = msgs[msgs.length - 1];
          if (last?.role === 'assistant') msgs[msgs.length - 1] = { ...last, sources: chunks };
          return msgs;
        });
      },
      onDone: () => setLoading(false),
      onError: () => {
        setError('Could not reach Mercury backend. Make sure it is running on port 8080.');
        setLoading(false);
      },
    });
  }, [input, loading, userId, collectionId]);

  function handleKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      send();
    }
  }

  function autoResize(e: React.ChangeEvent<HTMLTextAreaElement>) {
    setInput(e.target.value);
    e.target.style.height = 'auto';
    e.target.style.height = Math.min(e.target.scrollHeight, 160) + 'px';
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 56px - 72px)' }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px', flexShrink: 0 }}>
        <div>
          <div style={{ fontSize: '9px', letterSpacing: '0.24em', textTransform: 'uppercase', color: 'var(--mercury-accent)', marginBottom: '4px' }}>RAG Chat</div>
          <h1 style={{ fontFamily: 'var(--font-cinzel), Cinzel, serif', fontSize: '22px', fontWeight: 600, letterSpacing: '0.04em', color: 'var(--foreground)' }}>
            Ask
          </h1>
        </div>

        {collections.length > 0 && (
          <select
            value={collectionId}
            onChange={e => setCollectionId(e.target.value)}
            style={{
              background: 'var(--card)', border: '1px solid #121420', color: 'var(--foreground)',
              padding: '8px 14px', fontSize: '11px', fontFamily: 'inherit', outline: 'none',
              cursor: 'pointer', borderRadius: '2px',
            }}
          >
            <option value="">All documents</option>
            {collections.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        )}
      </div>

      {/* Messages area */}
      <div style={{ flex: 1, overflowY: 'auto', paddingRight: '4px', marginBottom: '16px' }}>
        {messages.length === 0 && !loading ? (
          <EmptyState />
        ) : (
          <div style={{ maxWidth: '800px' }}>
            {messages.map((msg, i) => {
              const isStreamingMsg = loading && i === messages.length - 1 && msg.role === 'assistant';
              const displayMsg = isStreamingMsg
                ? { ...msg, content: msg.content + '▌' }
                : msg;
              return <ChatMessage key={i} msg={displayMsg} />;
            })}
            <div ref={messagesEndRef} />
          </div>
        )}
      </div>

      {/* Error */}
      {error && (
        <div style={{ fontSize: '11px', color: 'var(--error)', marginBottom: '10px', flexShrink: 0 }}>
          {error}
        </div>
      )}

      {/* Input area */}
      <div style={{
        flexShrink: 0,
        border: '1px solid #121420',
        background: 'var(--card)',
        display: 'flex',
        alignItems: 'flex-end',
        gap: '0',
        borderRadius: '2px',
      }}>
        <textarea
          ref={textareaRef}
          value={input}
          onChange={autoResize}
          onKeyDown={handleKeyDown}
          placeholder="Ask a question about your documents... (Enter to send, Shift+Enter for new line)"
          disabled={loading}
          rows={1}
          style={{
            flex: 1,
            background: 'transparent',
            border: 'none',
            color: 'var(--foreground)',
            padding: '14px 16px',
            fontSize: '13px',
            fontFamily: 'inherit',
            outline: 'none',
            resize: 'none',
            lineHeight: 1.6,
            minHeight: '48px',
            maxHeight: '160px',
          }}
        />
        <button
          onClick={send}
          disabled={!input.trim() || loading}
          style={{
            padding: '0 20px',
            alignSelf: 'stretch',
            background: input.trim() && !loading ? 'var(--accent-dim)' : 'transparent',
            border: 'none',
            borderLeft: '1px solid #121420',
            color: input.trim() && !loading ? 'var(--mercury-accent)' : 'var(--text-muted-col)',
            cursor: input.trim() && !loading ? 'pointer' : 'not-allowed',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            transition: 'all 0.2s',
          }}
        >
          <Send size={15} />
        </button>
      </div>

      <div style={{ fontSize: '10px', color: 'var(--text-subtle)', marginTop: '6px', letterSpacing: '0.04em', flexShrink: 0 }}>
        Enter ↵ to send · Shift+Enter for new line · Session history is not persisted
      </div>
    </div>
  );
}
