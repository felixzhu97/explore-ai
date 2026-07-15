import type { AngularComponentImplementation } from '@a2ui/angular/v0_9';
import { ChartApi } from './chart.api';
import { ChartComponent } from './components/chart.component';
import { EXPLORE_CHAT_CATALOG_ID } from './catalog.constants';

export { EXPLORE_CHAT_CATALOG_ID };

/** Chart entry for BasicCatalog.extraComponents */
export const ChartComponentImplementation: AngularComponentImplementation = {
  name: ChartApi.name,
  schema: ChartApi.schema,
  component: ChartComponent,
};
