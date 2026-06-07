import {
  Component,
  signal,
  computed,
  inject,
  OnInit,
  OnDestroy,
  ElementRef,
  viewChild,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer } from '@angular/platform-browser';
import { ApiService, ChatMessage, ProviderInfo, ModelInfo, Voice } from '../services/api.service';
import { I18nService } from '../../../i18n';

type Tab = 'chat' | 'image' | 'tts';

interface ChatMessageData {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: number;
}

interface ImageSize {
  label: string;
  width: number;
  height: number;
}

@Component({
  selector: 'app-ai-hub',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="ai-hub">
      <!-- Tab Navigation -->
      <div class="tab-header">
        <div class="segmented-control">
          @for (tab of tabs(); track tab.value) {
            <button
              class="segment-button"
              [class.active]="activeTab() === tab.value"
              (click)="setActiveTab(tab.value)"
            >
              {{ tab.label }}
            </button>
          }
        </div>
      </div>

      <!-- Tab Content -->
      <div class="tab-content">
        <!-- Chat Tab -->
        @if (activeTab() === 'chat') {
          <div class="panel">
            <div class="panel-header">
              <div>
                <h3 class="panel-title">{{ i18n.t().aiHub.chat.title }}</h3>
                <p class="panel-description">{{ i18n.t().aiHub.chat.description }}</p>
              </div>
            </div>
            <div class="panel-content">
              <!-- Model Selector -->
              @if (providers().length > 0) {
                <div class="model-selector">
                  <span class="model-label">{{ i18n.t().aiHub.chat.provider }}:</span>
                  <select
                    class="model-select"
                    [ngModel]="selectedProvider()"
                    (ngModelChange)="onProviderChange($event)"
                  >
                    @for (provider of providers(); track provider.name) {
                      <option [value]="provider.name">{{ provider.display_name }}</option>
                    }
                  </select>

                  <span class="model-label">{{ i18n.t().aiHub.chat.model }}:</span>
                  <select
                    class="model-select"
                    [ngModel]="selectedModel()"
                    (ngModelChange)="setSelectedModel($event)"
                    [disabled]="isLoadingModels()"
                  >
                    @if (isLoadingModels()) {
                      <option>Loading...</option>
                    } @else {
                      @for (model of models(); track model.name) {
                        <option [value]="model.name">{{ model.name }}</option>
                      }
                    }
                  </select>

                  @if (selectedModel()) {
                    <span class="model-badge">
                      {{ selectedProvider() }}/{{ selectedModel() }}
                    </span>
                  }
                </div>
              }

              <!-- Chat Messages -->
              <div class="chat-container" #chatContainer>
                @if (chatMessages().length === 0) {
                  <div class="empty-state">
                    <div class="empty-icon">
                      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                      </svg>
                    </div>
                    <p class="empty-title">{{ i18n.t().agents.startConversation }}</p>
                    <p class="empty-subtitle">{{ i18n.t().aiHub.chat.description }}</p>
                    <div class="quick-actions">
                      <button class="quick-action" (click)="setChatInput(i18n.t().aiHub.quickPrompts.greeting)">
                        {{ i18n.t().aiHub.quickPrompts.greeting }}
                      </button>
                      <button class="quick-action" (click)="setChatInput(i18n.t().aiHub.quickPrompts.help)">
                        {{ i18n.t().aiHub.quickPrompts.help }}
                      </button>
                      <button class="quick-action" (click)="setChatInput(i18n.t().aiHub.quickPrompts.creative)">
                        {{ i18n.t().aiHub.quickPrompts.creative }}
                      </button>
                    </div>
                  </div>
                } @else {
                  @for (msg of chatMessages(); track msg.id) {
                    <div class="message-bubble" [class.user]="msg.role === 'user'">
                      <div class="message-content" [class.user]="msg.role === 'user'">
                        @if (msg.role === 'user') {
                          {{ msg.content }}
                        } @else {
                          <div class="markdown-content" [innerHTML]="renderMarkdown(msg.content)"></div>
                        }
                      </div>
                      <span class="message-time">{{ formatTime(msg.timestamp) }}</span>
                    </div>
                  }
                  @if (isChatLoading() && chatMessages()[chatMessages().length - 1]?.content === '') {
                    <div class="message-bubble">
                      <div class="message-content">
                        <span class="btn-spinner"></span> {{ i18n.t().aiHub.chat.thinking }}
                      </div>
                    </div>
                  }
                  <div #messagesEnd></div>
                }
              </div>

              @if (chatError()) {
                <div class="error-message">{{ chatError() }}</div>
              }

              <!-- Input Area -->
              <div class="input-area">
                <textarea
                  class="chat-input"
                  [ngModel]="chatInput()"
                  (ngModelChange)="setChatInput($event)"
                  (keydown)="onChatKeyDown($event)"
                  placeholder="{{ i18n.t().aiHub.chat.inputPlaceholder }}"
                  rows="1"
                  [disabled]="isChatLoading()"
                ></textarea>
                <button
                  class="send-button"
                  (click)="sendMessage()"
                  [disabled]="isChatLoading() || !chatInput().trim()"
                >
                  →
                </button>
              </div>
            </div>
          </div>
        }

        <!-- Image Generation Tab -->
        @if (activeTab() === 'image') {
          <div class="panel">
            <div class="panel-header">
              <div>
                <h3 class="panel-title">{{ i18n.t().aiHub.image.title }}</h3>
                <p class="panel-description">{{ i18n.t().aiHub.image.description }}</p>
              </div>
            </div>
            <div class="panel-content">
              <div class="image-section">
                <!-- Prompt Area -->
                <div class="prompt-area">
                  <div class="input-group">
                    <label class="input-label">{{ i18n.t().aiHub.image.promptLabel }}</label>
                    <textarea
                      class="text-input"
                      [ngModel]="imagePrompt()"
                      (ngModelChange)="setImagePrompt($event)"
                      placeholder="{{ i18n.t().aiHub.image.promptPlaceholder }}"
                      rows="4"
                    ></textarea>
                  </div>

                  <div class="input-group">
                    <label class="input-label">{{ i18n.t().aiHub.image.negativePromptLabel }}</label>
                    <textarea
                      class="text-input"
                      [ngModel]="imageNegativePrompt()"
                      (ngModelChange)="setImageNegativePrompt($event)"
                      placeholder="{{ i18n.t().aiHub.image.negativePromptPlaceholder }}"
                      rows="2"
                    ></textarea>
                  </div>

                  <div class="input-group">
                    <label class="input-label">{{ i18n.t().aiHub.image.sizeLabel }}</label>
                    <div class="size-selector">
                      @for (size of imageSizes; track size.label) {
                        <button
                          class="size-option"
                          [class.selected]="imageSize().label === size.label"
                          (click)="setImageSize(size)"
                        >
                          {{ size.label }}
                        </button>
                      }
                    </div>
                  </div>

                  <button
                    class="action-button primary"
                    (click)="generateImage()"
                    [disabled]="!imagePrompt().trim() || isGeneratingImage()"
                  >
                    @if (isGeneratingImage()) {
                      <span class="btn-spinner"></span> {{ i18n.t().aiHub.image.generating }}
                    } @else {
                      {{ i18n.t().aiHub.image.generateButton }}
                    }
                  </button>
                </div>

                <!-- Preview Area -->
                <div class="preview-area">
                  <div class="image-area">
                    @if (isGeneratingImage()) {
                      <div class="loading-overlay">
                        <div class="loading-spinner"></div>
                        <span class="loading-text">Generating...</span>
                      </div>
                    }
                    @if (generatedImage()) {
                      <img
                        class="generated-image"
                        [src]="generatedImage()"
                        alt="Generated"
                        (click)="zoomImage(generatedImage()!)"
                      />
                    } @else {
                      <div class="empty-state">
                        <div class="empty-icon">
                          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                            <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                            <circle cx="8.5" cy="8.5" r="1.5"/>
                            <polyline points="21 15 16 10 5 21"/>
                          </svg>
                        </div>
                        <p class="empty-title">{{ i18n.t().aiHub.image.emptyState }}</p>
                      </div>
                    }
                  </div>

                  @if (imageError()) {
                    <div class="error-message">{{ imageError() }}</div>
                  }

                  @if (generatedImage()) {
                    <div class="image-actions">
                      <button class="icon-button" (click)="downloadImage()">
                        ⬇️ {{ i18n.t().aiHub.image.download }}
                      </button>
                    </div>
                  }
                </div>
              </div>
            </div>
          </div>
        }

        <!-- TTS Tab -->
        @if (activeTab() === 'tts') {
          <div class="panel">
            <div class="panel-header">
              <div>
                <h3 class="panel-title">{{ i18n.t().aiHub.tts.title }}</h3>
                <p class="panel-description">{{ i18n.t().aiHub.tts.description }}</p>
              </div>
            </div>
            <div class="panel-content">
              <div class="tts-section">
                <div class="input-group">
                  <label class="input-label">{{ i18n.t().aiHub.tts.textLabel }}</label>
                  <textarea
                    class="text-input"
                    [ngModel]="ttsText()"
                    (ngModelChange)="setTtsText($event)"
                    placeholder="{{ i18n.t().aiHub.tts.textPlaceholder }}"
                    rows="4"
                  ></textarea>
                </div>

                <div class="control-row">
                  <div class="input-group">
                    <label class="input-label">{{ i18n.t().aiHub.tts.voiceLabel }}</label>
                    <select
                      class="text-select"
                      [ngModel]="ttsVoice()"
                      (ngModelChange)="setTtsVoice($event)"
                    >
                      @if (availableVoices().length > 0) {
                        @for (voice of availableVoices(); track voice.id) {
                          <option [value]="voice.id">{{ voice.name }} ({{ voice.language }})</option>
                        }
                      } @else {
                        <option value="en-US">English (US)</option>
                        <option value="en-GB">English (UK)</option>
                        <option value="zh-CN">中文</option>
                        <option value="ja-JP">日本語</option>
                      }
                    </select>
                  </div>

                  <div class="slider-container">
                    <label class="input-label">{{ i18n.t().aiHub.tts.speedLabel }}: {{ ttsSpeed().toFixed(1) }}x</label>
                    <input
                      type="range"
                      class="slider"
                      min="0.5"
                      max="2.0"
                      step="0.1"
                      [ngModel]="ttsSpeed()"
                      (ngModelChange)="setTtsSpeed($event)"
                    />
                  </div>
                </div>

                <button
                  class="action-button primary"
                  (click)="synthesize()"
                  [disabled]="!ttsText().trim() || isSynthesizing()"
                >
                    @if (isSynthesizing()) {
                      <span class="btn-spinner"></span> {{ i18n.t().aiHub.tts.synthesizing }}
                    } @else {
                      {{ i18n.t().aiHub.tts.synthesizeButton }}
                    }
                </button>

                @if (ttsError()) {
                  <div class="error-message">{{ ttsError() }}</div>
                }

                @if (audioUrl()) {
                  <div class="audio-player">
                    <div class="audio-controls">
                      <button
                        class="play-button"
                        [class.playing]="isPlaying()"
                        (click)="togglePlayPause()"
                      >
                        {{ isPlaying() ? '⏸' : '▶' }}
                      </button>
                      <div class="audio-info">
                        <span class="audio-label">{{ i18n.t().aiHub.tts.audioReady }}</span>
                        <div class="audio-bar">
                          <div class="audio-progress" [style.width.%]="audioProgress()"></div>
                        </div>
                      </div>
                    </div>
                    <button class="download-link" (click)="downloadAudio()">
                      ⬇️ {{ i18n.t().aiHub.tts.downloadAudio }}
                    </button>
                  </div>
                }
              </div>
            </div>
          </div>
        }
      </div>

      <!-- Image Zoom Modal -->
      @if (zoomedImage()) {
        <div class="zoom-modal" (click)="closeZoom()">
          <div class="zoom-content">
            <button class="zoom-close" (click)="closeZoom()">×</button>
            <img [src]="zoomedImage()" alt="Zoomed" />
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .ai-hub {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .tab-header {
      display: flex;
      justify-content: center;
      padding: 16px 0;
      overflow-x: auto;
    }

    .segmented-control {
      display: flex;
      background: #f5f5f7;
      border-radius: 12px;
      padding: 4px;
      gap: 4px;
    }

    .segment-button {
      padding: 8px 20px;
      border: none;
      background: transparent;
      border-radius: 8px;
      cursor: pointer;
      font-size: 14px;
      font-weight: 500;
      color: #6e6e73;
      transition: all 0.2s ease;
    }

    .segment-button:hover {
      color: #1d1d1f;
    }

    .segment-button.active {
      background: white;
      color: #1d1d1f;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    }

    .tab-content {
      animation: fadeIn 0.3s ease;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(8px); }
      to { opacity: 1; transform: translateY(0); }
    }

    .panel {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .panel-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 16px;
      background: #ffffff;
      border: 1px solid #e5e5e5;
      border-radius: 12px;
    }

    .panel-title {
      font-size: 18px;
      font-weight: 600;
      color: #1d1d1f;
      margin: 0;
    }

    .panel-description {
      font-size: 14px;
      color: #6e6e73;
      margin: 4px 0 0 0;
    }

    .panel-content {
      background: #ffffff;
      border: 1px solid #e5e5e5;
      border-radius: 12px;
      padding: 24px;
    }

    .model-selector {
      display: flex;
      gap: 16px;
      align-items: center;
      flex-wrap: wrap;
      padding: 16px;
      background: #f5f5f7;
      border-radius: 8px;
      margin-bottom: 16px;
    }

    .model-label {
      font-size: 14px;
      font-weight: 500;
      color: #6e6e73;
    }

    .model-select {
      padding: 8px 16px;
      font-size: 14px;
      border: 1px solid #e5e5e5;
      border-radius: 8px;
      background: #ffffff;
      color: #1d1d1f;
      cursor: pointer;
      min-width: 120px;
    }

    .model-select:focus {
      outline: none;
      border-color: #0071e3;
    }

    .model-select:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .model-badge {
      font-size: 12px;
      color: #86868b;
      padding: 2px 8px;
      background: #e5e5e5;
      border-radius: 4px;
    }

    .chat-container {
      display: flex;
      flex-direction: column;
      gap: 12px;
      max-height: 400px;
      min-height: 200px;
      overflow-y: auto;
      padding: 16px;
      background: #ffffff;
      border-radius: 12px;
      border: 1px solid #e5e5e5;
    }

    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 48px;
      color: #6e6e73;
      text-align: center;
      gap: 8px;
    }

    .empty-icon {
      font-size: 48px;
      opacity: 0.5;
    }

    .empty-title {
      font-size: 16px;
      font-weight: 500;
      color: #1d1d1f;
      margin: 0;
    }

    .empty-subtitle {
      font-size: 14px;
      color: #86868b;
      margin: 0;
    }

    .quick-actions {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      justify-content: center;
      margin-top: 8px;
    }

    .quick-action {
      padding: 6px 12px;
      font-size: 14px;
      background: #ffffff;
      border: 1px solid #e5e5e5;
      border-radius: 20px;
      color: #0071e3;
      cursor: pointer;
      transition: all 0.15s ease;
    }

    .quick-action:hover {
      background: #f5f5f7;
    }

    .message-bubble {
      display: flex;
      flex-direction: column;
      max-width: 75%;
      animation: fadeIn 0.25s ease;
      align-self: flex-start;
      align-items: flex-start;
    }

    .message-bubble.user {
      align-self: flex-end;
      align-items: flex-end;
    }

    .message-content {
      padding: 12px;
      border-radius: 12px;
      font-size: 15px;
      line-height: 1.5;
      word-break: break-word;
      background: #f5f5f7;
      color: #1d1d1f;
      border: 1px solid #e5e5e5;
    }

    .message-content.user {
      background: #0071e3;
      color: white;
      border: none;
    }

    .markdown-content {
      line-height: 1.6;
    }

    .markdown-content h1, .markdown-content h2, .markdown-content h3 {
      margin: 0.5em 0 0.25em;
      font-weight: 600;
    }

    .markdown-content p {
      margin: 0.5em 0;
    }

    .markdown-content code {
      font-family: 'SF Mono', Monaco, monospace;
      font-size: 0.9em;
      padding: 0.15em 0.4em;
      border-radius: 4px;
      background: #e5e5e5;
    }

    .markdown-content pre {
      margin: 0.5em 0;
      padding: 12px;
      border-radius: 8px;
      background: #e5e5e5;
      overflow-x: auto;
    }

    .markdown-content blockquote {
      margin: 0.5em 0;
      padding-left: 1em;
      border-left: 3px solid #0071e3;
      color: #6e6e73;
    }

    .message-time {
      font-size: 11px;
      color: #86868b;
      margin-top: 4px;
      padding: 0 4px;
    }

    .input-area {
      display: flex;
      gap: 8px;
      align-items: flex-end;
      margin-top: 16px;
    }

    .chat-input {
      flex: 1;
      padding: 12px;
      font-size: 15px;
      font-family: inherit;
      border: 1px solid #e5e5e5;
      border-radius: 12px;
      background: #ffffff;
      color: #1d1d1f;
      resize: none;
      min-height: 48px;
      max-height: 120px;
      transition: border-color 0.15s, box-shadow 0.15s;
    }

    .chat-input:focus {
      outline: none;
      border-color: #0071e3;
      box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.1);
    }

    .chat-input::placeholder {
      color: #86868b;
    }

    .send-button {
      width: 48px;
      height: 48px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #0071e3;
      color: white;
      border: none;
      border-radius: 12px;
      cursor: pointer;
      font-size: 18px;
      transition: all 0.2s ease;
    }

    .send-button:hover:not(:disabled) {
      background: #0077ed;
    }

    .send-button:active:not(:disabled) {
      background: #005bb5;
      transform: scale(0.95);
    }

    .send-button:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .error-message {
      padding: 12px;
      background: #ffebee;
      color: #c62828;
      border-radius: 8px;
      font-size: 14px;
      animation: fadeIn 0.2s ease;
      border: 1px solid #ffcdd2;
    }

    .spinner {
      display: inline-block;
      width: 18px;
      height: 18px;
      border: 2px solid currentColor;
      border-right-color: transparent;
      border-radius: 50%;
      animation: spin 0.6s linear infinite;
    }

    .btn-spinner {
      display: inline-block;
      width: 16px;
      height: 16px;
      border: 2px solid rgba(255, 255, 255, 0.3);
      border-top-color: white;
      border-radius: 50%;
      animation: spin 0.7s linear infinite;
    }

    @keyframes spin {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }

    /* Image Generation Styles */
    .image-section {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 24px;
    }

    @media (max-width: 768px) {
      .image-section {
        grid-template-columns: 1fr;
      }
    }

    .prompt-area {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .preview-area {
      display: flex;
      flex-direction: column;
      gap: 12px;
      min-height: 300px;
    }

    .input-group {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .input-label {
      font-size: 14px;
      font-weight: 500;
      color: #6e6e73;
    }

    .text-input {
      padding: 12px;
      font-size: 15px;
      font-family: inherit;
      border: 1px solid #e5e5e5;
      border-radius: 8px;
      background: #ffffff;
      color: #1d1d1f;
      resize: vertical;
      min-height: 80px;
      transition: border-color 0.15s, box-shadow 0.15s;
    }

    .text-input:focus {
      outline: none;
      border-color: #0071e3;
      box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.1);
    }

    .text-input::placeholder {
      color: #86868b;
    }

    .text-select {
      padding: 12px;
      font-size: 15px;
      font-family: inherit;
      border: 1px solid #e5e5e5;
      border-radius: 8px;
      background: #ffffff;
      color: #1d1d1f;
      cursor: pointer;
      min-width: 200px;
    }

    .text-select:focus {
      outline: none;
      border-color: #0071e3;
    }

    .size-selector {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }

    .size-option {
      padding: 8px 16px;
      font-size: 14px;
      font-weight: 500;
      background: #ffffff;
      color: #1d1d1f;
      border: 1px solid #e5e5e5;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.15s ease;
    }

    .size-option:hover {
      border-color: #0071e3;
    }

    .size-option.selected {
      background: #0071e3;
      color: white;
      border: none;
    }

    .action-button {
      padding: 12px 24px;
      font-size: 15px;
      font-weight: 500;
      font-family: inherit;
      background: #ffffff;
      color: #1d1d1f;
      border: 1px solid #e5e5e5;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.15s ease;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }

    .action-button.primary {
      background: #0071e3;
      color: white;
      border: none;
    }

    .action-button.primary:hover:not(:disabled) {
      background: #0077ed;
    }

    .action-button:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .image-area {
      flex: 1;
      position: relative;
      border-radius: 12px;
      background: #f5f5f7;
      display: flex;
      align-items: center;
      justify-content: center;
      overflow: hidden;
      min-height: 256px;
      border: 2px dashed #e5e5e5;
    }

    .generated-image {
      max-width: 100%;
      max-height: 400px;
      object-fit: contain;
      border-radius: 8px;
      cursor: zoom-in;
      animation: fadeIn 0.3s ease;
    }

    .generated-image:hover {
      transform: scale(1.02);
    }

    .loading-overlay {
      position: absolute;
      inset: 0;
      background: rgba(255, 255, 255, 0.9);
      backdrop-filter: blur(8px);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 12px;
      border-radius: 12px;
      z-index: 5;
    }

    .loading-spinner {
      width: 44px;
      height: 44px;
      border: 3px solid #f5f5f7;
      border-top-color: #0071e3;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }

    .loading-text {
      font-size: 14px;
      font-weight: 500;
      color: #6e6e73;
    }

    .image-actions {
      display: flex;
      gap: 8px;
      justify-content: center;
    }

    .icon-button {
      padding: 8px 16px;
      font-size: 14px;
      font-weight: 500;
      background: #ffffff;
      color: #0071e3;
      border: 1px solid #e5e5e5;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.15s ease;
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .icon-button:hover {
      background: #f5f5f7;
      border-color: #0071e3;
    }

    /* TTS Styles */
    .tts-section {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .control-row {
      display: flex;
      gap: 24px;
      align-items: flex-end;
      flex-wrap: wrap;
    }

    @media (max-width: 640px) {
      .control-row {
        flex-direction: column;
      }
    }

    .slider-container {
      display: flex;
      flex-direction: column;
      gap: 8px;
      flex: 1;
      min-width: 150px;
    }

    .slider {
      width: 100%;
      height: 4px;
      background: #e5e5e5;
      border-radius: 2px;
      outline: none;
      -webkit-appearance: none;
    }

    .slider::-webkit-slider-thumb {
      -webkit-appearance: none;
      width: 18px;
      height: 18px;
      background: #0071e3;
      border-radius: 50%;
      cursor: pointer;
      transition: transform 0.15s ease;
    }

    .slider::-webkit-slider-thumb:hover {
      transform: scale(1.15);
    }

    .audio-player {
      display: flex;
      flex-direction: column;
      gap: 12px;
      padding: 24px;
      background: #f5f5f7;
      border: 1px solid #0071e333;
      border-radius: 12px;
      animation: fadeIn 0.3s ease;
    }

    .audio-controls {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .play-button {
      width: 52px;
      height: 52px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #0071e3;
      color: white;
      border: none;
      border-radius: 12px;
      cursor: pointer;
      font-size: 18px;
      transition: all 0.2s ease;
    }

    .play-button.playing {
      background: #34c759;
    }

    .play-button:hover {
      opacity: 0.9;
      transform: scale(1.02);
    }

    .play-button:active {
      transform: scale(0.95);
    }

    .audio-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .audio-label {
      font-size: 15px;
      font-weight: 500;
      color: #1d1d1f;
    }

    .audio-bar {
      height: 6px;
      background: #e5e5e5;
      border-radius: 3px;
      overflow: hidden;
    }

    .audio-progress {
      height: 100%;
      width: 0%;
      background: #0071e3;
      transition: width 0.1s linear;
      border-radius: 3px;
    }

    .download-link {
      padding: 8px 16px;
      font-size: 14px;
      font-weight: 500;
      background: #ffffff;
      color: #0071e3;
      border: 1px solid #e5e5e5;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.15s ease;
      display: flex;
      align-items: center;
      gap: 4px;
      width: fit-content;
    }

    .download-link:hover {
      background: #f5f5f7;
      border-color: #0071e3;
    }

    /* Zoom Modal */
    .zoom-modal {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.9);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      cursor: pointer;
    }

    .zoom-content {
      position: relative;
      max-width: 90vw;
      max-height: 90vh;
    }

    .zoom-content img {
      max-width: 100%;
      max-height: 90vh;
      object-fit: contain;
    }

    .zoom-close {
      position: absolute;
      top: -40px;
      right: 0;
      width: 32px;
      height: 32px;
      background: rgba(255, 255, 255, 0.2);
      border: none;
      border-radius: 50%;
      color: white;
      font-size: 24px;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .zoom-close:hover {
      background: rgba(255, 255, 255, 0.3);
    }
  `]
})
export class AiHubComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  private readonly sanitizer = inject(DomSanitizer);
  protected readonly i18n = inject(I18nService);
  private readonly sessionId = `aihub_${Date.now()}`;

  // Tab state
  activeTab = signal<Tab>('chat');
  tabs = computed(() => {
    const t = this.i18n.t().aiHub.tabs;
    return [
      { value: 'chat' as Tab, label: t.chat },
      { value: 'image' as Tab, label: t.image },
      { value: 'tts' as Tab, label: t.tts },
    ];
  });

  // Chat state
  chatMessages = signal<ChatMessageData[]>([]);
  chatInput = signal('');
  isChatLoading = signal(false);
  chatError = signal<string | null>(null);
  private chatAbortController: AbortController | null = null;

  // Model selection state
  providers = signal<ProviderInfo[]>([]);
  models = signal<ModelInfo[]>([]);
  selectedProvider = signal('openai');
  selectedModel = signal('gpt-4o-mini');
  isLoadingModels = signal(false);

  // Image generation state
  imagePrompt = signal('');
  imageNegativePrompt = signal('');
  imageSizes: ImageSize[] = [
    { label: '512x512', width: 512, height: 512 },
    { label: '768x768', width: 768, height: 768 },
    { label: '1024x1024', width: 1024, height: 1024 },
  ];
  imageSize = signal<ImageSize>(this.imageSizes[2]);
  generatedImage = signal<string | null>(null);
  isGeneratingImage = signal(false);
  imageError = signal<string | null>(null);
  zoomedImage = signal<string | null>(null);

  // TTS state
  ttsText = signal('');
  ttsVoice = signal('en-US');
  ttsSpeed = signal(1.0);
  availableVoices = signal<Voice[]>([]);
  isSynthesizing = signal(false);
  ttsError = signal<string | null>(null);
  audioUrl = signal<string | null>(null);
  audioBlob = signal<Blob | null>(null);
  isPlaying = signal(false);
  audioProgress = signal(0);
  private audioElement: HTMLAudioElement | null = null;

  // ViewChild for scrolling
  messagesEnd = viewChild<ElementRef>('messagesEnd');

  ngOnInit() {
    this.loadVoices();
    this.loadProviders();
  }

  ngOnDestroy() {
    if (this.chatAbortController) {
      this.chatAbortController.abort();
    }
    if (this.audioElement) {
      this.audioElement.pause();
    }
    if (this.audioUrl()) {
      URL.revokeObjectURL(this.audioUrl()!);
    }
  }

  // ==================== Tab Navigation ====================

  setActiveTab(tab: Tab) {
    this.activeTab.set(tab);
  }

  // ==================== Chat ====================

  loadProviders() {
    this.api.getProviders().subscribe({
      next: (data) => {
        this.providers.set(data);
        if (data.length > 0) {
          this.selectedProvider.set(data[0].name);
          this.loadModelsForProvider(data[0].name);
        }
      },
      error: () => {
        // Use defaults
        this.providers.set([
          { name: 'openai', display_name: 'OpenAI', models: ['gpt-4o', 'gpt-4o-mini'], status: 'available' },
          { name: 'anthropic', display_name: 'Anthropic Claude', models: ['claude-sonnet-4-20250514'], status: 'available' },
        ]);
      },
    });
  }

  loadModelsForProvider(provider: string) {
    this.isLoadingModels.set(true);
    this.api.getModels(provider).subscribe({
      next: (data) => {
        this.models.set(data);
        if (data.length > 0) {
          const defaultModel = data.find((m) => m.name.includes('mini') || m.name.includes('3.5')) || data[0];
          this.selectedModel.set(defaultModel.name);
        }
      },
      error: () => {
        const providerData: Record<string, string[]> = {
          openai: ['gpt-4o', 'gpt-4o-mini', 'gpt-4-turbo'],
          anthropic: ['claude-sonnet-4-20250514', 'claude-opus-4-20250514'],
          ollama: ['qwen2.5:7b', 'llama3.2:3b'],
        };
        const modelList = providerData[provider] || providerData['openai'];
        this.models.set(modelList.map((name) => ({ name, provider })));
        this.selectedModel.set(modelList[0]);
      },
      complete: () => {
        this.isLoadingModels.set(false);
      },
    });
  }

  onProviderChange(provider: string) {
    this.selectedProvider.set(provider);
    this.loadModelsForProvider(provider);
  }

  setSelectedModel(model: string) {
    this.selectedModel.set(model);
  }

  setChatInput(text: string) {
    this.chatInput.set(text);
  }

  onChatKeyDown(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  sendMessage() {
    if (!this.chatInput().trim() || this.isChatLoading()) return;

    // Cancel any existing request
    if (this.chatAbortController) {
      this.chatAbortController.abort();
    }
    this.chatAbortController = new AbortController();

    const userMessage: ChatMessageData = {
      id: `user_${Date.now()}`,
      role: 'user',
      content: this.chatInput().trim(),
      timestamp: Date.now(),
    };

    this.chatMessages.update((msgs) => [...msgs, userMessage]);
    this.chatInput.set('');
    this.isChatLoading.set(true);
    this.chatError.set(null);

    const assistantMessageId = `assistant_${Date.now()}`;
    this.chatMessages.update((msgs) => [
      ...msgs,
      { id: assistantMessageId, role: 'assistant', content: '', timestamp: Date.now() },
    ]);

    const messages: ChatMessage[] = [
      ...this.chatMessages()
        .filter((m) => m.role === 'user' || (m.role === 'assistant' && m.content))
        .map((m) => ({ role: m.role, content: m.content })),
      { role: 'user', content: userMessage.content },
    ];

    let fullContent = '';

    this.api.chatStream(
      {
        messages,
        session_id: this.sessionId,
        provider: this.selectedProvider(),
        model: this.selectedModel(),
      },
      (chunk) => {
        fullContent += chunk;
        this.chatMessages.update((msgs) =>
          msgs.map((msg) => (msg.id === assistantMessageId ? { ...msg, content: fullContent } : msg))
        );
      },
      () => {
        this.isChatLoading.set(false);
        this.chatAbortController = null;
      },
      (error) => {
        let msg = error.message;
        if (msg.includes('Failed to fetch') || msg.includes('NetworkError')) {
          msg = 'Text Service unavailable. Please ensure the service is running.';
        }
        this.chatError.set(msg);
        this.chatMessages.update((msgs) =>
          msgs.map((m) => (m.id === assistantMessageId ? { ...m, content: msg } : m))
        );
        this.isChatLoading.set(false);
        this.chatAbortController = null;
      }
    );
  }

  // ==================== Image Generation ====================

  setImagePrompt(text: string) {
    this.imagePrompt.set(text);
  }

  setImageNegativePrompt(text: string) {
    this.imageNegativePrompt.set(text);
  }

  setImageSize(size: ImageSize) {
    this.imageSize.set(size);
  }

  generateImage() {
    if (!this.imagePrompt().trim() || this.isGeneratingImage()) return;

    this.isGeneratingImage.set(true);
    this.imageError.set(null);
    this.generatedImage.set(null);

    this.api
      .generateImage({
        prompt: this.imagePrompt(),
        negative_prompt: this.imageNegativePrompt() || undefined,
        width: this.imageSize().width,
        height: this.imageSize().height,
        num_images: 1,
      })
      .subscribe({
        next: (result) => {
          if (result.images && result.images.length > 0) {
            this.generatedImage.set(`data:image/png;base64,${result.images[0]}`);
          }
        },
        error: (err) => {
          this.imageError.set(err instanceof Error ? err.message : 'Image generation failed');
        },
        complete: () => {
          this.isGeneratingImage.set(false);
        },
      });
  }

  downloadImage() {
    if (this.generatedImage()) {
      const base64 = this.generatedImage()!.replace('data:image/png;base64,', '');
      this.api.downloadBase64Image(base64, `ai_generated_${Date.now()}.png`);
    }
  }

  zoomImage(src: string) {
    this.zoomedImage.set(src);
  }

  closeZoom() {
    this.zoomedImage.set(null);
  }

  // ==================== TTS ====================

  loadVoices() {
    this.api.getVoices().subscribe({
      next: (voices) => {
        this.availableVoices.set(voices);
        const defaultVoice = voices.find((v) => v.is_default) || voices[0];
        if (defaultVoice) {
          this.ttsVoice.set(defaultVoice.id);
        }
      },
      error: () => {
        // Use defaults
        this.availableVoices.set([
          { id: 'en-US', name: 'English (US)', language: 'en-US', provider: 'default', is_default: true },
        ]);
      },
    });
  }

  setTtsText(text: string) {
    this.ttsText.set(text);
  }

  setTtsVoice(voice: string) {
    this.ttsVoice.set(voice);
  }

  setTtsSpeed(speed: number) {
    this.ttsSpeed.set(speed);
  }

  synthesize() {
    if (!this.ttsText().trim() || this.isSynthesizing()) return;

    this.isSynthesizing.set(true);
    this.ttsError.set(null);

    this.api
      .synthesizeSpeech({
        text: this.ttsText(),
        voice: this.ttsVoice() || undefined,
        speed: this.ttsSpeed(),
        output_format: 'mp3',
      })
      .subscribe({
        next: (blob) => {
          // Clean up previous audio URL
          if (this.audioUrl()) {
            URL.revokeObjectURL(this.audioUrl()!);
          }

          const url = URL.createObjectURL(blob);
          this.audioUrl.set(url);
          this.audioBlob.set(blob);

          // Set up audio element
          this.audioElement = new Audio(url);
          this.audioElement.addEventListener('ended', () => this.isPlaying.set(false));
          this.audioElement.addEventListener('timeupdate', () => {
            if (this.audioElement) {
              this.audioProgress.set((this.audioElement.currentTime / this.audioElement.duration) * 100);
            }
          });
        },
        error: (err) => {
          this.ttsError.set(err instanceof Error ? err.message : 'Synthesis failed');
        },
        complete: () => {
          this.isSynthesizing.set(false);
        },
      });
  }

  togglePlayPause() {
    if (!this.audioElement) return;

    if (this.isPlaying()) {
      this.audioElement.pause();
      this.isPlaying.set(false);
    } else {
      this.audioElement.play();
      this.isPlaying.set(true);
    }
  }

  downloadAudio() {
    const blob = this.audioBlob();
    if (blob) {
      this.api.downloadBlob(blob, `speech_${Date.now()}.mp3`);
    }
  }

  // ==================== Utilities ====================

  formatTime(timestamp: number): string {
    return new Date(timestamp).toLocaleTimeString();
  }

  renderMarkdown(content: string): string {
    // Simple markdown rendering for demo
    // In production, use a proper markdown library
    let html = content
      .replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code>$2</code></pre>')
      .replace(/`([^`]+)`/g, '<code>$1</code>')
      .replace(/^### (.*$)/gm, '<h3>$1</h3>')
      .replace(/^## (.*$)/gm, '<h2>$1</h2>')
      .replace(/^# (.*$)/gm, '<h1>$1</h1>')
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/g, '<em>$1</em>')
      .replace(/^> (.*$)/gm, '<blockquote>$1</blockquote>')
      .replace(/^- (.*$)/gm, '<li>$1</li>')
      .replace(/\n/g, '<br>');

    return this.sanitizer.sanitize(1, html) || content;
  }
}
