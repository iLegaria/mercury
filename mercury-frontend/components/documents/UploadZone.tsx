'use client';

import { useRef, useState, DragEvent } from 'react';
import { Upload, X, FileText } from 'lucide-react';
import { toast } from 'sonner';
import { documentsApi } from '@/lib/documents';
import type { Collection, Document } from '@/types';

interface Props {
  userId: string;
  collections: Collection[];
  onUploaded: (doc: Document) => void;
}

export default function UploadZone({ userId, collections, onUploaded }: Props) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [title, setTitle] = useState('');
  const [collectionId, setCollectionId] = useState('');
  const [dragging, setDragging] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');

  function pickFile(f: File) {
    setFile(f);
    setTitle(f.name.replace(/\.[^.]+$/, ''));
    setError('');
  }

  function handleDrop(e: DragEvent) {
    e.preventDefault();
    setDragging(false);
    const f = e.dataTransfer.files[0];
    if (f) pickFile(f);
  }

  function clear() {
    setFile(null);
    setTitle('');
    setCollectionId('');
    setError('');
    if (inputRef.current) inputRef.current.value = '';
  }

  async function handleUpload() {
    if (!file || !title.trim()) return;
    setUploading(true);
    setError('');
    try {
      const doc = await documentsApi.upload(userId, title.trim(), file, collectionId || undefined);
      onUploaded(doc);
      toast.success('Document queued for ingestion');
      clear();
    } catch {
      setError('Upload failed. Make sure the backend is running.');
      toast.error('Upload failed');
    } finally {
      setUploading(false);
    }
  }

  const inputStyle: React.CSSProperties = {
    width: '100%',
    background: 'var(--background)',
    border: '1px solid #1c1912',
    color: 'var(--foreground)',
    padding: '9px 12px',
    fontSize: '13px',
    fontFamily: 'inherit',
    outline: 'none',
    borderRadius: '2px',
  };

  return (
    <div style={{ border: '1px solid #121420', background: 'var(--card)', marginBottom: '28px' }}>
      {!file ? (
        /* Drop zone */
        <div
          onClick={() => inputRef.current?.click()}
          onDragOver={e => { e.preventDefault(); setDragging(true); }}
          onDragLeave={() => setDragging(false)}
          onDrop={handleDrop}
          style={{
            padding: '40px',
            textAlign: 'center',
            cursor: 'pointer',
            border: `2px dashed ${dragging ? 'var(--mercury-accent)' : '#1c1912'}`,
            margin: '12px',
            borderRadius: '2px',
            background: dragging ? 'var(--accent-dim)' : 'transparent',
            transition: 'all 0.2s',
          }}
        >
          <Upload size={20} style={{ color: 'var(--text-muted-col)', marginBottom: '10px' }} />
          <div style={{ fontSize: '12px', color: 'var(--text-muted-col)' }}>
            Drop a file here or <span style={{ color: 'var(--mercury-accent)' }}>click to select</span>
          </div>
          <div style={{ fontSize: '10px', color: 'var(--text-subtle)', marginTop: '6px', letterSpacing: '0.04em' }}>
            PDF · DOCX · TXT · HTML
          </div>
          <input
            ref={inputRef}
            type="file"
            accept=".pdf,.docx,.txt,.html,.md"
            style={{ display: 'none' }}
            onChange={e => { const f = e.target.files?.[0]; if (f) pickFile(f); }}
          />
        </div>
      ) : (
        /* Form */
        <div style={{ padding: '24px 28px' }}>
          {/* File info */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '20px', padding: '10px 14px', background: 'var(--background)', border: '1px solid #1c1912', borderRadius: '2px' }}>
            <FileText size={14} style={{ color: 'var(--mercury-accent)', flexShrink: 0 }} />
            <span style={{ fontSize: '12px', color: 'var(--foreground)', flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{file.name}</span>
            <span style={{ fontSize: '10px', color: 'var(--text-muted-col)', flexShrink: 0 }}>{(file.size / 1024).toFixed(0)} KB</span>
            <button onClick={clear} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-muted-col)', padding: 0, display: 'flex' }}>
              <X size={13} />
            </button>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px', marginBottom: '16px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '9px', letterSpacing: '0.16em', textTransform: 'uppercase', color: 'var(--text-muted-col)', marginBottom: '5px' }}>Title</label>
              <input type="text" value={title} onChange={e => setTitle(e.target.value)} style={inputStyle} />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: '9px', letterSpacing: '0.16em', textTransform: 'uppercase', color: 'var(--text-muted-col)', marginBottom: '5px' }}>Collection (optional)</label>
              <select value={collectionId} onChange={e => setCollectionId(e.target.value)} style={{ ...inputStyle, cursor: 'pointer' }}>
                <option value="">No collection</option>
                {collections.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </div>
          </div>

          {error && <p style={{ fontSize: '11px', color: 'var(--error)', marginBottom: '12px' }}>{error}</p>}

          <button
            onClick={handleUpload}
            disabled={uploading || !title.trim()}
            style={{
              padding: '10px 28px',
              border: '1px solid var(--mercury-accent)',
              color: 'var(--mercury-accent)',
              background: 'transparent',
              fontSize: '11px',
              letterSpacing: '0.08em',
              textTransform: 'uppercase',
              fontFamily: 'inherit',
              cursor: uploading || !title.trim() ? 'not-allowed' : 'pointer',
              opacity: uploading || !title.trim() ? 0.5 : 1,
              transition: 'all 0.2s',
              borderRadius: '2px',
            }}
          >
            {uploading ? 'Uploading...' : 'Upload →'}
          </button>
        </div>
      )}
    </div>
  );
}
