// src/app/services/user.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, tap } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class UserService {
  private apiUrl = 'http://localhost:8081/api/users';

  // trzymamy wewnętrzne saldo
  private balanceSubject = new BehaviorSubject<number>(0);
  // expose jako Observable
  balance$ = this.balanceSubject.asObservable();

  constructor(private http: HttpClient) {
    // od razu przy starcie pobierz saldo
    this.refreshBalance();
  }

  /** robimy jednokrotny GET i wpychamy wynik do subjecta */
  refreshBalance() {
    this.http
      .get<number>(`${this.apiUrl}/balance`)
      .subscribe(b => this.balanceSubject.next(b));
  }

  /** wpłata -> zwraca nowe saldo i aktualizuje subject */
  deposit(amount: number) {
    return this.http
      .post<number>(`${this.apiUrl}/deposit`, null, { params: { amount } })
      .pipe(
        tap(newBal => this.balanceSubject.next(newBal))
      );
  }
}
