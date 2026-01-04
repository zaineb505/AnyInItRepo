import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpEventType, HttpHeaders, HttpParams } from '@angular/common/http';
import { catchError, map, Observable, of, switchMap, throwError, timeout, retryWhen, delay, take, NEVER, startWith, filter } from 'rxjs';


export interface DockerDeploymentResponse {
  containerId: string;
  port: number;
  status: string;
  buildLogs?: string;  // Optionnel selon le backend
  error?: boolean;
    message?: string;

}
@Injectable({
  providedIn: 'root'
})
export class DockerService {
private apiUrl =  'http://localhost:8080/api/docker';
  constructor(private http: HttpClient) { }


  checkGitLabAuth(): Observable<boolean> {
    return this.http.get<{ authenticated: boolean }>(
      `${this.apiUrl}/auth/status`,
      {
        withCredentials: true,
      }
    ).pipe(
      map(response => response.authenticated),
      catchError(() => of(false))
    );
  }

  deploy(repoUrl: string, imageName?: string, hostPort?: number, containerPort: number = 8080): Observable<DockerDeploymentResponse> {
        if (!repoUrl || hostPort === undefined || hostPort === null) {
        return throwError(() => new Error('repoUrl et hostPort sont obligatoires'));
    }
    const formData = new FormData();
    formData.append('repoUrl', repoUrl);
    if (imageName) formData.append('imageName', imageName);
    formData.append('hostPort', hostPort.toString());
    return this.checkGitLabAuth().pipe(
      switchMap(authenticated => {
        if (!authenticated) {
          // Sauvegarder l'état actuel avant redirection
          localStorage.setItem('pending_deployment', JSON.stringify({
            repoUrl, imageName, hostPort
          }));
          // Rediriger vers l'authentification GitLab
          window.location.href = 'http://localhost:8080/oauth2/authorization/gitlab';
          //return throwError(() => new Error('Redirection vers GitLab OAuth'));
           return NEVER;
        }
return this.http.post<DockerDeploymentResponse>(
  `${this.apiUrl}/deploy`,
  formData,
  { withCredentials: true,
    reportProgress: true, // Pour suivre la progression
      observe: 'events' // Permet de recevoir des événements progressifs

   }
   
).pipe(
    filter(event => this.isDeploymentEventWithLogs(event)),
    map(event => this.extractDeploymentData(event))
  );

    }),
  );
}

private isDeploymentEventWithLogs(event: any): boolean {
  return event.type === HttpEventType.Response 
      || (event.type === HttpEventType.DownloadProgress && event.partialText);
}

private extractDeploymentData(event: any): DockerDeploymentResponse {
  if (event.type === HttpEventType.Response) {
    return event.body;
  } else {
    return { 
      status: 'building', 
      buildLogs: event.partialText,
      containerId: '',
      port: 0
    };
  }
}
  stopContainer(containerId: string): Observable<void> {
    return this.http.post<void>(
      `${this.apiUrl}/stop`,
      null,
      { params: new HttpParams().set('containerId', containerId) }
    ).pipe(
      catchError(this.handleError)
    );
  }

  private handleError(error: HttpErrorResponse) {
    let errorMessage = 'Une erreur inconnue est survenue';

    if (error.error instanceof ErrorEvent) {
      // Erreur côté client
      errorMessage = `Erreur: ${error.error.message}`;
    } else {
      // Erreur côté serveur
      errorMessage = error.error?.message ||
        error.statusText ||
        `Erreur ${error.status}: ${error.message}`;
    }

    console.error('Erreur DockerService:', error);
    return throwError(() => new Error(errorMessage));
  }
}

