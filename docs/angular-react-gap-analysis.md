# Angular ↔ React Gap Analysis

**Document Version:** 1.0  
**Generated:** 2026-06-07  
**Purpose:** Definitive migration guide for completing Angular implementation

---

## Summary Statistics

| Metric | Count |
|--------|-------|
| Total React components analyzed | 10 |
| Total Angular components analyzed | 10 (+ 2 additional) |
| Fully migrated (100% parity) | 2 |
| Partially migrated (50-99% parity) | 6 |
| Not migrated | 2 |
| Angular-only features | 4 |
| Angular components with unit tests | 4 |
| React components with unit tests | 2 |

### Components Requiring Attention by Priority

| Priority | Components |
|----------|-----------|
| **Critical** | AIHub (10%), VisionPanel (50%), ChatMessage (40%) |
| **High** | RAGChat (50%), AIHub Chat/Image/TTS (0%) |
| **Medium** | ImageZoomModal (80%), SegmentedControl (85%), StatusBadge (85%) |
| **Low** | Button (90%), Card (95%) |

---

## Component-by-Component Analysis

### 1. AIHub

**Status:** Not Migrated  
**Parity Score:** 10%  
**React Complexity:** ~1400 lines | **Angular Complexity:** ~60 lines

#### Functional Gaps

| Feature | React | Angular | Gap Description |
|---------|-------|---------|-----------------|
| Tab Navigation (chat/image/tts) | ✅ Full implementation | ❌ Static header only | Angular version only has decorative header |
| Chat Streaming | ✅ Implemented | ❌ Not implemented | Missing chat UI, streaming, message bubbles |
| Image Generation | ✅ Full panel | ❌ Not implemented | Missing image gen with SDXL/Stable Diffusion |
| TTS Synthesis | ✅ Full panel | ❌ Not implemented | Missing TTS with voice selection, speed control |
| Provider/Model Selection | ✅ Dynamic loading | ❌ Not implemented | Missing provider dropdown, model selector |
| Quick Prompts | ✅ Implemented | ❌ Not implemented | Missing quick action buttons |
| Error Handling | ✅ Network error detection | ❌ Not implemented | Missing service unavailable messages |
| Abort Controller | ✅ Stream cancellation | ❌ Not implemented | Missing request cancellation |
| Markdown Rendering | ✅ react-markdown + highlight | ❌ Not implemented | Missing code highlighting |
| Audio Player | ✅ Custom progress bar | ❌ Not implemented | Missing TTS audio playback |
| Loading States | ✅ Spinners, overlays | ❌ Not implemented | Missing all loading indicators |

#### Visual Gaps

| Element | React | Angular | Difference |
|---------|-------|---------|------------|
| Container | flex-column, gap: md | ❌ Missing | Angular has static div only |
| Tab Header | centered, scrollable | ❌ Missing | Missing SegmentedControl usage |
| PanelHeader | with title + description | Only title | Missing description text |
| ChatContainer | 400px max-height | ❌ Missing | No message list styling |
| MessageBubble | fadeIn animation, max-width 75% | ❌ Missing | No message bubbles |
| InputArea | TextArea + SendButton | ❌ Missing | No input field |
| SizeSelector | grid of SizeOption buttons | ❌ Missing | No image size picker |
| AudioPlayer | Custom player with progress | ❌ Missing | No audio controls |

#### Accessibility Gaps

| Feature | React | Angular |
|---------|-------|---------|
| Tablist role | ✅ On Container | ❌ None |
| Tab role | ✅ On options | ❌ None |
| aria-selected | ✅ On options | ❌ None |
| Keyboard navigation | ✅ Enter to send | ❌ None |
| Focus management | ✅ focus-visible | ❌ None |

#### Missing Tests

- React: ❌ No unit tests (only `apps/web/src/components/__tests__/AIHub.test.tsx` exists for basic structure)
- Angular: ❌ No spec file for `AiHubComponent`

#### Recommended Actions

1. **CRITICAL** - Complete full feature parity:
   - Reimplement Chat tab with streaming support
   - Reimplement Image Generation tab
   - Reimplement TTS tab with audio player
2. Add provider/model selector dropdowns
3. Implement markdown rendering with syntax highlighting
4. Add loading spinners and error states
5. Write unit tests for all features

---

### 2. SegmentedControl

**Status:** Partially Migrated  
**Parity Score:** 85%

#### Functional Gaps

