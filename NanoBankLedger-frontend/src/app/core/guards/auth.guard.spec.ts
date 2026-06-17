import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

describe('AuthGuard', () => {
  let authServiceStub: any;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    authServiceStub = {
      isAuthenticated: () => false
    };
    routerSpy = jasmine.createSpyObj('Router', ['parseUrl']);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceStub },
        { provide: Router, useValue: routerSpy }
      ]
    });
  });

  it('should allow access when authenticated', () => {
    authServiceStub.isAuthenticated = () => true;
    const mockRoute = {} as any;
    const mockState = {} as any;
    const result = TestBed.runInInjectionContext(() => authGuard(mockRoute, mockState));
    expect(result).toBeTrue();
  });

  it('should redirect to login when not authenticated', () => {
    routerSpy.parseUrl.and.returnValue('/auth/login' as any);
    const mockRoute = {} as any;
    const mockState = {} as any;
    const result = TestBed.runInInjectionContext(() => authGuard(mockRoute, mockState));
    expect(routerSpy.parseUrl).toHaveBeenCalledWith('/auth/login');
  });

  it('should return a UrlTree when not authenticated', () => {
    const urlTree = {} as any;
    routerSpy.parseUrl.and.returnValue(urlTree);
    const mockRoute = {} as any;
    const mockState = {} as any;
    const result = TestBed.runInInjectionContext(() => authGuard(mockRoute, mockState));
    expect(result).toBe(urlTree as any);
  });
});
