import Link from 'next/link';

export default function NotFound() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '60vh', textAlign: 'center', padding: '40px' }}>
      <div style={{ fontFamily: 'var(--font-cinzel), Cinzel, serif', fontSize: '80px', fontWeight: 700, color: 'var(--surface-3)', lineHeight: 1, marginBottom: '16px' }}>
        404
      </div>
      <div style={{ fontSize: '9px', letterSpacing: '0.24em', textTransform: 'uppercase', color: 'var(--mercury-accent)', marginBottom: '12px' }}>
        Lost in Space
      </div>
      <h1 style={{ fontFamily: 'var(--font-cinzel), Cinzel, serif', fontSize: '20px', fontWeight: 600, letterSpacing: '0.04em', color: 'var(--foreground)', marginBottom: '12px' }}>
        Page Not Found
      </h1>
      <p style={{ fontSize: '12px', color: 'var(--text-muted-col)', maxWidth: '320px', lineHeight: 1.8, marginBottom: '32px' }}>
        This region of space hasn't been charted. The page you're looking for doesn't exist.
      </p>
      <Link href="/" style={{
        padding: '10px 24px', border: '1px solid var(--mercury-accent)', color: 'var(--mercury-accent)',
        textDecoration: 'none', fontSize: '11px', letterSpacing: '0.08em', textTransform: 'uppercase',
        borderRadius: '2px',
      }}>
        ← Return to Dashboard
      </Link>
    </div>
  );
}
