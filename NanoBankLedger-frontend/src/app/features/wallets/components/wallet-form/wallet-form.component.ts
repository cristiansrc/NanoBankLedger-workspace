import { Component, Input, OnInit, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WalletService } from '../../../../core/services/wallet.service';
import { Wallet, WalletType, CreateWalletRequest, UpdateWalletRequest } from '../../../../core/models/wallet.models';

@Component({
  selector: 'app-wallet-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './wallet-form.component.html',
  styleUrls: ['./wallet-form.component.css']
})
export class WalletFormComponent implements OnInit {
  @Input() wallet: Wallet | null = null;
  close = output<void>();
  saved = output<void>();

  formData: { name: string; type: WalletType; initial_balance?: string } = { name: '', type: 'SAVINGS', initial_balance: '' };
  saving = signal(false);
  submitError = signal<string | null>(null);

  constructor(private walletService: WalletService) {}

  ngOnInit(): void {
    if (this.wallet) {
      this.formData.name = this.wallet.name;
      this.formData.type = this.wallet.type;
    }
  }

  onSubmit(): void {
    if (!this.formData.name) return;
    this.saving.set(true);
    this.submitError.set(null);

    if (this.wallet) {
      const req: UpdateWalletRequest = { name: this.formData.name, type: this.formData.type };
      this.walletService.update(this.wallet.id, req).subscribe({
        next: () => this.saved.emit(),
        error: () => { this.submitError.set('Error al actualizar. Intenta de nuevo.'); this.saving.set(false); }
      });
    } else {
      const req: CreateWalletRequest = {
        name: this.formData.name,
        type: this.formData.type,
        initial_balance: this.formData.initial_balance || undefined
      };
      this.walletService.create(req).subscribe({
        next: () => this.saved.emit(),
        error: (err) => {
          if (err.status === 409) this.submitError.set('Ya existe una billetera con ese nombre.');
          else this.submitError.set('Error al crear. Intenta de nuevo.');
          this.saving.set(false);
        }
      });
    }
  }
}
