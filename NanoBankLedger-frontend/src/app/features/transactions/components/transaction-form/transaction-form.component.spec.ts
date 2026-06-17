import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TransactionFormComponent } from './transaction-form.component';
import { TransactionService } from '../../../../core/services/transaction.service';
import { Transaction } from '../../../../core/models/transaction.models';
import { Category } from '../../../../core/models/category.models';
import { of, throwError } from 'rxjs';
import { FormsModule } from '@angular/forms';

describe('TransactionFormComponent', () => {
  let component: TransactionFormComponent;
  let fixture: ComponentFixture<TransactionFormComponent>;
  let transactionServiceSpy: jasmine.SpyObj<TransactionService>;

  const mockCategories: Category[] = [
    { id: 'cat1', name: 'Comida', type: 'EXPENSE' },
    { id: 'cat2', name: 'Salario', type: 'INCOME' },
    { id: 'cat3', name: 'Transporte', type: 'EXPENSE' }
  ];

  const mockTransaction: Transaction = {
    id: 't1',
    wallet_id: 'w1',
    type: 'EXPENSE',
    amount: '50.00',
    category_id: 'cat1',
    description: 'Test transaction',
    date: '2024-06-15',
    created_at: '',
    updated_at: ''
  };

  beforeEach(async () => {
    transactionServiceSpy = jasmine.createSpyObj('TransactionService', ['create', 'update']);

    await TestBed.configureTestingModule({
      imports: [TransactionFormComponent, FormsModule],
      providers: [
        { provide: TransactionService, useValue: transactionServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TransactionFormComponent);
    component = fixture.componentInstance;
    component.walletId = 'w1';
    component.categories = mockCategories;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with default EXPENSE type and today date', () => {
    expect(component.type).toBe('EXPENSE');
    expect(component.date).toBeTruthy();
    expect(component.amount).toBe('');
    expect(component.description).toBe('');
  });

  it('should have isEditMode false when no transaction input', () => {
    expect(component.isEditMode).toBeFalse();
  });

  it('should have isEditMode true when transaction input is provided', () => {
    component.transaction = mockTransaction;
    component.ngOnInit();
    expect(component.isEditMode).toBeTrue();
  });

  it('should pre-fill form when editing', () => {
    component.transaction = mockTransaction;
    component.ngOnInit();

    expect(component.type).toBe('EXPENSE');
    expect(component.selectedCategoryId).toBe('cat1');
    expect(component.amount).toBe('50.00');
    expect(component.description).toBe('Test transaction');
    expect(component.date).toBe('2024-06-15');
  });

  it('should filter categories by type', () => {
    component.type = 'EXPENSE';
    const expenseCategories = component.filteredCategories;
    expect(expenseCategories.length).toBe(2);
    expect(expenseCategories.every(c => c.type === 'EXPENSE')).toBeTrue();

    component.type = 'INCOME';
    const incomeCategories = component.filteredCategories;
    expect(incomeCategories.length).toBe(1);
    expect(incomeCategories[0].name).toBe('Salario');
  });

  it('should clear selected category on type change', () => {
    component.selectedCategoryId = 'cat1';
    component.onTypeChange('INCOME');
    expect(component.type).toBe('INCOME');
    expect(component.selectedCategoryId).toBe('');
  });

  it('should validate form - amount is required', () => {
    component.amount = '';
    expect(component.isFormValid).toBeFalse();
  });

  it('should validate form - amount must be at least 0.01', () => {
    component.amount = '0';
    expect(component.isFormValid).toBeFalse();

    component.amount = '0.001';
    expect(component.isFormValid).toBeFalse();
  });

  it('should validate form - amount must have max 2 decimals', () => {
    component.amount = '10.999';
    expect(component.isFormValid).toBeFalse();
  });

  it('should validate form - category is required', () => {
    component.amount = '50.00';
    component.selectedCategoryId = '';
    expect(component.isFormValid).toBeFalse();
  });

  it('should validate form - date is required', () => {
    component.amount = '50.00';
    component.selectedCategoryId = 'cat1';
    component.date = '';
    expect(component.isFormValid).toBeFalse();
  });

  it('should validate form - valid when all fields are set', () => {
    component.amount = '50.00';
    component.selectedCategoryId = 'cat1';
    component.date = '2024-06-15';
    expect(component.isFormValid).toBeTrue();
  });

  it('should show amount error message for invalid amounts', () => {
    component.amount = '';
    expect(component.amountError).toBeNull();

    component.amount = 'abc';
    expect(component.amountError).toContain('monto');

    component.amount = '0';
    expect(component.amountError).toContain('monto');

    component.amount = '50.00';
    expect(component.amountError).toBeNull();
  });

  it('should not submit if form is invalid', () => {
    component.amount = '';
    component.onSubmit();
    expect(transactionServiceSpy.create).not.toHaveBeenCalled();
    expect(transactionServiceSpy.update).not.toHaveBeenCalled();
  });

  it('should not submit if walletId is null', () => {
    component.walletId = null;
    component.amount = '50.00';
    component.selectedCategoryId = 'cat1';
    component.date = '2024-06-15';
    component.onSubmit();
    expect(transactionServiceSpy.create).not.toHaveBeenCalled();
    expect(transactionServiceSpy.update).not.toHaveBeenCalled();
  });

  it('should call create for new transaction', () => {
    const mockResponse = { ...mockTransaction };
    transactionServiceSpy.create.and.returnValue(of(mockResponse));
    spyOn(component.saved, 'emit');

    component.amount = '50.00';
    component.selectedCategoryId = 'cat1';
    component.type = 'EXPENSE';
    component.description = 'Test';
    component.date = '2024-06-15';
    component.onSubmit();

    expect(transactionServiceSpy.create).toHaveBeenCalledWith('w1', {
      type: 'EXPENSE',
      amount: '50.00',
      category_id: 'cat1',
      description: 'Test',
      date: '2024-06-15'
    });
  });

  it('should call update for existing transaction', () => {
    component.transaction = mockTransaction;
    component.ngOnInit();
    transactionServiceSpy.update.and.returnValue(of(mockTransaction));
    spyOn(component.saved, 'emit');

    component.amount = '75.00';
    component.onSubmit();

    expect(transactionServiceSpy.update).toHaveBeenCalledWith('t1', {
      type: 'EXPENSE',
      amount: '75.00',
      category_id: 'cat1',
      description: 'Test transaction',
      date: '2024-06-15'
    });
  });

  it('should show error on create with 422', () => {
    transactionServiceSpy.create.and.returnValue(throwError(() => ({ status: 422 })));

    component.amount = '50.00';
    component.selectedCategoryId = 'cat1';
    component.date = '2024-06-15';
    component.onSubmit();

    expect(component.submitError()).toContain('Saldo insuficiente');
    expect(component.saving()).toBeFalse();
  });

  it('should show generic error on create failure', () => {
    transactionServiceSpy.create.and.returnValue(throwError(() => ({ status: 500 })));

    component.amount = '50.00';
    component.selectedCategoryId = 'cat1';
    component.date = '2024-06-15';
    component.onSubmit();

    expect(component.submitError()).toContain('Error al crear');
    expect(component.saving()).toBeFalse();
  });

  it('should show error on update with 422', () => {
    component.transaction = mockTransaction;
    component.ngOnInit();
    transactionServiceSpy.update.and.returnValue(throwError(() => ({ status: 422 })));

    component.amount = '99999.00';
    component.onSubmit();

    expect(component.submitError()).toContain('Saldo insuficiente');
    expect(component.saving()).toBeFalse();
  });

  it('should show generic error on update failure', () => {
    component.transaction = mockTransaction;
    component.ngOnInit();
    transactionServiceSpy.update.and.returnValue(throwError(() => ({ status: 500 })));

    component.amount = '75.00';
    component.onSubmit();

    expect(component.submitError()).toContain('Error al actualizar');
    expect(component.saving()).toBeFalse();
  });

  it('should emit saved on successful create', () => {
    transactionServiceSpy.create.and.returnValue(of(mockTransaction));
    spyOn(component.saved, 'emit');

    component.amount = '50.00';
    component.selectedCategoryId = 'cat1';
    component.date = '2024-06-15';
    component.onSubmit();

    expect(component.saved.emit).toHaveBeenCalled();
  });
});
