import {
  Component,
  inject,
  OnInit,
  OnDestroy,
  ElementRef,
  viewChild,
  ChangeDetectionStrategy,
  model,
  effect,
  computed,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MarkdownContentComponent } from '@shared/components/markdown-content.component';
import { I18nService } from '@core/i18n';
import { NxSenderComponent } from 'ng-zorro-x/sender';
import type { NxSpeechConfig } from 'ng-zorro-x/sender';
import { NzIconModule, provideNzIconsPatch } from 'ng-zorro-antd/icon';
import { ArrowUpOutline } from '@ant-design/icons-angular/icons';
import { VoiceInteractionService } from '@core/services/voice-interaction.service';
import { VoicePlaybackService } from '@core/services/voice-playback.service';
import { ChatService } from '../chat.service';

@Component({
  selector: 'app-chat-tab',
  imports: [FormsModule, NxSenderComponent, NzIconModule, MarkdownContentComponent],
  standalone: true,
  templateUrl: './chat.component.html',
  styles: [
    `
      @keyframes fade-in {
        from {
          opacity: 0;
          transform: translateY(8px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }
    `,
  ],
  providers: [provideNzIconsPatch([ArrowUpOutline])],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-1 min-h-0 w-full flex-col overflow-hidden' },
})
export class ChatTabComponent implements OnInit, OnDestroy {
  protected readonly chat = inject(ChatService);
  protected readonly i18n = inject(I18nService);
  protected readonly voice = inject(VoiceInteractionService);
  protected readonly voicePlayback = inject(VoicePlaybackService);

  readonly input = model('');
  readonly messagesEnd = viewChild<ElementRef>('messagesEnd');

  readonly speechConfig = computed<NxSpeechConfig>(() => ({
    recording: this.voice.isRecording(),
    onRecordingChange: recording => void this.voice.onRecordingChange(recording),
  }));

  constructor() {
    effect(() => {
      this.chat.messages();
      this.scrollToBottom();
    });
  }

  ngOnInit() {
    this.chat.loadProviders();
    this.voice.setOnFinal((text) => {
      if (!this.chat.isLoading()) {
        this.chat.sendMessage(text);
      }
    });
    this.chat.setOnStreamComplete(content => this.voicePlayback.speak(content));
  }

  ngOnDestroy() {
    this.chat.abortStream();
    this.voicePlayback.stop();
  }

  newChat(): void {
    this.chat.createSession();
  }

  onProviderChange(provider: string) {
    this.chat.setProvider(provider);
  }

  setSelectedModel(model: string) {
    this.chat.setModel(model);
  }

  send() {
    const text = this.input().trim();
    if (!text || this.chat.isLoading()) {
      return;
    }
    this.input.set('');
    this.chat.sendMessage(text);
  }

  toggleVoiceMute(): void {
    this.voicePlayback.toggleMuted();
  }

  formatTime(timestamp: number): string {
    return new Date(timestamp).toLocaleTimeString();
  }

  private scrollToBottom() {
    const el = this.messagesEnd();
    if (el) {
      el.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'end' });
    }
  }
}
