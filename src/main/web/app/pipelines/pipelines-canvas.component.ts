import {
  ChangeDetectorRef,
  Component,
  computed,
  effect,
  inject,
  input,
  type OnInit,
  output,
  signal,
} from '@angular/core';
import { form, FormField } from '@angular/forms/signals';
import {
  type FCreateConnectionEvent,
  FFlowModule,
  type FMoveNodesEvent,
} from '@foblex/flow';
import { I18nService } from '../i18n';
import { NotificationService } from '../ui/notification.service';
import {
  PipelinesService,
  type AgentInfoResponse,
  type PipelineTemplate,
  type PipelineTemplateDefinitionResponse,
  type CreatePipelineTemplateRequest,
} from './pipelines.service';
import {
  connectorInId,
  connectorOutId,
  nodeIdFromConnector,
  type PipelineConnection,
  type PipelineGraph,
  type PipelineNode,
} from './pipeline-graph';
import { applyPipelineTemplate } from './pipelines.templates';
import { ZardButtonComponent } from '../ui/button';
import { PipelinesGalleryComponent } from './pipelines-gallery.component';
import { PipelinesToolbarComponent } from './pipelines-toolbar.component';
import { hasText, textOr } from '../shared/presence';

type WorkspaceMode = 'gallery' | 'edit' | 'use';

interface ApplyableTemplate {
  id: string;
  name: string;
  description: string;
  agentTypes: string[];
  shortTopic: string;
  briefPrompt: string;
  /** When set, edits persist to this saved pipeline template. */
  savedTemplateId?: string;
}

const DEFAULT_BRIEF = 'Follow the configured agent pipeline for the user task.';

type NodeDraft = Pick<PipelineNode, 'name' | 'description' | 'systemPrompt' | 'toolKeys'>;

@Component({
  selector: 'app-pipelines-canvas',
  imports: [
    FFlowModule,
    FormField,
    PipelinesGalleryComponent,
    PipelinesToolbarComponent,
    ZardButtonComponent,
  ],
  templateUrl: './pipelines-canvas.component.html',
  host: { class: 'flex min-h-0 flex-1 overflow-hidden' },
})
export class PipelinesCanvasComponent implements OnInit {
  readonly #cdr = inject(ChangeDetectorRef);
  readonly #pipelinesApi = inject(PipelinesService);
  readonly #notifications = inject(NotificationService);
  readonly i18n = inject(I18nService);

  readonly agents = input.required<AgentInfoResponse[]>();
  readonly validationHint = input<string | null>(null);
  readonly runRequested = output<{ graph: PipelineGraph; task: string }>();
  readonly graphChanged = output<PipelineGraph>();
  readonly validationCleared = output<void>();
  readonly templateHintChanged = output<string | null>();

  /** Emits when a template is applied (task prefill + brief for invoke merge). */
  readonly templateApplied = output<{ topic: string; brief: string }>();

  readonly workspaceMode = signal<WorkspaceMode>('gallery');
  readonly isGallery = computed(() => this.workspaceMode() === 'gallery');
  readonly isEditMode = computed(() => this.workspaceMode() === 'edit');
  readonly activeTemplateName = signal('');
  readonly isSaving = signal(false);
  readonly task = signal('');

  /** Node editor occupies the center work area (graph copy only). */
  readonly editingNodeId = signal<string | null>(null);

  readonly isEditingNode = computed(() => this.editingNodeId() !== null);

