// login.component.ts
import { Component } from '@angular/core';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html', // Reference external file
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  constructor(private authService: AuthService) {}

  loginWithGithub() {
    this.authService.loginWithGithub();
  }

  loginWithGoogle() {
    this.authService.loginWithGoogle();
  }
}
