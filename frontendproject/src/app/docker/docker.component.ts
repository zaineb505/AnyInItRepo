import { Component, OnInit } from '@angular/core';
import { DockerService } from '../docker.service';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { lastValueFrom, firstValueFrom, catchError, of, filter, take, finalize, timeout, EMPTY } from 'rxjs';
import { AuthService } from '../auth.service';
import { LogWebsocketService } from '../log-websocket.service';

interface DeploymentResult {
  containerId: string;
  port: number;
  status: string;
  buildLogs?: string;
}

@Component({
  selector: 'app-docker',
  standalone: true,
  templateUrl: './docker.component.html',
  imports: [FormsModule, CommonModule],
  styleUrl: './docker.component.css',
})
export class DockerComponent implements OnInit{
  deploymentData = {
    repoUrl: '',
    imageName: '',
    hostPort: 8080,
    containerPort: 8080
  };

  isLoading = false;
  result: DeploymentResult | null = null;
  error: string | null = null;
  showLogs = false;
  private readonly STORAGE_KEY = 'docker_pending_deploy';
  currentLogs: string = '';

  showRealtimeLogs = true; // Contrôle l'affichage des logs
  realTimeLogs: string[] = [];

  get serverIp(): string {
    return window.location.hostname || 'localhost';
  }

  constructor(private dockerService: DockerService, private wsService: LogWebsocketService) {
  }
  ngOnInit(): void {
    this.checkPendingDeployment();

    this.wsService.connect((log) => {
      this.realTimeLogs.push(log);
      // Gardez seulement les 100 derniers logs pour éviter la surcharge mémoire
      if (this.realTimeLogs.length > 100) {
        this.realTimeLogs.shift();
      }
    });
  }

    ngOnDestroy(): void {
    // Nettoyez la connexion WebSocket quand le composant est détruit
    this.wsService.disconnect();
  }

  private checkPendingDeployment(): void {
    try {
      const pendingDeploy = localStorage.getItem(this.STORAGE_KEY);
      if (pendingDeploy) {
        const deployData = JSON.parse(pendingDeploy);
        if (this.isValidDeploymentData(deployData)) {
          this.deploymentData = {
            repoUrl: deployData.repoUrl,
            imageName: deployData.imageName || 'undefined',
            hostPort: deployData.port || 8080,
            containerPort: deployData.port || 8080
          };
        }
        setTimeout(() => this.startContainerization(), 1000);

        localStorage.removeItem(this.STORAGE_KEY);
      }
    } catch (e) {
      console.error('Error parsing pending deployment', e);
      localStorage.removeItem(this.STORAGE_KEY);
    }
  }

  private isValidDeploymentData(data: any): boolean {
    return data && 
           typeof data.repoUrl === 'string' && 
           data.repoUrl.trim() !== '';
  }

  async startContainerization(): Promise<void> {
    if (!this.isValidGitUrl(this.deploymentData.repoUrl)) {
      this.error = 'URL de dépôt GitLab invalide';
      return;
    }

    this.isLoading = true;
    this.error = null;
    try {
      const isAuthenticated = await lastValueFrom(
        this.dockerService.checkGitLabAuth().pipe(
          catchError(() => of(false))
        )
      );

      if (!isAuthenticated) {
        this.storePendingDeployment();
        this.redirectToAuth();
        return;
      }

      await this.executeDeployment();
    } catch (err) {
      this.handleError(err);
    } finally {
      this.isLoading = false;
    }
  }

  private isValidGitUrl(url: string): boolean {
    return url.startsWith('https://') || url.startsWith('git@');
  }

  private storePendingDeployment(): void {
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify({
      repoUrl: this.deploymentData.repoUrl,
      imageName: this.deploymentData.imageName,
      port: this.deploymentData.hostPort
    }));
  }

  private redirectToAuth(): void {
    const currentUrl = encodeURIComponent(window.location.href);
    window.location.href = 'http://localhost:8080/oauth2/authorization/gitlab';
  }

private async executeDeployment(): Promise<void> {
  this.isLoading = true;
  this.realTimeLogs = []; // Réinitialisez les logs pour chaque nouveau déploiement
  this.realTimeLogs.push('Démarrage du déploiement...');

  try {
    await this.dockerService.deploy(
      this.deploymentData.repoUrl,
      this.deploymentData.imageName,
      this.deploymentData.hostPort
    ).pipe(
      filter(response => {
        console.log('[DEBUG] Status:', response.status);
        return response.status !== 'pending';  // on ignore les réponses 'pending'
      }),
      catchError(err => {
        this.handleError(err);  // centralise la gestion des erreurs
        return EMPTY; // évite de re-jeter l’erreur
      })
    ).toPromise(); // attend l'exécution de l'observable (équivalent à firstValueFrom sans crash)
     this.realTimeLogs.push('Déploiement terminé avec succès!');

  } finally {
    this.isLoading = false;
  }
}


private handleError(error: any): void {
  console.error('Deployment error:', error);
  
  if (error?.status === 401) {
    this.error = 'Authentification requise - Redirection vers GitLab...';
    this.storePendingDeployment();
    setTimeout(() => this.redirectToAuth(), 1500);
  } else if (error?.name === 'TimeoutError') {
    this.error = 'Déploiement trop long (5 minutes maximum)';
  } else if (error?.message?.includes('GitLab OAuth')) {
    // Ne pas afficher d'erreur pour les redirections OAuth
    return;
  } else {
    this.error = error?.message || 
               error?.error?.message || 
               'Erreur technique lors du déploiement';
  }
}

  get applicationUrl(): string {
    if (!this.result) return '';
    return `http://${this.serverIp}:${this.result.port}`;
  }

  get canDeploy(): boolean {
    return !!this.deploymentData.repoUrl && !this.isLoading;
  }

  toggleLogs(): void {
    this.showLogs = !this.showLogs;
  }

  async stopContainer(): Promise<void> {
    if (!this.result?.containerId) return;

    try {
      await lastValueFrom(
        this.dockerService.stopContainer(this.result.containerId)
      );
      this.result = null;
    } catch (err: unknown) {
      this.handleError(err);
    }
  }

  resetForm(): void {
    this.deploymentData = {
      repoUrl: '',
      imageName: '',
      hostPort: 8080,
      containerPort: 8080
    };
    this.result = null;
    this.error = null;
  }
  retry(): void {
    this.error = null;
    this.startContainerization();
  }
  viewContainerDetails(): void {
    // Implémentez la navigation vers les détails
    console.log('View details for:', !this.result?.containerId);
  }
}
