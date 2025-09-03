// src/app/auth/auth.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs/operators';
import { BehaviorSubject, Observable } from 'rxjs';
import { AuthRequestDTO } from './auth-request.dto';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly API = 'http://localhost:8081/api/auth';
  private readonly TOKEN_KEY = 'BANK_APP_JWT';
  private currentEmail: string | null = null;

  // BehaviorSubject, który mówi czy mamy token w localStorage
  private _loggedIn$ = new BehaviorSubject<boolean>(!!this.getToken());
  public readonly loggedIn$ = this._loggedIn$.asObservable();

  constructor(private http: HttpClient) {}

  login(creds: AuthRequestDTO): Observable<string> {
    return this.http
      .post(`${this.API}/login`, creds, { responseType: 'text' })
      .pipe(
        tap(token => {
          localStorage.setItem(this.TOKEN_KEY, token);
          this._loggedIn$.next(true);      // ustawiamy stan na zalogowany
          this.currentEmail = creds.email;
        })
      );
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

   /** synchronousznie: */
  isLoggedIn(): boolean {
    return !!localStorage.getItem(this.TOKEN_KEY);
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    this._loggedIn$.next(false);         // ustawiamy stan na wylogowany
  }

  getEmail(): string | null {
    // najpierw sprawdź w polu, później fallback na localStorage
    return this.currentEmail;
  }
}
