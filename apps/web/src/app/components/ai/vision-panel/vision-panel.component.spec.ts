import { describe, it, expect, beforeEach, vi, beforeAll } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { VisionPanelComponent } from './vision-panel.component';
import { ApiService } from '../services/api.service';
import { I18nService } from '../../../i18n';
import { SegmentedControlComponent } from '../../segmented-control/segmented-control.component';
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
    vi.stubGlobal('FileReader', vi.fn().mockImplementation(() => ({
      readAsDataURL: vi.fn(),
      onload: null,
      result: 'data:image/png;base64,mock',
    })));
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

  describe('state management', () => {
    it('should update state for current task', () => {
      createFixture();
      component.updateState({ image: 'test-image-data', result: { caption: 'Test' } });
      
      const state = component.currentState();
      expect(state.image).toBe('test-image-data');
      expect(state.result).toEqual({ caption: 'Test' });
    });

    it('should clear error when updating state', () => {
      createFixture();
      component.updateState({ error: null });
      expect(component.currentState().error).toBeNull();
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
      component.updateState({ image: 'some-image' });
      const event = new Event('click');
      const stopPropagationSpy = vi.spyOn(event, 'stopPropagation');
      
      component.clearImage(event);
      
      expect(stopPropagationSpy).toHaveBeenCalled();
      expect(component.currentState().image).toBeNull();
    });
  });

  describe('submit functionality', () => {
    it('should not submit without file', () => {
      createFixture();
      component.submit();
      expect(mockApiService.captionImage).not.toHaveBeenCalled();
    });

    it('should call captionImage for caption task', () => {
      createFixture();
      const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      component.updateState({ file: mockFile });
      
      component.submit();
      
      expect(mockApiService.captionImage).toHaveBeenCalledWith(mockFile);
    });

    it('should call detectObjects for detect task', () => {
      createFixture();
      const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      component.setActiveTask('detect');
      component.updateState({ file: mockFile });
      
      component.submit();
      
      expect(mockApiService.detectObjects).toHaveBeenCalledWith(mockFile);
    });

    it('should call ocrImage for ocr task', () => {
      createFixture();
      const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      component.setActiveTask('ocr');
      component.updateState({ file: mockFile });
      
      component.submit();
      
      expect(mockApiService.ocrImage).toHaveBeenCalledWith(mockFile);
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
