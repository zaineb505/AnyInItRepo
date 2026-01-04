import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { GithubReposComponent } from './github-repos.component';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

describe('GithubReposComponent', () => {
  let component: GithubReposComponent;
  let fixture: ComponentFixture<GithubReposComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        GithubReposComponent,
        HttpClientTestingModule
      ],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            // Mock ce dont ton composant a besoin, par exemple un paramMap observable vide
            paramMap: of(new Map())
          }
        }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GithubReposComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
