package com.etzwallet.presenter.entities;

import com.etzwallet.tools.util.Utils;

public class ContactsEntity {
    private String cname;
    private String walletAddress;
    private String cphone;
    private String remarks;

    public String getCname() {
        if (!Utils.isNullOrEmpty(cname)) {
            return cname;
        } else {
            return "";
        }

    }

    public String getCphone() {
        if (!Utils.isNullOrEmpty(cphone)) {
            return cphone;
        } else {
            return "";
        }

    }

    public String getRemarks() {
        if (!Utils.isNullOrEmpty(remarks)) {
            return remarks;
        } else {
            return "";
        }

    }

    public String getWalletAddress() {
        if (!Utils.isNullOrEmpty(walletAddress)) {
            return walletAddress;
        } else {
            return "";
        }

    }

    public ContactsEntity(String cname, String walletAddress, String cphone, String remarks) {
        this.cname = cname;
        this.walletAddress = walletAddress;
        this.cphone = cphone;
        this.remarks = remarks;
    }
}
