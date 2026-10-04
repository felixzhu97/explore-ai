import { type Routes } from '@angular/router';
import { PoliciesPageComponent } from './policies.page';

export const POLICIES_ROUTES: Routes = [
  { path: '', component: PoliciesPageComponent },
  { path: ':slug', component: PoliciesPageComponent },
];
