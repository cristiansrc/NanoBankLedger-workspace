import { Directive, HostBinding, HostListener, Input } from '@angular/core';

@Directive({
  selector: '[appDraggable]',
  standalone: true
})
export class DraggableDirective {
  @Input({ required: true }) appDraggable!: string; // transaction id
  @Input() dragData: any;

  @HostBinding('class.dragging') isDragging = false;
  @HostBinding('attr.draggable') draggable = true;

  @HostListener('dragstart', ['$event'])
  onDragStart(event: DragEvent): void {
    this.isDragging = true;
    if (this.dragData) {
      event.dataTransfer?.setData('application/json', JSON.stringify(this.dragData));
    }
    event.dataTransfer!.effectAllowed = 'move';
  }

  @HostListener('dragend')
  onDragEnd(): void {
    this.isDragging = false;
  }
}
