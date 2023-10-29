package hu.agnos.report.server.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

import hu.agnos.cube.meta.dto.CubeList;
import hu.agnos.report.entity.Cube;
import hu.agnos.report.entity.Report;
import hu.agnos.report.server.entity.ReportList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;

/**
 *
 * @author parisek
 */
@Service
public class ReportService {

    @Autowired
    ReportList reportList;

    @Autowired
    CubeList cubeList;

    @Autowired
    private ApplicationContext applicationContext;

    private final SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    public String getReport(String reportUniqueName) {
        return getReportByName(reportUniqueName).asJson();
    }

    public String getReportsHeader(SecurityContext context) {

        String s = "{\"reports\":[";
        String origin = new String(s);

        for (Report report : AccessRoleService.availableForContext(context, this.reportList.getReportList())) {
            String reportString = report.asJson() + ",";
            // System.out.println(reportString);
            reportString = reportString.replaceFirst(".", "{\"updated\" : \"" + getLatestCreatedDate(report.getCubes()) + "\",");
            s += reportString;
        }

        if (!origin.equals(s)) {
            s = s.substring(0, (s.length() - 1));
        }

        return s + "]}";
    }

    public Report getReportByName(String reportName) {
        Report result = null;
        for (Report r : reportList.getReportList()) {
            if (r.getName().equalsIgnoreCase(reportName)) {
                result = r;
                break;
            }
        }
        return result;
    }

    private String getLatestCreatedDate(ArrayList<Cube> cubeList) {
        Date latest = cubeList.stream().map(c -> getCreatedDate(c.getName())).filter(Objects::nonNull).max(Date::compareTo).orElse(null);
        return dateParser.format(latest);
    }

    private Date getCreatedDate(String cubeName) {
        if (cubeList != null) {
            return cubeList.cubeMap().get(cubeName).createdDate();
        }
        return null;
    }
}
