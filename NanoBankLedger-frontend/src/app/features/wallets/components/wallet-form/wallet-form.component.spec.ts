import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WalletFormComponent } from './wallet-form.component';
import { WalletService } from '../../../../core/services/wallet.service';
import { Wallet } from '../../../../core/models/wallet.models';
import { of, throwError } from 'rxjs';

describe('WalletFormComponent', () => {
  let component: WalletFormComponent;
  let fixture: ComponentFixture<WalletFormComponent>;
  let walletServiceSpy: jasmine.SpyObj<WalletService>;

  const mockWallet: Wallet = {
    id: '1',
    name: 'Existing Wallet',
    type: 'CHECKING',
    balance: '500.00',
    user_id: '1',
    created_at: '',
    updated_at: ''
  };

  beforeEach(async () => {
    walletServiceSpy = jasmine.createSpyObj('WalletService', ['create', 'update']);

    await TestBed.configureTestingModule({
      imports: [WalletFormComponent],
      providers: [
        { provide: WalletService, useValue: walletServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(WalletFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have default form data', () => {
    expect(component.formData.name).toBe('');
    expect(component.formData.type).toBe('SAVINGS');
  });

  it('should pre-fill form data when editing existing wallet', () => {
    component.wallet = mockWallet;
    component.ngOnInit();

    expect(component.formData.name).toBe('Existing Wallet');
    expect(component.formData.type).toBe('CHECKING');
  });

  it('should not submit if name is empty', () => {
    component.formData.name = '';
    component.onSubmit();
    expect(walletServiceSpy.create).not.toHaveBeenCalled();
    expect(walletServiceSpy.update).not.toHaveBeenCalled();
  });

  it('should call walletService.create on submit for new wallet', () => {
    const createdWallet: Wallet = { id: '1', name: 'New Wallet', type: 'SAVINGS', balance: '0.00', user_id: '1', created_at: '', updated_at: '' };
    walletServiceSpy.create.and.returnValue(of(createdWallet));

    component.formData.name = 'New Wallet';
    component.formData.type = 'SAVINGS';
    component.onSubmit();

    expect(walletServiceSpy.create).toHaveBeenCalledWith(
      jasmine.objectContaining({ name: 'New Wallet', type: 'SAVINGS' })
    );
  });

  it('should call walletService.update on submit for existing wallet', () => {
    component.wallet = mockWallet;
    component.ngOnInit();

    const updatedWallet: Wallet = { ...mockWallet, name: 'Updated Wallet' };
    walletServiceSpy.update.and.returnValue(of(updatedWallet));

    component.formData.name = 'Updated Wallet';
    component.onSubmit();

    expect(walletServiceSpy.update).toHaveBeenCalledWith('1', { name: 'Updated Wallet', type: 'CHECKING' });
  });

  it('should show error on create with 409', () => {
    walletServiceSpy.create.and.returnValue(throwError(() => ({ status: 409 })));

    component.formData.name = 'Duplicate';
    component.onSubmit();

    expect(component.submitError()).toContain('Ya existe una billetera con ese nombre');
    expect(component.saving()).toBeFalse();
  });

  it('should show generic error on create failure', () => {
    walletServiceSpy.create.and.returnValue(throwError(() => ({ status: 500 })));

    component.formData.name = 'New Wallet';
    component.onSubmit();

    expect(component.submitError()).toContain('Error al crear');
    expect(component.saving()).toBeFalse();
  });

  it('should show error on update failure', () => {
    component.wallet = mockWallet;
    component.ngOnInit();

    walletServiceSpy.update.and.returnValue(throwError(() => ({ status: 500 })));

    component.formData.name = 'Updated';
    component.onSubmit();

    expect(component.submitError()).toContain('Error al actualizar');
    expect(component.saving()).toBeFalse();
  });

  it('should emit close event', () => {
    spyOn(component.close, 'emit');
    component.close.emit();
    expect(component.close.emit).toHaveBeenCalled();
  });

  it('should emit saved event on successful submit', () => {
    const createdWallet: Wallet = { id: '1', name: 'New', type: 'SAVINGS', balance: '0.00', user_id: '1', created_at: '', updated_at: '' };
    walletServiceSpy.create.and.returnValue(of(createdWallet));
    spyOn(component.saved, 'emit');

    component.formData.name = 'New';
    component.onSubmit();

    expect(component.saved.emit).toHaveBeenCalled();
  });
});
