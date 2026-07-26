import type { Language } from '../core/i18n/translations.types';

/** Page-only copy — kept out of the global i18n bundle (lazy with /privacy). */
export interface PrivacyPageCopy {
  title: string;
  subtitle: string;
  noticeHeading: string;
  noticeIdentity: string;
  noticeChat: string;
  noticeRetention: string;
  processorsHeading: string;
  analyticsHeading: string;
  analyticsHelp: string;
  analyticsLabel: string;
  controlsHeading: string;
  controlsHelp: string;
  eraseButton: string;
  resetButton: string;
  eraseConfirm: string;
  resetConfirm: string;
  eraseSuccess: string;
  eraseFailed: string;
  resetSuccess: string;
  resetFailed: string;
  backToChat: string;
}

export const PRIVACY_PAGE_COPY: Record<Language, PrivacyPageCopy> = {
  en: {
    title: 'Privacy',
    subtitle: 'How ExploreAI handles anonymous browser data',
    noticeHeading: 'Notice',
    noticeIdentity:
      'A functional HttpOnly cookie (Client Identity) scopes chat sessions to this browser. It is required for session isolation and is not used for advertising.',
    noticeChat:
      'Chat messages are sent to language-model and optional search providers to generate replies. Do not submit sensitive personal data you do not want processed.',
    noticeRetention:
      'Inactive chat sessions and related metrics events are purged after about 90 days. You can erase this browser’s sessions anytime below.',
    processorsHeading: 'Sub-processors',
    analyticsHeading: 'Analytics preference',
    analyticsHelp:
      'Optional Datadog RUM and LaunchDarkly load only when you allow analytics. Feature modules still work with local fallbacks when analytics is off.',
    analyticsLabel: 'Allow analytics & feature telemetry',
    controlsHeading: 'Your controls',
    controlsHelp:
      'Erase removes chat sessions and linked metrics for this browser identity. Reset identity issues a new cookie and hides prior sessions.',
    eraseButton: 'Erase my chat data',
    resetButton: 'Reset browser identity',
    eraseConfirm: 'Delete all chat sessions for this browser?',
    resetConfirm: 'Reset identity cookie and clear local session list?',
    eraseSuccess: 'Chat data erased',
    eraseFailed: 'Failed to erase chat data',
    resetSuccess: 'Browser identity reset',
    resetFailed: 'Failed to reset identity',
    backToChat: 'Back to chat',
  },
  zh: {
    title: '隐私',
    subtitle: 'ExploreAI 如何处理本浏览器的匿名数据',
    noticeHeading: '告知',
    noticeIdentity:
      '功能性 HttpOnly Cookie（客户端身份）用于将会话限定在本浏览器，服务于会话隔离，不用于广告。',
    noticeChat:
      '聊天内容会发送至大模型及可选的搜索服务以生成回复。请勿提交您不希望被处理的敏感个人信息。',
    noticeRetention:
      '不活跃会话及相关指标事件约 90 天后清理。您也可随时在下方擦除本浏览器会话。',
    processorsHeading: '子处理方',
    analyticsHeading: '分析偏好',
    analyticsHelp:
      '仅在您允许分析时加载可选的 Datadog RUM 与 LaunchDarkly。关闭分析时功能模块仍使用本地回退配置。',
    analyticsLabel: '允许分析与功能遥测',
    controlsHeading: '您的控制',
    controlsHelp:
      '擦除会删除本浏览器身份下的聊天会话及关联指标。重置身份会签发新 Cookie 并隐藏旧会话。',
    eraseButton: '擦除我的聊天数据',
    resetButton: '重置浏览器身份',
    eraseConfirm: '删除本浏览器的全部聊天会话？',
    resetConfirm: '重置身份 Cookie 并清空本地会话列表？',
    eraseSuccess: '聊天数据已擦除',
    eraseFailed: '擦除失败',
    resetSuccess: '浏览器身份已重置',
    resetFailed: '重置失败',
    backToChat: '返回聊天',
  },
  ja: {
    title: 'Privacy',
    subtitle: 'How ExploreAI handles anonymous browser data',
    noticeHeading: 'Notice',
    noticeIdentity:
      'A functional HttpOnly cookie (Client Identity) scopes chat sessions to this browser. It is required for session isolation and is not used for advertising.',
    noticeChat:
      'Chat messages are sent to language-model and optional search providers to generate replies. Do not submit sensitive personal data you do not want processed.',
    noticeRetention:
      'Inactive chat sessions and related metrics events are purged after about 90 days. You can erase this browser’s sessions anytime below.',
    processorsHeading: 'Sub-processors',
    analyticsHeading: 'Analytics preference',
    analyticsHelp:
      'Optional Datadog RUM and LaunchDarkly load only when you allow analytics. Feature modules still work with local fallbacks when analytics is off.',
    analyticsLabel: 'Allow analytics & feature telemetry',
    controlsHeading: 'Your controls',
    controlsHelp:
      'Erase removes chat sessions and linked metrics for this browser identity. Reset identity issues a new cookie and hides prior sessions.',
    eraseButton: 'Erase my chat data',
    resetButton: 'Reset browser identity',
    eraseConfirm: 'Delete all chat sessions for this browser?',
    resetConfirm: 'Reset identity cookie and clear local session list?',
    eraseSuccess: 'Chat data erased',
    eraseFailed: 'Failed to erase chat data',
    resetSuccess: 'Browser identity reset',
    resetFailed: 'Failed to reset identity',
    backToChat: 'Back to chat',
  },
  fr: {
    title: 'Privacy',
    subtitle: 'How ExploreAI handles anonymous browser data',
    noticeHeading: 'Notice',
    noticeIdentity:
      'A functional HttpOnly cookie (Client Identity) scopes chat sessions to this browser. It is required for session isolation and is not used for advertising.',
    noticeChat:
      'Chat messages are sent to language-model and optional search providers to generate replies. Do not submit sensitive personal data you do not want processed.',
    noticeRetention:
      'Inactive chat sessions and related metrics events are purged after about 90 days. You can erase this browser’s sessions anytime below.',
    processorsHeading: 'Sub-processors',
    analyticsHeading: 'Analytics preference',
    analyticsHelp:
      'Optional Datadog RUM and LaunchDarkly load only when you allow analytics. Feature modules still work with local fallbacks when analytics is off.',
    analyticsLabel: 'Allow analytics & feature telemetry',
    controlsHeading: 'Your controls',
    controlsHelp:
      'Erase removes chat sessions and linked metrics for this browser identity. Reset identity issues a new cookie and hides prior sessions.',
    eraseButton: 'Erase my chat data',
    resetButton: 'Reset browser identity',
    eraseConfirm: 'Delete all chat sessions for this browser?',
    resetConfirm: 'Reset identity cookie and clear local session list?',
    eraseSuccess: 'Chat data erased',
    eraseFailed: 'Failed to erase chat data',
    resetSuccess: 'Browser identity reset',
    resetFailed: 'Failed to reset identity',
    backToChat: 'Back to chat',
  },
  es: {
    title: 'Privacy',
    subtitle: 'How ExploreAI handles anonymous browser data',
    noticeHeading: 'Notice',
    noticeIdentity:
      'A functional HttpOnly cookie (Client Identity) scopes chat sessions to this browser. It is required for session isolation and is not used for advertising.',
    noticeChat:
      'Chat messages are sent to language-model and optional search providers to generate replies. Do not submit sensitive personal data you do not want processed.',
    noticeRetention:
      'Inactive chat sessions and related metrics events are purged after about 90 days. You can erase this browser’s sessions anytime below.',
    processorsHeading: 'Sub-processors',
    analyticsHeading: 'Analytics preference',
    analyticsHelp:
      'Optional Datadog RUM and LaunchDarkly load only when you allow analytics. Feature modules still work with local fallbacks when analytics is off.',
    analyticsLabel: 'Allow analytics & feature telemetry',
    controlsHeading: 'Your controls',
    controlsHelp:
      'Erase removes chat sessions and linked metrics for this browser identity. Reset identity issues a new cookie and hides prior sessions.',
    eraseButton: 'Erase my chat data',
    resetButton: 'Reset browser identity',
    eraseConfirm: 'Delete all chat sessions for this browser?',
    resetConfirm: 'Reset identity cookie and clear local session list?',
    eraseSuccess: 'Chat data erased',
    eraseFailed: 'Failed to erase chat data',
    resetSuccess: 'Browser identity reset',
    resetFailed: 'Failed to reset identity',
    backToChat: 'Back to chat',
  },
};