| Feature | React | Angular | Gap Description |
|---------|-------|---------|-----------------|
| Generic Type Support | ✅ `T extends string` | ✅ `T extends string` | Fully matched |
| Options array | ✅ `{value, label}[]` | ✅ Same structure | Fully matched |
| onChange callback | ✅ Direct callback | ✅ `output()` event emitter | Slight API difference |
| Disabled option | ✅ Passed through | ✅ `disabled?: boolean` | Fully matched |
| testID prop | ✅ Ignored | ❌ Not implemented | React test prop not needed |

#### API Differences

| Aspect | React | Angular |
|--------|-------|---------|
| Change handler | `onChange: (value: T) => void` | `changed = output<T>()` |
| Value binding | Direct prop | Signal `value = input.required<T>()` |
| Options binding | Direct prop | Signal `options = input.required<Option[]>()` |

#### Visual Gaps

| Element | React | Angular | Difference |
|---------|-------|---------|------------|
| Container bg | `colors.surface` | `transparent` | Minor - Angular has no bg |
| Box-shadow | `shadows.card + inset` | `0 2px 8px rgba + inset` | Nearly identical |
| Border-radius | `radius.md` | `8px` (hardcoded) | Should use CSS variable |
| Font | `typography.fontFamily.body` | Hardcoded system font | React more consistent |
| Focus ring | `shadows.input` | `0 0 0 3px rgba(0,122,255,0.3)` | Angular uses custom color |

#### Accessibility Gaps

| Feature | React | Angular | Notes |
|---------|-------|---------|-------|
| role="tablist" | ✅ | ✅ | Both implemented |
| role="tab" | ✅ | ✅ | Both implemented |
| aria-selected | ✅ | ✅ | Both implemented |
| aria-disabled | ❌ | ✅ | Angular has, React missing |
| Keyboard navigation | ✅ | ✅ | Enter key works |

#### Missing Tests

- React: ❌ No unit tests
- Angular: ✅ `segmented-control.component.spec.ts` (comprehensive)

#### Recommended Actions

1. Add `aria-disabled` attribute to React version
2. Move hardcoded values to CSS variables in Angular
3. Add unit tests to React version

---

### 3. StatusBadge

**Status:** Partially Migrated  
**Parity Score:** 85%

#### Functional Gaps

| Feature | React | Angular | Gap Description |
|---------|-------|---------|-----------------|
| Status types | ✅ 5 types | ✅ 5 types | Fully matched |
| Custom label | ✅ `label` prop | ✅ `label()` input | Fully matched |
| Dot indicator | ✅ `showDot` prop | ✅ `showDot()` input | Fully matched |
| Default labels | ✅ `statusLabels` object | ✅ Same object | Fully matched |

#### API Differences

| Aspect | React | Angular |
|--------|-------|---------|
| Props | Direct props | Signal-based inputs |
| Disabled state | Not supported | Not supported |

#### Visual Gaps

| Element | React | Angular | Difference |
|---------|-------|---------|------------|
| offline bg | `rgba(0,0,0,0.06)` | Same | Matched |
| offline color | `colors.textTertiary` | `var(--color-text-tertiary)` | Both use theme |
| CSS Variables | Used in Angular | React uses imports | Angular more portable |

#### Accessibility Gaps

| Feature | React | Angular |
|---------|-------|---------|
| ARIA roles | None | None |
| aria-live | None | None |
| Color contrast | Passes WCAG | Passes WCAG |

#### Missing Tests

- React: ❌ No unit tests
- Angular: ❌ No spec file for `StatusBadgeComponent`

#### Recommended Actions

1. Add unit tests for both versions
2. Consider adding `role="status"` for screen readers
3. Add support for custom icon slots

---

### 4. ChatMessage

**Status:** Partially Migrated  
**Parity Score:** 40%

#### Functional Gaps

| Feature | React | Angular | Gap Description |
|---------|-------|---------|-----------------|
| Message types | ✅ user/assistant/system | ✅ Same | Fully matched |
| ToolCalls display | ✅ Implemented | ✅ Implemented | Both support tool calls |
| JSON syntax highlighting | ✅ Custom component | ✅ `DomSanitizer` + regex | Angular uses custom parser |
| Markdown rendering | ✅ react-markdown | ❌ Custom regex parser | React uses full library |
| Code blocks | ✅ Multiple language support | ❌ Basic support only | React more robust |
| Image zoom | ✅ Implemented | ❌ Not implemented | Missing zoomable images |
| Query blocks (PromQL/SQL) | ✅ Detection + display | ❌ Not implemented | React has specialized query UI |
| rehype-highlight | ✅ Syntax colors | ❌ Not implemented | Missing code highlighting |

