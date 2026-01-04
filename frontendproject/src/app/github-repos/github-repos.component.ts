import { Component, OnInit } from '@angular/core';
import { AuthService } from '../auth.service';
import { GitHubService } from '../git-hub.service';
import { GitHubRepo } from '../models/github-repo';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavigationEnd, Router, RouterModule } from '@angular/router';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { GitlabCiComponent } from '../gitlab-ci/gitlab-ci.component';
import { filter, lastValueFrom } from 'rxjs';
import { HeaderComponent } from '../header/header.component';

@Component({
  selector: 'app-github-repos',
  standalone: true,
  imports: [CommonModule, FormsModule, MatSnackBarModule, GitlabCiComponent, RouterModule ],
  templateUrl: './github-repos.component.html',
  styleUrl: './github-repos.component.css'
})
export class GithubReposComponent implements OnInit {
  repos: GitHubRepo[] = [];
  selectedRepo: string = '';
  selectedBranch: string = 'main';

  /* **********  clone properties ************ */
  selectedOperation: string = 'clone';
  currentStep: number = 0; // Renamed from 'index' for clarity
  repoOwner: string = '';
  repoName: string = '';
  commitBranch: string = 'main';
  commitMessage: string = '';

  /* ********push & commit properties *********** */
  localRepoPath: string = '';
  pushBranch: string = 'main';
  remoteUrl: string = '';
    /****************************** pull************************ */
   pullBranch: string = '';
   isPulling: boolean = false;
   pullResult: any = null;
   pullRepoPath: any;

  constructor(
    private githubService: GitHubService,
    private authService: AuthService,
    private snackBar: MatSnackBar,
    private router: Router
  ) { }

  /******* Ajouté 2mai *******/


  ngOnInit() {
    this.loadRepositories();
    this.setupRouteListener();

  }

  loadRepositories() {
    this.githubService.getRepositories().subscribe({
      next: (repos) => this.repos = repos,
      error: (err) => console.error('Erreur:', err)
    });
  }

  /* *********   fonction clonage  ************ */

  onClone() {
    this.githubService.cloneToDesktop(this.selectedRepo, this.selectedBranch)
      .subscribe({
        next: (response) => {
          console.log('Clone réussi sur le Bureau', response);
          alert('Le dépôt a été cloné avec succès sur votre Bureau');
          this.loadRepositories();
        },
        error: (err) => {
          console.error('Erreur clonage', err);
          if (err.status === 403) {
            alert('Problème de sécurité - Essayez de vous reconnecter');
          } else {
            alert(`Erreur lors du clonage: ${err.message}`);
          }
        }
      });
  }

  showStep(stepIndex: number) {
    this.currentStep = stepIndex;
  }

  isActiveStep(stepIndex: number): boolean {
    return this.currentStep === stepIndex;
  }

  /*showStep(stepIndex: number) {
    this.currentStep = stepIndex;
  }*/

  //////////////////////

  private setupRouteListener() {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      this.updateStepFromUrl();
    });
  }
  private updateStepFromUrl() {
    if (this.router.url.includes('/gitlab-ci') ||
      this.router.url.includes('/github-repos/build')) {
      this.currentStep = 1;
    } else {
      this.currentStep = 0;
    }
  }


  private navigateToStep(stepIndex: number) {
    if (stepIndex === 1) {
      this.router.navigate(['/gitlab-ci']);
    } else {
      this.router.navigate(['/github-repos']);
    }
  }

  ////////////////////


  /*************  fonction push & commit    *****************/
