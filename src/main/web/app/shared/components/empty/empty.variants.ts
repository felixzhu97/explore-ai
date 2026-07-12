import { cva } from 'class-variance-authority';

export const emptyVariants = cva(
  'flex w-full min-w-0 flex-col items-center justify-center gap-6 rounded-lg border-dashed p-6 text-center md:p-12',
  {
    variants: {},
  },
);

export const emptyHeaderVariants = cva(
  'mx-auto flex w-full max-w-(--container-sm) flex-col items-center gap-2 text-center',
  {
    variants: {},
  },
);

export const emptyImageVariants = cva(
  'mb-2 flex shrink-0 items-center justify-center bg-transparent [&_svg]:pointer-events-none [&_svg]:shrink-0',
  {
    variants: {},
  },
);

export const emptyIconVariants = cva(
  `mb-2 flex size-10 shrink-0 items-center justify-center rounded-lg bg-muted text-foreground [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-6`,
  {
    variants: {},
  },
);

export const emptyTitleVariants = cva('text-lg font-medium tracking-tight', {
  variants: {},
});

export const emptyDescriptionVariants = cva(
  'text-sm/relaxed text-muted-foreground [&>a]:underline [&>a]:underline-offset-4 [&>a:hover]:text-primary',
  {
    variants: {},
  },
);

export const emptyActionsVariants = cva(
  'flex w-full max-w-(--container-sm) min-w-0 items-center justify-center gap-2 text-sm text-balance',
  {
    variants: {},
  },
);
