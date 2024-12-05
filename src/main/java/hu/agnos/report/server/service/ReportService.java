package hu.agnos.report.server.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;

import hu.agnos.cube.meta.queryDto.BaseVectorCoordinate;
import hu.agnos.cube.meta.queryDto.DrillVector;
import hu.agnos.cube.meta.queryDto.ReportQuery;
import hu.agnos.cube.meta.resultDto.CubeList;
import hu.agnos.report.entity.Cube;
import hu.agnos.report.entity.Dimension;
import hu.agnos.report.entity.Report;
import hu.agnos.report.server.entity.ReportList;

/**
 * @author parisek
 */
@Service
public class ReportService {

    private static final Pattern PATTERN = Pattern.compile(".");
    private final SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    @Autowired
    ReportList reportList;
    @Autowired
    CubeList cubeList;
    @Autowired
    private DataService dataService;

    public String getReportsHeader(SecurityContext context) {

        StringBuilder stringBuilder = new StringBuilder("{\"reports\":[");
        String origin = stringBuilder.toString();

        for (Report report : AccessRoleService.availableForContext(context, reportList.getReportList())) {
            report.setTopLevelValues(getKpiValue(report));
            String reportString = report.asJson() + ",";
            reportString = ReportService.PATTERN.matcher(reportString).replaceFirst("{\"updated\" : \"" + getLatestCreatedDate(report.getCubes()) + "\",");
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

    private String getLatestCreatedDate(Collection<Cube> cubesInReport) {
        Date latest = cubesInReport.stream().map(cube -> getCreatedDate(cube.getName())).filter(Objects::nonNull).max(Date::compareTo).orElse(null);
        return dateParser.format(latest);
    }

    private Date getCreatedDate(String cubeName) {
        if (cubeList != null) {
            return cubeList.cubeMap().get(cubeName).createdDate();
        }
        return null;
    }

    private String getKpiValue(Report report) {
        List<BaseVectorCoordinate> topBaseVector = new ArrayList<>(report.getDimensions().size());
        for (Dimension dimension : report.getDimensions()) {
            topBaseVector.add(new BaseVectorCoordinate(dimension.getName(), new ArrayList<>(0)));
        }

        List<DrillVector> noDrillVector = new ArrayList<>(1);
        noDrillVector.add(new DrillVector(new ArrayList<>(0)));

        ReportQuery query = new ReportQuery(report.getName(), topBaseVector, noDrillVector);
        return dataService.getData(report, query);
    }

}
