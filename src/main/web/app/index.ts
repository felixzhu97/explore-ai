// Re-export shared UI components for backwards compatibility
export { ButtonComponent } from '@shared/components/ui/button';
export type { ButtonVariant, ButtonSize } from '@shared/components/ui/button';

export { CardComponent } from '@shared/components/ui/card';
export type { CardVariant, CardPadding } from '@shared/components/ui/card';

export { SegmentedControlComponent } from '@shared/components/ui/segmented-control';
export type { SegmentedControlOption } from '@shared/components/ui/segmented-control';

export { ImageZoomModalComponent } from '@shared/components/image-zoom-modal';

// Agents
export { AgentPageComponent, type AgentInfo } from './agents';
export { ChatMessageComponent, type ChatMessageData } from './agents';
export { StatusBadgeComponent, type BadgeStatus } from './agents';
export { ToolResultComponent, type ToolCall, type ToolCallStatus } from './agents';

// Panels
export { AgentPanelComponent } from './panels';
export { AIInfraPanelComponent } from './panels';
