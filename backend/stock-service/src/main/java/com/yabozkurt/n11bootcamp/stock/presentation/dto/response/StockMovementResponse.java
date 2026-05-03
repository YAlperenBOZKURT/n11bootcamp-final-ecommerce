package com.yabozkurt.n11bootcamp.stock.presentation.dto.response;

import com.yabozkurt.n11bootcamp.stock.domain.model.enums.MovementType;

import java.time.LocalDateTime;

public class StockMovementResponse {

    private Long id;
    private MovementType type;
    private int quantity;
    private String note;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MovementType getType() { return type; }
    public void setType(MovementType type) { this.type = type; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
