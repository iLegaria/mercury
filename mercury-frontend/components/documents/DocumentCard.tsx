'use client';

import { useState } from 'react';
import Link from 'next/link';
import { Trash2, FolderInput, MoreHorizontal, BookOpen } from 'lucide-react';
import { toast } from 'sonner';
import StatusBadge from './StatusBadge';
import { documentsApi } from '@/lib/documents';
import type { Document, Collection } from '@/types';

const SOURCE_COLORS: Record<string, string> = {
  PDF: '#e8952a', DOCX: '#4a7fa8', TXT: '#4a9e6b', HTML: '#9e78c5', MARKDOWN: '#6e6758', OTHER: '#6e6758',
};

interface Props {
  doc: Document;
  userId: string;
  collections: Collection[];
  onDeleted: (id: string) => void;
  onUpdated: (doc: Document) => void;
}

export default function DocumentCard({ doc, userId, collections, onDeleted, onUpdated }: Props) {
  const [menuOpen, setMenuOpen] = useState(false);
  const [assigning, setAssigning] = useState(false);
  const [selectedCollection, setSelectedCollection] = useState(doc.collectionId ?? '');

  async function handleDelete() {
    if (!confirm(`Delete "${doc.title}"? This cannot be undone.`)) return;
    try {
      await documentsApi.delete(doc.id, userId);
      onDeleted(doc.id);
      toast.success('Document deleted');
    } catch { toast.error('Could not delete document'); }
  }

  async function handleAssign() {
    setAssigning(true);
    try {
      const updated = await documentsApi.assignCollection(doc.id, userId, selectedCollection || null);
      onUpdated(updated);
      setMenuOpen(false);
      toast.success('Collection assigned');
    } catch { toast.error('Could not assign collection'); }
    finally { setAssigning(false); }
  }

  const statusBorderColor: Record<string, string> = {
    COMPLETED: 'var(--mercury-accent)', PROCESSING: 'var(--warning)', PENDING: 'var(--cold)', FAILED: 'var(--error)',
  };

  return (
    <div style={{
      background: 'var(--card)',
      border: '1px solid #121420',
      borderLeft: `2px solid ${statusBorderColor[doc.status] ?? '#121420'}`,
      padding: '18px 20px',
      position: 'relative',
      transition: 'border-color 0.2s',
    }}>
      {/* Title + menu */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: '8px', marginBottom: '10px' }}>
        <div style={{ fontSize: '13px', color: 'var(--foreground)', fontWeight: 500, lineHeight: 1.4, overflow: 'hidden', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical' }}>
          {doc.title}
        </div>
        <button
          onClick={() => setMenuOpen(o => !o)}
          style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-muted-col)', padding: '2px', flexShrink: 0, display: 'flex' }}
        >
          <MoreHorizontal size={14} />
        </button>
      </div>

      {/* Badges */}
      <div style={{ display: 'flex', gap: '6px', alignItems: 'center', flexWrap: 'wrap', marginBottom: '10px' }}>
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
          <span style={{ fontSize: '9px', letterSpacing: '0.04em', color: 'var(--text-muted-col)', padding: '2px 7px', border: '1px solid #1c1912', borderRadius: '2px' }}>
            {doc.collectionName}
          </span>
        )}
      </div>

      {/* Date + Study link */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ fontSize: '10px', color: 'var(--text-subtle)' }}>
          {new Date(doc.createdAt).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })}
        </div>
        {doc.status === 'COMPLETED' && (
          <Link
            href={`/documents/${doc.id}`}
            style={{
              display: 'inline-flex', alignItems: 'center', gap: '4px',
              fontSize: '9px', letterSpacing: '0.08em', textTransform: 'uppercase',
              color: 'var(--mercury-accent)', padding: '3px 8px',
              border: '1px solid var(--border-accent)', borderRadius: '2px',
              textDecoration: 'none', transition: 'all 0.15s',
            }}
          >
            <BookOpen size={9} /> Study
          </Link>
        )}
      </div>

      {/* Dropdown menu */}
      {menuOpen && (
        <div style={{
          position: 'absolute', top: '36px', right: '12px', zIndex: 20,
          background: 'var(--bg-panel, #0d0b12)', border: '1px solid #1c1912',
          minWidth: '200px', boxShadow: '0 8px 24px rgba(0,0,0,0.5)',
        }}>
          {/* Assign collection */}
          <div style={{ padding: '12px 14px', borderBottom: '1px solid #1c1912' }}>
            <div style={{ fontSize: '9px', letterSpacing: '0.14em', textTransform: 'uppercase', color: 'var(--text-muted-col)', marginBottom: '6px', display: 'flex', alignItems: 'center', gap: '5px' }}>
              <FolderInput size={10} /> Assign Collection
            </div>
            <select
              value={selectedCollection}
              onChange={e => setSelectedCollection(e.target.value)}
              style={{ width: '100%', background: 'var(--background)', border: '1px solid #1c1912', color: 'var(--foreground)', padding: '6px 8px', fontSize: '11px', fontFamily: 'inherit', borderRadius: '2px', outline: 'none' }}
            >
              <option value="">No collection</option>
              {collections.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
            <button
              onClick={handleAssign}
              disabled={assigning}
              style={{ marginTop: '6px', width: '100%', padding: '6px', background: 'var(--accent-dim)', border: '1px solid var(--border-accent)', color: 'var(--mercury-accent)', fontSize: '10px', letterSpacing: '0.06em', cursor: 'pointer', fontFamily: 'inherit', borderRadius: '2px' }}
            >
              {assigning ? 'Saving...' : 'Save'}
            </button>
          </div>
          {/* Delete */}
          <button
            onClick={() => { setMenuOpen(false); handleDelete(); }}
            style={{ width: '100%', padding: '10px 14px', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--error)', fontSize: '11px', letterSpacing: '0.04em', fontFamily: 'inherit', display: 'flex', alignItems: 'center', gap: '6px', textAlign: 'left' }}
          >
            <Trash2 size={11} /> Delete Document
          </button>
        </div>
      )}

      {/* Close menu on outside click */}
      {menuOpen && <div onClick={() => setMenuOpen(false)} style={{ position: 'fixed', inset: 0, zIndex: 19 }} />}
    </div>
  );
}
