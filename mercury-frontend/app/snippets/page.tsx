'use client';

import { useEffect, useMemo, useState } from 'react';
import { ChevronDown, ChevronRight, ExternalLink, FileOutput, FileText, Layers, Scissors, Trash2 } from 'lucide-react';
import { toast } from 'sonner';
import { useUser } from '@/context/UserContext';
import { SkeletonCard } from '@/components/ui/Skeleton';
import { snippetsApi } from '@/lib/snippets';
import { documentsApi } from '@/lib/documents';
import { flashcardsApi } from '@/lib/flashcards';
import type { Snippet, Document, FlashcardDeck } from '@/types';

function Btn({
  onClick,
  children,
  variant = 'ghost',
  disabled = false,
}: {
  onClick: () => void;
  children: React.ReactNode;
  variant?: 'accent' | 'ghost';
  disabled?: boolean;
}) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '6px',
        padding: '9px 18px',
        border: `1px solid ${variant === 'accent' && !disabled ? 'var(--mercury-accent)' : '#1c1912'}`,
        color: variant === 'accent' && !disabled ? 'var(--mercury-accent)' : 'var(--text-muted-col)',
        background: 'transparent',
        fontSize: '10px',
        letterSpacing: '0.08em',
        textTransform: 'uppercase',
        fontFamily: 'inherit',
        cursor: disabled ? 'not-allowed' : 'pointer',
        borderRadius: '2px',
        transition: 'all 0.2s',
        opacity: disabled ? 0.4 : 1,
      }}
    >
      {children}
    </button>
  );
}

