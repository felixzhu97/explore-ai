import { cva, type VariantProps } from 'class-variance-authority';

export const alertVariants = cva('relative flex w-full items-center gap-3 rounded-lg border px-4 py-3 text-sm', {
  variants: {
    zType: {
      default: 'bg-card text-card-foreground',
      destructive: 'bg-card text-destructive',
    },
  },
  defaultVariants: {
    zType: 'default',
  },
});

export const alertIconVariants = cva('shrink-0 self-start text-base!');

export const alertTitleVariants = cva('leading-none font-medium tracking-tight');

export const alertDescriptionVariants = cva('mt-1 text-sm/relaxed', {
  variants: {
    zType: {
      default: 'text-muted-foreground',
      destructive: 'text-destructive/90',
    },
  },
  defaultVariants: {
    zType: 'default',
  },
});

export type ZardAlertTypeVariants = NonNullable<VariantProps<typeof alertVariants>['zType']>;
