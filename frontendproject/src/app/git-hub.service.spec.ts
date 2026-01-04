import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { GitHubService } from './git-hub.service';

describe('GitHubService', () => {
  let service: GitHubService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],  // ✅ ajoute ceci
      providers: [GitHubService]
    });
    service = TestBed.inject(GitHubService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
