import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { TransactionService } from '../../../../core/services/transaction.service';
import { WalletService } from '../../../../core/services/wallet.service';
import { CategoryService } from '../../../../core/services/category.service';
import { Wallet } from '../../../../core/models/wallet.models';
import { Transaction, TransactionFilters, TransactionType } from '../../../../core/models/transaction.models';
import { Category } from '../../../../core/models/category.models';
import { TransactionFormComponent } from '../../components/transaction-form/transaction-form.component';
import { DraggableDirective, DroppableDirective } from '../../../../shared/directives';

interface ToastMessage {
  type: 'success' | 'error' | 'info';
  message: string;
}

@Component({
  selector: 'app-transaction-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, TransactionFormComponent, DraggableDirective, DroppableDirective],
  templateUrl: './transaction-list.component.html',
  styleUrls: ['./transaction-list.component.css']
})
export class TransactionListComponent implements OnInit {
  // Data signals
  wallets = signal<Wallet[]>([]);
  selectedWalletId = signal<string | null>(null);
  transactions = signal<Transaction[]>([]);
  categories = signal<Category[]>([]);
  walletsForMove = signal<Wallet[]>([]);

  // Filter signals
  filters = signal<TransactionFilters>({});
  page = signal(0);
  size = signal(20);
  totalPages = signal(0);
  totalElements = signal(0);

  // UI state signals
  loading = signal(false);
  error = signal<string | null>(null);
  showForm = signal(false);
  editingTransaction = signal<Transaction | null>(null);

  // Computed
  get selectedWallet(): Wallet | undefined {
    return this.wallets().find(w => w.id === this.selectedWalletId());
  }

  // Move transaction (modal) state
  movingTransactionId = signal<string | null>(null);
  selectedTargetWalletId = signal<string>('');
  moveError = signal<string | null>(null);
  movingWalletId = signal<string | null>(null);

  // Drag & Drop optimistic UI
  optimisticUpdates = signal<Map<string, number>>(new Map());
  draggingTransaction = signal<Transaction | null>(null);
  toast = signal<ToastMessage | null>(null);

  constructor(
    private transactionService: TransactionService,
    private walletService: WalletService,
    private categoryService: CategoryService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    // Check if there's a wallet ID in the route params
    const walletIdFromRoute = this.route.snapshot.paramMap.get('id');
    if (walletIdFromRoute) {
      this.selectedWalletId.set(walletIdFromRoute);
    }
    this.loadWallets();
    this.loadCategories();
  }

  loadWallets(): void {
    this.walletService.findAll().subscribe({
      next: (data) => {
        this.wallets.set(data);
        // Si no hay wallet seleccionada aún y hay wallets, seleccionar la primera
        if (!this.selectedWalletId() && data.length > 0) {
          this.selectedWalletId.set(data[0].id);
        }
        // Si hay wallet seleccionada (desde ruta o por defecto), cargar sus transacciones
        if (this.selectedWalletId()) {
          this.loadTransactions();
        }
      },
      error: () => {
        this.error.set('Error al cargar las billeteras');
      }
    });
  }

  loadCategories(): void {
    this.categoryService.findAll().subscribe({
      next: (data) => this.categories.set(data),
      error: () => {}
    });
  }

  onWalletChange(walletId: string): void {
    this.selectedWalletId.set(walletId);
    this.page.set(0);
    this.loadTransactions();
  }

  loadTransactions(): void {
    const walletId = this.selectedWalletId();
    if (!walletId) return;

    this.loading.set(true);
    this.error.set(null);

    const currentFilters = this.filters();
    this.transactionService.findByWalletId(walletId, currentFilters).subscribe({
      next: (response) => {
        this.transactions.set(response);
        this.totalPages.set(0);
        this.totalElements.set(0);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar las transacciones');
        this.loading.set(false);
      }
    });
  }

  applyFilters(): void {
    this.page.set(0);
    this.loadTransactions();
  }

  resetFilters(): void {
    this.filters.set({});
    this.page.set(0);
    this.loadTransactions();
  }

  onFilterChange(): void {
    this.applyFilters();
  }

