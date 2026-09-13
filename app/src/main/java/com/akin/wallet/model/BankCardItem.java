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

    public BankCardItem(String cardType, String cardNetwork, String bankName, String holderName,
                        String cardNumber, String expiry, String cvv, String pin, int design) {
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
    }

    public BankCardItem(int id, String cardType, String cardNetwork, String bankName, String holderName,
                        String cardNumber, String expiry, String cvv, String pin, int design) {
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
}
