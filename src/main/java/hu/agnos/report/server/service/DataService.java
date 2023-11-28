package hu.agnos.report.server.service;

import java.util.ArrayList;
import java.util.Iterator;
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

    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public String getData(Report report, ReportQuery query) {
        int numberOfCubes = report.getCubes().size();

        long start = System.currentTimeMillis();
        List<CompletableFuture<List<ResultSet>>> resultFutures = new ArrayList<>(numberOfCubes);
        for (Cube cube : report.getCubes()) {
            resultFutures.add(getDataFromCacheAsync(report, cube, query));
        }
        List<ResultSet> resultSetsList = resultFutures.stream().map(CompletableFuture::join).flatMap(List::stream).collect(Collectors.toList());
        long end = System.currentTimeMillis();

        ResponseConverter responseConverter = new ResponseConverter(report, query, Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
        String answer = responseConverter.getAnswer(resultSetsList).asJson();
        long end2 = System.currentTimeMillis();
        System.out.println(cache.toString() + ". --- Answer from the cubes: " + (end - start) + "ms, postprocess: " + (end2 - end) + "ms.");
        return answer;
    }

    private CompletableFuture<List<ResultSet>> getDataFromCacheAsync(Report report, Cube cube, ReportQuery query) {
        CubeMetaDTO cubeMeta = cubeList.cubeMap().get(cube.getName());
        CubeQueryCreator cubeQueryCreator = new CubeQueryCreator(report, cube.getName(), cubeMeta);
        List<CubeQuery> queriesForCube = cubeQueryCreator.createCubeQuery(query);
        CompletableFuture<List<ResultSet>> completableFuture = new CompletableFuture<>();
        executor.submit(() -> {
            List<ResultSet> result = new ArrayList<>(queriesForCube.size());
            for (Iterator<CubeQuery> iterator = queriesForCube.iterator(); iterator.hasNext(); ) {
                CubeQuery cubeQuery = iterator.next();
                Optional<ResultSet> cachedResult = cache.get(cubeQuery);
                if (cachedResult.isPresent()) {
                    result.add(cachedResult.get());
                    iterator.remove();
                }
            }
            if (!queriesForCube.isEmpty()) {
                long startTime = System.currentTimeMillis();
                List<ResultSet> resultFromCube = CubeServerClient.getCubeData(cubeServerUri, queriesForCube);
                long runTime = System.currentTimeMillis() - startTime;
                int resultSize = resultFromCube.size();
                for (int i = 0; i < resultSize; i++) {
                    cache.insert(queriesForCube.get(i), resultFromCube.get(i), runTime);
                    result.add(resultFromCube.get(i));
                }
            }
            completableFuture.complete(result);
        });

        return completableFuture;
    }

}