  goToPage(newPage: number): void {
    if (newPage < 0 || newPage >= this.totalPages()) return;
    this.page.set(newPage);
    this.loadTransactions();
  }

  // Transaction form actions
  openCreateForm(): void {
    this.editingTransaction.set(null);
    this.showForm.set(true);
  }

  editTransaction(transaction: Transaction): void {
    this.editingTransaction.set(transaction);
    this.showForm.set(true);
  }

  closeForm(): void {
    this.showForm.set(false);
    this.editingTransaction.set(null);
  }

  onSaved(): void {
    this.closeForm();
    this.loadTransactions();
    this.loadWallets();
  }

  // Delete transaction
  deleteTransaction(id: string): void {
    if (!confirm('¿Estás seguro de eliminar esta transacción?')) return;
    this.transactionService.delete(id).subscribe({
      next: () => {
        this.loadTransactions();
        this.loadWallets();
      },
      error: (err) => {
        if (err.status === 422) {
          alert('No se puede eliminar: saldo insuficiente en la billetera.');
        } else {
          alert('Error al eliminar la transacción.');
        }
      }
    });
  }

  // Move transaction
  openMoveDialog(transaction: Transaction): void {
    this.movingTransactionId.set(transaction.id);
    this.selectedTargetWalletId.set('');
    this.moveError.set(null);
    this.movingWalletId.set(transaction.wallet_id);

    // Load wallets for the move dropdown (filter out current wallet)
    this.walletService.findAll().subscribe({
      next: (data) => {
        this.walletsForMove.set(data.filter(w => w.id !== transaction.wallet_id));
      },
      error: () => {
        this.moveError.set('Error al cargar billeteras disponibles');
      }
    });
  }

  confirmMove(): void {
    const transactionId = this.movingTransactionId();
    const targetWalletId = this.selectedTargetWalletId();
    if (!transactionId || !targetWalletId) return;

    this.moveError.set(null);
      this.transactionService.moveToWallet(transactionId, { target_wallet_id: targetWalletId }).subscribe({
      next: () => {
        this.movingTransactionId.set(null);
        this.selectedTargetWalletId.set('');
        this.loadTransactions();
        this.loadWallets();
      },
      error: (err) => {
        if (err.status === 409) {
          this.moveError.set('No se puede mover a la misma billetera.');
        } else if (err.status === 422) {
          this.moveError.set('Saldo insuficiente en la billetera de origen o destino.');
        } else {
          this.moveError.set('Error al mover la transacción.');
        }
      }
    });
  }

  cancelMove(): void {
    this.movingTransactionId.set(null);
    this.selectedTargetWalletId.set('');
    this.moveError.set(null);
  }

  // Drag & Drop handlers
  onDragStartTransaction(transaction: Transaction): void {
    this.draggingTransaction.set(transaction);
  }

  onDragEndTransaction(): void {
    this.draggingTransaction.set(null);
  }

