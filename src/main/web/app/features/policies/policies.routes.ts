import { Routes } from '@angular/router';
import { PoliciesPageComponent } from './pages/policies.page';

export const POLICIES_ROUTES: Routes = [
  { path: '', component: PoliciesPageComponent },
  { path: ':slug', component: PoliciesPageComponent },
];
