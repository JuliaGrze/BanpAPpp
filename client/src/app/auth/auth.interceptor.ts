// src/app/auth/auth.interceptor.ts
import { Injectable } from '@angular/core';
import {
  HttpInterceptor, HttpRequest, HttpHandler, HttpEvent
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';

// src/app/auth/auth.interceptor.ts
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private auth: AuthService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = this.auth.getToken();
    console.log('[AuthInterceptor] token z AuthService:', token);
    let authReq = req;

    if (token) {
      authReq = req.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      });
      console.log('[AuthInterceptor] doklejony header Authorization:', authReq.headers.get('Authorization'));
    }

    console.log('[AuthInterceptor] nagłówki przed wysłaniem:', authReq.headers.keys());
    return next.handle(authReq);
  }
}


