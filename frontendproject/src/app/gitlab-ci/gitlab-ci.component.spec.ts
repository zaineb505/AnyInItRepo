import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { GitlabCiComponent } from './gitlab-ci.component';

describe('GitlabCiComponent', () => {
  let component: GitlabCiComponent;
  let fixture: ComponentFixture<GitlabCiComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GitlabCiComponent , HttpClientTestingModule]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GitlabCiComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
