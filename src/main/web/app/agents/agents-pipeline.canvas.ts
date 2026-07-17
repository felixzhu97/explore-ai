import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import {
  FCreateConnectionEvent,
  FFlowModule,
  FMoveNodesEvent,
} from '@foblex/flow';
import { I18nService } from '@core/i18n';
import type { AgentInfo } from './agents.model';
import {
  connectorInId,
  connectorOutId,
  nodeIdFromConnector,
  type PipelineConnection,
  type PipelineGraph,
  type PipelineNode,
} from './agents-pipeline.model';
import {
  applyPipelineTemplate,
  findPipelineTemplate,
  PIPELINE_TEMPLATE_CATALOG,
  type PipelineTemplateId,
} from './agents-pipeline.templates';

@Component({
  selector: 'app-agents-pipeline-canvas',
  imports: [DragDropModule, FFlowModule],
  templateUrl: './agents-pipeline.canvas.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex min-h-0 flex-1 overflow-hidden' },
})
export class AgentsPipelineCanvasComponent {
  private readonly cdr = inject(ChangeDetectorRef);
  readonly i18n = inject(I18nService);

  readonly agents = input.required<AgentInfo[]>();
  readonly validationHint = input<string | null>(null);
  readonly runRequested = output<PipelineGraph>();
  readonly clearValidation = output<void>();
  readonly templateHint = output<string | null>();

  readonly nodes = signal<PipelineNode[]>([]);
  readonly connections = signal<PipelineConnection[]>([]);
  private nodeSeq = 0;
  private connectionSeq = 0;

  readonly workers = computed(() => this.agents().filter(agent => !agent.supervisor),
  );

  readonly templates = PIPELINE_TEMPLATE_CATALOG;

  /** Drop target holds no items; agents come from the palette via cdkDragData. */
  readonly canvasDropData: AgentInfo[] = [];

  readonly graph = computed<PipelineGraph>(() => ({
    nodes: this.nodes(),
    connections: this.connections(),
  }));

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
      x: 100 + this.nodes().length * 36,
      y: 100 + this.nodes().length * 28,
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
    this.cdr.markForCheck();
  }

  removeNode(nodeId: string): void {
    this.nodes.update(list => list.filter(node => node.id !== nodeId));
    this.connections.update(list => list.filter(
      edge => edge.sourceNodeId !== nodeId && edge.targetNodeId !== nodeId,
    ),
    );
    this.clearValidation.emit();
  }

  clearCanvas(): void {
    this.nodes.set([]);
    this.connections.set([]);
    this.clearValidation.emit();
    this.templateHint.emit(null);
  }

  applyTemplate(id: PipelineTemplateId): void {
    const definition = findPipelineTemplate(id);
    if (!definition) {
      return;
    }
    const seed = this.nodeSeq + 1;
    const result = applyPipelineTemplate(definition, this.agents(), seed);
    this.nodeSeq = seed + result.graph.nodes.length;
    this.connectionSeq = seed + result.graph.connections.length;
    this.nodes.set(result.graph.nodes);
    this.connections.set(result.graph.connections);
    this.clearValidation.emit();
    if (result.skippedAgentTypes.length > 0) {
      const template = this.i18n.t().agents.pipeline.templates.skipped.replace(
        '{types}',
        result.skippedAgentTypes.join(', '),
      );
      this.templateHint.emit(template);
    } else {
      this.templateHint.emit(null);
    }
    this.cdr.markForCheck();
  }

  templateLabel(id: PipelineTemplateId): { name: string; description: string } {
    return this.i18n.t().agents.pipeline.templates.items[id];
  }

  templateOrder(id: PipelineTemplateId): string {
    const definition = findPipelineTemplate(id);
    return definition?.agentTypes.join(' → ') ?? '';
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

  private addNode(agent: AgentInfo, position: { x: number; y: number }): void {
    this.nodeSeq += 1;
    this.nodes.update(list => [
      ...list,
      {
        id: `node-${this.nodeSeq}`,
        agentType: agent.type,
        name: agent.name,
        description: agent.description,
        position,
      },
    ]);
  }
}
