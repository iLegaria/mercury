'use client';

import { useState } from 'react';
import { Pencil, Loader2, X, Check } from 'lucide-react';
import { toast } from 'sonner';
import { chunksApi } from '@/lib/chunks';
import type { DocumentChunk } from '@/types';

interface Props {
  chunk: DocumentChunk;
  userId: string;
  onSaved: (updated: DocumentChunk) => void;
  disabled?: boolean;
}

export default function ChunkEditor({ chunk, userId, onSaved, disabled }: Props) {
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(chunk.content);
  const [saving, setSaving] = useState(false);

  async function handleSave() {
    if (!draft.trim()) return;
    setSaving(true);
    try {
      const updated = await chunksApi.update(chunk.chunkId, userId, draft.trim());
      onSaved(updated);
      setEditing(false);
      toast.success('Chunk updated and re-embedded');
    } catch {
      toast.error('Could not update chunk');
    } finally {
      setSaving(false);
    }
  }

  function handleCancel() {
    setDraft(chunk.content);
    setEditing(false);
  }

  return (
    <div style={{ display: 'flex', gap: '14px', padding: '16px 0', borderBottom: '1px solid #121420' }}>
      {/* Index rail */}
      <div style={{
        flexShrink: 0, width: '28px', paddingTop: '2px',
        fontSize: '9px', letterSpacing: '0.08em', color: '#e8952a',
        textAlign: 'right', fontVariantNumeric: 'tabular-nums',
      }}>
        {chunk.chunkIndex + 1}
      </div>

      {/* Content area */}
      <div style={{ flex: 1, minWidth: 0 }}>
        {editing ? (
          <>
            <textarea
              value={draft}
              onChange={e => setDraft(e.target.value)}
              rows={Math.max(4, Math.ceil(draft.length / 100))}
              style={{
                width: '100%', background: 'var(--card)',
                border: '1px solid var(--mercury-accent)',
                color: 'var(--foreground)', fontFamily: 'inherit',
                fontSize: '13px', lineHeight: 1.7, padding: '10px 12px',
                resize: 'vertical', outline: 'none', borderRadius: '2px',
                boxSizing: 'border-box',
              }}
            />
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: '6px' }}>
              <span style={{ fontSize: '9px', color: 'var(--text-subtle)' }}>{draft.length} chars</span>
              <div style={{ display: 'flex', gap: '6px' }}>
                <button
                  onClick={handleCancel}
                  disabled={saving}
                  style={{
                    padding: '5px 12px', background: 'none',
                    border: '1px solid #1c1912', color: 'var(--text-muted-col)',
                    fontSize: '10px', fontFamily: 'inherit', cursor: 'pointer', borderRadius: '2px',
                    display: 'inline-flex', alignItems: 'center', gap: '4px',
                  }}
                >
                  <X size={10} /> Cancel
                </button>
                <button
                  onClick={handleSave}
                  disabled={saving || !draft.trim()}
                  style={{
                    padding: '5px 12px', background: 'var(--accent-dim)',
                    border: '1px solid var(--mercury-accent)', color: 'var(--mercury-accent)',
                    fontSize: '10px', fontFamily: 'inherit', cursor: saving ? 'not-allowed' : 'pointer',
                    opacity: saving ? 0.6 : 1, borderRadius: '2px',
                    display: 'inline-flex', alignItems: 'center', gap: '4px',
                  }}
                >
                  {saving
                    ? <><Loader2 size={10} style={{ animation: 'spin 1s linear infinite' }} /> Saving</>
                    : <><Check size={10} /> Save</>}
                </button>
              </div>
            </div>
          </>
        ) : (
          <div style={{ display: 'flex', alignItems: 'flex-start', gap: '8px' }}>
            <p style={{
              flex: 1, margin: 0, fontSize: '13px', lineHeight: 1.7,
              color: 'var(--foreground)', whiteSpace: 'pre-wrap', wordBreak: 'break-word',
            }}>
              {chunk.content}
            </p>
            {!disabled && (
              <button
                onClick={() => { setDraft(chunk.content); setEditing(true); }}
                title="Edit chunk"
                style={{
                  flexShrink: 0, background: 'none', border: 'none', cursor: 'pointer',
                  color: 'var(--text-muted-col)', padding: '2px', marginTop: '2px',
                  display: 'flex', opacity: 0.6,
                }}
              >
                <Pencil size={12} />
              </button>
            )}
          </div>
        )}
        {!editing && chunk.tokenCount != null && (
          <div style={{ fontSize: '9px', color: 'var(--text-subtle)', marginTop: '4px' }}>
            {chunk.tokenCount} tokens
          </div>
        )}
      </div>
    </div>
  );
}
