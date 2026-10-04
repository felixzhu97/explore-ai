export function clamp(value: number, range: [number, number]): number {
  const [min, max] = range;
  return Math.min(Math.max(value, min), max);
}

export function convertValueToPercentage(
  value: number,
  min: number,
  max: number,
): number {
  if (max === min) {
    return 0;
  }

  return ((value - min) / (max - min)) * 100;
}

export function roundToStep(value: number, min: number, step: number): number {
  if (step === 0) {
    return value;
  }

  const steps = Math.round((value - min) / step);
  return min + steps * step;
}
