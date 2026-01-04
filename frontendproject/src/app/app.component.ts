import { Component, Inject, OnDestroy, OnInit, PLATFORM_ID } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './auth.service';
import { isPlatformBrowser } from '@angular/common';
import { HeaderComponent } from './header/header.component';
import { catchError, of, switchMap, takeUntil, Subject, Observable } from 'rxjs';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [HeaderComponent, RouterOutlet],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'authentification-front';
  githubToken: string | null = null;
  private destroy$ = new Subject<void>();

  constructor(
    private authService: AuthService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.initAuthFlow();
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initAuthFlow(): void {
    this.authService.handleAuthCallback().pipe(
      switchMap(() => this.authService.setUserFromBackend()),
      switchMap(() => this.handleGitHubToken()),
      catchError(err => {
        console.error('Authentication error:', err);
        return of(null);
      }),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      console.log('Authentication flow completed');
    });
  }

  private handleGitHubToken(): Observable<void> {
    return new Observable(subscriber => {
      if (!this.authService.isAuthenticated()) {
        subscriber.complete();
        return;
      }

      // Vérifie d'abord le localStorage
      const storedToken = localStorage.getItem('githubToken');
      if (storedToken) {
        this.githubToken = storedToken;
        console.log('Token GitHub récupéré depuis le cache:', this.githubToken);
        subscriber.complete();
        return;
      }

      // Sinon, demande un nouveau token
      this.authService.getGitHubAccessToken().pipe(
        takeUntil(this.destroy$)
      ).subscribe({
        next: (res) => {
          this.githubToken = res.access_token;
          localStorage.setItem('githubToken', this.githubToken);
          console.log('Nouveau token GitHub reçu:', this.githubToken);
          subscriber.next();
          subscriber.complete();
        },
        error: (err) => {
          console.error('Erreur token GitHub:', err);
          subscriber.error(err);
        }
      });
    });
  }
}