#### API Differences

| Aspect | React | Angular |
|--------|-------|---------|
| Props | `message: ChatMessageData` | `message = input.required<ChatMessageData>()` |
| Time formatting | Inline in render | `computed()` signal |
| Content rendering | `renderAssistantContent()` | `renderedContent = computed()` |

#### Visual Gaps

| Element | React | Angular | Difference |
|---------|-------|---------|------------|
| Message bubble max-width | 80% | 80% | Matched |
| Animation | `fadeIn 0.2s ease` | `fadeIn 0.2s ease` | Matched |
| Code font | `'SF Mono', Monaco, ...` | Same | Matched |
| blockquote border | `3px solid primary 50` | `3px solid rgba(0,122,255,0.3)` | Slightly different opacity |
| JSON syntax colors | Custom spans | HTML spans with classes | Angular uses HTML classes |

#### Accessibility Gaps

| Feature | React | Angular |
|---------|-------|---------|
| Semantic HTML | ✅ `<p>` for user messages | ✅ Template literal |
| Code blocks | ✅ `<pre><code>` | ✅ `<pre><code>` |
| aria-hidden | On icons | On icons |

#### Missing Tests

- React: ❌ No unit tests
- Angular: ❌ No spec file for `ChatMessageComponent`

#### Recommended Actions

1. **HIGH** - Add markdown rendering to Angular (use marked + highlight.js)
2. **HIGH** - Add image zoom functionality to Angular ChatMessage
3. **MEDIUM** - Add query block detection for PromQL/SQL
4. **MEDIUM** - Add rehype-highlight support for code blocks
5. Write unit tests for both versions

---

### 5. VisionPanel

**Status:** Partially Migrated  
**Parity Score:** 50%

#### Functional Gaps

| Feature | React | Angular | Gap Description |
|---------|-------|---------|-----------------|
| Tab navigation | ✅ SegmentedControl | ⚠️ Custom implementation | Angular uses inline buttons |
| Image upload | ✅ Drag & drop + click | ✅ Same | Fully matched |
| File validation | ✅ Type check | ✅ Type check | Both implemented |
| Tab states | ✅ Per-tab state | ✅ Per-tab state | Both maintain state |
| Result rendering | ✅ Caption/Detect/OCR | ✅ Same | Fully matched |
| Error display | ✅ ErrorMessage styled | ✅ Error styling | Both implemented |
| Image zoom | ✅ Separate component | ⚠️ Inline modal | Angular has inline modal |
| Loading overlay | ✅ Implemented | ✅ Implemented | Both show spinner |
| Clear button | ✅ Implemented | ✅ Implemented | Both work |
| Zoom hint | ✅ On hover | ✅ On hover | Both show hint |

#### API Differences

| Aspect | React | Angular |
|--------|-------|---------|
| State management | `useState` hooks | Angular signals |
| Tab switching | Direct state | Signal-based |
| File input | Hidden styled input | Dynamic DOM creation |

#### Visual Gaps

| Element | React | Angular | Difference |
|---------|-------|---------|------------|
| Spinner border | `colors.border` | `#e5e5e5` | React uses theme |
| Drop icon bg | `colors.primaryLight` | `#f5f5f7` | Different bg color |
| Error bg | `colors.errorLight` | `#ffebee` | Different error color |
| Zoom modal | Separate component | Inline with fixed position | Different architecture |

#### Accessibility Gaps

| Feature | React | Angular |
|---------|-------|---------|
| Input type="file" | ✅ Hidden styled | ✅ Hidden styled |
| Tab roles | ✅ Via SegmentedControl | ❌ No role="tablist" |
| aria-disabled | Via SegmentedControl | ❌ None |

#### Missing Tests

- React: ❌ No unit tests
- Angular: ❌ No spec file for `VisionPanelComponent`

#### Recommended Actions

1. **HIGH** - Add proper role="tablist/tab" to Angular tab buttons
2. **MEDIUM** - Extract zoom modal to separate component for consistency
3. **MEDIUM** - Use theme variables consistently
4. Write unit tests for both versions

---

### 6. RAGChat

**Status:** Partially Migrated  
**Parity Score:** 50%

