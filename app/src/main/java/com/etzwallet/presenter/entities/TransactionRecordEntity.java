package com.etzwallet.presenter.entities;

import com.etzwallet.tools.util.Utils;

public class TransactionRecordEntity {
    private String blockHash;
    private String blockNumber;
    private String from;
    private String gas;
    private String gasPrice;
    private String hash;
    private String input;
    private String nonce;
    private String to;
    private String transactionIndex;
    private String value;
    private String timestamp;
    private String gasUsed;
    private String contractAddress;
    private String status;
    private String confirmations;
    private String isError;
    private String iso;

    public TransactionRecordEntity(String blockHash, String blockNumber, String from, String gas, String gasPrice, String hash,
                                   String input, String nonce, String to, String transactionIndex, String value, String timestamp,
                                   String gasUsed, String contractAddress, String status, String confirmations, String isError, String iso) {
        this.blockHash = blockHash;
        this.blockNumber = blockNumber;
        this.from = from;
        this.gas = gas;
        this.gasPrice = gasPrice;
        this.hash = hash;
        this.input = input;
        this.nonce = nonce;
        this.to = to;
        this.transactionIndex = transactionIndex;
        this.value = value;
        this.timestamp = timestamp;
        this.gasUsed = gasUsed;
        this.contractAddress = contractAddress;
        this.status = status;
        this.confirmations = confirmations;
        this.isError = isError;
        this.iso=iso;
    }

    public String getBlockHash() {
        if (!Utils.isNullOrEmpty(blockHash)){
            return blockHash;
        }else {
            return "";
        }

    }

    public String getInput() {
        if (!Utils.isNullOrEmpty(input)){
            return input;
        }else {
            return "0x";
        }

    }

    public String getConfirmations() {
        if (!Utils.isNullOrEmpty(confirmations)){
            return confirmations;
        }else {
            return "0";
        }

    }

    public String getBlockNumber() {
        if (!Utils.isNullOrEmpty(blockNumber)){
            return blockNumber;
        }else {
            return "0";
        }

    }

    public String getFrom() {
        if (!Utils.isNullOrEmpty(from)){
            return from;
        }else {
            return "";
        }

    }

    public String getGas() {
        if (!Utils.isNullOrEmpty(gas)){
            return gas;
        }else {
            return "0";
        }

    }

    public String getGasPrice() {
        if (!Utils.isNullOrEmpty(gasPrice)){
            return gasPrice;
        }else {
            return "0";
        }
    }

    public String getHash() {
        if (!Utils.isNullOrEmpty(hash)){
            return hash;
        }else {
            return "0";
        }

    }

    public String getNonce() {
        if (!Utils.isNullOrEmpty(nonce)){
            return nonce;
        }else {
            return "0";
        }

    }

    public String getTo() {
        if (!Utils.isNullOrEmpty(to)){
            return to;
        }else {
            return "";
        }

    }

    public String getTransactionIndex() {
        if (!Utils.isNullOrEmpty(transactionIndex)){
            return transactionIndex;
        }else {
            return "0";
        }

    }

    public String getValue() {
        if (!Utils.isNullOrEmpty(value)){
            return value;
        }else {
            return "0";
        }

    }

    public String getTimestamp() {
        if (!Utils.isNullOrEmpty(timestamp)){
            return timestamp;
        }else {
            return "";
        }
    }

    public String getGasUsed() {
        if (!Utils.isNullOrEmpty(gasUsed)){
            return gasUsed;
        }else {
            return "0";
        }

    }

    public String getContractAddress() {
        if (!Utils.isNullOrEmpty(contractAddress)){
            return contractAddress;
        }else {
            return "0";
        }

    }

    public String getStatus() {
        if (!Utils.isNullOrEmpty(status)){
            return status;
        }else {
            return "";
        }

    }

    public String getIsError() {
        if (!Utils.isNullOrEmpty(isError)){
            return isError;
        }else {
            return "0";
        }
    }
    public String getIso(){
        if (!Utils.isNullOrEmpty(iso)){
            return iso;
        }else {
            return "";
        }

    }
}
