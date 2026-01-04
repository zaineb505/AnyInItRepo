import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, catchError, map, Observable, of, switchMap, tap, throwError } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';
import { User } from './models/user';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private backendUrl = 'http://localhost:8080';

  public currentUserSubject = new BehaviorSubject<any>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  private githubUserSubject = new BehaviorSubject<any>(null);
  public githubUser$ = this.githubUserSubject.asObservable();

  private gitlabUserSubject = new BehaviorSubject<any>(null);
  public gitlabUser$ = this.gitlabUserSubject.asObservable();

  constructor(
    @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,
    private router: Router
  ) {
    this.initializeAuthState();
  }

  private initializeAuthState() {
    const user = this.getCurrentUser();
    if (user) {
      this.currentUserSubject.next(user);
    }
    const gitlabUser = this.getGitlabUser();
    if (gitlabUser) {
      this.gitlabUserSubject.next(gitlabUser);
    }
  }

  // ======================
  // Storage Helpers (SSR-safe)
  // ======================
  private getLocalStorage(): Storage | null {
    return isPlatformBrowser(this.platformId) ? window.localStorage : null;
  }

  private getCurrentUser(): any {
    if (!isPlatformBrowser(this.platformId)) return null;
    const storage = this.getLocalStorage();
    const user = storage?.getItem('currentUser');
    return user ? JSON.parse(user) : null;
  }

  private setCurrentUser(user: any): void {
    if (!isPlatformBrowser(this.platformId)) return;
    const storage = this.getLocalStorage();
    storage?.setItem('currentUser', JSON.stringify(user));
  }

  private clearCurrentUser(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    const storage = this.getLocalStorage();
    storage?.removeItem('currentUser');
  }

  // ======================
  // OAuth Methods
  // ======================
  loginWithGithub() {
    if (!isPlatformBrowser(this.platformId)) return;
    window.location.href = `${this.backendUrl}/oauth2/authorization/github`;
  }

  loginWithGitLab() {
    if (!isPlatformBrowser(this.platformId)) return;
    window.location.href = `${this.backendUrl}/oauth2/authorization/gitlab`;
  }

  loginWithGoogle() {
    if (!isPlatformBrowser(this.platformId)) return;
    window.location.href = `${this.backendUrl}/oauth2/authorization/google`;
  }

  private handleUserResponse(user: any): void {
    if (!user) {
      console.error('Invalid user data received:', user);
      throw new Error('Invalid user data');
    }

    if (user.provider === 'github') {
      const normalizedUser = {
        id: user.id,
        name: user.name || 'User',
        email: user.email || '',
        avatarUrl: user.avatarUrl || null,
        provider: user.provider
      };

      this.setCurrentUser(normalizedUser);
      this.currentUserSubject.next(normalizedUser);

      // Force another emission in the next tick
      setTimeout(() => {
        this.currentUserSubject.next(normalizedUser);
      }, 0);
    }
  }

  // auth.service.ts
handleAuthCallback(): Observable<User> {
  return this.http.get<User>(`${this.backendUrl}/api/auth/user`, {
    withCredentials: true,
    headers: new HttpHeaders({
      'Accept': 'application/json',
      'Content-Type': 'application/json'
    }),
    responseType: 'json'
  }).pipe(
    tap((user: User) => {
      if (user?.provider === 'github') {
        this.handleUserResponse(user);
      } else if (user?.provider === 'gitlab') {
        this.setGitlabUser(user);
      }
    }),
    catchError(error => {
      console.error('Auth error:', error);
      this.clearUserData();
      return throwError(() => error);
    })
  );
}

