import { describe, expect, it } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { McpPageComponent } from './mcp.page';
import { McpService } from './mcp.service';

describe('McpPageComponent', () => {
  function setup() {
    TestBed.configureTestingModule({
      imports: [McpPageComponent],
      providers: [
        {
          provide: McpService,
          useValue: {
            getHealth: () => of({ status: 'UP' }),
            getClientStatus: () => of({ status: 'READY', registeredTools: 0, connectedServers: [] }),
            listTools: () => of([]),
          },
        },
      ],
    });
    const fixture = TestBed.createComponent(McpPageComponent);
    fixture.detectChanges();
    return fixture;
  }

  function sendButton(host: HTMLElement): HTMLButtonElement | undefined {
    return [...host.querySelectorAll('button')].find(button => button.classList.contains('mt-3'));
  }

  it('should disable send on first render when the question is empty', () => {
    const fixture = setup();
    expect(sendButton(fixture.nativeElement as HTMLElement)?.disabled).toBe(true);
  });

  it('should enable send once a question is typed', () => {
    const fixture = setup();
    const host = fixture.nativeElement as HTMLElement;
    const textarea = host.querySelector('textarea');
    if (textarea === null) {
      throw new Error('question textarea not rendered');
    }
    textarea.value = 'What tools exist?';
    textarea.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(fixture.componentInstance.question()).toBe('What tools exist?');
    expect(sendButton(host)?.disabled).toBe(false);
  });
});
