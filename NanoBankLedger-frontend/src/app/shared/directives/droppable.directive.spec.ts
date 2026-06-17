import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DroppableDirective } from './droppable.directive';
import { By } from '@angular/platform-browser';

@Component({
  template: `<div appDroppable></div>`,
  standalone: true,
  imports: [DroppableDirective]
})
class TestHostComponent {}

describe('DroppableDirective', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let directiveEl: DebugElement;
  let directive: DroppableDirective;
  let nativeEl: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();

    directiveEl = fixture.debugElement.query(By.directive(DroppableDirective));
    directive = directiveEl.injector.get(DroppableDirective);
    nativeEl = directiveEl.nativeElement as HTMLElement;
  });

  it('should create an instance', () => {
    expect(directive).toBeTruthy();
  });

  it('should not have drag-over class initially', () => {
    expect(directive.isDragOver).toBeFalse();
  });

  it('should emit dropped event', () => {
    spyOn(directive.dropped, 'emit');

    const dataTransfer = new DataTransfer();
    const dropEvent = new DragEvent('drop', { dataTransfer, bubbles: true });
    nativeEl.dispatchEvent(dropEvent);

    expect(directive.dropped.emit).toHaveBeenCalledWith(dropEvent);
  });

  it('should not be drag-over after drop', () => {
    const dataTransfer = new DataTransfer();
    nativeEl.dispatchEvent(new DragEvent('drop', { dataTransfer, bubbles: true }));

    expect(directive.isDragOver).toBeFalse();
  });

  it('should set drag-over class on dragover', () => {
    const dataTransfer = new DataTransfer();
    const dragOverEvent = new DragEvent('dragover', { dataTransfer, bubbles: true });
    nativeEl.dispatchEvent(dragOverEvent);

    expect(directive.isDragOver).toBeTrue();
  });

  it('should not be drag-over after dragleave', () => {
    nativeEl.dispatchEvent(new DragEvent('dragleave', { bubbles: true }));

    expect(directive.isDragOver).toBeFalse();
  });

  it('should set drag-over true on dragover then false on dragleave', () => {
    const dataTransfer = new DataTransfer();
    nativeEl.dispatchEvent(new DragEvent('dragover', { dataTransfer, bubbles: true }));
    expect(directive.isDragOver).toBeTrue();

    nativeEl.dispatchEvent(new DragEvent('dragleave', { bubbles: true }));
    expect(directive.isDragOver).toBeFalse();
  });
});
