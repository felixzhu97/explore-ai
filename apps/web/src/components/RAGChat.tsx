import { useState, useRef, useEffect, useCallback } from 'react';
import styled from '@emotion/styled';
import { css, keyframes } from '@emotion/react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeHighlight from 'rehype-highlight';
import { colors, shadows, radius, spacing, typography } from '../theme';
import { useI18n } from '../i18n';
import { ImageZoomModal } from './ImageZoomModal';

const RAG_API_URL = import.meta.env.VITE_RAG_SERVICE_URL || 'http://localhost:8010';

interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  sources?: SourceDocument[];
  timestamp: number;
}

interface SourceDocument {
  text: string;
  score: number;
  metadata: Record<string, unknown>;
}

interface UploadedDocument {
  id: string;
  title: string;
  status: 'uploading' | 'success' | 'error';
  progress?: number;
  error?: string;
}

interface Toast {
  id: string;
  message: string;
  type: 'success' | 'error' | 'info';
}

const fadeIn = keyframes`
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
`;

const slideIn = keyframes`
  from { opacity: 0; transform: translateX(100%); }
  to { opacity: 1; transform: translateX(0); }
`;

const pulse = keyframes`
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
`;

const transitions = {
  fast: '0.15s ease',
  default: '0.2s ease',
};

const spin = keyframes`
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
`;

const Container = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${spacing.lg};
  animation: ${fadeIn} 0.3s ease;
`;

const Header = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: ${spacing.md};
  border-bottom: 1px solid ${colors.border};
`;

const Title = styled.h2`
  font-size: ${typography.fontSize.xl};
  font-weight: ${typography.fontWeight.semibold};
  color: ${colors.text};
  margin: 0;
`;

const ModelBadge = styled.span`
  font-size: ${typography.fontSize.xs};
  color: ${colors.textSecondary};
  background: ${colors.surface};
  padding: 4px 8px;
  border-radius: ${radius.full};
  border: 1px solid ${colors.border};
`;

const ToastContainer = styled.div`
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  gap: ${spacing.sm};
`;

const ToastItem = styled.div<{ type: 'success' | 'error' | 'info' }>`
  padding: ${spacing.md} ${spacing.lg};
  border-radius: ${radius.lg};
  font-size: ${typography.fontSize.sm};
  box-shadow: ${shadows.elevated};
  animation: ${slideIn} 0.3s ease;
  display: flex;
  align-items: center;
  gap: ${spacing.sm};
  min-width: 280px;
  max-width: 400px;

  ${({ type }) => {
    switch (type) {
      case 'success':
        return css`
          background: #d4edda;
          color: #155724;
          border: 1px solid #c3e6cb;
        `;
      case 'error':
        return css`
          background: #f8d7da;
          color: #721c24;
          border: 1px solid #f5c6cb;
        `;
      default:
        return css`
          background: ${colors.surface};
          color: ${colors.text};
          border: 1px solid ${colors.border};
        `;
    }
  }}
`;

const ToastIcon = styled.span`
  font-size: 18px;
`;

const DocumentsSection = styled.div`
  background: ${colors.surface};
  border: 1px solid ${colors.border};
  border-radius: ${radius.lg};
  padding: ${spacing.md};
`;

const SectionHeader = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: ${spacing.md};
`;

const SectionTitle = styled.h3`
  font-size: ${typography.fontSize.base};
  font-weight: ${typography.fontWeight.medium};
  color: ${colors.text};
  margin: 0;
  display: flex;
  align-items: center;
  gap: ${spacing.sm};
`;

const DocumentCount = styled.span`
  font-size: ${typography.fontSize.xs};
  color: ${colors.textSecondary};
  background: ${colors.surfaceSecondary};
  padding: 2px 8px;
  border-radius: ${radius.full};
`;

const DocumentsList = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: ${spacing.sm};
  min-height: 32px;
`;

