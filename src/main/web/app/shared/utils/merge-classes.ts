import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function mergeClasses(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}

export function noopFn(): void {
  // Intentionally empty — used as default CVA / event handler placeholder
}
