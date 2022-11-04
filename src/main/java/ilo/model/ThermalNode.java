package ilo.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashSet;
import java.util.Set;

public class ThermalNode {
    JsonNode node;

    public ThermalNode(JsonNode node) {
        this.node = node;
    }

    public boolean isOldVersion() {
        final String version = node.get("@odata.type").asText();
        return "#Thermal.1.2.0.Thermal".equals(version);
    }
    public Set<FanNode> getFans() {
        final Set<FanNode> fans = new LinkedHashSet<>();
        for (JsonNode node : node.get("Fans")) {
            fans.add(new FanNode(node, this.isOldVersion()));
        }
        return fans;
    }

    public Set<TemperatureNode> getTempuratures() {
        final var temps = new LinkedHashSet<TemperatureNode>();
        for (JsonNode node : node.get("Temperatures")) {
            temps.add(new TemperatureNode(node));
        }
        return temps;
    }

}
