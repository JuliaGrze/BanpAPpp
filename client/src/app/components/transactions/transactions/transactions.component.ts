import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';              // <-- import
import { TransactionService } from '../../../services/transaction.service';
import { AuthService } from '../../../auth/auth.service';

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './transactions.component.html',
  styleUrls: ['./transactions.component.scss']
})
export class TransactionsComponent implements OnInit {
  email!: string;
  allTransactions: any[] = [];
  pendingTransactions: any[] = [];

  constructor(
    private txSvc: TransactionService,
    private router: Router,     
    private auth: AuthService                     
  ) {}

  ngOnInit(): void {
    const e = this.auth.getEmail();
    if (!e) {
      this.router.navigate(['/login']);
      return;
    }

    this.email = e;
    this.checkPendingAndRedirect();
  }

  private checkPendingAndRedirect(): void {
    this.txSvc.getPending(this.email).subscribe(pending => {
      this.pendingTransactions = pending;

      if (pending.length > 0) {
        // jeśli są pendingi, przejdź do widoku pending
        this.router.navigate(['/transactions/pending']);
      } else {
        // w przeciwnym razie pobierz wszystkie
        this.loadAll();
      }
    });
  }

  loadAll(): void {
    this.txSvc.getAll(this.email).subscribe(data => {
      this.allTransactions = data;
    });
  }

  confirm(id: number): void {
    this.txSvc.confirm(id).subscribe(response => {
      alert(response);
      this.checkPendingAndRedirect();  // po confirm/reject znów sprawdź pending
    });
  }

  reject(id: number): void {
    this.txSvc.reject(id).subscribe(response => {
      alert(response);
      this.checkPendingAndRedirect();  // po confirm/reject znów sprawdź pending
    });
  }
}
