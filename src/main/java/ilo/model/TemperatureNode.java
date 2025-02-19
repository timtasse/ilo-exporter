package ilo.model;

import com.fasterxml.jackson.databind.JsonNode;
import ilo.Labels;

import java.util.Optional;

public class TemperatureNode {
    JsonNode node;

    public TemperatureNode(JsonNode jsonNode) {
        this.node = jsonNode;
    }

    public Double getValue() {
        return node.get("ReadingCelsius").asDouble();
    }

    public String getName() {
        return Optional.ofNullable(node.get("Name")).map(JsonNode::asText).orElse("");
    }

    public String getLabel() {
        return Labels.from(getName());
    }

    @Override
    public String toString() {
        return getLabel() + "=" + getValue();
    }
}
