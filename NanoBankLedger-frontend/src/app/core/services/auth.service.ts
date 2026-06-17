import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest, UserSummary } from '../models/auth.models';

const STORAGE_KEYS = {
  ACCESS_TOKEN: 'nanobank_access_token',
  REFRESH_TOKEN: 'nanobank_refresh_token',
  USER: 'nanobank_user'
} as const;

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = '/api/v1/auth';

  private accessTokenSignal = signal<string | null>(null);
  private refreshTokenSignal = signal<string | null>(null);
  private currentUserSignal = signal<UserSummary | null>(null);

  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly isAuthenticated = computed(() => {
    const token = this.accessTokenSignal();
    if (!token) return false;
    // Verificar que el token no esté vencido
    if (this.isTokenExpired(token)) {
      return false;
    }
    return true;
  });

  get accessToken(): string | null {
    const token = this.accessTokenSignal();
    if (token && this.isTokenExpired(token)) {
      return null;
    }
    return token;
  }

  get refreshTokenValue(): string | null {
    return this.refreshTokenSignal();
  }

  constructor(private http: HttpClient) {
    this.initFromStorage();
  }

  private initFromStorage(): void {
    const accessToken = this.loadFromStorage<string>(STORAGE_KEYS.ACCESS_TOKEN);
    const refreshToken = this.loadFromStorage<string>(STORAGE_KEYS.REFRESH_TOKEN);
    const user = this.loadFromStorage<UserSummary>(STORAGE_KEYS.USER);

    if (accessToken) {
      // Si el token está vencido, intentar refrescar
      if (this.isTokenExpired(accessToken)) {
        if (refreshToken) {
          // Cargar el refresh token para intentar refrescar después
          this.refreshTokenSignal.set(refreshToken);
          this.currentUserSignal.set(user);
          // Intentar refresh automático (el interceptor lo hará al primer request)
        } else {
          // No hay refresh, limpiar todo
          this.clearAuth();
        }
      } else {
        // Token válido, restaurar sesión
        this.accessTokenSignal.set(accessToken);
        this.refreshTokenSignal.set(refreshToken);
        this.currentUserSignal.set(user);
      }
    }
  }

  private isTokenExpired(token: string): boolean {
    try {
      // Decodificar payload JWT (segunda parte del token)
      const payloadBase64 = token.split('.')[1];
      if (!payloadBase64) return true;

      // Decodificar base64 (manejar base64url)
      const payload = JSON.parse(atob(payloadBase64.replace(/-/g, '+').replace(/_/g, '/')));

      // Verificar expiración
      const now = Math.floor(Date.now() / 1000);
      return payload.exp && payload.exp < now;
    } catch {
      // Si no se puede decodificar, considerar vencido
      return true;
    }
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/login`, request)
      .pipe(tap(response => this.handleAuthResponse(response)));
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/register`, request)
      .pipe(tap(response => this.handleAuthResponse(response)));
  }

  refreshAccessToken(): Observable<AuthResponse> {
    const token = this.refreshTokenValue;
    if (!token) {
      throw new Error('No refresh token available');
    }
    return this.http.post<AuthResponse>(`${this.API_URL}/refresh`, { refresh_token: token })
      .pipe(tap(response => this.handleAuthResponse(response)));
  }

  logout(): Observable<void> {
    const token = this.refreshTokenValue;
    if (token) {
      return this.http.post<void>(`${this.API_URL}/logout`, { refresh_token: token })
        .pipe(tap(() => this.clearAuth()));
    }
    this.clearAuth();
    return new Observable<void>(observer => {
      observer.next();
      observer.complete();
    });
  }

  private handleAuthResponse(response: AuthResponse): void {
    this.accessTokenSignal.set(response.access_token);
    this.refreshTokenSignal.set(response.refresh_token);
    this.currentUserSignal.set(response.user);
    this.saveToStorage(STORAGE_KEYS.ACCESS_TOKEN, response.access_token);
    this.saveToStorage(STORAGE_KEYS.REFRESH_TOKEN, response.refresh_token);
    this.saveToStorage(STORAGE_KEYS.USER, response.user);
  }

  clearAuth(): void {
    this.accessTokenSignal.set(null);
    this.refreshTokenSignal.set(null);
    this.currentUserSignal.set(null);
    this.removeFromStorage(STORAGE_KEYS.ACCESS_TOKEN);
    this.removeFromStorage(STORAGE_KEYS.REFRESH_TOKEN);
    this.removeFromStorage(STORAGE_KEYS.USER);
  }

  private loadFromStorage<T>(key: string): T | null {
    try {
      const value = localStorage.getItem(key);
      return value ? JSON.parse(value) : null;
    } catch {
      return null;
    }
  }

  private saveToStorage(key: string, value: any): void {
    try {
      localStorage.setItem(key, JSON.stringify(value));
    } catch {
      // localStorage no disponible
    }
  }

  private removeFromStorage(key: string): void {
    try {
      localStorage.removeItem(key);
    } catch {
      // localStorage no disponible
    }
  }
}
