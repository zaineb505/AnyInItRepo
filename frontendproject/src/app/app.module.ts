import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterModule } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { FormsModule } from '@angular/forms';
// Angular Material Modules
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule, MatIconRegistry } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatCardModule } from '@angular/material/card';
import { MatMenuModule } from '@angular/material/menu';
import { DomSanitizer } from '@angular/platform-browser';

// Components
import { AppComponent } from './app.component';
import { LoginComponent } from './login/login.component';
import { CallbackComponent } from './callback/callback.component';
import { HeaderComponent } from './header/header.component';
// Services
import { AuthService } from './auth.service';
import { LogWebsocketService } from './log-websocket.service';

// Routes
import { routes } from './app.routes';
import { GitlabCiComponent } from './gitlab-ci/gitlab-ci.component';
import { WelcomePageComponent } from './welcome-page/welcome-page.component';
import { ClickOutsideDirective } from './click-outside.directive';
import { DockerComponent } from './docker/docker.component';
import { DocumentationComponent } from './documentation/documentation.component';
import { GuideComponent } from './guide/guide.component';
//import { ToastrModule } from 'ngx-toastr';



@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    CallbackComponent,
    GitlabCiComponent,
    WelcomePageComponent,
    HeaderComponent,
    ClickOutsideDirective,
    DockerComponent,
    DocumentationComponent,
    GuideComponent


  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    RouterModule.forRoot(routes),
    MatButtonModule,
    MatIconModule,
    MatToolbarModule,
    MatCardModule,
    MatMenuModule,
    LogWebsocketService,
    FormsModule
  ],
  providers: [
    provideHttpClient(),
    AuthService,
    MatDialogRef,
    MAT_DIALOG_DATA,
    ActivatedRoute
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
  constructor(
    private matIconRegistry: MatIconRegistry,
    private domSanitizer: DomSanitizer
  ) {
    this.registerSvgs();
  }

  private registerSvgs() {
    // Use forward slashes and correct asset path
    this.matIconRegistry.addSvgIcon(
      'DeployAnyInit',
      this.domSanitizer.bypassSecurityTrustResourceUrl('assets\logo-noir-1.png')
    );
    this.matIconRegistry.addSvgIcon(
      'google',
      this.domSanitizer.bypassSecurityTrustResourceUrl('assets/google-color-svgrepo-com.svg')
    );
    this.matIconRegistry.addSvgIcon(
      'github',
      this.domSanitizer.bypassSecurityTrustResourceUrl('assets/github-svgrepo-com.svg')
    );
  }
}
