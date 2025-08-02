package com.fintrack.service.finance;

import com.fintrack.model.finance.Category;
import com.fintrack.repository.finance.CategoriesRepository;
import com.fintrack.repository.finance.SubcategoriesRepository;
import com.fintrack.repository.finance.HoldingsCategoriesRepository;
import com.fintrack.constants.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoriesService Tests")
class CategoriesServiceTest {

    @Mock
    private CategoriesRepository categoriesRepository;
    
    @Mock
    private SubcategoriesRepository subcategoriesRepository;
    
    @Mock
    private HoldingsCategoriesRepository holdingsCategoriesRepository;

    private CategoriesService categoriesService;
    private UUID testAccountId;

    @BeforeEach
    void setUp() {
        categoriesService = new CategoriesService(
            categoriesRepository,
            subcategoriesRepository,
            holdingsCategoriesRepository
        );
        testAccountId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should add category successfully without color")
    void shouldAddCategorySuccessfullyWithoutColor() {
        // Given: Category name and no existing category with same name
        String categoryName = "Technology";
        when(categoriesRepository.findCategoryIdByAccountIdAndCategoryName(testAccountId, categoryName))
            .thenReturn(null);
        when(categoriesRepository.findMaxPriorityByAccountId(testAccountId))
            .thenReturn(5);
        when(categoriesRepository.insertCategory(testAccountId, categoryName, null, 1, 6))
            .thenReturn(1);

        // When: Adding a new category without color
        categoriesService.addCategory(testAccountId, categoryName);

        // Then: Should insert the category with calculated priority
        verify(categoriesRepository).findCategoryIdByAccountIdAndCategoryName(testAccountId, categoryName);
        verify(categoriesRepository).findMaxPriorityByAccountId(testAccountId);
        verify(categoriesRepository).insertCategory(testAccountId, categoryName, null, 1, 6);
    }

    @Test
    @DisplayName("Should add category successfully with color")
    void shouldAddCategorySuccessfullyWithColor() {
        // Given: Category name with valid color and no existing category
        String categoryName = "Technology";
        String hexCode = "#FF0000";
        when(categoriesRepository.findCategoryIdByAccountIdAndCategoryName(testAccountId, categoryName))
            .thenReturn(null);
        when(categoriesRepository.findMaxPriorityByAccountId(testAccountId))
            .thenReturn(0);
        when(categoriesRepository.insertCategory(testAccountId, categoryName, null, 1, 1))
            .thenReturn(1);

        // When: Adding a new category with color
        categoriesService.addCategory(testAccountId, categoryName, hexCode);

        // Then: Should insert the category and update its color
        verify(categoriesRepository).findCategoryIdByAccountIdAndCategoryName(testAccountId, categoryName);
        verify(categoriesRepository).findMaxPriorityByAccountId(testAccountId);
        verify(categoriesRepository).insertCategory(testAccountId, categoryName, null, 1, 1);
        verify(categoriesRepository).updateCategoryColor(testAccountId, 1, hexCode.toUpperCase());
    }

    @Test
    @DisplayName("Should throw exception when adding category with empty name")
    void shouldThrowExceptionWhenAddingCategoryWithEmptyName() {
        // Given: Empty category name
        String categoryName = "";

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            categoriesService.addCategory(testAccountId, categoryName);
        });
    }

    @Test
    @DisplayName("Should throw exception when adding category with null name")
    void shouldThrowExceptionWhenAddingCategoryWithNullName() {
        // Given: Null category name
        String categoryName = null;

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            categoriesService.addCategory(testAccountId, categoryName);
        });
    }

    @Test
    @DisplayName("Should throw exception when adding duplicate category")
    void shouldThrowExceptionWhenAddingDuplicateCategory() {
        // Given: Category name that already exists
        String categoryName = "Technology";
        when(categoriesRepository.findCategoryIdByAccountIdAndCategoryName(testAccountId, categoryName))
            .thenReturn(1);

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            categoriesService.addCategory(testAccountId, categoryName);
        });
    }

    @Test
    @DisplayName("Should throw exception when adding category with invalid color")
    void shouldThrowExceptionWhenAddingCategoryWithInvalidColor() {
        // Given: Category name with invalid color
        String categoryName = "Technology";
        String invalidHexCode = "#INVALID";

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            categoriesService.addCategory(testAccountId, categoryName, invalidHexCode);
        });
    }

    @Test
    @DisplayName("Should update category name successfully")
    void shouldUpdateCategoryNameSuccessfully() {
        // Given: Existing category and new name
        String oldCategoryName = "Technology";
        String newCategoryName = "Tech";
        when(categoriesRepository.findCategoryIdByAccountIdAndCategoryName(testAccountId, oldCategoryName))
            .thenReturn(1);

        // When: Updating category name
        categoriesService.updateCategoryName(testAccountId, oldCategoryName, newCategoryName);

        // Then: Should update the category name
        verify(categoriesRepository).findCategoryIdByAccountIdAndCategoryName(testAccountId, oldCategoryName);
        verify(categoriesRepository).updateCategoryName(testAccountId, 1, newCategoryName);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent category")
    void shouldThrowExceptionWhenUpdatingNonExistentCategory() {
        // Given: Non-existent category name
        String oldCategoryName = "NonExistent";
        String newCategoryName = "NewName";
        when(categoriesRepository.findCategoryIdByAccountIdAndCategoryName(testAccountId, oldCategoryName))
            .thenReturn(null);

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            categoriesService.updateCategoryName(testAccountId, oldCategoryName, newCategoryName);
        });
    }

    @Test
    @DisplayName("Should get category names successfully")
    void shouldGetCategoryNamesSuccessfully() {
        // Given: List of categories for the account
        List<Category> categories = createSampleCategories();
        when(categoriesRepository.findCategoriesByAccountId(testAccountId))
            .thenReturn(categories);

        // When: Getting category names
        List<String> result = categoriesService.getCategoryNames(testAccountId);

        // Then: Should return list of category names
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("Technology"));
        assertTrue(result.contains("Healthcare"));
        verify(categoriesRepository).findCategoriesByAccountId(testAccountId);
    }

    @Test
    @DisplayName("Should get categories and subcategories names map successfully")
    void shouldGetCategoriesAndSubcategoriesNamesMapSuccessfully() {
        // Given: Categories and their subcategories
        List<Category> categories = createSampleCategories();
        List<Category> techSubcategories = createSampleSubcategories("Technology");
        List<Category> healthSubcategories = createSampleSubcategories("Healthcare");
        
        when(categoriesRepository.findCategoriesByAccountId(testAccountId))
            .thenReturn(categories);
        when(subcategoriesRepository.findSubcategoriesByParentId(testAccountId, 1))
            .thenReturn(techSubcategories);
        when(subcategoriesRepository.findSubcategoriesByParentId(testAccountId, 2))
            .thenReturn(healthSubcategories);

        // When: Getting categories and subcategories map
        Map<String, Object> result = categoriesService.getCategoriesAndSubcategoriesNamesMap(testAccountId);

        // Then: Should return properly structured map
        assertNotNull(result);
        assertTrue(result.containsKey("categories"));
        assertTrue(result.containsKey("subcategories"));
        
        @SuppressWarnings("unchecked")
        List<String> categoryNames = (List<String>) result.get("categories");
        assertEquals(2, categoryNames.size());
        
        @SuppressWarnings("unchecked")
        Map<String, List<String>> subcategoriesMap = (Map<String, List<String>>) result.get("subcategories");
        assertEquals(2, subcategoriesMap.size());
    }

    @Test
    @DisplayName("Should remove category successfully")
    void shouldRemoveCategorySuccessfully() {
        // Given: Existing category to remove
        String categoryName = "Technology";
        List<Category> remainingCategories = createSampleCategories();
        remainingCategories.remove(0); // Remove first category
        
        when(categoriesRepository.findCategoryIdByAccountIdAndCategoryName(testAccountId, categoryName))
            .thenReturn(1);
        when(categoriesRepository.findCategoriesByAccountIdOrderedByPriority(testAccountId))
            .thenReturn(remainingCategories);

        // When: Removing the category
        categoriesService.removeCategory(testAccountId, categoryName);

        // Then: Should delete related data and reassign priorities
        verify(categoriesRepository).findCategoryIdByAccountIdAndCategoryName(testAccountId, categoryName);
        verify(holdingsCategoriesRepository).deleteByAccountIdAndCategoryId(testAccountId, 1);
        verify(subcategoriesRepository).deleteByParentId(testAccountId, 1);
        verify(categoriesRepository).deleteByCategoryId(1);
        verify(categoriesRepository).findCategoriesByAccountIdOrderedByPriority(testAccountId);
    }

    @Test
    @DisplayName("Should throw exception when removing non-existent category")
    void shouldThrowExceptionWhenRemovingNonExistentCategory() {
        // Given: Non-existent category name
        String categoryName = "NonExistent";
        when(categoriesRepository.findCategoryIdByAccountIdAndCategoryName(testAccountId, categoryName))
            .thenReturn(null);

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            categoriesService.removeCategory(testAccountId, categoryName);
        });
    }

    @Test
    @DisplayName("Should update category color successfully")
    void shouldUpdateCategoryColorSuccessfully() {
        // Given: Existing category and valid color
        String categoryName = "Technology";
        String hexCode = "#FF0000";
        when(categoriesRepository.findCategoryIdByAccountIdAndCategoryName(testAccountId, categoryName))
            .thenReturn(1);

        // When: Updating category color
        categoriesService.updateCategoryColor(testAccountId, categoryName, hexCode);

        // Then: Should update the category color
        verify(categoriesRepository).findCategoryIdByAccountIdAndCategoryName(testAccountId, categoryName);
        verify(categoriesRepository).updateCategoryColor(testAccountId, 1, hexCode.toUpperCase());
    }

    @Test
    @DisplayName("Should throw exception when updating color for non-existent category")
    void shouldThrowExceptionWhenUpdatingColorForNonExistentCategory() {
        // Given: Non-existent category name
        String categoryName = "NonExistent";
        String hexCode = "#FF0000";
        when(categoriesRepository.findCategoryIdByAccountIdAndCategoryName(testAccountId, categoryName))
            .thenReturn(null);

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            categoriesService.updateCategoryColor(testAccountId, categoryName, hexCode);
        });
    }

    @Test
    @DisplayName("Should throw exception when updating category with invalid color")
    void shouldThrowExceptionWhenUpdatingCategoryWithInvalidColor() {
        // Given: Existing category with invalid color
        String categoryName = "Technology";
        String invalidHexCode = "#INVALID";

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            categoriesService.updateCategoryColor(testAccountId, categoryName, invalidHexCode);
        });
    }

    @Test
    @DisplayName("Should get category color map successfully")
    void shouldGetCategoryColorMapSuccessfully() {
        // Given: Categories with colors
        List<Category> categories = createSampleCategoriesWithColors();
        when(categoriesRepository.findCategoriesByAccountId(testAccountId))
            .thenReturn(categories);

        // When: Getting category color map
        Map<String, Object> result = categoriesService.getCategoryColorMap(testAccountId);

        // Then: Should return map with category colors
        assertNotNull(result);
        assertTrue(result.containsKey("categoryColors"));
        
        @SuppressWarnings("unchecked")
        Map<String, String> categoryColors = (Map<String, String>) result.get("categoryColors");
        assertEquals(2, categoryColors.size());
        assertEquals("#FF0000", categoryColors.get("Technology"));
        assertEquals("#0000FF", categoryColors.get("Healthcare")); // Default color for null
    }

    @Test
    @DisplayName("Should handle data integrity violation when adding category")
    void shouldHandleDataIntegrityViolationWhenAddingCategory() {
        // Given: Category name that causes data integrity violation
        String categoryName = "Technology";
        when(categoriesRepository.findCategoryIdByAccountIdAndCategoryName(testAccountId, categoryName))
            .thenReturn(null);
        when(categoriesRepository.findMaxPriorityByAccountId(testAccountId))
            .thenReturn(0);
        when(categoriesRepository.insertCategory(testAccountId, categoryName, null, 1, 1))
            .thenThrow(new DataIntegrityViolationException("Constraint violation"));

        // When & Then: Should throw IllegalArgumentException with cause
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            categoriesService.addCategory(testAccountId, categoryName);
        });
        assertTrue(exception.getMessage().contains("Failed to insert category"));
    }

    // Helper methods to create test data
    private List<Category> createSampleCategories() {
        List<Category> categories = new ArrayList<>();
        
        Category techCategory = new Category();
        techCategory.setCategoryId(1);
        techCategory.setAccountId(testAccountId);
        techCategory.setCategoryName("Technology");
        techCategory.setColor("#FF0000");
        categories.add(techCategory);

        Category healthCategory = new Category();
        healthCategory.setCategoryId(2);
        healthCategory.setAccountId(testAccountId);
        healthCategory.setCategoryName("Healthcare");
        healthCategory.setColor("#00FF00");
        categories.add(healthCategory);

        return categories;
    }

    private List<Category> createSampleCategoriesWithColors() {
        List<Category> categories = new ArrayList<>();
        
        Category techCategory = new Category();
        techCategory.setCategoryId(1);
        techCategory.setAccountId(testAccountId);
        techCategory.setCategoryName("Technology");
        techCategory.setColor("#FF0000");
        categories.add(techCategory);

        Category healthCategory = new Category();
        healthCategory.setCategoryId(2);
        healthCategory.setAccountId(testAccountId);
        healthCategory.setCategoryName("Healthcare");
        healthCategory.setColor(null); // Will use default color
        categories.add(healthCategory);

        return categories;
    }

    private List<Category> createSampleSubcategories(String parentName) {
        List<Category> subcategories = new ArrayList<>();
        
        Category subcategory1 = new Category();
        subcategory1.setCategoryId(10);
        subcategory1.setAccountId(testAccountId);
        subcategory1.setCategoryName(parentName + " Sub 1");
        subcategory1.setParentId(1);
        subcategories.add(subcategory1);

        Category subcategory2 = new Category();
        subcategory2.setCategoryId(11);
        subcategory2.setAccountId(testAccountId);
        subcategory2.setCategoryName(parentName + " Sub 2");
        subcategory2.setParentId(1);
        subcategories.add(subcategory2);

        return subcategories;
    }
} 