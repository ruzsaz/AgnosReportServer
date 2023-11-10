package hu.agnos.report.server.configuration;

import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import hu.agnos.report.server.service.AccessRoleService;

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