setUserFromBackend(): Observable<User> {
  return this.http.get<User>(`${this.backendUrl}/api/auth/user`, {
    withCredentials: true,
    headers: new HttpHeaders({
      'Accept': 'application/json'
    })
  }).pipe(
    tap(user => {
      if (user) {
        const storage = this.getLocalStorage();
        storage?.setItem('currentUser', JSON.stringify(user));
        this.currentUserSubject.next(user);
      }
    })
  );
}

  private clearUserData(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    const storage = this.getLocalStorage();
    storage?.removeItem('currentUser');
    storage?.removeItem('gitlabUser');
    this.currentUserSubject.next(null);
    this.gitlabUserSubject.next(null);
  }

  // ======================
  // Auth State Helpers
  // ======================
  isAuthenticated(): boolean {
    return !!this.currentUserSubject.value;
  }

  logout() {
    if (!isPlatformBrowser(this.platformId)) return;
    const storage = this.getLocalStorage();
    storage?.removeItem('currentUser');
    storage?.removeItem('gitlabUser');
    storage?.removeItem('githubToken');
    sessionStorage.clear();

    this.currentUserSubject.next(null);
    this.gitlabUserSubject.next(null);

    window.location.href = `${this.backendUrl}/logout`;
  }

  getGitHubAccessToken() {
    return this.http.get<{ access_token: string }>(
      `${this.backendUrl}/api/auth/token`,
      {
        withCredentials: true,
        headers: new HttpHeaders({
          'Accept': 'application/json'
        })
      }
    ).pipe(
      catchError(error => {
        if (error.status === 401) {
          this.loginWithGithub();
        }
        return throwError(() => error);
      })
    );
  }

  handleGitLabAuthCallback(): Observable<any> {
    return this.http.get(`${this.backendUrl}/api/auth/gitlab/token`, {
      withCredentials: true
    }).pipe(
      tap((response: any) => {
        this.retryPendingGitLabAction();
        this.router.navigate(['/gitlab-ci']);
      }),
      catchError(error => {
        console.error('GitLab auth error:', error);
        this.router.navigate(['/'], {
          queryParams: { error: 'gitlab_auth_failed' }
        });
        return throwError(() => error);
      })
    );
  }

  // Vérification d'authentification GitLab
  checkGitLabAuth(): Observable<boolean> {
    return this.http.get<{ authenticated: boolean }>(
      `${this.backendUrl}/api/auth/gitlab/check-auth`,
      { withCredentials: true }
    ).pipe(
      map(response => response.authenticated),
      catchError(() => of(false))
    );
  }

  private retryPendingGitLabAction() {
    if (!isPlatformBrowser(this.platformId)) return;
    const storage = this.getLocalStorage();
    const pendingAction = storage?.getItem('gitlab_pending_action');
    if (pendingAction) {
      const action = JSON.parse(pendingAction);
      storage?.removeItem('gitlab_pending_action');

      if (action.type === 'deploy') {
        this.router.navigate(['/gitlab-ci'], {
          state: { retryDeploy: true }
        });
      }
    }
  }

  /*setUserFromBackend(): Observable<any> {
    return this.http.get('http://localhost:8080/api/auth/user').pipe(
      tap((user: any) => {
        if (isPlatformBrowser(this.platformId)) {
          const storage = this.getLocalStorage();
          storage?.setItem('currentUser', JSON.stringify(user));
        }
        this.currentUserSubject.next(user);
      })
    );
  }*/

  // Gestion utilisateurs GitHub
  setGithubUser(user: any): void {
    this.githubUserSubject.next(user);
    if (!isPlatformBrowser(this.platformId)) return;
    const currentUser = this.getCurrentUser() || {};
    const updatedUser = { ...currentUser, github: user };
    const storage = this.getLocalStorage();
    storage?.setItem('currentUser', JSON.stringify(updatedUser));
  }

  // Gestion utilisateurs GitLab
  setGitlabUser(user: any): void {
    const normalizedUser = {
      id: user.id,
      name: user.name || 'GitLab User',
      email: user.email || '',
      avatarUrl: user.avatarUrl || null,
      provider: 'gitlab'
    };

    this.gitlabUserSubject.next(normalizedUser);
    if (!isPlatformBrowser(this.platformId)) return;
    const storage = this.getLocalStorage();
    storage?.setItem('gitlabUser', JSON.stringify(normalizedUser));
  }

  getGitlabUser(): any {
    if (!isPlatformBrowser(this.platformId)) return null;
    const storage = this.getLocalStorage();
    const user = storage?.getItem('gitlabUser');
    return user ? JSON.parse(user) : null;
  }
}
