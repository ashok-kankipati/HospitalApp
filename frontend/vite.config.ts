import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import { fileURLToPath } from 'node:url';
import { readFileSync, copyFileSync } from 'node:fs';

const staticRoot = fileURLToPath(new URL('../src/main/resources/static/', import.meta.url));

export default defineConfig({
  base: '/app/',
  plugins: [react(), tailwindcss(), {
    name: 'careflow-existing-workflows',
    // Bundle the existing clinical workflows so they work identically in dev and production.
    generateBundle() {
      copyFileSync(fileURLToPath(new URL('./node_modules/dompurify/dist/purify.min.js', import.meta.url)), `${staticRoot}/js/vendor/purify.min.js`);
      for (const path of ['js/dashboard.js', 'js/workflow-icons.js', 'js/safe-html.js', 'js/vendor/purify.min.js', 'css/dashboard.css']) {
        this.emitFile({ type: 'asset', fileName: `workflows/${path}`, source: readFileSync(path === 'js/vendor/purify.min.js' ? fileURLToPath(new URL('./node_modules/dompurify/dist/purify.min.js', import.meta.url)) : `${staticRoot}/${path}`) });
      }
    },
    configureServer(server) {
      server.middlewares.use('/app/workflows/', (req, res, next) => {
        const path = req.url?.split('?')[0];
        if (!['js/dashboard.js', 'js/workflow-icons.js', 'js/safe-html.js', 'js/vendor/purify.min.js', 'css/dashboard.css'].includes(path || '')) return next();
        res.setHeader('Content-Type', path!.endsWith('.js') ? 'text/javascript' : 'text/css');
        res.end(readFileSync(`${staticRoot}/${path}`));
      });
    },
  }],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080', changeOrigin: true,
        configure(proxy) {
          proxy.on('proxyReq', (request, incoming) => {
            // Development is same-origin in the browser; align the forwarded
            // origin with the backend host for its CSRF origin check.
            if (incoming.headers.origin === 'http://localhost:5173' || incoming.headers.origin === 'http://127.0.0.1:5173') {
              request.setHeader('Origin', 'http://localhost:8080');
            }
          });
        },
      },
      '/patient-details.html': 'http://localhost:8080',
      '/css': 'http://localhost:8080',
      '/js': 'http://localhost:8080',
    },
  },
  build: { outDir: '../src/main/resources/static/app', emptyOutDir: true },
});
