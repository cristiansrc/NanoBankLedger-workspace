export interface Transaction {
  id: string;
  wallet_id: string;
  category_id?: string;
  type: TransactionType;
  amount: string;
  description?: string;
  date: string;
  created_at: string;
  updated_at: string;
}

export type TransactionType = 'INCOME' | 'EXPENSE';

export interface CreateTransactionRequest {
  type: TransactionType;
  amount: string;
  category_id?: string;
  description?: string;
  date?: string;
}

export interface UpdateTransactionRequest {
  type?: TransactionType;
  amount?: string;
  category_id?: string;
  description?: string;
  date?: string;
}

export interface MoveTransactionRequest {
  target_wallet_id: string;
}

export interface TransactionFilters {
  category_id?: string;
  date_from?: string;
  date_to?: string;
  type?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  total_pages: number;
  total_elements: number;
  page: number;
  size: number;
}
