// src/app/transactions/add-funds/add-funds.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { UserService } from '../../../services/user.service';

@Component({
  selector: 'app-add-funds',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule
  ],
  templateUrl: './add-funds.component.html',
  styleUrls: ['./add-funds.component.scss']
})
export class AddFundsComponent implements OnInit {
  balance = 0;
  depositForm = new FormGroup({
    amount: new FormControl<number>(0, {
      nonNullable: true,
      validators: [ Validators.required, Validators.min(0.01) ]
    })
  });

  constructor(private userSvc: UserService) {}

  ngOnInit(): void {
    this.loadBalance();
  }

  loadBalance(): void {
  this.userSvc.balance$
    .subscribe({
      next: (b: number) => this.balance = b,
      error: err => console.error('Błąd przy pobieraniu salda:', err)
    });
}


  onDeposit(): void {
    const { amount } = this.depositForm.getRawValue();
    this.userSvc.deposit(amount)
      .subscribe({
        next: (newBal: number) => {
          this.balance = newBal;
          this.depositForm.reset({ amount: 0 });
          alert(`Wpłacono ${amount} PLN. Nowe saldo: ${newBal} PLN`);
        },
        error: err => alert('Błąd wpłaty: ' + err.message)
      });
  }
}
