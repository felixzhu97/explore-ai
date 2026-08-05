import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  computed,
  effect,
  inject,
  input,
  OnInit,
  output,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  FCreateConnectionEvent,
  FFlowModule,
  FMoveNodesEvent,
} from '@foblex/flow';
import { I18nService } from '../core/i18n';
import { NotificationService } from '../core/notification.service';
import type {
  AgentInfo,
  SavedWorkflowTemplate,
  WorkflowTemplate,
  WorkflowTemplateWriteRequest,
} from './pipelines.model';
import {
  connectorInId,
  connectorOutId,
  nodeIdFromConnector,
  type PipelineConnection,
  type PipelineGraph,
  type PipelineNode,
} from './pipelines.model.graph';
import { applyPipelineTemplate } from './pipelines.templates';
import { PipelinesService } from './pipelines.service';

type WorkspaceMode = 'gallery' | 'edit' | 'use';

interface ApplyableTemplate {
  id: string;
  name: string;
  description: string;
  agentTypes: string[];
  shortTopic: string;
  briefPrompt: string;
  /** When set, edits persist to the client library. */
  libraryId?: string;
}

@Component({
  selector: 'app-pipelines-canvas',
  imports: [FFlowModule, FormsModule],
  templateUrl: './pipelines-canvas.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex min-h-0 flex-1 overflow-hidden' },
})
export class PipelinesCanvasComponent implements OnInit {
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly pipelinesApi = inject(PipelinesService);
  private readonly notifications = inject(NotificationService);
  readonly i18n = inject(I18nService);

  readonly agents = input.required<AgentInfo[]>();
  readonly validationHint = input<string | null>(null);
  readonly runRequested = output<{ graph: PipelineGraph; task: string }>();
  readonly graphChange = output<PipelineGraph>();
  readonly clearValidation = output<void>();
  readonly templateHint = output<string | null>();
  /** Emits when a template is applied (task prefill + brief for invoke merge). */
  readonly templateApplied = output<{ topic: string; brief: string }>();

  readonly nodes = signal<PipelineNode[]>([]);
  readonly connections = signal<PipelineConnection[]>([]);
  readonly builtinTemplates = signal<WorkflowTemplate[]>([]);
  readonly library = signal<SavedWorkflowTemplate[]>([]);
  readonly workspaceMode = signal<WorkspaceMode>('gallery');
  readonly activeTemplateName = signal('');
  readonly task = signal('');
  /** Null = builtin session copy or unsaved draft; set = library row. */
  readonly editingLibraryId = signal<string | null>(null);
  readonly isDraft = signal(false);
  readonly saving = signal(false);

  /** Node editor occupies the center work area (graph copy only). */
  readonly editingNodeId = signal<string | null>(null);
  readonly editName = signal('');
  readonly editDescription = signal('');
  readonly editSystemPrompt = signal('');
  readonly editToolKeys = signal<string[]>([]);

  /** One-shot agent picker for the current workflow — not a persistent Agents catalog. */
  readonly showAgentPicker = signal(false);

  readonly availableToolKeys = ['web', 'weather', 'datetime', 'document'] as const;

  private nodeSeq = 0;
  private connectionSeq = 0;
  private activeBrief = '';

  private static readonly DEFAULT_BRIEF =
    'Follow the configured agent pipeline for the user task.';

  readonly workers = computed(() => this.agents().filter(agent => !agent.supervisor));

  readonly isEditingNode = computed(() => this.editingNodeId() != null);
  readonly isEditMode = computed(() => this.workspaceMode() === 'edit');
  readonly isUseMode = computed(() => this.workspaceMode() === 'use');
  readonly isGallery = computed(() => this.workspaceMode() === 'gallery');

  readonly graph = computed<PipelineGraph>(() => ({
    nodes: this.nodes(),
    connections: this.connections(),
  }));

  constructor() {
    effect(() => {
      this.i18n.language();
      this.reloadBuiltinTemplates();
    });
  }

  ngOnInit(): void {
    this.reloadBuiltinTemplates();
    this.reloadLibrary();
  }

  openAgentPicker(): void {
    if (!this.isEditMode()) {
      return;
    }
    this.cancelNodeEdit();
    this.showAgentPicker.set(true);
  }

