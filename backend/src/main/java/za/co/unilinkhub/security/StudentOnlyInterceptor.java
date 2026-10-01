package za.co.unilinkhub.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import za.co.unilinkhub.common.exception.ForbiddenException;
import za.co.unilinkhub.common.web.RejectionMessages;

/** Enforces {@link StudentOnly}: a logged-in admin calling a marketplace endpoint gets a 403 saying why. */
public class StudentOnlyInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        StudentOnly rule = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), StudentOnly.class);
        if (rule == null) {
            rule = AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), StudentOnly.class);
        }
        if (rule != null && CurrentUserProvider.current().map(UserPrincipal::isAdmin).orElse(false)) {
            throw new ForbiddenException("Admin accounts can't " + rule.value() + ". Admin accounts are only for running "
                    + "UniLinkHub, so they stay neutral - use a student account to take part in the marketplace.",
                    RejectionMessages.CODE_ADMIN_ACCOUNT);
        }
        return true;
    }
}
