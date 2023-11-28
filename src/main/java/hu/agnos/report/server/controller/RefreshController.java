package hu.agnos.report.server.controller;

import java.util.Optional;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import hu.agnos.cube.meta.resultDto.CubeList;
import hu.agnos.report.server.entity.ReportList;
import hu.agnos.report.server.service.CubeServerClient;

/**
 * An endpoint to refresh the available cubes and reports for the report server. The refresh should be initiated after
 * a change in the base cubes or the reports.
 */
@RestController
public class RefreshController {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(RefreshController.class);
    @Autowired
    ReportList reportList;
    @Autowired
    CubeList cubeList;

    @Value("${agnos.cube.server.uri}")
    private String cubeServerUri;

    /**
     * Refreshes the available cubes from the cube server, and refreshes the available reports based on the presence
     * of their parent cubes.
     *
     * @return "Refreshed" if it was successful
     */
    @PostMapping("/refresh")
    public String refresh() {
        refreshCubeList();
        refreshReports();
        log.info("Cubes and reports are refreshed");
        return "Refreshed";
    }

    private void refreshCubeList() {
        Optional<CubeList> newCubeList = CubeServerClient.getCubeList(cubeServerUri);
        newCubeList.ifPresent(list -> cubeList.renewCubeMap(list.cubeMap()));
    }

    private void refreshReports() {
        reportList.init(cubeList);
    }

}