  closeAgentPicker(): void {
    this.showAgentPicker.set(false);
  }

  pickAgent(agent: AgentInfo): void {
    if (!this.isEditMode() || agent.supervisor) {
      return;
    }
    this.addNode(agent, {
      x: 100 + this.nodes().length * 220,
      y: 120,
    });
    this.showAgentPicker.set(false);
    this.clearValidation.emit();
    this.cdr.markForCheck();
  }

  onCreateConnection(event: FCreateConnectionEvent): void {
    if (!this.isEditMode() || !event.targetId) {
      return;
    }
    const sourceNodeId = nodeIdFromConnector(event.sourceId);
    const targetNodeId = nodeIdFromConnector(event.targetId);
    if (sourceNodeId === targetNodeId) {
      return;
    }
    const exists = this.connections().some(
      edge => edge.sourceNodeId === sourceNodeId && edge.targetNodeId === targetNodeId,
    );
    if (exists) {
      return;
    }
    this.connectionSeq += 1;
    this.connections.update(list => [
      ...list,
      {
        id: `edge-${this.connectionSeq}`,
        sourceNodeId,
        targetNodeId,
      },
    ]);
    this.emitGraph();
    this.clearValidation.emit();
    this.cdr.markForCheck();
  }

  onMoveNodes(event: FMoveNodesEvent): void {
    if (!this.isEditMode()) {
      return;
    }
    const moved = new Map(event.nodes.map(node => [node.id, node.position]));
    this.nodes.update(list => list.map((node) => {
      const position = moved.get(node.id);
      return position ? { ...node, position: { x: position.x, y: position.y } } : node;
    }),
    );
    this.emitGraph();
    this.cdr.markForCheck();
  }

  removeNode(nodeId: string): void {
    if (!this.isEditMode()) {
      return;
    }
    if (this.editingNodeId() === nodeId) {
      this.cancelNodeEdit();
    }
    this.nodes.update(list => list.filter(node => node.id !== nodeId));
    this.connections.update(list => list.filter(
      edge => edge.sourceNodeId !== nodeId && edge.targetNodeId !== nodeId,
    ));
    this.emitGraph();
    this.clearValidation.emit();
    this.cdr.markForCheck();
  }

  openNodeEditor(node: PipelineNode): void {
    if (!this.isEditMode()) {
      return;
    }
    this.showAgentPicker.set(false);
    this.editingNodeId.set(node.id);
    this.editName.set(node.name);
    this.editDescription.set(node.description);
    this.editSystemPrompt.set(node.systemPrompt);
    this.editToolKeys.set([...(node.toolKeys ?? [])]);
  }

  isEditToolSelected(toolKey: string): boolean {
    return this.editToolKeys().includes(toolKey);
  }

  toggleEditTool(toolKey: string): void {
    const current = this.editToolKeys();
    if (current.includes(toolKey)) {
      this.editToolKeys.set(current.filter(key => key !== toolKey));
      return;
    }
    this.editToolKeys.set([...current, toolKey]);
  }

  cancelNodeEdit(): void {
    this.editingNodeId.set(null);
  }

  saveNodeEdit(): void {
    const nodeId = this.editingNodeId();
    if (!nodeId) {
      return;
    }
    const snapshot = {
      name: this.editName().trim() || 'Agent',
      description: this.editDescription().trim(),
      systemPrompt: this.editSystemPrompt().trim(),
      toolKeys: [...this.editToolKeys()],
    };
    this.nodes.update(list => list.map((node) => {
      if (node.id !== nodeId) {
        return node;
      }
      return { ...node, ...snapshot };
    }));
    this.editingNodeId.set(null);
    this.emitGraph();
    this.cdr.markForCheck();
  }

  backToGallery(): void {
    this.nodes.set([]);
    this.connections.set([]);
    this.editingNodeId.set(null);
    this.showAgentPicker.set(false);
    this.workspaceMode.set('gallery');
    this.activeTemplateName.set('');
    this.editingLibraryId.set(null);
    this.isDraft.set(false);
    this.activeBrief = '';
    this.emitGraph();
    this.clearValidation.emit();
    this.templateHint.emit(null);
    this.reloadLibrary();
    this.cdr.markForCheck();
  }

