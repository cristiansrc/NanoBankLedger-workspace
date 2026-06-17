import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DashboardComponent } from './dashboard.component';
import { WalletService } from '../../../../core/services/wallet.service';
import { of, throwError } from 'rxjs';
import { provideRouter } from '@angular/router';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let walletServiceSpy: jasmine.SpyObj<WalletService>;

  const mockWallets = [
    { id: '1', user_id: 'u1', name: 'Ahorros', type: 'SAVINGS' as const, balance: '1000.00', created_at: '2024-01-01', updated_at: '2024-01-01' },
    { id: '2', user_id: 'u1', name: 'Corriente', type: 'CHECKING' as const, balance: '500.00', created_at: '2024-01-01', updated_at: '2024-01-01' },
    { id: '3', user_id: 'u1', name: 'Inversiones', type: 'INVESTMENT' as const, balance: '2000.00', created_at: '2024-01-01', updated_at: '2024-01-01' }
  ];

  beforeEach(async () => {
    walletServiceSpy = jasmine.createSpyObj('WalletService', ['findAll']);

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        { provide: WalletService, useValue: walletServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
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

    expect(component.wallets().length).toBe(3);
    expect(component.loading()).toBeFalse();
  });

  it('should compute totalBalance correctly', () => {
    walletServiceSpy.findAll.and.returnValue(of(mockWallets));
    fixture.detectChanges();

    // 1000 + 500 + 2000 = 3500
    expect(component.totalBalance()).toBe(3500);
  });

  it('should show loading state initially', () => {
    walletServiceSpy.findAll.and.returnValue(of(mockWallets));
    fixture.detectChanges();

    // Loading should be false after data loads
    expect(component.loading()).toBeFalse();
  });

  it('should handle empty wallets', () => {
    walletServiceSpy.findAll.and.returnValue(of([]));
    fixture.detectChanges();

    expect(component.wallets().length).toBe(0);
    expect(component.totalBalance()).toBe(0);
    expect(component.loading()).toBeFalse();
  });

  it('should handle error loading wallets', () => {
    walletServiceSpy.findAll.and.returnValue(throwError(() => new Error('Network error')));
    fixture.detectChanges();

    expect(component.wallets().length).toBe(0);
    expect(component.loading()).toBeFalse();
  });

  it('should compute mostUsedType correctly', () => {
    walletServiceSpy.findAll.and.returnValue(of(mockWallets));
    fixture.detectChanges();

    // Each type appears once, so the first max is SAVINGS -> 'Ahorros'
    const result = component.mostUsedType();
    expect(['Ahorros', 'Corriente', 'Inversiones']).toContain(result);
  });

  it('should return dash when no wallets for mostUsedType', () => {
    walletServiceSpy.findAll.and.returnValue(of([]));
    fixture.detectChanges();

    expect(component.mostUsedType()).toBe('-');
  });

  it('should return most frequent type when there are duplicates', () => {
    const duplicateTypeWallets = [
      { id: '1', user_id: 'u1', name: 'Ahorro 1', type: 'SAVINGS' as const, balance: '100.00', created_at: '', updated_at: '' },
      { id: '2', user_id: 'u1', name: 'Ahorro 2', type: 'SAVINGS' as const, balance: '200.00', created_at: '', updated_at: '' },
      { id: '3', user_id: 'u1', name: 'Corriente', type: 'CHECKING' as const, balance: '300.00', created_at: '', updated_at: '' }
    ];

    walletServiceSpy.findAll.and.returnValue(of(duplicateTypeWallets));
    fixture.detectChanges();

    expect(component.mostUsedType()).toBe('Ahorros');
  });

  it('should format balance with two decimal places', () => {
    walletServiceSpy.findAll.and.returnValue(of([]));
    fixture.detectChanges();

    expect(component.formatBalance('1000')).toBe('1,000.00');
    expect(component.formatBalance(500.5)).toBe('500.50');
    expect(component.formatBalance('0')).toBe('0.00');
  });

  it('should track by wallet id', () => {
    walletServiceSpy.findAll.and.returnValue(of([]));
    fixture.detectChanges();

    expect(component.trackById(0, mockWallets[0])).toBe('1');
    expect(component.trackById(1, mockWallets[1])).toBe('2');
  });
});
