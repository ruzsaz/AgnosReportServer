package hu.agnos.report.server.service;

import hu.agnos.report.entity.Report;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
@Component
public class AccessRoleService {

    private static final Logger logger = LoggerFactory.getLogger(AccessRoleService.class);

    @Value("${public-role}")
    private String publicRole;

    private static String PUBLIC_ROLE;

    @Value("${public-role}")
    public void setPublicRoleStatic(String publicRole){
        AccessRoleService.PUBLIC_ROLE = publicRole;
    }

    private static boolean hasRole(SecurityContext context, String role) {
        if (context.getAuthentication() instanceof AnonymousAuthenticationToken) {
            return PUBLIC_ROLE.equalsIgnoreCase(role);
        }
        return context.getAuthentication().getAuthorities().stream().anyMatch(aut -> aut.getAuthority().equalsIgnoreCase(role));
    }

    public static boolean isReportAccessible(SecurityContext context, Report report) {
        return hasRole(context, report.getRoleToAccess()) && !report.isBroken();
    }

    public static List<Report> availableForContext(SecurityContext context, List<Report> original) {
        return original.stream().filter(r -> isReportAccessible(context, r)).collect(Collectors.toList());
    }

    public static String getUserName(SecurityContext context) {
        if (context.getAuthentication() instanceof AnonymousAuthenticationToken) {
            return "anonymous";
        }
        return context.getAuthentication().getName();
    }

}
