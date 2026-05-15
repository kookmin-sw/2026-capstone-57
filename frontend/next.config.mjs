/** @type {import('next').NextConfig} */
const nextConfig = {
  typescript: {
    ignoreBuildErrors: true,
  },
  images: {
    unoptimized: true,
  },
  // Phaser uses 'self' and 'global' which need to be handled
  webpack: (config) => {
    config.resolve.fallback = {
      ...config.resolve.fallback,
      fs: false,
      path: false,
    };
    return config;
  },
  async rewrites() {
    return [
      {
        source: "/backend/:path*",
        destination: "http://54.80.10.143:8080/:path*",
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
