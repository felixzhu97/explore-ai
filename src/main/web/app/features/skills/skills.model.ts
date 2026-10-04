export interface Skill {
  id: string;
  name: string;
  description: string;
  instructions: string;
  allowedTools: string[];
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface SkillTemplate {
  id: string;
  name: string;
  description: string;
  instructions: string;
  allowedTools: string[];
  nameAliases?: string[];
}

export interface SkillWriteRequest {
  name: string;
  description: string;
  instructions: string;
  allowedTools: string[];
}
