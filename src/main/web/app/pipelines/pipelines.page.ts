import { httpResource } from '@angular/common/http';
import { Instant } from '@js-joda/core';
import {
  Component,
  type ElementRef,
  type OnDestroy,
  computed,
  effect,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { API_BASE_URL } from '../http/api.constants';
import {
  ChatMessagePaneComponent,
  type ChatMessageView,
  type ToolStep,
} from '../chat-shell';
import { I18nService } from '../i18n';
import { NzIconModule, provideNzIconsPatch } from 'ng-zorro-antd/icon';
import { ArrowUpOutline } from '@ant-design/icons-angular/icons';
import { ZardAlertComponent } from '../ui/alert';
import { PipelinesService, type AgentInfoResponse, type PipelineHandoffEvent } from './pipelines.service';
import { PipelinesCanvasComponent } from './pipelines-canvas.component';
import {
  toPipelineInvokeRequest,
  validatePipeline,
  type PipelineGraph,
} from './pipeline-graph';
import {
  parseDsmlToolInvocations,
  stripToolCallMarkup,
  toMinimalToolSteps,
} from '../chat-shell/tool-call-markup.util';
import {
  appendPipelineStage,
  finalizePipelineStages,
  mergeToolSteps,
} from './pipeline-stage.util';
import { hasText, textOr } from '../shared/presence';

const DEFAULT_RESULTS_RATIO = 0.38;
const MIN_PANE_PX = 240;

@Component({
  selector: 'app-pipelines-page',
  imports: [
    NzIconModule,
    ChatMessagePaneComponent,
    ZardAlertComponent,
    PipelinesCanvasComponent,
  ],
  templateUrl: './pipelines.page.html',
  providers: [provideNzIconsPatch([ArrowUpOutline])],
  host: { class: 'flex flex-1 min-h-0 w-full flex-col overflow-hidden bg-surface' },
})
export class PipelinesPageComponent implements OnDestroy {
  readonly #agentsApi = inject(PipelinesService);
  readonly i18n = inject(I18nService);

  protected readonly splitHost = viewChild<ElementRef<HTMLElement>>('splitHost');

  readonly isDraggingSplitter = signal(false);

  readonly agentsResource = httpResource<AgentInfoResponse[]>(() => ({
    url: `${API_BASE_URL}/pipelines/agent-types`,
    params: { lang: this.i18n.language() },
  }));

  readonly agents = computed(() => {
    if (this.agentsResource.hasValue()) {
      return this.agentsResource.value();
    }
    return [];
  });

  readonly pipelineHint = signal<string | null>(null);
  readonly isResultsCollapsed = signal(false);
  readonly resultsRatio = signal(DEFAULT_RESULTS_RATIO);
  readonly messages = signal<ChatMessageView[]>([]);
  readonly streamingMessageId = signal<string | null>(null);
  readonly error = signal<string | null>(null);

  readonly isLoading = signal(false);
  #activeBriefPrompt: string | null = null;
  #streamAbort: (() => void) | null = null;
  #messageSeq = 0;
  #savedRatio = DEFAULT_RESULTS_RATIO;

  constructor() {
    effect(() => {
      if (this.agentsResource.error() !== undefined && !this.agentsResource.hasValue()) {
        this.error.set(this.i18n.t().pipelines.errors.generic);
      }
    });
  }

  ngOnDestroy(): void {
    this.#streamAbort?.();
  }

  onDocumentPointerMove(event: PointerEvent): void {
    if (!this.isDraggingSplitter()) {
      return;
    }
    const host = this.splitHost()?.nativeElement;
    if (host === undefined) {
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
      this.#savedRatio = this.resultsRatio();
    }
  }

  runPipeline(event: { graph: PipelineGraph; task: string }): void {
    this.#executePipeline(event.graph, event.task);
  }

  onGraphChange(graph: PipelineGraph): void {
    if (graph.nodes.length === 0) {
      this.#activeBriefPrompt = null;
    }
    this.pipelineHint.set(null);
  }

  onTemplateHint(hint: string | null): void {
    this.pipelineHint.set(hint);
  }

  onTemplateApplied(event: { topic: string; brief: string }): void {
    this.#activeBriefPrompt = event.brief;
  }

  onSplitterPointerDown(event: PointerEvent): void {
    if (this.isResultsCollapsed()) {
      return;
    }
    event.preventDefault();
    this.isDraggingSplitter.set(true);
    (event.target as HTMLElement).setPointerCapture(event.pointerId);
  }

  toggleResultsCollapsed(): void {
    if (this.isResultsCollapsed()) {
      this.isResultsCollapsed.set(false);
      this.resultsRatio.set(this.#savedRatio);
      return;
    }
    this.#savedRatio = this.resultsRatio();
    this.isResultsCollapsed.set(true);
  }

  #executePipeline(graph: PipelineGraph, task: string): void {
    if (this.isLoading()) {
      return;
    }

    const result = validatePipeline(graph);
    if (!result.ok) {
      this.pipelineHint.set(this.#buildPipelineReasonMessage(result.reason));
      return;
    }

    const topic =
      textOr(task.trim(), this.i18n.t().pipelines.defaultMessage);
    const brief = this.#activeBriefPrompt?.trim();
    const invokeMessage = hasText(brief) ? `${topic}\n\n${brief}` : topic;

    this.#streamAbort?.();
    this.error.set(null);
    this.pipelineHint.set(null);
    this.isLoading.set(true);
    if (this.isResultsCollapsed()) {
      this.isResultsCollapsed.set(false);
      this.resultsRatio.set(this.#savedRatio);
    }

    const now = Instant.now();
    const userId = this.#generateNextId('user');
    const assistantId = this.#generateNextId('assistant');

    this.messages.update(messages => [
      ...messages,
      { id: userId, role: 'user', content: topic, timestamp: now },
      {
        id: assistantId,
        role: 'assistant',
        content: '',
        timestamp: now,
        streaming: true,
      },
    ]);
    this.streamingMessageId.set(assistantId);

    let rawContent = '';
    let pipelineStages: ToolStep[] = [];

    const buildVisibleSteps = (dsmlStatus: 'running' | 'success' | 'error') => mergeToolSteps(
      pipelineStages,
      toMinimalToolSteps(parseDsmlToolInvocations(rawContent), dsmlStatus),
    );

    const finish = (content: string, error?: Error) => {
      pipelineStages = finalizePipelineStages(
        pipelineStages,
        error !== undefined ? 'error' : 'success',
      );
      const cleaned = stripToolCallMarkup(content);
      this.#patchAssistant(
        assistantId,
        cleaned,
        false,
        buildVisibleSteps(error !== undefined ? 'error' : 'success'),
      );
      this.streamingMessageId.set(null);
      this.isLoading.set(false);
      this.#streamAbort = null;
      if (error !== undefined) {
        this.error.set(textOr(error.message, this.i18n.t().pipelines.errors.generic));
      }
    };

    const onChunk = (chunk: string) => {
      rawContent += chunk;
      const cleaned = stripToolCallMarkup(rawContent);
      this.#patchAssistant(assistantId, cleaned, true, buildVisibleSteps('running'));
    };
    const onHandoff = ({ agentType }: PipelineHandoffEvent) => {
      pipelineStages = appendPipelineStage(pipelineStages, agentType);
      rawContent += `\n_Delegated to **${agentType}**_\n\n`;
      const cleaned = stripToolCallMarkup(rawContent);
      this.#patchAssistant(assistantId, cleaned, true, buildVisibleSteps('running'));
    };

    const request = toPipelineInvokeRequest(invokeMessage, graph);
    const { abort } = this.#agentsApi.invokePipelineStream(
      request,
      onChunk,
      onHandoff,
      () => finish(textOr(rawContent, this.i18n.t().common.thinking)),
      error => finish(textOr(rawContent, this.i18n.t().pipelines.errors.generic), error),
    );
    this.#streamAbort = abort;
  }

  #buildPipelineReasonMessage(reason: string): string {
    const hints = this.i18n.t().pipelines.hints;
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

  #patchAssistant(
    id: string,
    content: string,
    streaming: boolean,
    toolSteps: ToolStep[] = [],
  ): void {
    this.messages.update((messages) => {
      return messages.map((message) => {
        if (message.id !== id) {
          return message;
        }
        return {
          ...message,
          content,
          streaming,
          toolSteps: toolSteps.length > 0 ? toolSteps : undefined,
        };
      });
    });
  }

  #generateNextId(prefix: string): string {
    this.#messageSeq += 1;
    return `${prefix}-${String(this.#messageSeq)}-${String(Instant.now().toEpochMilli())}`;
  }
}
