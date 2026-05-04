interface Props {
  width?: string;
  height?: string;
  style?: React.CSSProperties;
}

export function Skeleton({ width = '100%', height = '14px', style }: Props) {
  return <div className="skeleton" style={{ width, height, ...style }} />;
}

export function SkeletonCard() {
  return (
    <div style={{ background: 'var(--card)', border: '1px solid #121420', padding: '20px 22px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
      <Skeleton height="16px" width="60%" />
      <Skeleton height="11px" width="40%" />
      <Skeleton height="11px" width="80%" />
    </div>
  );
}

export function SkeletonStatCard() {
  return (
    <div style={{ background: 'var(--card)', border: '1px solid #121420', padding: '24px 28px', flex: 1 }}>
      <Skeleton height="32px" width="50%" style={{ marginBottom: '8px' }} />
      <Skeleton height="10px" width="70%" />
    </div>
  );
}

export function SkeletonRow() {
  return (
    <div style={{ background: 'var(--card)', padding: '14px 18px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px' }}>
      <div style={{ flex: 1 }}>
        <Skeleton height="13px" width="55%" style={{ marginBottom: '6px' }} />
        <Skeleton height="10px" width="35%" />
      </div>
      <Skeleton height="18px" width="70px" />
    </div>
  );
}
