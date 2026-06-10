# Angular Web Application

A modern Angular 20+ frontend application featuring:

- **AI Infrastructure Panel** - Model management, LLMOps, AIOps, VectorDB
- **RAG Chat** - Document Q&A with retrieval-augmented generation
- **Vision AI** - Image analysis with captioning, object detection, and OCR
- **AI Hub** - Chat, image generation, and text-to-speech

## Tech Stack

- Angular 20+
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

```
src/
├── app/
│   ├── components/     # UI components
│   │   ├── ai/         # AI-related components
│   │   ├── agents/     # Agent components
│   │   └── panels/     # Panel components
│   ├── services/       # Angular services
│   ├── i18n/           # Internationalization
│   ├── shared/         # Shared utilities & models
│   └── theme/          # Theme configuration
├── styles.scss         # Global styles
├── main.ts             # Application entry point
└── index.html          # HTML entry point
```

## Features

- Multi-language support (English, Chinese, Japanese, French, Spanish)
- Responsive design with Apple-style aesthetics
- Dark mode support (planned)
- PWA support (planned)
