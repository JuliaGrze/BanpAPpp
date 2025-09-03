// src/app/auth/auth.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { AuthRequestDTO } from './auth-request.dto';

interface JwtPayload {
  sub?: string;
  email?: string;
  roles?: string[] | string;
  exp?: number;
  [k: string]: any;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly API = 'http://localhost:8081/api/auth';
  private readonly TOKEN_KEY = 'BANK_APP_JWT';
  private readonly EMAIL_KEY = 'BANK_APP_EMAIL';
  private readonly ROLES_KEY = 'BANK_APP_ROLES';

  private currentEmail: string | null = null;
  private currentRoles: string[] = [];

  private _loggedIn$ = new BehaviorSubject<boolean>(!!this.getToken());
  public readonly loggedIn$ = this._loggedIn$.asObservable();

  constructor(private http: HttpClient) {
    this.restoreSessionFromStorage();
    window.addEventListener('storage', (e) => {
      if (e.key === this.TOKEN_KEY) this.restoreSessionFromStorage();
    });
  }

  login(creds: AuthRequestDTO): Observable<string> {
    return this.http
      // backend zwraca czysty string (JWT), więc `responseType: 'text'`
      .post(`${this.API}/login`, creds, { responseType: 'text' })
      .pipe(
        tap(token => {
          localStorage.setItem(this.TOKEN_KEY, token);
          const payload = this.decodeJwt(token);
          const email = payload?.email || payload?.sub || creds.email;
          const roles = this.normalizeRoles(payload?.roles);

          this.currentEmail = email ?? null;
          this.currentRoles = roles;

          if (email) localStorage.setItem(this.EMAIL_KEY, email);
          localStorage.setItem(this.ROLES_KEY, JSON.stringify(roles));
          this._loggedIn$.next(true);
        })
      ) as unknown as Observable<string>;
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.EMAIL_KEY);
    localStorage.removeItem(this.ROLES_KEY);
    this.currentEmail = null;
    this.currentRoles = [];
    this._loggedIn$.next(false);
  }

  isLoggedIn(): boolean {
    const token = this.getToken();
    if (!token) return false;
    const payload = this.decodeJwt(token);
    if (payload?.exp && Date.now() >= payload.exp * 1000) {
      this.logout();
      return false;
    }
    return true;
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getEmail(): string | null {
    return this.currentEmail ?? localStorage.getItem(this.EMAIL_KEY);
  }

  getRoles(): string[] {
    if (this.currentRoles.length) return this.currentRoles;
    const raw = localStorage.getItem(this.ROLES_KEY);
    if (!raw) return [];
    try { return JSON.parse(raw) as string[]; } catch { return []; }
  }

  // ---- helpers ----
  private restoreSessionFromStorage() {
    const token = this.getToken();
    if (!token) {
      this.currentEmail = null;
      this.currentRoles = [];
      this._loggedIn$.next(false);
      return;
    }
    const payload = this.decodeJwt(token);
    const email = localStorage.getItem(this.EMAIL_KEY) || payload?.email || payload?.sub || null;
    const roles = this.normalizeRoles(payload?.roles) || this.getRoles();
    this.currentEmail = email;
    this.currentRoles = roles;
    this._loggedIn$.next(this.isLoggedIn());
  }

  private decodeJwt(token: string): JwtPayload | null {
    try {
      const [, b64] = token.split('.');
      const json = atob(b64.replace(/-/g, '+').replace(/_/g, '/'));
      return JSON.parse(json) as JwtPayload;
    } catch { return null; }
  }

  private normalizeRoles(roles: unknown): string[] {
    if (!roles) return [];
    if (Array.isArray(roles)) return roles.map(String);
    if (typeof roles === 'string') return [roles];
    return [];
  }
}
