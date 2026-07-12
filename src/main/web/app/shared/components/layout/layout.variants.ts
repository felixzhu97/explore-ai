import { cva, type VariantProps } from 'class-variance-authority';

// Layout Variants
export const layoutVariants = cva('flex min-h-0 w-full', {
  variants: {
    zDirection: {
      horizontal: 'flex-row',
      vertical: 'flex-col',
      auto: 'flex-col',
    },
  },
  defaultVariants: {
    zDirection: 'auto',
  },
});
export type LayoutVariants = NonNullable<VariantProps<typeof layoutVariants>['zDirection']>;

// Header Variants
export const headerVariants = cva('flex shrink-0 items-center border-b border-border bg-background px-4', {
  variants: {},
});

// Footer Variants
export const footerVariants = cva('flex shrink-0 items-center border-t border-border bg-background px-6', {
  variants: {},
});

// Content Variants
export const contentVariants = cva('flex min-h-dvh flex-1 flex-col overflow-auto bg-background p-6');

// Sidebar Variants
export const sidebarVariants = cva(
  'relative flex h-full shrink-0 flex-col border-r border-sidebar-border bg-sidebar p-6 text-sidebar-foreground transition-all duration-300 ease-in-out',
);

export const sidebarTriggerVariants = cva(
  'absolute -right-3 bottom-4 z-10 flex size-6 cursor-pointer items-center justify-center rounded-sm border border-sidebar-border bg-sidebar transition-colors hover:bg-sidebar-accent focus:outline-none focus-visible:ring-2 focus-visible:ring-sidebar-ring focus-visible:ring-offset-2',
);

// Sidebar Group Variants
export const sidebarGroupVariants = cva('flex flex-col gap-1');

export const sidebarGroupLabelVariants = cva(
  'flex h-8 shrink-0 items-center rounded-md px-2 text-xs font-medium text-sidebar-foreground/70 outline-hidden transition-[margin,opacity] duration-200 ease-linear focus-visible:ring-2 focus-visible:ring-sidebar-ring [&>svg]:size-4 [&>svg]:shrink-0',
);
