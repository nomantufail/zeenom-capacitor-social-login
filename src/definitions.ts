export interface LoginPluginPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
