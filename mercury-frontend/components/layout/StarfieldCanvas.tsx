'use client';

import { useEffect, useRef } from 'react';

const PLANET_COLS = [
  { r: 232, g: 149, b: 42 },
  { r: 100, g: 190, b: 255 },
  { r: 180, g: 130, b: 255 },
  { r: 255, g: 210, b: 160 },
  { r: 140, g: 255, b: 200 },
  { r: 255, g: 150, b: 110 },
];

export default function StarfieldCanvas() {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    type Star = { x: number; y: number; radius: number; alpha: number; ts: number; to: number; warm: boolean };
    type Planet = { x: number; y: number; radius: number; alpha: number; ts: number; to: number; col: { r: number; g: number; b: number }; glow: number };
    type Comet = { x: number; y: number; vx: number; vy: number; len: number; alpha: number } | null;

    let stars: Star[] = [];
    let planets: Planet[] = [];
    let W = 0, H = 0, rot = 0;
    let comet: Comet = null, cc = 0;
    let frame = 0;
    let rafId = 0;

    function resize() {
      W = canvas!.width = window.innerWidth;
      H = canvas!.height = window.innerHeight;
    }

    function initStars() {
      stars = [];
      const n = Math.floor(W * H / 3600);
      for (let i = 0; i < n; i++) {
        const r = Math.random();
        stars.push({
          x: Math.random() * W, y: Math.random() * H,
          radius: r < 0.68 ? Math.random() * 0.55 + 0.15 : r < 0.9 ? Math.random() * 0.9 + 0.45 : Math.random() * 1.4 + 0.9,
          alpha: Math.random() * 0.6 + 0.2,
          ts: Math.random() * 0.01 + 0.002,
          to: Math.random() * Math.PI * 2,
          warm: Math.random() < 0.08,
        });
      }
    }

    function initPlanets() {
      planets = [];
      const n = 8 + Math.floor(Math.random() * 4);
      for (let i = 0; i < n; i++) {
        const col = PLANET_COLS[Math.floor(Math.random() * PLANET_COLS.length)];
        planets.push({
          x: Math.random() * W, y: Math.random() * H,
          radius: 1.8 + Math.random() * 2.4,
          alpha: 0.65 + Math.random() * 0.35,
          ts: 0.0015 + Math.random() * 0.005,
          to: Math.random() * Math.PI * 2,
          col, glow: 14 + Math.random() * 22,
        });
      }
    }

    function spawnComet() {
      const a = Math.PI * (0.17 + Math.random() * 0.22);
      const spd = 10 + Math.random() * 5;
      comet = { x: Math.random() * W * 0.5, y: Math.random() * H * 0.2, vx: Math.cos(a) * spd, vy: Math.sin(a) * spd, len: 90 + Math.random() * 110, alpha: 0 };
    }

    function drawComet() {
      if (!comet) return;
      comet.x += comet.vx; comet.y += comet.vy;
      comet.alpha = Math.min(1, comet.alpha + 0.055);
      if (comet.x > W + 160 || comet.y > H + 60) { comet = null; return; }
      const spd = Math.hypot(comet.vx, comet.vy);
      const tx = comet.x - (comet.vx / spd) * comet.len;
      const ty = comet.y - (comet.vy / spd) * comet.len;
      ctx!.save();
      const g = ctx!.createLinearGradient(tx, ty, comet.x, comet.y);
      g.addColorStop(0, 'rgba(180,215,255,0)');
      g.addColorStop(0.6, `rgba(215,232,255,${comet.alpha * 0.33})`);
      g.addColorStop(1, `rgba(255,255,255,${comet.alpha * 0.88})`);
      ctx!.strokeStyle = g; ctx!.lineWidth = 1.8;
      ctx!.beginPath(); ctx!.moveTo(tx, ty); ctx!.lineTo(comet.x, comet.y); ctx!.stroke();
      ctx!.shadowBlur = 10; ctx!.shadowColor = 'rgba(180,220,255,0.9)';
      ctx!.beginPath(); ctx!.arc(comet.x, comet.y, 1.8, 0, Math.PI * 2);
      ctx!.fillStyle = `rgba(255,255,255,${comet.alpha})`; ctx!.fill();
      ctx!.restore();
    }

    function draw() {
      ctx!.clearRect(0, 0, W, H);
      frame += 0.016; rot += 0.00028;
      ctx!.save();
      ctx!.translate(W / 2, H / 2); ctx!.rotate(rot); ctx!.translate(-W / 2, -H / 2);
      stars.forEach(s => {
        const fl = Math.sin(frame * s.ts * 60 + s.to);
        const a = Math.max(0, Math.min(1, s.alpha + fl * 0.13));
        ctx!.beginPath(); ctx!.arc(s.x, s.y, s.radius, 0, Math.PI * 2);
        ctx!.fillStyle = s.warm ? `rgba(240,190,100,${a})` : `rgba(200,212,235,${a})`;
        ctx!.fill();
      });
      ctx!.restore();
      planets.forEach(p => {
        const fl = Math.sin(frame * p.ts * 60 + p.to);
        const a = Math.max(0.3, Math.min(1, p.alpha + fl * 0.32));
        const gp = p.glow * (0.55 + 0.45 * ((fl + 1) / 2));
        ctx!.save();
        ctx!.shadowBlur = gp * 2.2;
        ctx!.shadowColor = `rgba(${p.col.r},${p.col.g},${p.col.b},${a * 0.35})`;
        ctx!.beginPath(); ctx!.arc(p.x, p.y, p.radius + 1.4, 0, Math.PI * 2);
        ctx!.fillStyle = `rgba(${p.col.r},${p.col.g},${p.col.b},${a * 0.12})`; ctx!.fill();
        ctx!.shadowBlur = gp;
        ctx!.shadowColor = `rgba(${p.col.r},${p.col.g},${p.col.b},${a})`;
        ctx!.beginPath(); ctx!.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
        ctx!.fillStyle = `rgba(${p.col.r},${p.col.g},${p.col.b},${a})`; ctx!.fill();
        ctx!.restore();
      });
      cc++; if (cc >= 700 && !comet) { cc = 0; spawnComet(); }
      drawComet();
      rafId = requestAnimationFrame(draw);
    }

    resize(); initStars(); initPlanets(); draw();

    let rt: ReturnType<typeof setTimeout>;
    const onResize = () => {
      clearTimeout(rt);
      rt = setTimeout(() => { resize(); initStars(); initPlanets(); }, 200);
    };
    window.addEventListener('resize', onResize);
    return () => {
      cancelAnimationFrame(rafId);
      window.removeEventListener('resize', onResize);
    };
  }, []);

  return (
    <canvas
      ref={canvasRef}
      style={{ position: 'fixed', inset: 0, zIndex: 0, pointerEvents: 'none' }}
    />
  );
}
