package ilo;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import ilo.model.*;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IloCollector extends Collector {

    private static final Logger LOGGER = LoggerFactory.getLogger(IloCollector.class);

    private final List<IloHttpClient> clients = new ArrayList<>();
    private final LoadingCache<IloHttpClient, ChassisNode> nodeCache;
    private final ExecutorService executorService;


    public IloCollector() {
        Credentials creds = Credentials.fromEnvironment();
        Duration refreshRate = Duration.parse(System.getenv().getOrDefault(Environment.REFRESH_RATE, "PT30s"));
        String hosts = System.getenv(Environment.HOSTS);
        Preconditions.checkNotNull(hosts, "ILO_HOSTS environment variable is not set");
        var servers = new HostParser().parseHosts(hosts);
        for (var ip : servers) {
            try {
                this.clients.add(new IloHttpClient(creds, ip, refreshRate));
            } catch (IllegalStateException e) {
                LOGGER.error("Cannot connect to {}", ip, e);
            }
        }
        executorService = Executors.newFixedThreadPool(50);
        nodeCache = CacheBuilder.newBuilder()
                .refreshAfterWrite(refreshRate)
                .initialCapacity(servers.size())
                .concurrencyLevel(50)
                .build(CacheLoader.asyncReloading(CacheLoader.from(IloHttpClient::getChassisNode), executorService));
        LOGGER.info("Refresh rate set to: {}", refreshRate);
        LOGGER.info("monitoring ilos: {}", servers);
        LOGGER.info("using credentials: {}", creds);
    }

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> samples = new ArrayList<>();
        GaugeMetricFamily powerSamples = new GaugeMetricFamily("ilo_chassis_power_in_watts", "power in watts",
                List.of("hostname"));
        GaugeMetricFamily fanSamples = new GaugeMetricFamily("ilo_chassis_fan_percent", "percent fans",
                Arrays.asList("hostname", "fan"));
        GaugeMetricFamily tempSamples = new GaugeMetricFamily("ilo_chassis_temp", "temperature (C)",
                Arrays.asList("hostname", "temp"));
        GaugeMetricFamily diskSamples = new GaugeMetricFamily("ilo_disk_status", "status of disks",
                Arrays.asList("hostname", "container_port", "box", "bay", "status", "state", "reason"));
        for (IloHttpClient client : clients) {
            ChassisNode node;
            try {
                node = nodeCache.get(client);
                String hostname = node.getHostName();
                powerSamples.addMetric(List.of(hostname), node.getPowerNode().getValue());
                for (FanNode fanNode : node.getThermalNode().getFans()) {
                    fanSamples.addMetric(List.of(hostname, fanNode.getLabel()), fanNode.getValue());
                }
                for (TemperatureNode tempNode : node.getThermalNode().getTempuratures()) {
                    tempSamples.addMetric(List.of(hostname, tempNode.getLabel()), tempNode.getValue());
                }
                for (ArrayController array : node.getSystemNode().getStorageNode().getArrays()) {
                    for (DiskNode disk : array.getDiskDrives()) {
                        var health = disk.getHealth();
                        double statusValue = health.getState().equals("Enabled") ? 1.0 : 0.0;

                        var location = disk.getLocation();
                        var port = location.getControllerPort();
                        var box = location.getBox();
                        var bay = location.getBay();
                        diskSamples.addMetric(List.of(hostname, port, box, bay, health.getStatus(),
                                health.getState(), health.getReason()), statusValue);
                    }
                }
            } catch (ExecutionException e) {
                LOGGER.error("Error in collection of data", e);
            }
        }
        samples.add(powerSamples);
        samples.add(fanSamples);
        samples.add(tempSamples);
        samples.add(diskSamples);
        return samples;
    }

}
