package com.yabozkurt.n11bootcamp.stock.presentation.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ReserveRequest {

    @NotNull
    @Min(1)
    private Integer quantity;

    private String note;

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
