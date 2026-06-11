import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CardComponent, CardVariant, CardPadding } from './card.component';

describe('CardComponent', () => {
  let fixture: ComponentFixture<CardComponent>;
  let component: CardComponent;

  const createFixture = () => {
    fixture = TestBed.createComponent(CardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CardComponent],
    }).compileComponents();
  });

  it('should create', () => {
    createFixture();
    expect(component).toBeTruthy();
  });

  it('should have default values', () => {
    createFixture();
    expect(component.variant()).toBe('default');
    expect(component.padding()).toBe('md');
    expect(component.hoverable()).toBe(false);
  });

  it('should render card with default classes', () => {
    createFixture();
    const card = fixture.nativeElement.querySelector('.card');
    expect(card.classList).toContain('card');
    expect(card.classList).toContain('card--default');
    expect(card.classList).toContain('card--padding-md');
  });

  it('should include interactive class when hoverable is true', () => {
    createFixture();
    fixture.componentRef.setInput('hoverable', true);
    fixture.detectChanges();
    
    const card = fixture.nativeElement.querySelector('.card');
    expect(card.classList).toContain('card--interactive');
  });

  it('should not include interactive class when hoverable is false', () => {
    createFixture();
    fixture.componentRef.setInput('hoverable', false);
    fixture.detectChanges();
    
    const card = fixture.nativeElement.querySelector('.card');
    expect(card.classList).not.toContain('card--interactive');
  });

  describe('variants', () => {
    const variants: CardVariant[] = ['default', 'elevated', 'outlined', 'glass'];
    variants.forEach((variant) => {
      it(`should support ${variant} variant`, () => {
        createFixture();
        fixture.componentRef.setInput('variant', variant);
        fixture.detectChanges();
        
        const card = fixture.nativeElement.querySelector('.card');
        expect(card.classList).toContain(`card--${variant}`);
      });
    });
  });

  describe('padding', () => {
    const paddings: CardPadding[] = ['none', 'sm', 'md', 'lg'];
    paddings.forEach((padding) => {
      it(`should support ${padding} padding`, () => {
        createFixture();
        fixture.componentRef.setInput('padding', padding);
        fixture.detectChanges();
        
        const card = fixture.nativeElement.querySelector('.card');
        expect(card.classList).toContain(`card--padding-${padding}`);
      });
    });
  });

  describe('hover state', () => {
    it('should apply interactive class when hoverable is true', () => {
      createFixture();
      fixture.componentRef.setInput('hoverable', true);
      fixture.detectChanges();
      
      const card = fixture.nativeElement.querySelector('.card');
      expect(card.classList).toContain('card--interactive');
    });

    it('should apply interactive class for hoverable cards', () => {
      createFixture();
      fixture.componentRef.setInput('hoverable', true);
      fixture.detectChanges();
      
      const card = fixture.nativeElement.querySelector('.card');
      expect(card.classList).toContain('card--interactive');
    });
  });
});
