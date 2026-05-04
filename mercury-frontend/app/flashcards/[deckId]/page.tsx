'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import Link from 'next/link';
import { useUser } from '@/context/UserContext';
import { flashcardsApi } from '@/lib/flashcards';
import FlashCard from '@/components/flashcards/FlashCard';
import ReviewControls from '@/components/flashcards/ReviewControls';
import type { FlashcardCard } from '@/types';

export default function ReviewPage() {
  const { deckId } = useParams<{ deckId: string }>();
  const { userId } = useUser();
  const [cards, setCards] = useState<FlashcardCard[]>([]);
  const [index, setIndex] = useState(0);
  const [flipped, setFlipped] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(false);
  const [loading, setLoading] = useState(true);
  const [reviewed, setReviewed] = useState(0);

  useEffect(() => {
    flashcardsApi.getDueCards(deckId, userId)
      .then(due => {
        if (due.length === 0) {
          // No due cards — fall back to all cards for browsing
          return flashcardsApi.getCards(deckId, userId);
        }
        return due;
      })
      .then(c => { setCards(c); if (c.length === 0) setDone(true); })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [deckId, userId]);

  async function handleReview(quality: number) {
    const card = cards[index];
    setSubmitting(true);
    try {
      await flashcardsApi.reviewCard(card.id, userId, quality);
      setReviewed(r => r + 1);
      const next = index + 1;
      if (next >= cards.length) {
        setDone(true);
      } else {
        setIndex(next);
        setFlipped(false);
      }
    } catch { /* ignore */ }
    finally { setSubmitting(false); }
  }

  if (loading) {
    return <div style={{ color: 'var(--text-muted-col)', fontSize: '11px', letterSpacing: '0.1em', paddingTop: '40px' }}>LOADING...</div>;
  }

  if (done) {
    return (
      <div style={{ maxWidth: '480px', textAlign: 'center', paddingTop: '40px' }}>
        <div style={{ fontSize: '48px', marginBottom: '16px' }}>🌑</div>
        <div style={{ fontFamily: 'var(--font-cinzel), Cinzel, serif', fontSize: '22px', fontWeight: 600, letterSpacing: '0.04em', color: 'var(--foreground)', marginBottom: '12px' }}>
          All caught up for today.
        </div>
        <p style={{ fontSize: '12px', color: 'var(--text-muted-col)', lineHeight: 1.8, marginBottom: '32px' }}>
          {reviewed > 0
            ? `You reviewed ${reviewed} ${reviewed === 1 ? 'card' : 'cards'}. The algorithm has scheduled your next review.`
            : 'No cards due right now. Come back later.'}
        </p>
        <Link href="/flashcards" style={{
          display: 'inline-block', padding: '10px 24px',
          border: '1px solid var(--mercury-accent)', color: 'var(--mercury-accent)',
          textDecoration: 'none', fontSize: '11px', letterSpacing: '0.08em', textTransform: 'uppercase',
          borderRadius: '2px',
        }}>
          ← Back to Decks
        </Link>
      </div>
    );
  }

  const card = cards[index];

  return (
    <div style={{ maxWidth: '600px' }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px' }}>
        <div>
          <div style={{ fontSize: '9px', letterSpacing: '0.22em', textTransform: 'uppercase', color: 'var(--mercury-accent)', marginBottom: '4px' }}>
            Flashcards · SM-2
          </div>
          <Link href="/flashcards" style={{ fontSize: '11px', color: 'var(--text-muted-col)', textDecoration: 'none', letterSpacing: '0.04em' }}>
            ← Decks
          </Link>
        </div>
        <div style={{ textAlign: 'right' }}>
          <div style={{ fontFamily: 'var(--font-cinzel), Cinzel, serif', fontSize: '22px', fontWeight: 600, color: 'var(--mercury-accent)' }}>
            {index + 1}<span style={{ fontSize: '14px', color: 'var(--text-muted-col)' }}>/{cards.length}</span>
          </div>
          <div style={{ fontSize: '9px', color: 'var(--text-muted-col)', letterSpacing: '0.06em' }}>
            {reviewed} reviewed
          </div>
        </div>
      </div>

      {/* Progress bar */}
      <div style={{ height: '2px', background: '#1c1912', marginBottom: '28px', borderRadius: '1px', overflow: 'hidden' }}>
        <div style={{ height: '100%', width: `${(index / cards.length) * 100}%`, background: 'var(--mercury-accent)', transition: 'width 0.35s ease' }} />
      </div>

      {/* Flash card */}
      <FlashCard card={card} flipped={flipped} onReveal={() => setFlipped(true)} />

      {/* Review controls — only show after flip */}
      {flipped && (
        <ReviewControls card={card} onReview={handleReview} disabled={submitting} />
      )}

      {submitting && (
        <div style={{ marginTop: '12px', fontSize: '10px', color: 'var(--text-muted-col)', letterSpacing: '0.08em', textAlign: 'center' }}>
          Saving...
        </div>
      )}
    </div>
  );
}
