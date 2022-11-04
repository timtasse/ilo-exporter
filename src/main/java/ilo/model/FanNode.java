package ilo.model;

import com.fasterxml.jackson.databind.JsonNode;
import ilo.Labels;

public class FanNode {
    JsonNode node;
    final boolean old;

    public FanNode(JsonNode jsonNode, final boolean oldVersion) {
        this.node = jsonNode;
        this.old = oldVersion;
    }

    public Double getValue() {
        if (this.old) {
            return node.get("CurrentReading").asDouble();
        }
        return node.get("Reading").asDouble();
    }

    public String getName() {
        if (this.old) {
            return node.get("FanName").asText();
        }
        return node.get("Name").asText();
    }

    public String getLabel() {
        return Labels.from(getName());
    }

    @Override
    public String toString() {
        return getLabel() + "=" + getValue();
    }
}
