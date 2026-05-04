'use client';

import { useEffect, useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { Plus, Upload, FileText } from 'lucide-react';
import { toast } from 'sonner';
import { useUser } from '@/context/UserContext';
import { SkeletonCard } from '@/components/ui/Skeleton';
import { documentsApi } from '@/lib/documents';
import { collectionsApi } from '@/lib/collections';
import DocumentCard from '@/components/documents/DocumentCard';
import UploadZone from '@/components/documents/UploadZone';
import type { Document, Collection } from '@/types';

function Btn({ onClick, children, variant = 'ghost' }: { onClick: () => void; children: React.ReactNode; variant?: 'accent' | 'ghost' }) {
  return (
    <button
      onClick={onClick}
      style={{
        display: 'inline-flex', alignItems: 'center', gap: '6px',
        padding: '9px 18px',
        border: `1px solid ${variant === 'accent' ? 'var(--mercury-accent)' : '#1c1912'}`,
        color: variant === 'accent' ? 'var(--mercury-accent)' : 'var(--text-muted-col)',
        background: 'transparent',
        fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase',
        fontFamily: 'inherit', cursor: 'pointer', borderRadius: '2px',
        transition: 'all 0.2s',
      }}
    >
      {children}
    </button>
  );
}

export default function DocumentsPage() {
  const router = useRouter();
  const { userId } = useUser();
  const [docs, setDocs] = useState<Document[]>([]);
  const [collections, setCollections] = useState<Collection[]>([]);
  const [filterCol, setFilterCol] = useState('');
  const [showUpload, setShowUpload] = useState(false);
  const [showNewCol, setShowNewCol] = useState(false);
  const [newColName, setNewColName] = useState('');
  const [loading, setLoading] = useState(true);

  const fetchAll = useCallback(async () => {
    if (!userId) return;
    try {
      const [d, c] = await Promise.all([
        documentsApi.list(userId, filterCol || undefined),
        collectionsApi.list(userId),
      ]);
      setDocs(d);
      setCollections(c);
    } catch { /* backend unreachable */ }
    finally { setLoading(false); }
  }, [userId, filterCol]);

  useEffect(() => { fetchAll(); }, [fetchAll]);

  // Poll while any doc is processing
  useEffect(() => {
    const hasActive = docs.some(d => d.status === 'PROCESSING' || d.status === 'PENDING');
    if (!hasActive) return;
    const id = setInterval(() => { fetchAll(); }, 5000);
    return () => clearInterval(id);
  }, [docs, fetchAll]);

  function handleUploaded(doc: Document) {
    setDocs(prev => [doc, ...prev]);
    setShowUpload(false);
  }

  function handleDeleted(id: string) {
    setDocs(prev => prev.filter(d => d.id !== id));
  }

  function handleUpdated(updated: Document) {
    setDocs(prev => prev.map(d => d.id === updated.id ? updated : d));
  }

  async function createCollection() {
    if (!newColName.trim()) return;
    try {
      const col = await collectionsApi.create(userId, newColName.trim());
      setCollections(prev => [...prev, col]);
      setNewColName('');
      setShowNewCol(false);
      toast.success('Collection created');
    } catch { toast.error('Could not create collection'); }
  }

  const sorted = docs.slice().sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

  return (
    <div>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '28px' }}>
        <div>
          <div style={{ fontSize: '9px', letterSpacing: '0.24em', textTransform: 'uppercase', color: 'var(--mercury-accent)', marginBottom: '6px' }}>Library</div>
          <h1 style={{ fontFamily: 'var(--font-cinzel), Cinzel, serif', fontSize: '26px', fontWeight: 600, letterSpacing: '0.04em', color: 'var(--foreground)' }}>
            Documents
          </h1>
        </div>
        <div style={{ display: 'flex', gap: '8px', marginTop: '4px' }}>
          <Btn onClick={() => setShowNewCol(o => !o)}><Plus size={11} /> New Collection</Btn>
          <Btn onClick={() => router.push('/documents/new')}><FileText size={11} /> Create Document</Btn>
          <Btn onClick={() => setShowUpload(o => !o)} variant="accent"><Upload size={11} /> Upload Document</Btn>
        </div>
      </div>

      {/* New collection inline form */}
      {showNewCol && (
        <div style={{ display: 'flex', gap: '8px', marginBottom: '16px', padding: '14px 16px', background: 'var(--card)', border: '1px solid #121420', borderRadius: '2px' }}>
          <input
            autoFocus
            type="text"
            placeholder="Collection name"
            value={newColName}
            onChange={e => setNewColName(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter') createCollection(); if (e.key === 'Escape') setShowNewCol(false); }}
            style={{ flex: 1, background: 'var(--background)', border: '1px solid #1c1912', color: 'var(--foreground)', padding: '8px 12px', fontSize: '13px', fontFamily: 'inherit', outline: 'none', borderRadius: '2px' }}
          />
          <Btn onClick={createCollection} variant="accent">Create</Btn>
          <Btn onClick={() => setShowNewCol(false)}>Cancel</Btn>
        </div>
      )}

      {/* Upload zone */}
      {showUpload && (
        <UploadZone userId={userId} collections={collections} onUploaded={handleUploaded} />
      )}

      {/* Filter bar */}
      {(collections.length > 0 || docs.length > 0) && (
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '24px' }}>
          <span style={{ fontSize: '10px', color: 'var(--text-muted-col)', letterSpacing: '0.06em' }}>Filter:</span>
          <select
            value={filterCol}
            onChange={e => setFilterCol(e.target.value)}
            style={{ background: 'var(--card)', border: '1px solid #121420', color: 'var(--foreground)', padding: '6px 12px', fontSize: '11px', fontFamily: 'inherit', outline: 'none', cursor: 'pointer', borderRadius: '2px' }}
          >
            <option value="">All Collections</option>
            {collections.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
          {docs.length > 0 && (
            <span style={{ fontSize: '10px', color: 'var(--text-muted-col)', marginLeft: 'auto' }}>
              {docs.length} {docs.length === 1 ? 'document' : 'documents'}
            </span>
          )}
        </div>
      )}

      {/* Loading */}
      {loading && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '1px', background: '#121420', border: '1px solid #121420' }}>
          {Array.from({ length: 6 }).map((_, i) => <SkeletonCard key={i} />)}
        </div>
      )}

      {/* Empty state */}
      {!loading && docs.length === 0 && (
        <div style={{ border: '1px solid #121420', background: 'var(--card)', padding: '60px', textAlign: 'center' }}>
          <div style={{ fontFamily: 'var(--font-cinzel), Cinzel, serif', fontSize: '16px', color: 'var(--foreground)', marginBottom: '12px' }}>
            No documents yet
          </div>
          <p style={{ fontSize: '12px', color: 'var(--text-muted-col)', marginBottom: '24px' }}>
            {filterCol ? 'No documents in this collection.' : 'Upload a PDF, DOCX, or TXT file to start building your knowledge base.'}
          </p>
          {!filterCol && (
            <Btn onClick={() => setShowUpload(true)} variant="accent"><Upload size={11} /> Upload Document</Btn>
          )}
        </div>
      )}

      {/* Document grid */}
      {!loading && docs.length > 0 && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '1px', background: '#121420', border: '1px solid #121420' }}>
          {sorted.map(doc => (
            <DocumentCard
              key={doc.id}
              doc={doc}
              userId={userId}
              collections={collections}
              onDeleted={handleDeleted}
              onUpdated={handleUpdated}
            />
          ))}
        </div>
      )}
    </div>
  );
}