#### Functional Gaps

| Feature | React | Angular | Gap Description |
|---------|-------|---------|-----------------|
| Document list | ✅ Skeleton + cards | ✅ Skeleton + cards | Fully matched |
| Document selection | ✅ Multi-select with animation | ✅ Same | Both have delete animation |
| File upload | ✅ FormData + progress | ✅ Same | Both implement |
| Toast notifications | ✅ SlideIn animation | ✅ SlideIn animation | Fully matched |
| Chat streaming | ✅ SSE reader | ✅ SSE via ApiService | Both implement |
| Source display | ✅ Expandable panel | ✅ Expandable panel | Fully matched |
| Markdown rendering | ✅ react-markdown | ⚠️ Custom regex parser | React more complete |
| Image zoom | ✅ Via ImageZoomModal | ❌ Not implemented | React has zoom |
| Skeleton loading | ✅ shimmer animation | ✅ shimmer animation | Fully matched |
| Success animation | ✅ fadeOut + checkmark | ⚠️ fadeOut only | Missing success overlay |
| Delete animation | ✅ Staggered (200ms + 300ms) | ⚠️ Single setTimeout | Angular less smooth |
| i18n | ✅ Full integration | ❌ Hardcoded strings | React uses I18nService |
| i18n.tReplace | ✅ Dynamic placeholders | ❌ No replacement | Missing interpolation |

#### API Differences

| Aspect | React | Angular |
|--------|-------|---------|
| State management | `useState` + `useCallback` | Angular signals |
| Refs | `useRef` for DOM | `viewChild` + `ElementRef` |
| Effects | `useEffect` | `ngOnInit`, `ngOnDestroy` |
| Input binding | Direct props | Signals with `input()` |
| Output binding | Callbacks | `output()` emitters |

#### Visual Gaps

| Element | React | Angular | Difference |
|---------|-------|---------|------------|
| Message bubble bg | `surfaceSecondary` | `#f5f5f7` | Different values |
| Toast success bg | `#d4edda` | Same | Matched |
| Source border-left | `primary` | `#0071e3` | Different format |
| Upload progress | pulse animation | pulse animation | Matched |
| Delete button | rgba overlay when selected | rgba overlay | Matched |
| Quick actions | From i18n | Hardcoded English | Angular needs i18n |

#### Accessibility Gaps

| Feature | React | Angular |
|---------|-------|---------|
| role="checkbox" | ✅ On doc cards | ✅ On doc cards |
| aria-checked | ✅ Dynamic | ✅ Dynamic |
| Keyboard nav | ✅ Enter/Space | ✅ Enter/Space |
| aria-label on delete | ✅ Yes | ✅ Yes |
| Focus ring | ✅ focus-visible | ❌ None on doc cards |

#### Missing Tests

- React: ❌ No unit tests
- Angular: ❌ No spec file for `RagChatComponent`

#### Recommended Actions

1. **CRITICAL** - Add i18n support to Angular RAGChat
2. **HIGH** - Add image zoom functionality
3. **HIGH** - Improve markdown rendering in Angular
4. **MEDIUM** - Add success overlay animation to Angular
5. **MEDIUM** - Add focus-visible styling to document cards
6. Write unit tests for both versions

---

### 7. ImageZoomModal

**Status:** Partially Migrated  
**Parity Score:** 80%

#### Functional Gaps

| Feature | React | Angular | Gap Description |
|---------|-------|---------|-----------------|
| Keyboard close | ✅ Escape key | ✅ HostListener decorator | Both work |
| Click outside close | ✅ On Overlay | ✅ On overlay div | Both implemented |
| Body scroll lock | ✅ overflow: hidden | ✅ Via effect() | Both lock scroll |
| Caption display | ✅ Conditional | ✅ Conditional | Both show alt |
| Close button | ✅ SVG icon | ✅ SVG icon | Both have close |
| Animation | ✅ fadeIn + slideIn | ✅ fadeIn + slideIn | Fully matched |

#### API Differences

| Aspect | React | Angular |
|--------|-------|---------|
| Props | `src, alt, onClose` | `src, alt` signals + `closed` output |
| Open state | Parent controls | Self-contained with signal |
| Close handler | `onClose()` callback | `closed.emit()` + body style reset |

#### Visual Gaps

