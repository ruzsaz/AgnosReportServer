/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.agnos.report.server.controller;

import java.util.Base64;
import java.util.Optional;

import hu.agnos.report.server.service.AccessRoleService;
import hu.agnos.report.server.service.CubeService;
import hu.mi.agnos.report.entity.Report;
import org.slf4j.LoggerFactory;
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
public class CubeController {

    private final org.slf4j.Logger log = LoggerFactory.getLogger(CubeController.class);

    @Autowired
    private CubeService cubeService;

    @GetMapping(value = "/cube", produces = "application/json")
    ResponseEntity<?> getData(@RequestParam(value = "queries", required = false) String encodedQueries) throws Exception {
        String queries = new String(Base64.getDecoder().decode(encodedQueries));
        Report report = cubeService.getReportEntity(queries);
        System.out.println(report.getRoleToAccess());
        if (AccessRoleService.reportAccessible(SecurityContextHolder.getContext(), report)) {
            String resultSet = cubeService.getData(queries);
            Optional<String> result = Optional.ofNullable(resultSet);
            return result.map(response -> ResponseEntity.ok().body(response))
                    .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        }
        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

}