  readonly #nodeDraft = signal<NodeDraft>({
    name: '',
    description: '',
    systemPrompt: '',
    toolKeys: [],
  });

  protected readonly nodeForm = form(this.#nodeDraft);
  readonly availableToolKeys = ['web', 'weather', 'datetime', 'document'] as const;

  /** One-shot agent picker for the current workflow — not a persistent Agents catalog. */
  readonly showAgentPicker = signal(false);

  readonly workers = computed(() => this.agents().filter(agent => !agent.supervisor));
  readonly builtinTemplates = signal<PipelineTemplateDefinitionResponse[]>([]);
  readonly savedTemplates = signal<PipelineTemplate[]>([]);
  readonly addingTemplateId = signal<string | null>(null);
  readonly connections = signal<PipelineConnection[]>([]);
  readonly nodes = signal<PipelineNode[]>([]);

  /** Null = builtin session copy or unsaved draft; set = saved template id. */
  readonly editingTemplateId = signal<string | null>(null);

  readonly isDraft = signal(false);
  #nodeSeq = 0;
  #connectionSeq = 0;
  #activeBrief = '';
  readonly isUseMode = computed(() => this.workspaceMode() === 'use');

  readonly graph = computed<PipelineGraph>(() => ({
    nodes: this.nodes(),
    connections: this.connections(),
  }));

  constructor() {
    effect(() => {
      this.i18n.language();
      this.#reloadBuiltinTemplates();
    });
  }

  ngOnInit(): void {
    this.#reloadBuiltinTemplates();
    this.#reloadSavedTemplates();
  }

  backToGallery(): void {
    this.nodes.set([]);
    this.connections.set([]);
    this.editingNodeId.set(null);
    this.showAgentPicker.set(false);
    this.workspaceMode.set('gallery');
    this.activeTemplateName.set('');
    this.editingTemplateId.set(null);
    this.isDraft.set(false);
    this.#activeBrief = '';
    this.#emitGraph();
    this.validationCleared.emit();
    this.templateHintChanged.emit(null);
    this.#reloadSavedTemplates();
    this.#cdr.markForCheck();
  }

  openAgentPicker(): void {
    if (!this.isEditMode()) {
      return;
    }
    this.cancelNodeEdit();
    this.showAgentPicker.set(true);
  }

  switchToUse(): void {
    if (this.nodes().length === 0) {
      this.#notifications.showWarning(
        this.i18n.t().pipelines.templates.canvasEmpty,
      );
      return;
    }
    this.cancelNodeEdit();
    this.showAgentPicker.set(false);
    this.#persistTemplateIfNeeded(() => {
      this.workspaceMode.set('use');
      this.templateApplied.emit({
        topic: textOr(this.task().trim(), this.activeTemplateName()),
        brief: this.#activeBrief,
      });
      this.#cdr.markForCheck();
    });
  }

  switchToEdit(): void {
    if (this.nodes().length === 0) {
      return;
    }
    this.workspaceMode.set('edit');
    this.#cdr.markForCheck();
  }

  run(): void {
    if (!this.isUseMode()) {
      return;
    }
    this.runRequested.emit({ graph: this.graph(), task: this.task().trim() });
  }

  isEditToolSelected(toolKey: string): boolean {
    return this.#nodeDraft().toolKeys.includes(toolKey);
  }

  toggleEditTool(toolKey: string): void {
    this.nodeForm.toolKeys().value.update(current => (current.includes(toolKey)
      ? current.filter(key => key !== toolKey)
      : [...current, toolKey]));
  }

  saveNodeEdit(): void {
    const nodeId = this.editingNodeId();
    if (!hasText(nodeId)) {
      return;
    }
    const draft = this.#nodeDraft();
    const snapshot: NodeDraft = {
      name: textOr(draft.name.trim(), 'Agent'),
      description: draft.description.trim(),
      systemPrompt: draft.systemPrompt.trim(),
      toolKeys: [...draft.toolKeys],
    };
    this.nodes.update(list => list.map((node) => {
      if (node.id !== nodeId) {
        return node;
      }
      return { ...node, ...snapshot };
    }));
    this.editingNodeId.set(null);
    this.#emitGraph();
    this.#cdr.markForCheck();
  }

  cancelNodeEdit(): void {
    this.editingNodeId.set(null);
  }

  closeAgentPicker(): void {
    this.showAgentPicker.set(false);
  }

  pickAgent(agent: AgentInfoResponse): void {
    if (!this.isEditMode() || agent.supervisor) {
      return;
    }
    this.#addNode(agent, {
      x: 100 + this.nodes().length * 220,
      y: 120,
    });
    this.showAgentPicker.set(false);
    this.validationCleared.emit();
    this.#cdr.markForCheck();
  }

  addWorkflow(): void {
    this.cancelNodeEdit();
    this.showAgentPicker.set(false);
    this.nodes.set([]);
    this.connections.set([]);
    this.editingTemplateId.set(null);
    this.isDraft.set(true);
    this.activeTemplateName.set(
      this.i18n.t().pipelines.templates.newTemplateName,
    );
    this.#activeBrief = DEFAULT_BRIEF;
    this.task.set('');
    this.workspaceMode.set('edit');
    this.#emitGraph();
    this.validationCleared.emit();
    this.templateHintChanged.emit(null);
    this.#cdr.markForCheck();
  }

  useTemplate(template: PipelineTemplateDefinitionResponse): void {
    this.#applyTemplate({
      id: template.id,
      name: template.name,
      description: template.description,
      agentTypes: template.agentTypes,
      shortTopic: template.shortTopic,
      briefPrompt: template.briefPrompt,
    }, 'use');
  }

  editTemplate(template: PipelineTemplateDefinitionResponse): void {
    this.#applyTemplate({
      id: template.id,
      name: template.name,
      description: template.description,
      agentTypes: template.agentTypes,
      shortTopic: template.shortTopic,
      briefPrompt: template.briefPrompt,
    }, 'edit');
  }

  addFromTemplate(template: PipelineTemplateDefinitionResponse): void {
    if (this.addingTemplateId() !== null || this.isSaved(template)) {
      return;
    }
    this.addingTemplateId.set(template.id);
    this.#pipelinesApi.createTemplateFromDefinition(template.id).subscribe({
      next: () => {
        this.addingTemplateId.set(null);
        this.#notifications.showSuccess(
          this.i18n.t().pipelines.templates.added,
        );
        this.#reloadSavedTemplates();
      },
      error: () => {
        this.addingTemplateId.set(null);
        this.#notifications.showError(
          this.i18n.t().pipelines.templates.errors.saveFailed,
        );
      },
    });
  }

  useSavedTemplate(template: PipelineTemplate): void {
    this.#applyTemplate({
      id: template.id,
      name: template.name,
      description: template.description,
      agentTypes: template.agentTypes,
      shortTopic: template.shortTopic,
      briefPrompt: template.briefPrompt,
      savedTemplateId: template.id,
    }, 'use');
  }

  editSavedTemplate(template: PipelineTemplate): void {
    this.#applyTemplate({
      id: template.id,
      name: template.name,
      description: template.description,
      agentTypes: template.agentTypes,
      shortTopic: template.shortTopic,
      briefPrompt: template.briefPrompt,
      savedTemplateId: template.id,
    }, 'edit');
  }

  deleteSavedTemplate(template: PipelineTemplate): void {
    const message = this.i18n.t().pipelines.templates.deleteConfirm.replace(
      '{name}',
      template.name,
    );
    if (!globalThis.confirm(message)) {
      return;
    }
    this.#pipelinesApi.deleteTemplate(template.id).subscribe({
      next: () => this.#reloadSavedTemplates(),
      error: () => {
        this.#notifications.showError(
          this.i18n.t().pipelines.templates.errors.deleteFailed,
        );
      },
    });
  }

  onCreateConnection(event: FCreateConnectionEvent): void {
    if (!this.isEditMode() || !hasText(event.targetId)) {
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
    this.#connectionSeq += 1;
    this.connections.update(list => [
      ...list,
      {
        id: `edge-${String(this.#connectionSeq)}`,
        sourceNodeId,
        targetNodeId,
      },
    ]);
    this.#emitGraph();
    this.validationCleared.emit();
    this.#cdr.markForCheck();
  }

  onMoveNodes(event: FMoveNodesEvent): void {
    if (!this.isEditMode()) {
      return;
    }
    const moved = new Map(event.nodes.map(node => [node.id, node.position]));
    this.nodes.update(list => list.map((node) => {
      const position = moved.get(node.id);
      return position !== undefined
        ? { ...node, position: { x: position.x, y: position.y } }
        : node;
    }),
    );
    this.#emitGraph();
    this.#cdr.markForCheck();
  }

  outId(nodeId: string): string {
    return connectorOutId(nodeId);
  }

  inId(nodeId: string): string {
    return connectorInId(nodeId);
  }

  openNodeEditor(node: PipelineNode): void {
    if (!this.isEditMode()) {
      return;
    }
    this.showAgentPicker.set(false);
    this.editingNodeId.set(node.id);
    this.#nodeDraft.set({
      name: node.name,
      description: node.description,
      systemPrompt: node.systemPrompt,
      toolKeys: [...node.toolKeys],
    });
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
    this.#emitGraph();
    this.validationCleared.emit();
    this.#cdr.markForCheck();
  }

  isSaved(template: PipelineTemplateDefinitionResponse): boolean {
    return this.savedTemplates().some(item => item.sourceTemplateId === template.id);
  }

  #applyTemplate(template: ApplyableTemplate, mode: 'edit' | 'use'): void {
    this.cancelNodeEdit();
    this.showAgentPicker.set(false);
    const seed = this.#nodeSeq + 1;
    const result = applyPipelineTemplate(
      { id: template.id, agentTypes: template.agentTypes },
      this.workers(),
      seed,
    );
    this.#nodeSeq = seed + result.graph.nodes.length;
    this.#connectionSeq = seed + result.graph.connections.length;
    this.nodes.set(result.graph.nodes);
    this.connections.set(result.graph.connections);
    this.workspaceMode.set(mode);
    this.activeTemplateName.set(template.name);
    this.#activeBrief = textOr(template.briefPrompt, DEFAULT_BRIEF);
    this.editingTemplateId.set(template.savedTemplateId ?? null);
    this.isDraft.set(false);
    this.#emitGraph();
    this.validationCleared.emit();
    const defaultTask = template.shortTopic.trim();
    if (defaultTask !== '') {
      this.task.set(defaultTask);
    }
    this.templateApplied.emit({
      topic: textOr(defaultTask, template.name),
      brief: this.#activeBrief,
    });
    if (result.skippedAgentTypes.length > 0) {
      const hint = this.i18n.t().pipelines.templates.skipped.replace(
        '{types}',
        result.skippedAgentTypes.join(', '),
      );
      this.templateHintChanged.emit(hint);
    } else {
      this.templateHintChanged.emit(null);
    }
    this.#cdr.markForCheck();
  }

  #persistTemplateIfNeeded(done: () => void): void {
    const shouldPersist = this.isDraft() || this.editingTemplateId() !== null;
    if (!shouldPersist) {
      done();
      return;
    }
    const request = this.#buildTemplateWriteRequest();
    if (request === null) {
      this.#notifications.showWarning(
        this.i18n.t().pipelines.templates.canvasEmpty,
      );
      return;
    }
    this.isSaving.set(true);
    const savedTemplateId = this.editingTemplateId();
    const request$ = hasText(savedTemplateId)
      ? this.#pipelinesApi.updateTemplate(savedTemplateId, request)
      : this.#pipelinesApi.createTemplate(request);
    request$.subscribe({
      next: (saved) => {
        this.isSaving.set(false);
        this.editingTemplateId.set(saved.id);
        this.isDraft.set(false);
        this.activeTemplateName.set(saved.name);
        this.#reloadSavedTemplates();
        done();
      },
      error: () => {
        this.isSaving.set(false);
        this.#notifications.showError(
          this.i18n.t().pipelines.templates.errors.saveFailed,
        );
      },
    });
  }

  #buildTemplateWriteRequest(): CreatePipelineTemplateRequest | null {
    const agentTypes = this.nodes().map(node => node.agentType);
    if (agentTypes.length === 0) {
      return null;
    }
    const name = textOr(
      this.activeTemplateName().trim(),
      this.i18n.t().pipelines.templates.newTemplateName,
    );
    return {
      name,
      description: '',
      agentTypes,
      shortTopic: this.task().trim(),
      briefPrompt: textOr(this.#activeBrief.trim(), DEFAULT_BRIEF),
    };
  }

  #reloadBuiltinTemplates(): void {
    this.#pipelinesApi.listTemplateDefinitions().subscribe({
      next: (templates) => {
        this.builtinTemplates.set(templates);
        this.#cdr.markForCheck();
      },
      error: () => undefined,
    });
  }

  #reloadSavedTemplates(): void {
    this.#pipelinesApi.listTemplates().subscribe({
      next: (savedTemplates) => {
        this.savedTemplates.set(savedTemplates);
        this.#cdr.markForCheck();
      },
      error: () => undefined,
    });
  }

  #addNode(agent: AgentInfoResponse, position: { x: number; y: number }): void {
    const chainTailId = this.#findChainTailId();
    this.#nodeSeq += 1;
    const nodeId = `node-${String(this.#nodeSeq)}`;
    this.nodes.update(list => [
      ...list,
      {
        id: nodeId,
        agentType: agent.type,
        name: agent.name,
        description: agent.description,
        systemPrompt: agent.systemPrompt,
        toolKeys: [...agent.toolKeys],
        position,
      },
    ]);
    if (hasText(chainTailId)) {
      this.#connectionSeq += 1;
      this.connections.update(list => [
        ...list,
        {
          id: `edge-${String(this.#connectionSeq)}`,
          sourceNodeId: chainTailId,
          targetNodeId: nodeId,
        },
      ]);
    }
    this.#emitGraph();
  }

  #findChainTailId(): string | null {
    const nodes = this.nodes();
    const sources = new Set(this.connections().map(edge => edge.sourceNodeId));
    const tail = nodes.filter(node => !sources.has(node.id)).at(-1) ?? nodes.at(-1);
    return tail?.id ?? null;
  }

  #emitGraph(): void {
    this.graphChanged.emit(this.graph());
  }
}
