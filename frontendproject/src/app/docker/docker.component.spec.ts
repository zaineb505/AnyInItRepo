import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DockerComponent } from './docker.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('DockerComponent', () => {
  let component: DockerComponent;
  let fixture: ComponentFixture<DockerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DockerComponent, HttpClientTestingModule]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DockerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
