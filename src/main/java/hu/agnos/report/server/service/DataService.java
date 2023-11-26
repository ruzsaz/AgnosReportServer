package hu.agnos.report.server.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import hu.agnos.cube.meta.queryDto.CubeQuery;
import hu.agnos.cube.meta.queryDto.ReportQuery;
import hu.agnos.cube.meta.resultDto.CubeList;
import hu.agnos.cube.meta.resultDto.CubeMetaDTO;
import hu.agnos.cube.meta.resultDto.ResultSet;
import hu.agnos.report.entity.Cube;
import hu.agnos.report.entity.Report;
import hu.agnos.report.server.entity.Cache;
import hu.agnos.report.server.service.answerProcessor.ResponseConverter;
import hu.agnos.report.server.service.queryGenerator.CubeQueryCreator;

@Service
public class DataService {

    @Value("${agnos.cube.server.uri}")
    private String cubeServerUri;

    @Autowired
    private CubeList cubeList;

    @Autowired
    private Cache cache;

    private ExecutorService executor;

    public String getData(Report report, ReportQuery query) {
        int numberOfCubes = report.getCubes().size();

        long start = System.currentTimeMillis();
        executor = Executors.newFixedThreadPool(numberOfCubes);
        List<CompletableFuture<ResultSet>> resultFutures = new ArrayList<>(numberOfCubes);
        for (Cube cube : report.getCubes()) {
            resultFutures.addAll(getDataFromCacheAsync(report, cube, query));
        }
        List<ResultSet> resultSetsList = resultFutures.stream().map(CompletableFuture::join).collect(Collectors.toList());
        long end = System.currentTimeMillis();
        System.out.printf("Data query from the cubes took %s ms%n", end - start);

        ResponseConverter responseConverter = new ResponseConverter(report, query, Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
        String answer = responseConverter.getAnswer(resultSetsList).asJson();
        long end2 = System.currentTimeMillis();
        System.out.printf("Resolving the data took %s ms%n", end2 - end);
        return answer;
    }

    private List<CompletableFuture<ResultSet>> getDataFromCacheAsync(Report report, Cube cube, ReportQuery query) {
        List<CompletableFuture<ResultSet>> completableFutures = new ArrayList<>();
        CubeMetaDTO cubeMeta = cubeList.cubeMap().get(cube.getName());
        CubeQueryCreator cubeQueryCreator = new CubeQueryCreator(report, cube.getName(), cubeMeta);
        List<CubeQuery> queriesForCube = cubeQueryCreator.createCubeQuery(query);
        for (CubeQuery queryForCube : queriesForCube) {
            CompletableFuture<ResultSet> completableFuture = new CompletableFuture<>();
            executor.submit(() -> {
                Optional<ResultSet> cachedResult = cache.get(queryForCube);
                if (cachedResult.isPresent()) {
                    System.out.println("CACHE hit");
                    completableFuture.complete(cachedResult.get());
                } else {
                    System.out.println("Cache miss");
                    long startTime = System.currentTimeMillis();
                    ResultSet result = CubeServerClient.getCubeData(cubeServerUri, queryForCube);
                    long endTime = System.currentTimeMillis();
                    cache.insert(queryForCube, result, endTime - startTime);
                    completableFuture.complete(result);
                }
            });
            completableFutures.add(completableFuture);
        }
        return completableFutures;
    }

}
