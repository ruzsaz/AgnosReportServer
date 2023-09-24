package hu.agnos.report.server.entity;

import java.util.List;

import hu.agnos.cube.meta.dto.CubeList;
import hu.agnos.report.entity.Report;
import hu.agnos.report.repository.ReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class ReportList {

    private List<Report> reportList;

    @Autowired
    public ReportList(CubeList cubeList) {
        this.init(cubeList);
    }

    public void init(CubeList cubeList) {
        this.reportList = (new ReportRepository()).findAll();
        setBrokenStatesFromAvailableCubes(cubeList);
    }

    public void setBrokenStatesFromAvailableCubes(CubeList cubeList) {
        for (Report r : reportList) {
            r.setBroken((cubeList == null) || !cubeList.containsCubeWithName(r.getCubeName()));
        }
    }

    public List<Report> getReportList() {
        return reportList;
    }

}
