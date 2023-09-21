package hu.agnos.report.server.controller;

import java.util.List;

import hu.agnos.report.entity.Report;
import hu.agnos.report.repository.ReportRepository;
import hu.agnos.report.server.entity.ReportList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RefreshController {

	@Autowired
	ReportList reportList;

	@GetMapping("/refresh")
	public String refresh() {
		reportList.init();
		return "Refreshed";
	}
}