const DocumentChip = styled.div<{ selected?: boolean }>`
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  font-size: ${typography.fontSize.sm};
  background: ${({ selected }) => (selected ? colors.primary : colors.primaryLight)};
  color: ${({ selected }) => (selected ? 'white' : colors.primary)};
  border-radius: ${radius.full};
  border: 1px solid ${({ selected }) => (selected ? colors.primary : colors.primary + '20')};
  cursor: pointer;
  transition: all ${transitions.fast};
  user-select: none;

  &:hover {
    background: ${({ selected }) => (selected ? colors.primaryHover : colors.primary + '30')};
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(0);
  }
`;

const DocumentIcon = styled.span`
  font-size: 14px;
`;

const SelectionControls = styled.div`
  display: flex;
  gap: ${spacing.sm};
  margin-top: ${spacing.sm};
  flex-wrap: wrap;
`;

const SelectButton = styled.button`
  padding: 4px 10px;
  font-size: ${typography.fontSize.xs};
  background: transparent;
  border: 1px solid ${colors.border};
  border-radius: ${radius.full};
  color: ${colors.textSecondary};
  cursor: pointer;
  transition: all ${transitions.fast};
  display: flex;
  align-items: center;
  gap: 4px;

  &:hover {
    background: ${colors.surfaceSecondary};
    color: ${colors.text};
    border-color: ${colors.textSecondary};
  }
`;

const SelectedBadge = styled.span`
  font-size: ${typography.fontSize.xs};
  color: ${colors.primary};
  font-weight: ${typography.fontWeight.medium};
`;

const EmptyDocsMessage = styled.div`
  font-size: ${typography.fontSize.sm};
  color: ${colors.textTertiary};
  font-style: italic;
`;

const ChatContainer = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${spacing.md};
  max-height: 400px;
  overflow-y: auto;
  padding: ${spacing.md};
  background: ${colors.surface};
  border-radius: ${radius.lg};
  border: 1px solid ${colors.border};
`;

const MessageBubble = styled.div<{ isUser: boolean }>`
  display: flex;
  flex-direction: column;
  max-width: 80%;
  animation: ${fadeIn} 0.2s ease;

  ${({ isUser }) =>
    isUser
      ? css`
          align-self: flex-end;
          align-items: flex-end;
        `
      : css`
          align-self: flex-start;
          align-items: flex-start;
        `}
