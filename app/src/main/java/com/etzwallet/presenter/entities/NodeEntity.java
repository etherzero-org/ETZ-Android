package com.etzwallet.presenter.entities;

public class NodeEntity {
    private String nodeAddress;
    private String node;

    public NodeEntity(String nodeAddress, String node) {
        this.nodeAddress = nodeAddress;
        this.node = node;
    }

    public String getNodeAddress() {
        return nodeAddress;
    }

    public String getNode() {
        return node;
    }

}
