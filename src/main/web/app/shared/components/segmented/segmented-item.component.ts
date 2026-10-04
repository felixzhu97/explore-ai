import { booleanAttribute, ChangeDetectionStrategy, Component, input, ViewEncapsulation } from '@angular/core';

@Component({
  selector: 'z-segmented-item',
  template: `
    <ng-content />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
})
export class ZardSegmentedItemComponent {
  readonly value = input.required<string>();
  readonly label = input.required<string>();
  readonly zDisabled = input(false, { transform: booleanAttribute });
}
