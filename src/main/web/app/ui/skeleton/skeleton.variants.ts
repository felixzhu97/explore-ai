import { cva, type VariantProps } from 'class-variance-authority';

export const skeletonVariants = cva('animate-pulse rounded-md bg-accent');
export type ZardSkeletonVariants = VariantProps<typeof skeletonVariants>;
