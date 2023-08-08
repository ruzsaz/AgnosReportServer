/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.agnos.report.server.controller;

import hu.agnos.report.server.service.CubeService;
import java.util.Base64;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author parisek
 */
@RestController
public class MetaCubeController {

    @Autowired
    private CubeService cubeService;

    @GetMapping("/meta/cube")
    ResponseEntity<?> getCubeMeta(@RequestParam(value = "cube_name", required = true) String cubeName) {
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
