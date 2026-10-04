import { cva, type VariantProps } from 'class-variance-authority';

export const badgeVariants = cva(
  'inline-flex w-fit shrink-0 items-center justify-center gap-1 overflow-hidden border px-2 py-0.5 text-xs font-medium whitespace-nowrap transition-[color,box-shadow] focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 aria-invalid:border-destructive aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 [&>svg]:pointer-events-none [&>svg]:size-3',
  {
    variants: {
      zType: {
        default: 'h-5 border-transparent bg-primary text-primary-foreground [a&]:hover:bg-primary-hover',
        secondary: 'h-5 border-transparent bg-secondary text-secondary-foreground [a&]:hover:bg-secondary/90',
        destructive:
          'h-5 border-transparent bg-destructive text-white focus-visible:ring-destructive/20 dark:bg-destructive/60 dark:focus-visible:ring-destructive/40 [a&]:hover:bg-destructive/90',
        outline: 'text-foreground [a&]:hover:bg-accent [a&]:hover:text-accent-foreground',
      },
      zShape: {
        default: 'rounded-md',
        square: 'rounded-none',
        pill: 'rounded-full',
      },
    },
    defaultVariants: {
      zType: 'default',
      zShape: 'default',
    },
  },
);

export type ZardBadgeTypeVariants = NonNullable<VariantProps<typeof badgeVariants>['zType']>;
export type ZardBadgeShapeVariants = NonNullable<VariantProps<typeof badgeVariants>['zShape']>;
