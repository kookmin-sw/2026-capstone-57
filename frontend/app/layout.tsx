import type { Metadata, Viewport } from 'next'
import { Noto_Sans_KR } from 'next/font/google'
import { Analytics } from '@vercel/analytics/next'
import { AuthGuard } from '@/components/auth-guard'
import './globals.css'

const notoSansKr = Noto_Sans_KR({ 
  subsets: ['latin'],
  weight: ['400', '500', '600', '700'],
  variable: '--font-sans',
})

export const metadata: Metadata = {
  title: '일기예보 - 오늘의 만남 예보',
  description: '캠퍼스에서 우연히 스친 인연을 발견하세요',
  generator: 'v0.app',
  icons: {
    icon: '/logo.png',
      
    apple: '/logo.png',
  },
}

export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
  maximumScale: 1,
  userScalable: false,
  themeColor: '#FFF8EC',
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="ko" className="bg-background h-full">
      <body className={`${notoSansKr.variable} font-sans antialiased h-full`}>
        <AuthGuard>
          {children}
        </AuthGuard>
        {process.env.NODE_ENV === 'production' && <Analytics />}
      </body>
    </html>
  )
}
