'use client';

import { useState } from 'react';
import { ChevronDown, ChevronUp } from 'lucide-react';
import type { ChunkSearchResult } from '@/types';

export default function SourceCard({ chunks }: { chunks: ChunkSearchResult[] }) {
  const [open, setOpen] = useState(false);

  if (!chunks.length) return null;

  return (
    <div style={{ marginTop: '10px' }}>
      <button
        onClick={() => setOpen(o => !o)}
        style={{
          display: 'inline-flex', alignItems: 'center', gap: '5px',
          background: 'none', border: '1px solid #1c1912', color: 'var(--text-muted-col)',
          padding: '4px 10px', fontSize: '10px', letterSpacing: '0.06em', cursor: 'pointer',
          fontFamily: 'inherit', borderRadius: '2px',
        }}
      >
        {open ? <ChevronUp size={10} /> : <ChevronDown size={10} />}
        {chunks.length} {chunks.length === 1 ? 'source' : 'sources'}
      </button>

      {open && (
        <div style={{ marginTop: '8px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
          {chunks.map((c, i) => (
            <div key={c.chunkId ?? i} style={{
              background: 'var(--background)',
              border: '1px solid #1c1912',
              borderLeft: '2px solid var(--cold)',
              padding: '10px 14px',
              borderRadius: '2px',
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px', gap: '8px' }}>
                <span style={{ fontSize: '9px', letterSpacing: '0.06em', color: 'var(--cold)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', flex: 1 }}>
                  {c.documentTitle ?? `Passage ${(c.chunkIndex ?? i) + 1}`}
                </span>
                <span style={{ fontSize: '9px', color: 'var(--text-muted-col)', flexShrink: 0 }}>
                  {Math.round(c.similarity * 100)}% match
                </span>
              </div>
              <p style={{ fontSize: '11px', color: 'var(--text-subtle)', lineHeight: 1.7, margin: 0 }}>
                {c.content.length > 300 ? c.content.slice(0, 300) + '…' : c.content}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
