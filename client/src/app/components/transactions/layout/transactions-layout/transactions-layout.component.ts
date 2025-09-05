// src/app/transactions/layout/transactions-layout.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule }       from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { UserService }        from '../../../../services/user.service';
import { AuthService } from '../../../../auth/auth.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-transactions-layout',
  standalone: true,
  imports: [ CommonModule, RouterModule ],
  templateUrl: './transactions-layout.component.html',
  styleUrls:   ['./transactions-layout.component.scss']
})
export class TransactionsLayoutComponent {
  balance$!: Observable<number>; 

  constructor(
    private userSvc: UserService,
    private auth: AuthService,
    private router: Router
  ) {
    this.balance$ = this.userSvc.balance$;  // przypisanie po wstrzyknięciu
  }

  goAdd() {
    this.router.navigate(['transactions','add']);
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}

