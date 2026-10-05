import { Component, effect, input, linkedSignal, model, untracked } from '@angular/core';
import { form, FormField, type FormValueControl } from '@angular/forms/signals';
import { type Instant, LocalDate } from '@js-joda/core';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { fromNativeDate, toNativeDate } from './native-date';

/** ng-zorro date-time picker bound to an `Instant`; its `Date` stays in this file. */
@Component({
  selector: 'app-instant-picker',
  imports: [FormField, NzDatePickerModule],
  template: `
    <nz-date-picker
      class="w-full"
      nzSize="large"
      nzShowTime
      nzFormat="yyyy-MM-dd HH:mm:ss"
      [nzDropdownClassName]="dropdownClass()"
      [nzPlaceHolder]="placeholder()"
      [nzDisabledDate]="disabledDate"
      [formField]="nativeField"
    />
  `,
  host: { class: 'block w-full' },
})
export class InstantPickerComponent implements FormValueControl<Instant | null> {
  readonly value = model<Instant | null>(null);
  readonly placeholder = input('');
  readonly dropdownClass = input('');
  readonly disablePast = input(false);

  protected readonly disabledDate = (current: Date): boolean => {
    const day = LocalDate.ofInstant(fromNativeDate(current));
    return this.disablePast() && day.isBefore(LocalDate.now());
  };

  readonly #native = linkedSignal<Date | null>(() => {
    const instant = this.value();
    return instant === null ? null : toNativeDate(instant);
  });

  protected readonly nativeField = form(this.#native);

  constructor() {
    effect(() => {
      const native = this.#native();
      const picked = native === null ? null : fromNativeDate(native);
      untracked(() => {
        const current = this.value();
        const unchanged = picked === null
          ? current === null
          : current !== null && picked.equals(current);
        if (!unchanged) {
          this.value.set(picked);
        }
      });
    });
  }
}
