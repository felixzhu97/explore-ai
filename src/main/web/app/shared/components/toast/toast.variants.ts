import { cva, type VariantProps } from 'class-variance-authority';

export const toastVariants = cva(
  'group group-[.toaster]:border-border group-[.toaster]:bg-background group-[.toaster]:text-foreground group-[.toaster]:shadow-lg',
  {
    variants: {
      variant: {
        default: 'group-[.toaster]:bg-background group-[.toaster]:text-foreground',
        destructive:
          'group-[.toaster]:border-destructive group-[.toaster]:bg-destructive group-[.toaster]:text-foreground',
      },
    },
    defaultVariants: {
      variant: 'default',
    },
  },
);

export type ZardToastVariants = NonNullable<VariantProps<typeof toastVariants>['variant']>;
