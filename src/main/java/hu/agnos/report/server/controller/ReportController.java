package hu.agnos.report.server.controller;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import hu.agnos.cube.meta.queryDto.ReportQuery;
import hu.agnos.report.entity.Report;
import hu.agnos.report.server.service.AccessRoleService;
import hu.agnos.report.server.service.DataService;
import hu.agnos.report.server.service.ReportService;

@RestController
public class ReportController {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ReportController.class);

    @Autowired
    private ReportService reportService;

    @Autowired
    private DataService dataService;

    /**
     * @param encodedQueries
     * @return
     * @throws com.fasterxml.jackson.core.JsonProcessingException
     * @throws com.fasterxml.jackson.databind.JsonMappingException
     * @throws IllegalArgumentException
     */
    @GetMapping(value = "/report", produces = "application/json")
    ResponseEntity<?> getData(@RequestParam(value = "queries", required = false) String encodedQueries) throws JsonProcessingException, JsonMappingException {
        log.debug("New report query ---------------------------------------------");
        String queries = URLDecoder.decode(new String(Base64.getDecoder().decode(encodedQueries), StandardCharsets.UTF_8));
        log.debug(queries);
        ReportQuery query = new ObjectMapper().readValue(queries, ReportQuery.class);
        log.debug(query.toString());
        Report report = reportService.getReportByName(query.reportName());
        MDC.put("report", report.getName());

        if (AccessRoleService.isReportAccessible(SecurityContextHolder.getContext(), report)) {
            String resultSet = dataService.getData(report, query, false);
            Optional<String> result = Optional.ofNullable(resultSet);
            MDC.put("type", (query.isCubePreparationRequired()) ? "report_access" : "data");
            log.info("Successful data access");
            return result.map(response -> ResponseEntity.ok().body(response))
                    .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        }
        MDC.put("type", "unauthorized");
        log.debug("Unauthorized data access attempt");
        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

}
