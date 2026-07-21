import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  inject,
  model,
  signal,
  viewChild,
} from '@angular/core';
import {
  ChatBubbleMessage,
  ChatMessagePaneComponent,
  ChatSenderBarComponent,
} from '../shared/components/chat-shell';
import { I18nService } from '../core/i18n';
import { NzIconModule, provideNzIconsPatch } from 'ng-zorro-antd/icon';
import { ArrowUpOutline } from '@ant-design/icons-angular/icons';
import { ZardAlertComponent } from '../shared/components/alert';
import { AgentsService } from './agents.service';
import { AgentsPipelineCanvasComponent } from './agents-pipeline.canvas';
import type { AgentInfo } from './agents.model';
import {
  toPipelineInvokeRequest,
  validatePipeline,
  type PipelineGraph,
} from './agents-pipeline.model';
import {
  parseDsmlToolInvocations,
  stripToolCallMarkup,
  toMinimalToolSteps,
} from './tool-call-markup.filter';
import type { ChatBubbleToolStep } from '../shared/components/chat-shell';

const DEFAULT_RESULTS_RATIO = 0.38;
const MIN_PANE_PX = 240;

@Component({
  selector: 'app-agents-page',
  imports: [
    NzIconModule,
    ChatMessagePaneComponent,
    ChatSenderBarComponent,
    ZardAlertComponent,
    AgentsPipelineCanvasComponent,
  ],
  templateUrl: './agents.page.html',
  providers: [provideNzIconsPatch([ArrowUpOutline])],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-1 min-h-0 w-full flex-col overflow-hidden bg-surface' },
})
export class AgentsPageComponent implements OnInit, OnDestroy {
  private readonly agentsApi = inject(AgentsService);
  readonly i18n = inject(I18nService);

  private readonly splitHost = viewChild<ElementRef<HTMLElement>>('splitHost');

  readonly agents = signal<AgentInfo[]>([]);
  readonly messages = signal<ChatBubbleMessage[]>([]);
  readonly streamingMessageId = signal<string | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly pipelineHint = signal<string | null>(null);
  readonly input = model('');
  readonly resultsCollapsed = signal(false);
  readonly resultsRatio = signal(DEFAULT_RESULTS_RATIO);
  readonly isDraggingSplitter = signal(false);

  private currentGraph: PipelineGraph = { nodes: [], connections: [] };
  private activeBriefPrompt: string | null = null;
  private streamAbort: (() => void) | null = null;
  private messageSeq = 0;
  private savedRatio = DEFAULT_RESULTS_RATIO;

  ngOnInit(): void {
    this.agentsApi.listAgents().subscribe({
      next: agents => this.agents.set(agents),
      error: () => this.error.set(this.i18n.t().agents.errorMessage),
    });
  }

  ngOnDestroy(): void {
    this.streamAbort?.();
  }

  onGraphChange(graph: PipelineGraph): void {
    this.currentGraph = graph;
    if (graph.nodes.length === 0) {
      this.activeBriefPrompt = null;
    }
    this.pipelineHint.set(null);
  }

  onTemplateHint(hint: string | null): void {
    this.pipelineHint.set(hint);
  }

  onTemplateApplied(event: { topic: string; brief: string }): void {
    this.activeBriefPrompt = event.brief;
    this.input.set(event.topic);
  }

  runPipeline(graph: PipelineGraph): void {
    this.currentGraph = graph;
    this.executePipeline(graph);
  }

  send(): void {
    this.executePipeline(this.currentGraph);
  }

  toggleResultsCollapsed(): void {
    if (this.resultsCollapsed()) {
      this.resultsCollapsed.set(false);
      this.resultsRatio.set(this.savedRatio);
      return;
    }
    this.savedRatio = this.resultsRatio();
    this.resultsCollapsed.set(true);
  }

  onSplitterPointerDown(event: PointerEvent): void {
    if (this.resultsCollapsed()) {
      return;
    }
    event.preventDefault();
    this.isDraggingSplitter.set(true);
    (event.target as HTMLElement).setPointerCapture?.(event.pointerId);
  }

  onDocumentPointerMove(event: PointerEvent): void {
    if (!this.isDraggingSplitter()) {
      return;
    }
    const host = this.splitHost()?.nativeElement;
    if (!host) {
      return;
    }
    const rect = host.getBoundingClientRect();
    if (rect.width <= 0) {
      return;
    }
    const fromRight = rect.right - event.clientX;
    const minRight = MIN_PANE_PX;
    const maxRight = rect.width - MIN_PANE_PX;
    const clamped = Math.min(maxRight, Math.max(minRight, fromRight));
    this.resultsRatio.set(clamped / rect.width);
  }

