import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { WalletService } from '../../../../core/services/wallet.service';
import { Wallet } from '../../../../core/models/wallet.models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  wallets = signal<Wallet[]>([]);
  loading = signal(true);

  totalBalance = computed(() => {
    return this.wallets().reduce((sum, w) => sum + parseFloat(w.balance), 0);
  });

  mostUsedType = computed(() => {
    const counts: Record<string, number> = {};
    this.wallets().forEach(w => { counts[w.type] = (counts[w.type] || 0) + 1; });
    const max = Math.max(...Object.values(counts), 0);
    const type = Object.keys(counts).find(k => counts[k] === max);
    const labels: Record<string, string> = { SAVINGS: 'Ahorros', CHECKING: 'Corriente', INVESTMENT: 'Inversiones', CASH: 'Efectivo' };
    return type ? labels[type] || type : '-';
  });

  constructor(private walletService: WalletService) {}

  ngOnInit(): void {
    this.walletService.findAll().subscribe({
      next: (data) => { this.wallets.set(data); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  trackById(_index: number, wallet: Wallet): string {
    return wallet.id;
  }

  formatBalance(balance: string | number): string {
    const num = typeof balance === 'string' ? parseFloat(balance) : balance;
    return num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
}
