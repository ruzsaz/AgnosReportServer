package hu.agnos.report.server.controller;

import java.util.Optional;

import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import hu.agnos.report.server.service.ReportService;

@RestController
public class MetaReportsController {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(MetaReportsController.class);

    @Autowired
    private ReportService reportService;

    @GetMapping(value = "reports", produces = "application/json")
    ResponseEntity<?> getReportsList() {
        String jsonCubesHeader = reportService.getReportsHeader(SecurityContextHolder.getContext());
        Optional<String> result = Optional.ofNullable(jsonCubesHeader);
        MDC.put("type", "reportList");
        log.info("Available reports list query");
        return result.map(response -> ResponseEntity.ok().body(response))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

//    @GetMapping(value = "keywords", produces = "application/json")
//    ResponseEntity<?> getKeywordsList() {
//        String keywordList = reportService.getReportsHeader(SecurityContextHolder.getContext());
//        Optional<String> result = Optional.ofNullable(keywordList);
//        MDC.put("type", "keywordList");
//        log.info("Available keywords query");
//        return result.map(response -> ResponseEntity.ok().body(response))
//                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
//    }

}
