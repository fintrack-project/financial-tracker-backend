package com.fintrack.service;

import com.fintrack.model.Asset;
import com.fintrack.repository.AssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AssetService {

    private final AssetRepository assetRepository;

    @Autowired
    public AssetService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    public List<Asset> getAllAssets() {
        return assetRepository.findAll();
    }

    public Optional<Asset> getAssetById(Long id) {
        return assetRepository.findById(id);
    }

    public Asset createAsset(Asset asset) {
        return assetRepository.save(asset);
    }

    public Asset updateAsset(Long id, Asset assetDetails) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found with id " + id));
        asset.setName(assetDetails.getName());
        asset.setValue(assetDetails.getValue());
        // Update other fields as necessary
        return assetRepository.save(asset);
    }

    public void deleteAsset(Long id) {
        assetRepository.deleteById(id);
    }
}