| Element | React | Angular | Difference |
|---------|-------|---------|------------|
| Overlay bg | `rgba(0,0,0,0.85)` | `rgba(0,0,0,0.85)` | Matched |
| Close button bg | `colors.surface` | `#ffffff` | Different |
| Close button border | `colors.border` | `#d1d1d6` | Slightly different |
| Focus ring | Not in styles | `0 0 0 3px rgba(0,122,255,0.3)` | Angular has focus |

#### Accessibility Gaps

| Feature | React | Angular | Notes |
|---------|-------|---------|-------|
| role="dialog" | ❌ None | ✅ `role="dialog"` | Angular better |
| aria-modal | ❌ None | ✅ `aria-modal="true"` | Angular better |
| aria-label | On close button only | On overlay + close | Angular better |
| Keyboard focus | ✅ Escape works | ✅ Escape via HostListener | Both work |
| Focus trap | None | None | Both missing |

#### Missing Tests

- React: ❌ No unit tests
- Angular: ✅ `image-zoom-modal.component.spec.ts` exists

#### Recommended Actions

1. **MEDIUM** - Add ARIA attributes to React version (role, aria-modal, aria-label)
2. **LOW** - Add focus ring to close button in React
3. Write unit tests for React version

---

### 8. Button

**Status:** Fully Migrated  
**Parity Score:** 90%

#### Functional Gaps

| Feature | React | Angular | Gap Description |
|---------|-------|---------|-----------------|
| Variants | ✅ 4 types | ✅ 4 types | Fully matched |
| Sizes | ✅ sm/md/lg | ✅ sm/md/lg | Fully matched |
| Loading state | ✅ Spinner shown | ✅ Spinner shown | Both implement |
| Disabled state | ✅ Prevents click | ✅ Prevents click | Both implemented |
| Icon support | ✅ Icon prop | ⚠️ Content projection | Slight difference |
| Full width | ✅ fullWidth prop | ✅ fullWidth input | Fully matched |
| aria-busy | ❌ Not implemented | ✅ `aria-busy` | Angular has this |

#### API Differences

| Aspect | React | Angular |
|--------|-------|---------|
| Click handler | Native onClick spread | `clicked = output<MouseEvent>()` |
| Disabled | Native disabled prop | Native disabled binding |
| Icon | `icon?: React.ReactNode` | `<ng-content select="[icon]">` |

#### Visual Gaps

| Element | React | Angular | Difference |
|---------|-------|---------|------------|
| primary bg | `colors.primary` | `#007aff` | Angular hardcoded |
| primary hover | `colors.primaryHover` | `#0066d6` | Different values |
| Danger bg | `colors.error` | `#ff3b30` | Angular hardcoded |
| Focus ring | `shadows.input` | `0 0 0 3px rgba(0,122,255,0.3)` | Similar |
| Radius sm | `radius.sm` | `6px` | Angular hardcoded |

#### Accessibility Gaps

| Feature | React | Angular |
|---------|-------|---------|
| aria-busy | ❌ None | ✅ On button |
| aria-disabled | Native | Native |
| Focus visible | ✅ | ✅ |

#### Missing Tests

- React: ❌ No unit tests
- Angular: ✅ `button.component.spec.ts` (comprehensive)

#### Recommended Actions

1. **LOW** - Add `aria-busy` to React version
2. **LOW** - Use theme variables consistently in Angular
3. Write unit tests for React version

---

### 9. Card

**Status:** Fully Migrated  
**Parity Score:** 95%

#### Functional Gaps

| Feature | React | Angular | Gap Description |
|---------|-------|---------|-----------------|
| Variants | ✅ 4 types | ✅ 4 types | Fully matched |
| Padding options | ✅ none/sm/md/lg | ✅ none/sm/md/lg | Fully matched |
| Hoverable state | ✅ hoverable prop | ✅ hoverable input | Both implement |
| Click handler | ✅ onClick prop | ⚠️ handler exists but unused | Angular doesn't emit |
| Hover animation | ✅ translateY(-2px) | ✅ translateY(-2px) | Fully matched |

#### API Differences

| Aspect | React | Angular |
|--------|-------|---------|
| onClick | Native onClick | `handleClick()` exists but no emit |
| Hover state | CSS `:hover` | Signal-based with mouse events |
| Padding | Function-based | CSS class-based | Different approaches |

#### Visual Gaps

