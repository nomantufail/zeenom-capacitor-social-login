import { registerPlugin } from '@capacitor/core';

import type { LoginPluginPlugin } from './definitions';

const LoginPlugin = registerPlugin<LoginPluginPlugin>('LoginPlugin', {
  web: () => import('./web').then((m) => new m.LoginPluginWeb()),
});

export * from './definitions';
export { LoginPlugin };
