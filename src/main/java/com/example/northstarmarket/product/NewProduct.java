package com.example.northstarmarket.product;

import java.math.BigDecimal;

record NewProduct(String name, String description, BigDecimal price, boolean active) {
}
