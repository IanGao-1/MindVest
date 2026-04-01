package com.reserio.financialmanagement.dto;

import java.util.List;

public class HoldingValueHistoryDTO {
    private String ticker;
    private String name;
    private List<ValueHistoryPointDTO> points;

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

    public List<ValueHistoryPointDTO> getPoints() {
        return points;
    }

    public void setPoints(List<ValueHistoryPointDTO> points) {
        this.points = points;
    }
}
