/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.agnos.report.server.controller;

import hu.agnos.report.server.service.CubeService;
import java.util.Base64;
import java.util.Optional;
import org.slf4j.LoggerFactory;
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
public class CubeController {

    private final org.slf4j.Logger log = LoggerFactory.getLogger(CubeController.class);

    @Autowired
    private CubeService cubeService;

    @GetMapping("/cube")
    ResponseEntity<?> getData(@RequestParam(value = "queries", required = false) String decodedQueries) throws Exception {
        String queries = Base64.getEncoder().encodeToString(decodedQueries.getBytes());

        String resultSet = cubeService.getData(queries);

        Optional<String> result = Optional.ofNullable(resultSet);
        return result.map(response -> ResponseEntity.ok().body(response))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

}
