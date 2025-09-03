// src/app/app.routes.ts
import { Routes } from '@angular/router';
import { LoginComponent } from './auth/login/login.component';
import { TransactionsLayoutComponent } from './components/transactions/layout/transactions-layout/transactions-layout.component';
import { PendingComponent } from './components/transactions/pending/pending.component';
import { TransactionsComponent } from './components/transactions/transactions/transactions.component';
import { AddFundsComponent } from './components/transactions/add-funds/add-funds.component';
import { AuthGuard } from './auth/auth.guard';

export const appRoutes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: 'transactions',
    component: TransactionsLayoutComponent,
    canActivate: [AuthGuard],
    children: [
      { path: 'pending', component: PendingComponent },
      { path: 'all', component: TransactionsComponent },
      { path: 'add', component: AddFundsComponent },
      { path: '', redirectTo: 'pending', pathMatch: 'full' }
    ]
  },
  { path: '', redirectTo: 'login', pathMatch: 'full' }
];
