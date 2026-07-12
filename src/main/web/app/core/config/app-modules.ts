export interface AppModules {
  vision: boolean;
  audioAsr: boolean;
  mcp: boolean;
  eval: boolean;
}

export function isModuleEnabled(modules: AppModules, module: keyof AppModules): boolean {
  return modules[module];
}
