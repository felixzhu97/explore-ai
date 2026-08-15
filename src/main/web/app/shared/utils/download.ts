export function downloadBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

export function base64ToBlob(base64: string, mimeType = 'image/png'): Blob {
  const byteCharacters = atob(base64);
  const byteNumbers = new Array(byteCharacters.length);
  for (let i = 0; i < byteCharacters.length; i++) {
    byteNumbers[i] = byteCharacters.charCodeAt(i);
  }
  const byteArray = new Uint8Array(byteNumbers);
  return new Blob([byteArray], { type: mimeType });
}

export function downloadBase64Image(base64: string, filename = 'image.png'): void {
  const mimeType = base64.startsWith('/9j/') ? 'image/jpeg' : 'image/png';
  downloadBlob(base64ToBlob(base64, mimeType), filename);
}

/** Download an SVG markup string as a `.svg` file. */
export function downloadSvgMarkup(svgMarkup: string, filename = 'diagram.svg'): void {
  const trimmed = svgMarkup.trim();
  if (!trimmed) {
    return;
  }
  downloadBlob(new Blob([trimmed], { type: 'image/svg+xml;charset=utf-8' }), filename);
}
