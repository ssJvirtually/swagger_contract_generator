package org.example.temp.model;

public class CR {

    public enum CRStatus {
        ACTIVE("ACTIVE"),
        INACTIVE("INACTIVE"),
        PENDING("PENDING");

        private String value;

        CRStatus(String value) {
            this.value = value;
        }
    }

    CRStatus crStatus;

    String crId;
}
