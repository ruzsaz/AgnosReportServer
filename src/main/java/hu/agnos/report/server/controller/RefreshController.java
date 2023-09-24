package hu.agnos.report.server.controller;

import hu.agnos.cube.meta.dto.CubeList;
import hu.agnos.report.server.entity.ReportList;
import hu.agnos.report.server.util.CubeServerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RefreshController {

	@Autowired
	ReportList reportList;

	@Autowired
	CubeList cubeList;

	@Value("${agnos.cube.server.uri}")
	private String cubeServerUri ;

	@PostMapping("/refresh")
	public String refresh() {
		refreshCubeList();
		refreshReports(cubeList);
		return "Refreshed";
	}

	private void refreshReports(CubeList cubeList) {
		reportList.init(cubeList);
	}

	private void refreshCubeList() {
		CubeList tmpCubeList = CubeServerClient.getCubeList(cubeServerUri).orElse(null);
		if (cubeList != null && tmpCubeList != null) {
			cubeList.setCubesNameAndDate(tmpCubeList.getCubesNameAndDate());
		}
	}

}
