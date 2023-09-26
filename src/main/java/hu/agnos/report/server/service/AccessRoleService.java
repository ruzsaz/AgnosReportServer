package hu.agnos.report.server.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import hu.agnos.report.entity.Report;


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

    public static boolean reportAccessible(SecurityContext context, Report report) {
        return hasRole(context, report.getRoleToAccess()) && !report.isBroken();
    }

    public static List<Report> availableForContext(SecurityContext context, List<Report> original) {
        return original.stream().filter(r -> reportAccessible(context, r)).collect(Collectors.toList());
    }

    public static String getUserName(SecurityContext context) {
        if (context.getAuthentication() instanceof AnonymousAuthenticationToken) {
            return "anonymous";
        }
        return context.getAuthentication().getName();
    }

}
