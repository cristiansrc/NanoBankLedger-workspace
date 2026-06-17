import { Component, Input, OnInit, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TransactionService } from '../../../../core/services/transaction.service';
import { Transaction, CreateTransactionRequest, UpdateTransactionRequest, TransactionType } from '../../../../core/models/transaction.models';
import { Category } from '../../../../core/models/category.models';

@Component({
  selector: 'app-transaction-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './transaction-form.component.html',
  styleUrls: ['./transaction-form.component.css']
})
export class TransactionFormComponent implements OnInit {
  @Input() walletId: string | null = null;
  @Input() transaction: Transaction | null = null;
  @Input() categories: Category[] = [];

  close = output<void>();
  saved = output<void>();

  // Form state
  type: TransactionType = 'EXPENSE';
  selectedCategoryId: string = '';
  amount: string = '';
  description: string = '';
  date: string = '';

  // UI state
  saving = signal(false);
  submitError = signal<string | null>(null);

  constructor(private transactionService: TransactionService) {}

  ngOnInit(): void {
    // Set default date to today
    const today = new Date();
    this.date = today.toISOString().split('T')[0];

    if (this.transaction) {
      // Edit mode: pre-fill form
      this.type = this.transaction.type;
      this.selectedCategoryId = this.transaction.category_id || '';
      this.amount = this.transaction.amount;
      this.description = this.transaction.description || '';
      this.date = this.transaction.date;
    }
  }

  get filteredCategories(): Category[] {
    return this.categories.filter(c => c.type === this.type);
  }

  get isEditMode(): boolean {
    return this.transaction !== null;
  }

  get isFormValid(): boolean {
    if (!this.amount) return false;
    const amountNum = parseFloat(this.amount);
    if (isNaN(amountNum) || amountNum < 0.01) return false;
    const amountRegex = /^\d+(\.\d{1,2})?$/;
    if (!amountRegex.test(this.amount)) return false;
    // category_id es opcional
    if (!this.date) return false;
    return true;
  }

  get amountError(): string | null {
    if (!this.amount) return null;
    const amountNum = parseFloat(this.amount);
    if (isNaN(amountNum) || amountNum < 0.01 || !/^\d+(\.\d{1,2})?$/.test(this.amount)) {
      return 'El monto debe ser mayor a 0.01 y tener máximo 2 decimales.';
    }
    return null;
  }

  onTypeChange(newType: TransactionType): void {
    this.type = newType;
    this.selectedCategoryId = '';
  }

  onSubmit(): void {
    if (!this.isFormValid || !this.walletId) return;

    this.saving.set(true);
    this.submitError.set(null);

    if (this.isEditMode && this.transaction) {
      const req: UpdateTransactionRequest = {
        type: this.type,
        amount: this.amount,
        category_id: this.selectedCategoryId || undefined,
        description: this.description || undefined,
        date: this.date
      };

      this.transactionService.update(this.transaction.id, req).subscribe({
        next: () => this.saved.emit(),
        error: (err) => {
          this.saving.set(false);
          if (err.status === 422) {
            this.submitError.set('Saldo insuficiente en la billetera.');
          } else {
            this.submitError.set('Error al actualizar la transacción. Intenta de nuevo.');
          }
        }
      });
    } else {
      const req: CreateTransactionRequest = {
        type: this.type,
        amount: this.amount,
        category_id: this.selectedCategoryId || undefined,
        description: this.description || undefined,
        date: this.date
      };

      this.transactionService.create(this.walletId, req).subscribe({
        next: () => this.saved.emit(),
        error: (err) => {
          this.saving.set(false);
          if (err.status === 422) {
            this.submitError.set('Saldo insuficiente en la billetera.');
          } else {
            this.submitError.set('Error al crear la transacción. Intenta de nuevo.');
          }
        }
      });
    }
  }
}