`;

const MessageContent = styled.div<{ isUser: boolean }>`
  padding: ${spacing.md};
  border-radius: ${radius.lg};
  font-size: ${typography.fontSize.base};
  line-height: ${typography.lineHeight.relaxed};
  word-break: break-word;

  ${({ isUser }) =>
    isUser
      ? css`
          background: ${colors.primary};
          color: white;
          border-bottom-right-radius: ${radius.sm};
        `
      : css`
          background: ${colors.surfaceSecondary};
          color: ${colors.text};
          border-bottom-left-radius: ${radius.sm};
        `}

  /* Markdown styles */
  h1, h2, h3, h4, h5, h6 {
    margin: 0.5em 0 0.25em;
    font-weight: 600;
    line-height: 1.3;
    &:first-of-type { margin-top: 0; }
    &:last-of-type { margin-bottom: 0; }
  }
  h1 { font-size: 1.25em; }
  h2 { font-size: 1.15em; }
  h3 { font-size: 1.05em; }

  p {
    margin: 0.5em 0;
    &:first-of-type { margin-top: 0; }
    &:last-of-type { margin-bottom: 0; }
  }

  ul, ol {
    margin: 0.5em 0;
    padding-left: 1.5em;
  }

  li {
    margin: 0.25em 0;
  }

  code {
    font-family: 'SF Mono', Monaco, 'Cascadia Code', monospace;
    font-size: 0.9em;
    padding: 0.15em 0.4em;
    border-radius: 3px;
    background: ${colors.surface};
  }

  pre {
    margin: 0.5em 0;
    padding: ${spacing.sm};
    border-radius: ${radius.sm};
    background: ${colors.surface};
    overflow-x: auto;
    border: 1px solid ${colors.border};

    code {
      padding: 0;
      background: none;
    }
  }

  blockquote {
    margin: 0.5em 0;
    padding-left: 1em;
    border-left: 3px solid ${colors.primary}50;
    color: ${colors.textSecondary};
  }

  a {
    color: ${colors.primary};
    text-decoration: none;
    &:hover { text-decoration: underline; }
  }

  table {
    width: 100%;
    border-collapse: collapse;
    margin: 0.5em 0;
    font-size: 0.9em;
  }

  th, td {
    border: 1px solid ${colors.border};
    padding: 0.5em;
    text-align: left;
  }

  th {
    background: ${colors.surface};
    font-weight: 600;
  }

  strong {
    font-weight: 600;
  }

  /* Syntax highlighting */
  .hljs {
    background: ${colors.surface};
    color: ${colors.text};
  }
  .hljs-keyword, .hljs-selector-tag { color: #7c3aed; }
  .hljs-string, .hljs-attr { color: #059669; }
  .hljs-number { color: #dc2626; }
  .hljs-comment { color: ${colors.textTertiary}; font-style: italic; }
  .hljs-function, .hljs-title { color: #2563eb; }
  .hljs-variable, .hljs-params { color: #ea580c; }
  .hljs-built_in { color: #0891b2; }

  hr {
    border: none;
    border-top: 1px solid ${colors.border};
    margin: 1em 0;
  }
`;

const MessageMeta = styled.div`
  display: flex;
  align-items: center;
  gap: ${spacing.sm};
  margin-top: 4px;
  padding: 0 4px;
`;

const MessageTime = styled.span`
  font-size: ${typography.fontSize.xs};
  color: ${colors.textTertiary};
`;

const SourceBadge = styled.span`
  font-size: ${typography.fontSize.xs};
  color: ${colors.primary};
  background: ${colors.primaryLight};
  padding: 2px 6px;
  border-radius: ${radius.sm};
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  transition: all ${transitions.fast};

  &:hover {
    background: ${colors.primary}20;
  }
`;

const SourcesPanel = styled.div`
  margin-top: ${spacing.sm};
  padding: ${spacing.sm};
  background: ${colors.surface};
  border-radius: ${radius.md};
  border: 1px solid ${colors.border};
  font-size: ${typography.fontSize.sm};
  width: 100%;
  max-width: 400px;
`;

const SourcesTitle = styled.div`
  font-weight: ${typography.fontWeight.medium};
  color: ${colors.textSecondary};
  margin-bottom: ${spacing.sm};
  display: flex;
  align-items: center;
  gap: ${spacing.xs};
`;

const SourceItem = styled.div`
  padding: ${spacing.sm};
  background: ${colors.surfaceSecondary};
  border-radius: ${radius.sm};
  margin-bottom: ${spacing.sm};
  border-left: 3px solid ${colors.primary};

  &:last-of-type {
    margin-bottom: 0;
  }
`;

const SourceText = styled.div`
  color: ${colors.text};
  line-height: ${typography.lineHeight.relaxed};
  margin-bottom: 4px;

  p { margin: 0.25em 0; }
  code {
    font-size: 0.9em;
    padding: 0.1em 0.3em;
    background: ${colors.surfaceSecondary};
    border-radius: 2px;
  }
`;

const SourceMeta = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: ${typography.fontSize.xs};
  color: ${colors.textTertiary};
`;

const SourceScore = styled.span<{ score: number }>`
  color: ${({ score }) => (score > 0.8 ? colors.success : score > 0.6 ? colors.warning : colors.textSecondary)};
  font-weight: ${typography.fontWeight.medium};
`;

const InputArea = styled.div`
  display: flex;
  gap: ${spacing.sm};
  align-items: flex-end};
`;

const TextArea = styled.textarea`
  flex: 1;
  padding: ${spacing.md};
  font-size: ${typography.fontSize.base};
  font-family: ${typography.fontFamily.body};
  border: 1px solid ${colors.border};
  border-radius: ${radius.lg};
  background: ${colors.surface};
  color: ${colors.text};
  resize: none;
  min-height: 48px;
  max-height: 120px;
  transition: border-color ${transitions.fast}, box-shadow ${transitions.fast};

  &:focus {
    outline: none;
    border-color: ${colors.primary};
    box-shadow: ${shadows.input};
  }

  &::placeholder {
    color: ${colors.textTertiary};
  }
`;

const SendButton = styled.button<{ disabled?: boolean }>`
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: ${colors.primary};
  color: white;
  border: none;
  border-radius: ${radius.lg};
  cursor: ${({ disabled }) => (disabled ? 'not-allowed' : 'pointer')};
  opacity: ${({ disabled }) => (disabled ? 0.5 : 1)};
  transition: all ${transitions.default};
  font-size: 18px;

  &:hover:not(:disabled) {
    background: ${colors.primaryHover};
  }

  &:active:not(:disabled) {
    background: ${colors.primaryActive};
    transform: scale(0.95);
  }
`;

const Spinner = styled.span`
  display: inline-block;
  width: 18px;
  height: 18px;
  border: 2px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  animation: ${spin} 0.6s linear infinite;
`;

const EmptyState = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: ${spacing.xxl};
  color: ${colors.textSecondary};
  text-align: center;
  gap: ${spacing.sm};
`;

const EmptyIcon = styled.div`
  font-size: 48px;
  opacity: 0.5;
`;

const QuickActions = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: ${spacing.sm};
  margin-top: ${spacing.sm};
`;

const QuickAction = styled.button`
  padding: 6px 12px;
  font-size: ${typography.fontSize.sm};
  background: ${colors.surface};
  border: 1px solid ${colors.border};
  border-radius: ${radius.full};
  color: ${colors.primary};
  cursor: pointer;
  transition: all ${transitions.fast};

  &:hover {
    background: ${colors.primaryLight};
    border-color: ${colors.primary};
  }
`;

const FileUploadArea = styled.div`
  display: flex;
  gap: ${spacing.sm};
  padding: ${spacing.md};
  background: ${colors.surface};
  border: 1px dashed ${colors.border};
  border-radius: ${radius.lg};
  transition: all ${transitions.fast};
  flex-wrap: wrap;
  align-items: center;

  &:hover {
    border-color: ${colors.primary};
  }
`;

const FileInput = styled.input`
  display: none;
`;

const FileUploadLabel = styled.label`
  display: flex;
  align-items: center;
  gap: ${spacing.sm};
  padding: ${spacing.sm} ${spacing.md};
  font-size: ${typography.fontSize.sm};
  color: ${colors.primary};
  cursor: pointer;
  transition: all ${transitions.fast};
  border: 1px solid ${colors.primary};
  border-radius: ${radius.md};

  &:hover {
    background: ${colors.primaryLight};
  }
`;

const UploadedFiles = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: ${spacing.sm};
  flex: 1;
`;

const FileTag = styled.div<{ status?: 'uploading' | 'success' | 'error' }>`
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  font-size: ${typography.fontSize.xs};
  border-radius: ${radius.sm};
  animation: ${fadeIn} 0.2s ease;

  ${({ status }) => {
    switch (status) {
      case 'uploading':
        return css`
          background: #fff3cd;
          color: #856404;
          border: 1px solid #ffc107;
        `;
      case 'success':
        return css`
          background: ${colors.successLight};
          color: ${colors.success};
          border: 1px solid ${colors.success};
        `;
      case 'error':
        return css`
          background: ${colors.errorLight};
          color: ${colors.error};
          border: 1px solid ${colors.error};
        `;
      default:
        return css`
          background: ${colors.primaryLight};
          color: ${colors.primary};
          border: 1px solid ${colors.primary};
        `;
    }
  }}
`;

const RemoveButton = styled.button`
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  font-size: 14px;
  opacity: 0.7;
  display: flex;
  align-items: center;

  &:hover {
    opacity: 1;
  }
`;

const UploadButton = styled.button`
  padding: ${spacing.sm} ${spacing.md};
  font-size: ${typography.fontSize.sm};
  background: ${colors.primary};
  color: white;
  border: none;
  border-radius: ${radius.md};
  cursor: pointer;
  transition: all ${transitions.fast};
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: ${spacing.xs};

  &:hover:not(:disabled) {
    background: ${colors.primaryHover};
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
`;

const UploadProgress = styled.div`
  width: 60px;
  height: 4px;
  background: ${colors.border};
  border-radius: 2px;
  overflow: hidden;
`;

const UploadProgressBar = styled.div<{ progress: number }>`
  width: ${({ progress }) => progress}%;
  height: 100%;
  background: ${colors.primary};
  animation: ${pulse} 1s ease-in-out infinite;
`;

const MarkdownImage = styled.img`
  max-width: 100%;
  max-height: 200px;
  object-fit: contain;
  border-radius: ${radius.sm};
  cursor: zoom-in;
  transition: transform 0.2s ease;
  margin: 0.5em 0;

  &:hover {
    transform: scale(1.02);
  }
`;

export function RAGChat() {
  const { t, tReplace } = useI18n();
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [sessionId] = useState(() => `session_${Date.now()}`);
  const [pendingFiles, setPendingFiles] = useState<File[]>([]);
  const [uploadStatuses, setUploadStatuses] = useState<Map<string, UploadedDocument>>(new Map());
  const [availableDocs, setAvailableDocs] = useState<{ id: string; title: string }[]>([]);
  const [selectedDocIds, setSelectedDocIds] = useState<Set<string>>(new Set());
  const [toasts, setToasts] = useState<Toast[]>([]);
  const [expandedSources, setExpandedSources] = useState<Set<string>>(new Set());
  const [zoomedImage, setZoomedImage] = useState<{ src: string; alt?: string } | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const abortControllerRef = useRef<AbortController | null>(null);

  const addToast = useCallback((message: string, type: Toast['type']) => {
    const id = `toast_${Date.now()}`;
    setToasts((prev) => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 4000);
  }, []);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  useEffect(() => {
    fetchAvailableDocs();
  }, []);

  // Cleanup SSE connection on unmount
  useEffect(() => {
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
        abortControllerRef.current = null;
      }
    };
  }, []);

  const fetchAvailableDocs = async () => {
    try {
      const response = await fetch(`${RAG_API_URL}/documents/`);
      if (response.ok) {
        const data = await response.json();
        const docs = data.documents || [];
        setAvailableDocs(docs.map((d: { doc_id: string; filename: string }) => ({
          id: d.doc_id,
          title: d.filename,
        })));
        // Auto-select all newly fetched docs
        setSelectedDocIds(new Set(docs.map((d: { doc_id: string }) => d.doc_id)));
      }
    } catch (error) {
      console.error('Failed to fetch documents:', error);
    }
  };

  const toggleDocSelection = (docId: string) => {
    setSelectedDocIds((prev) => {
      const next = new Set(prev);
      if (next.has(docId)) {
        next.delete(docId);
      } else {
        next.add(docId);
      }
      return next;
    });
  };

  const selectAllDocs = () => {
    setSelectedDocIds(new Set(availableDocs.map((d) => d.id)));
  };

  const clearDocSelection = () => {
    setSelectedDocIds(new Set());
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (files) {
      const newFiles = Array.from(files).filter(
        (f) => !pendingFiles.some((pf) => pf.name === f.name)
      );
      setPendingFiles((prev) => [...prev, ...newFiles]);
      addToast(tReplace(t.ragChat.filesSelected, { count: newFiles.length }), 'info');
    }
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const removePendingFile = (index: number) => {
    setPendingFiles((prev) => prev.filter((_, i) => i !== index));
  };

  const handleUpload = async () => {
    if (pendingFiles.length === 0) return;

    const newStatuses = new Map<string, UploadedDocument>();

    for (const file of pendingFiles) {
      const docId = `doc_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
      newStatuses.set(file.name, {
        id: docId,
        title: file.name,
        status: 'uploading',
        progress: 0,
      });
      setUploadStatuses(new Map(newStatuses));

      const formData = new FormData();
      formData.append('file', file);
      formData.append('title', file.name);

      try {
        const response = await fetch(`${RAG_API_URL}/documents/upload`, {
          method: 'POST',
          body: formData,
        });

        if (!response.ok) {
          throw new Error('Upload failed');
        }

        const data = await response.json();
        newStatuses.set(file.name, {
          id: data.id || docId,
          title: file.name,
          status: 'success',
        });
        setUploadStatuses(new Map(newStatuses));
        addToast(tReplace(t.ragChat.uploadSuccess, { name: file.name }), 'success');
      } catch (error) {
        console.error('Error uploading file:', error);
        newStatuses.set(file.name, {
          id: docId,
          title: file.name,
          status: 'error',
          error: 'Upload failed',
        });
        setUploadStatuses(new Map(newStatuses));
        addToast(tReplace(t.ragChat.uploadFailed, { name: file.name }), 'error');
      }
    }

    setPendingFiles([]);
    fetchAvailableDocs();

    setTimeout(() => {
      setUploadStatuses(new Map());
    }, 2000);
  };

  const handleSend = async () => {
    if (!input.trim() || isLoading) return;

    // Cancel any existing request
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    abortControllerRef.current = new AbortController();

    const userMessage: Message = {
      id: `user_${Date.now()}`,
      role: 'user',
      content: input.trim(),
      timestamp: Date.now(),
    };

    setMessages((prev) => [...prev, userMessage]);
    setInput('');
    setIsLoading(true);

    // Add placeholder for streaming response
    const assistantMessageId = `assistant_${Date.now()}`;
    setMessages((prev) => [
      ...prev,
      {
        id: assistantMessageId,
        role: 'assistant',
        content: '',
        timestamp: Date.now(),
      },
    ]);

    try {
      const requestBody: Record<string, unknown> = {
        query: userMessage.content,
        session_id: sessionId,
        top_k: 5,
        temperature: 0.7,
      };

      if (selectedDocIds.size > 0) {
        requestBody.doc_ids = Array.from(selectedDocIds);
      }

      // Use streaming endpoint
      const response = await fetch(`${RAG_API_URL}/chat/stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestBody),
        signal: abortControllerRef.current.signal,
      });

      if (!response.ok) {
        throw new Error('Failed to get response');
      }

      const reader = response.body?.getReader();
      if (!reader) {
        throw new Error('Response body not available');
      }

      const decoder = new TextDecoder();
      let fullContent = '';
      let currentEvent = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        const chunk = decoder.decode(value, { stream: true });
        const lines = chunk.split('\n');

        for (const line of lines) {
          if (line.startsWith('event: ')) {
            currentEvent = line.slice(7).trim();
          } else if (line.startsWith('data: ')) {
            const data = line.slice(6);
            
            if (currentEvent === 'sources') {
              try {
                const sourcesData = JSON.parse(data);
                setMessages((prev) =>
                  prev.map((msg) =>
                    msg.id === assistantMessageId
                      ? { ...msg, sources: sourcesData }
                      : msg
                  )
                );
              } catch {
                // Ignore parse errors for sources
              }
              currentEvent = '';
            } else if (currentEvent === 'meta') {
              currentEvent = '';
            } else if (data === '[DONE]') {
              break;
            } else if (data.startsWith('Error:')) {
              throw new Error(data.slice(6));
            } else {
              // Handle HTML line breaks in content
              const text = data.replace(/<br>/g, '\n');
              fullContent += text;

              setMessages((prev) =>
                prev.map((msg) =>
                  msg.id === assistantMessageId
                    ? { ...msg, content: fullContent }
                    : msg
                )
              );
            }
          } else if (line.trim() === '') {
            currentEvent = '';
          }
        }
      }
    } catch (error) {
      // Ignore abort errors (user cancelled or component unmounted)
      if (error instanceof Error && error.name === 'AbortError') {
        return;
      }
      console.error('Error sending message:', error);
      setMessages((prev) =>
        prev.map((msg) =>
          msg.id === assistantMessageId
            ? {
                ...msg,
                content: t.ragChat.errorMessage,
              }
            : msg
        )
      );
    } finally {
      setIsLoading(false);
      if (abortControllerRef.current) {
        abortControllerRef.current = null;
      }
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const toggleSources = (messageId: string) => {
    setExpandedSources((prev) => {
      const next = new Set(prev);
      if (next.has(messageId)) {
        next.delete(messageId);
      } else {
        next.add(messageId);
      }
      return next;
    });
  };

  const quickQuestions = [
    t.ragChat.whatIsThis,
    t.ragChat.summarize,
    t.ragChat.keyInfo,
    t.ragChat.explain,
  ];

  const markdownComponents = {
    img: ({ src, alt }: { src?: string; alt?: string }) => (
      <MarkdownImage
        src={src || ''}
        alt={alt}
        onClick={() => src && setZoomedImage({ src, alt })}
      />
    ),
  };

  const hasPendingUploads = pendingFiles.length > 0;
  const hasUploadingFiles = Array.from(uploadStatuses.values()).some((s) => s.status === 'uploading');

  return (
    <Container>
      <ToastContainer>
        {toasts.map((toast) => (
          <ToastItem key={toast.id} type={toast.type}>
            <ToastIcon>
              {toast.type === 'success' ? '✓' : toast.type === 'error' ? '✕' : 'ℹ'}
            </ToastIcon>
            {toast.message}
          </ToastItem>
        ))}
      </ToastContainer>

      <Header>
        <Title>{t.ragChat.title}</Title>
        <ModelBadge>{t.ragChat.modelBadge}</ModelBadge>
      </Header>

      <DocumentsSection>
        <SectionHeader>
          <SectionTitle>
            {t.ragChat.documents}
            <DocumentCount>{availableDocs.length}</DocumentCount>
          </SectionTitle>
          {availableDocs.length > 0 && selectedDocIds.size > 0 && (
            <SelectedBadge>
              {tReplace(t.ragChat.selectedDocuments, { count: selectedDocIds.size })}
            </SelectedBadge>
          )}
        </SectionHeader>
        {availableDocs.length > 0 ? (
          <>
            <DocumentsList>
              {availableDocs.map((doc) => (
                <DocumentChip
                  key={doc.id}
                  selected={selectedDocIds.has(doc.id)}
                  onClick={() => toggleDocSelection(doc.id)}
                  title={selectedDocIds.has(doc.id) ? 'Click to deselect' : 'Click to select'}
                >
                  <DocumentIcon>{selectedDocIds.has(doc.id) ? '✓' : '📄'}</DocumentIcon>
                  {doc.title}
                </DocumentChip>
              ))}
            </DocumentsList>
            <SelectionControls>
              <SelectButton onClick={selectAllDocs}>
                {t.ragChat.selectAll}
              </SelectButton>
              {selectedDocIds.size > 0 && (
                <SelectButton onClick={clearDocSelection}>
                  {t.ragChat.clearSelection}
                </SelectButton>
              )}
            </SelectionControls>
          </>
        ) : (
          <EmptyDocsMessage>{t.ragChat.noDocuments}</EmptyDocsMessage>
        )}
      </DocumentsSection>

      <FileUploadArea>
        <FileInput
          ref={fileInputRef}
          type="file"
          id="file-upload"
          accept=".pdf,.md,.txt,.markdown"
          multiple
          onChange={handleFileSelect}
        />
        <FileUploadLabel htmlFor="file-upload">
          📎 {t.ragChat.uploadDocs}
        </FileUploadLabel>

        {pendingFiles.length > 0 && (
          <UploadedFiles>
            {pendingFiles.map((file, index) => {
              const status = uploadStatuses.get(file.name);
              return (
                <FileTag key={file.name} status={status?.status}>
                  {status?.status === 'uploading' && (
                    <UploadProgress>
                      <UploadProgressBar progress={status.progress || 50} />
                    </UploadProgress>
                  )}
                  {file.name}
                  <RemoveButton onClick={() => removePendingFile(index)}>×</RemoveButton>
                </FileTag>
              );
            })}
          </UploadedFiles>
        )}

        {hasPendingUploads && (
          <UploadButton onClick={handleUpload} disabled={hasUploadingFiles}>
            {hasUploadingFiles ? (
              <>
                <Spinner /> {t.ragChat.uploading}
              </>
            ) : (
              <>↑ {t.ragChat.upload} ({pendingFiles.length})</>
            )}
          </UploadButton>
        )}
      </FileUploadArea>

      <ChatContainer>
        {messages.length === 0 ? (
          <EmptyState>
            <EmptyIcon>💬</EmptyIcon>
            <p>{t.ragChat.askQuestion}</p>
            <QuickActions>
              {quickQuestions.map((q, i) => (
                <QuickAction key={i} onClick={() => setInput(q)}>
                  {q}
                </QuickAction>
              ))}
            </QuickActions>
          </EmptyState>
        ) : (
          <>
            {messages.map((msg) => (
              <MessageBubble key={msg.id} isUser={msg.role === 'user'}>
                <MessageContent isUser={msg.role === 'user'}>
                  {msg.role === 'user' ? (
                    msg.content
                  ) : (
                    <ReactMarkdown
                      remarkPlugins={[remarkGfm]}
                      rehypePlugins={[rehypeHighlight]}
                      components={markdownComponents}
                    >
                      {msg.content}
                    </ReactMarkdown>
                  )}
                </MessageContent>
                <MessageMeta>
                  <MessageTime>{new Date(msg.timestamp).toLocaleTimeString()}</MessageTime>
                  {msg.role === 'assistant' && msg.sources && msg.sources.length > 0 && (
                    <SourceBadge onClick={() => toggleSources(msg.id)}>
                      📚 {tReplace(t.ragChat.basedOn, { count: msg.sources.length })}
                      {expandedSources.has(msg.id) ? ' ▲' : ' ▼'}
                    </SourceBadge>
                  )}
                </MessageMeta>
                {msg.role === 'assistant' && msg.sources && expandedSources.has(msg.id) && (
                  <SourcesPanel>
                    <SourcesTitle>📖 {t.ragChat.sources}</SourcesTitle>
                    {msg.sources.slice(0, 3).map((source, i) => (
                      <SourceItem key={i}>
                        <SourceText>
                          {source.text.length > 200 ? `${source.text.slice(0, 200)}...` : source.text}
                        </SourceText>
                        <SourceMeta>
                          {source.metadata?.source !== undefined && <span>📄 {String(source.metadata?.source)}</span>}
                          <SourceScore score={source.score}>
                            {t.ragChat.similarity}: {(source.score * 100).toFixed(1)}%
                          </SourceScore>
                        </SourceMeta>
                      </SourceItem>
                    ))}
                  </SourcesPanel>
                )}
              </MessageBubble>
            ))}
            {isLoading && messages[messages.length - 1]?.role === 'assistant' && !messages[messages.length - 1]?.content && (
              <MessageBubble isUser={false}>
                <MessageContent isUser={false}>
                  <Spinner /> {t.ragChat.thinking}
                </MessageContent>
              </MessageBubble>
            )}
            <div ref={messagesEndRef} />
          </>
        )}
      </ChatContainer>

      <InputArea>
        <TextArea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={t.ragChat.inputPlaceholder}
          rows={1}
          disabled={isLoading}
        />
        <SendButton onClick={handleSend} disabled={isLoading || !input.trim()}>
          {isLoading ? <Spinner /> : '→'}
        </SendButton>
      </InputArea>

      {zoomedImage && (
        <ImageZoomModal
          src={zoomedImage.src}
          alt={zoomedImage.alt}
          onClose={() => setZoomedImage(null)}
        />
      )}
    </Container>
  );
}
