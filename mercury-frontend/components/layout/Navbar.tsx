import Link from 'next/link';
import Image from 'next/image';
import UserBadge from './UserBadge';

export default function Navbar() {
  return (
    <nav style={{
      position: 'fixed',
      inset: '0 0 auto 0',
      zIndex: 50,
      borderBottom: '1px solid #121420',
      background: 'rgba(5,7,14,0.85)',
      backdropFilter: 'blur(18px)',
      WebkitBackdropFilter: 'blur(18px)',
    }}>
      <div style={{
        maxWidth: '1400px',
        margin: '0 auto',
        padding: '0 40px',
        height: '56px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
      }}>
        <Link href="/" style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: '10px',
          fontFamily: 'var(--font-cinzel), Cinzel, serif',
          fontSize: '13px',
          fontWeight: 600,
          letterSpacing: '0.18em',
          color: 'var(--mercury-accent)',
          textDecoration: 'none',
          textTransform: 'uppercase',
        }}>
          <Image
            src="/mercury-logo.svg"
            alt=""
            width={24}
            height={24}
            priority
            style={{ display: 'block', filter: 'drop-shadow(0 0 10px rgba(245,184,65,0.22))' }}
          />
          Mercury
        </Link>
        <UserBadge />
      </div>
    </nav>
  );
}
