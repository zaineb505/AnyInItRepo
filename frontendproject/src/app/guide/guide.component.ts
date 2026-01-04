import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-guide',
  standalone: true,
  imports: [RouterModule],
  templateUrl: './guide.component.html',
  styleUrl: './guide.component.css'
})
export class GuideComponent {
scrollTo(sectionId: string) {
    const element = document.getElementById(sectionId);
    if (element) {
      element.scrollIntoView({
        behavior: 'smooth',
        block: 'start'
      });
    }
  }
}
