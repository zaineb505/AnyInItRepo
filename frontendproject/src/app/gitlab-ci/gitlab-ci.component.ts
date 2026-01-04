import { Component, Inject, PLATFORM_ID } from '@angular/core';
import { GitlabService } from '../git-lab.service';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { EMPTY, finalize, map, Observable, of, switchMap, tap } from 'rxjs';
import { GitlabVerificationModalComponent } from '../gitlab-verification-modal/gitlab-verification-modal.component';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-gitlab-ci',
  standalone: true,
  templateUrl: './gitlab-ci.component.html',
  imports: [CommonModule, FormsModule],
  styleUrl: './gitlab-ci.component.css'
})
export class GitlabCiComponent {
  // Données du formulaire
  gitlabRepoUrl = '';
  selectedLanguage = 'nodejs';
  selectedDockerImage = '';
  includeBuild = true;
  includeTest = true;

  // État
  isLoading = false;
  generatedYaml = '';
  showYamlPreview = false;

  // Pour vérifier si on est côté navigateur
  isBrowser: boolean;

isEditingYaml = false;
  editedYaml = '';
  yamlModified = false;

  constructor(
    private gitlabService: GitlabService,
    private dialog: MatDialog,
    private authService: AuthService,
    private snackBar: MatSnackBar,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);

    if (this.isBrowser) {
      this.checkPendingDeploy();
    }
  }

  get dockerImages() {
    return this.gitlabService.dockerImages;
  }

  generatePipeline() {
    this.isLoading = true;

    const config = {
      repoUrl: this.gitlabRepoUrl,
      language: this.selectedLanguage,
      dockerImage: this.selectedDockerImage || this.getDefaultImage(),
      includeBuild: this.includeBuild,
      includeTest: this.includeTest
    };

    this.gitlabService.generatePipeline(config).subscribe({
      next: (yaml) => {
        this.generatedYaml = yaml;
        this.showYamlPreview = true;
        this.editedYaml = yaml;
        this.yamlModified = false;
      },
      error: (err) => {
        console.error('Erreur:', err);
        this.snackBar.open('Erreur lors de la génération du pipeline', 'Fermer', { duration: 3000 });
      },
      complete: () => this.isLoading = false
    });
  }
enableEditing() {
  this.isEditingYaml = true;
}

confirmYamlEdit() {
  this.generatedYaml = this.editedYaml;
  this.isEditingYaml = false;
  this.yamlModified = true;
  this.snackBar.open('Le fichier .gitlab-ci.yml a été modifié.', 'Fermer', { duration: 3000 });
}
  private checkPendingDeploy() {
    if (!this.isBrowser) return;

    const pendingDeploy = localStorage.getItem('gitlab_pending_deploy');
    if (pendingDeploy) {
      localStorage.removeItem('gitlab_pending_deploy');
      const { repoUrl, yamlContent, branch } = JSON.parse(pendingDeploy);
      this.gitlabRepoUrl = repoUrl;
      this.generatedYaml = yamlContent;
      this.showYamlPreview = true;
    }
  }

  private getDefaultImage(): string {
    const images = this.dockerImages[this.selectedLanguage];
    const latestImage = images.find(img => img.name.toLowerCase().includes('latest'));
    return latestImage?.image || images[0]?.image || '';
  }

  deployPipeline() {
    if (!this.generatedYaml) return;

    this.isLoading = true;

    this.gitlabService.checkGitLabAuth().pipe(
      switchMap(authenticated => {
        if (!authenticated) {
          if (this.isBrowser) {
            localStorage.setItem('gitlab_pending_deploy', JSON.stringify({
              repoUrl: this.gitlabRepoUrl,
              yamlContent: this.generatedYaml,
              branch: 'master'
            }));
            window.location.href = 'http://localhost:8080/oauth2/authorization/gitlab';
          }
          return EMPTY;
        }

        return this.gitlabService.getUserStatus();
      }),
      switchMap(userStatus => {
        if (userStatus.is_first_deployment) {
          return this.showVerificationModal().pipe(
            switchMap(confirmed => {
              if (!confirmed) {
                this.isLoading = false;
                return EMPTY;
              }
              return this.performDeployment(userStatus.gitlab_user_id);
            })
          );
        }
        return this.performDeployment(userStatus.gitlab_user_id);
      })
    ).subscribe({
      error: (err) => {
        this.isLoading = false;
        this.snackBar.open(`Erreur: ${err.message}`, 'Fermer', { duration: 5000 });
      }
    });
  }

  private performDeployment(gitlabUserId: number): Observable<void> {
    return this.gitlabService.deployPipeline(
      this.gitlabRepoUrl,
      this.generatedYaml,
      'master'
    ).pipe(
      tap(() => {
        this.gitlabService.markAsDeployed(gitlabUserId).subscribe();
        this.snackBar.open('Pipeline déployé avec succès !', 'Fermer', { duration: 3000 });
      }),
      finalize(() => this.isLoading = false)
    );
  }

  private showVerificationModal(): Observable<boolean> {
    const dialogRef = this.dialog.open(GitlabVerificationModalComponent, {
      width: '500px',
      data: {
        title: 'Vérification Requise',
        message: 'Pour votre premier déploiement, veuillez vérifier votre compte GitLab via SMS.',
        verificationUrl: 'https://gitlab.com/-/identity_verification'
      }
    });

    return dialogRef.afterClosed();
  }
}
