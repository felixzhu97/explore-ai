// @ts-check
import eslint from '@eslint/js';
import stylistic from '@stylistic/eslint-plugin';
import angular from 'angular-eslint';
import { defineConfig } from 'eslint/config';
import tseslint from 'typescript-eslint';
import eslintPluginTailwindcss from 'eslint-plugin-tailwindcss';

export default defineConfig([
  {
    files: ['**/*.ts'],
    languageOptions: {
      // https://typescript-eslint.io/getting-started/typed-linting/
      parserOptions: {
        projectService: true,
        tsconfigRootDir: import.meta.dirname,
      },
    },
    extends: [
      eslint.configs.recommended,
      tseslint.configs.recommended,
      tseslint.configs.stylistic,
      angular.configs.tsRecommended,
      eslintPluginTailwindcss.configs.recommended,
      // https://eslint.style/guide/config-presets
      stylistic.configs.customize({
        braceStyle: '1tbs',
        jsx: false,
        quoteProps: 'as-needed',
        semi: true,
      }),
    ],
    processor: angular.processInlineTemplates,
    rules: {
      '@stylistic/implicit-arrow-linebreak': 'error',
      '@stylistic/linebreak-style': 'error',
      '@stylistic/max-len': [
        'error', 90, 2,
        {
          ignoreComments: false,
          ignoreUrls: true,
          ignoreRegExpLiterals: true,
          ignoreStrings: true,
          ignoreTemplateLiterals: true,
        },
      ],
      '@stylistic/no-extra-semi': 'error',
      '@stylistic/object-curly-newline': ['error', { multiline: true, consistent: true }],
      '@stylistic/operator-linebreak': [
        'error',
        'before',
        { overrides: { '=': 'after' } },
      ],
      '@stylistic/switch-colon-spacing': 'error',
      'no-param-reassign': ['error', { props: true }],
      '@typescript-eslint/no-empty-function': [
        'error',
        {
          allow: ['private-constructors'],
        },
      ],
      '@angular-eslint/component-selector': [
        'error',
        {
          type: 'element',
          prefix: ['app', 'z'],
          style: 'kebab-case',
        },
      ],
      '@angular-eslint/directive-selector': [
        'error',
        [
          { type: 'element', prefix: ['app', 'z'], style: 'kebab-case' },
          { type: 'attribute', prefix: ['app', 'z', 'zard'], style: 'camelCase' },
        ],
      ],
      // Covered by angularCompilerOptions.strictStandalone — avoid double reporting
      '@angular-eslint/prefer-standalone': 'off',
      // Modern Angular style (explicit; prefer-inject also in tsRecommended)
      '@angular-eslint/prefer-inject': 'error',
      '@angular-eslint/prefer-host-metadata-property': 'error',
      '@angular-eslint/prefer-output-emitter-ref': 'error',
      '@angular-eslint/no-uncalled-signals': 'error',
      '@angular-eslint/prefer-on-push-component-change-detection': 'error',
      '@angular-eslint/prefer-signal-model': 'error',
      '@angular-eslint/prefer-signals': [
        'error',
        {
          useTypeChecking: true,
        },
      ],
      '@angular-eslint/sort-keys-in-type-decorator': 'error',
      '@angular-eslint/sort-lifecycle-methods': 'error',
      '@angular-eslint/use-lifecycle-interface': 'error',
    },
  },
  {
    files: ['**/*.html'],
    ignores: ['**/index.html'],
    extends: [
      angular.configs.templateRecommended,
      angular.configs.templateAccessibility,
    ],
    rules: {
      '@angular-eslint/template/button-has-type': 'error',
      '@angular-eslint/template/label-has-associated-control': [
        'error',
        {
          controlComponents: [
            'app-input-number',
            'app-checkbox',
          ],
        }
      ],
      '@angular-eslint/template/no-duplicate-attributes': [
        'error',
        {
          allowStylePrecedenceDuplicates: true,
        },
      ],
      '@angular-eslint/template/no-interpolation-in-attributes': [
        'error',
        {
          allowSubstringInterpolation: true,
        },
      ],
      '@angular-eslint/template/no-empty-control-flow': 'error',
      '@angular-eslint/template/no-positive-tabindex': 'error',
      '@angular-eslint/template/prefer-at-empty': 'error',
      '@angular-eslint/template/prefer-class-binding': 'error',
      '@angular-eslint/template/prefer-contextual-for-variables': 'error',
      '@angular-eslint/template/prefer-control-flow': 'error',
      '@angular-eslint/template/prefer-ngsrc': 'error',
      '@angular-eslint/template/prefer-self-closing-tags': 'error',
      '@angular-eslint/template/prefer-static-string-properties': 'error',
      '@angular-eslint/template/prefer-template-literal': 'error',
    },
  },
  // Dynamic / base64 previews cannot use NgOptimizedImage
  {
    files: [
      '**/media-shell/**',
      '**/chat-shell/**',
      '**/image-zoom/**',
      '**/vision/**',
      '**/rag.page.html',
    ],
    rules: {
      '@angular-eslint/template/prefer-ngsrc': 'off',
    },
  },
  // https://www.npmjs.com/package/eslint-plugin-tailwindcss
  {
    files: ['**/*.ts', '**/*.html'],
    ignores: ['**/index.html'],
    plugins: {
      tailwindcss: eslintPluginTailwindcss,
    },
    settings: {
      tailwindcss: {
        cssConfigPath: './src/main/web/styles.css',
      },
    },
    // https://github.com/francoismassart/eslint-plugin-tailwindcss/tree/4edd2dc560e52f99f9268d6380f00942a601cc4d/docs/rules
    rules: {
      // https://github.com/francoismassart/eslint-plugin-tailwindcss/blob/HEAD/docs/rules/classnames-order.md
      'tailwindcss/classnames-order': 'error',
      'tailwindcss/enforces-shorthand': 'error',
      'tailwindcss/enforces-negative-arbitrary-values': 'error',
      'tailwindcss/no-arbitrary-value': 'off',
      'tailwindcss/no-unnecessary-arbitrary-value': 'error',
      'tailwindcss/no-contradicting-classname': 'error',
      'tailwindcss/no-custom-classname': [
        'error',
        {
          whitelist: [
            'custom-.*',
          ],
        },
      ],
    },
  },
  {
    files: [
      'src/main/web/app/shared/components/input/input.directive.ts',
      'src/main/web/app/shared/components/layout/sidebar-menu-button.directive.ts',
      'src/main/web/app/shared/components/menu/context-menu.directive.ts',
      'src/main/web/app/shared/components/menu/menu-content.directive.ts',
      'src/main/web/app/shared/components/menu/menu-item.directive.ts',
      'src/main/web/app/shared/components/menu/menu.directive.ts',
    ],
    rules: {
      '@angular-eslint/directive-selector': [
        'error',
        {
          type: 'attribute',
          prefix: ['app', 'z'],
          style: 'kebab-case',
        },
      ],
    },
  },
]);
