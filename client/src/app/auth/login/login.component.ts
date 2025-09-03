// src/app/auth/login/login.component.ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl, FormGroup, Validators } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { TransactionService } from '../../services/transaction.service';
import { AuthRequestDTO } from '../auth-request.dto';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ CommonModule, ReactiveFormsModule, RouterModule ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'] 
})
export class LoginComponent {
  form = new FormGroup({
    email: new FormControl<string>('', {
      nonNullable: true,
      validators: [ Validators.required, Validators.email ]
    }),
    password: new FormControl<string>('', {
      nonNullable: true,
      validators: [ Validators.required ]
    })
  });

  constructor(
    private auth: AuthService,
    private tx: TransactionService,
    private router: Router
  ) {}

  onSubmit() {
    if (!this.form.valid) return;
    const creds: AuthRequestDTO = this.form.getRawValue();

    this.auth.login(creds).subscribe({
      next: token => {
        // tutaj AuthService powinien już sam zapisać token do localStorage
        // teraz sprawdzamy, czy są transakcje pending:
        this.tx.getPending(creds.email).subscribe(pending => {
          if (pending.length > 0) {
            this.router.navigate(['transactions','pending']);
            console.log(pending.length);
            
          } else {
            this.router.navigate(['transactions','all']);
          }
        });
      },
      error: err => alert('Błąd logowania: ' + err.message)
    });
  }
}
