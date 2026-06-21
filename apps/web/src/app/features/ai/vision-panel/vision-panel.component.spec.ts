import { describe, it, expect, beforeEach, vi, beforeAll } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { VisionPanelComponent } from './vision-panel.component';
import { ApiService } from '@core/services/api.service';
import { I18nService } from '@core/i18n';
import { SegmentedControlComponent } from '@shared/components/ui/segmented-control/segmented-control.component';
import { of } from 'rxjs';

describe('VisionPanelComponent', () => {
  let fixture: ComponentFixture<VisionPanelComponent>;
  let component: VisionPanelComponent;
  let mockApiService: Partial<ApiService>;

  const mockI18n = {
    t: vi.fn().mockReturnValue({
      imageUploader: {
        caption: 'Caption',
        detect: 'Detect',
        ocr: 'OCR',
        imageLabel: 'Image',
        dropText: 'Drop image here',
        dropHint: 'or click to select',
        resultLabel: 'Result',
        noImageYet: 'No image selected',
        analyzing: 'Analyzing...',
        startAnalyze: 'Analyze',
        selectImageError: 'Please select an image file',
      },
    }),
  };

  const createMockApiService = () => {
    mockApiService = {
      captionImage: vi.fn().mockReturnValue(of({ caption: 'Test caption' })),
      detectObjects: vi.fn().mockReturnValue(of({ detections: [] })),
      ocrImage: vi.fn().mockReturnValue(of({ full_text: 'Test text' })),
    };
    return mockApiService;
  };

  const createFixture = () => {
    fixture = TestBed.createComponent(VisionPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  beforeAll(() => {
    vi.stubGlobal('FileReader', class {
      onload: ((e: ProgressEvent) => void) | null = null;
      result: string | ArrayBuffer | null = 'data:image/png;base64,mock';
      readAsDataURL = vi.fn();
      constructor() {
        // Trigger onload asynchronously to simulate real behavior
        setTimeout(() => {
          if (this.onload) {
            this.onload({ target: { result: this.result } } as ProgressEvent);
          }
        }, 0);
      }
    });
  });

  beforeEach(async () => {
    createMockApiService();
    await TestBed.configureTestingModule({
      imports: [VisionPanelComponent, SegmentedControlComponent],
      providers: [
        { provide: ApiService, useValue: mockApiService },
        { provide: I18nService, useValue: mockI18n },
      ],
    }).compileComponents();
  });

  describe('component creation', () => {
    it('should create', () => {
      createFixture();
      expect(component).toBeTruthy();
    });

    it('should have activeTask signal', () => {
      createFixture();
      expect(component.activeTask).toBeDefined();
      expect(component.activeTask()).toBe('caption');
    });

    it('should have tabStates signal', () => {
      createFixture();
      expect(component.tabStates).toBeDefined();
      const states = component.tabStates();
      expect(states.caption).toBeDefined();
      expect(states.detect).toBeDefined();
      expect(states.ocr).toBeDefined();
    });

    it('should have isLoading signal', () => {
      createFixture();
      expect(component.isLoading).toBeDefined();
      expect(component.isLoading()).toBe(false);
    });

    it('should have zoomedImage signal', () => {
      createFixture();
      expect(component.zoomedImage).toBeDefined();
      expect(component.zoomedImage()).toBeNull();
    });
  });

  describe('computed values', () => {
    it('should compute taskOptions', () => {
      createFixture();
      expect(component.taskOptions).toBeDefined();
      const options = component.taskOptions();
      expect(options.length).toBe(3);
      expect(options[0].value).toBe('caption');
      expect(options[1].value).toBe('detect');
      expect(options[2].value).toBe('ocr');
    });

    it('should return current state', () => {
      createFixture();
      expect(component.currentState()).toBeDefined();
      expect(component.currentState().image).toBeNull();
    });
  });

  describe('task switching', () => {
    it('should set active task', () => {
      createFixture();
      component.setActiveTask('detect');
      expect(component.activeTask()).toBe('detect');
    });

    it('should switch task options when task changes', () => {
      createFixture();
      component.setActiveTask('ocr');
      fixture.detectChanges();
      expect(component.taskOptions()[2].value).toBe('ocr');
    });
  });

  describe('zoom functionality', () => {
    it('should set zoomed image', () => {
      createFixture();
      component.zoomImage('zoomed-image-url');
      expect(component.zoomedImage()).toBe('zoomed-image-url');
    });

    it('should close zoom', () => {
      createFixture();
      component.zoomedImage.set('some-image');
      component.closeZoom();
      expect(component.zoomedImage()).toBeNull();
    });
  });

  describe('drag and drop handlers', () => {
    it('should prevent default on drag over', () => {
      createFixture();
      const event = {
        type: 'dragover',
        preventDefault: vi.fn(),
      } as unknown as DragEvent;

      component.onDragOver(event);

      expect(event.preventDefault).toHaveBeenCalled();
    });

    it('should clear image on clearImage', () => {
      createFixture();
      const event = new Event('click');
      const stopPropagationSpy = vi.spyOn(event, 'stopPropagation');

      component.clearImage(event);

      expect(stopPropagationSpy).toHaveBeenCalled();
    });

    it('should handle dropped file', () => {
      createFixture();
      const mockFile = new File(['test'], 'test.png', { type: 'image/png' });
      const event = {
        preventDefault: vi.fn(),
        dataTransfer: {
          files: [mockFile],
        },
      } as unknown as DragEvent;

      component.onDrop(event);

      expect(event.preventDefault).toHaveBeenCalled();
    });

    it('should not process drop without file', () => {
      createFixture();
      const event = {
        preventDefault: vi.fn(),
        dataTransfer: {
          files: [],
        },
      } as unknown as DragEvent;

      component.onDrop(event);

      expect(event.preventDefault).toHaveBeenCalled();
    });
  });

  describe('file handling', () => {
    it('should handle onFileChange with valid file', async () => {
      createFixture();
      const mockFile = new File(['test'], 'test.png', { type: 'image/png' });
      const event = {
        target: {
          files: [mockFile],
          value: 'test',
        },
      } as unknown as Event;

      component.onFileChange(event);
      await new Promise((resolve) => setTimeout(resolve, 10));

      expect(component.tabStates().caption.file).toBe(mockFile);
    });

    it('should clear input value after file selection', async () => {
      createFixture();
      const mockFile = new File(['test'], 'test.png', { type: 'image/png' });
      const event = {
        target: {
          files: [mockFile],
          value: 'test',
        },
      } as unknown as Event;

      component.onFileChange(event);
      await new Promise((resolve) => setTimeout(resolve, 10));

      expect((event.target as any).value).toBe('');
    });

    it('should not process file change without file', () => {
      createFixture();
      const event = {
        target: {
          files: [],
        },
      } as unknown as Event;

      component.onFileChange(event);

      expect(component.tabStates().caption.file).toBeNull();
    });

    it('should reject non-image files with error', () => {
      createFixture();
      const mockFile = new File(['test'], 'test.txt', { type: 'text/plain' });
      const event = {
        target: {
          files: [mockFile],
        },
      } as unknown as Event;

      component.onFileChange(event);

      expect(component.tabStates().caption.error).toBe('Please select an image file');
    });
  });

  describe('submit functionality', () => {
    it('should not submit without file', () => {
      createFixture();
      component.submit();
      expect(mockApiService.captionImage).not.toHaveBeenCalled();
    });

    it('should submit caption task with file', async () => {
      createFixture();
      const mockFile = new File(['test'], 'test.png', { type: 'image/png' });
      const event = {
        target: {
          files: [mockFile],
        },
      } as unknown as Event;
      component.onFileChange(event);
      await new Promise((resolve) => setTimeout(resolve, 10));

      component.submit();

      expect(mockApiService.captionImage).toHaveBeenCalledWith(mockFile);
    });

    it('should submit detect task with file', async () => {
      createFixture();
      component.setActiveTask('detect');
      const mockFile = new File(['test'], 'test.png', { type: 'image/png' });
      const event = {
        target: {
          files: [mockFile],
        },
      } as unknown as Event;
      component.onFileChange(event);
      await new Promise((resolve) => setTimeout(resolve, 10));

      component.submit();

      expect(mockApiService.detectObjects).toHaveBeenCalledWith(mockFile);
    });

    it('should submit ocr task with file', async () => {
      createFixture();
      component.setActiveTask('ocr');
      const mockFile = new File(['test'], 'test.png', { type: 'image/png' });
      const event = {
        target: {
          files: [mockFile],
        },
      } as unknown as Event;
      component.onFileChange(event);
      await new Promise((resolve) => setTimeout(resolve, 10));

      component.submit();

      expect(mockApiService.ocrImage).toHaveBeenCalledWith(mockFile);
    });

    it('should set isLoading to true during submit', async () => {
      createFixture();
      const mockFile = new File(['test'], 'test.png', { type: 'image/png' });
      const event = {
        target: {
          files: [mockFile],
        },
      } as unknown as Event;
      component.onFileChange(event);
      await new Promise((resolve) => setTimeout(resolve, 10));

      // Mock API to not complete immediately
      (mockApiService.captionImage as any).mockReturnValue({
        subscribe: vi.fn(),
      });

      component.submit();

      expect(component.isLoading()).toBe(true);
    });

    it('should handle API error', async () => {
      createFixture();
      (mockApiService.captionImage as any).mockImplementation(() => {
        return {
          subscribe: ({ error }: any) => {
            // Call error callback synchronously for test
            error(new Error('API Error'));
          },
        };
      });
      const mockFile = new File(['test'], 'test.png', { type: 'image/png' });
      const event = {
        target: {
          files: [mockFile],
        },
      } as unknown as Event;
      component.onFileChange(event);
      await new Promise((resolve) => setTimeout(resolve, 10));

      component.submit();

      expect(component.tabStates().caption.error).toBe('API Error');
    });

    it('should preserve state across tab switches', async () => {
      createFixture();
      const mockFile = new File(['test'], 'test.png', { type: 'image/png' });
      const event = {
        target: {
          files: [mockFile],
        },
      } as unknown as Event;
      component.onFileChange(event);
      await new Promise((resolve) => setTimeout(resolve, 10));

      component.setActiveTask('detect');
      component.onFileChange(event);
      await new Promise((resolve) => setTimeout(resolve, 10));

      component.setActiveTask('caption');
      expect(component.currentState().file).toBe(mockFile);
    });
  });

  describe('template rendering', () => {
    it('should render vision panel container', () => {
      createFixture();
      const panel = fixture.nativeElement.querySelector('.vision-panel');
      expect(panel).toBeTruthy();
    });

    it('should render segmented control', () => {
      createFixture();
      const control = fixture.nativeElement.querySelector('app-segmented-control');
      expect(control).toBeTruthy();
    });

    it('should render image area', () => {
      createFixture();
      const imageArea = fixture.nativeElement.querySelector('.image-area');
      expect(imageArea).toBeTruthy();
    });

    it('should render empty state when no image', () => {
      createFixture();
      const emptyState = fixture.nativeElement.querySelector('.empty-state');
      expect(emptyState).toBeTruthy();
    });

    it('should not render zoom modal initially', () => {
      createFixture();
      const zoomModal = fixture.nativeElement.querySelector('.zoom-modal');
      expect(zoomModal).toBeFalsy();
    });
  });
});
