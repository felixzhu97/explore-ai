import { defineConfig } from 'vitest/config';
import angular from '@analogjs/vite-plugin-angular';

export default defineConfig({
  plugins: [angular()],
  test: {
    include: ['src/main/web/**/*.spec.ts'],
    environment: 'jsdom',
    globals: true,
    setupFiles: ['src/main/web/test-setup.ts'],
    exclude: ['src/main/web/app/app.config.spec.ts'],
  },
});
