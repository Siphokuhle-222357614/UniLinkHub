/// <reference types="vite/client" />

import "vue-router";

declare module "vue-router" {
  interface RouteMeta {
    requiresAuth?: boolean;
    requiresAdmin?: boolean;
    /** Full-height screens (the chat) where the site footer would get in the way. */
    hideFooter?: boolean;
  }
}
