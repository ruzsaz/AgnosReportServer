package hu.agnos.report.server.controller;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import hu.agnos.cube.meta.resultDto.CubeList;
import hu.agnos.report.server.entity.ReportList;
import hu.agnos.report.server.service.CubeServerClient;

@RestController
public class RefreshController {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(RefreshController.class);

    @Autowired
    ReportList reportList;

    @Autowired
    CubeList cubeList;

    @Value("${agnos.cube.server.uri}")
    private String cubeServerUri;

    @PostMapping("/refresh")
    public String refresh() {
        // TODO: check for credentials?
        refreshCubeList();
        refreshReports();
        log.info("Reports refreshed");
        return "Refreshed";
    }

    private void refreshCubeList() {
        cubeList = CubeServerClient.getCubeList(cubeServerUri).orElse(null);
    }

    private void refreshReports() {
        reportList.init(cubeList);
    }

}
