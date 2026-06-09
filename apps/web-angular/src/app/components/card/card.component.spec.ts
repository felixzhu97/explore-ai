import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CardComponent, CardVariant, CardPadding } from './card.component';

describe('CardComponent', () => {
  let fixture: ComponentFixture<CardComponent>;
  let component: CardComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CardComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(CardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have default values', () => {
    expect(component.variant()).toBe('default');
    expect(component.padding()).toBe('md');
    expect(component.hoverable()).toBe(false);
  });

  it('should compute card classes correctly', () => {
    expect(component.cardClasses()).toContain('card--default');
    expect(component.cardClasses()).toContain('card--padding-md');
    expect(component.cardClasses()).not.toContain('card--interactive');
  });

  it('should include interactive class when hoverable is true', () => {
    component.hoverable.set(true);
    expect(component.cardClasses()).toContain('card--interactive');
  });

  it('should include interactive class when onClick is provided', () => {
    component.hoverable.set(false);
    expect(component.cardClasses()).not.toContain('card--interactive');
  });

  describe('variants', () => {
    const variants: CardVariant[] = ['default', 'elevated', 'outlined', 'glass'];
    variants.forEach((variant) => {
      it(`should support ${variant} variant`, () => {
        component.variant.set(variant);
        expect(component.cardClasses()).toContain(`card--${variant}`);
      });
    });
  });

  describe('padding', () => {
    const paddings: CardPadding[] = ['none', 'sm', 'md', 'lg'];
    paddings.forEach((padding) => {
      it(`should support ${padding} padding`, () => {
        component.padding.set(padding);
        expect(component.cardClasses()).toContain(`card--padding-${padding}`);
      });
    });
  });

  describe('hover state', () => {
    it('should update hovered state on mouseenter', () => {
      component.hoverable.set(true);
      const cardElement = fixture.nativeElement.querySelector('.card');
      
      cardElement.dispatchEvent(new Event('mouseenter'));
      fixture.detectChanges();
      
      expect(component.isHovered()).toBe(true);
      expect(component.cardClasses()).toContain('card--hovered');
    });

    it('should clear hovered state on mouseleave', () => {
      component.hoverable.set(true);
      const cardElement = fixture.nativeElement.querySelector('.card');
      
      cardElement.dispatchEvent(new Event('mouseenter'));
      cardElement.dispatchEvent(new Event('mouseleave'));
      fixture.detectChanges();
      
      expect(component.isHovered()).toBe(false);
      expect(component.cardClasses()).not.toContain('card--hovered');
    });
  });
});
