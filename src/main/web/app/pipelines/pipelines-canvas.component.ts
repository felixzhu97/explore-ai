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
import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
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

interface ApplyableTemplate {
  id: string;
  name: string;
  description: string;
  agentTypes: string[];
  shortTopic: string;
  briefPrompt: string;
}

@Component({
  selector: 'app-pipelines-canvas',
  imports: [DragDropModule, FFlowModule, FormsModule],
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
  readonly runRequested = output<PipelineGraph>();
  readonly graphChange = output<PipelineGraph>();
  readonly clearValidation = output<void>();
  readonly templateHint = output<string | null>();
  /** Emits short topic for the input box and brief prompt for invoke-time merge. */
  readonly templateApplied = output<{ topic: string; brief: string }>();

  readonly nodes = signal<PipelineNode[]>([]);
  readonly connections = signal<PipelineConnection[]>([]);
  readonly builtinTemplates = signal<WorkflowTemplate[]>([]);
  readonly library = signal<SavedWorkflowTemplate[]>([]);
  readonly addingTemplateId = signal<string | null>(null);
  readonly showForm = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly formName = signal('');
  readonly formDescription = signal('');
  readonly formAgentTypes = signal('research, analyst');
  readonly formShortTopic = signal('');
  readonly formBriefPrompt = signal('');
  readonly saving = signal(false);

  private nodeSeq = 0;
  private connectionSeq = 0;

  readonly workers = computed(() => this.agents().filter(agent => !agent.supervisor));

  readonly enabledLibrary = computed(() => this.library().filter(item => item.enabled));

  readonly ownedNames = computed(() => {
    const names = new Set<string>();
    for (const item of this.library()) {
      names.add(item.name.trim().toLowerCase());
    }
    return names;
  });

  readonly isEmpty = computed(() => this.nodes().length === 0);

  /** Drop target holds no items; agents come from the palette via cdkDragData. */
  readonly canvasDropData: AgentInfo[] = [];

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
    this.reloadLibrary();
    this.reloadBuiltinTemplates();
  }

  isInLibrary(template: WorkflowTemplate): boolean {
    const owned = this.ownedNames();
    const aliases = [template.name, ...(template.nameAliases ?? [])]
      .map(name => name.trim().toLowerCase())
      .filter(Boolean);
    return aliases.some(
      alias => owned.has(alias) || [...owned].some(ownedName => ownedName.startsWith(`${alias} (`)),
    );
  }

  onCanvasDrop(event: CdkDragDrop<AgentInfo[]>): void {
    const agent = event.item.data as AgentInfo | undefined;
    if (!agent || agent.supervisor) {
      return;
    }
    this.addNode(agent, {
      x: 120 + this.nodes().length * 40,
      y: 120 + this.nodes().length * 24,
    });
    this.clearValidation.emit();
  }

  addNodeFromClick(agent: AgentInfo): void {
    if (agent.supervisor) {
      return;
    }
    this.addNode(agent, {
      x: 100 + this.nodes().length * 220,
      y: 120,
    });
    this.clearValidation.emit();
  }

  onCreateConnection(event: FCreateConnectionEvent): void {
    if (!event.targetId) {
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
    this.nodes.update(list => list.filter(node => node.id !== nodeId));
    this.connections.update(list => list.filter(
      edge => edge.sourceNodeId !== nodeId && edge.targetNodeId !== nodeId,
    ),
    );
    this.emitGraph();
    this.clearValidation.emit();
  }

  clearCanvas(): void {
    this.nodes.set([]);
    this.connections.set([]);
    this.emitGraph();
    this.clearValidation.emit();
    this.templateHint.emit(null);
  }

  applyBuiltin(template: WorkflowTemplate): void {
    this.applyTemplate({
      id: template.id,
      name: template.name,
      description: template.description,
      agentTypes: template.agentTypes,
      shortTopic: template.shortTopic,
      briefPrompt: template.briefPrompt,
    });
  }

  applySaved(template: SavedWorkflowTemplate): void {
    this.applyTemplate({
      id: template.id,
      name: template.name,
      description: template.description,
      agentTypes: template.agentTypes,
      shortTopic: template.shortTopic,
      briefPrompt: template.briefPrompt,
    });
  }

  addFromTemplate(template: WorkflowTemplate): void {
    this.addingTemplateId.set(template.id);
    this.pipelinesApi.createFromTemplate(template.id).subscribe({
      next: () => {
        this.addingTemplateId.set(null);
        this.reloadLibrary();
        this.notifications.showSuccess(this.templateChrome().added);
      },
      error: () => {
        this.addingTemplateId.set(null);
        this.notifications.showError(this.templateChrome().saveFailed);
      },
    });
  }

  customizeTemplate(template: WorkflowTemplate): void {
    this.editingId.set(null);
    this.formName.set(template.name);
    this.formDescription.set(template.description);
    this.formAgentTypes.set(template.agentTypes.join(', '));
    this.formShortTopic.set(template.shortTopic);
    this.formBriefPrompt.set(template.briefPrompt);
    this.showForm.set(true);
    this.cdr.markForCheck();
  }

  startCreate(): void {
    this.editingId.set(null);
    this.formName.set('');
    this.formDescription.set('');
    this.formAgentTypes.set('research, analyst');
    this.formShortTopic.set('');
    this.formBriefPrompt.set('');
    this.showForm.set(true);
  }

  editSaved(template: SavedWorkflowTemplate): void {
    this.editingId.set(template.id);
    this.formName.set(template.name);
    this.formDescription.set(template.description);
    this.formAgentTypes.set(template.agentTypes.join(', '));
    this.formShortTopic.set(template.shortTopic);
    this.formBriefPrompt.set(template.briefPrompt);
    this.showForm.set(true);
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.editingId.set(null);
  }

  saveForm(): void {
    const request = this.buildWriteRequest();
    if (!request) {
      this.notifications.showError(this.templateChrome().nameRequired);
      return;
    }
    this.saving.set(true);
    const editingId = this.editingId();
    const request$ = editingId
      ? this.pipelinesApi.updateLibraryTemplate(editingId, request)
      : this.pipelinesApi.createLibraryTemplate(request);
    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.showForm.set(false);
        this.editingId.set(null);
        this.reloadLibrary();
        this.notifications.showSuccess(this.templateChrome().added);
      },
      error: () => {
        this.saving.set(false);
        this.notifications.showError(this.templateChrome().saveFailed);
      },
    });
  }

  toggleEnabled(template: SavedWorkflowTemplate): void {
    const enabled = !template.enabled;
    this.pipelinesApi.setLibraryTemplateEnabled(template.id, enabled).subscribe({
      next: () => this.reloadLibrary(),
      error: () => {
        this.notifications.showError(this.templateChrome().updateFailed);
      },
    });
  }

  deleteSaved(template: SavedWorkflowTemplate): void {
    const message = this.templateChrome().deleteConfirm.replace('{name}', template.name);
    if (!globalThis.confirm(message)) {
      return;
    }
    this.pipelinesApi.deleteLibraryTemplate(template.id).subscribe({
      next: () => this.reloadLibrary(),
      error: () => {
        this.notifications.showError(this.templateChrome().deleteFailed);
      },
    });
  }

  templateOrder(agentTypes: readonly string[]): string {
    return agentTypes.join(' → ');
  }

  run(): void {
    this.runRequested.emit(this.graph());
  }

  outId(nodeId: string): string {
    return connectorOutId(nodeId);
  }

  inId(nodeId: string): string {
    return connectorInId(nodeId);
  }

  private applyTemplate(template: ApplyableTemplate): void {
    const seed = this.nodeSeq + 1;
    const result = applyPipelineTemplate(
      { id: template.id, agentTypes: template.agentTypes },
      this.agents(),
      seed,
    );
    this.nodeSeq = seed + result.graph.nodes.length;
    this.connectionSeq = seed + result.graph.connections.length;
    this.nodes.set(result.graph.nodes);
    this.connections.set(result.graph.connections);
    this.emitGraph();
    this.clearValidation.emit();
    this.templateApplied.emit({
      topic: template.shortTopic || template.name,
      brief: template.briefPrompt,
    });
    if (result.skippedAgentTypes.length > 0) {
      const hint = this.templateChrome().skipped.replace(
        '{types}',
        result.skippedAgentTypes.join(', '),
      );
      this.templateHint.emit(hint);
    } else {
      this.templateHint.emit(null);
    }
    this.cdr.markForCheck();
  }

  private templateChrome() {
    return this.i18n.t().pipelines.pipeline.templates;
  }

  private buildWriteRequest(): WorkflowTemplateWriteRequest | null {
    const name = this.formName().trim();
    const briefPrompt = this.formBriefPrompt().trim();
    const agentTypes = this.formAgentTypes()
      .split(/[,，\s]+/)
      .map(part => part.trim().toLowerCase())
      .filter(Boolean);
    if (!name || !briefPrompt || agentTypes.length === 0) {
      return null;
    }
    return {
      name,
      description: this.formDescription().trim(),
      agentTypes,
      shortTopic: this.formShortTopic().trim(),
      briefPrompt,
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

  /** Prefer a node with no outgoing edge; otherwise the last node on the canvas. */
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
