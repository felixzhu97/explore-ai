export interface SavedAgent {
  id: string;
  typeKey: string;
  name: string;
  description: string;
  systemPrompt: string;
  toolKeys: string[];
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface SavedAgentWriteRequest {
  typeKey?: string;
  name: string;
  description: string;
  systemPrompt: string;
  toolKeys: string[];
}