| Element | React | Angular | Difference |
|---------|-------|---------|------------|
| Default shadow | `shadows.card` | `0 2px 8px rgba(0,0,0,0.08)` | Same value |
| Glass bg | `colors.glass` | `rgba(255,255,255,0.8)` | Same value |
| Hover shadow | `shadows.cardHover` | `0 8px 24px rgba(0,0,0,0.12)` | Same value |
| Radius | `radius.lg` | `12px` | Angular hardcoded |

#### Accessibility Gaps

| Feature | React | Angular |
|---------|-------|---------|
| Interactive cursor | ✅ When hoverable | ✅ When hoverable |
| Focus styles | ❌ None | ❌ None |
| role attribute | None | None |

#### Missing Tests

- React: ❌ No unit tests
- Angular: ✅ `card.component.spec.ts` exists

#### Recommended Actions

1. **LOW** - Add click output to Angular Card
2. **LOW** - Add focus-visible styles for keyboard navigation
3. Write unit tests for React version

---

### 10. Theme

**Status:** Fully Migrated  
**Parity Score:** 100%

#### Token Comparison

| Token Category | React | Angular | Match |
|----------------|-------|---------|-------|
| Colors | ✅ Export object | ✅ Export object | ✅ 100% |
| Shadows | ✅ Export object | ✅ Export object | ✅ 100% |
| Radius | ✅ Export object | ✅ Export object | ✅ 100% |
| Spacing | ✅ Export object | ✅ Export object | ✅ 100% |
| Typography | ✅ Export object | ✅ Export object | ✅ 100% |
| Transitions | ✅ Export object | ✅ Export object | ✅ 100% |
| zIndex | ✅ Export object | ✅ Export object | ✅ 100% |

#### CSS Variable Usage

| Component | React | Angular |
|-----------|-------|---------|
| Uses CSS vars | ❌ Uses JS imports | ✅ Uses `--var(--)` |
| Emotion styled | ✅ Yes | N/A |
| Inline styles | Rarely | Sometimes hardcoded |

---

## Cross-Cutting Concerns

### Missing Shared Infrastructure

#### API Services (Angular Missing/Replacing)

| React Library | Purpose | Angular Status |
|--------------|---------|----------------|
| `aiServices.ts` | Chat, image gen, TTS | Need `ApiService` implementation |
| `lib/aiServices` | Provider/model APIs | Need equivalent service |
| `downloadBlob` | File downloads | Need utility |

#### State Management

| Aspect | React | Angular |
|--------|-------|---------|
| Global state | Context API | Services with signals |
| Component state | useState | signals |
| Side effects | useEffect | effects + lifecycle |
| Callbacks | useCallback | Methods |

### i18n Inconsistencies

| Component | React | Angular |
|-----------|-------|---------|
| AIHub | ✅ Full i18n | ❌ Hardcoded |
| VisionPanel | ✅ Full i18n | ✅ I18nService |
| RAGChat | ✅ Full i18n | ❌ Hardcoded |
| StatusBadge | ✅ Hardcoded | ✅ Hardcoded |
| SegmentedControl | ✅ Via parent | ✅ Via parent |

### State Management Differences

| Pattern | React | Angular |
|---------|-------|---------|
| Local state | `useState` | `signal()` |
| Derived state | `useMemo` / inline | `computed()` |
| Effects | `useEffect` | `effect()` |
| Refs | `useRef` | `viewChild` |

### Build/Deployment Differences

| Aspect | React (Vite) | Angular (CLI) |
|--------|--------------|---------------|
| Dev server | Port 5173 | Port 4200 |
| Build tool | Vite | esbuild |
| Testing | Vitest | Jasmine/Karma |
| CSS-in-JS | Emotion | Component styles |
| Environment | `.env` files | `environment.ts` |

---

## Migration Priority Recommendations

### High Priority (Critical User-Facing Gaps)

1. **AIHub Complete Rewrite**
   - Chat functionality with streaming
   - Image generation panel
   - TTS synthesis panel
   - Provider/model selection
   - All i18n integration

2. **ChatMessage Markdown Enhancement**
   - Add full markdown support (use `marked` + `highlight.js`)
   - Add image zoom functionality
   - Add query block detection for PromQL/SQL

3. **RAGChat i18n Integration**
   - Replace all hardcoded strings with I18nService
   - Add dynamic placeholder replacement (tReplace equivalent)

### Medium Priority (Important Features)

4. **VisionPanel Accessibility**
   - Add proper ARIA roles to tab navigation
   - Extract zoom modal to separate component

