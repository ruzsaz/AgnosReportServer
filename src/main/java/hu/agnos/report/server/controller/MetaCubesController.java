/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.agnos.report.server.controller;

import hu.agnos.report.server.service.CubeService;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author parisek
 */
@RestController
public class MetaCubesController {

    @Autowired
    private CubeService cubeService;

    @GetMapping("/meta/cubes")
    ResponseEntity<?> getCubeList() {
        System.out.println("itt");
        String jsonCubesHeader = cubeService.getReportsHeader();
        Optional<String> result = Optional.ofNullable(jsonCubesHeader);
        return result.map(response -> ResponseEntity.ok().body(response))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

}
