'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { ArrowLeft, Loader2, FileText } from 'lucide-react';
import { toast } from 'sonner';
import { useUser } from '@/context/UserContext';
import { documentsApi } from '@/lib/documents';
import { collectionsApi } from '@/lib/collections';
import type { Collection } from '@/types';

export default function NewDocumentPage() {
  const router = useRouter();
  const { userId } = useUser();

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [collectionId, setCollectionId] = useState('');
  const [collections, setCollections] = useState<Collection[]>([]);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!userId) return;
    collectionsApi.list(userId).then(setCollections).catch(() => {});
  }, [userId]);

  const wordCount = content.trim() ? content.trim().split(/\s+/).length : 0;

  async function handleSubmit() {
    if (!title.trim() || !content.trim()) return;
    setSaving(true);
    try {
      const doc = await documentsApi.createFromText(
        userId,
        title.trim(),
        content.trim(),
        collectionId || undefined,
      );
      toast.success('Document created — processing in background');
      router.push(`/documents/${doc.id}`);
    } catch {
      toast.error('Could not create document');
      setSaving(false);
    }
  }

  const inputStyle = {
    width: '100%',
    background: 'var(--card)',
    border: '1px solid #1c1912',
    color: 'var(--foreground)',
    fontFamily: 'inherit',
    fontSize: '13px',
    padding: '10px 14px',
    outline: 'none',
    borderRadius: '2px',
    boxSizing: 'border-box' as const,
  };

  const labelStyle = {
    fontSize: '9px',
    letterSpacing: '0.18em',
    textTransform: 'uppercase' as const,
    color: 'var(--text-muted-col)',
    display: 'block',
    marginBottom: '6px',
  };

  return (
    <div style={{ maxWidth: '800px' }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '32px' }}>
        <button
          onClick={() => router.push('/documents')}
          style={{
            display: 'inline-flex', alignItems: 'center', gap: '6px',
            background: 'none', border: 'none', cursor: 'pointer',
            color: 'var(--text-muted-col)', fontSize: '11px',
            fontFamily: 'inherit', padding: '0',
          }}
        >
          <ArrowLeft size={13} /> Back
        </button>
      </div>

      <div style={{ marginBottom: '28px' }}>
        <div style={{ fontSize: '9px', letterSpacing: '0.24em', textTransform: 'uppercase', color: 'var(--mercury-accent)', marginBottom: '6px' }}>
          Library
        </div>
        <h1 style={{
          fontFamily: 'var(--font-cinzel), Cinzel, serif',
          fontSize: '26px', fontWeight: 600,
          letterSpacing: '0.04em', color: 'var(--foreground)',
          display: 'flex', alignItems: 'center', gap: '12px',
        }}>
          <FileText size={22} style={{ color: 'var(--mercury-accent)' }} />
          New Document
        </h1>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
        {/* Title */}
        <div>
          <label style={labelStyle}>Title</label>
          <input
            type="text"
            value={title}
            onChange={e => setTitle(e.target.value)}
            placeholder="e.g. DDIA Ch6 — Replication"
            style={inputStyle}
            onKeyDown={e => e.key === 'Enter' && document.getElementById('content-area')?.focus()}
          />
        </div>

        {/* Collection */}
        {collections.length > 0 && (
          <div>
            <label style={labelStyle}>Collection (optional)</label>
            <select
              value={collectionId}
              onChange={e => setCollectionId(e.target.value)}
              style={{ ...inputStyle, cursor: 'pointer' }}
            >
              <option value="">None</option>
              {collections.map(c => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          </div>
        )}

        {/* Content */}
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
            <label style={{ ...labelStyle, marginBottom: 0 }}>Content</label>
            {wordCount > 0 && (
              <span style={{ fontSize: '9px', color: 'var(--text-subtle)' }}>
                ~{wordCount.toLocaleString()} words
              </span>
            )}
          </div>
          <textarea
            id="content-area"
            value={content}
            onChange={e => setContent(e.target.value)}
            placeholder="Paste your notes, highlights, or any text here…"
            style={{
              ...inputStyle,
              minHeight: '55vh',
              lineHeight: 1.8,
              resize: 'vertical',
              padding: '16px 18px',
            }}
          />
          <p style={{ fontSize: '9px', color: 'var(--text-muted-col)', marginTop: '6px' }}>
            Text will be chunked, embedded, and indexed for semantic search, Q&amp;A, flashcards, and quizzes.
          </p>
        </div>

        {/* Actions */}
        <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
          <button
            onClick={() => router.push('/documents')}
            disabled={saving}
            style={{
              padding: '9px 20px', background: 'none',
              border: '1px solid #1c1912', color: 'var(--text-muted-col)',
              fontSize: '10px', fontFamily: 'inherit', cursor: 'pointer',
              borderRadius: '2px', letterSpacing: '0.06em',
            }}
          >
            Cancel
          </button>
          <button
            onClick={handleSubmit}
            disabled={saving || !title.trim() || !content.trim()}
            style={{
              padding: '9px 20px',
              background: 'var(--accent-dim)',
              border: '1px solid var(--mercury-accent)',
              color: 'var(--mercury-accent)',
              fontSize: '10px', fontFamily: 'inherit',
              cursor: saving || !title.trim() || !content.trim() ? 'not-allowed' : 'pointer',
              opacity: saving || !title.trim() || !content.trim() ? 0.5 : 1,
              borderRadius: '2px',
              display: 'inline-flex', alignItems: 'center', gap: '6px',
              letterSpacing: '0.06em',
            }}
          >
            {saving
              ? <><Loader2 size={11} style={{ animation: 'spin 1s linear infinite' }} /> Creating…</>
              : 'Create & Process'}
          </button>
        </div>
      </div>
    </div>
  );
}
