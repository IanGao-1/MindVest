package com.reserio.financialmanagement.dto;

import java.util.Date;

public class AssetDTO {
    private Long id;
    private String ticker;
    private String name;
    private String type;
    private Double quantity;
    private Double avgCost;
    private Date purchaseDate;
    private Double currentPrice;
    private Date lastUpdated;
    private String notes;
    private Double costBasis;
    private Double currentValue;
    private Double unrealizedPnL;
    private Double unrealizedPnLRate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Double getAvgCost() {
        return avgCost;
    }

    public void setAvgCost(Double avgCost) {
        this.avgCost = avgCost;
    }

    public Date getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(Date purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public Double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(Double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Double getCostBasis() {
        return costBasis;
    }

    public void setCostBasis(Double costBasis) {
        this.costBasis = costBasis;
    }

    public Double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(Double currentValue) {
        this.currentValue = currentValue;
    }

    public Double getUnrealizedPnL() {
        return unrealizedPnL;
    }

    public void setUnrealizedPnL(Double unrealizedPnL) {
        this.unrealizedPnL = unrealizedPnL;
    }

    public Double getUnrealizedPnLRate() {
        return unrealizedPnLRate;
    }

    public void setUnrealizedPnLRate(Double unrealizedPnLRate) {
        this.unrealizedPnLRate = unrealizedPnLRate;
    }
}
