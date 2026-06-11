// Re-export shared UI components for backwards compatibility
export { ButtonComponent } from '@shared/components/ui/button';
export type { ButtonVariant, ButtonSize } from '@shared/components/ui/button';

export { CardComponent } from '@shared/components/ui/card';
export type { CardVariant, CardPadding } from '@shared/components/ui/card';

export { SegmentedControlComponent } from '@shared/components/ui/segmented-control';
export type { SegmentedControlOption } from '@shared/components/ui/segmented-control';

export { ImageZoomModalComponent } from '@shared/components/image-zoom-modal';

// Agents
export { AgentChatComponent, type AgentInfo } from '@features/agents';
export { ChatMessageComponent, type ChatMessageData } from '@features/agents';
export { StatusBadgeComponent, type BadgeStatus } from '@features/agents';
export { ToolResultComponent, type ToolCall, type ToolCallStatus } from '@features/agents';

// Panels
export { AgentPanelComponent } from '@features/panels';
export { AIInfraPanelComponent } from '@features/panels';
