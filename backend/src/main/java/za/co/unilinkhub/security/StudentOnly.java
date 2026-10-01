package za.co.unilinkhub.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an endpoint as taking part in the marketplace (buying, selling, booking, reviewing...),
 * which admin accounts must never do: admins run UniLinkHub and have to stay neutral - an admin
 * who sells could, for example, approve their own business or moderate their competitors.
 * {@link StudentOnlyInterceptor} rejects admins with a plain-English 403 built from {@link #value()}.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface StudentOnly {

    /** What the admin was trying to do, phrased to follow "Admin accounts can't ...", e.g. "place orders". */
    String value();
}
