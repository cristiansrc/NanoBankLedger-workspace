import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TransactionListComponent } from './transaction-list.component';
import { TransactionService } from '../../../../core/services/transaction.service';
import { WalletService } from '../../../../core/services/wallet.service';
import { CategoryService } from '../../../../core/services/category.service';
import { Wallet } from '../../../../core/models/wallet.models';
import { Transaction } from '../../../../core/models/transaction.models';
import { Category } from '../../../../core/models/category.models';
import { of, throwError } from 'rxjs';
import { ActivatedRoute } from '@angular/router';

describe('TransactionListComponent', () => {
  let component: TransactionListComponent;
  let fixture: ComponentFixture<TransactionListComponent>;
  let transactionServiceSpy: jasmine.SpyObj<TransactionService>;
  let walletServiceSpy: jasmine.SpyObj<WalletService>;
  let categoryServiceSpy: jasmine.SpyObj<CategoryService>;

  const mockWallets: Wallet[] = [
    { id: '1', name: 'Wallet 1', type: 'SAVINGS', balance: '1000.00', user_id: '1', created_at: '', updated_at: '' },
    { id: '2', name: 'Wallet 2', type: 'CASH', balance: '500.00', user_id: '1', created_at: '', updated_at: '' }
  ];

  const mockCategories: Category[] = [
    { id: 'cat1', name: 'Comida', type: 'EXPENSE' },
    { id: 'cat2', name: 'Salario', type: 'INCOME' }
  ];

  const mockTransactions: Transaction[] = [
    {
      id: 't1', wallet_id: '1', type: 'EXPENSE', amount: '50.00',
      category_id: 'cat1', description: 'Test',
      date: new Date().toISOString(), created_at: '', updated_at: ''
    }
  ];

  beforeEach(async () => {
    transactionServiceSpy = jasmine.createSpyObj('TransactionService', [
      'findByWalletId', 'delete', 'moveToWallet'
    ]);
    walletServiceSpy = jasmine.createSpyObj('WalletService', ['findAll']);
    categoryServiceSpy = jasmine.createSpyObj('CategoryService', ['findAll']);

    await TestBed.configureTestingModule({
      imports: [TransactionListComponent],
      providers: [
        { provide: TransactionService, useValue: transactionServiceSpy },
        { provide: WalletService, useValue: walletServiceSpy },
        { provide: CategoryService, useValue: categoryServiceSpy },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => null } } } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TransactionListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    walletServiceSpy.findAll.and.returnValue(of(mockWallets));
    categoryServiceSpy.findAll.and.returnValue(of(mockCategories));
    transactionServiceSpy.findByWalletId.and.returnValue(of(mockTransactions));
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should load wallets and categories on init', () => {
    walletServiceSpy.findAll.and.returnValue(of(mockWallets));
    categoryServiceSpy.findAll.and.returnValue(of(mockCategories));
    transactionServiceSpy.findByWalletId.and.returnValue(of(mockTransactions));

    fixture.detectChanges();

    expect(walletServiceSpy.findAll).toHaveBeenCalled();
    expect(categoryServiceSpy.findAll).toHaveBeenCalled();
    expect(component.wallets().length).toBe(2);
    expect(component.categories().length).toBe(2);
  });

  it('should select first wallet and load transactions', () => {
    walletServiceSpy.findAll.and.returnValue(of(mockWallets));
    categoryServiceSpy.findAll.and.returnValue(of(mockCategories));
    transactionServiceSpy.findByWalletId.and.returnValue(of(mockTransactions));

    fixture.detectChanges();

    expect(component.selectedWalletId()).toBe('1');
    expect(transactionServiceSpy.findByWalletId).toHaveBeenCalledWith('1', {});
    expect(component.transactions().length).toBe(1);
  });

  it('should handle wallet load error', () => {
    walletServiceSpy.findAll.and.returnValue(throwError(() => ({ status: 500 })));
    categoryServiceSpy.findAll.and.returnValue(of(mockCategories));

    fixture.detectChanges();

    expect(component.error()).toContain('Error al cargar las billeteras');
  });

  it('should change wallet and reload transactions', () => {
    walletServiceSpy.findAll.and.returnValue(of(mockWallets));
    categoryServiceSpy.findAll.and.returnValue(of(mockCategories));
    transactionServiceSpy.findByWalletId.and.returnValue(of(mockTransactions));

    fixture.detectChanges();

    component.onWalletChange('2');
    expect(component.selectedWalletId()).toBe('2');
    expect(component.page()).toBe(0);
  });

  it('should apply filters and reload', () => {
    walletServiceSpy.findAll.and.returnValue(of(mockWallets));
    categoryServiceSpy.findAll.and.returnValue(of(mockCategories));
    transactionServiceSpy.findByWalletId.and.returnValue(of(mockTransactions));

    fixture.detectChanges();

    component.filters.set({ type: 'INCOME' });
    component.applyFilters();
    expect(component.page()).toBe(0);
  });

  it('should reset filters and reload', () => {
    walletServiceSpy.findAll.and.returnValue(of(mockWallets));
    categoryServiceSpy.findAll.and.returnValue(of(mockCategories));
    transactionServiceSpy.findByWalletId.and.returnValue(of(mockTransactions));

    fixture.detectChanges();

    component.filters.set({ type: 'INCOME', category_id: 'cat1' });
    component.resetFilters();
    expect(component.filters()).toEqual({});
    expect(component.page()).toBe(0);
  });

  it('should navigate pages', () => {
    component.totalPages.set(5);
    component.page.set(2);

    component.goToPage(3);
    expect(component.page()).toBe(3);

    component.goToPage(0);
    expect(component.page()).toBe(0);
  });

  it('should not navigate to invalid pages', () => {
    component.totalPages.set(5);
    component.page.set(2);

    component.goToPage(-1);
    expect(component.page()).toBe(2);

    component.goToPage(10);
    expect(component.page()).toBe(2);
  });

  it('should open and close transaction form', () => {
    component.openCreateForm();
    expect(component.showForm()).toBeTrue();
    expect(component.editingTransaction()).toBeNull();

    component.closeForm();
    expect(component.showForm()).toBeFalse();
  });

  it('should track items by id', () => {
    expect(component.trackById(0, { id: '42' })).toBe('42');
  });

  it('should format amount with currency', () => {
    expect(component.formatAmount('1234.5')).toContain('$');
    expect(component.formatAmount('1234.5')).toContain('1,234.50');
  });

  it('should format date in spanish locale', () => {
    const dateStr = '2024-06-15T12:00:00Z';
    const formatted = component.formatDate(dateStr);
    expect(formatted).toBeTruthy();
    expect(typeof formatted).toBe('string');
  });

  it('should get category name by id', () => {
    component.categories.set(mockCategories);
    expect(component.getCategoryName('cat1')).toBe('Comida');
    expect(component.getCategoryName('nonexistent')).toBe('—');
    expect(component.getCategoryName(undefined)).toBe('—');
  });

  it('should get category icon by id', () => {
    const categoriesWithIcon: Category[] = [
      { id: 'cat1', name: 'Comida', type: 'EXPENSE', icon: '🍕' }
    ];
    component.categories.set(categoriesWithIcon);
    expect(component.getCategoryIcon('cat1')).toBe('🍕');
    expect(component.getCategoryIcon('nonexistent')).toBe('');
  });

  it('should get category color by id', () => {
    const categoriesWithColor: Category[] = [
      { id: 'cat1', name: 'Comida', type: 'EXPENSE', color: '#ff0000' }
    ];
    component.categories.set(categoriesWithColor);
    expect(component.getCategoryColor('cat1')).toBe('#ff0000');
    expect(component.getCategoryColor('nonexistent')).toBe('');
  });

  it('should reload transactions on saved', () => {
    walletServiceSpy.findAll.and.returnValue(of(mockWallets));
    categoryServiceSpy.findAll.and.returnValue(of(mockCategories));
    transactionServiceSpy.findByWalletId.and.returnValue(of(mockTransactions));
    fixture.detectChanges();

    transactionServiceSpy.findByWalletId.calls.reset();
    transactionServiceSpy.findByWalletId.and.returnValue(of(mockTransactions));

    component.onSaved();
    expect(component.showForm()).toBeFalse();
    expect(transactionServiceSpy.findByWalletId).toHaveBeenCalled();
  });

  it('should edit transaction and open form', () => {
    const txn = mockTransactions[0];
    component.editTransaction(txn);
    expect(component.editingTransaction()).toEqual(txn);
    expect(component.showForm()).toBeTrue();
  });

  it('should delete transaction successfully', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    transactionServiceSpy.delete.and.returnValue(of(void 0));
    transactionServiceSpy.findByWalletId.and.returnValue(of(mockTransactions));
    walletServiceSpy.findAll.and.returnValue(of(mockWallets));
    categoryServiceSpy.findAll.and.returnValue(of(mockCategories));
    fixture.detectChanges();

    transactionServiceSpy.findByWalletId.calls.reset();
    transactionServiceSpy.findByWalletId.and.returnValue(of(mockTransactions));

    component.deleteTransaction('t1');
    expect(transactionServiceSpy.delete).toHaveBeenCalledWith('t1');
  });

  it('should not delete if confirm is cancelled', () => {
    spyOn(window, 'confirm').and.returnValue(false);
    component.deleteTransaction('t1');
    expect(transactionServiceSpy.delete).not.toHaveBeenCalled();
  });

  it('should show alert on delete with 422 error', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    spyOn(window, 'alert');
    transactionServiceSpy.delete.and.returnValue(throwError(() => ({ status: 422 })));

    component.deleteTransaction('t1');
    expect(window.alert).toHaveBeenCalledWith(jasmine.stringMatching(/saldo/i));
  });

  it('should show generic alert on delete error', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    spyOn(window, 'alert');
    transactionServiceSpy.delete.and.returnValue(throwError(() => ({ status: 500 })));

    component.deleteTransaction('t1');
    expect(window.alert).toHaveBeenCalledWith(jasmine.stringMatching(/Error al eliminar/i));
  });

  it('should open move dialog', () => {
    walletServiceSpy.findAll.and.returnValue(of(mockWallets));
    const txn = mockTransactions[0];
    component.openMoveDialog(txn);
    expect(component.movingTransactionId()).toBe('t1');
    expect(component.selectedTargetWalletId()).toBe('');
    expect(component.moveError()).toBeNull();
    expect(component.movingWalletId()).toBe('1');
    expect(walletServiceSpy.findAll).toHaveBeenCalled();
    expect(component.walletsForMove().length).toBe(1);
  });

  it('should handle move dialog load error', () => {
    walletServiceSpy.findAll.and.returnValue(throwError(() => ({ status: 500 })));
    const txn = mockTransactions[0];
    component.openMoveDialog(txn);
    expect(component.moveError()).toContain('Error al cargar billeteras');
  });

  it('should cancel move', () => {
    component.movingTransactionId.set('t1');
    component.selectedTargetWalletId.set('w2');
    component.moveError.set('some error');
    component.cancelMove();
    expect(component.movingTransactionId()).toBeNull();
    expect(component.selectedTargetWalletId()).toBe('');
    expect(component.moveError()).toBeNull();
  });

  it('should confirm move successfully', () => {
    walletServiceSpy.findAll.and.returnValue(of(mockWallets));
    categoryServiceSpy.findAll.and.returnValue(of(mockCategories));
    transactionServiceSpy.findByWalletId.and.returnValue(of(mockTransactions));
    transactionServiceSpy.moveToWallet.and.returnValue(of(mockTransactions[0]));
    fixture.detectChanges();

    component.movingTransactionId.set('t1');
    component.selectedTargetWalletId.set('w2');
    component.confirmMove();
    expect(transactionServiceSpy.moveToWallet).toHaveBeenCalledWith('t1', { target_wallet_id: 'w2' });
  });

  it('should not confirm move without required data', () => {
    component.movingTransactionId.set(null);
    component.selectedTargetWalletId.set('');
    component.confirmMove();
    expect(transactionServiceSpy.moveToWallet).not.toHaveBeenCalled();
  });

  it('should handle move with 409 error', () => {
    transactionServiceSpy.moveToWallet.and.returnValue(throwError(() => ({ status: 409 })));
    component.movingTransactionId.set('t1');
    component.selectedTargetWalletId.set('w2');
    component.confirmMove();
    expect(component.moveError()).toContain('misma billetera');
  });

  it('should handle move with 422 error', () => {
    transactionServiceSpy.moveToWallet.and.returnValue(throwError(() => ({ status: 422 })));
    component.movingTransactionId.set('t1');
    component.selectedTargetWalletId.set('w2');
    component.confirmMove();
    expect(component.moveError()).toContain('Saldo insuficiente');
  });

  it('should handle move with generic error', () => {
    transactionServiceSpy.moveToWallet.and.returnValue(throwError(() => ({ status: 500 })));
    component.movingTransactionId.set('t1');
    component.selectedTargetWalletId.set('w2');
    component.confirmMove();
    expect(component.moveError()).toContain('Error al mover');
  });

  it('should handle load transactions error', () => {
    walletServiceSpy.findAll.and.returnValue(of(mockWallets));
    categoryServiceSpy.findAll.and.returnValue(of([]));
    transactionServiceSpy.findByWalletId.and.returnValue(throwError(() => ({ status: 500 })));
    fixture.detectChanges();
    expect(component.error()).toContain('Error al cargar las transacciones');
    expect(component.loading()).toBeFalse();
  });

  it('should call loadTransactions on filter change', () => {
    walletServiceSpy.findAll.and.returnValue(of(mockWallets));
    categoryServiceSpy.findAll.and.returnValue(of(mockCategories));
    transactionServiceSpy.findByWalletId.and.returnValue(of(mockTransactions));
    fixture.detectChanges();

    spyOn(component, 'loadTransactions').and.callThrough();
    component.filters.set({ type: 'INCOME' });
    component.onFilterChange();
    expect(component.loadTransactions).toHaveBeenCalled();
  });

  it('should dismiss toast', () => {
    component.toast.set({ type: 'success', message: 'Test' });
    component.dismissToast();
    expect(component.toast()).toBeNull();
  });

  it('should handle drag start and drag end', () => {
    const txn = mockTransactions[0];
    component.onDragStartTransaction(txn);
    expect(component.draggingTransaction()).toEqual(txn);

    component.onDragEndTransaction();
    expect(component.draggingTransaction()).toBeNull();
  });

  it('should get wallet balance without optimistic updates', () => {
    const wallet = mockWallets[0];
    const balance = component.getWalletBalance(wallet);
    expect(balance).toBe('1,000.00');
  });

  it('should get wallet balance with optimistic updates', () => {
    const wallet = mockWallets[0];
    const updates = new Map<string, number>();
    updates.set('1', -50);
    component.optimisticUpdates.set(updates);
    const balance = component.getWalletBalance(wallet);
    expect(balance).toBe('950.00');
  });

  it('should check optimistic update presence', () => {
    expect(component.hasOptimisticUpdate('1')).toBeFalse();
    const updates = new Map<string, number>();
    updates.set('1', 100);
    component.optimisticUpdates.set(updates);
    expect(component.hasOptimisticUpdate('1')).toBeTrue();
  });

  it('should get optimistic delta', () => {
    expect(component.getOptimisticDelta('1')).toBe(0);
    const updates = new Map<string, number>();
    updates.set('1', 200);
    component.optimisticUpdates.set(updates);
    expect(component.getOptimisticDelta('1')).toBe(200);
  });
});
