import { afterEach, describe, expect, it, vi } from 'vitest';
import { base64ToBlob, downloadBase64Image, downloadBlob } from './download';

describe('download utils', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should_create_blob_from_base64', () => {
    const blob = base64ToBlob(btoa('hello'), 'text/plain');
    expect(blob.type).toBe('text/plain');
    expect(blob.size).toBe(5);
  });

  it('should_download_blob_via_anchor_click', () => {
    const click = vi.fn();
    const revoke = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    const create = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:mock');
    const append = vi.spyOn(document.body, 'appendChild').mockImplementation(node => node);
    const remove = vi.spyOn(document.body, 'removeChild').mockImplementation(node => node);
    vi.spyOn(document, 'createElement').mockReturnValue({
      href: '',
      download: '',
      click,
    } as unknown as HTMLAnchorElement);

    downloadBlob(new Blob(['x']), 'file.txt');

    expect(create).toHaveBeenCalled();
    expect(append).toHaveBeenCalled();
    expect(click).toHaveBeenCalled();
    expect(remove).toHaveBeenCalled();
    expect(revoke).toHaveBeenCalledWith('blob:mock');
  });

  it('should_detect_jpeg_mime_when_downloading_base64_image', () => {
    const downloadSpy = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:img');
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    vi.spyOn(document.body, 'appendChild').mockImplementation(node => node);
    vi.spyOn(document.body, 'removeChild').mockImplementation(node => node);
    vi.spyOn(document, 'createElement').mockReturnValue({
      href: '',
      download: '',
      click: vi.fn(),
    } as unknown as HTMLAnchorElement);

    downloadBase64Image('/9j/' + btoa('jpeg-bytes'), 'photo.jpg');

    expect(downloadSpy).toHaveBeenCalled();
  });
});
