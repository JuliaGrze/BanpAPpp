import { Component, OnInit } from '@angular/core';
import { CommonModule }          from '@angular/common';
import { TransactionService }    from '../../../services/transaction.service';
import { UserService }           from '../../../services/user.service';
import { AuthService } from '../../../auth/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-pending',
  standalone: true,
  imports: [ CommonModule ],
  templateUrl: './pending.component.html',
  styleUrls: ['./pending.component.scss']
})
export class PendingComponent implements OnInit {
  email!: string;
  allTransactions: any[] = [];
  pendingTransactions: any[] = [];
  balance = 0;

  constructor(
    private txSvc: TransactionService,
    private userSvc: UserService,
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
    this.loadBalance();
    this.loadAll();
    this.loadPending();
  }

  private loadBalance(): void {
    this.userSvc.refreshBalance()
    this.userSvc.balance$
      .subscribe({
        next: b => this.balance = b,
        error: err => console.error('Błąd pobierania salda', err)
      });
  }

  loadAll(): void {
    this.txSvc.getAll(this.email).subscribe(data => this.allTransactions = data);
  }

  loadPending(): void {
    this.txSvc.getPending(this.email).subscribe(data => this.pendingTransactions = data);
  }

  confirm(id: number): void {
    this.txSvc.confirm(id).subscribe({
      next: msg => {
        alert(msg);
        // po confirmie:
        this.loadPending();
        this.loadAll();
        this.loadBalance();       // <-- odśwież saldo
      },
      error: err => alert('Błąd confirm: ' + err.message)
    });
  }

  reject(id: number): void {
    this.txSvc.reject(id).subscribe({
      next: msg => {
        alert(msg);
        // po reject:
        this.loadPending();
        this.loadAll();
        this.loadBalance();       // <-- odśwież saldo
      },
      error: err => alert('Błąd reject: ' + err.message)
    });
  }
}
