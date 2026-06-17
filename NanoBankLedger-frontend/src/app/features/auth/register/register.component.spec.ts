import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RegisterComponent } from './register.component';
import { AuthService } from '../../../core/services/auth.service';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { provideRouter } from '@angular/router';
import { Component } from '@angular/core';

@Component({ template: '', standalone: true })
class DummyDashboardComponent {}

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['register']);

    await TestBed.configureTestingModule({
      imports: [RegisterComponent, FormsModule],
      providers: [
        provideRouter([
          { path: 'dashboard', component: DummyDashboardComponent }
        ]),
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have default empty fields', () => {
    expect(component.name).toBe('');
    expect(component.email).toBe('');
    expect(component.password).toBe('');
    expect(component.loading).toBeFalse();
    expect(component.error).toBe('');
  });

  it('should not call authService.register when fields are empty', () => {
    component.name = '';
    component.email = '';
    component.password = '';
    component.onSubmit();
    expect(authServiceSpy.register).not.toHaveBeenCalled();
  });

  it('should show validation error when password is less than 8 characters', () => {
    component.name = 'Test User';
    component.email = 'test@test.com';
    component.password = '1234567';
    component.onSubmit();

    expect(component.error).toContain('La contraseña debe tener al menos 8 caracteres');
    expect(authServiceSpy.register).not.toHaveBeenCalled();
  });

  it('should call authService.register on submit with valid data', () => {
    const mockResponse = {
      access_token: 'token',
      refresh_token: 'refresh',
      expires_in: 900,
      token_type: 'Bearer',
      user: { id: '1', name: 'Test User', email: 'test@test.com' }
    };
    authServiceSpy.register.and.returnValue(of(mockResponse));

    component.name = 'Test User';
    component.email = 'test@test.com';
    component.password = 'password123';
    component.onSubmit();

    expect(authServiceSpy.register).toHaveBeenCalledWith({
      name: 'Test User',
      email: 'test@test.com',
      password: 'password123'
    });
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should show error on 409 (duplicate email)', () => {
    authServiceSpy.register.and.returnValue(throwError(() => ({ status: 409 })));

    component.name = 'Test User';
    component.email = 'existing@test.com';
    component.password = 'password123';
    component.onSubmit();

    expect(component.error).toContain('Este email ya está registrado');
    expect(component.loading).toBeFalse();
  });

  it('should show generic error on other errors', () => {
    authServiceSpy.register.and.returnValue(throwError(() => ({ status: 500 })));

    component.name = 'Test User';
    component.email = 'test@test.com';
    component.password = 'password123';
    component.onSubmit();

    expect(component.error).toContain('Error al crear la cuenta');
    expect(component.loading).toBeFalse();
  });

  it('should set loading to true during register attempt', () => {
    authServiceSpy.register.and.returnValue(of({
      access_token: 'token',
      refresh_token: 'refresh',
      expires_in: 900,
      token_type: 'Bearer',
      user: { id: '1', name: 'Test User', email: 'test@test.com' }
    }));

    component.name = 'Test User';
    component.email = 'test@test.com';
    component.password = 'password123';
    component.onSubmit();

    expect(component.loading).toBeTrue();
  });
});
