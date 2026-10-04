import { cva, type VariantProps } from 'class-variance-authority';

export const switchVariants = cva(
  'peer inline-flex shrink-0 cursor-pointer items-center rounded-full border-2 border-transparent transition-colors focus-visible:ring-2 focus-visible:ring-primary/30 focus-visible:ring-offset-2 focus-visible:ring-offset-background focus-visible:outline-none disabled:cursor-not-allowed disabled:opacity-50',
  {
    variants: {
      zType: {
        default: '',
        destructive: 'data-[state=checked]:bg-destructive',
      },
      zSize: {
        default: 'h-6 w-11',
        sm: 'h-5 w-9',
        lg: 'h-7 w-13',
      },
    },
    defaultVariants: {
      zType: 'default',
      zSize: 'default',
    },
  },
);

export type ZardSwitchSizeVariants = NonNullable<VariantProps<typeof switchVariants>['zSize']>;
export type ZardSwitchTypeVariants = NonNullable<VariantProps<typeof switchVariants>['zType']>;
