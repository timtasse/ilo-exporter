package ilo;

import com.fasterxml.jackson.databind.JsonNode;
import ilo.model.ArrayController;
import ilo.model.DiskNode;
import ilo.model.StorageNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class StorageClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageClient.class);

    private final IloHttpClient client;

    StorageClient(IloHttpClient client) {
        this.client = client;
    }

    public List<DiskNode> getDiskDrives(JsonNode disksJson) {
        var set = new ArrayList<DiskNode>();
        if (disksJson.get("Members") == null) {
            return set;
        }
        for (JsonNode member : disksJson.get("Members")) {
            var link = member.get("@odata.id").asText();
            var diskUri = client.getBaseUri().resolve(link);
            var diskJson = client.getJson(client.session().uri(diskUri).build());
            set.add(new DiskNode(diskJson));
        }
        return set;
    }

    public List<ArrayController> getArrays(JsonNode arrayControllers) {
        var arrays = new ArrayList<ArrayController>();
        if (arrayControllers.get("Members") == null) {
            return arrays;
        }
        for (JsonNode arrayMember : arrayControllers.get("Members")) {
            String arrayLink = arrayMember.get("@odata.id").asText();
            var disksUri = client.getBaseUri().resolve(arrayLink + "diskdrives/");
            var disksJson = client.getJson(client.session().uri(disksUri).build());
            LOGGER.trace("disks: {}", disksJson);
            arrays.add(new ArrayController(getDiskDrives(disksJson)));
        }
        return arrays;
    }

    public StorageNode getStorageNode() {
        URI arrayControllersUri = URI.create(client.getSystemUri().toString() + "SmartStorage/ArrayControllers/");
        JsonNode arraysJson = client.getJson(client.session().uri(arrayControllersUri).build());
        LOGGER.trace("arrays: {}", arraysJson);
        return new StorageNode(arraysJson, getArrays(arraysJson));
    }
}
