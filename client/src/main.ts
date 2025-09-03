// src/main.ts
import { bootstrapApplication }   from '@angular/platform-browser';
import { provideRouter }          from '@angular/router';
import {
  provideHttpClient,
  withInterceptorsFromDi
}                                  from '@angular/common/http';
import { HTTP_INTERCEPTORS }      from '@angular/common/http';

import { AppComponent }           from './app/app.component';
import { appRoutes }              from './app/app.routes';
import { AuthInterceptor }        from './app/auth/auth.interceptor';

bootstrapApplication(AppComponent, {
  providers: [
    // 1) wire up the HttpClient standalone API
    provideHttpClient(
      // tell the HTTP client to pick up whatever
      // interceptors you’ve registered under HTTP_INTERCEPTORS
      withInterceptorsFromDi()
    ),

    // 2) register your interceptor class in DI
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    },

    // 3) your router, etc.
    provideRouter(appRoutes),
  ]
})
.catch(err => console.error(err));
