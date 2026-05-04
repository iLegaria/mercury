'use client';

import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { ArrowLeft, BookOpen, Layers, BrainCircuit, BarChart2, Loader2, Pencil, FileText, Trash2 } from 'lucide-react';
import { toast } from 'sonner';
import { useUser } from '@/context/UserContext';
import { documentsApi } from '@/lib/documents';
import { studyApi } from '@/lib/study';
import StatusBadge from '@/components/documents/StatusBadge';
import ContentTab from '@/components/documents/ContentTab';
import type { Document, DocumentStudyStats } from '@/types';

type Tab = 'overview' | 'flashcards' | 'quiz' | 'content';
type QuizGenMode = 'EXTRACT' | 'GENERATE';

const QUIZ_LOADING_MSGS = [
  'Sampling document chunks...',
  'Generating questions with AI...',
  'Preparing your quiz...',
  'Almost ready...',
];

const QUIZ_SOURCE_INFO: Record<QuizGenMode, { label: string; desc: string }> = {
  EXTRACT: { label: 'From existing Q&A', desc: 'Uses Q&A pairs already in the document. Best for study guides.' },
  GENERATE: { label: 'Generate new', desc: 'LLM creates questions from content. Best for articles and papers.' },
};

const SOURCE_COLORS: Record<string, string> = {
  PDF: '#e8952a', DOCX: '#4a7fa8', TXT: '#4a9e6b', HTML: '#9e78c5', MARKDOWN: '#6e6758', OTHER: '#6e6758',
};

function StatBox({ label, value }: { label: string; value: string | number }) {
  return (
    <div style={{ background: 'var(--card)', border: '1px solid #121420', padding: '18px 20px', borderRadius: '2px' }}>
      <div style={{ fontSize: '9px', letterSpacing: '0.18em', textTransform: 'uppercase', color: 'var(--text-muted-col)', marginBottom: '8px' }}>
        {label}
      </div>
      <div style={{ fontSize: '22px', fontWeight: 600, color: 'var(--foreground)', letterSpacing: '0.02em' }}>
        {value}
      </div>
    </div>
  );
}

function ActionButton({
  onClick,
  disabled,
  children,
  variant = 'default',
}: {
  onClick: () => void;
  disabled?: boolean;
  children: React.ReactNode;
  variant?: 'default' | 'accent';
}) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      style={{
        padding: '14px 20px',
        border: `1px solid ${variant === 'accent' ? 'var(--mercury-accent)' : '#1c1912'}`,
        background: variant === 'accent' ? 'var(--accent-dim)' : 'var(--card)',
        color: variant === 'accent' ? 'var(--mercury-accent)' : 'var(--foreground)',
        fontSize: '12px',
        fontFamily: 'inherit',
        cursor: disabled ? 'not-allowed' : 'pointer',
        opacity: disabled ? 0.5 : 1,
        borderRadius: '2px',
        transition: 'all 0.2s',
        textAlign: 'left' as const,
        width: '100%',
      }}
    >
      {children}
    </button>
  );
}

