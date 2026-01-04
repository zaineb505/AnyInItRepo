import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { CallbackComponent } from './callback/callback.component';
import { GithubReposComponent } from './github-repos/github-repos.component';
import { GitlabCiComponent } from './gitlab-ci/gitlab-ci.component';
import { StepsComponent } from './steps/steps.component';
import { WelcomePageComponent } from './welcome-page/welcome-page.component';
import { DockerComponent } from './docker/docker.component';
import { DocumentationComponent } from './documentation/documentation.component';
import { AppComponent } from './app.component';
import { MainlayoutComponent } from './mainlayout/mainlayout.component';
import { GuideComponent } from './guide/guide.component';
export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },

  // Routes without header
  { path: 'login', component: LoginComponent },
  { path: 'welcomePage', component: WelcomePageComponent },
  { path: 'guide', component: GuideComponent },

  // Routes with header
  {
    path: '',
    component: MainlayoutComponent,
    children: [
      { path: 'documentation', component: DocumentationComponent },
      {
        path: '',
        component: StepsComponent,
        children: [
          { path: 'github-repos', component: GithubReposComponent },
          { path: 'gitlab-ci', component: GitlabCiComponent },
          { path: 'docker', component: DockerComponent },
          { path: '', redirectTo: 'github-repos', pathMatch: 'full' }
        ]
      }
    ]
  },

  { path: 'callback', component: CallbackComponent } // Add this where appropriate
];