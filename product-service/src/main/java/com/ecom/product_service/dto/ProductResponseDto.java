package com.ecom.product_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponseDto {
    private String productId;
    private String name;
    private String description;
    private Double price;
    private Integer stockQuantity;
    private Boolean inStock;
    private String categoryName;


}
