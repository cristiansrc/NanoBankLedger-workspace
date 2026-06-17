import { Routes } from '@angular/router';
import { WalletListComponent } from './pages/wallet-list/wallet-list.component';

export const walletsRoutes: Routes = [
  { path: '', component: WalletListComponent },
  { path: ':id/transactions', loadChildren: () => import('../transactions/transactions.routes').then(m => m.transactionsRoutes) }
];
