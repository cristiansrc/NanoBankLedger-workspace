import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Wallet, CreateWalletRequest, UpdateWalletRequest } from '../models/wallet.models';

@Injectable({
  providedIn: 'root'
})
export class WalletService {
  private readonly API_URL = '/api/v1/wallets';

  constructor(private http: HttpClient) {}

  findAll(): Observable<Wallet[]> {
    return this.http.get<Wallet[]>(this.API_URL);
  }

  findById(id: string): Observable<Wallet> {
    return this.http.get<Wallet>(`${this.API_URL}/${id}`);
  }

  create(request: CreateWalletRequest): Observable<Wallet> {
    return this.http.post<Wallet>(this.API_URL, request);
  }

  update(id: string, request: UpdateWalletRequest): Observable<Wallet> {
    return this.http.patch<Wallet>(`${this.API_URL}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }
}
