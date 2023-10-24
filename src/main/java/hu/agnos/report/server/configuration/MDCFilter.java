package hu.agnos.report.server.configuration;

import java.io.IOException;

import jakarta.servlet.*;

import hu.agnos.report.server.service.AccessRoleService;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Automatically determine the username in each request, and put into the global
 * MDC environment for logging.
 */
@Component
public class MDCFilter implements Filter {

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        String userName = AccessRoleService.getUserName(SecurityContextHolder.getContext());
        MDC.put("user", userName);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

}