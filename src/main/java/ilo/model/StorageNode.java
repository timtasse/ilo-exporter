package ilo.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public class StorageNode {

    private JsonNode jsonNode;

    private List<ArrayController> arrays;

    public StorageNode(JsonNode node, List<ArrayController> arrays) {
        this.jsonNode = node;
        this.arrays = arrays;
    }

    public List<ArrayController> getArrays() {
        return arrays;
    }

    public JsonNode toJson() {
        return jsonNode;
    }

    public String toString() {
        return arrays.toString();
    }
}
