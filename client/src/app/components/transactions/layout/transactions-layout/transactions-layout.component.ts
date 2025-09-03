// src/app/transactions/layout/transactions-layout.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule }       from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { UserService }        from '../../../../services/user.service';
import { AuthService } from '../../../../auth/auth.service';

@Component({
  selector: 'app-transactions-layout',
  standalone: true,
  imports: [ CommonModule, RouterModule ],
  templateUrl: './transactions-layout.component.html',
  styleUrls:   ['./transactions-layout.component.scss']
})
export class TransactionsLayoutComponent implements OnInit {
  balance = 0;

  constructor(
    private userSvc: UserService,
    private auth: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadBalance();
  }

  loadBalance(): void {
  this.userSvc.balance$
    .subscribe({
      next: (b: number) => this.balance = b,
      error: err => console.error('Błąd przy pobieraniu salda:', err)
    })};

  goAdd() {
    this.router.navigate(['transactions','add']);
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
