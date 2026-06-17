import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DraggableDirective } from './draggable.directive';
import { By } from '@angular/platform-browser';

@Component({
  template: `<div [appDraggable]="transactionId" [dragData]="dragData"></div>`,
  standalone: true,
  imports: [DraggableDirective]
})
class TestHostComponent {
  transactionId = 'txn-1';
  dragData: any = { id: 'txn-1', amount: '50.00', type: 'EXPENSE', walletId: 'w1' };
}

describe('DraggableDirective', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let directiveEl: DebugElement;
  let directive: DraggableDirective;
  let nativeEl: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();

    directiveEl = fixture.debugElement.query(By.directive(DraggableDirective));
    directive = directiveEl.injector.get(DraggableDirective);
    nativeEl = directiveEl.nativeElement as HTMLElement;
  });

  it('should create an instance', () => {
    expect(directive).toBeTruthy();
  });

  it('should have draggable attribute', () => {
    expect(nativeEl.getAttribute('draggable')).toBe('true');
  });

  it('should not have dragging class initially', () => {
    expect(nativeEl.classList.contains('dragging')).toBeFalse();
  });

  it('should set dragging on dragstart', () => {
    const dataTransfer = new DataTransfer();
    nativeEl.dispatchEvent(new DragEvent('dragstart', { dataTransfer, bubbles: true }));

    expect(directive.isDragging).toBeTrue();
  });

  it('should set drag data as JSON on dataTransfer', () => {
    const dataTransfer = new DataTransfer();
    nativeEl.dispatchEvent(new DragEvent('dragstart', { dataTransfer, bubbles: true }));

    const rawData = dataTransfer.getData('application/json');
    expect(rawData).toBe(JSON.stringify({ id: 'txn-1', amount: '50.00', type: 'EXPENSE', walletId: 'w1' }));
  });

  it('should remove dragging on dragend', () => {
    const dt = new DataTransfer();
    nativeEl.dispatchEvent(new DragEvent('dragstart', { dataTransfer: dt, bubbles: true }));
    expect(directive.isDragging).toBeTrue();

    nativeEl.dispatchEvent(new DragEvent('dragend', { bubbles: true }));
    expect(directive.isDragging).toBeFalse();
  });

  it('should not call dragstart handler if no dragData', () => {
    fixture.componentInstance.dragData = undefined;
    fixture.detectChanges();

    const dataTransfer = new DataTransfer();
    nativeEl.dispatchEvent(new DragEvent('dragstart', { dataTransfer, bubbles: true }));

    const rawData = dataTransfer.getData('application/json');
    expect(rawData).toBe('');
    expect(directive.isDragging).toBeTrue();
  });
});
