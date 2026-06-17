import { Routes } from '@angular/router';
import { TransactionListComponent } from './pages/transaction-list/transaction-list.component';

export const transactionsRoutes: Routes = [
  { path: '', component: TransactionListComponent }
];
