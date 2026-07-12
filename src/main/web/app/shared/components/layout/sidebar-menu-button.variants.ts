import { cva, type VariantProps } from 'class-variance-authority';

import { buttonVariants } from '@/shared/components/button/button.variants';
import { mergeClasses } from '@/shared/utils/merge-classes';

export const sidebarMenuButtonVariants = cva(
  mergeClasses(
    buttonVariants({ zType: 'ghost' }),
    'border-transparent hover:border-transparent focus-visible:border-transparent data-[active=true]:border-transparent',
    'justify-start gap-2 rounded-lg px-2 py-1.5 text-left text-[13px] font-medium text-sidebar-foreground/80 no-underline',
    'hover:bg-sidebar-accent hover:text-sidebar-accent-foreground',
    'data-[active=true]:bg-sidebar-accent data-[active=true]:font-medium data-[active=true]:text-sidebar-accent-foreground',
    'outline-none focus-visible:ring-2 focus-visible:ring-sidebar-ring focus-visible:ring-offset-0',
    '[&>svg]:size-4 [&>svg]:shrink-0',
    '[&>span:last-child]:min-w-0 [&>span:last-child]:truncate',
  ),
  {
    variants: {
      zIconOnly: {
        true: 'size-8 justify-center px-0',
        false: '',
      },
      zFull: {
        true: 'w-full',
        false: '',
      },
    },
    defaultVariants: {
      zIconOnly: false,
      zFull: true,
    },
  },
);

type SidebarMenuButtonVariants = VariantProps<typeof sidebarMenuButtonVariants>;
export type ZardSidebarMenuButtonIconOnlyVariants = NonNullable<
  SidebarMenuButtonVariants['zIconOnly']
>;
