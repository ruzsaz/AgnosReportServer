package hu.agnos.report.server.controller;

import java.util.Base64;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.agnos.cube.meta.drillDto.ReportQuery;
import hu.agnos.report.entity.Report;
import hu.agnos.report.server.service.AccessRoleService;
import hu.agnos.report.server.service.DataService;
import hu.agnos.report.server.service.ReportService;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author parisek
 */
@RestController
public class ReportController {

    private final org.slf4j.Logger log = LoggerFactory.getLogger(ReportController.class);

    @Autowired
    private ReportService reportService;

    @Autowired
    private DataService dataService;

    @GetMapping(value = "/report", produces = "application/json")
    ResponseEntity<?> getData(@RequestParam(value = "queries", required = false) String encodedQueries) throws Exception {
        String queries = new String(Base64.getDecoder().decode(encodedQueries));
        System.out.println(queries);
        ObjectMapper objectMapper = new ObjectMapper();

        ReportQuery query = objectMapper.readValue(queries, ReportQuery.class);
        Report report = reportService.getReportByName(query.reportName());
        MDC.put("report", report.getName());

        if (AccessRoleService.reportAccessible(SecurityContextHolder.getContext(), report)) {
            String resultSet = dataService.getData(report, query);
            Optional<String> result = Optional.ofNullable(resultSet);
            MDC.put("type", "data");
            log.info("Successful data access");
            return result.map(response -> ResponseEntity.ok().body(response))
                    .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        }
        MDC.put("type", "unauthorized");
        log.debug("Unauthorized data access attempt");
        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

}
