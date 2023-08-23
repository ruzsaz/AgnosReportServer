/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.agnos.report.server.controller;

import java.util.Base64;
import java.util.Optional;

import hu.agnos.report.server.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author parisek
 */
@RestController
public class MetaReportController {

    @Autowired
    private ReportService cubeService;

    @GetMapping(value = "/meta/cube", produces = "application/json")
    ResponseEntity<?> getCubeMeta(@RequestParam(value = "cube_name") String cubeName) {
        byte[] decodedBytes = Base64.getDecoder().decode(cubeName);
        cubeName = new String(decodedBytes);
        String cubeUniqueName = cubeName.split(":")[0];
        String reportUniqueName = cubeName.split(":")[1];
        String fullReport = cubeService.getReport(cubeUniqueName, reportUniqueName);
        Optional<String> result = Optional.ofNullable(fullReport);
        return result.map(response -> ResponseEntity.ok().body(response))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
