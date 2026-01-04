import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { catchError, map, Observable, of, switchMap, throwError } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';

interface DockerImage {
  image: string;
  name: string;
}

interface PipelineConfig {
  repoUrl: string;
  language: string;
  dockerImage: string;
  includeBuild: boolean;
  includeTest: boolean;
}
/*************************/
interface UserStatus {
  is_first_deployment: boolean;
  gitlab_user_id: number;
}
/************************/
@Injectable({
  providedIn: 'root'
})
export class GitlabService {
  private apiUrl = 'http://localhost:8080/api/gitlab';
  private authUrl = 'http://localhost:8080/api/auth/gitlab';


  // Images Docker prédéfinies par langage
  dockerImages: Record<string, DockerImage[]> = {
    nodejs: [
      { image: 'node:20', name: 'Node 20 (latest)' },
      { image: 'node:18', name: 'Node 18 (LTS)' },
      { image: 'node:16', name: 'Node 16' }
    ],
    java_maven: [
      { image: 'maven:3.8-openjdk-17', name: 'Maven + JDK 17 (LTS)' },
      { image: 'maven:3.9-openjdk-21', name: 'Maven + JDK 21 (Latest)' }

    ],
    java_gradle: [
      { image: 'gradle:8-jdk17', name: 'Gradle + JDK 17' },
      { image: 'gradle:8-jdk21', name: 'Gradle + JDK 21' }

    ],
    python: [
      { image: 'python:3.10', name: 'Python 3.10' },
      { image: 'python:3.9', name: 'Python 3.9' }
    ]
  };

  constructor(  @Inject(PLATFORM_ID) private platformId: Object,
    private http: HttpClient,) { }

  generatePipeline(config: PipelineConfig): Observable<string> {
    return this.http.post(
      `${this.apiUrl}/generate-pipeline`,
      {
        repoUrl: config.repoUrl,
        language: config.language,
        dockerImage: config.dockerImage,
        includeBuild: config.includeBuild,
        includeTest: config.includeTest
      },
      {
        responseType: 'text' as const,// Important change
        withCredentials: true
      }
    );
  }

  /*deployPipeline(repoUrl: string, yamlContent: string): Observable<void> {
    return this.http.post<void>(
      `${this.apiUrl}/deploy-pipeline`,
      { repoUrl, yamlContent },
      { withCredentials: true }
    );
  }
*/


  checkGitLabAuth(): Observable<boolean> {
    return this.http.get<{ authenticated: boolean }>(`${this.authUrl}/check-auth`, {
      withCredentials: true
    }).pipe(
      map(response => response.authenticated),
      catchError(() => of(false))
    );
  }


deployPipeline(repoUrl: string, yamlContent: string, branch: string = 'master'): Observable<void> {
    return this.checkGitLabAuth().pipe(
      switchMap(authenticated => {
        if (!authenticated) {
          if (isPlatformBrowser(this.platformId)) {
            // Sauvegarder l'état actuel avant redirection
            localStorage.setItem('gitlab_pending_deploy', JSON.stringify({
              repoUrl, yamlContent, branch
            }));
            // Rediriger vers l'authentification GitLab
            window.location.href = 'http://localhost:8080/oauth2/authorization/gitlab';
          }
          return throwError(() => new Error('Redirection vers GitLab OAuth'));
        }
        // Si authentifié, procéder au déploiement
        return this.http.post<void>(
          `${this.apiUrl}/deploy-pipeline`,
          { repoUrl, yamlContent, branch },
          { withCredentials: true }
        );
      })
    );
  }

  /************************************************/
  getUserStatus(): Observable<UserStatus> {
    return this.http.get<UserStatus>(
      `${this.apiUrl}/user-status`,
      { withCredentials: true }
    );
  }

  markAsDeployed(userId: number): Observable<void> {
    return this.http.post<void>(
      `${this.apiUrl}/mark-deployed`,
      { gitlabUserId: userId }, // Envoyez comme objet JSON
      {
        withCredentials: true,
        headers: { 'Content-Type': 'application/json' }
      }
    );
  }
  /********************************************************/
}
