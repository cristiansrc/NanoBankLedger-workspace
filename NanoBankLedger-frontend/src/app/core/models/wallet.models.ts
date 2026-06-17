export interface Wallet {
  id: string;
  user_id: string;
  name: string;
  type: WalletType;
  balance: string;
  created_at: string;
  updated_at: string;
}

export type WalletType = 'SAVINGS' | 'CHECKING' | 'INVESTMENT' | 'CASH';

export interface CreateWalletRequest {
  name: string;
  type: WalletType;
  initial_balance?: string;
}

export interface UpdateWalletRequest {
  name: string;
  type?: WalletType;
}
