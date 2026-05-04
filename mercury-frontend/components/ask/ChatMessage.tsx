import SourceCard from './SourceCard';
import type { ChatMessage as ChatMsg } from '@/types';

export default function ChatMessage({ msg }: { msg: ChatMsg }) {
  const isUser = msg.role === 'user';

  if (isUser) {
    return (
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '16px' }}>
        <div style={{
          background: 'var(--surface-2)',
          border: '1px solid #302c22',
          padding: '12px 16px',
          maxWidth: '70%',
          borderRadius: '2px 2px 0 2px',
          fontSize: '13px',
          color: 'var(--foreground)',
          lineHeight: 1.7,
          whiteSpace: 'pre-wrap',
        }}>
          {msg.content}
        </div>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', justifyContent: 'flex-start', marginBottom: '16px' }}>
      <div style={{ maxWidth: '80%' }}>
        {/* Mercury label */}
        <div style={{ fontSize: '9px', letterSpacing: '0.18em', textTransform: 'uppercase', color: 'var(--mercury-accent)', marginBottom: '6px' }}>
          Mercury
        </div>
        <div style={{
          background: 'var(--card)',
          border: '1px solid #121420',
          borderLeft: '2px solid var(--mercury-accent)',
          padding: '14px 18px',
          borderRadius: '0 2px 2px 2px',
          fontSize: '13px',
          color: 'var(--foreground)',
          lineHeight: 1.8,
          whiteSpace: 'pre-wrap',
        }}>
          {msg.content}
        </div>
        {msg.sources && msg.sources.length > 0 && (
          <SourceCard chunks={msg.sources} />
        )}
      </div>
    </div>
  );
}
