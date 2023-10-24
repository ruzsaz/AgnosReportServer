///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package hu.agnos.report.server.controller;
//
//import java.util.Base64;
//import java.util.Optional;
//
//import hu.agnos.report.server.service.ReportService;
//import org.slf4j.LoggerFactory;
//import org.slf4j.MDC;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
///**
// *
// * @author parisek
// */
//@RestController
//public class MetaReportController {
//
//    private final org.slf4j.Logger log = LoggerFactory.getLogger(MetaReportController.class);
//
//    @Autowired
//    private ReportService reportService;
//
//    @GetMapping(value = "report_meta", produces = "application/json")
//    ResponseEntity<?> getCubeMeta(@RequestParam(value = "report_name") String encodedReportName) {
//        byte[] decodedBytes = Base64.getDecoder().decode(encodedReportName);
//        String reportName = new String(decodedBytes);
//        System.out.println(reportName);
//
//        String fullReport = reportService.getReport(reportName);
//        Optional<String> result = Optional.ofNullable(fullReport);
//        MDC.put("report", reportName);
//        MDC.put("type", "report");
//        log.info("Report opening");
//        return result.map(response -> ResponseEntity.ok().body(response))
//                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
//    }
//}
