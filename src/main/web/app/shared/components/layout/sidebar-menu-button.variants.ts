import { cva, type VariantProps } from 'class-variance-authority';

import { buttonVariants } from '../button/button.variants';
import { mergeClasses } from '../../utils/merge-classes';

export const sidebarMenuButtonVariants = cva(
  mergeClasses(
    buttonVariants({ zType: 'ghost' }),
    'border-transparent hover:border-transparent focus-visible:border-transparent data-[active=true]:border-transparent',
    'justify-start gap-1.5 rounded-lg px-2 py-1.5 text-left text-sm font-normal text-[#0D0D0D] no-underline',
    'hover:bg-sidebar-accent hover:text-[#0D0D0D]',
    'data-[active=true]:bg-sidebar-accent data-[active=true]:font-normal data-[active=true]:text-[#0D0D0D]',
    'outline-none focus-visible:ring-2 focus-visible:ring-sidebar-ring focus-visible:ring-offset-0',
    '[&>svg]:size-3.5 [&>svg]:shrink-0',
    '[&>span:last-child]:min-w-0 [&>span:last-child]:truncate',
  ),
  {
    variants: {
      zIconOnly: {
        true: 'size-7 justify-center px-0',
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
