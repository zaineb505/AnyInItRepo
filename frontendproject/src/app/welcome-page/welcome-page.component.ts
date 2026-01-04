import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule, NgIf } from '@angular/common';
import { GuideComponent } from '../guide/guide.component';

@Component({
  selector: 'app-welcome-page',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, NgIf, GuideComponent, RouterModule],
  templateUrl: './welcome-page.component.html',
  styleUrl: './welcome-page.component.css',
})
export class WelcomePageComponent {
    activeStep: number = 1;
  contactForm: FormGroup;
  formSubmitted = false;
  formError = '';
  emailAddress = 'contact@deployanyit.com';

  constructor(
    private router: Router,
    @Inject(DOCUMENT) private document: Document,
    @Inject(PLATFORM_ID) private platformId: Object,
    private fb: FormBuilder
  ) {
    this.contactForm = this.fb.group({
      name: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      message: ['', Validators.required]
    });
  }

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.checkHashOnInit();
      window.addEventListener('hashchange', () => {
        const hash = window.location.hash;
        if (hash === '#pipeline-section' || hash === '#contact-section') {
          this.scrollToSection(hash.substring(1));
        }
      });
    }
  }

  navigateToLogin() {
    this.router.navigate(['/login']);
  }

  scrollToSection(sectionId: string, event?: Event) {
    if (event) event.preventDefault();
    const element = this.document.getElementById(sectionId);
    if (element) {
      const navbarHeight = 80; // Adjust this value to match your navbar height
      const elementPosition = element.getBoundingClientRect().top;
      const offsetPosition = elementPosition + window.pageYOffset - navbarHeight;

      window.scrollTo({
        top: offsetPosition,
        behavior: 'smooth'
      });

      history.replaceState(null, '', `#${sectionId}`);
    }
  }
  private checkHashOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      const hash = window.location.hash;
      if (hash === '#pipeline-section' || hash === '#contact-section') {
        setTimeout(() => this.scrollToSection(hash.substring(1)), 100);
      }
    }
  }


  private scrollToHash() {
    const element = this.document.getElementById('pipeline-section');
    if (element) {
      setTimeout(() => {
        element.scrollIntoView({ behavior: 'smooth' });
      }, 100);
    }
  }

  // Pipeline Step Navigation
  nextStep() {
    if (this.activeStep < 4) {
      this.activeStep++;
    }
  }

  prevStep() {
    if (this.activeStep > 1) {
      this.activeStep--;
    }
  }

  activateStep(step: number) {
    this.activeStep = step;
  }

  // Contact Form Methods
  onSubmit() {
    if (this.contactForm.valid) {
      console.log('Form submitted:', this.contactForm.value);
      this.formSubmitted = true;
      this.contactForm.reset();

      setTimeout(() => {
        this.formSubmitted = false;
      }, 3000);
    } else {
      this.formError = 'Please fill all required fields correctly';
    }
  }
  // Add this to your component class
steps = [
  {
    id: 1,
    icon: 'assets/github-142-svgrepo-com.svg',
    title: 'Code Management',
    subtitle: 'GitHub',
    docsUrl: 'https://docs.github.com'  // GitHub docs URL
  },
  {
    id: 2,
    icon: 'assets/gitlab-svgrepo-com.svg',
    title: 'Test & Build',
    subtitle: 'GitLab CI/CD',
    docsUrl: 'https://docs.gitlab.com/ee/ci/'  // GitLab docs URL
  },
  {
    id: 3,
    icon: 'assets/docker-svgrepo-com (1).svg',
    title: 'Containerization',
    subtitle: 'Docker',
    docsUrl: 'https://docs.docker.com'  // Docker docs URL
  },
  {
    id: 4,
    icon: 'assets/kubernetes-svgrepo-com.svg',
    title: 'Deployment',
    subtitle: 'Kubernetes',
    docsUrl: 'https://kubernetes.io/docs/home/'  // Kubernetes docs URL
  }
];

// Add this method to handle navigation
navigateToDocs(url: string, event: Event) {
  event.stopPropagation(); // Prevent triggering the step activation
  window.open(url, '_blank'); // Open in new tab
}
}

