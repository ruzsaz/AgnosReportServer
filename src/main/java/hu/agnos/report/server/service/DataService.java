package hu.agnos.report.server.service;

import hu.agnos.cube.meta.queryDto.CubeQuery;
import hu.agnos.cube.meta.queryDto.ReportQuery;
import hu.agnos.cube.meta.resultDto.CubeList;
import hu.agnos.cube.meta.resultDto.CubeMetaDTO;
import hu.agnos.cube.meta.resultDto.ResultSet;
import hu.agnos.report.entity.Cube;
import hu.agnos.report.entity.Report;
import hu.agnos.report.server.service.answerProcessor.ResponseConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static hu.agnos.report.server.service.queryGenerator.CubeQueryCreator.createCubeQuery;

@Service
public class DataService {

    @Value("${agnos.cube.server.uri}")
    private String cubeServerUri;

    @Autowired
    private CubeList cubeList;

    private ExecutorService executor;

    public String getData(Report report, ReportQuery query) {
        int numberOfCubes = report.getCubes().size();

        long start = System.currentTimeMillis();
        executor = Executors.newFixedThreadPool(numberOfCubes);
        List<CompletableFuture<ResultSet[]>> resultFutures = new ArrayList<>(numberOfCubes);
        for (Cube cube : report.getCubes()) {
            resultFutures.add(getDataFromCubeAsync(report, cube, query));
        }
        List<ResultSet[]> resultSetsList = resultFutures.stream().map(CompletableFuture::join).collect(Collectors.toList());
        long end = System.currentTimeMillis();
        System.out.printf("Data query from the cubes took %s ms%n", end - start);

        ResponseConverter responseConverter = new ResponseConverter(report, query, Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
        String answer = responseConverter.getAnswer(resultSetsList).asJson();
        long end2 = System.currentTimeMillis();
        System.out.printf("Resolving the data took %s ms%n", end2 - end);
        return answer;
    }

    private CompletableFuture<ResultSet[]> getDataFromCubeAsync(Report report, Cube cube, ReportQuery query) {
        CompletableFuture<ResultSet[]> completableFuture = new CompletableFuture<>();
        executor.submit(() -> {
            CubeMetaDTO cubeMeta = cubeList.cubeMap().get(cube.getName());
            CubeQuery queryForCube = createCubeQuery(report, cube.getName(), cubeMeta, query);
            completableFuture.complete(CubeServerClient.getCubeData(cubeServerUri, queryForCube));
        });
        return completableFuture;
    }

}
