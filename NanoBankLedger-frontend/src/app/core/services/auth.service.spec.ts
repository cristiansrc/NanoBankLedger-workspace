import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

/**
 * Helper: crea un token JWT (solo payload) con expiración controlada.
 * Estructura: header.payload.signature (base64url)
 */
function createJwt(expOffsetSec: number): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const payload = btoa(JSON.stringify({
    sub: 'test-user-id',
    email: 'test@test.com',
    exp: Math.floor(Date.now() / 1000) + expOffsetSec,
    iat: Math.floor(Date.now() / 1000)
  }));
  const signature = btoa('fakesignature');
  return `${header}.${payload}.${signature}`;
}

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService]
    });
  });

  afterEach(() => {
    // Limpiar localStorage después de cada test
    localStorage.clear();
  });

  describe('fresh instance (no stored tokens)', () => {
    beforeEach(() => {
      service = TestBed.inject(AuthService);
      httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => httpMock.verify());

    it('should be created', () => expect(service).toBeTruthy());

    it('should not be authenticated before login', () => {
      expect(service.isAuthenticated()).toBeFalse();
    });

    it('should login and store tokens in signals', () => {
      const token = createJwt(900); // 15 min válido
      const mockResponse = {
        access_token: token,
        refresh_token: 'ref123',
        expires_in: 900,
        token_type: 'Bearer',
        user: { id: '1', name: 'Test', email: 't@t.com' }
      };

      service.login({ email: 't@t.com', password: 'pass' }).subscribe(() => {
        expect(service.isAuthenticated()).toBeTrue();
        expect(service.currentUser()?.name).toBe('Test');
      });

      const req = httpMock.expectOne('/api/v1/auth/login');
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });

    it('should not be authenticated after logout', () => {
      const token = createJwt(900);
      const mockResponse = {
        access_token: token,
        refresh_token: 'ref123',
        expires_in: 900,
        token_type: 'Bearer',
        user: { id: '1', name: 'Test', email: 't@t.com' }
      };

      service.login({ email: 't@t.com', password: 'pass' }).subscribe();
      httpMock.expectOne('/api/v1/auth/login').flush(mockResponse);

      expect(service.isAuthenticated()).toBeTrue();

      service.logout().subscribe();
      httpMock.expectOne('/api/v1/auth/logout').flush(null);

      expect(service.isAuthenticated()).toBeFalse();
      expect(service.currentUser()).toBeNull();
    });

    it('should handle login error', () => {
      service.login({ email: 'bad@test.com', password: 'wrong' }).subscribe({
        error: (err) => {
          expect(err.status).toBe(401);
        }
      });

      const req = httpMock.expectOne('/api/v1/auth/login');
      req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    });

    it('should register and store tokens', () => {
      const token = createJwt(900);
      const mockResponse = {
        access_token: token,
        refresh_token: 'refReg',
        expires_in: 900,
        token_type: 'Bearer',
        user: { id: '2', name: 'NewUser', email: 'new@t.com' }
      };

      service.register({ name: 'NewUser', email: 'new@t.com', password: 'pass' })
        .subscribe(() => {
          expect(service.isAuthenticated()).toBeTrue();
        });

      const req = httpMock.expectOne('/api/v1/auth/register');
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });

    it('should get access token via getter', () => {
      const token = createJwt(900);
      const mockResponse = {
        access_token: token,
        refresh_token: 'refToken',
        expires_in: 900,
        token_type: 'Bearer',
        user: { id: '1', name: 'Test', email: 't@t.com' }
      };

      service.login({ email: 't@t.com', password: 'pass' }).subscribe(() => {
        expect(service.accessToken).toBe(token);
      });

      httpMock.expectOne('/api/v1/auth/login').flush(mockResponse);
    });

    it('should refresh access token', () => {
      const oldToken = createJwt(900);
      const mockResponse = {
        access_token: oldToken,
        refresh_token: 'oldRefresh',
        expires_in: 900,
        token_type: 'Bearer',
        user: { id: '1', name: 'Test', email: 't@t.com' }
      };

      // First login
      service.login({ email: 't@t.com', password: 'pass' }).subscribe();
      httpMock.expectOne('/api/v1/auth/login').flush(mockResponse);

      // Then refresh
      const newToken = createJwt(900);
      const refreshResponse = {
        access_token: newToken,
        refresh_token: 'newRefresh',
        expires_in: 900,
        token_type: 'Bearer',
        user: { id: '1', name: 'Test', email: 't@t.com' }
      };

      service.refreshAccessToken().subscribe(() => {
        expect(service.accessToken).toBe(newToken);
      });

      const req = httpMock.expectOne('/api/v1/auth/refresh');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ refresh_token: 'oldRefresh' });
      req.flush(refreshResponse);
    });

    it('should throw error when refreshing without token', () => {
      expect(() => service.refreshAccessToken()).toThrowError('No refresh token available');
    });

    it('should handle logout without token gracefully', () => {
      service.logout().subscribe({
        next: () => {
          expect(service.isAuthenticated()).toBeFalse();
        }
      });
    });

    it('should return false for isAuthenticated when token is expired', () => {
      const expiredToken = createJwt(-3600); // expirado hace 1 hora
      const mockResponse = {
        access_token: expiredToken,
        refresh_token: 'refExpired',
        expires_in: 900,
        token_type: 'Bearer',
        user: { id: '1', name: 'Test', email: 't@t.com' }
      };

      service.login({ email: 't@t.com', password: 'pass' }).subscribe(() => {
        expect(service.isAuthenticated()).toBeFalse();
        expect(service.accessToken).toBeNull();
      });

      httpMock.expectOne('/api/v1/auth/login').flush(mockResponse);
    });

    it('should return null for accessToken getter when token is expired', () => {
      const expiredToken = createJwt(-3600);
      const mockResponse = {
        access_token: expiredToken,
        refresh_token: 'refExpired',
        expires_in: 900,
        token_type: 'Bearer',
        user: { id: '1', name: 'Test', email: 't@t.com' }
      };

      service.login({ email: 't@t.com', password: 'pass' }).subscribe(() => {
        expect(service.accessToken).toBeNull();
      });

      httpMock.expectOne('/api/v1/auth/login').flush(mockResponse);
    });

    it('should treat malformed token as not authenticated', () => {
      const mockResponse = {
        access_token: 'invalid-token-format',
        refresh_token: 'refMalformed',
        expires_in: 900,
        token_type: 'Bearer',
        user: { id: '1', name: 'Test', email: 't@t.com' }
      };

      service.login({ email: 't@t.com', password: 'pass' }).subscribe(() => {
        expect(service.isAuthenticated()).toBeFalse();
        expect(service.accessToken).toBeNull();
      });

      httpMock.expectOne('/api/v1/auth/login').flush(mockResponse);
    });
  });

  describe('initialization from localStorage', () => {
    it('should restore session when valid token exists', () => {
      const validToken = createJwt(900);
      localStorage.setItem('nanobank_access_token', JSON.stringify(validToken));
      localStorage.setItem('nanobank_refresh_token', JSON.stringify('refStored'));
      localStorage.setItem('nanobank_user', JSON.stringify({ id: '1', name: 'Stored', email: 's@t.com' }));

      service = TestBed.inject(AuthService);
      httpMock = TestBed.inject(HttpTestingController);

      expect(service.isAuthenticated()).toBeTrue();
      expect(service.currentUser()?.name).toBe('Stored');
      expect(service.accessToken).toBe(validToken);
    });

    it('should clear auth when expired token stored without refresh token', () => {
      const expiredToken = createJwt(-3600);
      localStorage.setItem('nanobank_access_token', JSON.stringify(expiredToken));
      localStorage.setItem('nanobank_refresh_token', JSON.stringify('refStored'));
      localStorage.setItem('nanobank_user', JSON.stringify({ id: '1', name: 'Stored', email: 's@t.com' }));

      service = TestBed.inject(AuthService);
      httpMock = TestBed.inject(HttpTestingController);

      // Token expirado pero con refresh token presente -> isAuthenticated false, pero signals almacenan refresh y user
      expect(service.isAuthenticated()).toBeFalse();
      expect(service.currentUser()?.name).toBe('Stored');
      // accessToken debe ser null porque el token está expirado
      expect(service.accessToken).toBeNull();
      // Pero refreshTokenValue debe mantenerse para intentar refresh
      expect(service.refreshTokenValue).toBe('refStored');
    });

    it('should clear auth when expired token stored without refresh token', () => {
      const expiredToken = createJwt(-3600);
      localStorage.setItem('nanobank_access_token', JSON.stringify(expiredToken));
      // Sin refresh token en localStorage
      localStorage.setItem('nanobank_user', JSON.stringify({ id: '1', name: 'Stored', email: 's@t.com' }));

      service = TestBed.inject(AuthService);
      httpMock = TestBed.inject(HttpTestingController);

      expect(service.isAuthenticated()).toBeFalse();
      expect(service.currentUser()).toBeNull();
      expect(service.accessToken).toBeNull();
      expect(service.refreshTokenValue).toBeNull();
    });

    it('should not authenticate when no token stored', () => {
      localStorage.clear();
      service = TestBed.inject(AuthService);
      httpMock = TestBed.inject(HttpTestingController);

      expect(service.isAuthenticated()).toBeFalse();
      expect(service.currentUser()).toBeNull();
      expect(service.accessToken).toBeNull();
    });

    it('should clear everything when malformed token and no refresh token', () => {
      localStorage.setItem('nanobank_access_token', JSON.stringify('not-a-valid-jwt'));
      localStorage.setItem('nanobank_user', JSON.stringify({ id: '1', name: 'Stored', email: 's@t.com' }));

      service = TestBed.inject(AuthService);
      httpMock = TestBed.inject(HttpTestingController);

      expect(service.isAuthenticated()).toBeFalse();
      expect(service.accessToken).toBeNull();
      expect(service.currentUser()).toBeNull();
      expect(service.refreshTokenValue).toBeNull();
    });

    it('should handle malformed stored token gracefully', () => {
      localStorage.setItem('nanobank_access_token', JSON.stringify('not-a-valid-jwt'));
      localStorage.setItem('nanobank_refresh_token', JSON.stringify('refStored'));
      localStorage.setItem('nanobank_user', JSON.stringify({ id: '1', name: 'Stored', email: 's@t.com' }));

      service = TestBed.inject(AuthService);
      httpMock = TestBed.inject(HttpTestingController);

      // Token malformado se considera expirado -> isAuthenticated false
      expect(service.isAuthenticated()).toBeFalse();
      expect(service.accessToken).toBeNull();
      // Cuando hay refresh token disponible, se conserva el usuario
      // para que el interceptor pueda intentar un refresh
      expect(service.currentUser()?.name).toBe('Stored');
      expect(service.refreshTokenValue).toBe('refStored');
    });
  });
});
