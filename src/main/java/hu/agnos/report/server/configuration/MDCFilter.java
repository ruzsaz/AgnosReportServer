package hu.agnos.report.server.configuration;

import hu.agnos.report.server.service.AccessRoleService;
import jakarta.servlet.*;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Automatically determine the username in each request, and put into the global MDC environment for logging.
 */
@Component
public class MDCFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        String userName = AccessRoleService.getUserName(SecurityContextHolder.getContext());
        MDC.put("user", userName);
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            MDC.clear();
        }
    }

}