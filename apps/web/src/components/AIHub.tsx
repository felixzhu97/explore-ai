import { useState, useRef, useEffect } from 'react';
import styled from '@emotion/styled';
import { css, keyframes } from '@emotion/react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeHighlight from 'rehype-highlight';
import { colors, radius, spacing, typography, shadows, transitions } from '../theme';
import { ImageZoomModal } from './ImageZoomModal';
import { SegmentedControl } from './SegmentedControl';
import { useI18n } from '../i18n';
import {
  chatStream,
  generateImage,
  getVoices,
  synthesizeSpeech,
  downloadBase64Image,
  downloadBlob,
  getProviders,
  getModels,
  ChatMessage,
  Voice,
  ProviderInfo,
  ModelInfo,
} from '../lib/aiServices';

// ==================== Animations ====================

const fadeIn = keyframes`
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
`;

const spin = keyframes`
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
`;

const pulse = keyframes`
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
`;

// ==================== Styled Components ====================

const Container = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${spacing.md};
`;

const TabHeader = styled.div`
  display: flex;
  justify-content: center;
  padding: ${spacing.md} 0;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
  -ms-overflow-style: none;

  &::-webkit-scrollbar {
    display: none;
  }

  @media (max-width: 640px) {
    justify-content: flex-start;
  }
`;

const TabSection = styled.div``;

// Panel Components (matching AI Infrastructure style)
const PanelContainer = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${spacing.md};
`;

const PanelHeader = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: ${spacing.md};
  background: ${colors.surface};
  border: 1px solid ${colors.border};
  border-radius: ${radius.lg};
`;

const PanelTitle = styled.h3`
  font-size: ${typography.fontSize.lg};
  font-weight: ${typography.fontWeight.semibold};
  color: ${colors.text};
  margin: 0;
`;

const PanelDescription = styled.p`
  font-size: ${typography.fontSize.sm};
  color: ${colors.textSecondary};
  margin: 0;
  margin-top: ${spacing.xs};
`;

const PanelContent = styled.div`
  background: ${colors.surface};
  border: 1px solid ${colors.border};
  border-radius: ${radius.lg};
  padding: ${spacing.lg};
`;

// Model selector styles
const ModelSelector = styled.div`
  display: flex;
  gap: ${spacing.md};
  align-items: center;
  flex-wrap: wrap;
  padding: ${spacing.md};
  background: ${colors.surfaceSecondary};
  border-radius: ${radius.md};
  margin-bottom: ${spacing.md};
`;

const ModelSelectorLabel = styled.span`
  font-size: ${typography.fontSize.sm};
  font-weight: ${typography.fontWeight.medium};
  color: ${colors.textSecondary};
`;

const ModelSelect = styled.select`
  padding: ${spacing.sm} ${spacing.md};
  font-size: ${typography.fontSize.sm};
  font-family: ${typography.fontFamily.body};
  border: 1px solid ${colors.border};
  border-radius: ${radius.md};
  background: ${colors.surface};
  color: ${colors.text};
  cursor: pointer;
  min-width: 120px;

  &:focus {
    outline: none;
    border-color: ${colors.primary};
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
`;

const CurrentModelBadge = styled.span`
  font-size: ${typography.fontSize.xs};
  color: ${colors.textTertiary};
  padding: 2px 8px;
  background: ${colors.border};
  border-radius: ${radius.sm};
`;

const ModelLoadingSpinner = styled.span`
  display: inline-block;
  width: 12px;
  height: 12px;
  border: 2px solid ${colors.border};
  border-top-color: ${colors.primary};
  border-radius: 50%;
  animation: spin 0.6s linear infinite;

  @keyframes spin {
    to { transform: rotate(360deg); }
  }
`;

// Chat Components (matching AI Infrastructure style)
const ChatContainer = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${spacing.md};
  max-height: 400px;
  min-height: 200px;
  overflow-y: auto;
  padding: ${spacing.md};
  background: ${colors.surface};
  border-radius: ${radius.lg};
  border: 1px solid ${colors.border};
`;

const EmptyChatState = styled.div`
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

const EmptyTitle = styled.p`
  font-size: ${typography.fontSize.md};
  font-weight: ${typography.fontWeight.medium};
  color: ${colors.text};
  margin: 0;
`;

const EmptySubtitle = styled.p`
  font-size: ${typography.fontSize.sm};
  color: ${colors.textSecondary};
  margin: 0;
`;

const QuickActions = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: ${spacing.sm};
  justify-content: center;
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
  }
`;

const MessageBubble = styled.div<{ isUser: boolean }>`
  display: flex;
  flex-direction: column;
  max-width: 75%;
  animation: ${fadeIn} 0.25s ease;
  align-self: ${({ isUser }) => (isUser ? 'flex-end' : 'flex-start')};
  align-items: ${({ isUser }) => (isUser ? 'flex-end' : 'flex-start')};
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
        `
      : css`
          background: ${colors.surface};
          color: ${colors.text};
          border: 1px solid ${colors.border};
        `}

  /* Markdown styles */
  h1, h2, h3, h4, h5, h6 {
    margin: 0.5em 0 0.25em;
    font-weight: ${typography.fontWeight.semibold};
    &:first-of-type { margin-top: 0; }
    &:last-of-type { margin-bottom: 0; }
  }
  h1 { font-size: 1.15em; }
  h2 { font-size: 1.1em; }
  h3 { font-size: 1em; }

  p {
    margin: 0.5em 0;
    &:first-of-type { margin-top: 0; }
    &:last-of-type { margin-bottom: 0; }
  }

  ul, ol {
    margin: 0.5em 0;
    padding-left: 1.5em;
  }

  li { margin: 0.25em 0; }

  code {
    font-family: ${typography.fontFamily.body};
    font-size: 0.9em;
    padding: 0.15em 0.4em;
    border-radius: ${radius.sm};
    background: ${colors.border};
  }

  pre {
    margin: 0.5em 0;
    padding: ${spacing.md};
    border-radius: ${radius.md};
    background: ${colors.border};
    overflow-x: auto;
    code { padding: 0; background: none; }
  }

  blockquote {
    margin: 0.5em 0;
    padding-left: 1em;
    border-left: 3px solid ${colors.primary};
    color: ${colors.textSecondary};
  }

  a {
    color: ${({ isUser }) => (isUser ? '#ffffff' : colors.primary)};
    text-decoration: none;
    &:hover { text-decoration: underline; }
  }
