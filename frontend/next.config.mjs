/** @type {import('next').NextConfig} */
const nextConfig = {
  typescript: {
    ignoreBuildErrors: true,
  },
  images: {
    unoptimized: true,
  },
  turbopack: {},
  async rewrites() {
    return [
      {
        source: "/backend/:path*",
        destination: "http://54.174.25.221:8080/:path*",
      },
      {
        source: "/api/:path*",
        destination: "http://54.174.25.221:8080/api/:path*",
      },
    ]
  },
}

export default nextConfig