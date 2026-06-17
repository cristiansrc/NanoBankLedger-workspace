import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WalletListComponent } from './wallet-list.component';
import { WalletService } from '../../../../core/services/wallet.service';
import { Wallet } from '../../../../core/models/wallet.models';
import { provideRouter } from '@angular/router';
import { Component } from '@angular/core';
import { of, throwError } from 'rxjs';

@Component({ template: '', standalone: true })
class DummyComponent {}

describe('WalletListComponent', () => {
  let component: WalletListComponent;
  let fixture: ComponentFixture<WalletListComponent>;
  let walletServiceSpy: jasmine.SpyObj<WalletService>;

  const mockWallets: Wallet[] = [
    { id: '1', name: 'Test', type: 'SAVINGS', balance: '100.00', user_id: '1', created_at: '', updated_at: '' }
  ];

  const mockWallet: Wallet = { id: '1', name: 'Test', type: 'SAVINGS', balance: '100.00', user_id: '1', created_at: '', updated_at: '' };

  beforeEach(async () => {
    walletServiceSpy = jasmine.createSpyObj('WalletService', ['findAll', 'delete']);

    await TestBed.configureTestingModule({
      imports: [WalletListComponent],
      providers: [
        provideRouter([{ path: 'wallets/:id/transactions', component: DummyComponent }]),
        { provide: WalletService, useValue: walletServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(WalletListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    walletServiceSpy.findAll.and.returnValue(of([]));
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should load wallets on init', () => {
    walletServiceSpy.findAll.and.returnValue(of(mockWallets));

    fixture.detectChanges();

    expect(component.wallets()).toEqual(mockWallets);
    expect(component.loading()).toBeFalse();
  });

  it('should handle error when loading wallets', () => {
    walletServiceSpy.findAll.and.returnValue(throwError(() => ({ status: 500 })));

    fixture.detectChanges();

    expect(component.error()).toContain('Error al cargar billeteras');
    expect(component.loading()).toBeFalse();
  });

  it('should format balance correctly', () => {
    expect(component.formatBalance('1234.5')).toBe('1,234.50');
    expect(component.formatBalance(0)).toBe('0.00');
    expect(component.formatBalance('100')).toBe('100.00');
  });

  it('should track items by id', () => {
    expect(component.trackById(0, mockWallet)).toBe('1');
  });

  it('should open form for editing', () => {
    component.editWallet(mockWallet);
    expect(component.showForm()).toBeTrue();
    expect(component.editingWallet()).toEqual(mockWallet);
  });

  it('should close form', () => {
    component.closeForm();
    expect(component.showForm()).toBeFalse();
    expect(component.editingWallet()).toBeNull();
  });

  it('should reload wallets on saved', () => {
    walletServiceSpy.findAll.and.returnValue(of([]));
    component.onSaved();
    expect(component.showForm()).toBeFalse();
    expect(component.editingWallet()).toBeNull();
    expect(walletServiceSpy.findAll).toHaveBeenCalled();
  });

  it('should delete wallet successfully after confirm', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    walletServiceSpy.delete.and.returnValue(of(void 0));
    walletServiceSpy.findAll.and.returnValue(of([]));

    component.deleteWallet('1');
    expect(walletServiceSpy.delete).toHaveBeenCalledWith('1');
    expect(walletServiceSpy.findAll).toHaveBeenCalled();
  });

  it('should not delete wallet if confirm is cancelled', () => {
    spyOn(window, 'confirm').and.returnValue(false);
    component.deleteWallet('1');
    expect(walletServiceSpy.delete).not.toHaveBeenCalled();
  });

  it('should show alert on delete with 409 conflict', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    spyOn(window, 'alert');
    walletServiceSpy.delete.and.returnValue(throwError(() => ({ status: 409 })));

    component.deleteWallet('1');
    expect(window.alert).toHaveBeenCalledWith(jasmine.stringMatching(/transacciones/i));
  });

  it('should show generic alert on delete error', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    spyOn(window, 'alert');
    walletServiceSpy.delete.and.returnValue(throwError(() => ({ status: 500 })));

    component.deleteWallet('1');
    expect(window.alert).toHaveBeenCalledWith(jasmine.stringMatching(/Error al eliminar/i));
  });
});
