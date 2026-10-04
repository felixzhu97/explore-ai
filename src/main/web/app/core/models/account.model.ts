export type OAuthProviderId = 'google' | 'github' | 'explore-iam';

export interface AccountMe {
  mode: string;
  clientId: string;
  userId: string | null;
  email: string | null;
  plan: string;
  loginAvailable: boolean;
  loginProviders: OAuthProviderId[];
}
