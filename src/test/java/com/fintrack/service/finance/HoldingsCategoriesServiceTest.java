package com.fintrack.service.finance;

import com.fintrack.model.finance.Category;
import com.fintrack.repository.finance.HoldingsCategoriesRepository;
import com.fintrack.repository.finance.CategoriesRepository;
import com.fintrack.repository.finance.SubcategoriesRepository;
import com.fintrack.repository.finance.AssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HoldingsCategoriesService Tests")
class HoldingsCategoriesServiceTest {

    @Mock
    private HoldingsCategoriesRepository holdingsCategoriesRepository;
    
    @Mock
    private CategoriesRepository categoriesRepository;
    
    @Mock
    private SubcategoriesRepository subcategoriesRepository;
    
    @Mock
    private AssetRepository assetRepository;

    private HoldingsCategoriesService holdingsCategoriesService;
    private UUID testAccountId;

    @BeforeEach
    void setUp() {
        holdingsCategoriesService = new HoldingsCategoriesService(
            holdingsCategoriesRepository,
            categoriesRepository,
            subcategoriesRepository,
            assetRepository
        );
        testAccountId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should update holdings categories successfully with existing category")
    void shouldUpdateHoldingsCategoriesSuccessfullyWithExistingCategory() {
        // Given: Holdings categories data with existing category
        Map<String, Map<String, String>> holdingsCategories = createSampleHoldingsCategories();
        when(categoriesRepository.findCategoryIdByAccountIdAndCategoryName(testAccountId, "Technology"))
            .thenReturn(1);
        when(categoriesRepository.findCategoryIdByAccountIdAndCategoryName(testAccountId, "Software"))
            .thenReturn(10);

        // When: Updating holdings categories
        holdingsCategoriesService.updateHoldingsCategories(testAccountId, holdingsCategories);

        // Then: Should upsert holdings categories
        verify(categoriesRepository).findCategoryIdByAccountIdAndCategoryName(testAccountId, "Technology");
        verify(categoriesRepository).findCategoryIdByAccountIdAndCategoryName(testAccountId, "Software");
        verify(holdingsCategoriesRepository).upsertHoldingCategory(testAccountId, "AAPL", 10, "Technology", "Software");
        verify(holdingsCategoriesRepository).upsertHoldingCategory(testAccountId, "GOOGL", 1, "Technology", null);
    }

    @Test
    @DisplayName("Should update holdings categories successfully with new category")
    void shouldUpdateHoldingsCategoriesSuccessfullyWithNewCategory() {
        // Given: Holdings categories data with new category
        Map<String, Map<String, String>> holdingsCategories = createSampleHoldingsCategories();
        when(categoriesRepository.findCategoryIdByAccountIdAndCategoryName(testAccountId, "Technology"))
            .thenReturn(null);
        when(categoriesRepository.insertCategory(testAccountId, "Technology", null, 1, 1))
            .thenReturn(1);
        when(categoriesRepository.findCategoryIdByAccountIdAndCategoryName(testAccountId, "Software"))
            .thenReturn(null);
        when(categoriesRepository.insertCategory(testAccountId, "Software", 1, 2, 1))
            .thenReturn(10);

        // When: Updating holdings categories
        holdingsCategoriesService.updateHoldingsCategories(testAccountId, holdingsCategories);

        // Then: Should create new categories and upsert holdings categories
        verify(categoriesRepository).findCategoryIdByAccountIdAndCategoryName(testAccountId, "Technology");
        verify(categoriesRepository).insertCategory(testAccountId, "Technology", null, 1, 1);
        verify(categoriesRepository).findCategoryIdByAccountIdAndCategoryName(testAccountId, "Software");
        verify(categoriesRepository).insertCategory(testAccountId, "Software", 1, 2, 1);
        verify(holdingsCategoriesRepository).upsertHoldingCategory(testAccountId, "AAPL", 10, "Technology", "Software");
        verify(holdingsCategoriesRepository).upsertHoldingCategory(testAccountId, "GOOGL", 1, "Technology", null);
    }

    @Test
    @DisplayName("Should throw exception when updating with null category name")
    void shouldThrowExceptionWhenUpdatingWithNullCategoryName() {
        // Given: Holdings categories with null category name
        Map<String, Map<String, String>> holdingsCategories = new HashMap<>();
        holdingsCategories.put(null, new HashMap<>());

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            holdingsCategoriesService.updateHoldingsCategories(testAccountId, holdingsCategories);
        });
    }

    @Test
    @DisplayName("Should throw exception when updating with empty category name")
    void shouldThrowExceptionWhenUpdatingWithEmptyCategoryName() {
        // Given: Holdings categories with empty category name
        Map<String, Map<String, String>> holdingsCategories = new HashMap<>();
        holdingsCategories.put("", new HashMap<>());

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            holdingsCategoriesService.updateHoldingsCategories(testAccountId, holdingsCategories);
        });
    }

    @Test
    @DisplayName("Should throw exception when updating with null asset name")
    void shouldThrowExceptionWhenUpdatingWithNullAssetName() {
        // Given: Holdings categories with null asset name
        Map<String, Map<String, String>> holdingsCategories = new HashMap<>();
        Map<String, String> assets = new HashMap<>();
        assets.put(null, "Software");
        holdingsCategories.put("Technology", assets);
        when(categoriesRepository.findCategoryIdByAccountIdAndCategoryName(testAccountId, "Technology"))
            .thenReturn(1);

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            holdingsCategoriesService.updateHoldingsCategories(testAccountId, holdingsCategories);
        });
    }

    @Test
    @DisplayName("Should add holdings categories successfully")
    void shouldAddHoldingsCategoriesSuccessfully() {
        // Given: Holdings categories data
        Map<String, Map<String, String>> holdingsCategories = createSampleHoldingsCategories();
        when(categoriesRepository.findCategoryIdByAccountIdAndCategoryName(testAccountId, "Technology"))
            .thenReturn(null);
        when(categoriesRepository.insertCategory(testAccountId, "Technology", null, 1, 1))
            .thenReturn(1);
        when(categoriesRepository.findCategoryIdByAccountIdAndCategoryName(testAccountId, "Software"))
            .thenReturn(null);
        when(categoriesRepository.insertCategory(testAccountId, "Software", 1, 2, 1))
            .thenReturn(10);

        // When: Adding holdings categories
        holdingsCategoriesService.addHoldingsCategories(testAccountId, holdingsCategories);

        // Then: Should create categories and insert holdings categories
        verify(categoriesRepository).findCategoryIdByAccountIdAndCategoryName(testAccountId, "Technology");
        verify(categoriesRepository).insertCategory(testAccountId, "Technology", null, 1, 1);
        verify(categoriesRepository).findCategoryIdByAccountIdAndCategoryName(testAccountId, "Software");
        verify(categoriesRepository).insertCategory(testAccountId, "Software", 1, 2, 1);
        verify(holdingsCategoriesRepository).insertHoldingCategory(testAccountId, "AAPL", 10, "Technology", "Software");
        verify(holdingsCategoriesRepository).insertHoldingCategory(testAccountId, "GOOGL", 1, "Technology", null);
    }

    @Test
    @DisplayName("Should throw exception when adding with null category name")
    void shouldThrowExceptionWhenAddingWithNullCategoryName() {
        // Given: Holdings categories with null category name
        Map<String, Map<String, String>> holdingsCategories = new HashMap<>();
        holdingsCategories.put(null, new HashMap<>());

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            holdingsCategoriesService.addHoldingsCategories(testAccountId, holdingsCategories);
        });
    }

    @Test
    @DisplayName("Should throw exception when adding with empty category name")
    void shouldThrowExceptionWhenAddingWithEmptyCategoryName() {
        // Given: Holdings categories with empty category name
        Map<String, Map<String, String>> holdingsCategories = new HashMap<>();
        holdingsCategories.put("", new HashMap<>());

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            holdingsCategoriesService.addHoldingsCategories(testAccountId, holdingsCategories);
        });
    }

    @Test
    @DisplayName("Should throw exception when adding with null asset name")
    void shouldThrowExceptionWhenAddingWithNullAssetName() {
        // Given: Holdings categories with null asset name
        Map<String, Map<String, String>> holdingsCategories = new HashMap<>();
        Map<String, String> assets = new HashMap<>();
        assets.put(null, "Software");
        holdingsCategories.put("Technology", assets);
        when(categoriesRepository.findCategoryIdByAccountIdAndCategoryName(testAccountId, "Technology"))
            .thenReturn(1);

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            holdingsCategoriesService.addHoldingsCategories(testAccountId, holdingsCategories);
        });
    }

    @Test
    @DisplayName("Should remove holdings category successfully")
    void shouldRemoveHoldingsCategorySuccessfully() {
        // Given: Category name to remove
        String categoryName = "Technology";

        // When: Removing holdings category
        holdingsCategoriesService.removeHoldingsCategory(testAccountId, categoryName);

        // Then: Should delete holdings category
        verify(holdingsCategoriesRepository).deleteByAccountIdAndCategory(testAccountId, categoryName);
    }

    @Test
    @DisplayName("Should throw exception when removing with null category name")
    void shouldThrowExceptionWhenRemovingWithNullCategoryName() {
        // Given: Null category name
        String categoryName = null;

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            holdingsCategoriesService.removeHoldingsCategory(testAccountId, categoryName);
        });
    }

    @Test
    @DisplayName("Should throw exception when removing with empty category name")
    void shouldThrowExceptionWhenRemovingWithEmptyCategoryName() {
        // Given: Empty category name
        String categoryName = "";

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            holdingsCategoriesService.removeHoldingsCategory(testAccountId, categoryName);
        });
    }

    @Test
    @DisplayName("Should fetch holdings categories successfully")
    void shouldFetchHoldingsCategoriesSuccessfully() {
        // Given: Categories and subcategories with holdings data
        List<Category> categories = createSampleCategories();
        List<Category> subcategories = createSampleSubcategories();
        List<Map<String, Object>> holdingsData = createSampleHoldingsData();
        
        when(categoriesRepository.findCategoriesByAccountIdOrderedByPriority(testAccountId))
            .thenReturn(categories);
        when(subcategoriesRepository.findSubcategoriesByParentId(testAccountId, 1))
            .thenReturn(subcategories);
        when(holdingsCategoriesRepository.findHoldingsByCategoryId(testAccountId, 10))
            .thenReturn(holdingsData);

        // When: Fetching holdings categories
        Map<String, Map<String, String>> result = holdingsCategoriesService.fetchHoldingsCategories(testAccountId);

        // Then: Should return properly structured map
        assertNotNull(result);
        assertTrue(result.containsKey("Technology"));
        
        Map<String, String> technologyAssets = result.get("Technology");
        assertNotNull(technologyAssets);
        assertEquals("Software", technologyAssets.get("AAPL"));
        assertEquals("Hardware", technologyAssets.get("GOOGL"));
        
        verify(categoriesRepository).findCategoriesByAccountIdOrderedByPriority(testAccountId);
        verify(subcategoriesRepository).findSubcategoriesByParentId(testAccountId, 1);
        verify(holdingsCategoriesRepository).findHoldingsByCategoryId(testAccountId, 10);
    }

    @Test
    @DisplayName("Should cleanup orphaned holdings categories successfully")
    void shouldCleanupOrphanedHoldingsCategoriesSuccessfully() {
        // Given: Orphaned holdings categories to clean up
        when(holdingsCategoriesRepository.deleteOrphanedHoldingsCategories(testAccountId))
            .thenReturn(5);

        // When: Cleaning up orphaned holdings categories
        holdingsCategoriesService.cleanupOrphanedHoldingsCategories(testAccountId);

        // Then: Should delete orphaned holdings categories
        verify(holdingsCategoriesRepository).deleteOrphanedHoldingsCategories(testAccountId);
    }

    @Test
    @DisplayName("Should cleanup orphaned holdings categories for specific assets successfully")
    void shouldCleanupOrphanedHoldingsCategoriesForAssetsSuccessfully() {
        // Given: Asset names and orphaned holdings categories to clean up
        List<String> assetNames = Arrays.asList("AAPL", "GOOGL");
        when(holdingsCategoriesRepository.deleteOrphanedHoldingsCategoriesForAssets(testAccountId, assetNames))
            .thenReturn(3);

        // When: Cleaning up orphaned holdings categories for specific assets
        holdingsCategoriesService.cleanupOrphanedHoldingsCategoriesForAssets(testAccountId, assetNames);

        // Then: Should delete orphaned holdings categories for specific assets
        verify(holdingsCategoriesRepository).deleteOrphanedHoldingsCategoriesForAssets(testAccountId, assetNames);
    }

    @Test
    @DisplayName("Should handle cleanup orphaned holdings categories for empty asset list")
    void shouldHandleCleanupOrphanedHoldingsCategoriesForEmptyAssetList() {
        // Given: Empty asset names list
        List<String> assetNames = new ArrayList<>();

        // When: Cleaning up orphaned holdings categories for empty asset list
        holdingsCategoriesService.cleanupOrphanedHoldingsCategoriesForAssets(testAccountId, assetNames);

        // Then: Should not call repository method
        verify(holdingsCategoriesRepository, never()).deleteOrphanedHoldingsCategoriesForAssets(any(), any());
    }

    @Test
    @DisplayName("Should handle cleanup orphaned holdings categories for null asset list")
    void shouldHandleCleanupOrphanedHoldingsCategoriesForNullAssetList() {
        // Given: Null asset names list
        List<String> assetNames = null;

        // When: Cleaning up orphaned holdings categories for null asset list
        holdingsCategoriesService.cleanupOrphanedHoldingsCategoriesForAssets(testAccountId, assetNames);

        // Then: Should not call repository method
        verify(holdingsCategoriesRepository, never()).deleteOrphanedHoldingsCategoriesForAssets(any(), any());
    }

    @Test
    @DisplayName("Should cleanup orphaned assets successfully")
    void shouldCleanupOrphanedAssetsSuccessfully() {
        // Given: Orphaned assets to clean up
        when(assetRepository.deleteOrphanedAssets(testAccountId))
            .thenReturn(2);

        // When: Cleaning up orphaned assets
        holdingsCategoriesService.cleanupOrphanedAssets(testAccountId);

        // Then: Should delete orphaned assets
        verify(assetRepository).deleteOrphanedAssets(testAccountId);
    }

    @Test
    @DisplayName("Should handle cleanup with no orphaned data")
    void shouldHandleCleanupWithNoOrphanedData() {
        // Given: No orphaned data to clean up
        when(holdingsCategoriesRepository.deleteOrphanedHoldingsCategories(testAccountId))
            .thenReturn(0);
        when(assetRepository.deleteOrphanedAssets(testAccountId))
            .thenReturn(0);

        // When: Cleaning up with no orphaned data
        holdingsCategoriesService.cleanupOrphanedHoldingsCategories(testAccountId);
        holdingsCategoriesService.cleanupOrphanedAssets(testAccountId);

        // Then: Should call repository methods but return 0
        verify(holdingsCategoriesRepository).deleteOrphanedHoldingsCategories(testAccountId);
        verify(assetRepository).deleteOrphanedAssets(testAccountId);
    }

    // Helper methods to create test data
    private Map<String, Map<String, String>> createSampleHoldingsCategories() {
        Map<String, Map<String, String>> holdingsCategories = new HashMap<>();
        
        Map<String, String> technologyAssets = new HashMap<>();
        technologyAssets.put("AAPL", "Software");
        technologyAssets.put("GOOGL", null);
        holdingsCategories.put("Technology", technologyAssets);
        
        return holdingsCategories;
    }

    private List<Category> createSampleCategories() {
        List<Category> categories = new ArrayList<>();
        
        Category technologyCategory = new Category();
        technologyCategory.setCategoryId(1);
        technologyCategory.setAccountId(testAccountId);
        technologyCategory.setCategoryName("Technology");
        technologyCategory.setParentId(null);
        categories.add(technologyCategory);
        
        return categories;
    }

    private List<Category> createSampleSubcategories() {
        List<Category> subcategories = new ArrayList<>();
        
        Category softwareSubcategory = new Category();
        softwareSubcategory.setCategoryId(10);
        softwareSubcategory.setAccountId(testAccountId);
        softwareSubcategory.setCategoryName("Software");
        softwareSubcategory.setParentId(1);
        subcategories.add(softwareSubcategory);
        
        Category hardwareSubcategory = new Category();
        hardwareSubcategory.setCategoryId(11);
        hardwareSubcategory.setAccountId(testAccountId);
        hardwareSubcategory.setCategoryName("Hardware");
        hardwareSubcategory.setParentId(1);
        subcategories.add(hardwareSubcategory);
        
        return subcategories;
    }

    private List<Map<String, Object>> createSampleHoldingsData() {
        List<Map<String, Object>> holdingsData = new ArrayList<>();
        
        Map<String, Object> holding1 = new HashMap<>();
        holding1.put("asset_name", "AAPL");
        holding1.put("subcategory", "Software");
        holdingsData.add(holding1);
        
        Map<String, Object> holding2 = new HashMap<>();
        holding2.put("asset_name", "GOOGL");
        holding2.put("subcategory", "Hardware");
        holdingsData.add(holding2);
        
        return holdingsData;
    }
} 