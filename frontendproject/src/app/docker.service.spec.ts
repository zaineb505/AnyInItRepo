import { TestBed } from '@angular/core/testing';

import { DockerService } from './docker.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('DockerService', () => {
  let service: DockerService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(DockerService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
