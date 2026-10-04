import type { AngularComponentImplementation } from '@a2ui/angular/v0_9';
import { ChartApi } from './a2ui-chart.api';
import { A2uiChartComponent } from './a2ui-chart.component';
import { EXPLORE_CHAT_CATALOG_ID } from './a2ui-catalog.constants';

export { EXPLORE_CHAT_CATALOG_ID };

/** Chart entry for BasicCatalog.extraComponents */
export const ChartComponentImplementation: AngularComponentImplementation = {
  name: ChartApi.name,
  schema: ChartApi.schema,
  component: A2uiChartComponent,
};
