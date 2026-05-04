'use client';

import { useEffect, useState, useCallback } from 'react';
import Link from 'next/link';
import { useUser } from '@/context/UserContext';
import { documentsApi } from '@/lib/documents';
import { collectionsApi } from '@/lib/collections';
import { flashcardsApi } from '@/lib/flashcards';
import StatusBadge from '@/components/documents/StatusBadge';
import { SkeletonStatCard, SkeletonRow } from '@/components/ui/Skeleton';
import type { Document, Collection, FlashcardDeck } from '@/types';

function StatCard({ value, label }: { value: string | number; label: string }) {
  return (
    <div style={{
      background: 'var(--card)',
      border: '1px solid #121420',
      padding: '24px 28px',
      flex: 1,
    }}>
      <div style={{ fontFamily: 'var(--font-cinzel), Cinzel, serif', fontSize: '30px', fontWeight: 700, color: 'var(--mercury-accent)', lineHeight: 1, marginBottom: '6px' }}>
        {value}
      </div>
      <div style={{ fontSize: '10px', color: 'var(--text-muted-col)', letterSpacing: '0.06em' }}>
        {label}
      </div>
    </div>
  );
}

function SectionTitle({ label, title }: { label: string; title: string }) {
  return (
    <div style={{ marginBottom: '20px' }}>
      <div style={{ fontSize: '9px', letterSpacing: '0.22em', textTransform: 'uppercase', color: 'var(--mercury-accent)', marginBottom: '4px' }}>{label}</div>
      <h2 style={{ fontFamily: 'var(--font-cinzel), Cinzel, serif', fontSize: '18px', fontWeight: 600, letterSpacing: '0.04em', color: 'var(--foreground)' }}>{title}</h2>
    </div>
  );
}

