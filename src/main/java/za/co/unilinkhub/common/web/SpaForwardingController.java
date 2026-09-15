package za.co.unilinkhub.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Only relevant when the frontend's built assets are bundled into this app's own
 * src/main/resources/static (a single-deployable production setup) rather than served by a
 * separate dev server. Vue Router uses browser history mode, so a hard refresh - or anyone
 * pasting a link - on a client-side route like /dashboard or /listings/{id} is a real request to
 * this server for a path that doesn't correspond to any file; without this, that request 404s
 * instead of loading the SPA shell, which then takes over routing client-side.
 *
 * Excludes anything starting with /api, /actuator or /assets (Vite's build always puts its
 * hashed JS/CSS/image output under /assets - excluding it here, rather than only checking the
 * first path segment for a dot, matters because the {path}/** form below only constrains the
 * FIRST segment; without this exclusion a request for /assets/index-XXXX.js would match on the
 * "assets" segment - no dot, not api/actuator - and get wrongly forwarded to index.html instead
 * of served as the real file), and anything that looks like a static file request at the root
 * (has a dot in its own segment, e.g. /favicon.svg), so those still resolve normally.
 */
@Controller
public class SpaForwardingController {

    @GetMapping(value = {
            "/{path:^(?!api|actuator|assets)[^.]*}",
            "/{path:^(?!api|actuator|assets)[^.]*}/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
