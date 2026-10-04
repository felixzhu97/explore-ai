import type { ChatSourceView } from './chat-bubble-list.component';

const LABEL_MAX_CHARS = 14;
const TITLE_MAX_CHARS = 80;

export function truncateText(text: string, max: number): string {
  if (!text) {
    return '';
  }
  return text.length > max ? `${text.slice(0, max).trimEnd()}…` : text;
}

export function sourceHostname(source: ChatSourceView): string {
  const raw = source.url?.trim();
  if (!raw) {
    return '';
  }
  try {
    const host = new URL(raw).hostname.toLowerCase();
    return host.startsWith('www.') ? host.slice(4) : host;
  } catch {
    return '';
  }
}

export function sourceLabel(source: ChatSourceView, fallback: string): string {
  const host = sourceHostname(source);
  if (host) {
    return truncateText(host, LABEL_MAX_CHARS);
  }
  if (source.title?.trim()) {
    return truncateText(source.title.trim(), LABEL_MAX_CHARS);
  }
  return truncateText(source.text, LABEL_MAX_CHARS) || fallback;
}

export function sourceFaviconUrl(source: ChatSourceView): string | null {
  const host = sourceHostname(source);
  if (!host) {
    return null;
  }
  return `https://www.google.com/s2/favicons?domain=${encodeURIComponent(host)}&sz=32`;
}

export function sourceInitial(source: ChatSourceView, fallback: string): string {
  const label = sourceLabel(source, fallback).trim();
  return (label.charAt(0) || '?').toUpperCase();
}

export function sourceTitle(source: ChatSourceView, fallback: string): string {
  if (source.title?.trim()) {
    return source.title.trim();
  }
  const host = sourceHostname(source);
  if (host) {
    return host;
  }
  return truncateText(source.text, TITLE_MAX_CHARS) || fallback;
}

export function sourcePublishedAt(source: ChatSourceView): string {
  const direct = source.publishedAt?.trim();
  if (direct) {
    return direct;
  }
  const meta = source.metadata;
  if (!meta) {
    return '';
  }
  for (const key of ['publishedAt', 'date', 'published', 'published_at']) {
    const value = meta[key];
    if (typeof value === 'string' && value.trim()) {
      return value.trim();
    }
  }
  return '';
}
