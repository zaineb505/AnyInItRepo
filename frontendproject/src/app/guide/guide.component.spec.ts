import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GuideComponent } from './guide.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('GuideComponent', () => {
  let component: GuideComponent;
  let fixture: ComponentFixture<GuideComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GuideComponent, HttpClientTestingModule]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GuideComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