  onDropTransaction(event: DragEvent, targetWalletId: string): void {
    try {
      const rawData = event.dataTransfer?.getData('application/json');
      if (!rawData) return;

      const transaction: Transaction = JSON.parse(rawData);
      const sourceWalletId = transaction.wallet_id;

      // Prevent drop on same wallet
      if (sourceWalletId === targetWalletId) {
        this.showToast('info', 'La transacción ya está en esta billetera');
        return;
      }

      const amount = parseFloat(transaction.amount);
      if (isNaN(amount) || amount <= 0) return;

      // Calculate balance deltas for optimistic UI
      // Source: reverts the original effect
      // Target: applies the effect in the new wallet
      let sourceDelta: number;
      let targetDelta: number;

      if (transaction.type === 'INCOME') {
        // INCOME originally increased source balance
        // Removing it from source: decrease
        // Adding to target: increase
        sourceDelta = -amount;
        targetDelta = +amount;
      } else {
        // EXPENSE originally decreased source balance
        // Removing it from source: increase
        // Adding to target: decrease
        sourceDelta = +amount;
        targetDelta = -amount;
      }

      // Apply optimistic UI update
      const currentUpdates = this.optimisticUpdates();
      const newUpdates = new Map(currentUpdates);
      // Store cumulative delta for source
      const existingSource = newUpdates.get(sourceWalletId) || 0;
      newUpdates.set(sourceWalletId, existingSource + sourceDelta);
      // Store cumulative delta for target
      const existingTarget = newUpdates.get(targetWalletId) || 0;
      newUpdates.set(targetWalletId, existingTarget + targetDelta);
      this.optimisticUpdates.set(newUpdates);

      this.showToast('info', 'Moviendo transacción...');

      // Call API
      this.transactionService.moveToWallet(transaction.id, { target_wallet_id: targetWalletId }).subscribe({
        next: () => {
          // Success: clear optimistic updates for these wallets
          const successUpdates = this.optimisticUpdates();
          const clearedUpdates = new Map(successUpdates);
          clearedUpdates.delete(sourceWalletId);
          clearedUpdates.delete(targetWalletId);
          this.optimisticUpdates.set(clearedUpdates);
          this.draggingTransaction.set(null);

          this.showToast('success', 'Transacción movida exitosamente');

          // Refresh data
          this.loadTransactions();
          this.loadWallets();
        },
        error: (err) => {
          // Error: revert optimistic update
          const errorUpdates = this.optimisticUpdates();
          const revertedUpdates = new Map(errorUpdates);
          const currentSource = revertedUpdates.get(sourceWalletId) || 0;
          const currentTarget = revertedUpdates.get(targetWalletId) || 0;
          revertedUpdates.set(sourceWalletId, currentSource - sourceDelta);
          revertedUpdates.set(targetWalletId, currentTarget - targetDelta);
          // Clean up zero deltas
          if (revertedUpdates.get(sourceWalletId) === 0) revertedUpdates.delete(sourceWalletId);
          if (revertedUpdates.get(targetWalletId) === 0) revertedUpdates.delete(targetWalletId);
          this.optimisticUpdates.set(revertedUpdates);
          this.draggingTransaction.set(null);

          // Show error message based on status code
          if (err.status === 409) {
            this.showToast('error', 'No puedes mover una transacción a la misma billetera');
          } else if (err.status === 422) {
            this.showToast('error', 'La billetera destino no tiene saldo suficiente');
          } else {
            this.showToast('error', 'Error al mover la transacción');
          }
        }
      });
    } catch {
      this.showToast('error', 'Error al procesar la transacción arrastrada');
    }
  }

  getWalletBalance(wallet: Wallet): string {
    const baseBalance = parseFloat(wallet.balance);
    const deltas = this.optimisticUpdates();
    const delta = deltas.get(wallet.id) || 0;
    const adjustedBalance = baseBalance + delta;
    return adjustedBalance.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  hasOptimisticUpdate(walletId: string): boolean {
    return this.optimisticUpdates().has(walletId);
  }

  getOptimisticDelta(walletId: string): number {
    return this.optimisticUpdates().get(walletId) || 0;
  }

  // Toast notification
  private showToast(type: ToastMessage['type'], message: string): void {
    this.toast.set({ type, message });
    setTimeout(() => {
      this.toast.set(null);
    }, 4000);
  }

  dismissToast(): void {
    this.toast.set(null);
  }

  formatAmount(amount: string | number): string {
    const num = typeof amount === 'string' ? parseFloat(amount) : amount;
    return '$' + num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  formatDate(date: string): string {
    const d = new Date(date);
    return d.toLocaleDateString('es-ES', { year: 'numeric', month: 'short', day: 'numeric' });
  }

  getCategoryName(categoryId?: string): string {
    if (!categoryId) return '—';
    const cat = this.categories().find(c => c.id === categoryId);
    return cat ? cat.name : '—';
  }

  getCategoryIcon(categoryId?: string): string {
    if (!categoryId) return '';
    const cat = this.categories().find(c => c.id === categoryId);
    return cat?.icon || '';
  }

  getCategoryColor(categoryId?: string): string {
    if (!categoryId) return '';
    const cat = this.categories().find(c => c.id === categoryId);
    return cat?.color || '';
  }

  trackById(_index: number, item: { id: string }): string {
    return item.id;
  }
}
