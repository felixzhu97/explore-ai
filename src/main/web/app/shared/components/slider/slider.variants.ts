import { cva, type VariantProps } from 'class-variance-authority';

export const sliderVariants = cva(
  'relative flex w-full touch-none items-center select-none data-[orientation=vertical]:h-full data-[orientation=vertical]:min-h-44 data-[orientation=vertical]:w-auto data-[orientation=vertical]:flex-col',
  {
    variants: {
      orientation: {
        horizontal: 'items-center',
        vertical: 'h-full min-h-44 w-auto flex-col',
      },
      disabled: {
        true: 'pointer-events-none opacity-50',
        false: '',
      },
    },
    defaultVariants: {
      orientation: 'horizontal',
      disabled: false,
    },
  },
);

export type SliderVariants = VariantProps<typeof sliderVariants>;

export const sliderTrackVariants = cva(
  'relative flex grow overflow-hidden rounded-full bg-muted data-[orientation=horizontal]:h-1.5 data-[orientation=horizontal]:w-full data-[orientation=vertical]:h-full data-[orientation=vertical]:w-1.5',
  {
    variants: {
      zOrientation: {
        horizontal: 'h-1.5 w-full',
        vertical: 'h-full min-h-44 w-1.5',
      },
    },
    defaultVariants: {
      zOrientation: 'horizontal',
    },
  },
);

export type SliderTrackVariants = VariantProps<typeof sliderTrackVariants>;

export const sliderRangeVariants = cva(
  'absolute bg-primary data-[orientation=horizontal]:h-full data-[orientation=vertical]:w-full',
  {
    variants: {
      zOrientation: {
        horizontal: 'h-full',
        vertical: 'w-full',
      },
    },
    defaultVariants: {
      zOrientation: 'horizontal',
    },
  },
);

export type SliderRangeVariants = VariantProps<typeof sliderRangeVariants>;

export const sliderThumbVariants = cva(
  'block size-4 shrink-0 rounded-full border border-primary bg-background shadow-sm ring-ring/50 transition-[color,box-shadow] focus-visible:ring-4 focus-visible:outline-hidden disabled:pointer-events-none disabled:opacity-50',
  {
    variants: {
      disabled: {
        true: '',
        false: 'hover:ring-4',
      },
    },
  },
);

export type SliderThumbVariants = VariantProps<typeof sliderThumbVariants>;

export const sliderOrientationVariants = cva('absolute', {
  variants: {
    zOrientation: {
      horizontal: 'translate-x-[-50%]',
      vertical: 'translate-y-[50%]',
    },
  },
  defaultVariants: {
    zOrientation: 'horizontal',
  },
});

export type SliderOrientationVariants = VariantProps<typeof sliderOrientationVariants>;
