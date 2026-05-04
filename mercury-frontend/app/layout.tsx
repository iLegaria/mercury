import type { Metadata } from 'next';
import { Cinzel, JetBrains_Mono } from 'next/font/google';
import './globals.css';
import StarfieldCanvas from '@/components/layout/StarfieldCanvas';
import Navbar from '@/components/layout/Navbar';
import Sidebar from '@/components/layout/Sidebar';
import UserGuard from '@/components/layout/UserGuard';
import { Toaster } from '@/components/ui/sonner';

const cinzel = Cinzel({
  variable: '--font-cinzel',
  subsets: ['latin'],
  weight: ['400', '600', '700'],
});

const jetbrainsMono = JetBrains_Mono({
  variable: '--font-jetbrains',
  subsets: ['latin'],
  weight: ['300', '400', '500'],
});

export const metadata: Metadata = {
  title: 'Mercury — Personal Knowledge Engine',
  description: 'Your self-hosted RAG knowledge base',
  icons: { icon: '/favicon.svg' },
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={`${cinzel.variable} ${jetbrainsMono.variable}`}>
      <body>
        <StarfieldCanvas />
        <div className="mercury-glow" />
        <div className="grain" />
        <Navbar />
        <div className="layout-wrapper">
          <Sidebar />
          <main className="layout-main">
            <UserGuard>{children}</UserGuard>
          </main>
        </div>
        <Toaster theme="dark" position="bottom-right" />
      </body>
    </html>
  );
}
