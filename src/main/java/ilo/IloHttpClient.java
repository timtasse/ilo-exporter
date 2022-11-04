package ilo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import ilo.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public class IloHttpClient {

    public static final Logger LOGGER = LoggerFactory.getLogger(IloHttpClient.class);

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final HttpClient client;
    private final URI baseUri;
    private final URI power;
    private final URI thermal;
    private final URI chassis;
    private final URI system;
    private final Credentials creds;
    private final String ip;
    private final LoadingCache<HttpRequest, JsonNode> responseCache;

    private final URI sessionUrl;
    private String sessionToken;

    public IloHttpClient(Credentials creds, String ip, final Duration refreshRate) throws IllegalStateException {
        this.creds = creds;
        this.ip = ip;
        this.baseUri = URI.create("https://" + ip);
        system = this.baseUri.resolve("/redfish/v1/systems/1/");
        chassis = this.baseUri.resolve("/redfish/v1/chassis/1/");
        thermal = this.chassis.resolve("thermal/");
        power = this.chassis.resolve("power/");
        client = HttpClientBuilder.insecure();
        sessionUrl = this.baseUri.resolve("/redfish/v1/SessionService/Sessions/");
        sessionToken = createSession(sessionUrl);
        this.responseCache = CacheBuilder.newBuilder().refreshAfterWrite(refreshRate).build(CacheLoader.from(this::getJsonInternal));
    }

    public URI getSystemUri() {
        return system;
    }

    public URI getBaseUri() {
        return baseUri;
    }

    public ChassisNode getChassisNode() {
        var req = session().uri(chassis).build();
        JsonNode node = getJson(req);
        LOGGER.trace("Chassis: {}", node);
        return new ChassisNode(node, getThermalNode(), getPowerNode(), getSystemNode());
    }

    public PowerNode getPowerNode() {
        var req = session().uri(power).build();

        JsonNode node = getJson(req);
        LOGGER.trace("power: {}", node);
        return new PowerNode(node);
    }

    public ThermalNode getThermalNode() {
        var req = session().uri(thermal).build();
        JsonNode node = getJson(req);
        LOGGER.trace("Thermal: {}", node);
        return new ThermalNode(node);
    }

    public SystemNode getSystemNode() {
        var req = session().uri(system).build();
        JsonNode node = getJson(req);
        LOGGER.trace("System: {}", node);
        return new SystemNode(node, getStorageNode());
    }

    public StorageNode getStorageNode() {
        StorageClient storageClient = new StorageClient(this);
        return storageClient.getStorageNode();
    }

    private String createSession(URI sessionUrl) {
        HttpRequest req = HttpRequest.newBuilder().header("Content-Type", "application/json")
                .header("OData-Version", "4.0").uri(sessionUrl).POST(BodyPublishers.ofString(creds.toJson())).build();
        try {
            HttpResponse<String> resp = client.send(req, BodyHandlers.ofString());
            LOGGER.debug("url: {}, status: {}, body: {}", sessionUrl, resp.statusCode(), resp.body());
            final Optional<String> authToken = resp.headers().firstValue("x-auth-token");
            return authToken.orElseThrow(() -> new IllegalStateException("No x-auth-token header found"));
        } catch (IOException e) {
            throw new IllegalStateException("Could not create session", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Could not create session", e);
        }
    }

    public JsonNode getJson(HttpRequest request) {
        return responseCache.getUnchecked(request);
    }

    private JsonNode getJsonInternal(HttpRequest request) {

        try {
            var response = client.send(request, BodyHandlers.ofString());
            if (response.statusCode() == 401) {
                sessionToken = createSession(sessionUrl);
            }
            if (response.statusCode() != 200) {
                throw new IllegalStateException("could not retrieve json: " + response.toString());
            }

            return mapper.readTree(response.body());
        } catch (IOException e) {
            throw new IllegalStateException("could not retrieve json", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Could not create session", e);
        }
    }

    public HttpRequest.Builder session() {
        return HttpRequest.newBuilder().header("x-auth-token", sessionToken);
    }

    public HttpRequest.Builder basic() {
        return HttpRequest.newBuilder().header("Authorization", basicAuth(creds));
    }

    private String basicAuth(Credentials creds) {
        return HttpClientBuilder.basicAuth(creds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        IloHttpClient other = (IloHttpClient) obj;
        return Objects.equals(ip, other.ip);
    }

}
