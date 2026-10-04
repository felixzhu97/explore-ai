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
  // App code only: the generated ZardUI library under ui/ keeps its upstream style.
  {
    files: ['src/main/web/**/*.ts'],
    ignores: ['src/main/web/app/ui/**'],
    rules: {
      '@angular-eslint/component-class-suffix': 'error',
      '@angular-eslint/computed-must-return': 'error',
      '@angular-eslint/consistent-component-styles': 'error',
      '@angular-eslint/contextual-decorator': 'error',
      '@angular-eslint/directive-class-suffix': 'error',
      '@angular-eslint/inject-at-top': 'error',
      '@angular-eslint/no-async-lifecycle-method': 'error',
      '@angular-eslint/no-attribute-decorator': 'error',
      '@angular-eslint/no-duplicates-in-metadata-arrays': 'error',
      '@angular-eslint/no-forward-ref': 'error',
      '@angular-eslint/no-implicit-take-until-destroyed': 'error',
      '@angular-eslint/no-lifecycle-call': 'error',
      '@angular-eslint/no-pipe-impure': 'error',
      '@angular-eslint/no-queries-metadata-property': 'error',
      '@angular-eslint/prefer-output-readonly': 'error',
      '@angular-eslint/prefer-service-decorator': 'error',
      '@angular-eslint/reactive-context-must-read-signal': 'error',
      '@angular-eslint/relative-url-prefix': 'error',
      '@angular-eslint/require-lifecycle-on-prototype': 'error',
      '@angular-eslint/use-component-selector': 'error',
      '@angular-eslint/use-component-view-encapsulation': 'error',
      'no-restricted-syntax': [
        'error',
        {
          selector: ':matches(PropertyDefinition, MethodDefinition, AccessorProperty, '
            + 'TSParameterProperty)[accessibility="private"]',
          message: 'Use an ECMAScript #private member instead of the private keyword.',
        },
        {
          selector: 'TSEnumDeclaration',
          message: 'Use a string literal union type instead of an enum.',
        },
        {
          selector: ':matches(TSInterfaceDeclaration, TSTypeAliasDeclaration)[id.name=/Dto$/]',
          message: 'Name wire types after the Java record (e.g. SkillResponse), without a Dto suffix.',
        },
        {
          selector: ':matches(TSInterfaceDeclaration, TSTypeAliasDeclaration)'
            + '[id.name=/(Response|Event)$/] TSPropertySignature[optional=true]',
          message: 'Jackson serializes nulls, so response and event fields are `T | null`, not optional.',
        },
      ],
      '@typescript-eslint/explicit-member-accessibility': [
        'error',
        { accessibility: 'no-public' },
      ],
      '@typescript-eslint/naming-convention': [
        'error',
        {
          selector: 'default',
          format: ['camelCase'],
          leadingUnderscore: 'forbid',
          trailingUnderscore: 'forbid',
        },
        { selector: 'import', format: null },
        { selector: 'variable', format: ['camelCase', 'UPPER_CASE', 'PascalCase'] },
        {
          selector: 'classProperty',
          modifiers: ['static', 'readonly'],
          format: ['camelCase', 'UPPER_CASE'],
        },
        { selector: 'typeLike', format: ['PascalCase'] },
        { selector: 'enumMember', format: ['PascalCase', 'UPPER_CASE'] },
        {
          selector: 'parameter',
          modifiers: ['unused'],
          format: ['camelCase'],
          leadingUnderscore: 'allow',
        },
        {
          selector: ['objectLiteralProperty', 'objectLiteralMethod', 'typeProperty', 'typeMethod'],
          format: null,
        },
      ],
      '@typescript-eslint/prefer-readonly': 'error',
      '@typescript-eslint/await-thenable': 'error',
      '@typescript-eslint/consistent-type-imports': [
        'error',
        { fixStyle: 'inline-type-imports' },
      ],
      '@typescript-eslint/no-confusing-void-expression': [
        'error',
        { ignoreArrowShorthand: true },
      ],
      '@typescript-eslint/no-deprecated': 'error',
      '@typescript-eslint/no-floating-promises': 'error',
      '@typescript-eslint/no-misused-promises': 'error',
      '@typescript-eslint/no-non-null-assertion': 'error',
      '@typescript-eslint/no-unsafe-argument': 'error',
      '@typescript-eslint/no-unsafe-assignment': 'error',
      '@typescript-eslint/no-unsafe-call': 'error',
      '@typescript-eslint/no-unsafe-member-access': 'error',
      '@typescript-eslint/no-unsafe-return': 'error',
      '@typescript-eslint/no-unnecessary-condition': [
        'error',
        { allowConstantLoopConditions: 'only-allowed-literals' },
      ],
      '@typescript-eslint/prefer-nullish-coalescing': [
        'error',
        { ignorePrimitives: { string: true } },
      ],
      '@typescript-eslint/no-base-to-string': 'error',
      '@typescript-eslint/require-await': 'error',
      '@typescript-eslint/restrict-template-expressions': [
        'error',
        {
          allowAny: false,
          allowArray: false,
          allowBoolean: false,
          allowNever: false,
          allowNullish: false,
          allowNumber: false,
          allowRegExp: false,
        },
      ],
      '@typescript-eslint/strict-boolean-expressions': [
        'error',
        {
          allowString: false,
          allowNumber: false,
          allowNullableObject: false,
          allowNullableBoolean: false,
          allowNullableString: false,
          allowNullableNumber: false,
          allowNullableEnum: false,
          allowAny: false,
        },
      ],
      '@typescript-eslint/use-unknown-in-catch-callback-variable': 'error',
      'no-console': ['error', { allow: ['warn', 'error'] }],
      '@typescript-eslint/no-unnecessary-type-assertion': 'error',
      '@typescript-eslint/only-throw-error': 'error',
      '@typescript-eslint/switch-exhaustiveness-check': 'error',
      curly: ['error', 'all'],
      eqeqeq: ['error', 'always'],
      'no-implicit-coercion': [
        'error',
        { boolean: true, number: true, string: true, disallowTemplateShorthand: true },
      ],
      'no-restricted-imports': [
        'error',
        {
          paths: [
            {
              name: '@angular/forms',
              importNames: ['FormsModule', 'NgModel'],
              message: 'Bind with [formField] from @angular/forms/signals or the control\'s '
                + 'signal inputs and outputs instead.',
            },
          ],
        },
      ],
      'object-shorthand': 'error',
      'prefer-template': 'error',
    },
  },
  {
    files: ['src/main/web/**/*.spec.ts'],
    rules: {
      '@typescript-eslint/no-non-null-assertion': 'off',
      '@typescript-eslint/no-unsafe-argument': 'off',
      '@typescript-eslint/no-unsafe-assignment': 'off',
      '@typescript-eslint/no-unsafe-call': 'off',
      '@typescript-eslint/no-unsafe-member-access': 'off',
      '@typescript-eslint/no-unsafe-return': 'off',
    },
  },
  // Dates are js-joda types; `Date` only crosses into third-party APIs through time/.
  {
    files: ['src/main/web/**/*.ts'],
    ignores: [
      'src/main/web/app/ui/**',
      'src/main/web/app/time/native-date.ts',
      'src/main/web/app/time/instant-picker.component.ts',
    ],
    rules: {
      'no-restricted-globals': [
        'error',
        { name: 'Date', message: 'Use Instant, LocalDate or LocalDateTime from @js-joda/core.' },
      ],
      '@typescript-eslint/no-restricted-types': [
        'error',
        {
          types: {
            Date: 'Use Instant, LocalDate or LocalDateTime from @js-joda/core.',
          },
        },
      ],
    },
  },
  {
    files: ['src/main/web/**/*.html'],
    ignores: ['src/main/web/app/ui/**', '**/index.html'],
    rules: {
      '@angular-eslint/template/attributes-order': 'error',
      '@angular-eslint/template/cyclomatic-complexity': ['error', { maxComplexity: 12 }],
      '@angular-eslint/template/no-inline-styles': ['error', { allowBindToStyle: true }],
      '@angular-eslint/template/conditional-complexity': 'error',
      '@angular-eslint/template/no-any': 'error',
      '@angular-eslint/template/no-non-null-assertion': 'error',
      '@angular-eslint/template/no-nested-tags': 'error',
      '@angular-eslint/template/no-outerhtml': 'error',
      '@angular-eslint/template/prefer-built-in-pipes': 'error',
      '@angular-eslint/template/prefer-at-else': 'error',
      '@angular-eslint/template/prefer-style-binding': 'error',
      '@angular-eslint/template/require-switch-default': 'error',
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
      '**/image/image-gen-form.component.ts/**',
      '**/image/media-preview-panel.component.ts/**',
      '**/chat-shell/**',
      '**/ui/image-zoom-dialog.component.ts/**',
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
      'src/main/web/app/ui/input/input.directive.ts',
      'src/main/web/app/ui/layout/sidebar-menu-button.directive.ts',
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
