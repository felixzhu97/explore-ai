import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { VisionPanelComponent } from './vision-panel.component';
import { I18nService } from '../../../i18n/i18n.service';
import { ApiService, Detection } from '../services/api.service';
import { translations } from '../../../i18n/i18n.model';

describe('VisionPanelComponent', () => {
  let fixture: ComponentFixture<VisionPanelComponent>;
  let component: VisionPanelComponent;
  let httpMock: HttpTestingController;

  const mockI18nService = {
    t: jasmine.createSpy('t').and.returnValue(translations.en.imageUploader),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VisionPanelComponent, HttpClientTestingModule],
      providers: [
        ApiService,
        { provide: I18nService, useValue: mockI18nService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(VisionPanelComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with caption task', () => {
    expect(component.activeTask()).toBe('caption');
  });

  it('should have task options from i18n', () => {
    const options = component.taskOptions();
    expect(options.length).toBe(3);
    expect(options[0].value).toBe('caption');
    expect(options[1].value).toBe('detect');
    expect(options[2].value).toBe('ocr');
  });

  it('should switch tabs correctly', () => {
    component.setActiveTask('detect');
    expect(component.activeTask()).toBe('detect');
    
    component.setActiveTask('ocr');
    expect(component.activeTask()).toBe('ocr');
    
    component.setActiveTask('caption');
    expect(component.activeTask()).toBe('caption');
  });

  describe('file handling', () => {
    it('should update state on file selection', () => {
      const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      const stateBefore = component.currentState();
      
      component.processFile(mockFile);
      fixture.detectChanges();
      
      const stateAfter = component.currentState();
      expect(stateAfter.image).toBeTruthy();
      expect(stateAfter.file).toBe(mockFile);
    });

    it('should reject non-image files', () => {
      const mockFile = new File(['test'], 'test.txt', { type: 'text/plain' });
      component.processFile(mockFile);
      fixture.detectChanges();
      
      const state = component.currentState();
      expect(state.image).toBeNull();
      expect(state.error).toBeTruthy();
    });

    it('should clear error when processing valid file', () => {
      component['tabStates'].update(states => ({
        ...states,
        caption: { ...states.caption, error: 'Previous error' },
      }));
      
      const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      component.processFile(mockFile);
      fixture.detectChanges();
      
      const state = component.currentState();
      expect(state.error).toBeNull();
    });
  });

  describe('image state management', () => {
    it('should have empty initial state', () => {
      const state = component.currentState();
      expect(state.image).toBeNull();
      expect(state.file).toBeNull();
      expect(state.result).toBeNull();
      expect(state.error).toBeNull();
    });

    it('should maintain separate states for each task', () => {
      component.setActiveTask('caption');
      component['tabStates'].update(states => ({
        ...states,
        caption: { image: 'caption_image', file: null, result: null, error: null },
      }));
      
      component.setActiveTask('detect');
      component['tabStates'].update(states => ({
        ...states,
        detect: { image: 'detect_image', file: null, result: null, error: null },
      }));
      
      expect(component.currentState().image).toBe('detect_image');
      
      component.setActiveTask('caption');
      expect(component.currentState().image).toBe('caption_image');
    });

    it('should clear image state', () => {
      component['tabStates'].update(states => ({
        ...states,
        caption: { 
          image: 'test_image', 
          file: new File([''], 'test.jpg', { type: 'image/jpeg' }), 
          result: { caption: 'Test' },
          error: null,
        },
      }));
      
      component.clearImage(new Event('click'));
      fixture.detectChanges();
      
      const state = component.currentState();
      expect(state.image).toBeNull();
      expect(state.file).toBeNull();
      expect(state.result).toBeNull();
    });
  });

  describe('API calls', () => {
    it('should call caption API with correct parameters', () => {
      component.setActiveTask('caption');
      const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      component.processFile(mockFile);
      
      component.submit();
      
      const req = httpMock.expectOne('/api/vision/caption');
      expect(req.request.method).toBe('POST');
      req.flush({ caption: 'A beautiful sunset' });
    });

    it('should call detect API with correct parameters', () => {
      component.setActiveTask('detect');
      const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      component.processFile(mockFile);
      
      component.submit();
      
      const req = httpMock.expectOne('/api/vision/detect');
      expect(req.request.method).toBe('POST');
      
      const detections: Detection[] = [
        { class_name: 'person', confidence: 0.95, bbox: [100, 100, 200, 300] },
      ];
      req.flush({ detections });
    });

    it('should call OCR API with correct parameters', () => {
      component.setActiveTask('ocr');
      const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      component.processFile(mockFile);
      
      component.submit();
      
      const req = httpMock.expectOne('/api/vision/ocr');
      expect(req.request.method).toBe('POST');
      req.flush({ full_text: 'Sample text content' });
    });

    it('should handle API errors', () => {
      component.setActiveTask('caption');
      const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      component.processFile(mockFile);
      
      component.submit();
      
      const req = httpMock.expectOne('/api/vision/caption');
      req.error(new ProgressEvent('error'), { status: 500, statusText: 'Server Error' });
      
      fixture.detectChanges();
      const state = component.currentState();
      expect(state.error).toBeTruthy();
    });

    it('should set loading state during API call', () => {
      component.setActiveTask('caption');
      const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      component.processFile(mockFile);
      
      component.submit();
      expect(component.isLoading()).toBe(true);
      
      const req = httpMock.expectOne('/api/vision/caption');
      req.flush({ caption: 'Test' });
      
      expect(component.isLoading()).toBe(false);
    });
  });

  describe('results display', () => {
    it('should display caption result', () => {
      component.setActiveTask('caption');
      component['tabStates'].update(states => ({
        ...states,
        caption: { 
          image: 'test', 
          file: null, 
          result: { caption: 'A beautiful sunset over the ocean' },
          error: null,
        },
      }));
      fixture.detectChanges();
      
      const resultText = fixture.nativeElement.querySelector('.result-text');
      expect(resultText).toBeTruthy();
      expect(resultText.textContent).toContain('A beautiful sunset');
    });

    it('should display detection results with confidence', () => {
      component.setActiveTask('detect');
      const detections: Detection[] = [
        { class_name: 'person', confidence: 0.95, bbox: [100, 100, 200, 300] },
        { class_name: 'car', confidence: 0.85, bbox: [50, 50, 150, 150] },
      ];
      component['tabStates'].update(states => ({
        ...states,
        detect: { 
          image: 'test', 
          file: null, 
          result: { detections },
          error: null,
        },
      }));
      fixture.detectChanges();
      
      const detectionItems = fixture.nativeElement.querySelectorAll('.detection-item');
      expect(detectionItems.length).toBe(2);
      
      const firstItem = detectionItems[0];
      expect(firstItem.textContent).toContain('person');
      expect(firstItem.textContent).toContain('95%');
    });

    it('should display OCR text', () => {
      component.setActiveTask('ocr');
      component['tabStates'].update(states => ({
        ...states,
        ocr: { 
          image: 'test', 
          file: null, 
          result: { full_text: 'This is extracted text from the image' },
          error: null,
        },
      }));
      fixture.detectChanges();
      
      const ocrText = fixture.nativeElement.querySelector('.ocr-text');
      expect(ocrText).toBeTruthy();
      expect(ocrText.textContent).toContain('This is extracted text');
    });

    it('should display error message when present', () => {
      component.setActiveTask('caption');
      component['tabStates'].update(states => ({
        ...states,
        caption: { 
          image: 'test', 
          file: null, 
          result: null,
          error: 'Processing failed: Invalid image format',
        },
      }));
      fixture.detectChanges();
      
      const errorMessage = fixture.nativeElement.querySelector('.error-message');
      expect(errorMessage).toBeTruthy();
      expect(errorMessage.textContent).toContain('Processing failed');
    });

    it('should show empty state when no result', () => {
      component.setActiveTask('caption');
      component['tabStates'].update(states => ({
        ...states,
        caption: { 
          image: null, 
          file: null, 
          result: null,
          error: null,
        },
      }));
      fixture.detectChanges();
      
      const emptyState = fixture.nativeElement.querySelector('.empty-state');
      expect(emptyState).toBeTruthy();
    });
  });

  describe('drag and drop', () => {
    it('should call processFile on drop', () => {
      const processFileSpy = spyOn(component, 'processFile');
      const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      
      const dropEvent = new DragEvent('drop', {
        dataTransfer: {
          files: [mockFile],
        },
      } as any);
      
      component.onDrop(dropEvent);
      expect(processFileSpy).toHaveBeenCalledWith(mockFile);
    });

    it('should prevent default on drag over', () => {
      const preventDefaultSpy = jasmine.createSpy('preventDefault');
      const dragEvent = new DragEvent('dragover', {
        dataTransfer: new DataTransfer(),
      });
      spyOn(dragEvent, 'preventDefault').and.callFake(preventDefaultSpy);
      
      component.onDragOver(dragEvent);
      expect(preventDefaultSpy).toHaveBeenCalled();
    });
  });

  describe('zoom modal', () => {
    it('should open zoom modal', () => {
      component.zoomImage('https://example.com/test.jpg');
      expect(component.zoomedImage()).toBe('https://example.com/test.jpg');
    });

    it('should close zoom modal', () => {
      component.zoomedImage.set('https://example.com/test.jpg');
      component.closeZoom();
      expect(component.zoomedImage()).toBeNull();
    });

    it('should not show zoom modal initially', () => {
      expect(component.zoomedImage()).toBeNull();
    });
  });

  describe('submit button state', () => {
    it('should be disabled when no file is selected', () => {
      component['tabStates'].update(states => ({
        ...states,
        caption: { image: null, file: null, result: null, error: null },
      }));
      fixture.detectChanges();
      
      const button = fixture.nativeElement.querySelector('.action-button.primary') as HTMLButtonElement;
      expect(button.disabled).toBe(true);
    });

    it('should be disabled when loading', () => {
      component['tabStates'].update(states => ({
        ...states,
        caption: { 
          image: 'data:image/jpeg;base64,...', 
          file: new File([''], 'test.jpg', { type: 'image/jpeg' }), 
          result: null, 
          error: null,
        },
      }));
      component.isLoading.set(true);
      fixture.detectChanges();
      
      const button = fixture.nativeElement.querySelector('.action-button.primary') as HTMLButtonElement;
      expect(button.disabled).toBe(true);
    });

    it('should be enabled when file is selected and not loading', () => {
      component['tabStates'].update(states => ({
        ...states,
        caption: { 
          image: 'data:image/jpeg;base64,...', 
          file: new File([''], 'test.jpg', { type: 'image/jpeg' }), 
          result: null, 
          error: null,
        },
      }));
      component.isLoading.set(false);
      fixture.detectChanges();
      
      const button = fixture.nativeElement.querySelector('.action-button.primary') as HTMLButtonElement;
      expect(button.disabled).toBe(false);
    });
  });
});
