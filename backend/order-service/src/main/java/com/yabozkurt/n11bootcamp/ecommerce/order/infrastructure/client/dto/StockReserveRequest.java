package com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client.dto;

public class StockReserveRequest {
    private Integer quantity;
    private String note;

    public StockReserveRequest() {}

    public StockReserveRequest(int quantity, String note) {
        this.quantity = quantity;
        this.note = note;
    }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
