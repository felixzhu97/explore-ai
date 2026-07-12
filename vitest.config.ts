import { defineConfig } from 'vitest/config';
import angular from '@analogjs/vite-plugin-angular';
import { resolve } from 'path';

export default defineConfig({
  plugins: [angular()],
  test: {
    include: ['src/main/web/**/*.spec.ts'],
    environment: 'jsdom',
    globals: true,
    setupFiles: ['src/main/web/test-setup.ts'],
    exclude: ['src/main/web/app/app.config.spec.ts'],
  },
  resolve: {
    alias: {
      '@core': resolve(__dirname, 'src/main/web/app/core'),
      '@shared': resolve(__dirname, 'src/main/web/app/shared'),
      '@features': resolve(__dirname, 'src/main/web/app/features'),
      '@env': resolve(__dirname, 'src/main/web/environments'),
      '@': resolve(__dirname, 'src/main/web/app'),
    },
  },
});
