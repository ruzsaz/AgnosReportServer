package hu.agnos.report.server.service;

import java.util.List;
import java.util.stream.Collectors;

import hu.mi.agnos.report.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import hu.mi.agnos.report.entity.Report;


@Service
@Component
public class AccessRoleService {

    private static final Logger logger = LoggerFactory.getLogger(AccessRoleService.class);;

    @Value("${public-role}")
    private String publicRole;

    private static String PUBLIC_ROLE;



    @Value("${public-role}")
    public void setPublicRoleStatic(String publicRole){
        AccessRoleService.PUBLIC_ROLE = publicRole;
    }

    private static boolean hasRole(SecurityContext context, String role) {
        for (GrantedAuthority ga : context.getAuthentication().getAuthorities()){
            logger.info("role: " + role + ", autority: " +  ga.getAuthority());
        }

        if (context.getAuthentication() instanceof AnonymousAuthenticationToken) {
            return PUBLIC_ROLE.equalsIgnoreCase(role);
        }


        return context.getAuthentication().getAuthorities().stream().anyMatch(aut -> aut.getAuthority().equalsIgnoreCase(role));
    }

    public static boolean reportAccessible(SecurityContext context, Report report) {
        return hasRole(context, report.getRoleToAccess());
    }

    public static List<Report> availableForContext(SecurityContext context, List<Report> original) {


        List<Report> result = original.stream().filter(r -> hasRole(context, r.getRoleToAccess())).collect(Collectors.toList());
        return result;
    }

}
