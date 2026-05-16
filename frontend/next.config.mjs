/** @type {import('next').NextConfig} */
const nextConfig = {
  typescript: {
    ignoreBuildErrors: true,
  },
  images: {
    unoptimized: true,
  },
  // Turbopack 호환 (빈 설정으로 경고 제거)
  turbopack: {},
  async rewrites() {
    return [
      {
        source: "/backend/:path*",
        destination: "http://localhost:8080/:path*",
      },
      // Game API proxy (development)
      {
        source: "/api/:path*",
        destination: "http://localhost:8080/api/:path*",
      },
      // Game WebSocket proxy
      {
        source: "/ws/:path*",
        destination: "http://localhost:8080/ws/:path*",
      },
    ]
  },
}

export default nextConfig
