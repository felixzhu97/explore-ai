import { requiredError, type SchemaPath, validate } from '@angular/forms/signals';

/** Like `required`, but whitespace-only text also counts as empty. */
export function requiredText(
  path: SchemaPath<string>,
  when: () => boolean = () => true,
): void {
  validate(path, ({ value }) => (!when() || value().trim() !== '' ? null : requiredError()));
}
