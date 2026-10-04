import { cva, type VariantProps } from 'class-variance-authority';

export const dialogVariants = cva(
  'fixed top-[50%] left-[50%] z-50 grid w-full max-w-[calc(100%-2rem)] translate-[-50%] gap-4 rounded-lg border bg-background p-6 shadow-lg',
);
export type ZardDialogVariants = VariantProps<typeof dialogVariants>;