export default function DocumentHubPage() {
  const params = useParams<{ id: string }>();
  const id = params?.id ?? '';
  const router = useRouter();
  const { userId } = useUser();
  const [doc, setDoc] = useState<Document | null>(null);
  const [stats, setStats] = useState<DocumentStudyStats | null>(null);
  const [tab, setTab] = useState<Tab>('content');
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [editingTitle, setEditingTitle] = useState(false);
  const [titleDraft, setTitleDraft] = useState('');
  const [savingTitle, setSavingTitle] = useState(false);
  const [numQuestions, setNumQuestions] = useState(5);
  const [quizGenMode, setQuizGenMode] = useState<QuizGenMode>('GENERATE');
  const [quizGenerating, setQuizGenerating] = useState(false);
  const [loadingMsgIdx, setLoadingMsgIdx] = useState(0);

  useEffect(() => {
    if (!quizGenerating) { setLoadingMsgIdx(0); return; }
    const id = setInterval(() => setLoadingMsgIdx(i => (i + 1) % QUIZ_LOADING_MSGS.length), 2200);
    return () => clearInterval(id);
  }, [quizGenerating]);

  useEffect(() => {
    if (!userId || !id) return;
    Promise.all([
      documentsApi.get(id),
      studyApi.getStats(id, userId),
    ])
      .then(([d, s]) => { setDoc(d); setStats(s); })
      .catch(() => toast.error('Could not load document'))
      .finally(() => setLoading(false));
  }, [userId, id]);

  async function handleGenerateFlashcards(mode: 'EXTRACT' | 'GENERATE') {
    setGenerating(true);
    try {
      const deck = await studyApi.generateFlashcards(id, userId, mode, mode === 'EXTRACT' ? 50 : 10);
      toast.success(`Deck ready — ${deck.cardCount} cards`);
      router.push(`/flashcards/${deck.id}`);
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : 'Could not generate flashcards';
      toast.error(msg);
    } finally {
      setGenerating(false);
    }
  }

  async function handleStartQuiz() {
    setQuizGenerating(true);
    try {
      const session = await studyApi.startQuiz(id, userId, quizGenMode, numQuestions);
      router.push(`/quiz/${session.sessionId}`);
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : 'Could not start quiz';
      toast.error(msg);
    } finally {
      setQuizGenerating(false);
    }
  }

  async function handleDelete() {
    if (!confirm(`Delete "${doc?.title}"? This cannot be undone.`)) return;
    setDeleting(true);
    try {
      await documentsApi.delete(id, userId);
      router.push('/documents');
    } catch {
      toast.error('Could not delete document');
      setDeleting(false);
    }
  }

  async function handleUpdateTitle() {
    if (!titleDraft.trim() || !doc) return;
    setSavingTitle(true);
    try {
      const updated = await documentsApi.updateTitle(id, userId, titleDraft.trim());
      setDoc(updated);
      setEditingTitle(false);
    } catch {
      toast.error('Could not update title');
    } finally {
      setSavingTitle(false);
    }
  }

  const tabs: { key: Tab; label: string; icon: React.ReactNode }[] = [
    { key: 'content', label: 'Content', icon: <FileText size={12} /> },
    { key: 'overview', label: 'Overview', icon: <BarChart2 size={12} /> },
    { key: 'flashcards', label: 'Flashcards', icon: <Layers size={12} /> },
    { key: 'quiz', label: 'Quiz', icon: <BrainCircuit size={12} /> },
  ];

  if (loading) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '200px', color: 'var(--text-muted-col)' }}>
        <Loader2 size={18} style={{ animation: 'spin 1s linear infinite' }} />
      </div>
    );
  }

  if (!doc) {
    return (
      <div style={{ color: 'var(--text-muted-col)', fontSize: '13px' }}>
        Document not found.{' '}
        <button onClick={() => router.push('/documents')} style={{ background: 'none', border: 'none', color: 'var(--mercury-accent)', cursor: 'pointer', fontSize: '13px', fontFamily: 'inherit' }}>
          Back to Documents
        </button>
      </div>
    );
  }

  const isReady = doc.status === 'COMPLETED';

  return (
    <div>
      {/* Back nav + delete */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px' }}>
        <button
          onClick={() => router.push('/documents')}
          style={{
            display: 'inline-flex', alignItems: 'center', gap: '5px',
            background: 'none', border: 'none', cursor: 'pointer',
            color: 'var(--text-muted-col)', fontSize: '10px',
            letterSpacing: '0.06em', fontFamily: 'inherit', padding: '0',
          }}
        >
          <ArrowLeft size={11} /> Documents
        </button>
        <button
          onClick={handleDelete}
          disabled={deleting}
          style={{
            display: 'inline-flex', alignItems: 'center', gap: '5px',
            background: 'none', border: 'none', cursor: deleting ? 'not-allowed' : 'pointer',
            color: 'var(--error)', fontSize: '10px', opacity: deleting ? 0.5 : 1,
            letterSpacing: '0.06em', fontFamily: 'inherit', padding: '0',
          }}
        >
          {deleting ? <Loader2 size={11} style={{ animation: 'spin 1s linear infinite' }} /> : <Trash2 size={11} />}
          Delete
        </button>
      </div>

      {/* Document header */}
      <div style={{ marginBottom: '32px' }}>
        <div style={{ fontSize: '9px', letterSpacing: '0.24em', textTransform: 'uppercase', color: 'var(--mercury-accent)', marginBottom: '6px' }}>
          Study Hub
        </div>
        {editingTitle ? (
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
            <input
              type="text"
              value={titleDraft}
              onChange={e => setTitleDraft(e.target.value)}
              onKeyDown={e => {
                if (e.key === 'Enter') handleUpdateTitle();
                if (e.key === 'Escape') setEditingTitle(false);
              }}
              autoFocus
              style={{
                flex: 1, fontFamily: 'var(--font-cinzel), Cinzel, serif',
                fontSize: '22px', fontWeight: 600, letterSpacing: '0.04em',
                color: 'var(--foreground)', background: 'transparent',
                border: 'none', borderBottom: '1px solid var(--mercury-accent)',
                outline: 'none', lineHeight: 1.4, padding: '0',
              }}
            />
            {savingTitle
              ? <Loader2 size={14} style={{ animation: 'spin 1s linear infinite', color: 'var(--mercury-accent)', flexShrink: 0 }} />
              : (
                <button
                  onClick={handleUpdateTitle}
                  style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--mercury-accent)', padding: '2px', display: 'flex', flexShrink: 0 }}
                >
                  <Pencil size={13} />
                </button>
              )
            }
          </div>
        ) : (
          <div style={{ display: 'flex', alignItems: 'flex-start', gap: '8px', marginBottom: '12px' }}>
            <h1 style={{
              fontFamily: 'var(--font-cinzel), Cinzel, serif',
              fontSize: '22px', fontWeight: 600, letterSpacing: '0.04em',
              color: 'var(--foreground)', lineHeight: 1.4, margin: 0,
            }}>
              {doc.title}
            </h1>
            <button
              onClick={() => { setTitleDraft(doc.title); setEditingTitle(true); }}
              style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-muted-col)', padding: '2px', marginTop: '4px', display: 'flex', flexShrink: 0, opacity: 0.6 }}
            >
              <Pencil size={12} />
            </button>
          </div>
        )}
        <div style={{ display: 'flex', gap: '8px', alignItems: 'center', flexWrap: 'wrap' }}>
          <span style={{
            fontSize: '9px', letterSpacing: '0.08em', padding: '2px 7px',
            border: `1px solid ${SOURCE_COLORS[doc.sourceType] ?? '#6e6758'}22`,
            color: SOURCE_COLORS[doc.sourceType] ?? 'var(--text-muted-col)',
            borderRadius: '2px',
          }}>
            {doc.sourceType}
          </span>
          <StatusBadge status={doc.status} />
          {doc.collectionName && (
            <span style={{ fontSize: '9px', color: 'var(--text-muted-col)', padding: '2px 7px', border: '1px solid #1c1912', borderRadius: '2px' }}>
              {doc.collectionName}
            </span>
          )}
        </div>
      </div>

      {/* Not ready banner */}
      {!isReady && (
        <div style={{
          background: 'var(--accent-dim)', border: '1px solid #1c1912',
          padding: '12px 16px', marginBottom: '24px', borderRadius: '2px',
          fontSize: '12px', color: 'var(--text-muted-col)',
        }}>
          Document is still being processed. Study features will be available once processing completes.
        </div>
      )}

      {/* Tabs */}
      <div style={{ display: 'flex', gap: '0', borderBottom: '1px solid #121420', marginBottom: '28px' }}>
        {tabs.map(t => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            style={{
              display: 'inline-flex', alignItems: 'center', gap: '6px',
              padding: '10px 18px',
              background: 'none', border: 'none',
              borderBottom: tab === t.key ? '2px solid var(--mercury-accent)' : '2px solid transparent',
              color: tab === t.key ? 'var(--mercury-accent)' : 'var(--text-muted-col)',
              fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase',
              fontFamily: 'inherit', cursor: 'pointer',
              marginBottom: '-1px',
              transition: 'all 0.15s',
            }}
          >
            {t.icon} {t.label}
          </button>
        ))}
      </div>

      {/* Overview tab */}
      {tab === 'overview' && stats && (
        <div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))', gap: '1px', background: '#121420', border: '1px solid #121420', marginBottom: '24px' }}>
            <StatBox label="Chunks Indexed" value={stats.chunkCount} />
            <StatBox label="Flashcard Decks" value={stats.deckCount} />
            <StatBox label="Total Cards" value={stats.totalCards} />
            <StatBox label="Quiz Attempts" value={stats.quizAttempts} />
            <StatBox label="Quizzes Completed" value={stats.completedQuizzes} />
            <StatBox
              label="Avg Quiz Score"
              value={stats.avgScore != null ? `${Math.round(stats.avgScore * 100)}%` : '—'}
            />
          </div>

          <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
            <button
              onClick={() => setTab('flashcards')}
              style={{
                display: 'inline-flex', alignItems: 'center', gap: '6px',
                padding: '10px 20px',
                border: '1px solid var(--mercury-accent)',
                color: 'var(--mercury-accent)', background: 'transparent',
                fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase',
                fontFamily: 'inherit', cursor: 'pointer', borderRadius: '2px',
              }}
            >
              <Layers size={11} /> Flashcards
            </button>
            <button
              onClick={() => setTab('quiz')}
              style={{
                display: 'inline-flex', alignItems: 'center', gap: '6px',
                padding: '10px 20px',
                border: '1px solid #1c1912',
                color: 'var(--text-muted-col)', background: 'transparent',
                fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase',
                fontFamily: 'inherit', cursor: 'pointer', borderRadius: '2px',
              }}
            >
              <BrainCircuit size={11} /> Quiz
            </button>
          </div>
        </div>
      )}

      {/* Flashcards tab */}
      {tab === 'flashcards' && (
        <div style={{ maxWidth: '560px' }}>
          {stats && stats.deckCount > 0 ? (
            <div style={{ background: 'var(--card)', border: '1px solid #121420', padding: '24px', borderRadius: '2px', marginBottom: '20px' }}>
              <div style={{ fontSize: '9px', letterSpacing: '0.18em', textTransform: 'uppercase', color: 'var(--mercury-accent)', marginBottom: '8px' }}>
                Deck Already Generated
              </div>
              <p style={{ fontSize: '13px', color: 'var(--foreground)', marginBottom: '4px' }}>
                {stats.totalCards} cards across {stats.deckCount} {stats.deckCount === 1 ? 'deck' : 'decks'}.
              </p>
              <p style={{ fontSize: '11px', color: 'var(--text-muted-col)', marginBottom: '16px' }}>
                View and study them in the Flashcards section.
              </p>
              <button
                onClick={() => router.push('/flashcards')}
                style={{
                  padding: '10px 20px', border: '1px solid var(--mercury-accent)',
                  color: 'var(--mercury-accent)', background: 'transparent',
                  fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase',
                  fontFamily: 'inherit', cursor: 'pointer', borderRadius: '2px',
                }}
              >
                Go to Flashcards →
              </button>
            </div>
          ) : null}

          <div style={{ marginBottom: '20px' }}>
            <div style={{ fontSize: '9px', letterSpacing: '0.18em', textTransform: 'uppercase', color: 'var(--text-muted-col)', marginBottom: '14px' }}>
              {stats && stats.deckCount > 0 ? 'Regenerate' : 'Choose Generation Mode'}
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
              <ActionButton
                onClick={() => handleGenerateFlashcards('EXTRACT')}
                disabled={!isReady || generating}
                variant="accent"
              >
                <div style={{ fontSize: '12px', fontWeight: 500, marginBottom: '5px', letterSpacing: '0.02em' }}>
                  {generating ? <Loader2 size={12} style={{ display: 'inline', animation: 'spin 1s linear infinite' }} /> : 'Extract Q&A'}
                </div>
                <div style={{ fontSize: '10.5px', color: 'var(--text-muted-col)', lineHeight: 1.6 }}>
                  Pulls existing Q&A pairs verbatim. Best for study guides, flashcard sets, citizenship prep.
                </div>
              </ActionButton>
              <ActionButton
                onClick={() => handleGenerateFlashcards('GENERATE')}
                disabled={!isReady || generating}
              >
                <div style={{ fontSize: '12px', fontWeight: 500, marginBottom: '5px', letterSpacing: '0.02em' }}>
                  {generating ? <Loader2 size={12} style={{ display: 'inline', animation: 'spin 1s linear infinite' }} /> : 'Generate'}
                </div>
                <div style={{ fontSize: '10.5px', color: 'var(--text-muted-col)', lineHeight: 1.6 }}>
                  LLM creates cards from content. Best for articles, papers, books with no Q&A structure.
                </div>
              </ActionButton>
            </div>
          </div>

          <p style={{ fontSize: '10.5px', color: 'var(--text-subtle)', lineHeight: 1.7 }}>
            Cards are studied using the SM-2 spaced repetition algorithm. Intervals grow for easy cards, shrink for hard ones.
          </p>
        </div>
      )}

      {/* Quiz tab */}
      {tab === 'quiz' && (
        <div style={{ maxWidth: '560px' }}>
          {/* Stats summary */}
          {stats && stats.quizAttempts > 0 && (
            <div style={{
              background: 'var(--accent-dim)', border: '1px solid #1c1912',
              padding: '14px 18px', marginBottom: '24px', borderRadius: '2px',
              fontSize: '12px', color: 'var(--mercury-accent)',
            }}>
              {stats.completedQuizzes} quizzes completed from this document
              {stats.avgScore != null && ` · avg score ${Math.round(stats.avgScore * 100)}%`}
            </div>
          )}

          {/* Number of questions */}
          <div style={{ marginBottom: '24px' }}>
            <div style={{ fontSize: '9px', letterSpacing: '0.18em', textTransform: 'uppercase', color: 'var(--text-muted-col)', marginBottom: '10px' }}>
              Questions — <span style={{ color: 'var(--mercury-accent)' }}>{numQuestions}</span>
            </div>
            <input
              type="range" min={3} max={15} value={numQuestions}
              onChange={e => setNumQuestions(+e.target.value)}
              style={{ width: '100%', accentColor: 'var(--mercury-accent)', cursor: 'pointer' }}
            />
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '9px', color: 'var(--text-subtle)', marginTop: '4px' }}>
              <span>3 min</span><span>15 max</span>
            </div>
          </div>

          {/* Question source selector */}
          <div style={{ marginBottom: '24px' }}>
            <div style={{ fontSize: '9px', letterSpacing: '0.18em', textTransform: 'uppercase', color: 'var(--text-muted-col)', marginBottom: '10px' }}>
              Question Source
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
              {(['EXTRACT', 'GENERATE'] as QuizGenMode[]).map(m => (
                <button
                  key={m}
                  onClick={() => setQuizGenMode(m)}
                  disabled={quizGenerating}
                  style={{
                    padding: '14px 16px',
                    border: `1px solid ${quizGenMode === m ? 'var(--mercury-accent)' : '#1c1912'}`,
                    background: quizGenMode === m ? 'var(--accent-dim)' : 'var(--card)',
                    textAlign: 'left', cursor: quizGenerating ? 'not-allowed' : 'pointer',
                    fontFamily: 'inherit', borderRadius: '2px', transition: 'all 0.2s',
                    opacity: quizGenerating ? 0.5 : 1,
                  }}
                >
                  <div style={{ fontSize: '12px', fontWeight: 500, marginBottom: '4px', color: quizGenMode === m ? 'var(--mercury-accent)' : 'var(--foreground)' }}>
                    {QUIZ_SOURCE_INFO[m].label}
                  </div>
                  <div style={{ fontSize: '10.5px', color: 'var(--text-muted-col)', lineHeight: 1.5 }}>
                    {QUIZ_SOURCE_INFO[m].desc}
                  </div>
                </button>
              ))}
            </div>
          </div>

          {/* Generate button + loading message */}
          <button
            onClick={handleStartQuiz}
            disabled={!isReady || quizGenerating}
            style={{
              padding: '12px 32px',
              border: '1px solid var(--mercury-accent)',
              color: 'var(--mercury-accent)',
              background: quizGenerating ? 'var(--accent-dim)' : 'transparent',
              fontSize: '11px', letterSpacing: '0.08em', textTransform: 'uppercase',
              fontFamily: 'inherit',
              cursor: !isReady || quizGenerating ? 'not-allowed' : 'pointer',
              opacity: !isReady || quizGenerating ? 0.7 : 1,
              transition: 'all 0.2s', borderRadius: '2px',
              display: 'flex', alignItems: 'center', gap: '8px',
            }}
          >
            {quizGenerating && <Loader2 size={12} style={{ animation: 'spin 1s linear infinite' }} />}
            {quizGenerating ? 'Generating...' : 'Generate Quiz →'}
          </button>

          {quizGenerating && (
            <p style={{
              fontSize: '10.5px', color: 'var(--mercury-accent)', marginTop: '12px',
              lineHeight: 1.7, transition: 'opacity 0.4s',
            }}>
              {QUIZ_LOADING_MSGS[loadingMsgIdx]}
            </p>
          )}

          {!quizGenerating && (
            <p style={{ fontSize: '10.5px', color: 'var(--text-subtle)', marginTop: '12px', lineHeight: 1.7 }}>
              Questions generated by Cohere Command-R from your document.
            </p>
          )}
        </div>
      )}

      {/* Content tab */}
      <ContentTab doc={doc} userId={userId} active={tab === 'content'} isReady={isReady} onUpdated={setDoc} />
    </div>
  );
}
