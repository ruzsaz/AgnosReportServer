package hu.agnos.report.server.service;

import hu.agnos.cube.meta.resultDto.CubeList;
import hu.agnos.report.entity.Cube;
import hu.agnos.report.entity.Report;
import hu.agnos.report.server.entity.ReportList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * @author parisek
 */
@Service
public class ReportService {

    private static final Pattern COMPILE = Pattern.compile(".");
    private final SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    @Autowired
    ReportList reportList;
    @Autowired
    CubeList cubeList;

    public String getReportsHeader(SecurityContext context) {

        StringBuilder stringBuilder = new StringBuilder("{\"reports\":[");
        String origin = stringBuilder.toString();

        for (Report report : AccessRoleService.availableForContext(context, reportList.getReportList())) {
            String reportString = report.asJson() + ",";
            reportString = COMPILE.matcher(reportString).replaceFirst("{\"updated\" : \"" + getLatestCreatedDate(report.getCubes()) + "\",");
            stringBuilder.append(reportString);
        }

        if (!origin.contentEquals(stringBuilder)) {
            stringBuilder = new StringBuilder(stringBuilder.substring(0, (stringBuilder.length() - 1)));
        }

        return stringBuilder + "]}";
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

    private String getLatestCreatedDate(Collection<Cube> cubeList) {
        Date latest = cubeList.stream().map(cube -> getCreatedDate(cube.getName())).filter(Objects::nonNull).max(Date::compareTo).orElse(null);
        return dateParser.format(latest);
    }

    private Date getCreatedDate(String cubeName) {
        if (cubeList != null) {
            return cubeList.cubeMap().get(cubeName).createdDate();
        }
        return null;
    }

}
