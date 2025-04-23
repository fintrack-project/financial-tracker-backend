package com.fintrack.constants;

public enum AssetType {
    STOCK("stock"),
    CRYPTO("crypto"),
    COMMODITY("commodity"),
    FOREX("forex"),
    UNKNOWN("unknown");
    
    private final String assetTypeName;

    AssetType(String assetTypeName) {
        this.assetTypeName = assetTypeName;
    }

    public String getAssetTypeName() {
        return assetTypeName;
    }
}