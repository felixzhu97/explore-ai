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
  processors: { name: string; purpose: string }[];
  analyticsHeading: string;
  analyticsHelp: string;
  analyticsLabel: string;
  contactEmailLabel: string;
  contactEmailHelp: string;
  contactEmailInvalid: string;
  savePreferencesButton: string;
  savePreferencesSaving: string;
  savePreferencesSuccess: string;
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
  legalHub: string;
  subprocessorsLink: string;
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
    processors: [
      { name: 'DeepSeek', purpose: 'Large language model inference for chat' },
      { name: 'OpenAI', purpose: 'Optional image generation and text-to-speech' },
      { name: 'Serper', purpose: 'Web search tool results' },
      { name: 'LaunchDarkly', purpose: 'Feature flags (only with analytics consent)' },
      { name: 'Datadog', purpose: 'Optional RUM / APM (only with analytics consent)' },
    ],
    analyticsHeading: 'Analytics preference',
    analyticsHelp:
      'Optional Datadog RUM and LaunchDarkly load only when you allow analytics. Feature modules still work with local fallbacks when analytics is off.',
    analyticsLabel: 'Allow analytics & feature telemetry',
    contactEmailLabel: 'Privacy contact email (optional)',
    contactEmailHelp:
      'Optional email if you want a record for privacy requests tied to this browser.',
    contactEmailInvalid: 'Enter a valid email address',
    savePreferencesButton: 'Save preferences',
    savePreferencesSaving: 'Saving…',
    savePreferencesSuccess: 'Privacy preferences saved',
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
    legalHub: 'Policies',
    subprocessorsLink: 'Sub-processors',
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
    processors: [
      { name: 'DeepSeek', purpose: '聊天用大语言模型推理' },
      { name: 'OpenAI', purpose: '可选的图像生成与语音合成' },
      { name: 'Serper', purpose: '网络搜索工具结果' },
      { name: 'LaunchDarkly', purpose: '功能开关（仅在同意分析时）' },
      { name: 'Datadog', purpose: '可选 RUM / APM（仅在同意分析时）' },
    ],
    analyticsHeading: '分析偏好',
    analyticsHelp:
      '仅在您允许分析时加载可选的 Datadog RUM 与 LaunchDarkly。关闭分析时功能模块仍使用本地回退配置。',
    analyticsLabel: '允许分析与功能遥测',
    contactEmailLabel: '隐私联系邮箱（可选）',
    contactEmailHelp: '可选：便于将隐私请求与本浏览器关联。',
    contactEmailInvalid: '请输入有效邮箱地址',
    savePreferencesButton: '保存偏好',
    savePreferencesSaving: '保存中…',
    savePreferencesSuccess: '隐私偏好已保存',
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
    legalHub: '政策',
    subprocessorsLink: '子处理方',
  },
  ja: {
    title: 'プライバシー',
    subtitle: 'ExploreAI が匿名のブラウザデータを扱う方法',
    noticeHeading: 'お知らせ',
    noticeIdentity:
      '機能的な HttpOnly Cookie（クライアント識別子）により、チャットセッションはこのブラウザに限定されます。セッション分離に必要であり、広告には使用しません。',
    noticeChat:
      'チャットメッセージは返信生成のため、言語モデルおよび任意の検索プロバイダーに送信されます。処理されたくない機微な個人データは送信しないでください。',
    noticeRetention:
      '非アクティブなチャットセッションと関連メトリクスイベントは約 90 日後に削除されます。下記からいつでもこのブラウザのセッションを消去できます。',
    processorsHeading: 'サブプロセッサー',
    processors: [
      { name: 'DeepSeek', purpose: 'チャット向け大規模言語モデル推論' },
      { name: 'OpenAI', purpose: '任意の画像生成と音声合成' },
      { name: 'Serper', purpose: 'ウェブ検索ツールの結果' },
      { name: 'LaunchDarkly', purpose: '機能フラグ（分析同意時のみ）' },
      { name: 'Datadog', purpose: '任意の RUM / APM（分析同意時のみ）' },
    ],
    analyticsHeading: '分析の設定',
    analyticsHelp:
      '任意の Datadog RUM と LaunchDarkly は、分析を許可した場合にのみ読み込まれます。分析をオフにしても機能モジュールはローカルのフォールバックで動作します。',
    analyticsLabel: '分析と機能テレメトリを許可する',
    contactEmailLabel: 'プライバシー連絡用メール（任意）',
    contactEmailHelp:
      'このブラウザに紐づくプライバシー請求の記録が必要な場合に任意で入力できます。',
    contactEmailInvalid: '有効なメールアドレスを入力してください',
    savePreferencesButton: '設定を保存',
    savePreferencesSaving: '保存中…',
    savePreferencesSuccess: 'プライバシー設定を保存しました',
    controlsHeading: 'ご自身のコントロール',
    controlsHelp:
      '消去すると、このブラウザ識別子のチャットセッションと関連メトリクスが削除されます。識別子のリセットは新しい Cookie を発行し、以前のセッションを非表示にします。',
    eraseButton: 'チャットデータを消去',
    resetButton: 'ブラウザ識別子をリセット',
    eraseConfirm: 'このブラウザのすべてのチャットセッションを削除しますか？',
    resetConfirm: '識別子 Cookie をリセットし、ローカルのセッション一覧をクリアしますか？',
    eraseSuccess: 'チャットデータを消去しました',
    eraseFailed: 'チャットデータの消去に失敗しました',
    resetSuccess: 'ブラウザ識別子をリセットしました',
    resetFailed: '識別子のリセットに失敗しました',
    backToChat: 'チャットに戻る',
    legalHub: 'ポリシー',
    subprocessorsLink: 'サブプロセッサー',
  },
  fr: {
    title: 'Confidentialité',
    subtitle: 'Comment ExploreAI traite les données anonymes du navigateur',
    noticeHeading: 'Avis',
    noticeIdentity:
      'Un cookie HttpOnly fonctionnel (identité client) limite les sessions de chat à ce navigateur. Il est requis pour l’isolation des sessions et n’est pas utilisé à des fins publicitaires.',
    noticeChat:
      'Les messages de chat sont envoyés à des fournisseurs de modèles de langage et, le cas échéant, de recherche pour générer des réponses. N’envoyez pas de données personnelles sensibles que vous ne souhaitez pas voir traitées.',
    noticeRetention:
      'Les sessions de chat inactives et les événements de métriques associés sont purgés après environ 90 jours. Vous pouvez effacer les sessions de ce navigateur à tout moment ci-dessous.',
    processorsHeading: 'Sous-traitants',
    processors: [
      { name: 'DeepSeek', purpose: 'Inférence de grand modèle de langage pour le chat' },
      { name: 'OpenAI', purpose: 'Génération d’images et synthèse vocale optionnelles' },
      { name: 'Serper', purpose: 'Résultats de l’outil de recherche web' },
      { name: 'LaunchDarkly', purpose: 'Indicateurs de fonctionnalités (uniquement avec consentement analytique)' },
      { name: 'Datadog', purpose: 'RUM / APM optionnels (uniquement avec consentement analytique)' },
    ],
    analyticsHeading: 'Préférence d’analyse',
    analyticsHelp:
      'Datadog RUM et LaunchDarkly optionnels ne se chargent que si vous autorisez l’analyse. Les modules fonctionnent toujours avec des replis locaux lorsque l’analyse est désactivée.',
    analyticsLabel: 'Autoriser l’analyse et la télémétrie des fonctionnalités',
    contactEmailLabel: 'E-mail de contact confidentialité (facultatif)',
    contactEmailHelp:
      'E-mail facultatif si vous souhaitez un enregistrement pour les demandes de confidentialité liées à ce navigateur.',
    contactEmailInvalid: 'Saisissez une adresse e-mail valide',
    savePreferencesButton: 'Enregistrer les préférences',
    savePreferencesSaving: 'Enregistrement…',
    savePreferencesSuccess: 'Préférences de confidentialité enregistrées',
    controlsHeading: 'Vos contrôles',
    controlsHelp:
      'Effacer supprime les sessions de chat et les métriques liées pour cette identité de navigateur. Réinitialiser l’identité émet un nouveau cookie et masque les sessions précédentes.',
    eraseButton: 'Effacer mes données de chat',
    resetButton: 'Réinitialiser l’identité du navigateur',
    eraseConfirm: 'Supprimer toutes les sessions de chat de ce navigateur ?',
    resetConfirm: 'Réinitialiser le cookie d’identité et vider la liste locale des sessions ?',
    eraseSuccess: 'Données de chat effacées',
    eraseFailed: 'Échec de l’effacement des données de chat',
    resetSuccess: 'Identité du navigateur réinitialisée',
    resetFailed: 'Échec de la réinitialisation de l’identité',
    backToChat: 'Retour au chat',
    legalHub: 'Politiques',
    subprocessorsLink: 'Sous-traitants',
  },
  es: {
    title: 'Privacidad',
    subtitle: 'Cómo ExploreAI trata los datos anónimos del navegador',
    noticeHeading: 'Aviso',
    noticeIdentity:
      'Una cookie HttpOnly funcional (identidad del cliente) limita las sesiones de chat a este navegador. Es necesaria para el aislamiento de sesiones y no se usa para publicidad.',
    noticeChat:
      'Los mensajes de chat se envían a proveedores de modelos de lenguaje y, opcionalmente, de búsqueda para generar respuestas. No envíe datos personales sensibles que no desee que se procesen.',
    noticeRetention:
      'Las sesiones de chat inactivas y los eventos de métricas relacionados se eliminan tras unos 90 días. Puede borrar las sesiones de este navegador en cualquier momento más abajo.',
    processorsHeading: 'Subencargados del tratamiento',
    processors: [
      { name: 'DeepSeek', purpose: 'Inferencia de modelo de lenguaje grande para el chat' },
      { name: 'OpenAI', purpose: 'Generación de imágenes y texto a voz opcionales' },
      { name: 'Serper', purpose: 'Resultados de la herramienta de búsqueda web' },
      { name: 'LaunchDarkly', purpose: 'Indicadores de funciones (solo con consentimiento de analítica)' },
      { name: 'Datadog', purpose: 'RUM / APM opcionales (solo con consentimiento de analítica)' },
    ],
    analyticsHeading: 'Preferencia de analítica',
    analyticsHelp:
      'Datadog RUM y LaunchDarkly opcionales solo se cargan si permite la analítica. Los módulos siguen funcionando con alternativas locales cuando la analítica está desactivada.',
    analyticsLabel: 'Permitir analítica y telemetría de funciones',
    contactEmailLabel: 'Correo de contacto de privacidad (opcional)',
    contactEmailHelp:
      'Correo opcional si desea un registro para solicitudes de privacidad vinculadas a este navegador.',
    contactEmailInvalid: 'Introduzca una dirección de correo válida',
    savePreferencesButton: 'Guardar preferencias',
    savePreferencesSaving: 'Guardando…',
    savePreferencesSuccess: 'Preferencias de privacidad guardadas',
    controlsHeading: 'Sus controles',
    controlsHelp:
      'Borrar elimina las sesiones de chat y las métricas vinculadas de esta identidad del navegador. Restablecer la identidad emite una cookie nueva y oculta las sesiones anteriores.',
    eraseButton: 'Borrar mis datos de chat',
    resetButton: 'Restablecer identidad del navegador',
    eraseConfirm: '¿Eliminar todas las sesiones de chat de este navegador?',
    resetConfirm: '¿Restablecer la cookie de identidad y vaciar la lista local de sesiones?',
    eraseSuccess: 'Datos de chat borrados',
    eraseFailed: 'Error al borrar los datos de chat',
    resetSuccess: 'Identidad del navegador restablecida',
    resetFailed: 'Error al restablecer la identidad',
    backToChat: 'Volver al chat',
    legalHub: 'Políticas',
    subprocessorsLink: 'Subencargados',
  },
};
