package com.fintrack.constants.finance;

public enum AssetType {
    STOCK("STOCK"),
    CRYPTO("CRYPTO"),
    COMMODITY("COMMODITY"),
    FOREX("FOREX"),
    UNKNOWN("UNKNOWN");

    private final String assetTypeName;

    AssetType(String assetTypeName) {
        this.assetTypeName = assetTypeName;
    }

    public String getAssetTypeName() {
        return assetTypeName;
    }
}