  addWorkflow(): void {
    this.cancelNodeEdit();
    this.showAgentPicker.set(false);
    this.nodes.set([]);
    this.connections.set([]);
    this.editingLibraryId.set(null);
    this.isDraft.set(true);
    this.activeTemplateName.set(
      this.i18n.t().pipelines.pipeline.templates.newWorkflowName,
    );
    this.activeBrief = PipelinesCanvasComponent.DEFAULT_BRIEF;
    this.task.set('');
    this.workspaceMode.set('edit');
    this.emitGraph();
    this.clearValidation.emit();
    this.templateHint.emit(null);
    this.cdr.markForCheck();
  }

  editTemplate(template: WorkflowTemplate): void {
    this.applyTemplate({
      id: template.id,
      name: template.name,
      description: template.description,
      agentTypes: template.agentTypes,
      shortTopic: template.shortTopic,
      briefPrompt: template.briefPrompt,
    }, 'edit');
  }

  useTemplate(template: WorkflowTemplate): void {
    this.applyTemplate({
      id: template.id,
      name: template.name,
      description: template.description,
      agentTypes: template.agentTypes,
      shortTopic: template.shortTopic,
      briefPrompt: template.briefPrompt,
    }, 'use');
  }

  editLibrary(template: SavedWorkflowTemplate): void {
    this.applyTemplate({
      id: template.id,
      name: template.name,
      description: template.description,
      agentTypes: template.agentTypes,
      shortTopic: template.shortTopic,
      briefPrompt: template.briefPrompt,
      libraryId: template.id,
    }, 'edit');
  }

  useLibrary(template: SavedWorkflowTemplate): void {
    this.applyTemplate({
      id: template.id,
      name: template.name,
      description: template.description,
      agentTypes: template.agentTypes,
      shortTopic: template.shortTopic,
      briefPrompt: template.briefPrompt,
      libraryId: template.id,
    }, 'use');
  }

  deleteLibrary(template: SavedWorkflowTemplate): void {
    const message = this.i18n.t().pipelines.pipeline.templates.deleteConfirm.replace(
      '{name}',
      template.name,
    );
    if (!globalThis.confirm(message)) {
      return;
    }
    this.pipelinesApi.deleteLibraryTemplate(template.id).subscribe({
      next: () => this.reloadLibrary(),
      error: () => {
        this.notifications.showError(
          this.i18n.t().pipelines.pipeline.templates.deleteFailed,
        );
      },
    });
  }

  switchToUse(): void {
    if (this.nodes().length === 0) {
      this.notifications.showWarning(
        this.i18n.t().pipelines.pipeline.templates.canvasEmpty,
      );
      return;
    }
    this.cancelNodeEdit();
    this.showAgentPicker.set(false);
    this.persistLibraryIfNeeded(() => {
      this.workspaceMode.set('use');
      this.templateApplied.emit({
        topic: this.task().trim() || this.activeTemplateName(),
        brief: this.activeBrief,
      });
      this.cdr.markForCheck();
    });
  }

  switchToEdit(): void {
    if (this.nodes().length === 0) {
      return;
    }
    this.workspaceMode.set('edit');
    this.cdr.markForCheck();
  }

  templateOrder(agentTypes: readonly string[]): string {
    return agentTypes.join(' → ');
  }

  run(): void {
    if (!this.isUseMode()) {
      return;
    }
    this.runRequested.emit({ graph: this.graph(), task: this.task().trim() });
  }

  outId(nodeId: string): string {
    return connectorOutId(nodeId);
  }

  inId(nodeId: string): string {
    return connectorInId(nodeId);
  }

