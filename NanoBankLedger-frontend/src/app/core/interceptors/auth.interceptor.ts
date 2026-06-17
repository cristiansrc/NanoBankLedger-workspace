import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { catchError, switchMap, throwError, BehaviorSubject, filter, take } from 'rxjs';

let isRefreshing = false;
const refreshTokenSubject = new BehaviorSubject<string | null>(null);

export const authInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.accessToken;

  if (token && !req.url.includes('/auth/refresh') && !req.url.includes('/auth/login') && !req.url.includes('/auth/register')) {
    req = addTokenToRequest(req, token);
  }

  return next(req).pipe(
    catchError(error => {
      if (error instanceof HttpErrorResponse && (error.status === 401 || error.status === 403) && !req.url.includes('/auth/')) {
        return handleAuthError(req, next, authService, router, error);
      }
      return throwError(() => error);
    })
  );
};

function addTokenToRequest(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({
    setHeaders: { Authorization: `Bearer ${token}` }
  });
}

function handleAuthError(req: HttpRequest<unknown>, next: HttpHandlerFn, authService: AuthService, router: Router, error: HttpErrorResponse) {
  if (!isRefreshing && authService.refreshTokenValue) {
    isRefreshing = true;
    refreshTokenSubject.next(null);

    return authService.refreshAccessToken().pipe(
      switchMap(response => {
        isRefreshing = false;
        refreshTokenSubject.next(response.access_token);
        return next(addTokenToRequest(req, response.access_token));
      }),
      catchError(refreshError => {
        isRefreshing = false;
        authService.clearAuth();
        router.navigate(['/auth/login']);
        return throwError(() => refreshError);
      })
    );
  } else if (isRefreshing) {
    return refreshTokenSubject.pipe(
      filter(token => token !== null),
      take(1),
      switchMap(token => next(addTokenToRequest(req, token!)))
    );
  } else {
    // No hay refresh token, redirigir directamente al login
    authService.clearAuth();
    router.navigate(['/auth/login']);
    return throwError(() => error);
  }
}
