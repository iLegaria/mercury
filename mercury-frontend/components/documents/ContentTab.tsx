'use client';

import { useState } from 'react';
import { Pencil, Loader2, X, Check } from 'lucide-react';
import { toast } from 'sonner';
import { documentsApi } from '@/lib/documents';
import type { Document } from '@/types';

interface Props {
  doc: Document;
  userId: string;
  active: boolean;
  isReady: boolean;
  onUpdated: (doc: Document) => void;
}

export default function ContentTab({ doc, userId, active, isReady, onUpdated }: Props) {
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(doc.extractedText ?? '');
  const [saving, setSaving] = useState(false);

  async function handleSave() {
    if (!draft.trim()) return;
    setSaving(true);
    try {
      const updated = await documentsApi.updateContent(doc.id, userId, draft.trim());
      onUpdated(updated);
      setEditing(false);
      toast.success('Content updated — document is being re-processed');
    } catch {
      toast.error('Could not update content');
    } finally {
      setSaving(false);
    }
  }

  function handleCancel() {
    setDraft(doc.extractedText ?? '');
    setEditing(false);
  }

  if (!active) return null;

  const wordCount = doc.extractedText
    ? doc.extractedText.trim().split(/\s+/).length
    : null;

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' }}>
        <div style={{ fontSize: '9px', letterSpacing: '0.18em', textTransform: 'uppercase', color: 'var(--text-muted-col)' }}>
          Document Content
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          {wordCount != null && !editing && (
            <span style={{ fontSize: '9px', color: 'var(--text-subtle)' }}>
              ~{wordCount.toLocaleString()} words
            </span>
          )}
          {isReady && !editing && doc.extractedText && (
            <button
              onClick={() => { setDraft(doc.extractedText ?? ''); setEditing(true); }}
              style={{
                display: 'inline-flex', alignItems: 'center', gap: '4px',
                background: 'none', border: 'none', cursor: 'pointer',
                color: 'var(--text-muted-col)', fontSize: '9px',
                letterSpacing: '0.06em', fontFamily: 'inherit', padding: '0',
              }}
            >
              <Pencil size={10} /> Edit
            </button>
          )}
        </div>
      </div>

      {doc.extractedText ? (
        editing ? (
          <>
            <textarea
              value={draft}
              onChange={e => setDraft(e.target.value)}
              style={{
                width: '100%', minHeight: '60vh',
                background: 'var(--card)', border: '1px solid var(--mercury-accent)',
                color: 'var(--foreground)', fontFamily: 'inherit',
                fontSize: '13px', lineHeight: 1.8, padding: '20px 24px',
                resize: 'vertical', outline: 'none', borderRadius: '2px',
                boxSizing: 'border-box',
              }}
            />
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: '10px' }}>
              <span style={{ fontSize: '9px', color: 'var(--text-subtle)' }}>
                Saving will re-process the document with the new content.
              </span>
              <div style={{ display: 'flex', gap: '8px' }}>
                <button
                  onClick={handleCancel}
                  disabled={saving}
                  style={{
                    padding: '8px 16px', background: 'none',
                    border: '1px solid #1c1912', color: 'var(--text-muted-col)',
                    fontSize: '10px', fontFamily: 'inherit', cursor: 'pointer',
                    borderRadius: '2px', display: 'inline-flex', alignItems: 'center', gap: '5px',
                  }}
                >
                  <X size={10} /> Cancel
                </button>
                <button
                  onClick={handleSave}
                  disabled={saving || !draft.trim()}
                  style={{
                    padding: '8px 16px', background: 'var(--accent-dim)',
                    border: '1px solid var(--mercury-accent)', color: 'var(--mercury-accent)',
                    fontSize: '10px', fontFamily: 'inherit',
                    cursor: saving ? 'not-allowed' : 'pointer',
                    opacity: saving ? 0.6 : 1, borderRadius: '2px',
                    display: 'inline-flex', alignItems: 'center', gap: '5px',
                  }}
                >
                  {saving
                    ? <><Loader2 size={10} style={{ animation: 'spin 1s linear infinite' }} /> Saving</>
                    : <><Check size={10} /> Save &amp; Re-process</>}
                </button>
              </div>
            </div>
          </>
        ) : (
          <div style={{
            maxHeight: '70vh', overflowY: 'auto',
            background: 'var(--card)', border: '1px solid #121420',
            padding: '20px 24px', borderRadius: '2px',
          }}>
            <p style={{
              margin: 0, fontSize: '13px', lineHeight: 1.8,
              color: 'var(--foreground)', whiteSpace: 'pre-wrap', wordBreak: 'break-word',
            }}>
              {doc.extractedText}
            </p>
          </div>
        )
      ) : (
        <div style={{
          background: 'var(--card)', border: '1px solid #121420',
          padding: '24px', borderRadius: '2px',
          fontSize: '12px', color: 'var(--text-muted-col)', textAlign: 'center',
        }}>
          Content not available — re-upload the document to enable this feature.
        </div>
      )}
    </div>
  );
}
