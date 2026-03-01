package com.chuanyi.backend.modules.component.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

@TableName("component_sku")
public class ComponentSkuEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String skuCode;
    private Long categoryId;
    private String name;
    private String specText;
    private BigDecimal beadDiameterMm;
    private BigDecimal price;
    private Integer stockQty;
    private Integer stockWarnQty;
    private String salesStatus;
    private String imageUrl;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecText() {
        return specText;
    }

    public void setSpecText(String specText) {
        this.specText = specText;
    }

    public BigDecimal getBeadDiameterMm() {
        return beadDiameterMm;
    }

    public void setBeadDiameterMm(BigDecimal beadDiameterMm) {
        this.beadDiameterMm = beadDiameterMm;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStockQty() {
        return stockQty;
    }

    public void setStockQty(Integer stockQty) {
        this.stockQty = stockQty;
    }

    public Integer getStockWarnQty() {
        return stockWarnQty;
    }

    public void setStockWarnQty(Integer stockWarnQty) {
        this.stockWarnQty = stockWarnQty;
    }

    public String getSalesStatus() {
        return salesStatus;
    }

    public void setSalesStatus(String salesStatus) {
        this.salesStatus = salesStatus;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
