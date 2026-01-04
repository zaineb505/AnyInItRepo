import { Directive, Output, EventEmitter, HostListener, ElementRef } from '@angular/core';

@Directive({
  standalone: true, 
  selector: '[clickOutside]'
})
export class ClickOutsideDirective {
  @Output() clickOutside = new EventEmitter<void>();

  @HostListener('document:click', ['$event.target'])
  public onClick(target: any) {
    const clickedInside = this.elementRef.nativeElement.contains(target);
    if (!clickedInside) {
      this.clickOutside.emit();
    }
  }

  constructor(private elementRef: ElementRef) {}
}
