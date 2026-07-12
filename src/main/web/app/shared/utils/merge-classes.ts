import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function mergeClasses(...classValues: ClassValue[]): string {
  return twMerge(
    // eslint-disable-next-line tailwindcss/no-custom-classname
    clsx(classValues),
  );
}

export function noopFn(): void {
  // Intentionally empty — used as default CVA / event handler placeholder
}
