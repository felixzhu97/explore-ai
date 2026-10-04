/** True when the list exists and is not empty; narrows away `null` and `undefined`. */
export function hasItems<L extends readonly unknown[]>(
  list: L | null | undefined,
): list is L {
  return list !== null && list !== undefined && list.length > 0;
}

/** True when the string exists and is not empty; narrows away `null` and `undefined`. */
export function hasText(text: string | null | undefined): text is string {
  return text !== null && text !== undefined && text !== '';
}

/** `text` when it is a non-empty string, otherwise `fallback`. */
export function textOr<F>(text: string | null | undefined, fallback: F): string | F {
  return hasText(text) ? text : fallback;
}
