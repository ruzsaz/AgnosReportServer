package hu.agnos.report.server.controller;

import hu.agnos.cube.meta.dto.CubeList;
import hu.agnos.report.server.entity.ReportList;
import hu.agnos.report.server.util.CubeServerClient;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RefreshController {

	private final org.slf4j.Logger log = LoggerFactory.getLogger(RefreshController.class);

	@Autowired
	ReportList reportList;

	@Autowired
	CubeList cubeList;

	@Value("${agnos.cube.server.uri}")
	private String cubeServerUri ;

	@PostMapping("/refresh")
	public String refresh() {
		// TODO: check for credentials?
		refreshCubeList();
		refreshReports(cubeList);
		log.info("Reports refreshed");
		return "Refreshed";
	}

	private void refreshReports(CubeList cubeList) {
		reportList.init(cubeList);
	}

	private void refreshCubeList() {
		cubeList = CubeServerClient.getCubeList(cubeServerUri).orElse(null);
	}

}
