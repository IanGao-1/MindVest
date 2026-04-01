package com.reserio.financialmanagement.dto;

public class ValueHistoryPointDTO {
    private String timestamp;
    private Long timestampValue;
    private Double quantity;
    private Double price;
    private Double costBasis;
    private Double value;
    private Double pnl;
    private Double pnlRate;

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Long getTimestampValue() {
        return timestampValue;
    }

    public void setTimestampValue(Long timestampValue) {
        this.timestampValue = timestampValue;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Double getCostBasis() {
        return costBasis;
    }

    public void setCostBasis(Double costBasis) {
        this.costBasis = costBasis;
    }

    public Double getPnl() {
        return pnl;
    }

    public void setPnl(Double pnl) {
        this.pnl = pnl;
    }

    public Double getPnlRate() {
        return pnlRate;
    }

    public void setPnlRate(Double pnlRate) {
        this.pnlRate = pnlRate;
    }
}
