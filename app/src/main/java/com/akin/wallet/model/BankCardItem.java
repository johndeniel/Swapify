package com.akin.wallet.model;

public class BankCardItem {
    private final int id;
    private final String cardType;
    private final String cardNetwork;
    private final String bankName;
    private final String holderName;
    private final String cardNumber;
    private final String expiry;
    private final String cvv;
    private final String pin;
    private final int design;
    // Epoch millis (UTC). 0 = unknown (pre-migration rows or unsaved drafts);
    // the DB fills real values on insert.
    private final long createdAt;
    private final long updatedAt;

    public BankCardItem(String cardType, String cardNetwork, String bankName, String holderName,
                        String cardNumber, String expiry, String cvv, String pin, int design) {
        this(cardType, cardNetwork, bankName, holderName,
                cardNumber, expiry, cvv, pin, design, 0, 0);
    }

    public BankCardItem(int id, String cardType, String cardNetwork, String bankName, String holderName,
                        String cardNumber, String expiry, String cvv, String pin, int design) {
        this(id, cardType, cardNetwork, bankName, holderName,
                cardNumber, expiry, cvv, pin, design, 0, 0);
    }

    public BankCardItem(String cardType, String cardNetwork, String bankName, String holderName,
                        String cardNumber, String expiry, String cvv, String pin, int design,
                        long createdAt, long updatedAt) {
        this.id = -1;
        this.cardType = cardType;
        this.cardNetwork = cardNetwork;
        this.bankName = bankName;
        this.holderName = holderName;
        this.cardNumber = cardNumber;
        this.expiry = expiry;
        this.cvv = cvv;
        this.pin = pin;
        this.design = design;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public BankCardItem(int id, String cardType, String cardNetwork, String bankName, String holderName,
                        String cardNumber, String expiry, String cvv, String pin, int design,
                        long createdAt, long updatedAt) {
        this.id = id;
        this.cardType = cardType;
        this.cardNetwork = cardNetwork;
        this.bankName = bankName;
        this.holderName = holderName;
        this.cardNumber = cardNumber;
        this.expiry = expiry;
        this.cvv = cvv;
        this.pin = pin;
        this.design = design;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getId() {
        return id;
    }

    public String getCardType() {
        return cardType;
    }

    public String getCardNetwork() {
        return cardNetwork;
    }

    public String getBankName() {
        return bankName;
    }

    public String getHolderName() {
        return holderName;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getExpiry() {
        return expiry;
    }

    public String getCvv() {
        return cvv;
    }

    public String getPin() {
        return pin;
    }

    public int getDesign() {
        return design;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