export default function SnippetsPage() {
  const { userId } = useUser();
  const [snippets, setSnippets] = useState<Snippet[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [compiling, setCompiling] = useState(false);
  const [showCompileModal, setShowCompileModal] = useState(false);
  const [documentTitle, setDocumentTitle] = useState('');
  const [collapsed, setCollapsed] = useState<Set<string>>(new Set());
  const [documents, setDocuments] = useState<Document[]>([]);
  const [decks, setDecks] = useState<FlashcardDeck[]>([]);
  const [showAppendModal, setShowAppendModal] = useState(false);
  const [showFlashcardsModal, setShowFlashcardsModal] = useState(false);
  const [selectedDocId, setSelectedDocId] = useState('');
  const [selectedDeckId, setSelectedDeckId] = useState('');
  const [appending, setAppending] = useState(false);
  const [creatingCards, setCreatingCards] = useState(false);

  useEffect(() => {
    if (!userId) return;
    snippetsApi.list(userId).then(setSnippets).catch(() => {}).finally(() => setLoading(false));
    documentsApi.list(userId).then(docs => setDocuments(docs.filter(d => d.status === 'COMPLETED'))).catch(() => {});
    flashcardsApi.listDecks(userId).then(setDecks).catch(() => {});
  }, [userId]);

  function toggleSelect(id: string) {
    setSelected(prev => {
      const s = new Set(prev);
      s.has(id) ? s.delete(id) : s.add(id);
      return s;
    });
  }

  async function handleDelete(id: string) {
    try {
      await snippetsApi.delete(id, userId);
      setSnippets(prev => prev.filter(s => s.id !== id));
      setSelected(prev => {
        const s = new Set(prev);
        s.delete(id);
        return s;
      });
      toast.success('Snippet deleted');
    } catch {
      toast.error('Could not delete snippet');
    }
  }

  async function handleCompile() {
    if (compiling || selected.size === 0 || !documentTitle.trim()) return;
    setCompiling(true);
    try {
      await snippetsApi.compile(userId, [...selected], documentTitle.trim());
      toast.success('Document created — processing started');
      setSelected(new Set());
      setDocumentTitle('');
      setShowCompileModal(false);
    } catch {
      toast.error('Compile failed');
    } finally {
      setCompiling(false);
    }
  }

  async function handleAppend() {
    if (appending || selected.size === 0 || !selectedDocId) return;
    setAppending(true);
    try {
      const result = await snippetsApi.appendToDocument(userId, [...selected], selectedDocId);
      const doc = documents.find(d => d.id === selectedDocId);
      toast.success(`${result.count} chunk${result.count !== 1 ? 's' : ''} added to "${doc?.title ?? 'document'}"`);
      setSelected(new Set());
      setShowAppendModal(false);
      setSelectedDocId('');
    } catch {
      toast.error('Failed to append to document');
    } finally {
      setAppending(false);
    }
  }

  async function handleCreateFlashcards() {
    if (creatingCards || selected.size === 0 || !selectedDeckId) return;
    setCreatingCards(true);
    try {
      const result = await snippetsApi.createFlashcards(userId, [...selected], selectedDeckId);
      const deck = decks.find(d => d.id === selectedDeckId);
      toast.success(`${result.count} card${result.count !== 1 ? 's' : ''} added to "${deck?.name ?? 'deck'}"`);
      setSelected(new Set());
      setShowFlashcardsModal(false);
      setSelectedDeckId('');
    } catch {
      toast.error('Failed to create flashcards');
    } finally {
      setCreatingCards(false);
    }
  }

  const sourceHostname = (url?: string) => {
    if (!url) return null;
    try {
      return new URL(url).hostname;
    } catch {
      return url;
    }
  };

  function toggleCollapse(key: string) {
    setCollapsed(prev => {
      const s = new Set(prev);
      s.has(key) ? s.delete(key) : s.add(key);
      return s;
    });
  }

  const groups = useMemo(() => {
    const map = new Map<string, typeof snippets>();
    for (const s of snippets) {
      const key = s.sourceUrl ?? '__no_source__';
      if (!map.has(key)) map.set(key, []);
      map.get(key)!.push(s);
    }
    return [...map.entries()].map(([key, snips]) => ({
      key,
      url: key === '__no_source__' ? null : key,
      title: snips[0].sourceTitle ?? null,
      snippets: snips,
    }));
  }, [snippets]);

  return (
    <div>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '28px' }}>
        <div>
          <div style={{ fontSize: '9px', letterSpacing: '0.24em', textTransform: 'uppercase', color: 'var(--mercury-accent)', marginBottom: '6px' }}>
            Web Capture
          </div>
          <h1 style={{ fontFamily: 'var(--font-cinzel), Cinzel, serif', fontSize: '26px', fontWeight: 600, letterSpacing: '0.04em', color: 'var(--foreground)' }}>
            Snippets
          </h1>
        </div>
        <div style={{ display: 'flex', gap: '8px', marginTop: '4px' }}>
          {selected.size > 0 && (
            <span style={{ fontSize: '10px', color: 'var(--text-muted-col)', alignSelf: 'center', letterSpacing: '0.06em' }}>
              {selected.size} selected
            </span>
          )}
          <Btn
            onClick={() => { setSelectedDocId(documents[0]?.id ?? ''); setShowAppendModal(true); }}
            variant={selected.size > 0 ? 'accent' : 'ghost'}
            disabled={selected.size === 0 || documents.length === 0}
          >
            <FileText size={11} /> Add to Doc
          </Btn>
          <Btn
            onClick={() => { setSelectedDeckId(decks[0]?.id ?? ''); setShowFlashcardsModal(true); }}
            variant={selected.size > 0 ? 'accent' : 'ghost'}
            disabled={selected.size === 0 || decks.length === 0}
          >
            <Layers size={11} /> To Flashcards
          </Btn>
          <Btn
            onClick={() => setShowCompileModal(true)}
            variant={selected.size > 0 ? 'accent' : 'ghost'}
            disabled={selected.size === 0}
          >
            <FileOutput size={11} /> Compile into Document
          </Btn>
        </div>
      </div>

      {/* Skeleton */}
      {loading && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1px', background: '#121420', border: '1px solid #121420' }}>
          {Array.from({ length: 4 }).map((_, i) => <SkeletonCard key={i} />)}
        </div>
      )}

      {/* Empty state */}
      {!loading && snippets.length === 0 && (
        <div style={{ border: '1px solid #121420', background: 'var(--card)', padding: '60px', textAlign: 'center' }}>
          <Scissors size={28} style={{ color: 'var(--text-muted-col)', margin: '0 auto 16px' }} />
          <div style={{ fontFamily: 'var(--font-cinzel), Cinzel, serif', fontSize: '16px', color: 'var(--foreground)', marginBottom: '12px' }}>
            No snippets yet
          </div>
          <p style={{ fontSize: '12px', color: 'var(--text-muted-col)', maxWidth: '360px', margin: '0 auto' }}>
            Install the Mercury browser extension, select any text on a webpage, right-click and choose &ldquo;Save to Knowledge Engine&rdquo;.
          </p>
        </div>
      )}

      {/* Snippet list grouped by URL */}
      {!loading && snippets.length > 0 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {groups.map(group => {
            const isCollapsed = collapsed.has(group.key);
            const hostname = sourceHostname(group.url ?? undefined);

            return (
              <div key={group.key} style={{ border: '1px solid #121420' }}>
                {/* Group header */}
                <button
                  onClick={() => toggleCollapse(group.key)}
                  style={{
                    width: '100%',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    padding: '10px 14px',
                    background: '#0a0910',
                    border: 'none',
                    borderBottom: isCollapsed ? 'none' : '1px solid #121420',
                    cursor: 'pointer',
                    textAlign: 'left',
                  }}
                >
                  {isCollapsed
                    ? <ChevronRight size={12} style={{ color: 'var(--text-muted-col)', flexShrink: 0 }} />
                    : <ChevronDown size={12} style={{ color: 'var(--text-muted-col)', flexShrink: 0 }} />}
                  {group.url ? (
                    <a
                      href={group.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      onClick={e => e.stopPropagation()}
                      style={{ display: 'inline-flex', alignItems: 'center', gap: '5px', textDecoration: 'none' }}
                    >
                      <ExternalLink size={10} style={{ color: 'var(--text-muted-col)', flexShrink: 0 }} />
                      <span style={{ fontSize: '11px', color: 'var(--mercury-accent)', letterSpacing: '0.04em' }}>
                        {group.title ?? hostname}
                      </span>
                      {group.title && hostname && (
                        <span style={{ fontSize: '10px', color: 'var(--text-muted-col)', marginLeft: '4px' }}>
                          {hostname}
                        </span>
                      )}
                    </a>
                  ) : (
                    <span style={{ fontSize: '11px', color: 'var(--text-muted-col)', letterSpacing: '0.04em' }}>No source</span>
                  )}
                  <span style={{ marginLeft: 'auto', fontSize: '10px', color: 'var(--text-muted-col)' }}>
                    {group.snippets.length}
                  </span>
                </button>

                {/* Snippet cards */}
                {!isCollapsed && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '1px', background: '#121420' }}>
                    {group.snippets.map(snippet => {
                      const isSelected = selected.has(snippet.id);
                      const preview = snippet.content.length > 220
                        ? snippet.content.slice(0, 220) + '…'
                        : snippet.content;

                      return (
                        <div
                          key={snippet.id}
                          style={{
                            display: 'flex',
                            alignItems: 'flex-start',
                            gap: '14px',
                            padding: '16px 18px',
                            background: 'var(--card)',
                            borderLeft: `3px solid ${isSelected ? 'var(--mercury-accent)' : 'transparent'}`,
                            transition: 'border-color 0.15s',
                          }}
                        >
                          <input
                            type="checkbox"
                            checked={isSelected}
                            onChange={() => toggleSelect(snippet.id)}
                            style={{ marginTop: '3px', cursor: 'pointer', accentColor: 'var(--mercury-accent)', flexShrink: 0 }}
                          />
                          <div style={{ flex: 1, minWidth: 0 }}>
                            <p style={{ margin: '0 0 8px', fontSize: '13px', color: 'var(--foreground)', lineHeight: '1.6', wordBreak: 'break-word' }}>
                              {preview}
                            </p>
                            <span style={{ fontSize: '10px', color: 'var(--text-muted-col)' }}>
                              {new Date(snippet.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}
                            </span>
                          </div>
                          <button
                            onClick={() => handleDelete(snippet.id)}
                            style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-muted-col)', padding: '2px', flexShrink: 0, display: 'flex', alignItems: 'center' }}
                            title="Delete snippet"
                          >
                            <Trash2 size={13} />
                          </button>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Append to Document Modal */}
      {showAppendModal && (
        <>
          <div onClick={() => setShowAppendModal(false)} style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', zIndex: 50 }} />
          <div style={{ position: 'fixed', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', zIndex: 51, background: '#0d0b12', border: '1px solid #1c1912', padding: '28px 32px', width: '400px', maxWidth: 'calc(100vw - 32px)', borderRadius: '2px' }}>
            <h2 style={{ fontFamily: 'var(--font-cinzel), Cinzel, serif', fontSize: '16px', fontWeight: 600, color: 'var(--foreground)', marginBottom: '6px' }}>
              Add to Document
            </h2>
            <p style={{ fontSize: '11px', color: 'var(--text-muted-col)', marginBottom: '20px' }}>
              {selected.size} snippet{selected.size !== 1 ? 's' : ''} will be appended as searchable chunks.
            </p>
            <label style={{ fontSize: '9px', letterSpacing: '0.14em', textTransform: 'uppercase', color: 'var(--text-muted-col)', display: 'block', marginBottom: '6px' }}>
              Document
            </label>
            <select
              value={selectedDocId}
              onChange={e => setSelectedDocId(e.target.value)}
              style={{ width: '100%', boxSizing: 'border-box', background: '#0a0910', border: '1px solid #1c1912', color: 'var(--foreground)', padding: '9px 12px', fontSize: '13px', fontFamily: 'inherit', outline: 'none', borderRadius: '2px', marginBottom: '20px', cursor: 'pointer' }}
            >
              {documents.map(doc => (
                <option key={doc.id} value={doc.id}>{doc.title}</option>
              ))}
            </select>
            <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
              <Btn onClick={() => setShowAppendModal(false)}>Cancel</Btn>
              <Btn onClick={handleAppend} variant="accent" disabled={!selectedDocId || appending}>
                <FileText size={11} /> {appending ? 'Appending…' : 'Append'}
              </Btn>
            </div>
          </div>
        </>
      )}

      {/* Create Flashcards Modal */}
      {showFlashcardsModal && (
        <>
          <div onClick={() => setShowFlashcardsModal(false)} style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', zIndex: 50 }} />
          <div style={{ position: 'fixed', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', zIndex: 51, background: '#0d0b12', border: '1px solid #1c1912', padding: '28px 32px', width: '400px', maxWidth: 'calc(100vw - 32px)', borderRadius: '2px' }}>
            <h2 style={{ fontFamily: 'var(--font-cinzel), Cinzel, serif', fontSize: '16px', fontWeight: 600, color: 'var(--foreground)', marginBottom: '6px' }}>
              Create Flashcards
            </h2>
            <p style={{ fontSize: '11px', color: 'var(--text-muted-col)', marginBottom: '20px' }}>
              {selected.size} snippet{selected.size !== 1 ? 's' : ''} — AI generates a question for each, snippet becomes the answer.
            </p>
            <label style={{ fontSize: '9px', letterSpacing: '0.14em', textTransform: 'uppercase', color: 'var(--text-muted-col)', display: 'block', marginBottom: '6px' }}>
              Deck
            </label>
            <select
              value={selectedDeckId}
              onChange={e => setSelectedDeckId(e.target.value)}
              style={{ width: '100%', boxSizing: 'border-box', background: '#0a0910', border: '1px solid #1c1912', color: 'var(--foreground)', padding: '9px 12px', fontSize: '13px', fontFamily: 'inherit', outline: 'none', borderRadius: '2px', marginBottom: '20px', cursor: 'pointer' }}
            >
              {decks.map(deck => (
                <option key={deck.id} value={deck.id}>{deck.name}</option>
              ))}
            </select>
            <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
              <Btn onClick={() => setShowFlashcardsModal(false)}>Cancel</Btn>
              <Btn onClick={handleCreateFlashcards} variant="accent" disabled={!selectedDeckId || creatingCards}>
                <Layers size={11} /> {creatingCards ? 'Generating…' : 'Create Cards'}
              </Btn>
            </div>
          </div>
        </>
      )}

      {/* Compile Modal */}
      {showCompileModal && (
        <>
          <div
            onClick={() => setShowCompileModal(false)}
            style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', zIndex: 50 }}
          />
          <div style={{
            position: 'fixed',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            zIndex: 51,
            background: '#0d0b12',
            border: '1px solid #1c1912',
            padding: '28px 32px',
            width: '400px',
            maxWidth: 'calc(100vw - 32px)',
            borderRadius: '2px',
          }}>
            <h2 style={{ fontFamily: 'var(--font-cinzel), Cinzel, serif', fontSize: '16px', fontWeight: 600, color: 'var(--foreground)', marginBottom: '6px' }}>
              Compile into Document
            </h2>
            <p style={{ fontSize: '11px', color: 'var(--text-muted-col)', marginBottom: '20px' }}>
              {selected.size} snippet{selected.size !== 1 ? 's' : ''} will be merged and queued for RAG ingestion.
            </p>
            <label style={{ fontSize: '9px', letterSpacing: '0.14em', textTransform: 'uppercase', color: 'var(--text-muted-col)', display: 'block', marginBottom: '6px' }}>
              Document Title
            </label>
            <input
              autoFocus
              type="text"
              placeholder="e.g. Research notes on React"
              value={documentTitle}
              onChange={e => setDocumentTitle(e.target.value)}
              onKeyDown={e => { if (e.key === 'Enter') handleCompile(); if (e.key === 'Escape') setShowCompileModal(false); }}
              style={{
                width: '100%',
                boxSizing: 'border-box',
                background: '#0a0910',
                border: '1px solid #1c1912',
                color: 'var(--foreground)',
                padding: '9px 12px',
                fontSize: '13px',
                fontFamily: 'inherit',
                outline: 'none',
                borderRadius: '2px',
                marginBottom: '20px',
              }}
            />
            <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
              <Btn onClick={() => setShowCompileModal(false)}>Cancel</Btn>
              <Btn
                onClick={handleCompile}
                variant="accent"
                disabled={!documentTitle.trim() || compiling}
              >
                <FileOutput size={11} /> {compiling ? 'Compiling…' : 'Compile'}
              </Btn>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
