import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CardComponent } from './card.component';

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
    const card = fixture.nativeElement.querySelector('div');
    expect(card.classList).toContain('bg-surface');
    expect(card.classList).toContain('shadow-card');
  });

  it('should include hover classes when hoverable is true', () => {
    createFixture();
    fixture.componentRef.setInput('hoverable', true);
    fixture.detectChanges();

    const card = fixture.nativeElement.querySelector('div');
    expect(card.classList).toContain('cursor-pointer');
    expect(card.classList).toContain('hover:-translate-y-0.5');
  });

  describe('variants', () => {
    it('should apply default variant styles', () => {
      createFixture();
      fixture.componentRef.setInput('variant', 'default');
      fixture.detectChanges();

      const card = fixture.nativeElement.querySelector('div');
      expect(card.classList).toContain('shadow-card');
    });

    it('should apply elevated variant styles', () => {
      createFixture();
      fixture.componentRef.setInput('variant', 'elevated');
      fixture.detectChanges();

      const card = fixture.nativeElement.querySelector('div');
      expect(card.classList).toContain('shadow-elevated');
    });

    it('should apply outlined variant styles', () => {
      createFixture();
      fixture.componentRef.setInput('variant', 'outlined');
      fixture.detectChanges();

      const card = fixture.nativeElement.querySelector('div');
      expect(card.classList).toContain('border');
    });

    it('should apply glass variant styles', () => {
      createFixture();
      fixture.componentRef.setInput('variant', 'glass');
      fixture.detectChanges();

      const card = fixture.nativeElement.querySelector('div');
      expect(card.classList).toContain('backdrop-blur-xl');
    });
  });

  describe('padding', () => {
    it('should not have padding for none', () => {
      createFixture();
      fixture.componentRef.setInput('padding', 'none');
      fixture.detectChanges();

      const card = fixture.nativeElement.querySelector('div');
      expect(card.classList).not.toContain('p-');
    });

    it('should apply sm padding', () => {
      createFixture();
      fixture.componentRef.setInput('padding', 'sm');
      fixture.detectChanges();

      const card = fixture.nativeElement.querySelector('div');
      expect(card.classList).toContain('p-2');
    });

    it('should apply md padding', () => {
      createFixture();
      fixture.componentRef.setInput('padding', 'md');
      fixture.detectChanges();

      const card = fixture.nativeElement.querySelector('div');
      expect(card.classList).toContain('p-4');
    });

    it('should apply lg padding', () => {
      createFixture();
      fixture.componentRef.setInput('padding', 'lg');
      fixture.detectChanges();

      const card = fixture.nativeElement.querySelector('div');
      expect(card.classList).toContain('p-6');
    });
  });
});