`;

const MessageTime = styled.span`
  font-size: ${typography.fontSize.xs};
  color: ${colors.textTertiary};
  margin-top: ${spacing.xs};
  padding: 0 ${spacing.xs};
`;

const InputArea = styled.div`
  display: flex;
  gap: ${spacing.sm};
  align-items: flex-end;
  margin-top: ${spacing.md};
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

  svg {
    width: 20px;
    height: 20px;
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

const ErrorMessage = styled.div`
  padding: ${spacing.md};
  background: ${colors.errorLight};
  color: ${colors.error};
  border-radius: ${radius.md};
  font-size: ${typography.fontSize.sm};
  animation: ${fadeIn} 0.2s ease;
  border: 1px solid ${colors.error}20;
`;

// Image Generation Components (matching AI Infrastructure style)
const ImageSection = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: ${spacing.lg};

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
`;

const PromptArea = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${spacing.md};
`;

const InputGroup = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${spacing.xs};
`;

const Label = styled.label`
  font-size: ${typography.fontSize.sm};
  font-weight: ${typography.fontWeight.medium};
  color: ${colors.textSecondary};
`;

const Input = styled.input`
  padding: ${spacing.md};
  font-size: ${typography.fontSize.base};
  font-family: ${typography.fontFamily.body};
  border: 1px solid ${colors.border};
  border-radius: ${radius.md};
  background: ${colors.surface};
  color: ${colors.text};
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

const TextAreaStyled = styled.textarea`
  padding: ${spacing.md};
  font-size: ${typography.fontSize.base};
  font-family: ${typography.fontFamily.body};
  border: 1px solid ${colors.border};
  border-radius: ${radius.md};
  background: ${colors.surface};
  color: ${colors.text};
  resize: vertical;
  min-height: 80px;
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

const Select = styled.select`
  padding: ${spacing.md};
  font-size: ${typography.fontSize.base};
  font-family: ${typography.fontFamily.body};
  border: 1px solid ${colors.border};
  border-radius: ${radius.md};
  background: ${colors.surface};
  color: ${colors.text};
  cursor: pointer;
  transition: border-color ${transitions.fast};

  &:focus {
    outline: none;
    border-color: ${colors.primary};
  }
`;

const SizeSelector = styled.div`
  display: flex;
  gap: ${spacing.sm};
  flex-wrap: wrap;
`;

const SizeOption = styled.button<{ selected?: boolean }>`
  padding: 8px 16px;
  font-size: ${typography.fontSize.sm};
  font-weight: ${typography.fontWeight.medium};
  background: ${({ selected }) => (selected ? colors.primary : colors.surface)};
  color: ${({ selected }) => (selected ? 'white' : colors.text)};
  border: 1px solid ${({ selected }) => (selected ? 'transparent' : colors.border)};
  border-radius: ${radius.md};
  cursor: pointer;
  transition: all ${transitions.fast};

  &:hover {
    border-color: ${colors.primary};
  }
`;

const ActionButton = styled.button<{ primary?: boolean; disabled?: boolean }>`
  padding: ${spacing.md} ${spacing.lg};
  font-size: ${typography.fontSize.base};
  font-weight: ${typography.fontWeight.medium};
  font-family: ${typography.fontFamily.body};
  background: ${({ primary, disabled }) => (primary ? (disabled ? colors.border : colors.primary) : colors.surface)};
  color: ${({ primary, disabled }) => (primary ? 'white' : colors.text)};
  border: 1px solid ${({ primary, disabled }) => (primary ? 'transparent' : (disabled ? colors.border : colors.border))};
  border-radius: ${radius.md};
  cursor: ${({ disabled }) => (disabled ? 'not-allowed' : 'pointer')};
  opacity: ${({ disabled }) => (disabled ? 0.6 : 1)};
  transition: all ${transitions.fast};
  display: flex;
  align-items: center;
  justify-content: center;
  gap: ${spacing.sm};

  &:hover:not(:disabled) {
    background: ${({ primary }) => (primary ? colors.primaryHover : colors.surface)};
  }
`;

const ImagePreviewArea = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${spacing.md};
  min-height: 300px;
`;

const ImageArea = styled.div`
  flex: 1;
  position: relative;
  border-radius: ${radius.lg};
  background: ${colors.surface};
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  min-height: 256px;
  border: 2px dashed ${colors.border};
`;

const GeneratedImage = styled.img`
  max-width: 100%;
  max-height: 400px;
  object-fit: contain;
  border-radius: ${radius.md};
  cursor: zoom-in;
  transition: transform ${transitions.fast};
  animation: ${fadeIn} 0.3s ease;

  &:hover {
    transform: scale(1.02);
  }
`;

const LoadingOverlay = styled.div`
  position: absolute;
  inset: 0;
  background: ${colors.glass};
  backdrop-filter: blur(8px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: ${spacing.md};
  border-radius: ${radius.lg};
  z-index: 5;
`;

const LoadingSpinner = styled.div`
  width: 44px;
  height: 44px;
  border: 3px solid ${colors.primaryLight};
  border-top-color: ${colors.primary};
  border-radius: 50%;
  animation: ${spin} 0.8s linear infinite;
`;

const LoadingText = styled.span`
  font-size: ${typography.fontSize.sm};
  font-weight: ${typography.fontWeight.medium};
  color: ${colors.textSecondary};
`;

const ImageActions = styled.div`
  display: flex;
  gap: ${spacing.sm};
  justify-content: center;
`;

const IconButton = styled.button`
  padding: ${spacing.sm} ${spacing.md};
  font-size: ${typography.fontSize.sm};
  font-weight: ${typography.fontWeight.medium};
  background: ${colors.surface};
  color: ${colors.primary};
  border: 1px solid ${colors.border};
  border-radius: ${radius.md};
  cursor: pointer;
  transition: all ${transitions.fast};
  display: flex;
  align-items: center;
  gap: ${spacing.xs};

  &:hover {
    background: ${colors.primaryLight};
    border-color: ${colors.primary};
  }
`;

// TTS Components (matching AI Infrastructure style)
const TTSSection = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${spacing.lg};
`;

const ControlRow = styled.div`
  display: flex;
  gap: ${spacing.lg};
  align-items: flex-end;
  flex-wrap: wrap;

  @media (max-width: 640px) {
    flex-direction: column;
  }
`;

const SliderContainer = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${spacing.sm};
  flex: 1;
  min-width: 150px;
`;

const Slider = styled.input`
  width: 100%;
  height: 4px;
  background: ${colors.border};
  border-radius: 2px;
  outline: none;
  -webkit-appearance: none;

  &::-webkit-slider-thumb {
    -webkit-appearance: none;
    width: 18px;
    height: 18px;
    background: ${colors.primary};
    border-radius: 50%;
    cursor: pointer;
    transition: transform ${transitions.fast};
  }

  &::-webkit-slider-thumb:hover {
    transform: scale(1.15);
  }
`;

const SliderValue = styled.span`
  font-size: ${typography.fontSize.sm};
  font-weight: ${typography.fontWeight.medium};
  color: ${colors.textSecondary};
  text-align: center;
`;

const AudioPlayer = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${spacing.md};
  padding: ${spacing.lg};
  background: ${colors.primaryLight};
  border: 1px solid ${colors.primary}20;
  border-radius: ${radius.lg};
  animation: ${fadeIn} 0.3s ease;
`;

const AudioControls = styled.div`
  display: flex;
  align-items: center;
  gap: ${spacing.md};
`;

const PlayButton = styled.button<{ isPlaying?: boolean }>`
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: ${({ isPlaying }) => (isPlaying ? colors.success : colors.primary)};
  color: white;
  border: none;
  border-radius: ${radius.lg};
  cursor: pointer;
  transition: all ${transitions.default};
  font-size: 18px;

  &:hover {
    opacity: 0.9;
    transform: scale(1.02);
  }

  &:active {
    transform: scale(0.95);
  }
`;

const AudioInfo = styled.div`
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: ${spacing.sm};
`;

const AudioLabel = styled.span`
  font-size: ${typography.fontSize.base};
  font-weight: ${typography.fontWeight.medium};
  color: ${colors.text};
`;

const AudioBar = styled.div`
  height: 6px;
  background: ${colors.border};
  border-radius: 3px;
  overflow: hidden;
`;

const AudioProgress = styled.div<{ progress: number }>`
  height: 100%;
  width: ${({ progress }) => progress}%;
  background: ${colors.primary};
  transition: width 0.1s linear;
  border-radius: 3px;
`;

const DownloadLink = styled.button`
  padding: ${spacing.sm} ${spacing.md};
  font-size: ${typography.fontSize.sm};
  font-weight: ${typography.fontWeight.medium};
  background: ${colors.surface};
  color: ${colors.primary};
  border: 1px solid ${colors.border};
  border-radius: ${radius.md};
  cursor: pointer;
  transition: all ${transitions.fast};
  display: flex;
  align-items: center;
  gap: ${spacing.xs};
  width: fit-content;

  &:hover {
    background: ${colors.primaryLight};
    border-color: ${colors.primary};
  }
`;

// ==================== Types ====================

type Tab = 'chat' | 'image' | 'tts';

interface ChatMessageData {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: number;
}

const IMAGE_SIZES = [
  { label: '512x512', width: 512, height: 512 },
  { label: '768x768', width: 768, height: 768 },
  { label: '1024x1024', width: 1024, height: 1024 },
];

// ==================== Component ====================

export function AIHub() {
  const { t } = useI18n();
  const [activeTab, setActiveTab] = useState<Tab>('chat');
  const [sessionId] = useState(() => `aihub_${Date.now()}`);

  // Chat state
  const [chatMessages, setChatMessages] = useState<ChatMessageData[]>([]);
  const [chatInput, setChatInput] = useState('');
  const [isChatLoading, setIsChatLoading] = useState(false);
  const [chatError, setChatError] = useState<string | null>(null);
  const chatAbortRef = useRef<AbortController | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Model selection state
  const [providers, setProviders] = useState<ProviderInfo[]>([]);
  const [models, setModels] = useState<ModelInfo[]>([]);
  const [selectedProvider, setSelectedProvider] = useState<string>('');
  const [selectedModel, setSelectedModel] = useState<string>('');
  const [isLoadingModels, setIsLoadingModels] = useState(false);

  // Image generation state
  const [imagePrompt, setImagePrompt] = useState('');
  const [imageNegativePrompt, setImageNegativePrompt] = useState('');
  const [imageSize, setImageSize] = useState(IMAGE_SIZES[2]);
  const [generatedImage, setGeneratedImage] = useState<string | null>(null);
  const [isGeneratingImage, setIsGeneratingImage] = useState(false);
  const [imageError, setImageError] = useState<string | null>(null);
  const [zoomedImage, setZoomedImage] = useState<string | null>(null);

  // TTS state
  const [ttsText, setTtsText] = useState('');
  const [ttsVoice, setTtsVoice] = useState<string>('en-US');
  const [ttsSpeed, setTtsSpeed] = useState(1.0);
  const [availableVoices, setAvailableVoices] = useState<Voice[]>([]);
  const [isSynthesizing, setIsSynthesizing] = useState(false);
  const [ttsError, setTtsError] = useState<string | null>(null);
  const [audioUrl, setAudioUrl] = useState<string | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [audioProgress, setAudioProgress] = useState(0);
  const audioRef = useRef<HTMLAudioElement | null>(null);

  // Scroll to bottom of chat
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [chatMessages]);

  // Load voices on mount
  useEffect(() => {
    loadVoices();
    loadProviders();
  }, []);

  const loadProviders = async () => {
    // Default providers when API is unavailable
    const defaultProviders: ProviderInfo[] = [
      { name: 'openai', display_name: 'OpenAI', models: ['gpt-4o', 'gpt-4o-mini', 'gpt-4-turbo', 'gpt-3.5-turbo'], status: 'available' },
      { name: 'anthropic', display_name: 'Anthropic Claude', models: ['claude-sonnet-4-20250514', 'claude-opus-4-20250514', 'claude-3-5-sonnet-20241022'], status: 'available' },
      { name: 'ollama', display_name: 'Ollama (Local)', models: ['qwen2.5:7b', 'qwen2.5:14b', 'llama3.2:3b', 'llama3.1:8b', 'mistral:7b'], status: 'available' },
    ];

    try {
      const data = await getProviders();
      setProviders(data);
      if (data.length > 0) {
        setSelectedProvider(data[0].name);
        loadModelsForProvider(data[0].name);
      }
    } catch (error) {
      console.warn('Failed to load providers from API, using defaults:', error);
      setProviders(defaultProviders);
      setSelectedProvider('openai');
      setModels(defaultProviders[0].models.map(name => ({ name, provider: 'openai' })));
    }
  };

  const loadModelsForProvider = async (provider: string) => {
    setIsLoadingModels(true);
    try {
      const data = await getModels(provider);
      setModels(data);
      if (data.length > 0) {
        const defaultModel = data.find(m => m.name.includes('mini') || m.name.includes('3.5')) || data[0];
        setSelectedModel(defaultModel.name);
      }
    } catch (error) {
      console.warn('Failed to load models from API, using defaults:', error);
      const providerData = {
        openai: ['gpt-4o', 'gpt-4o-mini', 'gpt-4-turbo', 'gpt-3.5-turbo'],
        anthropic: ['claude-sonnet-4-20250514', 'claude-opus-4-20250514', 'claude-3-5-sonnet-20241022'],
        ollama: ['qwen2.5:7b', 'qwen2.5:14b', 'llama3.2:3b', 'llama3.1:8b', 'mistral:7b'],
      };
      const modelList = providerData[provider as keyof typeof providerData] || providerData.openai;
      setModels(modelList.map(name => ({ name, provider })));
      setSelectedModel(modelList[0]);
    } finally {
      setIsLoadingModels(false);
    }
  };

  const handleProviderChange = (provider: string) => {
    setSelectedProvider(provider);
    loadModelsForProvider(provider);
  };

  // Cleanup audio URL
  useEffect(() => {
    return () => {
      if (audioUrl) {
        URL.revokeObjectURL(audioUrl);
      }
    };
  }, [audioUrl]);

  const loadVoices = async () => {
    try {
      const voices = await getVoices();
      setAvailableVoices(voices);
      // Set default voice
      const defaultVoice = voices.find((v) => v.is_default) || voices[0];
      if (defaultVoice) {
        setTtsVoice(defaultVoice.id);
      }
    } catch (error) {
      console.error('Failed to load voices:', error);
    }
  };

  // Chat handlers
  const handleSendMessage = async () => {
    if (!chatInput.trim() || isChatLoading) return;

    // Cancel any existing request
    if (chatAbortRef.current) {
      chatAbortRef.current.abort();
    }
    chatAbortRef.current = new AbortController();

    const userMessage: ChatMessageData = {
      id: `user_${Date.now()}`,
      role: 'user',
      content: chatInput.trim(),
      timestamp: Date.now(),
    };

    setChatMessages((prev) => [...prev, userMessage]);
    setChatInput('');
    setIsChatLoading(true);
    setChatError(null);

    const assistantMessageId = `assistant_${Date.now()}`;
    setChatMessages((prev) => [
      ...prev,
      { id: assistantMessageId, role: 'assistant', content: '', timestamp: Date.now() },
    ]);

    try {
      const messages: ChatMessage[] = [
        ...chatMessages.map((m) => ({ role: m.role, content: m.content })),
        { role: 'user', content: userMessage.content },
      ];

      let fullContent = '';

      await chatStream(
        { messages, session_id: sessionId, provider: selectedProvider, model: selectedModel },
        (chunk) => {
          fullContent += chunk;
          setChatMessages((prev) =>
            prev.map((msg) =>
              msg.id === assistantMessageId ? { ...msg, content: fullContent } : msg
            )
          );
        },
        () => {},
        (error) => {
          let msg = error.message;
          if (msg.includes('Failed to fetch') || msg.includes('NetworkError') || msg.includes('ERR_CONNECTION_REFUSED')) {
            msg = `Text Service 不可用。请确保服务在 ${import.meta.env.VITE_TEXT_SERVICE_URL || 'http://localhost:8006'} 运行。`;
          }
          setChatError(msg);
          setChatMessages((prev) =>
            prev.map((m) =>
              m.id === assistantMessageId ? { ...m, content: msg } : m
            )
          );
        }
      );
    } catch (error) {
      if (error instanceof Error && error.name === 'AbortError') {
        return;
      }
      const errorMessage = error instanceof Error ? error.message : 'Chat failed';
      let userMessage = errorMessage;
      if (errorMessage.includes('Failed to fetch') || errorMessage.includes('NetworkError') || errorMessage.includes('ERR_CONNECTION_REFUSED')) {
        userMessage = `Text Service 不可用。请确保服务在 ${import.meta.env.VITE_TEXT_SERVICE_URL || 'http://localhost:8006'} 运行。`;
      }
      setChatError(userMessage);
      setChatMessages((prev) =>
        prev.map((msg) =>
          msg.id === assistantMessageId ? { ...msg, content: userMessage } : msg
        )
      );
    } finally {
      setIsChatLoading(false);
      chatAbortRef.current = null;
    }
  };

  const handleChatKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  // Image generation handlers
  const handleGenerateImage = async () => {
    if (!imagePrompt.trim() || isGeneratingImage) return;

    setIsGeneratingImage(true);
    setImageError(null);
    setGeneratedImage(null);

    try {
      const result = await generateImage({
        prompt: imagePrompt,
        negative_prompt: imageNegativePrompt || undefined,
        width: imageSize.width,
        height: imageSize.height,
        num_images: 1,
      });

      if (result.images && result.images.length > 0) {
        setGeneratedImage(result.images[0]);
      }
    } catch (error) {
      setImageError(error instanceof Error ? error.message : 'Image generation failed');
    } finally {
      setIsGeneratingImage(false);
    }
  };

  const handleDownloadImage = () => {
    if (generatedImage) {
      downloadBase64Image(generatedImage, `ai_generated_${Date.now()}.png`);
    }
  };

  // TTS handlers
  const handleSynthesize = async () => {
    if (!ttsText.trim() || isSynthesizing) return;

    setIsSynthesizing(true);
    setTtsError(null);

    try {
      const blob = await synthesizeSpeech({
        text: ttsText,
        voice: ttsVoice || undefined,
        speed: ttsSpeed,
        output_format: 'mp3',
      });

      // Clean up previous audio URL
      if (audioUrl) {
        URL.revokeObjectURL(audioUrl);
      }

      const url = URL.createObjectURL(blob);
      setAudioUrl(url);

      // Set up audio element
      audioRef.current = new Audio(url);
      audioRef.current.addEventListener('ended', () => setIsPlaying(false));
      audioRef.current.addEventListener('timeupdate', () => {
        if (audioRef.current) {
          setAudioProgress(
            (audioRef.current.currentTime / audioRef.current.duration) * 100
          );
        }
      });
    } catch (error) {
      setTtsError(error instanceof Error ? error.message : 'Synthesis failed');
    } finally {
      setIsSynthesizing(false);
    }
  };

  const handlePlayPause = () => {
    if (!audioRef.current) return;

    if (isPlaying) {
      audioRef.current.pause();
      setIsPlaying(false);
    } else {
      audioRef.current.play();
      setIsPlaying(true);
    }
  };

  const handleDownloadAudio = () => {
    if (audioUrl) {
      fetch(audioUrl)
        .then((res) => res.blob())
        .then((blob) => downloadBlob(blob, `speech_${Date.now()}.mp3`));
    }
  };

  // Quick prompts
  const quickPrompts = [
    t.aiHub.quickPrompts.greeting,
    t.aiHub.quickPrompts.help,
    t.aiHub.quickPrompts.creative,
  ];

  const tabOptions: { value: Tab; label: string }[] = [
    { value: 'chat', label: t.aiHub.tabs.chat },
    { value: 'image', label: t.aiHub.tabs.image },
    { value: 'tts', label: t.aiHub.tabs.tts },
  ];

  return (
    <Container>
      {/* Tab Navigation */}
      <TabHeader>
        <SegmentedControl
          options={tabOptions}
          value={activeTab}
          onChange={setActiveTab}
        />
      </TabHeader>

      <TabSection key={activeTab} css={{ animation: `${fadeIn} 0.3s ease` }}>
        {/* Chat Tab */}
        {activeTab === 'chat' && (
          <PanelContainer>
            <PanelHeader>
              <div>
                <PanelTitle>{t.aiHub.chat.title}</PanelTitle>
                <PanelDescription>{t.aiHub.chat.description}</PanelDescription>
              </div>
            </PanelHeader>
            <PanelContent>
              {providers.length > 0 && (
                <ModelSelector>
                  <ModelSelectorLabel>Provider:</ModelSelectorLabel>
                  <ModelSelect
                    value={selectedProvider}
                    onChange={(e) => handleProviderChange(e.target.value)}
                  >
                    {providers.map((p) => (
                      <option key={p.name} value={p.name}>
                        {p.display_name}
                      </option>
                    ))}
                  </ModelSelect>

                  <ModelSelectorLabel>Model:</ModelSelectorLabel>
                  <ModelSelect
                    value={selectedModel}
                    onChange={(e) => setSelectedModel(e.target.value)}
                    disabled={isLoadingModels}
                  >
                    {isLoadingModels ? (
                      <option>Loading...</option>
                    ) : (
                      models.map((m) => (
                        <option key={m.name} value={m.name}>
                          {m.name}
                        </option>
                      ))
                    )}
                  </ModelSelect>

                  {selectedModel && (
                    <CurrentModelBadge>
                      {selectedProvider}/{selectedModel}
                    </CurrentModelBadge>
                  )}
                </ModelSelector>
              )}
              <ChatContainer>
                {chatMessages.length === 0 ? (
                  <EmptyChatState>
                    <EmptyIcon>
                      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                      </svg>
                    </EmptyIcon>
                    <EmptyTitle>{t.aiHub.chat.placeholder}</EmptyTitle>
                    <EmptySubtitle>Choose a prompt below to get started</EmptySubtitle>
                    <QuickActions>
                      {quickPrompts.map((prompt, i) => (
                        <QuickAction key={i} onClick={() => setChatInput(prompt)}>
                          {prompt}
                        </QuickAction>
                      ))}
                    </QuickActions>
                  </EmptyChatState>
                ) : (
                  <>
                    {chatMessages.map((msg) => (
                      <MessageBubble key={msg.id} isUser={msg.role === 'user'}>
                        <MessageContent isUser={msg.role === 'user'}>
                          {msg.role === 'user' ? (
                            msg.content
                          ) : (
                            <ReactMarkdown
                              remarkPlugins={[remarkGfm]}
                              rehypePlugins={[rehypeHighlight]}
                            >
                              {msg.content}
                            </ReactMarkdown>
                          )}
                        </MessageContent>
                        <MessageTime>
                          {new Date(msg.timestamp).toLocaleTimeString()}
                        </MessageTime>
                      </MessageBubble>
                    ))}
                    {isChatLoading && chatMessages[chatMessages.length - 1]?.content === '' && (
                      <MessageBubble isUser={false}>
                        <MessageContent isUser={false}>
                          <Spinner /> {t.aiHub.chat.thinking}
                        </MessageContent>
                      </MessageBubble>
                    )}
                    <div ref={messagesEndRef} />
                  </>
                )}
              </ChatContainer>
              {chatError && <ErrorMessage>{chatError}</ErrorMessage>}
              <InputArea>
                <TextArea
                  value={chatInput}
                  onChange={(e) => setChatInput(e.target.value)}
                  onKeyDown={handleChatKeyDown}
                  placeholder={t.aiHub.chat.inputPlaceholder}
                  rows={1}
                  disabled={isChatLoading}
                />
                <SendButton onClick={handleSendMessage} disabled={isChatLoading || !chatInput.trim()}>
                  →
                </SendButton>
              </InputArea>
            </PanelContent>
          </PanelContainer>
        )}

        {/* Image Generation Tab */}
        {activeTab === 'image' && (
          <PanelContainer>
            <PanelHeader>
              <div>
                <PanelTitle>{t.aiHub.image.title}</PanelTitle>
                <PanelDescription>{t.aiHub.image.description}</PanelDescription>
              </div>
            </PanelHeader>
            <PanelContent>
              <ImageSection>
                <PromptArea>
                  <InputGroup>
                    <Label>{t.aiHub.image.promptLabel}</Label>
                    <TextAreaStyled
                      value={imagePrompt}
                      onChange={(e) => setImagePrompt(e.target.value)}
                      placeholder={t.aiHub.image.promptPlaceholder}
                      rows={4}
                    />
                  </InputGroup>

                  <InputGroup>
                    <Label>{t.aiHub.image.negativePromptLabel}</Label>
                    <TextAreaStyled
                      value={imageNegativePrompt}
                      onChange={(e) => setImageNegativePrompt(e.target.value)}
                      placeholder={t.aiHub.image.negativePromptPlaceholder}
                      rows={2}
                    />
                  </InputGroup>

                  <InputGroup>
                    <Label>{t.aiHub.image.sizeLabel}</Label>
                    <SizeSelector>
                      {IMAGE_SIZES.map((size) => (
                        <SizeOption
                          key={size.label}
                          selected={imageSize.label === size.label}
                          onClick={() => setImageSize(size)}
                        >
                          {size.label}
                        </SizeOption>
                      ))}
                    </SizeSelector>
                  </InputGroup>

                  <ActionButton
                    primary
                    disabled={!imagePrompt.trim() || isGeneratingImage}
                    onClick={handleGenerateImage}
                  >
                    {isGeneratingImage ? (
                      <>
                        <Spinner /> {t.aiHub.image.generating}
                      </>
                    ) : (
                      t.aiHub.image.generateButton
                    )}
                  </ActionButton>
                </PromptArea>

                <ImagePreviewArea>
                  <ImageArea>
                    {isGeneratingImage && (
                      <LoadingOverlay>
                        <LoadingSpinner />
                        <LoadingText>{t.aiHub.image.generating}</LoadingText>
                      </LoadingOverlay>
                    )}
                    {generatedImage ? (
                      <GeneratedImage
                        src={`data:image/png;base64,${generatedImage}`}
                        alt="Generated"
                        onClick={() => setZoomedImage(`data:image/png;base64,${generatedImage}`)}
                      />
                    ) : (
                      <EmptyChatState>
                        <EmptyIcon>
                          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                            <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                            <circle cx="8.5" cy="8.5" r="1.5"/>
                            <polyline points="21 15 16 10 5 21"/>
                          </svg>
                        </EmptyIcon>
                        <EmptyTitle>{t.aiHub.image.emptyState}</EmptyTitle>
                      </EmptyChatState>
                    )}
                  </ImageArea>
                  {imageError && <ErrorMessage>{imageError}</ErrorMessage>}
                  {generatedImage && (
                    <ImageActions>
                      <IconButton onClick={handleDownloadImage}>
                        ⬇️ {t.aiHub.image.download}
                      </IconButton>
                    </ImageActions>
                  )}
                </ImagePreviewArea>
              </ImageSection>
            </PanelContent>
          </PanelContainer>
        )}

        {/* TTS Tab */}
        {activeTab === 'tts' && (
          <PanelContainer>
            <PanelHeader>
              <div>
                <PanelTitle>{t.aiHub.tts.title}</PanelTitle>
                <PanelDescription>{t.aiHub.tts.description}</PanelDescription>
              </div>
            </PanelHeader>
            <PanelContent>
              <TTSSection>
                <InputGroup>
                  <Label>{t.aiHub.tts.textLabel}</Label>
                  <TextAreaStyled
                    value={ttsText}
                    onChange={(e) => setTtsText(e.target.value)}
                    placeholder={t.aiHub.tts.textPlaceholder}
                    rows={4}
                  />
                </InputGroup>

                <ControlRow>
                  <InputGroup>
                    <Label>{t.aiHub.tts.voiceLabel}</Label>
                    <Select
                      value={ttsVoice}
                      onChange={(e) => setTtsVoice(e.target.value)}
                      style={{ minWidth: 200 }}
                    >
                      {availableVoices.length > 0 ? (
                        availableVoices.map((voice) => (
                          <option key={voice.id} value={voice.id}>
                            {voice.name} ({voice.language})
                          </option>
                        ))
                      ) : (
                        <>
                          <option value="en-US">English (US)</option>
                          <option value="en-GB">English (UK)</option>
                          <option value="zh-CN">中文</option>
                          <option value="ja-JP">日本語</option>
                        </>
                      )}
                    </Select>
                  </InputGroup>

                  <SliderContainer>
                    <Label>{t.aiHub.tts.speedLabel}: {ttsSpeed.toFixed(1)}x</Label>
                    <Slider
                      type="range"
                      min="0.5"
                      max="2.0"
                      step="0.1"
                      value={ttsSpeed}
                      onChange={(e) => setTtsSpeed(parseFloat(e.target.value))}
                    />
                  </SliderContainer>
                </ControlRow>

                <ActionButton
                  primary
                  disabled={!ttsText.trim() || isSynthesizing}
                  onClick={handleSynthesize}
                >
                  {isSynthesizing ? (
                    <>
                      <Spinner /> {t.aiHub.tts.synthesizing}
                    </>
                  ) : (
                    t.aiHub.tts.synthesizeButton
                  )}
                </ActionButton>

                {ttsError && <ErrorMessage>{ttsError}</ErrorMessage>}

                {audioUrl && (
                  <AudioPlayer>
                    <AudioControls>
                      <PlayButton isPlaying={isPlaying} onClick={handlePlayPause}>
                        {isPlaying ? '⏸' : '▶'}
                      </PlayButton>
                      <AudioInfo>
                        <AudioLabel>{t.aiHub.tts.audioReady}</AudioLabel>
                        <AudioBar>
                          <AudioProgress progress={audioProgress} />
                        </AudioBar>
                      </AudioInfo>
                    </AudioControls>
                    <DownloadLink onClick={handleDownloadAudio}>
                      ⬇️ {t.aiHub.tts.downloadAudio}
                    </DownloadLink>
                  </AudioPlayer>
                )}
              </TTSSection>
            </PanelContent>
          </PanelContainer>
        )}
      </TabSection>

      {zoomedImage && (
        <ImageZoomModal
          src={zoomedImage}
          alt="Generated image"
          onClose={() => setZoomedImage(null)}
        />
      )}
    </Container>
  );
}