5. **ImageZoomModal Accessibility**
   - Add ARIA attributes to React version

6. **SegmentedControl Enhancement**
   - Add `aria-disabled` to React version
   - Add unit tests to React version

### Low Priority (Nice to Have)

7. **Button aria-busy** - Add to React version
8. **Card click output** - Implement in Angular
9. **StatusBadge tests** - Write unit tests
10. **Code highlighting parity** - Ensure Angular uses highlight.js

---

## Appendix: CSS Token Mapping Table

| React Token | React Value | Angular Token | Angular Value | Match? |
|------------|-------------|--------------|---------------|--------|
| `colors.background` | `#f5f5f7` | `--color-background` | `#f5f5f7` | ✅ |
| `colors.surface` | `#ffffff` | `--color-surface` | `#ffffff` | ✅ |
| `colors.primary` | `#007aff` | `--color-primary` | `#007aff` | ✅ |
| `colors.primaryHover` | `#0071e3` | `--color-primary-hover` | `#0071e3` | ✅ |
| `colors.text` | `#1d1d1f` | `--color-text` | `#1d1d1f` | ✅ |
| `colors.textSecondary` | `#86868b` | `--color-text-secondary` | `#86868b` | ✅ |
| `colors.success` | `#34c759` | `--color-success` | `#34c759` | ✅ |
| `colors.error` | `#ff3b30` | `--color-error` | `#ff3b30` | ✅ |
| `colors.border` | `rgba(0,0,0,0.08)` | `--color-border` | `rgba(0,0,0,0.08)` | ✅ |
| `shadows.sm` | `0 1px 2px rgba(0,0,0,0.04)` | `--shadow-sm` | `0 1px 2px rgba(0,0,0,0.04)` | ✅ |
| `shadows.card` | `0 2px 8px rgba(0,0,0,0.08)` | `--shadow-card` | `0 2px 8px rgba(0,0,0,0.08)` | ✅ |
| `radius.sm` | `6px` | `--radius-sm` | `6px` | ✅ |
| `radius.md` | `10px` | `--radius-md` | `10px` | ✅ |
| `radius.lg` | `14px` | `--radius-lg` | `14px` | ✅ |
| `spacing.xs` | `4px` | `--spacing-xs` | `4px` | ✅ |
| `spacing.sm` | `8px` | `--spacing-sm` | `8px` | ✅ |
| `spacing.md` | `16px` | `--spacing-md` | `16px` | ✅ |
| `typography.fontSize.base` | `14px` | `--font-size-base` | `14px` | ✅ |
| `typography.fontWeight.medium` | `500` | `--font-weight-medium` | `500` | ✅ |

---

## Angular-Specific Additional Components

### ToolResult (Angular Only)

**Status:** Angular-Only Feature  
**Purpose:** Display AI tool calls and their results

| Feature | Status |
|---------|--------|
| Tool name display | ✅ Implemented |
| Status indicators | ✅ pending/running/success/error |
| Expandable details | ✅ With input/output sections |
| JSON formatting | ✅ With syntax coloring |
| Image URL detection | ✅ Basic implementation |
| Spinner animation | ✅ For running state |

**Recommendation:** Port to React if needed for agent features

### AIInfraPanelComponent

**Status:** Angular-Only  
**Route:** `/aiinfra`  
**Purpose:** AI Infrastructure dashboard

**Recommendation:** Check if React equivalent exists

---

## Test Coverage Summary

| Component | React Tests | Angular Tests |
|-----------|-------------|---------------|
| AIHub | Basic | None |
| SegmentedControl | None | Comprehensive |
| StatusBadge | None | None |
| ChatMessage | None | None |
| VisionPanel | None | None |
| RAGChat | None | None |
| ImageZoomModal | None | Yes |
| Button | None | Comprehensive |
| Card | None | Yes |
| ToolResult | N/A | None |

---

## Conclusion

The Angular migration has made significant progress with **2 fully migrated components** (Button, Card, Theme) and **6 partially migrated components**. The most critical gaps exist in:

1. **AIHub** - Only 10% parity, needs complete feature implementation
2. **ChatMessage** - Missing markdown library, image zoom
3. **RAGChat** - Missing i18n, image zoom, advanced markdown

Total estimated work remaining:
- **High Priority:** ~15-20 hours
- **Medium Priority:** ~8-10 hours
- **Low Priority:** ~4-6 hours

Total: ~27-36 hours of development work remaining.
