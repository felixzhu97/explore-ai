export interface PluginDefinition {
  id: string;
  name: string;
  description: string;
  category: string;
  iconKey: string;
  featured: boolean;
  builtin: boolean;
  docsUrl: string;
  requiresEndpoint: boolean;
}

export interface PluginInstallation {
  id: string;
  definitionId: string;
  displayName: string;
  endpoint: string | null;
  enabled: boolean;
  healthStatus: string;
  builtin: boolean;
  hasAuthToken: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface InstallPluginRequest {
  definitionId: string;
  endpoint?: string;
  authToken?: string;
  customName?: string;
}
