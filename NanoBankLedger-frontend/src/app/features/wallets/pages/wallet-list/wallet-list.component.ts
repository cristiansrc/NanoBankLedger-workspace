import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { WalletService } from '../../../../core/services/wallet.service';
import { Wallet } from '../../../../core/models/wallet.models';
import { WalletFormComponent } from '../../components/wallet-form/wallet-form.component';

@Component({
  selector: 'app-wallet-list',
  standalone: true,
  imports: [CommonModule, RouterLink, WalletFormComponent],
  templateUrl: './wallet-list.component.html',
  styleUrls: ['./wallet-list.component.css']
})
export class WalletListComponent implements OnInit {
  wallets = signal<Wallet[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  showForm = signal(false);
  editingWallet = signal<Wallet | null>(null);

  constructor(private walletService: WalletService) {}

  ngOnInit(): void {
    this.loadWallets();
  }

  loadWallets(): void {
    this.loading.set(true);
    this.error.set(null);
    this.walletService.findAll().subscribe({
      next: (data) => { this.wallets.set(data); this.loading.set(false); },
      error: () => { this.error.set('Error al cargar billeteras'); this.loading.set(false); }
    });
  }

  editWallet(wallet: Wallet): void {
    this.editingWallet.set(wallet);
    this.showForm.set(true);
  }

  deleteWallet(wallet: Wallet): void {
    if (!confirm(`¿Estás seguro de eliminar "${wallet.name}"?`)) return;
    this.error.set(null);
    this.walletService.delete(wallet.id).subscribe({
      next: () => this.loadWallets(),
      error: (err) => {
        if (err.status === 409) {
          this.error.set(`No se puede eliminar "${wallet.name}" porque tiene transacciones registradas. Primero elimina las transacciones de esta billetera.`);
        } else {
          this.error.set('Error al eliminar la billetera. Intenta de nuevo.');
        }
      }
    });
  }

  closeForm(): void {
    this.showForm.set(false);
    this.editingWallet.set(null);
  }

  onSaved(): void {
    this.closeForm();
    this.loadWallets();
  }

  trackById(_index: number, wallet: Wallet): string {
    return wallet.id;
  }

  formatBalance(balance: string | number): string {
    const num = typeof balance === 'string' ? parseFloat(balance) : balance;
    return num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
}
