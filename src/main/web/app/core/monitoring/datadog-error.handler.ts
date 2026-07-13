import { ErrorHandler, inject, Injectable } from '@angular/core';
import { DatadogRumService } from './datadog-rum.service';

@Injectable()
export class DatadogErrorHandler implements ErrorHandler {
  private readonly datadogRum = inject(DatadogRumService);

  handleError(error: unknown): void {
    console.error(error);
    this.datadogRum.addError(error);
  }
}
