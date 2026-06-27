import {
  Component,
  inject,
  OnInit,
  ElementRef,
  viewChild,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RagService } from './rag.service';
import { MarkdownService } from '@shared/utils/markdown.service';
import { I18nService } from '@core/i18n';

interface UploadedDocument {
  id: string;
  title: string;
  status: 'uploading' | 'success' | 'error';
  progress?: number;
  error?: string;
}

@Component({
  selector: 'app-rag-page',
  imports: [CommonModule, FormsModule],
  standalone: true,
  template: `
    <div class="rag-chat">
      <!-- Header -->
      <div class="header">
        <h2 class="title">{{ i18n.t().ragChat.title }}</h2>
        <span class="model-badge">{{ i18n.t().ragChat.modelBadge }}</span>
      </div>

      <!-- Documents Section -->
      <div class="documents-section">
        <div class="section-header">
          <h3 class="section-title">
            📄 {{ i18n.t().ragChat.documents }}
            @if (!ragService.isLoadingDocs()) {
              <span class="document-count">{{ ragService.availableDocs().length }}</span>
            }
          </h3>
          @if (ragService.availableDocs().length > 0 && ragService.selectedDocIds().size > 0) {
            <span class="selected-badge">
              {{
                i18n
                  .t()
                  .ragChat.selectedDocuments.replace('{count}', ragService.selectedDocIds().size.toString())
              }}
            </span>
          }
        </div>

        @if (ragService.isLoadingDocs()) {
          <div class="skeleton-row">
            <div class="skeleton skeleton-doc"></div>
            <div class="skeleton skeleton-doc" style="width: 100px"></div>
            <div class="skeleton skeleton-doc" style="width: 140px"></div>
          </div>
        } @else if (ragService.availableDocs().length > 0) {
          <div class="documents-list">
            @for (doc of ragService.availableDocs(); track $index) {
              <div
                class="document-card"
                [class.selected]="ragService.selectedDocIds().has(doc.id)"
                [class.deleting]="ragService.deletingDocIds().has(doc.id)"
                (click)="ragService.toggleDocSelection(doc.id)"
                (keydown)="onDocKeyDown($event, doc.id)"
                tabindex="0"
                role="checkbox"
                [attr.aria-checked]="ragService.selectedDocIds().has(doc.id)"
              >
                <span class="doc-icon">
                  {{ ragService.selectedDocIds().has(doc.id) ? '✓' : '📄' }}
                </span>
                <span class="doc-title">{{ doc.title }}</span>
                <button
                  type="button"
                  class="delete-button"
                  [class.selected]="ragService.selectedDocIds().has(doc.id)"
                  (click)="deleteDocument(doc.id, $event)"
                  aria-label="Delete {{ doc.title }}"
                >
                  ✕
                </button>
              </div>
            }
          </div>
          <div class="selection-controls">
            <button type="button" class="select-button" (click)="ragService.selectAllDocs()">
              {{ i18n.t().ragChat.selectAll }}
            </button>
            @if (ragService.selectedDocIds().size > 0) {
              <button type="button" class="select-button" (click)="ragService.clearDocSelection()">
                {{ i18n.t().ragChat.clearSelection }}
              </button>
            }
          </div>
        } @else {
          <p class="empty-docs">{{ i18n.t().ragChat.noDocuments }}</p>
        }
      </div>

      <!-- File Upload Area -->
      <div class="file-upload-area">
        <input
          #fileInput
          type="file"
          id="file-upload"
          accept=".pdf,.md,.txt,.markdown"
          multiple
          (change)="onFileSelect($event)"
          class="visually-hidden"
        />
        <label for="file-upload" class="file-upload-label">
          📎 {{ i18n.t().ragChat.uploadDocs }}
        </label>

        @if (ragService.pendingFiles().length > 0) {
          <div class="uploaded-files">
            @for (file of ragService.pendingFiles(); track file.name) {
              <div class="file-tag" [class]="getUploadStatus(file.name)?.status || 'pending'">
                @if (getUploadStatus(file.name)?.status === 'uploading') {
                  <div class="upload-progress">
                    <div class="upload-progress-bar" [style.width.%]="50"></div>
                  </div>
                }
                {{ file.name }}
                <button
                  type="button"
                  class="remove-button"
                  (click)="ragService.removePendingFile($index)"
                  aria-label="Remove file"
                >×</button>
              </div>
            }
          </div>
        }

        @if (ragService.pendingFiles().length > 0) {
          <button type="button" class="upload-button" (click)="uploadFiles()" [disabled]="ragService.isUploading()">
            @if (ragService.isUploading()) {
              <span class="spinner"></span> {{ i18n.t().ragChat.uploading }}
            } @else {
              ↑ {{ i18n.t().ragChat.upload }} ({{ ragService.pendingFiles().length }})
            }
          </button>
        }
      </div>

      <!-- Chat Container -->
      <div class="chat-container" #chatContainer>
        @if (ragService.messages().length === 0) {
          <div class="empty-state">
            <div class="empty-icon">💬</div>
            <p>{{ i18n.t().ragChat.askQuestion }}</p>
            <div class="quick-actions">
              <button type="button" class="quick-action" (click)="setInput(i18n.t().ragChat.whatIsThis)">
                {{ i18n.t().ragChat.whatIsThis }}
              </button>
              <button type="button" class="quick-action" (click)="setInput(i18n.t().ragChat.summarize)">
                {{ i18n.t().ragChat.summarize }}
              </button>
              <button type="button" class="quick-action" (click)="setInput(i18n.t().ragChat.keyInfo)">
                {{ i18n.t().ragChat.keyInfo }}
              </button>
            </div>
          </div>
        } @else {
          @for (msg of ragService.messages(); track msg.id) {
            <div class="message-bubble" [class.user]="msg.role === 'user'">
              <div class="message-content" [class.user]="msg.role === 'user'">
                @if (msg.role === 'user') {
                  {{ msg.content }}
                } @else {
                  <div class="markdown-content" [innerHTML]="renderMarkdown(msg.content)"></div>
                }
              </div>
              <div class="message-meta">
                <span class="message-time">{{ formatTime(msg.timestamp) }}</span>
                @if (msg.role === 'assistant' && msg.sources && msg.sources.length > 0) {
                  <span
                    class="source-badge"
                    (click)="ragService.toggleSources(msg.id)"
                    (keydown.enter)="ragService.toggleSources(msg.id)"
                    tabindex="0"
                    role="button"
                  >
                    📚
                    {{ i18n.t().ragChat.basedOn.replace('{count}', msg.sources.length.toString()) }}
                    {{ ragService.expandedSources().has(msg.id) ? '▲' : '▼' }}
                  </span>
                }
              </div>
              @if (msg.role === 'assistant' && msg.sources && ragService.expandedSources().has(msg.id)) {
                <div class="sources-panel">
                  <div class="sources-title">📖 {{ i18n.t().ragChat.sources }}</div>
                  @for (source of msg.sources.slice(0, 3); track $index) {
                    <div class="source-item">
                      <div class="source-text">
                        {{
                          source.text.length > 200 ? source.text.slice(0, 200) + '...' : source.text
                        }}
                      </div>
                      <div class="source-meta">
                        <span
                          >{{ i18n.t().ragChat.similarity }}:
                          {{ (source.score * 100).toFixed(1) }}%</span
                        >
                      </div>
                    </div>
                  }
                </div>
              }
            </div>
          }
          @if (
            ragService.isLoading() &&
            ragService.messages()[ragService.messages().length - 1]?.role === 'assistant' &&
            !ragService.messages()[ragService.messages().length - 1]?.content
          ) {
            <div class="message-bubble">
              <div class="message-content">
                <span class="spinner"></span> {{ i18n.t().ragChat.thinking }}
              </div>
            </div>
          }
          <div #messagesEnd></div>
        }
      </div>

      <!-- Input Area -->
      <div class="input-area">
        <textarea
          class="chat-input"
          [value]="ragService.input()"
          (input)="ragService.input.set($any($event.target).value)"
          (keydown)="onInputKeyDown($event)"
          [placeholder]="i18n.t().ragChat.inputPlaceholder"
          rows="1"
          [disabled]="ragService.isLoading()"
        ></textarea>
        <button
          type="button"
          class="send-button"
          (click)="ragService.sendMessage()"
          [disabled]="ragService.isLoading() || !ragService.input().trim()"
        >
          @if (ragService.isLoading()) {
            <span class="spinner"></span>
          } @else {
            →
          }
        </button>
      </div>
    </div>
  `,
  styles: [
    `
      .rag-chat {
        display: flex;
        flex-direction: column;
        gap: 16px;
        animation: fadeIn 0.3s ease;
        overflow-x: hidden;
        word-break: break-word;
      }

      @media (max-width: 640px) {
        .rag-chat {
          overflow-x: hidden;
          padding: 0 8px;
        }
      }

      @keyframes fadeIn {
        from {
          opacity: 0;
          transform: translateY(8px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }

      .header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding-bottom: 16px;
        border-bottom: 1px solid rgba(0, 0, 0, 0.08);
      }

      .title {
        font-size: 20px;
        font-weight: 600;
        color: #1d1d1f;
        margin: 0;
      }

      .model-badge {
        font-size: 12px;
        color: #86868b;
        background: #ffffff;
        padding: 4px 8px;
        border-radius: 20px;
        border: 1px solid var(--color-border);
      }

      .documents-section {
        background: #ffffff;
        border: 1px solid var(--color-border);
        border-radius: 14px;
        padding: 16px;
      }

      .section-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 12px;
      }

      .section-title {
        font-size: 15px;
        font-weight: 500;
        color: #1d1d1f;
        margin: 0;
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .document-count {
        font-size: 12px;
        color: #6e6e73;
        background: #f5f5f7;
        padding: 2px 8px;
        border-radius: 20px;
      }

      .selected-badge {
        font-size: 12px;
        color: #0071e3;
        font-weight: 500;
      }

      .documents-list {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        min-height: 32px;
      }

      .document-card {
        display: flex;
        align-items: center;
        gap: 4px;
        padding: 8px 14px;
        font-size: 14px;
        background: #ffffff;
        color: #1d1d1f;
        border-radius: 14px;
        border: 1.5px solid rgba(0, 0, 0, 0.08);
        cursor: pointer;
        transition: all 0.35s cubic-bezier(0.32, 0.72, 0, 1);
        user-select: none;
      }

      .document-card:hover {
        border-color: #007aff;
        transform: translateY(-2px);
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
      }

      .document-card:active {
        transform: scale(0.98);
      }

      .document-card.selected {
        background: #007aff;
        color: white;
        border-color: #007aff;
        box-shadow: 0 2px 8px rgba(0, 122, 255, 0.3);
      }

      .document-card.deleting {
        animation: fadeOut 0.3s ease forwards;
        pointer-events: none;
      }

      @keyframes fadeOut {
        from {
          opacity: 1;
          transform: scale(1);
        }
        to {
          opacity: 0;
          transform: scale(0.95);
        }
      }

      .doc-icon {
        font-size: 16px;
        opacity: 0.7;
      }

      .document-card.selected .doc-icon {
        opacity: 1;
      }

      .doc-title {
        max-width: 150px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .delete-button {
        background: #f5f5f7;
        border: none;
        padding: 4px 6px;
        font-size: 14px;
        cursor: pointer;
        opacity: 0.6;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 4px;
        transition: all 0.15s ease;
        margin-left: 4px;
        color: #6e6e73;
      }

      .document-card.selected .delete-button {
        background: rgba(255, 255, 255, 0.25);
        color: white;
      }

      .delete-button:hover {
        opacity: 1;
        transform: scale(1.1);
      }

      .selection-controls {
        display: flex;
        gap: 8px;
        margin-top: 8px;
        flex-wrap: wrap;
      }

      .select-button {
        padding: 4px 10px;
        font-size: 12px;
        background: transparent;
        border: 1px solid #e5e5e5;
        border-radius: 20px;
        color: #6e6e73;
        cursor: pointer;
        transition: all 0.15s ease;
      }

      .select-button:hover {
        background: #f5f5f7;
        color: #1d1d1f;
      }

      .empty-docs {
        font-size: 14px;
        color: #86868b;
        font-style: italic;
      }

      .skeleton-row {
        display: flex;
        gap: 8px;
        flex-wrap: wrap;
      }

      .skeleton {
        background: linear-gradient(90deg, #f5f5f7 0%, #e5e5ea 50%, #f5f5f7 100%);
        background-size: 200% 100%;
        animation: shimmer 1.8s ease-in-out infinite;
        border-radius: 20px;
      }

      .skeleton-doc {
        height: 28px;
        width: 120px;
      }

      @keyframes shimmer {
        0% {
          background-position: 200% 0;
        }
        100% {
          background-position: -200% 0;
        }
      }

      .file-upload-area {
        display: flex;
        gap: 8px;
        padding: 12px;
        background: #ffffff;
        border: 1px dashed rgba(0, 0, 0, 0.08);
        border-radius: 14px;
        transition: all 0.15s ease;
        flex-wrap: wrap;
        align-items: center;
        z-index: 10;
        position: relative;
      }

      .file-upload-area:hover {
        border-color: #007aff;
      }

      .file-upload-label {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px 16px;
        font-size: 14px;
        color: #007aff;
        cursor: pointer;
        transition: all 0.15s ease;
        border: 1px solid #007aff;
        border-radius: 10px;
      }

      .file-upload-label:hover {
        background: rgba(0, 122, 255, 0.12);
      }

      .uploaded-files {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        flex: 1;
      }

      .file-tag {
        display: flex;
        align-items: center;
        gap: 4px;
        padding: 4px 8px;
        font-size: 12px;
        border-radius: 4px;
        animation: fadeIn 0.2s ease;
      }

      .file-tag.uploading {
        background: #fff3cd;
        color: #856404;
        border: 1px solid #ffc107;
      }

      .file-tag.success {
        background: #d4edda;
        color: #155724;
        border: 1px solid #c3e6cb;
      }

      .file-tag.error {
        background: #f8d7da;
        color: #721c24;
        border: 1px solid #f5c6cb;
      }

      .upload-progress {
        width: 60px;
        height: 4px;
        background: #e5e5e5;
        border-radius: 2px;
        overflow: hidden;
      }

      .upload-progress-bar {
        height: 100%;
        background: #007aff;
        animation: pulse 1s ease-in-out infinite;
      }

      @keyframes pulse {
        0%,
        100% {
          opacity: 1;
        }
        50% {
          opacity: 0.5;
        }
      }

      .remove-button {
        background: none;
        border: none;
        padding: 0;
        cursor: pointer;
        font-size: 14px;
        opacity: 0.7;
        display: flex;
        align-items: center;
      }

      .remove-button:hover {
        opacity: 1;
      }

      .upload-button {
        padding: 8px 16px;
        font-size: 14px;
        background: #007aff;
        color: white;
        border: none;
        border-radius: 10px;
        cursor: pointer;
        transition: all 0.15s ease;
        margin-left: auto;
        display: flex;
        align-items: center;
        gap: 4px;
      }

      .upload-button:hover:not(:disabled) {
        background: #0071e3;
      }

      .upload-button:disabled {
        opacity: 0.5;
        cursor: not-allowed;
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
        border-radius: 14px;
        border: 1px solid var(--color-border);
        position: relative;
        z-index: 1;
      }

      @media (max-width: 640px) {
        .chat-container {
          max-height: 300px;
          padding: 12px;
          border-radius: 8px;
        }
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

      .quick-actions {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        margin-top: 8px;
      }

      .quick-action {
        padding: 6px 12px;
        font-size: 14px;
        background: #ffffff;
        border: 1px solid var(--color-border);
        border-radius: 20px;
        color: #007aff;
        cursor: pointer;
        transition: all 0.15s ease;
      }

      .quick-action:hover {
        background: rgba(0, 122, 255, 0.12);
        border-color: #007aff;
      }

      .message-bubble {
        display: flex;
        flex-direction: column;
        max-width: 80%;
        animation: fadeIn 0.2s ease;
        align-self: flex-start;
        align-items: flex-start;
      }

      .message-bubble.user {
        align-self: flex-end;
        align-items: flex-end;
      }

      .message-content {
        padding: 12px;
        border-radius: 14px;
        font-size: 15px;
        line-height: 1.6;
        word-break: break-word;
        background: rgba(0, 0, 0, 0.04);
        color: #1d1d1f;
      }

      .message-content.user {
        background: #007aff;
        color: white;
        border-bottom-right-radius: 6px;
      }

      .message-bubble:not(.user) .message-content {
        border-bottom-left-radius: 4px;
      }

      .markdown-content {
        line-height: 1.6;
        white-space: pre-wrap;
        word-break: break-word;
      }

      .markdown-content h1,
      .markdown-content h2,
      .markdown-content h3 {
        margin: 0.6em 0 0.3em;
        font-weight: 600;
        color: #1c1c1e;
      }

      .markdown-content h1 {
        font-size: 1.4em;
      }
      .markdown-content h2 {
        font-size: 1.2em;
      }
      .markdown-content h3 {
        font-size: 1.1em;
      }

      .markdown-content p {
        margin: 0.5em 0;
        line-height: 1.6;
      }

      .markdown-content ul,
      .markdown-content ol {
        margin: 0.5em 0;
        padding-left: 1.5em;
      }

      .markdown-content li {
        margin: 0.25em 0;
        line-height: 1.5;
      }

      .markdown-content blockquote {
        margin: 0.5em 0;
        padding: 0.5em 1em;
        border-left: 3px solid #007aff;
        background: rgba(0, 122, 255, 0.05);
        color: #636366;
      }

      .markdown-content code {
        font-family: 'SF Mono', Monaco, 'Cascadia Code', monospace;
        font-size: 0.9em;
        padding: 0.15em 0.4em;
        border-radius: 4px;
        background: rgba(0, 0, 0, 0.06);
        color: #d63384;
      }

      .markdown-content pre {
        margin: 0.75em 0;
        padding: 14px;
        border-radius: 10px;
        background: #1e1e1e;
        overflow-x: auto;
      }

      .markdown-content pre code {
        background: transparent;
        color: #d4d4d4;
        padding: 0;
        font-size: 0.85em;
        line-height: 1.5;
      }

      .markdown-content a {
        color: #007aff;
        text-decoration: none;
      }

      .markdown-content a:hover {
        text-decoration: underline;
      }

      .markdown-content table {
        width: 100%;
        border-collapse: collapse;
        margin: 0.5em 0;
        font-size: 0.9em;
      }

      .markdown-content th,
      .markdown-content td {
        padding: 8px 12px;
        border: 1px solid #e5e5ea;
        text-align: left;
      }

      .markdown-content th {
        background: rgba(0, 0, 0, 0.03);
        font-weight: 600;
      }

      .message-meta {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-top: 4px;
        padding: 0 4px;
      }

      .message-time {
        font-size: 11px;
        color: #86868b;
      }

      .source-badge {
        font-size: 12px;
        color: #007aff;
        background: rgba(0, 122, 255, 0.12);
        padding: 2px 6px;
        border-radius: 6px;
        cursor: pointer;
        transition: all 0.15s ease;
      }

      .source-badge:hover {
        background: rgba(0, 122, 255, 0.2);
      }

      .sources-panel {
        margin-top: 8px;
        padding: 8px;
        background: #ffffff;
        border-radius: 8px;
        border: 1px solid #e5e5e5;
        font-size: 14px;
        width: 100%;
        max-width: 400px;
      }

      .sources-title {
        font-weight: 500;
        color: #6e6e73;
        margin-bottom: 8px;
        display: flex;
        align-items: center;
        gap: 4px;
      }

      .source-item {
        padding: 8px;
        background: #f5f5f7;
        border-radius: 4px;
        margin-bottom: 8px;
        border-left: 3px solid #0071e3;
      }

      .source-item:last-of-type {
        margin-bottom: 0;
      }

      .source-text {
        color: #1d1d1f;
        line-height: 1.6;
        margin-bottom: 4px;
      }

      .source-meta {
        display: flex;
        justify-content: space-between;
        font-size: 12px;
        color: #86868b;
      }

      .input-area {
        display: flex;
        gap: 8px;
        align-items: flex-end;
        position: relative;
        z-index: 2;
      }

      @media (max-width: 640px) {
        .input-area {
          gap: 6px;
        }
      }

      .chat-input {
        flex: 1;
        padding: 12px;
        font-size: 15px;
        font-family: inherit;
        border: 1px solid var(--color-border);
        border-radius: 14px;
        background: #ffffff;
        color: #1d1d1f;
        resize: none;
        min-height: 48px;
        max-height: 120px;
        transition:
          border-color 0.15s,
          box-shadow 0.15s;
      }

      .chat-input:focus {
        outline: none;
        border-color: #007aff;
        box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.2);
      }

      .chat-input::placeholder {
        color: #86868b;
      }

      .chat-input::-webkit-input-placeholder {
        color: #86868b;
      }

      .chat-input::-moz-placeholder {
        color: #86868b;
        opacity: 1;
      }

      .send-button {
        width: 48px;
        height: 48px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #007aff;
        color: white;
        border: none;
        border-radius: 14px;
        cursor: pointer;
        font-size: 18px;
        transition: all 0.2s ease;
      }

      .send-button:hover:not(:disabled) {
        background: #0071e3;
      }

      .send-button:active:not(:disabled) {
        background: #0056b3;
        transform: scale(0.95);
      }

      .send-button:disabled {
        opacity: 0.5;
        cursor: not-allowed;
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

      @keyframes spin {
        from {
          transform: rotate(0deg);
        }
        to {
          transform: rotate(360deg);
        }
      }

      .visually-hidden {
        position: absolute;
        width: 1px;
        height: 1px;
        padding: 0;
        margin: -1px;
        overflow: hidden;
        clip: rect(0, 0, 0, 0);
        white-space: nowrap;
        border: 0;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RagPageComponent implements OnInit {
  protected readonly ragService = inject(RagService);
  protected readonly i18n = inject(I18nService);
  protected readonly markdown = inject(MarkdownService);

  readonly fileInput = viewChild<ElementRef>('fileInput');
  readonly chatContainer = viewChild<ElementRef>('chatContainer');

  ngOnInit() {
    this.ragService.fetchAvailableDocs();
  }

  // ==================== Documents ====================

  deleteDocument(docId: string, event: Event) {
    event.stopPropagation();
    this.ragService.deleteDocument(docId);
  }

  onDocKeyDown(event: KeyboardEvent, docId: string) {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.ragService.toggleDocSelection(docId);
    }
  }

  // ==================== File Upload ====================

  onFileSelect(event: Event) {
    const input = event.target as HTMLInputElement;
    const files = input.files;
    if (files) {
      this.ragService.onFileSelect(Array.from(files));
    }
    input.value = '';
  }

  getUploadStatus(filename: string): UploadedDocument | undefined {
    return this.ragService.uploadStatuses().get(filename) as UploadedDocument | undefined;
  }

  uploadFiles() {
    this.ragService.uploadFiles(() => {
      // Upload complete callback
    });
  }

  // ==================== Chat ====================

  setInput(text: string) {
    this.ragService.setInput(text);
  }

  onInputKeyDown(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.ragService.sendMessage();
    }
  }

  // ==================== Utilities ====================

  formatTime(timestamp: number): string {
    return new Date(timestamp).toLocaleTimeString();
  }

  renderMarkdown(content: string): string {
    return this.markdown.renderToString(content);
  }
}
