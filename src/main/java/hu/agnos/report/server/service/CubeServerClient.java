package hu.agnos.report.server.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.LoggerFactory;

import hu.agnos.cube.meta.queryDto.CubeQuery;
import hu.agnos.cube.meta.resultDto.CubeList;
import hu.agnos.cube.meta.resultDto.ResultSet;

import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * Retrieves data from a running Cube Server service.
 */
public final class CubeServerClient {

    private CubeServerClient() {
    }

    /**
     * Retrieve data for a frontend request from a single Cube.
     *
     * @param cubeServerUri Uri to reach the Cube Server
     * @param cubeQueries Query for a single cube to fulfill the data request
     * @return List of result sets, where an element contains the answer for a single drill
     */
    public static List<ResultSet> getCubeData(String cubeServerUri, List<CubeQuery> cubeQueries) {
        try {
            String body = new ObjectMapper().writeValueAsString(cubeQueries);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URIBuilder(cubeServerUri + "/data").build())
                    .timeout(Duration.of(20L, SECONDS))
                    .setHeader("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            TypeReference<ArrayList<ResultSet>> type = new ArrayListTypeReference();

            return new ObjectMapper().readValue(response.body(), type);
        } catch (URISyntaxException | IOException | InterruptedException ex) {
            LoggerFactory.getLogger(CubeServerClient.class).error("Error at downloading data", ex);
        }
        return null;
    }

    /**
     * Downloads all the cubes' meta.
     *
     * @param cubeServerUri Uri where the meta is found
     * @return All available cube meta
     */
    public static Optional<CubeList> getCubeList(String cubeServerUri) {
        CubeList cubeList = null;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(cubeServerUri + "/cube_list"))
                    .timeout(Duration.of(10L, SECONDS))
                    .GET()
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            cubeList = new ObjectMapper().readValue(response.body(), CubeList.class);
        } catch (URISyntaxException | IOException | InterruptedException ex) {
            LoggerFactory.getLogger(CubeServerClient.class).error("Error at downloading cube list", ex);
        }
        return Optional.ofNullable(cubeList);
    }

    private static class ArrayListTypeReference extends TypeReference<ArrayList<ResultSet>> {
    }
}
