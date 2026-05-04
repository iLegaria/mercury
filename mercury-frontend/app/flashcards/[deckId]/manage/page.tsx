'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import Link from 'next/link';
import { Check, Pencil, Plus, Trash2, X } from 'lucide-react';
import { toast } from 'sonner';
import { useUser } from '@/context/UserContext';
import { flashcardsApi } from '@/lib/flashcards';
import type { FlashcardCard } from '@/types';

const taStyle: React.CSSProperties = {
  width: '100%',
  background: 'var(--background)',
  border: '1px solid var(--mercury-accent)',
  color: 'var(--foreground)',
  padding: '8px 10px',
  fontSize: '12px',
  fontFamily: 'inherit',
  resize: 'vertical',
  lineHeight: 1.5,
  borderRadius: '2px',
  outline: 'none',
  boxSizing: 'border-box',
};

export default function ManagePage() {
  const { deckId } = useParams<{ deckId: string }>();
  const { userId } = useUser();

  const [cards, setCards] = useState<FlashcardCard[]>([]);
  const [loading, setLoading] = useState(true);

  const [editingId, setEditingId] = useState<string | null>(null);
  const [editQ, setEditQ] = useState('');
  const [editA, setEditA] = useState('');
  const [saving, setSaving] = useState(false);

  const [showAdd, setShowAdd] = useState(false);
  const [newQ, setNewQ] = useState('');
  const [newA, setNewA] = useState('');
  const [adding, setAdding] = useState(false);

  useEffect(() => {
    flashcardsApi.getCards(deckId, userId)
      .then(setCards)
      .catch(() => toast.error('Failed to load cards'))
      .finally(() => setLoading(false));
  }, [deckId, userId]);

  function startEdit(card: FlashcardCard) {
    setEditingId(card.id);
    setEditQ(card.question);
    setEditA(card.answer);
  }

  function cancelEdit() {
    setEditingId(null);
    setEditQ('');
    setEditA('');
  }

  async function saveEdit(cardId: string) {
    if (!editQ.trim() || !editA.trim()) return;
    setSaving(true);
    try {
      const updated = await flashcardsApi.updateCard(cardId, userId, editQ, editA);
      setCards(cs => cs.map(c => c.id === cardId ? updated : c));
      cancelEdit();
    } catch { toast.error('Failed to save changes'); }
    finally { setSaving(false); }
  }

  async function handleDelete(cardId: string) {
    if (!confirm('Delete this card?')) return;
    try {
      await flashcardsApi.deleteCard(cardId, userId);
      setCards(cs => cs.filter(c => c.id !== cardId));
      toast.success('Card deleted');
    } catch { toast.error('Could not delete card'); }
  }

  async function handleAdd() {
    if (!newQ.trim() || !newA.trim() || adding) return;
    setAdding(true);
    try {
      const card = await flashcardsApi.addCard(deckId, userId, newQ, newA);
      setCards(cs => [...cs, card]);
      setNewQ('');
      setNewA('');
      setShowAdd(false);
      toast.success('Card added');
    } catch { toast.error('Failed to add card'); }
    finally { setAdding(false); }
  }

  return (
    <div style={{ maxWidth: '720px' }}>
      {/* Header */}
      <div style={{ marginBottom: '28px' }}>
        <div style={{ fontSize: '9px', letterSpacing: '0.22em', textTransform: 'uppercase', color: 'var(--mercury-accent)', marginBottom: '6px' }}>
          Flashcards · Manage
        </div>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px' }}>
          <Link href="/flashcards" style={{ fontSize: '11px', color: 'var(--text-muted-col)', textDecoration: 'none', letterSpacing: '0.04em' }}>
            ← Decks
          </Link>
          {!loading && (
            <span style={{ fontSize: '10px', color: 'var(--text-muted-col)' }}>
              {cards.length} {cards.length === 1 ? 'card' : 'cards'}
            </span>
          )}
        </div>
      </div>

      {/* Add card */}
      {!showAdd ? (
        <button
          onClick={() => setShowAdd(true)}
          style={{
            width: '100%', padding: '10px', marginBottom: '20px',
            border: '1px dashed #1c1912', background: 'none',
            color: 'var(--text-muted-col)', cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px',
            fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase',
            fontFamily: 'inherit', borderRadius: '2px',
          }}
        >
          <Plus size={12} /> Add Card
        </button>
      ) : (
        <div style={{
          background: 'var(--card)', border: '1px solid var(--mercury-accent)',
          padding: '16px 18px', marginBottom: '20px', borderRadius: '2px',
          display: 'flex', flexDirection: 'column', gap: '10px',
        }}>
          <div style={{ fontSize: '9px', letterSpacing: '0.16em', textTransform: 'uppercase', color: 'var(--mercury-accent)' }}>
            New Card
          </div>
          <textarea
            placeholder="Question"
            value={newQ}
            onChange={e => setNewQ(e.target.value)}
            rows={2}
            style={taStyle}
          />
          <textarea
            placeholder="Answer"
            value={newA}
            onChange={e => setNewA(e.target.value)}
            rows={2}
            style={taStyle}
          />
          <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
            <button
              onClick={() => { setShowAdd(false); setNewQ(''); setNewA(''); }}
              style={{ padding: '7px 16px', border: '1px solid #1c1912', background: 'none', color: 'var(--text-muted-col)', fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase', fontFamily: 'inherit', cursor: 'pointer', borderRadius: '2px' }}
            >
              Cancel
            </button>
            <button
              onClick={handleAdd}
              disabled={!newQ.trim() || !newA.trim() || adding}
              style={{
                padding: '7px 18px', border: '1px solid var(--mercury-accent)',
                background: 'transparent', color: 'var(--mercury-accent)',
                fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase',
                fontFamily: 'inherit', borderRadius: '2px',
                cursor: (!newQ.trim() || !newA.trim() || adding) ? 'not-allowed' : 'pointer',
                opacity: (!newQ.trim() || !newA.trim() || adding) ? 0.5 : 1,
              }}
            >
              {adding ? 'Adding...' : 'Add →'}
            </button>
          </div>
        </div>
      )}

      {/* Card list */}
      {loading ? (
        <div style={{ color: 'var(--text-muted-col)', fontSize: '11px', letterSpacing: '0.1em', paddingTop: '20px' }}>
          LOADING...
        </div>
      ) : cards.length === 0 ? (
        <div style={{ textAlign: 'center', paddingTop: '40px', color: 'var(--text-muted-col)', fontSize: '12px', lineHeight: 1.7 }}>
          No cards yet.<br />Add one above or import a CSV from the deck list.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '3px' }}>
          {cards.map((card, i) => (
            <div
              key={card.id}
              style={{
                background: 'var(--card)',
                border: '1px solid #121420',
                borderLeft: `2px solid ${editingId === card.id ? 'var(--mercury-accent)' : '#252118'}`,
                borderRadius: '2px',
                overflow: 'hidden',
              }}
            >
              {editingId === card.id ? (
                <div style={{ padding: '14px 16px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  <textarea
                    value={editQ}
                    onChange={e => setEditQ(e.target.value)}
                    rows={2}
                    style={taStyle}
                  />
                  <textarea
                    value={editA}
                    onChange={e => setEditA(e.target.value)}
                    rows={2}
                    style={taStyle}
                  />
                  <div style={{ display: 'flex', gap: '6px', justifyContent: 'flex-end' }}>
                    <button
                      onClick={cancelEdit}
                      style={{ padding: '5px 12px', border: '1px solid #1c1912', background: 'none', color: 'var(--text-muted-col)', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px', fontSize: '10px', letterSpacing: '0.06em', textTransform: 'uppercase', fontFamily: 'inherit', borderRadius: '2px' }}
                    >
                      <X size={11} /> Cancel
                    </button>
                    <button
                      onClick={() => saveEdit(card.id)}
                      disabled={saving || !editQ.trim() || !editA.trim()}
                      style={{
                        padding: '5px 14px', border: '1px solid var(--mercury-accent)',
                        background: 'transparent', color: 'var(--mercury-accent)',
                        cursor: saving ? 'wait' : 'pointer',
                        display: 'flex', alignItems: 'center', gap: '4px',
                        fontSize: '10px', letterSpacing: '0.06em', textTransform: 'uppercase',
                        fontFamily: 'inherit', borderRadius: '2px',
                        opacity: saving ? 0.6 : 1,
                      }}
                    >
                      <Check size={11} /> {saving ? 'Saving...' : 'Save'}
                    </button>
                  </div>
                </div>
              ) : (
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr auto', alignItems: 'stretch' }}>
                  <div style={{ padding: '12px 14px', borderRight: '1px solid #121420', fontSize: '12px', color: 'var(--foreground)', lineHeight: 1.5 }}>
                    <div style={{ fontSize: '8px', letterSpacing: '0.12em', textTransform: 'uppercase', color: 'var(--text-muted-col)', marginBottom: '4px' }}>
                      Q · {i + 1}
                    </div>
                    {card.question}
                  </div>
                  <div style={{ padding: '12px 14px', borderRight: '1px solid #121420', fontSize: '12px', color: 'var(--text-muted-col)', lineHeight: 1.5 }}>
                    <div style={{ fontSize: '8px', letterSpacing: '0.12em', textTransform: 'uppercase', color: 'var(--text-muted-col)', marginBottom: '4px' }}>
                      A
                    </div>
                    {card.answer}
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column' }}>
                    <button
                      onClick={() => startEdit(card)}
                      title="Edit"
                      style={{ flex: 1, padding: '8px 12px', border: 'none', background: 'none', color: 'var(--text-muted-col)', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                    >
                      <Pencil size={13} />
                    </button>
                    <button
                      onClick={() => handleDelete(card.id)}
                      title="Delete"
                      style={{ flex: 1, padding: '8px 12px', border: 'none', background: 'none', color: 'var(--error)', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                    >
                      <Trash2 size={13} />
                    </button>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
