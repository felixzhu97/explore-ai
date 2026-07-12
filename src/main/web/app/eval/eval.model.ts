export interface EvaluationRequest {
  userMessage: string;
  assistantResponse: string;
  referenceDocuments?: string[];
}

export interface EvaluationResponse {
  coherenceScore: number;
  relevanceScore: number;
  helpfulnessScore: number;
  factualityScore: number | null;
  factualityAvailable: boolean;
  overallScore: number;
  hasSafetyIssues: boolean;
  safetyFlags: string[];
  suggestions: string[];
}
