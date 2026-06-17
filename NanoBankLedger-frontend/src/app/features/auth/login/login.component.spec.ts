import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LoginComponent } from './login.component';
import { AuthService } from '../../../core/services/auth.service';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { provideRouter } from '@angular/router';
import { Component } from '@angular/core';

@Component({ template: '', standalone: true })
class DummyDashboardComponent {}

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['login']);

    await TestBed.configureTestingModule({
      imports: [LoginComponent, FormsModule],
      providers: [
        provideRouter([
          { path: 'dashboard', component: DummyDashboardComponent }
        ]),
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have default empty fields', () => {
    expect(component.email).toBe('');
    expect(component.password).toBe('');
    expect(component.loading).toBeFalse();
    expect(component.error).toBe('');
  });

  it('should not call authService.login when fields are empty', () => {
    component.email = '';
    component.password = '';
    component.onSubmit();
    expect(authServiceSpy.login).not.toHaveBeenCalled();
  });

  it('should call authService.login on submit with valid fields', () => {
    const mockResponse = {
      access_token: 'token',
      refresh_token: 'refresh',
      expires_in: 900,
      token_type: 'Bearer',
      user: { id: '1', name: 'Test', email: 'test@test.com' }
    };
    authServiceSpy.login.and.returnValue(of(mockResponse));

    component.email = 'test@test.com';
    component.password = 'password123';
    component.onSubmit();

    expect(authServiceSpy.login).toHaveBeenCalledWith({
      email: 'test@test.com',
      password: 'password123'
    });
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should show error on 401', () => {
    authServiceSpy.login.and.returnValue(throwError(() => ({ status: 401 })));

    component.email = 'test@test.com';
    component.password = 'wrong';
    component.onSubmit();

    expect(component.error).toContain('Email o contraseña incorrectos');
    expect(component.loading).toBeFalse();
  });

  it('should show generic error on other errors', () => {
    authServiceSpy.login.and.returnValue(throwError(() => ({ status: 500 })));

    component.email = 'test@test.com';
    component.password = 'password123';
    component.onSubmit();

    expect(component.error).toContain('Error al iniciar sesión');
    expect(component.loading).toBeFalse();
  });

  it('should set loading to true during login attempt', () => {
    authServiceSpy.login.and.returnValue(of({
      access_token: 'token',
      refresh_token: 'refresh',
      expires_in: 900,
      token_type: 'Bearer',
      user: { id: '1', name: 'Test', email: 'test@test.com' }
    }));

    component.email = 'test@test.com';
    component.password = 'password123';
    component.onSubmit();

    expect(component.loading).toBeTrue();
  });
});
