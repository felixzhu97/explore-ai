import { cva, type VariantProps } from 'class-variance-authority';

import { mergeClasses } from '../../utils/merge-classes';

export const buttonVariants = cva(
  mergeClasses(
    'focus-visible:border-ring focus-visible:ring-ring/50 aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40',
    'aria-invalid:border-destructive dark:aria-invalid:border-destructive/50 rounded-lg border border-transparent bg-clip-padding',
    'text-sm font-medium focus-visible:ring-3 aria-invalid:ring-3 [&_svg:not([class*=\'size-\'])]:size-4 inline-flex items-center',
    'justify-center whitespace-nowrap transition-all disabled:pointer-events-none disabled:border-border disabled:bg-surface disabled:text-muted-foreground disabled:opacity-100 [&_svg]:pointer-events-none',
    'shrink-0 [&_svg]:shrink-0 outline-none group/button select-none [&_ng-icon]:flex [&_ng-icon]:items-center',
  ),
  {
    variants: {
      zType: {
        default:
          'border-border bg-surface text-foreground hover:border-foreground/25 hover:bg-surface',
        destructive:
          'border-transparent bg-destructive/10 text-destructive hover:bg-destructive/20 focus-visible:border-destructive/40 focus-visible:ring-destructive/20 dark:bg-destructive/20 dark:hover:bg-destructive/30 dark:focus-visible:ring-destructive/40',
        outline:
          'border-border bg-surface text-foreground hover:border-foreground/25 hover:bg-surface aria-expanded:border-foreground/25 aria-expanded:bg-surface',
        secondary:
          'border-border bg-surface text-foreground hover:border-foreground/25 hover:bg-surface aria-expanded:border-foreground/25 aria-expanded:bg-surface',
        ghost:
          'border border-transparent bg-transparent text-foreground hover:border-border hover:bg-surface aria-expanded:border-border aria-expanded:bg-surface',
        link: 'text-foreground underline-offset-4 hover:underline',
      },
      zSize: {
        default: 'h-8 gap-1.5 px-2.5 has-data-[icon=inline-end]:pr-2 has-data-[icon=inline-start]:pl-2',
        xs: 'h-6 gap-1 rounded-[min(var(--radius-md),10px)] px-2 text-xs in-data-[slot=button-group]:rounded-lg has-data-[icon=inline-end]:pr-1.5 has-data-[icon=inline-start]:pl-1.5 [&_svg:not([class*=\'size-\'])]:size-3',
        sm: 'h-7 gap-1 rounded-[min(var(--radius-md),12px)] px-2.5 text-[0.8rem] in-data-[slot=button-group]:rounded-lg has-data-[icon=inline-end]:pr-1.5 has-data-[icon=inline-start]:pl-1.5 [&_svg:not([class*=\'size-\'])]:size-3.5',
        lg: 'h-9 gap-1.5 px-2.5 has-data-[icon=inline-end]:pr-3 has-data-[icon=inline-start]:pl-3',
        icon: 'size-8',
        'icon-xs':
          'size-6 rounded-[min(var(--radius-md),10px)] in-data-[slot=button-group]:rounded-lg [&_svg:not([class*=\'size-\'])]:size-3',
        'icon-sm': 'size-7 rounded-[min(var(--radius-md),12px)] in-data-[slot=button-group]:rounded-lg',
        'icon-lg': 'size-9',
      },
      zShape: {
        default: 'rounded-md',
        circle: 'rounded-full',
        square: 'rounded-none',
      },
      zFull: {
        true: 'w-full',
      },
      zLoading: {
        true: 'pointer-events-none opacity-50',
      },
      zDisabled: {
        true: 'pointer-events-none border-border bg-surface text-muted-foreground opacity-100',
      },
    },
    defaultVariants: {
      zType: 'default',
      zSize: 'default',
      zShape: 'default',
    },
  },
);
export type ZardButtonShapeVariants = NonNullable<VariantProps<typeof buttonVariants>['zShape']>;
export type ZardButtonSizeVariants = NonNullable<VariantProps<typeof buttonVariants>['zSize']>;
export type ZardButtonTypeVariants = NonNullable<VariantProps<typeof buttonVariants>['zType']>;