  onDocumentPointerUp(): void {
    if (this.isDraggingSplitter()) {
      this.isDraggingSplitter.set(false);
      this.savedRatio = this.resultsRatio();
    }
  }

  private executePipeline(graph: PipelineGraph): void {
    if (this.loading()) {
      return;
    }

    const result = validatePipeline(graph);
    if (!result.ok) {
      this.pipelineHint.set(this.pipelineReasonMessage(result.reason));
      return;
    }

    const topic =
      this.input().trim() || this.i18n.t().agents.pipeline.defaultMessage;
    const brief = this.activeBriefPrompt?.trim();
    const invokeMessage = brief ? `${topic}\n\n${brief}` : topic;

    this.streamAbort?.();
    this.error.set(null);
    this.pipelineHint.set(null);
    this.input.set(topic);
    this.loading.set(true);
    if (this.resultsCollapsed()) {
      this.resultsCollapsed.set(false);
      this.resultsRatio.set(this.savedRatio);
    }

    const userId = this.nextId('user');
    const assistantId = this.nextId('assistant');

    this.messages.update(msgs => [
      ...msgs,
      { id: userId, role: 'user', content: topic, timestamp: Date.now() },
      {
        id: assistantId,
        role: 'assistant',
        content: '',
        timestamp: Date.now(),
        streaming: true,
      },
    ]);
    this.streamingMessageId.set(assistantId);

    let rawContent = '';
    const finish = (content: string, err?: Error) => {
      const cleaned = stripToolCallMarkup(content);
      const steps = toMinimalToolSteps(
        parseDsmlToolInvocations(content),
        err ? 'error' : 'success',
      );
      this.patchAssistant(assistantId, cleaned, false, steps);
      this.streamingMessageId.set(null);
      this.loading.set(false);
      this.streamAbort = null;
      if (err) {
        this.error.set(err.message || this.i18n.t().agents.errorMessage);
      }
    };

    const onChunk = (chunk: string) => {
      rawContent += chunk;
      const cleaned = stripToolCallMarkup(rawContent);
      const steps = toMinimalToolSteps(
        parseDsmlToolInvocations(rawContent),
        'running',
      );
      this.patchAssistant(assistantId, cleaned, true, steps);
    };
    const onHandoff = (handoff: string) => {
      try {
        const parsed = JSON.parse(handoff) as { agentType?: string; reason?: string };
        if (parsed.agentType) {
          const note = `\n_Delegated to **${parsed.agentType}**_\n\n`;
          rawContent += note;
          const cleaned = stripToolCallMarkup(rawContent);
          const steps = toMinimalToolSteps(
            parseDsmlToolInvocations(rawContent),
            'running',
          );
          this.patchAssistant(assistantId, cleaned, true, steps);
        }
      } catch {
        // ignore malformed handoff payloads
      }
    };

    const request = toPipelineInvokeRequest(invokeMessage, graph);
    const { abort } = this.agentsApi.invokePipelineStream(
      request,
      onChunk,
      onHandoff,
      () => finish(rawContent || this.i18n.t().agents.thinking),
      err => finish(rawContent || this.i18n.t().agents.errorMessage, err),
    );
    this.streamAbort = abort;
  }

  private pipelineReasonMessage(reason: string): string {
    const hints = this.i18n.t().agents.pipeline.hints;
    switch (reason) {
      case 'empty':
        return hints.empty;
      case 'needConnections':
        return hints.needConnections;
      case 'orphan':
        return hints.orphan;
      case 'cycle':
        return hints.cycle;
      default:
        return hints.invalid;
    }
  }

  private patchAssistant(
    id: string,
    content: string,
    streaming: boolean,
    toolSteps: ChatBubbleToolStep[] = [],
  ): void {
    this.messages.update((msgs) => {
      return msgs.map((msg) => {
        if (msg.id !== id) {
          return msg;
        }
        return {
          ...msg,
          content,
          streaming,
          toolSteps: toolSteps.length > 0 ? toolSteps : undefined,
        };
      });
    });
  }

  private nextId(prefix: string): string {
    this.messageSeq += 1;
    return `${prefix}-${this.messageSeq}-${Date.now()}`;
  }
}
