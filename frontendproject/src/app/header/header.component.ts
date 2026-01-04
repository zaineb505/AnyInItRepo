import {
  Component,
  OnInit,
  OnDestroy,
  ChangeDetectorRef,
  Inject,
  PLATFORM_ID
} from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Subject, takeUntil } from 'rxjs';
import { AuthService } from '../auth.service';
import { RouterModule } from '@angular/router';
import { DocumentationComponent } from '../documentation/documentation.component';

@Component({
  standalone: true,
  imports: [CommonModule, RouterModule, DocumentationComponent],
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit, OnDestroy {
  userProfile: any = null;
  userName: string | null = null;
  userImage: string | null = null;
  userInitials: string = 'U';
  isDropdownOpen = false;
  isLoading = true;

  private destroy$ = new Subject<void>();
  private isBrowser: boolean;

  constructor(
    private authService: AuthService,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  ngOnInit(): void {
    if (this.isBrowser) {
      const storedUser = localStorage.getItem('currentUser');
      if (storedUser) {
        try {
          const parsedUser = JSON.parse(storedUser);
          this.authService.currentUserSubject.next(parsedUser);
        } catch (err) {
          console.error('Erreur parsing localStorage user:', err);
        }
      }

      this.authService.currentUser$.pipe(
        takeUntil(this.destroy$)
      ).subscribe(user => {
        this.updateUserProfile(user);
        this.cdr.detectChanges();

        // Double update in next tick
        setTimeout(() => {
          this.updateUserProfile(user);
          this.cdr.detectChanges();
        }, 0);
      });
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private updateUserProfile(user: any): void {
    this.isLoading = false;

    if (user) {
      this.userProfile = user;
      this.userName = user.name || null;
      this.userImage = user.avatarUrl || null;

      if (!this.userImage && this.userName) {
        this.userInitials = this.userName
          .split(' ')
          .map((word: string) => word[0])
          .join('')
          .toUpperCase();
      }
    } else {
      this.userProfile = null;
      this.userName = null;
      this.userImage = null;
      this.userInitials = 'U';
    }
  }

  toggleDropdown(): void {
    this.isDropdownOpen = !this.isDropdownOpen;
  }

  logout(): void {
    this.isLoading = true;
    this.authService.logout();
  }

  handleImageError(): void {
    this.userImage = null;
    this.cdr.detectChanges();
  }
}
