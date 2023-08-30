/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.agnos.report.server.controller;

import hu.agnos.report.server.service.ReportService;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author parisek
 */
@RestController
public class MetaReportsController {

    @Autowired
    private ReportService reportService;

    @GetMapping(value = "/meta/cubes", produces = "application/json")
    ResponseEntity<?> getCubeList() {
        String jsonCubesHeader = reportService.getReportsHeader(SecurityContextHolder.getContext());
        Optional<String> result = Optional.ofNullable(jsonCubesHeader);
        return result.map(response -> ResponseEntity.ok().body(response))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

}