export default function DashboardPage() {
  const { userId, userName } = useUser();
  const [docs, setDocs] = useState<Document[]>([]);
  const [collections, setCollections] = useState<Collection[]>([]);
  const [decks, setDecks] = useState<FlashcardDeck[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchData = useCallback(async () => {
    if (!userId) return;
    try {
      const [d, c, dk] = await Promise.all([
        documentsApi.list(userId),
        collectionsApi.list(userId),
        flashcardsApi.listDecks(userId),
      ]);
      setDocs(d);
      setCollections(c);
      setDecks(dk);
    } catch {
      // Backend unreachable — keep empty state
    } finally {
      setLoading(false);
    }
  }, [userId]);

  useEffect(() => { fetchData(); }, [fetchData]);

  // Poll every 5s while any doc is PROCESSING
  useEffect(() => {
    const hasProcessing = docs.some(d => d.status === 'PROCESSING' || d.status === 'PENDING');
    if (!hasProcessing) return;
    const id = setInterval(() => { fetchData(); }, 5000);
    return () => clearInterval(id);
  }, [docs, fetchData]);

  const totalDue = decks.reduce((sum, d) => sum + d.dueCount, 0);
  const totalCards = decks.reduce((sum, d) => sum + d.cardCount, 0);
  const recent = docs.slice().sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()).slice(0, 5);
  const decksDue = decks.filter(d => d.dueCount > 0);

  if (loading) {
    return (
      <div>
        <div style={{ height: '36px', marginBottom: '36px' }} />
        <div style={{ display: 'flex', gap: '1px', background: '#121420', border: '1px solid #121420', marginBottom: '52px' }}>
          {Array.from({ length: 4 }).map((_, i) => <SkeletonStatCard key={i} />)}
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1px', background: '#121420', border: '1px solid #121420' }}>
          {Array.from({ length: 5 }).map((_, i) => <SkeletonRow key={i} />)}
        </div>
      </div>
    );
  }

  return (
    <div>
      {/* Header */}
      <div style={{ marginBottom: '8px', fontSize: '9px', letterSpacing: '0.24em', textTransform: 'uppercase', color: 'var(--mercury-accent)' }}>
        Knowledge Base
      </div>
      <h1 style={{ fontFamily: 'var(--font-cinzel), Cinzel, serif', fontSize: '26px', fontWeight: 600, letterSpacing: '0.04em', color: 'var(--foreground)', marginBottom: '36px' }}>
        {userName ? `Welcome back, ${userName.split(' ')[0]}` : 'Dashboard'}
      </h1>

      {/* Stats */}
      <div style={{ display: 'flex', gap: '1px', background: '#121420', border: '1px solid #121420', marginBottom: '52px' }}>
        <StatCard value={docs.length} label="Documents" />
        <StatCard value={collections.length} label="Collections" />
        <StatCard value={totalDue} label="Cards Due Today" />
        <StatCard value={totalCards} label="Total Cards" />
      </div>

      {/* Empty state */}
      {docs.length === 0 && (
        <div style={{
          border: '1px solid #121420',
          background: 'var(--card)',
          padding: '48px',
          textAlign: 'center',
          marginBottom: '40px',
        }}>
          <div style={{ fontFamily: 'var(--font-cinzel), Cinzel, serif', fontSize: '16px', color: 'var(--foreground)', marginBottom: '12px' }}>
            Your knowledge base is empty
          </div>
          <p style={{ fontSize: '12px', color: 'var(--text-muted-col)', marginBottom: '28px' }}>
            Upload a document to get started — PDF, DOCX, or plain text.
          </p>
          <Link href="/documents" style={{
            display: 'inline-block',
            padding: '10px 24px',
            border: '1px solid var(--mercury-accent)',
            color: 'var(--mercury-accent)',
            textDecoration: 'none',
            fontSize: '11px',
            letterSpacing: '0.08em',
            textTransform: 'uppercase',
          }}>
            Upload your first document →
          </Link>
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '40px' }}>
        {/* Recent Documents */}
        {docs.length > 0 && (
          <div>
            <SectionTitle label="Library" title="Recent Documents" />
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1px', background: '#121420', border: '1px solid #121420' }}>
              {recent.map(doc => (
                <div key={doc.id} style={{
                  background: 'var(--card)',
                  padding: '14px 18px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  gap: '12px',
                }}>
                  <div style={{ minWidth: 0 }}>
                    <div style={{ fontSize: '12.5px', color: 'var(--foreground)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {doc.title}
                    </div>
                    <div style={{ fontSize: '10px', color: 'var(--text-muted-col)', marginTop: '2px' }}>
                      {doc.sourceType} · {new Date(doc.createdAt).toLocaleDateString()}
                    </div>
                  </div>
                  <StatusBadge status={doc.status} />
                </div>
              ))}
            </div>
            {docs.length > 5 && (
              <Link href="/documents" style={{ display: 'block', marginTop: '10px', fontSize: '10px', color: 'var(--text-muted-col)', textDecoration: 'none', letterSpacing: '0.06em' }}>
                View all {docs.length} documents →
              </Link>
            )}
          </div>
        )}

        {/* Due for Review */}
        {decks.length > 0 && (
          <div>
            <SectionTitle label="Flashcards" title="Due for Review" />
            {decksDue.length === 0 ? (
              <div style={{ border: '1px solid #121420', background: 'var(--card)', padding: '24px 18px', fontSize: '12px', color: 'var(--text-muted-col)' }}>
                All caught up — no cards due today.
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1px', background: '#121420', border: '1px solid #121420' }}>
                {decksDue.map(deck => (
                  <Link key={deck.id} href={`/flashcards/${deck.id}`} style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    background: 'var(--card)',
                    padding: '14px 18px',
                    textDecoration: 'none',
                    transition: 'background 0.15s',
                  }}>
                    <span style={{ fontSize: '12.5px', color: 'var(--foreground)' }}>{deck.name}</span>
                    <span style={{
                      fontSize: '9px',
                      letterSpacing: '0.06em',
                      padding: '2px 8px',
                      background: 'var(--accent-dim)',
                      border: '1px solid var(--border-accent)',
                      color: 'var(--mercury-accent)',
                      borderRadius: '2px',
                    }}>
                      {deck.dueCount} DUE
                    </span>
                  </Link>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
