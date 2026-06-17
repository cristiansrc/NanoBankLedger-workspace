import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Transaction, CreateTransactionRequest, UpdateTransactionRequest, MoveTransactionRequest, TransactionFilters } from '../models/transaction.models';

@Injectable({
  providedIn: 'root'
})
export class TransactionService {
  private readonly API_URL = '/api/v1';

  constructor(private http: HttpClient) {}

  findByWalletId(
    walletId: string,
    filters?: TransactionFilters
  ): Observable<Transaction[]> {
    let params = new HttpParams();
    if (filters?.category_id) params = params.set('category_id', filters.category_id);
    if (filters?.date_from) params = params.set('date_from', filters.date_from);
    if (filters?.date_to) params = params.set('date_to', filters.date_to);
    if (filters?.type) params = params.set('type', filters.type);

    return this.http.get<Transaction[]>(`${this.API_URL}/wallets/${walletId}/transactions`, { params });
  }

  findById(id: string): Observable<Transaction> {
    return this.http.get<Transaction>(`${this.API_URL}/transactions/${id}`);
  }

  create(walletId: string, request: CreateTransactionRequest): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.API_URL}/wallets/${walletId}/transactions`, request);
  }

  update(id: string, request: UpdateTransactionRequest): Observable<Transaction> {
    return this.http.patch<Transaction>(`${this.API_URL}/transactions/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/transactions/${id}`);
  }

  moveToWallet(id: string, request: MoveTransactionRequest): Observable<Transaction> {
    return this.http.patch<Transaction>(`${this.API_URL}/transactions/${id}/move`, request);
  }
}
