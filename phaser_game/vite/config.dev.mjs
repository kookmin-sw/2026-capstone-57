import { defineConfig } from 'vite';

export default defineConfig({
    base: './',
    define: {
        global: 'globalThis',
    },
    build: {
        rollupOptions: {
            output: {
                manualChunks: {
                    phaser: ['phaser']
                }
            }
        },
    },
    server: {
        port: 5173,
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true
            },
            '/ws': {
                target: 'http://localhost:8080',
                ws: true,
                changeOrigin: true
            }
        }
    }
});
