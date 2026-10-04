import { cva } from 'class-variance-authority';

export const menuLabelVariants = cva(
  'relative flex items-center px-2 py-1.5 text-sm font-medium text-muted-foreground',
  {
    variants: {
      inset: {
        true: 'pl-8',
        false: '',
      },
    },
    defaultVariants: {
      inset: false,
    },
  },
);
