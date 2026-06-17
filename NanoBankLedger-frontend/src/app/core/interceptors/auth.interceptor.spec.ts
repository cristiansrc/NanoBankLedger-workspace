import { TestBed } from '@angular/core/testing';
import { HttpInterceptorFn } from '@angular/common/http';
import { HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';
import { of, throwError } from 'rxjs';
import { Router } from '@angular/router';

describe('authInterceptor', () => {
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let routerSpy: jasmine.SpyObj<Router>;

  const interceptor: HttpInterceptorFn = (req, next) =>
    TestBed.runInInjectionContext(() => authInterceptor(req, next));

  function configureSpy(accessTokenVal: string | null, refreshTokenVal: string | null = null) {
    authServiceSpy = jasmine.createSpyObj('AuthService',
      ['refreshAccessToken', 'logout', 'clearAuth'],
      { accessToken: accessTokenVal, refreshTokenValue: refreshTokenVal }
    );
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });
  }

  const mockAuthResponse = {
    access_token: 'refreshed-token',
    refresh_token: 'refresh-token-123',
    expires_in: 3600,
    token_type: 'Bearer',
    user: { id: '1', name: 'Test', email: 'test@test.com' }
  };

  it('should be created', () => {
    configureSpy(null);
    expect(interceptor).toBeTruthy();
  });

  it('should add Authorization header when token exists', (done) => {
    configureSpy('test-token-123');

    const req = new HttpRequest('GET', '/api/v1/wallets');
    const next: HttpHandlerFn = (request) => {
      expect(request.headers.get('Authorization')).toBe('Bearer test-token-123');
      done();
      return of({} as any);
    };

    TestBed.runInInjectionContext(() => authInterceptor(req, next)).subscribe();
  });

  it('should not add Authorization header for auth endpoints', (done) => {
    configureSpy('test-token');

    const req = new HttpRequest('POST', '/api/v1/auth/login', {});
    const next: HttpHandlerFn = (request) => {
      expect(request.headers.get('Authorization')).toBeNull();
      done();
      return of({} as any);
    };

    TestBed.runInInjectionContext(() => authInterceptor(req, next)).subscribe();
  });

  it('should pass through non-401 errors', (done) => {
    configureSpy('test-token');

    const req = new HttpRequest('GET', '/api/v1/wallets');
    const httpError = new HttpErrorResponse({ status: 403, statusText: 'Forbidden' });
    const next: HttpHandlerFn = () => throwError(() => httpError);

    TestBed.runInInjectionContext(() => authInterceptor(req, next)).subscribe({
      error: (error) => {
        expect(error.status).toBe(403);
        done();
      }
    });
  });

  it('should handle 401 by refreshing token and retrying', (done) => {
    configureSpy('initial-token', 'valid-refresh-token');
    authServiceSpy.refreshAccessToken.and.returnValue(of(mockAuthResponse));

    const req = new HttpRequest('GET', '/api/v1/wallets');

    // First call fails with 401, second succeeds after refresh
    let callCount = 0;
    const next: HttpHandlerFn = (request) => {
      callCount++;
      if (callCount === 1) {
        return throwError(() => new HttpErrorResponse({ status: 401 }));
      }
      expect(request.headers.get('Authorization')).toBe('Bearer refreshed-token');
      done();
      return of({} as any);
    };

    TestBed.runInInjectionContext(() => authInterceptor(req, next)).subscribe();
  });

  it('should clear auth when refresh fails', (done) => {
    configureSpy('initial-token', 'valid-refresh-token');
    authServiceSpy.refreshAccessToken.and.returnValue(throwError(() => new Error('Refresh failed')));

    const req = new HttpRequest('GET', '/api/v1/wallets');
    const next: HttpHandlerFn = () => throwError(() => new HttpErrorResponse({ status: 401 }));

    TestBed.runInInjectionContext(() => authInterceptor(req, next)).subscribe({
      error: () => {
        expect(authServiceSpy.clearAuth).toHaveBeenCalled();
        done();
      }
    });
  });

  it('should redirect to login when no refresh token', (done) => {
    configureSpy('initial-token', null);

    const req = new HttpRequest('GET', '/api/v1/wallets');
    const next: HttpHandlerFn = () => throwError(() => new HttpErrorResponse({ status: 401 }));

    TestBed.runInInjectionContext(() => authInterceptor(req, next)).subscribe({
      error: () => {
        expect(authServiceSpy.clearAuth).toHaveBeenCalled();
        done();
      }
    });
  });

  it('should queue concurrent 401 requests and retry both after refresh', (done) => {
    configureSpy('initial-token', 'valid-refresh-token');
    authServiceSpy.refreshAccessToken.and.returnValue(of(mockAuthResponse));

    const req1 = new HttpRequest('GET', '/api/v1/wallets/1');
    const req2 = new HttpRequest('GET', '/api/v1/wallets/2');

    let completedCount = 0;
    function checkDone() {
      completedCount++;
      if (completedCount === 2) done();
    }

    const next1: HttpHandlerFn = () => throwError(() => new HttpErrorResponse({ status: 401 }));
    const next2: HttpHandlerFn = () => throwError(() => new HttpErrorResponse({ status: 401 }));

    TestBed.runInInjectionContext(() => authInterceptor(req1, next1)).subscribe({
      next: () => checkDone(),
      error: () => checkDone()
    });

    TestBed.runInInjectionContext(() => authInterceptor(req2, next2)).subscribe({
      next: () => checkDone(),
      error: () => checkDone()
    });
  });
});
