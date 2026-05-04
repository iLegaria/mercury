'use client';

import { useRef, useState } from 'react';
import Link from 'next/link';
import { MessageCircle, Settings2, Trash2, Upload, X } from 'lucide-react';
import { toast } from 'sonner';
import { flashcardsApi } from '@/lib/flashcards';
import type { FlashcardDeck } from '@/types';

interface Props {
  deck: FlashcardDeck;
  userId: string;
  onDeleted: (id: string) => void;
  onImported: (deckId: string) => void;
}

interface PreviewCard { question: string; answer: string; }

function parseCsvPreview(text: string): PreviewCard[] {
  return text
    .split('\n')
    .map(line => line.trim())
    .filter(line => line.length > 0)
    .map(line => {
      const commaIdx = line.indexOf(',');
      if (commaIdx < 0) return null;
      return {
        question: line.slice(0, commaIdx).trim().replace(/^"|"$/g, ''),
        answer: line.slice(commaIdx + 1).trim().replace(/^"|"$/g, ''),
      };
    })
    .filter((c): c is PreviewCard => c !== null && c.question.length > 0 && c.answer.length > 0);
}

export default function DeckCard({ deck, userId, onDeleted, onImported }: Props) {
  const fileRef = useRef<HTMLInputElement>(null);
  const [showModal, setShowModal] = useState(false);
  const [pendingFile, setPendingFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<PreviewCard[]>([]);
  const [importing, setImporting] = useState(false);
  const [reminderEnabled, setReminderEnabled] = useState(deck.whatsappReminderEnabled);
  const [togglingReminder, setTogglingReminder] = useState(false);

  async function handleDelete() {
    if (!confirm(`Delete deck "${deck.name}" and all its cards?`)) return;
    try {
      await flashcardsApi.deleteDeck(deck.id, userId);
      onDeleted(deck.id);
      toast.success('Deck deleted');
    } catch { toast.error('Could not delete deck'); }
  }

  function openModal() {
    setPendingFile(null);
    setPreview([]);
    setShowModal(true);
  }

  function closeModal() {
    setShowModal(false);
    setPendingFile(null);
    setPreview([]);
    if (fileRef.current) fileRef.current.value = '';
  }

  async function handleFileSelected(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    setPendingFile(file);
    const text = await file.text();
    setPreview(parseCsvPreview(text));
  }

  async function toggleReminder() {
    if (togglingReminder) return;
    setTogglingReminder(true);
    const next = !reminderEnabled;
    try {
      await flashcardsApi.setWhatsappReminder(deck.id, userId, next);
      setReminderEnabled(next);
      toast.success(next ? 'WhatsApp reminders enabled' : 'WhatsApp reminders disabled');
    } catch { toast.error('Could not update reminder setting'); }
    finally { setTogglingReminder(false); }
  }

  async function confirmImport() {
    if (!pendingFile || importing) return;
    setImporting(true);
    try {
      await flashcardsApi.importCsv(deck.id, userId, pendingFile);
      onImported(deck.id);
      toast.success(`${preview.length} cards imported`);
      closeModal();
    } catch { toast.error('Import failed — check CSV format'); }
    finally { setImporting(false); }
  }

  const hasDue = deck.dueCount > 0;

  return (
    <>
      <div style={{
        background: 'var(--card)',
        border: '1px solid #121420',
        borderLeft: `2px solid ${hasDue ? 'var(--mercury-accent)' : '#252118'}`,
        padding: '20px 22px',
        display: 'flex',
        flexDirection: 'column',
        gap: '12px',
      }}>
        {/* Name + due badge */}
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: '10px' }}>
          <div style={{ fontFamily: 'var(--font-cinzel), Cinzel, serif', fontSize: '15px', fontWeight: 600, letterSpacing: '0.03em', color: 'var(--foreground)', lineHeight: 1.3 }}>
            {deck.name}
          </div>
          {hasDue && (
            <span style={{
              flexShrink: 0,
              fontSize: '9px', letterSpacing: '0.08em', padding: '2px 8px',
              background: 'var(--accent-dim)', border: '1px solid var(--border-accent)',
              color: 'var(--mercury-accent)', borderRadius: '2px',
            }}>
              {deck.dueCount} DUE
            </span>
          )}
        </div>

        {/* Stats */}
        <div style={{ fontSize: '10px', color: 'var(--text-muted-col)' }}>
          {deck.cardCount} {deck.cardCount === 1 ? 'card' : 'cards'} · created {new Date(deck.createdAt).toLocaleDateString()}
        </div>

        {/* WhatsApp reminder toggle */}
        <div
          onClick={toggleReminder}
          style={{
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            padding: '8px 10px', borderRadius: '2px',
            background: reminderEnabled ? 'var(--accent-dim)' : 'transparent',
            border: `1px solid ${reminderEnabled ? 'var(--border-accent)' : '#1c1912'}`,
            cursor: togglingReminder ? 'wait' : 'pointer',
            transition: 'all 0.2s',
            opacity: togglingReminder ? 0.6 : 1,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <MessageCircle size={11} color={reminderEnabled ? 'var(--mercury-accent)' : 'var(--text-muted-col)'} />
            <span style={{ fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase', color: reminderEnabled ? 'var(--mercury-accent)' : 'var(--text-muted-col)' }}>
              WhatsApp reminders
            </span>
          </div>
          <div style={{
            width: '28px', height: '14px', borderRadius: '7px',
            background: reminderEnabled ? 'var(--mercury-accent)' : '#1c1912',
            position: 'relative', transition: 'background 0.2s', flexShrink: 0,
          }}>
            <div style={{
              position: 'absolute', top: '2px',
              left: reminderEnabled ? '16px' : '2px',
              width: '10px', height: '10px', borderRadius: '50%',
              background: reminderEnabled ? '#000' : 'var(--text-muted-col)',
              transition: 'left 0.2s',
            }} />
          </div>
        </div>

        {/* Actions */}
        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginTop: '4px' }}>
          {hasDue ? (
            <Link href={`/flashcards/${deck.id}`} style={{
              flex: 1, textAlign: 'center', padding: '8px 14px',
              border: '1px solid var(--mercury-accent)', color: 'var(--mercury-accent)',
              textDecoration: 'none', fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase',
              borderRadius: '2px', transition: 'all 0.2s',
            }}>
              Review {deck.dueCount} →
            </Link>
          ) : (
            <Link href={`/flashcards/${deck.id}`} style={{
              flex: 1, textAlign: 'center', padding: '8px 14px',
              border: '1px solid #1c1912', color: 'var(--text-muted-col)',
              textDecoration: 'none', fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase',
              borderRadius: '2px',
            }}>
              Browse All
            </Link>
          )}
          <Link href={`/flashcards/${deck.id}/manage`} style={{
            padding: '8px 12px', border: '1px solid #1c1912', color: 'var(--text-muted-col)',
            textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '5px',
            fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase', borderRadius: '2px',
          }}>
            <Settings2 size={11} /> Manage
          </Link>
          <button
            onClick={openModal}
            style={{ padding: '8px 12px', border: '1px solid #1c1912', background: 'none', color: 'var(--text-muted-col)', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '5px', fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase', fontFamily: 'inherit', borderRadius: '2px' }}
          >
            <Upload size={11} /> Import CSV
          </button>
          <button
            onClick={handleDelete}
            title="Delete deck"
            style={{ padding: '8px 12px', border: '1px solid #1c1912', background: 'none', color: 'var(--error)', cursor: 'pointer', display: 'flex', alignItems: 'center', borderRadius: '2px' }}
          >
            <Trash2 size={12} />
          </button>
          <input ref={fileRef} type="file" accept=".csv" style={{ display: 'none' }} onChange={handleFileSelected} />
        </div>
      </div>

      {/* Import modal */}
      {showModal && (
        <div
          onClick={e => { if (e.target === e.currentTarget) closeModal(); }}
          style={{
            position: 'fixed', inset: 0, zIndex: 50,
            background: 'rgba(0,0,0,0.7)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            padding: '20px',
          }}
        >
          <div style={{
            background: 'var(--card)', border: '1px solid #1c1912',
            borderTop: '2px solid var(--mercury-accent)',
            width: '100%', maxWidth: '560px', maxHeight: '80vh',
            display: 'flex', flexDirection: 'column',
            borderRadius: '2px',
          }}>
            {/* Modal header */}
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '18px 22px', borderBottom: '1px solid #121420' }}>
              <div>
                <div style={{ fontSize: '9px', letterSpacing: '0.2em', textTransform: 'uppercase', color: 'var(--mercury-accent)', marginBottom: '3px' }}>
                  Import Cards
                </div>
                <div style={{ fontFamily: 'var(--font-cinzel), Cinzel, serif', fontSize: '15px', fontWeight: 600, color: 'var(--foreground)' }}>
                  {deck.name}
                </div>
              </div>
              <button onClick={closeModal} style={{ background: 'none', border: 'none', color: 'var(--text-muted-col)', cursor: 'pointer', padding: '4px' }}>
                <X size={16} />
              </button>
            </div>

            {/* Modal body */}
            <div style={{ padding: '20px 22px', overflowY: 'auto', flex: 1, display: 'flex', flexDirection: 'column', gap: '16px' }}>

              {/* Format description */}
              <div style={{ background: 'var(--background)', border: '1px solid #121420', padding: '14px 16px', borderRadius: '2px' }}>
                <div style={{ fontSize: '9px', letterSpacing: '0.16em', textTransform: 'uppercase', color: 'var(--text-muted-col)', marginBottom: '8px' }}>
                  CSV Format
                </div>
                <p style={{ fontSize: '11px', color: 'var(--text-muted-col)', lineHeight: 1.7, margin: '0 0 10px' }}>
                  One card per row. First column = question (front), second column = answer (back). No header row needed.
                </p>
                <pre style={{ fontSize: '11px', color: 'var(--mercury-accent)', background: '#0a0c14', padding: '10px 12px', borderRadius: '2px', margin: 0, overflowX: 'auto', lineHeight: 1.6 }}>
{`What is the supreme law of the land?,The Constitution
How many amendments does the Constitution have?,27
Who wrote the Declaration of Independence?,Thomas Jefferson`}
                </pre>
              </div>

              {/* File picker */}
              {!pendingFile ? (
                <button
                  onClick={() => fileRef.current?.click()}
                  style={{
                    width: '100%', padding: '28px', border: '1px dashed #1c1912',
                    background: 'none', color: 'var(--text-muted-col)', cursor: 'pointer',
                    display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px',
                    fontSize: '11px', letterSpacing: '0.08em', textTransform: 'uppercase',
                    fontFamily: 'inherit', borderRadius: '2px',
                  }}
                >
                  <Upload size={18} style={{ color: 'var(--mercury-accent)' }} />
                  Choose CSV file
                </button>
              ) : (
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 14px', background: 'var(--background)', border: '1px solid #1c1912', borderRadius: '2px' }}>
                  <span style={{ fontSize: '12px', color: 'var(--foreground)' }}>{pendingFile.name}</span>
                  <button onClick={() => fileRef.current?.click()} style={{ background: 'none', border: 'none', color: 'var(--mercury-accent)', fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase', fontFamily: 'inherit', cursor: 'pointer' }}>
                    Change
                  </button>
                </div>
              )}

              {/* Preview */}
              {preview.length > 0 && (
                <div>
                  <div style={{ fontSize: '9px', letterSpacing: '0.16em', textTransform: 'uppercase', color: 'var(--text-muted-col)', marginBottom: '8px' }}>
                    Preview · {preview.length} cards detected
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                    {preview.slice(0, 5).map((card, i) => {
                      const answers = card.answer.split('|').map(a => a.trim()).filter(Boolean);
                      return (
                        <div key={i} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1px', background: '#121420', border: '1px solid #121420', borderRadius: '2px', overflow: 'hidden', fontSize: '11px' }}>
                          <div style={{ background: 'var(--background)', padding: '8px 10px', color: 'var(--foreground)', lineHeight: 1.4 }}>{card.question}</div>
                          <div style={{ background: 'var(--card)', padding: '8px 10px', lineHeight: 1.4 }}>
                            {answers.length > 1 ? (
                              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px' }}>
                                {answers.map((a, j) => (
                                  <span key={j} style={{ fontSize: '10px', padding: '1px 6px', background: 'var(--accent-dim)', border: '1px solid var(--border-accent)', color: 'var(--mercury-accent)', borderRadius: '2px' }}>{a}</span>
                                ))}
                              </div>
                            ) : (
                              <span style={{ color: 'var(--text-muted-col)' }}>{card.answer}</span>
                            )}
                          </div>
                        </div>
                      );
                    })}
                    {preview.length > 5 && (
                      <div style={{ fontSize: '10px', color: 'var(--text-muted-col)', textAlign: 'center', padding: '4px' }}>
                        +{preview.length - 5} more cards
                      </div>
                    )}
                  </div>
                </div>
              )}

              {pendingFile && preview.length === 0 && (
                <p style={{ fontSize: '11px', color: 'var(--error)', margin: 0 }}>
                  No valid cards detected. Check that each row has question,answer separated by a comma.
                </p>
              )}
            </div>

            {/* Modal footer */}
            <div style={{ padding: '14px 22px', borderTop: '1px solid #121420', display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
              <button onClick={closeModal} style={{ padding: '9px 20px', border: '1px solid #1c1912', background: 'none', color: 'var(--text-muted-col)', fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase', fontFamily: 'inherit', cursor: 'pointer', borderRadius: '2px' }}>
                Cancel
              </button>
              <button
                onClick={confirmImport}
                disabled={!pendingFile || preview.length === 0 || importing}
                style={{
                  padding: '9px 22px', border: '1px solid var(--mercury-accent)',
                  background: 'transparent', color: 'var(--mercury-accent)',
                  fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase',
                  fontFamily: 'inherit', cursor: (!pendingFile || preview.length === 0 || importing) ? 'not-allowed' : 'pointer',
                  opacity: (!pendingFile || preview.length === 0 || importing) ? 0.5 : 1,
                  borderRadius: '2px',
                }}
              >
                {importing ? 'Importing...' : `Import ${preview.length} Cards →`}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
