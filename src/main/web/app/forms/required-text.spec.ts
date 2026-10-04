import { describe, expect, it } from 'vitest';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { form } from '@angular/forms/signals';
import { requiredText } from './required-text';

describe('requiredText', () => {
  function setup(when?: () => boolean) {
    const model = signal({ name: '' });
    const fields = TestBed.runInInjectionContext(() => form(model, (path) => {
      requiredText(path.name, when);
    }));
    return { model, fields };
  }

  it('should be invalid when the text is empty', () => {
    const { fields } = setup();
    expect(fields.name().invalid()).toBe(true);
  });

  it('should be invalid when the text is only whitespace', () => {
    const { model, fields } = setup();
    model.set({ name: '   ' });
    expect(fields.name().invalid()).toBe(true);
  });

  it('should be valid when the text has content', () => {
    const { model, fields } = setup();
    model.set({ name: ' Daily brief ' });
    expect(fields.name().valid()).toBe(true);
  });

  it('should skip the check when the condition is false', () => {
    const { fields } = setup(() => false);
    expect(fields.name().valid()).toBe(true);
  });
});
