import { cva, type VariantProps } from 'class-variance-authority';

export const segmentedVariants = cva(
  'inline-flex flex-wrap items-center gap-0.5 rounded-lg border border-border bg-surface p-0.5 text-foreground',
  {
    variants: {
      zSize: {
        sm: 'min-h-8 text-xs',
        default: 'min-h-9 text-sm',
        lg: 'min-h-11 text-base',
      },
    },
    defaultVariants: {
      zSize: 'default',
    },
  },
);

export const segmentedItemVariants = cva(
  'inline-flex items-center justify-center rounded-md font-medium whitespace-nowrap text-foreground transition-all focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-1 focus-visible:outline-none disabled:pointer-events-none disabled:opacity-50',
  {
    variants: {
      zSize: {
        sm: 'px-2.5 py-1 text-xs',
        default: 'px-3 py-1.5 text-sm',
        lg: 'px-4 py-2 text-base',
      },
      isActive: {
        true: 'border border-transparent bg-surface-secondary font-medium text-foreground',
        false: 'border border-transparent text-foreground',
      },
    },
    defaultVariants: {
      zSize: 'default',
      isActive: false,
    },
  },
);

export type ZardSegmentedVariants = VariantProps<typeof segmentedVariants>;
export type ZardSegmentedItemVariants = VariantProps<typeof segmentedItemVariants>;
