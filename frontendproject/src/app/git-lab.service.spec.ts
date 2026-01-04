import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { GitlabService } from './git-lab.service';

describe('GitLabService', () => {
  let service: GitlabService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [GitlabService]
    });
    service = TestBed.inject(GitlabService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
