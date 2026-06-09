import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { VisionPanelComponent } from './vision-panel.component';
import { I18nService } from '../../../i18n/i18n.service';
import { ApiService } from '../services/api.service';

describe('VisionPanelComponent', () => {
  let fixture: ComponentFixture<VisionPanelComponent>;
  let component: VisionPanelComponent;

  const mockTranslations = {
    imageUploader: {
      imageLabel: 'Image',
      dropText: 'Drop image here',
      dropHint: 'Supports JPG, PNG, GIF',
      resultLabel: 'Result',
      selectImageError: 'Please select an image file',
      processingFailed: 'Processing failed',
      noImageYet: 'No image yet',
      caption: 'Caption',
      detect: 'Detect',
      ocr: 'OCR',
      analyzing: 'Analyzing...',
      startAnalyze: 'Analyze Image',
    },
  };

  const mockI18nService = {
    t: vi.fn().mockReturnValue(mockTranslations),
  };

  const createMockApiService = () => ({
    captionImage: vi.fn().mockReturnValue({ subscribe: vi.fn() }),
    detectObjects: vi.fn().mockReturnValue({ subscribe: vi.fn() }),
    ocrImage: vi.fn().mockReturnValue({ subscribe: vi.fn() }),
  });

  let mockApiService: ReturnType<typeof createMockApiService>;

  beforeEach(async () => {
    mockApiService = createMockApiService();

    await TestBed.configureTestingModule({
      imports: [VisionPanelComponent],
      providers: [
        { provide: I18nService, useValue: mockI18nService },
        { provide: ApiService, useValue: mockApiService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(VisionPanelComponent);
    component = fixture.componentInstance;
  });

  describe('component creation', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize with caption task', () => {
      expect(component.activeTask()).toBe('caption');
    });

    it('should have task options', () => {
      const options = component.taskOptions();
      expect(options.length).toBe(3);
    });

    it('should have caption, detect, and ocr options', () => {
      const options = component.taskOptions();
      expect(options.map((o) => o.value)).toEqual(['caption', 'detect', 'ocr']);
    });

    it('should have empty tab states', () => {
      const state = component.currentState();
      expect(state.image).toBeNull();
      expect(state.file).toBeNull();
      expect(state.result).toBeNull();
      expect(state.error).toBeNull();
    });

    it('should not be loading initially', () => {
      expect(component.isLoading()).toBe(false);
    });

    it('should not have zoomed image initially', () => {
      expect(component.zoomedImage()).toBeNull();
    });
  });

  describe('tab state management', () => {
    it('should return correct state for current task', () => {
      const state = component.currentState();
      expect(state).toEqual({
        image: null,
        file: null,
        result: null,
        error: null,
      });
    });

    it('should switch between tasks', () => {
      component.setActiveTask('detect');
      expect(component.activeTask()).toBe('detect');

      component.setActiveTask('ocr');
      expect(component.activeTask()).toBe('ocr');

      component.setActiveTask('caption');
      expect(component.activeTask()).toBe('caption');
    });

    it('should maintain separate states for each task', () => {
      component['tabStates'].update((states) => ({
        ...states,
        caption: { image: 'caption-image', file: null, result: null, error: null },
      }));

      component.setActiveTask('detect');
      expect(component.currentState().image).toBeNull();

      component.setActiveTask('caption');
      expect(component.currentState().image).toBe('caption-image');
    });
  });

  describe('setActiveTask', () => {
    it('should set active task', () => {
      component.setActiveTask('detect');
      expect(component.activeTask()).toBe('detect');
    });

    it('should switch from caption to ocr', () => {
      component.setActiveTask('ocr');
      expect(component.activeTask()).toBe('ocr');
    });
  });

  describe('processFile', () => {
    it('should reject non-image files', () => {
      const nonImageFile = new File(['test'], 'test.txt', { type: 'text/plain' });
      component.processFile(nonImageFile);

      const state = component.currentState();
      expect(state.error).toBeTruthy();
      expect(state.image).toBeNull();
    });
  });

  describe('onDrop', () => {
    it('should process dropped file', () => {
      const imageFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      const processFileSpy = vi.spyOn(component, 'processFile');
      const event = {
        preventDefault: vi.fn(),
        dataTransfer: { files: [imageFile] },
      } as unknown as DragEvent;

      component.onDrop(event);

      expect(processFileSpy).toHaveBeenCalledWith(imageFile);
    });

    it('should not process when no files dropped', () => {
      const processFileSpy = vi.spyOn(component, 'processFile');
      const event = {
        preventDefault: vi.fn(),
        dataTransfer: { files: [] },
      } as unknown as DragEvent;

      component.onDrop(event);

      expect(processFileSpy).not.toHaveBeenCalled();
    });
  });

  describe('onDragOver', () => {
    it('should prevent default event', () => {
      const event = { preventDefault: vi.fn() } as unknown as DragEvent;
      component.onDragOver(event);
      expect(event.preventDefault).toHaveBeenCalled();
    });
  });

  describe('clearImage', () => {
    it('should clear image state', () => {
      component['tabStates'].update((states) => ({
        ...states,
        caption: {
          image: 'test-image-url',
          file: new File([''], 'test.jpg', { type: 'image/jpeg' }),
          result: { caption: 'Some caption' },
          error: null,
        },
      }));

      const event = { stopPropagation: vi.fn() } as unknown as Event;
      component.clearImage(event);

      const state = component.currentState();
      expect(state.image).toBeNull();
      expect(state.file).toBeNull();
      expect(state.result).toBeNull();
    });

    it('should call stopPropagation', () => {
      const event = { stopPropagation: vi.fn() } as unknown as Event;
      component.clearImage(event);
      expect(event.stopPropagation).toHaveBeenCalled();
    });
  });

  describe('zoom functionality', () => {
    it('should open zoom modal with image', () => {
      component.zoomImage('https://example.com/image.jpg');
      expect(component.zoomedImage()).toBe('https://example.com/image.jpg');
    });

    it('should close zoom modal', () => {
      component.zoomedImage.set('https://example.com/image.jpg');
      component.closeZoom();
      expect(component.zoomedImage()).toBeNull();
    });
  });

  describe('submit', () => {
    it('should not submit without file', () => {
      const captionSpy = vi.spyOn(mockApiService, 'captionImage');
      component.submit();
      expect(captionSpy).not.toHaveBeenCalled();
    });

    it('should call captionImage for caption task', () => {
      component['tabStates'].update((states) => ({
        ...states,
        caption: {
          ...states.caption,
          file: new File([''], 'test.jpg', { type: 'image/jpeg' }),
        },
      }));

      mockApiService.captionImage.mockReturnValue({
        subscribe: vi.fn((callbacks) => {
          callbacks.complete?.();
        }),
      });

      component.submit();
      expect(mockApiService.captionImage).toHaveBeenCalled();
    });

    it('should call detectObjects for detect task', () => {
      component.setActiveTask('detect');
      component['tabStates'].update((states) => ({
        ...states,
        detect: {
          ...states.detect,
          file: new File([''], 'test.jpg', { type: 'image/jpeg' }),
        },
      }));

      mockApiService.detectObjects.mockReturnValue({
        subscribe: vi.fn((callbacks) => {
          callbacks.complete?.();
        }),
      });

      component.submit();
      expect(mockApiService.detectObjects).toHaveBeenCalled();
    });

    it('should call ocrImage for ocr task', () => {
      component.setActiveTask('ocr');
      component['tabStates'].update((states) => ({
        ...states,
        ocr: {
          ...states.ocr,
          file: new File([''], 'test.jpg', { type: 'image/jpeg' }),
        },
      }));

      mockApiService.ocrImage.mockReturnValue({
        subscribe: vi.fn((callbacks) => {
          callbacks.complete?.();
        }),
      });

      component.submit();
      expect(mockApiService.ocrImage).toHaveBeenCalled();
    });
  });

  describe('updateState', () => {
    it('should update current tab state', () => {
      component['updateState']({ error: 'Test error' });

      const state = component.currentState();
      expect(state.error).toBe('Test error');
    });

    it('should preserve other properties', () => {
      component['tabStates'].update((states) => ({
        ...states,
        caption: { ...states.caption, image: 'test-image' },
      }));

      component['updateState']({ error: 'Test error' });

      const state = component.currentState();
      expect(state.image).toBe('test-image');
      expect(state.error).toBe('Test error');
    });

    it('should only update current tab', () => {
      component['tabStates'].update((states) => ({
        ...states,
        caption: { ...states.caption, image: 'caption-image' },
      }));

      component.setActiveTask('detect');
      component['updateState']({ image: 'detect-image' });

      const captionState = component['tabStates']().caption;
      expect(captionState.image).toBe('caption-image');
    });
  });

  describe('onImageAreaClick', () => {
    it('should not open file dialog when image exists', () => {
      component['tabStates'].update((states) => ({
        ...states,
        caption: { ...states.caption, image: 'existing-image' },
      }));

      const createElementSpy = vi.spyOn(document, 'createElement');
      component.onImageAreaClick();
      expect(createElementSpy).not.toHaveBeenCalled();
    });
  });

  describe('onFileChange', () => {
    it('should process selected file', () => {
      const processFileSpy = vi.spyOn(component, 'processFile');
      const file = new File([''], 'test.jpg', { type: 'image/jpeg' });
      const event = {
        target: { files: [file], value: 'test' },
      } as unknown as Event;

      component.onFileChange(event);

      expect(processFileSpy).toHaveBeenCalledWith(file);
    });

    it('should not process when no file selected', () => {
      const processFileSpy = vi.spyOn(component, 'processFile');
      const event = {
        target: { files: [], value: '' },
      } as unknown as Event;

      component.onFileChange(event);

      expect(processFileSpy).not.toHaveBeenCalled();
    });
  });

  describe('template rendering', () => {
    it('should render tab header', () => {
      fixture.detectChanges();
      const tabHeader = fixture.nativeElement.querySelector('.tab-header');
      expect(tabHeader).toBeTruthy();
    });

    it('should render image area', () => {
      fixture.detectChanges();
      const imageArea = fixture.nativeElement.querySelector('.image-area');
      expect(imageArea).toBeTruthy();
    });

    it('should show drop zone when no image', () => {
      fixture.detectChanges();
      const dropZone = fixture.nativeElement.querySelector('.drop-zone');
      expect(dropZone).toBeTruthy();
    });

    it('should show empty state when no result', () => {
      fixture.detectChanges();
      const emptyState = fixture.nativeElement.querySelector('.empty-state');
      expect(emptyState).toBeTruthy();
    });

    it('should disable submit button when no file', () => {
      fixture.detectChanges();
      const submitButton = fixture.nativeElement.querySelector('.action-button') as HTMLButtonElement;
      expect(submitButton.disabled).toBe(true);
    });
  });

  describe('zoom modal', () => {
    it('should not render zoom modal initially', () => {
      fixture.detectChanges();
      const zoomModal = fixture.nativeElement.querySelector('.zoom-modal');
      expect(zoomModal).toBeFalsy();
    });

    it('should render zoom modal when image is zoomed', () => {
      component.zoomedImage.set('data:image/jpeg;base64,test');
      fixture.detectChanges();

      const zoomModal = fixture.nativeElement.querySelector('.zoom-modal');
      expect(zoomModal).toBeTruthy();
    });
  });
});
