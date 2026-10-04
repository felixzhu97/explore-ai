export function formatMessageTime(timestamp: number): string {
  return new Date(timestamp).toLocaleTimeString();
}
