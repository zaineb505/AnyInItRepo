import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, Observable, of, throwError } from 'rxjs';
import { GitHubRepo } from './models/github-repo';

@Injectable({
  providedIn: 'root'
})

export class GitHubService {
  private apiUrl = 'http://localhost:8080/api/github';

  constructor(private http: HttpClient) { }

  getRepositories(): Observable<GitHubRepo[]> {
    return this.http.get<GitHubRepo[]>(`${this.apiUrl}/repos`, {
      withCredentials: true
    });
  }

/*********************************** */
  cloneRepository(repoUrl: string, branch: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/clone`, null, {
      params: { repoUrl, branch },
      withCredentials: true
    });
  }
//********************************* */
cloneToDesktop(repoUrl: string, branch: string): Observable<any> {
  return this.http.post(`${this.apiUrl}/clone-to-desktop`, null, {
    params: { repoUrl, branch },
    withCredentials: true
  });
}

  /* ******************   PUSH  &  COMMIT    *************************** */
  commitAndPush(repoPath: string, commitMessage: string, branchName: string, remoteUrl: string): Observable<any> {
    const normalizedPath = repoPath.replace(/\\/g, '/');
    return this.http.post(`${this.apiUrl}/commit-and-push`, null, {
      params: {
        repoPath,
        commitMessage,
        branchName,
        remoteUrl
      },
      withCredentials: true
    });

  }

/*********************************pull*/////////////////////////////

pullRepository(repoPath: string, branch: string): Observable<any> {
  const normalizedPath = repoPath.replace(/\\/g, '/');
  return this.http.post(`${this.apiUrl}/pull-existing`, null, {
    params: {
      repoPath: normalizedPath,
      branch: branch || 'main'
    },
    withCredentials: true
  }).pipe(
    catchError(error => {
      console.error('Pull error:', error);
      return throwError(() => new Error(
        error.error?.error ||
        error.message ||
        'Erreur inconnue lors du pull'
      ));
    })
  );
}
// In GitHubService
detectLocalBranch(repoPath: string): Observable<string> {
  const normalizedPath = repoPath.replace(/\\/g, '/');
  return this.http.get<string>(`${this.apiUrl}/detect-branch`, {
    params: { repoPath: normalizedPath },
    withCredentials: true
  }).pipe(
    catchError(() => of('main')) // Fallback to 'main' if detection fails
  );
}}

