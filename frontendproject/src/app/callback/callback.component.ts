import { Component, Inject, PLATFORM_ID } from '@angular/core';
import { AuthService } from '../auth.service';
import { Router } from '@angular/router';
import { GitlabService } from '../git-lab.service';
import { isPlatformBrowser } from '@angular/common';

@Component({
  template: `<div>Processing login...</div>`
})
export class CallbackComponent {
  private isBrowser: boolean;

  constructor(
    private auth: AuthService,
    private router: Router,
    private gitlabService: GitlabService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);

    this.auth.handleAuthCallback();

    this.auth.handleGitLabAuthCallback().subscribe({
      next: () => {
        if (this.isBrowser) {
          const pendingDeploy = localStorage.getItem('gitlab_pending_deploy');
          if (pendingDeploy) {
            localStorage.removeItem('gitlab_pending_deploy');
            const { repoUrl, yamlContent, branch } = JSON.parse(pendingDeploy);

            this.gitlabService.deployPipeline(repoUrl, yamlContent, branch).subscribe({
              next: () => this.router.navigate(['/gitlab-ci']),
              error: () => this.router.navigate(['/gitlab-ci'])
            });
            return;
          }
        }
        this.router.navigate(['/gitlab-ci']);
      },
      error: () => this.router.navigate(['/gitlab-ci'])
    });
  }
}
