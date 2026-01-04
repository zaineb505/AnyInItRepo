import { TestBed } from '@angular/core/testing';

import { LogWebsocketService } from './log-websocket.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('LogWebsocketService', () => {
  let service: LogWebsocketService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      
    });
    service = TestBed.inject(LogWebsocketService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
