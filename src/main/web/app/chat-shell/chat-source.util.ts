import type { ChatSourceView } from './chat-bubble-list.component';
import { hasText, textOr } from '../shared/presence';

const LABEL_MAX_CHARS = 14;
const TITLE_MAX_CHARS = 80;

export function truncateText(text: string, max: number): string {
  if (text === '') {
    return '';
  }
  return text.length > max ? `${text.slice(0, max).trimEnd()}…` : text;
}

export function sourceHostname(source: ChatSourceView): string {
  const raw = source.url?.trim();
  if (!hasText(raw)) {
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
  if (host !== '') {
    return truncateText(host, LABEL_MAX_CHARS);
  }
  const title = source.title?.trim();
  if (hasText(title)) {
    return truncateText(title, LABEL_MAX_CHARS);
  }
  return textOr(truncateText(source.text, LABEL_MAX_CHARS), fallback);
}

export function sourceFaviconUrl(source: ChatSourceView): string | null {
  const host = sourceHostname(source);
  if (host === '') {
    return null;
  }
  return `https://www.google.com/s2/favicons?domain=${encodeURIComponent(host)}&sz=32`;
}

export function sourceInitial(source: ChatSourceView, fallback: string): string {
  const label = sourceLabel(source, fallback).trim();
  return (label === '' ? '?' : label.charAt(0)).toUpperCase();
}

export function sourceTitle(source: ChatSourceView, fallback: string): string {
  const title = source.title?.trim();
  if (hasText(title)) {
    return title;
  }
  const host = sourceHostname(source);
  if (host !== '') {
    return host;
  }
  return textOr(truncateText(source.text, TITLE_MAX_CHARS), fallback);
}

export function sourcePublishedAt(source: ChatSourceView): string {
  const direct = source.publishedAt?.trim();
  if (hasText(direct)) {
    return direct;
  }
  const meta = source.metadata;
  if (meta === undefined) {
    return '';
  }
  for (const key of ['publishedAt', 'date', 'published', 'published_at']) {
    const value = meta[key];
    if (typeof value === 'string' && value.trim() !== '') {
      return value.trim();
    }
  }
  return '';
}
