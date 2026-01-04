import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { GitlabVerificationModalComponent } from './gitlab-verification-modal.component';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

describe('GitlabVerificationModalComponent', () => {
  let component: GitlabVerificationModalComponent;
  let fixture: ComponentFixture<GitlabVerificationModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        GitlabVerificationModalComponent,
        HttpClientTestingModule
      ],
      providers: [
        {
          provide: MatDialogRef,
          useValue: {}  // mock du dialogRef
        },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {}  // données fictives que ton composant attend
        }
      ]     
    })
    .compileComponents();

    fixture = TestBed.createComponent(GitlabVerificationModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
