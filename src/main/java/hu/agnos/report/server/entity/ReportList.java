package hu.agnos.report.server.entity;

import java.util.List;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import hu.agnos.cube.meta.resultDto.CubeList;
import hu.agnos.report.entity.Cube;
import hu.agnos.report.entity.Report;
import hu.agnos.report.repository.ReportRepository;

@Getter
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public final class ReportList {

    private List<Report> reportList;

    @Autowired
    public ReportList(CubeList cubeList) {
        init(cubeList);
    }

    public void init(CubeList cubeList) {
        this.reportList = (new ReportRepository()).findAll();
        setBrokenStatesFromAvailableCubes(cubeList);
    }

    private void setBrokenStatesFromAvailableCubes(CubeList cubeList) {
        for (Report report : reportList) {
            ReportList.setBrokenStateFromAvailableCubes(cubeList, report);
        }
    }

    private static void setBrokenStateFromAvailableCubes(CubeList cubeList, Report report) {
        for (Cube cube : report.getCubes()) {
            if ((cubeList == null) || !cubeList.cubeMap().containsKey(cube.getName())) {
                report.setBroken(true);
                break;
            }
        }
    }

}
