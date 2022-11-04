package ilo.model;

import com.fasterxml.jackson.databind.JsonNode;

public class PowerNode {
    JsonNode node;

    public PowerNode(JsonNode jsonNode) {
        this.node = jsonNode;
    }

    public Double getValue() {
        return node.get("PowerControl").get(0).get("PowerConsumedWatts").asDouble();
    }

    @Override
    public String toString() {
        return "Power=" + getValue();

    }
}
