'use client';

import { useEffect, useState } from 'react';
import { Plus } from 'lucide-react';
import { toast } from 'sonner';
import { useUser } from '@/context/UserContext';
import { flashcardsApi } from '@/lib/flashcards';
import DeckCard from '@/components/flashcards/DeckCard';
import { SkeletonCard } from '@/components/ui/Skeleton';
import type { FlashcardDeck } from '@/types';

export default function FlashcardsPage() {
  const { userId } = useUser();
  const [decks, setDecks] = useState<FlashcardDeck[]>([]);
  const [loading, setLoading] = useState(true);
  const [showNew, setShowNew] = useState(false);
  const [newName, setNewName] = useState('');
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    flashcardsApi.listDecks(userId)
      .then(setDecks)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [userId]);

  async function createDeck() {
    if (!newName.trim() || creating) return;
    setCreating(true);
    try {
      const deck = await flashcardsApi.createDeck(userId, newName.trim());
      setDecks(prev => [deck, ...prev]);
      setNewName('');
      setShowNew(false);
      toast.success('Deck created');
    } catch { toast.error('Could not create deck'); }
    finally { setCreating(false); }
  }

  function handleDeleted(id: string) {
    setDecks(prev => prev.filter(d => d.id !== id));
  }

  async function handleImported(deckId: string) {
    // Re-fetch the deck to get updated cardCount
    try {
      const updated = await flashcardsApi.listDecks(userId);
      setDecks(updated);
    } catch { /* ignore */ }
  }

  const totalDue = decks.reduce((s, d) => s + d.dueCount, 0);

  return (
    <div>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '28px' }}>
        <div>
          <div style={{ fontSize: '9px', letterSpacing: '0.24em', textTransform: 'uppercase', color: 'var(--mercury-accent)', marginBottom: '6px' }}>
            Spaced Repetition · SM-2
          </div>
          <h1 style={{ fontFamily: 'var(--font-cinzel), Cinzel, serif', fontSize: '26px', fontWeight: 600, letterSpacing: '0.04em', color: 'var(--foreground)' }}>
            Flashcards
          </h1>
        </div>
        <button
          onClick={() => setShowNew(o => !o)}
          style={{
            marginTop: '4px',
            display: 'inline-flex', alignItems: 'center', gap: '6px',
            padding: '9px 18px', border: '1px solid var(--mercury-accent)',
            color: 'var(--mercury-accent)', background: 'transparent',
            fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase',
            fontFamily: 'inherit', cursor: 'pointer', borderRadius: '2px',
          }}
        >
          <Plus size={11} /> New Deck
        </button>
      </div>

      {/* Due summary */}
      {totalDue > 0 && (
        <div style={{ background: 'var(--accent-dim)', border: '1px solid var(--border-accent)', padding: '12px 18px', marginBottom: '24px', borderRadius: '2px', fontSize: '12px', color: 'var(--mercury-accent)' }}>
          {totalDue} {totalDue === 1 ? 'card' : 'cards'} due for review today across {decks.filter(d => d.dueCount > 0).length} {decks.filter(d => d.dueCount > 0).length === 1 ? 'deck' : 'decks'}.
        </div>
      )}

      {/* New deck form */}
      {showNew && (
        <div style={{ display: 'flex', gap: '8px', marginBottom: '20px', padding: '14px 16px', background: 'var(--card)', border: '1px solid #121420', borderRadius: '2px' }}>
          <input
            autoFocus
            type="text"
            placeholder="Deck name"
            value={newName}
            onChange={e => setNewName(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter') createDeck(); if (e.key === 'Escape') setShowNew(false); }}
            style={{
              flex: 1, background: 'var(--background)', border: '1px solid #1c1912',
              color: 'var(--foreground)', padding: '8px 12px', fontSize: '13px',
              fontFamily: 'inherit', outline: 'none', borderRadius: '2px',
            }}
          />
          <button
            onClick={createDeck}
            disabled={creating || !newName.trim()}
            style={{ padding: '8px 18px', border: '1px solid var(--mercury-accent)', color: 'var(--mercury-accent)', background: 'transparent', fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase', fontFamily: 'inherit', cursor: 'pointer', borderRadius: '2px' }}
          >
            {creating ? '...' : 'Create'}
          </button>
          <button
            onClick={() => setShowNew(false)}
            style={{ padding: '8px 14px', border: '1px solid #1c1912', color: 'var(--text-muted-col)', background: 'transparent', fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase', fontFamily: 'inherit', cursor: 'pointer', borderRadius: '2px' }}
          >
            Cancel
          </button>
        </div>
      )}

      {loading && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '1px', background: '#121420', border: '1px solid #121420' }}>
          {Array.from({ length: 4 }).map((_, i) => <SkeletonCard key={i} />)}
        </div>
      )}

      {!loading && decks.length === 0 && (
        <div style={{ border: '1px solid #121420', background: 'var(--card)', padding: '60px', textAlign: 'center' }}>
          <div style={{ fontFamily: 'var(--font-cinzel), Cinzel, serif', fontSize: '16px', color: 'var(--foreground)', marginBottom: '12px' }}>
            No decks yet
          </div>
          <p style={{ fontSize: '12px', color: 'var(--text-muted-col)', marginBottom: '8px', lineHeight: 1.7 }}>
            Create a deck manually or auto-generate one from a document in the Study Hub.
          </p>
          <p style={{ fontSize: '11px', color: 'var(--text-muted-col)', marginBottom: '28px', lineHeight: 1.7 }}>
            You can also bulk-import cards from a CSV file once a deck is created.<br />
            Format: <code style={{ color: 'var(--mercury-accent)', fontSize: '11px' }}>question,answer</code> — one card per row, no header.
          </p>
          <div style={{ display: 'flex', gap: '10px', justifyContent: 'center', flexWrap: 'wrap' }}>
            <button
              onClick={() => setShowNew(true)}
              style={{ padding: '10px 24px', border: '1px solid var(--mercury-accent)', color: 'var(--mercury-accent)', background: 'transparent', fontSize: '11px', letterSpacing: '0.08em', textTransform: 'uppercase', fontFamily: 'inherit', cursor: 'pointer', borderRadius: '2px' }}
            >
              <Plus size={11} style={{ display: 'inline', marginRight: '6px' }} /> New Deck
            </button>
          </div>
        </div>
      )}

      {!loading && decks.length > 0 && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '1px', background: '#121420', border: '1px solid #121420' }}>
          {decks.map(deck => (
            <DeckCard
              key={deck.id}
              deck={deck}
              userId={userId}
              onDeleted={handleDeleted}
              onImported={handleImported}
            />
          ))}
        </div>
      )}
    </div>
  );
}