  private applyTemplate(template: ApplyableTemplate, mode: 'edit' | 'use'): void {
    this.cancelNodeEdit();
    this.showAgentPicker.set(false);
    const seed = this.nodeSeq + 1;
    const result = applyPipelineTemplate(
      { id: template.id, agentTypes: template.agentTypes },
      this.workers(),
      seed,
    );
    this.nodeSeq = seed + result.graph.nodes.length;
    this.connectionSeq = seed + result.graph.connections.length;
    this.nodes.set(result.graph.nodes);
    this.connections.set(result.graph.connections);
    this.workspaceMode.set(mode);
    this.activeTemplateName.set(template.name);
    this.activeBrief = template.briefPrompt || PipelinesCanvasComponent.DEFAULT_BRIEF;
    this.editingLibraryId.set(template.libraryId ?? null);
    this.isDraft.set(false);
    this.emitGraph();
    this.clearValidation.emit();
    const defaultTask = (template.shortTopic ?? '').trim();
    if (defaultTask) {
      this.task.set(defaultTask);
    }
    this.templateApplied.emit({
      topic: defaultTask || template.name,
      brief: this.activeBrief,
    });
    if (result.skippedAgentTypes.length > 0) {
      const hint = this.i18n.t().pipelines.pipeline.templates.skipped.replace(
        '{types}',
        result.skippedAgentTypes.join(', '),
      );
      this.templateHint.emit(hint);
    } else {
      this.templateHint.emit(null);
    }
    this.cdr.markForCheck();
  }

  private persistLibraryIfNeeded(done: () => void): void {
    const shouldPersist = this.isDraft() || this.editingLibraryId() != null;
    if (!shouldPersist) {
      done();
      return;
    }
    const request = this.buildLibraryWriteRequest();
    if (!request) {
      this.notifications.showWarning(
        this.i18n.t().pipelines.pipeline.templates.canvasEmpty,
      );
      return;
    }
    this.saving.set(true);
    const libraryId = this.editingLibraryId();
    const request$ = libraryId
      ? this.pipelinesApi.updateLibraryTemplate(libraryId, request)
      : this.pipelinesApi.createLibraryTemplate(request);
    request$.subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.editingLibraryId.set(saved.id);
        this.isDraft.set(false);
        this.activeTemplateName.set(saved.name);
        this.reloadLibrary();
        done();
      },
      error: () => {
        this.saving.set(false);
        this.notifications.showError(
          this.i18n.t().pipelines.pipeline.templates.saveFailed,
        );
      },
    });
  }

  private buildLibraryWriteRequest(): WorkflowTemplateWriteRequest | null {
    const agentTypes = this.nodes().map(node => node.agentType);
    if (agentTypes.length === 0) {
      return null;
    }
    const name = this.activeTemplateName().trim()
      || this.i18n.t().pipelines.pipeline.templates.newWorkflowName;
    return {
      name,
      description: '',
      agentTypes,
      shortTopic: this.task().trim(),
      briefPrompt: this.activeBrief.trim() || PipelinesCanvasComponent.DEFAULT_BRIEF,
    };
  }

  private reloadBuiltinTemplates(): void {
    this.pipelinesApi.listTemplates().subscribe({
      next: (templates) => {
        this.builtinTemplates.set(templates);
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
  }

  private reloadLibrary(): void {
    this.pipelinesApi.listLibrary().subscribe({
      next: (library) => {
        this.library.set(library);
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
  }

  private addNode(agent: AgentInfo, position: { x: number; y: number }): void {
    const chainTailId = this.findChainTailId();
    this.nodeSeq += 1;
    const nodeId = `node-${this.nodeSeq}`;
    this.nodes.update(list => [
      ...list,
      {
        id: nodeId,
        agentType: agent.type,
        name: agent.name,
        description: agent.description,
        systemPrompt: agent.systemPrompt ?? '',
        toolKeys: [...(agent.toolKeys ?? [])],
        position,
      },
    ]);
    if (chainTailId) {
      this.connectionSeq += 1;
      this.connections.update(list => [
        ...list,
        {
          id: `edge-${this.connectionSeq}`,
          sourceNodeId: chainTailId,
          targetNodeId: nodeId,
        },
      ]);
    }
    this.emitGraph();
  }

  private findChainTailId(): string | null {
    const nodes = this.nodes();
    if (nodes.length === 0) {
      return null;
    }
    const sources = new Set(this.connections().map(edge => edge.sourceNodeId));
    const tails = nodes.filter(node => !sources.has(node.id));
    if (tails.length > 0) {
      return tails[tails.length - 1].id;
    }
    return nodes[nodes.length - 1].id;
  }

  private emitGraph(): void {
    this.graphChange.emit(this.graph());
  }
}