async onCommitAndPush() {
    if (!this.remoteUrl) {
      this.snackBar.open('Remote repository URL is required', 'Close', { duration: 3000 });
      return;
    }

    if (!this.pushBranch) {
      this.snackBar.open('Please select a branch', 'Close', { duration: 3000 });
      return;
    }

    try {
      await lastValueFrom(
        this.githubService.commitAndPush(
          this.localRepoPath,
          this.commitMessage,
          this.pushBranch,
          this.remoteUrl
        )
      );

      this.snackBar.open(`Successfully pushed to ${this.pushBranch}`, 'Close', { duration: 3000 });
    } catch (error: unknown) {
      let errorMessage = `Failed to push to ${this.pushBranch}`;

      // Type-safe error handling
      if (error instanceof Error) {
        errorMessage += `: ${error.message}`;
      } else if (typeof error === 'string') {
        errorMessage += `: ${error}`;
      } else if (error && typeof error === 'object' && 'error' in error) {
        // Handle HTTP error responses
        const err = error as { error?: { message?: string } };
        errorMessage += `: ${err.error?.message || 'Unknown error'}`;
      }

      console.error('Push failed:', error);
      this.snackBar.open(errorMessage, 'Close', { duration: 5000 });
    }
  }
  /**************************pull*********************** */
  async selectFolder(): Promise<void> {
    try {
      // Try the modern API first
      if ('showDirectoryPicker' in window) {
        // @ts-ignore - Experimental API
        const dirHandle = await window.showDirectoryPicker();
        this.pullRepoPath = dirHandle.name;
      } else {
        // Fallback for browsers that don't support the API
        this.pullRepoPath = prompt('Please enter the full path to your repository:');
      }
      this.detectBranch();
    } catch (error) {
      console.error('Error selecting folder:', error);
      this.snackBar.open('Error selecting folder. Please enter path manually.', 'Close', { duration: 3000 });
    }
  }

  // Add this method to handle repository selection from dropdown
  selectedRepoObject: GitHubRepo | null = null;

  // Update the onRepoSelected method
  // Dans onRepoSelected()
  onRepoSelected(event: Event): void {
    const selectElement = event.target as HTMLSelectElement;
    const selectedValue = selectElement.value;

    this.selectedRepoObject = this.repos.find(repo => repo.clone_url === selectedValue) || null;

    if (this.selectedRepoObject) {
      // Mettre à jour toutes les branches concernées
      const defaultBranch = this.selectedRepoObject.default_branch;
      this.selectedBranch = defaultBranch;
      this.pullBranch = defaultBranch;
      this.pushBranch = defaultBranch;
    }
  }
  // Add this method to handle repository selection from dropdown


  // Detect branch from local repo
  detectBranch() {
    if (!this.pullRepoPath) return;

    this.githubService.detectLocalBranch(this.pullRepoPath).subscribe({
      next: (branch) => {
        this.pullBranch = branch || 'main';
        // Mettre aussi à jour pushBranch si on est dans le même contexte
        if (this.selectedOperation === 'commit-push') {
          this.pushBranch = branch || 'main';
        }
      },
      error: (err) => {
        console.error('Could not detect branch:', err);
        // Définir des valeurs par défaut
        this.pullBranch = 'main';
        this.pushBranch = 'main';
      }
    });
  }
  async onPull() {
    if (!this.pullRepoPath) {
      this.snackBar.open('Veuillez sélectionner un dossier de dépôt', 'Fermer', {
        duration: 3000,
        panelClass: ['error-snackbar']
      });
      return;
    }

    this.isPulling = true;
    this.pullResult = null;

    try {
      const result = await lastValueFrom(
        this.githubService.pullRepository(this.pullRepoPath, this.pullBranch)
      );

      this.pullResult = result;

      if (result.success) {
        this.snackBar.open(result.message || 'Pull réussi', 'Fermer', {
          duration: 3000,
          panelClass: ['success-snackbar']
        });
      } else {
        this.snackBar.open(result.error || 'Échec du pull', 'Fermer', {
          duration: 5000,
          panelClass: ['error-snackbar']
        });
      }

    } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : 'Erreur inconnue';
      this.snackBar.open(`Échec du pull: ${errorMessage}`, 'Fermer', {
        duration: 5000,
        panelClass: ['error-snackbar']
      });
    } finally {
      this.isPulling = false;
    }
  }

  // Helper to get change type icon
  getChangeIcon(fileChange: string): string {
    if (fileChange.startsWith('ADD')) return 'fa-plus-circle text-success';
    if (fileChange.startsWith('MODIFY')) return 'fa-edit text-warning';
    if (fileChange.startsWith('DELETE')) return 'fa-trash-alt text-danger';
    return 'fa-file-alt';
  }
  // Update the selectFolder method to have a fallback

  }


