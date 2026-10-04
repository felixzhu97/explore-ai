# Angular Web Application

A modern Angular 22 frontend application featuring:

- **AI Infrastructure Panel** - Model management, LLMOps, AIOps, VectorDB
- **RAG Chat** - Document Q&A with retrieval-augmented generation
- **Image Analysis** - Image analysis with captioning, object detection, and OCR
- **AI Hub** - Chat, image generation, and text-to-speech

## Tech Stack

- Angular 22
- TypeScript
- SCSS
- Angular Signals for state management
- Standalone components

## Getting Started

```bash
# Install pnpm if not already installed
npm install -g pnpm

# Install dependencies
pnpm install

# Start development server
pnpm start

# Build for production
pnpm build

# Run tests
pnpm test
```

## Project Structure

Each bounded context is one flat folder under `app/`. Pages,
services, components and `<context>.routes.ts` sit side by side, and
types live in the service or component that uses them (no `core`,
`shared` or `*.model.ts`).

```
src/main/web/
├── app/
│   ├── app.config.ts, app.routes.ts, storage-keys.ts
│   ├── layout/          # Main layout, header, sidebar, nav config
│   ├── http/            # API base URL, interceptors, SSE client
│   ├── i18n/            # I18nService, translations, locales
│   ├── feature-flags/   # FeatureFlagService, module guard
│   ├── ui/              # zard kit (one folder per component)
│   ├── chat-shell/      # Message pane, bubbles, markdown, A2UI
│   ├── account/  agents/  automations/  chat/  eval/
│   ├── generate/  image/  tts/  mcp/  metrics/  pipelines/
│   └── policies/  privacy/  rag/  skills/  speech-to-text/  vision/
├── styles.css           # Global styles
├── main.ts              # Application entry point
└── index.html           # HTML entry point
```

## Features

- Multi-language support (English, Chinese, Japanese, French, Spanish)
- Responsive design with Apple-style aesthetics
- Dark mode support (planned)
- PWA support (planned)
