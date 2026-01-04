import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';

@Component({
  selector: 'app-gitlab-verification-modal',
  standalone: true,
  imports: [],
  templateUrl: './gitlab-verification-modal.component.html',
  styleUrl: './gitlab-verification-modal.component.css'
})
export class GitlabVerificationModalComponent {
  constructor(
    public dialogRef: MatDialogRef<GitlabVerificationModalComponent>,
    @Inject(MAT_DIALOG_DATA) public data: {
      title: string;
      message: string;
      verificationUrl: string;
    }
  ) { }
  confirm() {
    this.dialogRef.close(true);
  }

  cancel() {
    this.dialogRef.close(false);
  }


}
