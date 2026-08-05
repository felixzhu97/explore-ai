export interface AgentSessionInfo {
  id: string;
  title: string;
  createdAt: string;
  lastActivityAt: string;
}

export interface AgentInvokeRequest {
  message: string;
}

export interface AgentPlanEvent {
  summary: string;
  goal: string;
  steps: string[];
  iteration: number;
}

export interface AgentThoughtEvent {
  text: string;
  iteration: number;
}

export interface AgentEvaluationEvent {
  verdict: string;
  feedback: string;
  iteration: number;
}
