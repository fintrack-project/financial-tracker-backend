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

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubcategoriesService Tests")
class SubcategoriesServiceTest {

    @Mock
    private CategoriesRepository categoriesRepository;
    
    @Mock
    private SubcategoriesRepository subcategoriesRepository;
    
    @Mock
    private HoldingsCategoriesRepository holdingsCategoriesRepository;

    private SubcategoriesService subcategoriesService;
    private UUID testAccountId;

    @BeforeEach
    void setUp() {
        subcategoriesService = new SubcategoriesService(
            categoriesRepository,
            subcategoriesRepository,
            holdingsCategoriesRepository
        );
        testAccountId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should add subcategory successfully without color")
    void shouldAddSubcategorySuccessfullyWithoutColor() {
        // Given: Category name, subcategory name and no existing subcategory
        String categoryName = "Technology";
        String subcategoryName = "Software";
        when(subcategoriesRepository.findSubcategoryIdByAccountIdAndCategoryNameAndSubcategoryName(
            testAccountId, categoryName, subcategoryName))
            .thenReturn(null);
        when(subcategoriesRepository.findMaxSubcategoryPriorityByAccountIdAndCategoryName(testAccountId, categoryName))
            .thenReturn(3);
        doNothing().when(subcategoriesRepository).insertSubcategory(testAccountId, categoryName, subcategoryName, 4);

        // When: Adding a new subcategory without color
        subcategoriesService.addSubcategory(testAccountId, categoryName, subcategoryName);

        // Then: Should insert the subcategory with calculated priority
        verify(subcategoriesRepository).findSubcategoryIdByAccountIdAndCategoryNameAndSubcategoryName(
            testAccountId, categoryName, subcategoryName);
        verify(subcategoriesRepository).findMaxSubcategoryPriorityByAccountIdAndCategoryName(testAccountId, categoryName);
        verify(subcategoriesRepository).insertSubcategory(testAccountId, categoryName, subcategoryName, 4);
    }

    @Test
    @DisplayName("Should add subcategory successfully with color")
    void shouldAddSubcategorySuccessfullyWithColor() {
        // Given: Category name, subcategory name with valid color and no existing subcategory
        String categoryName = "Technology";
        String subcategoryName = "Software";
        String hexCode = "#FF0000";
        when(subcategoriesRepository.findSubcategoryIdByAccountIdAndCategoryNameAndSubcategoryName(
            testAccountId, categoryName, subcategoryName))
            .thenReturn(null)
            .thenReturn(10); // First call returns null, second call returns ID after insertion
        when(subcategoriesRepository.findMaxSubcategoryPriorityByAccountIdAndCategoryName(testAccountId, categoryName))
            .thenReturn(0);
        doNothing().when(subcategoriesRepository).insertSubcategory(testAccountId, categoryName, subcategoryName, 1);

        // When: Adding a new subcategory with color
        subcategoriesService.addSubcategory(testAccountId, categoryName, subcategoryName, hexCode);

        // Then: Should insert the subcategory and update its color
        verify(subcategoriesRepository, times(2)).findSubcategoryIdByAccountIdAndCategoryNameAndSubcategoryName(
            testAccountId, categoryName, subcategoryName);
        verify(subcategoriesRepository).findMaxSubcategoryPriorityByAccountIdAndCategoryName(testAccountId, categoryName);
        verify(subcategoriesRepository).insertSubcategory(testAccountId, categoryName, subcategoryName, 1);
        verify(categoriesRepository).updateCategoryColor(testAccountId, 10, hexCode.toUpperCase());
    }

    @Test
    @DisplayName("Should throw exception when adding subcategory with empty name")
    void shouldThrowExceptionWhenAddingSubcategoryWithEmptyName() {
        // Given: Empty subcategory name
        String categoryName = "Technology";
        String subcategoryName = "";

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            subcategoriesService.addSubcategory(testAccountId, categoryName, subcategoryName);
        });
    }

    @Test
    @DisplayName("Should throw exception when adding subcategory with null name")
    void shouldThrowExceptionWhenAddingSubcategoryWithNullName() {
        // Given: Null subcategory name
        String categoryName = "Technology";
        String subcategoryName = null;

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            subcategoriesService.addSubcategory(testAccountId, categoryName, subcategoryName);
        });
    }

    @Test
    @DisplayName("Should throw exception when adding duplicate subcategory")
    void shouldThrowExceptionWhenAddingDuplicateSubcategory() {
        // Given: Subcategory name that already exists
        String categoryName = "Technology";
        String subcategoryName = "Software";
        when(subcategoriesRepository.findSubcategoryIdByAccountIdAndCategoryNameAndSubcategoryName(
            testAccountId, categoryName, subcategoryName))
            .thenReturn(1);

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            subcategoriesService.addSubcategory(testAccountId, categoryName, subcategoryName);
        });
    }

    @Test
    @DisplayName("Should throw exception when adding subcategory with invalid color")
    void shouldThrowExceptionWhenAddingSubcategoryWithInvalidColor() {
        // Given: Subcategory name with invalid color
        String categoryName = "Technology";
        String subcategoryName = "Software";
        String invalidHexCode = "#INVALID";

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            subcategoriesService.addSubcategory(testAccountId, categoryName, subcategoryName, invalidHexCode);
        });
    }

    @Test
    @DisplayName("Should update subcategory name successfully")
    void shouldUpdateSubcategoryNameSuccessfully() {
        // Given: Existing subcategory and new name
        String categoryName = "Technology";
        String oldSubcategoryName = "Software";
        String newSubcategoryName = "Development";
        when(subcategoriesRepository.findSubcategoryIdByAccountIdAndCategoryNameAndSubcategoryName(
            testAccountId, categoryName, oldSubcategoryName))
            .thenReturn(1);
        when(subcategoriesRepository.findSubcategoryIdByAccountIdAndCategoryNameAndSubcategoryName(
            testAccountId, categoryName, newSubcategoryName))
            .thenReturn(null);

        // When: Updating subcategory name
        subcategoriesService.updateSubcategoryName(testAccountId, categoryName, oldSubcategoryName, newSubcategoryName);

        // Then: Should update the subcategory name
        verify(subcategoriesRepository).findSubcategoryIdByAccountIdAndCategoryNameAndSubcategoryName(
            testAccountId, categoryName, oldSubcategoryName);
        verify(subcategoriesRepository).findSubcategoryIdByAccountIdAndCategoryNameAndSubcategoryName(
            testAccountId, categoryName, newSubcategoryName);
        verify(subcategoriesRepository).updateSubcategoryName(testAccountId, categoryName, oldSubcategoryName, newSubcategoryName);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent subcategory")
    void shouldThrowExceptionWhenUpdatingNonExistentSubcategory() {
        // Given: Non-existent subcategory name
        String categoryName = "Technology";
        String oldSubcategoryName = "NonExistent";
        String newSubcategoryName = "NewName";
        when(subcategoriesRepository.findSubcategoryIdByAccountIdAndCategoryNameAndSubcategoryName(
            testAccountId, categoryName, oldSubcategoryName))
            .thenReturn(null);

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            subcategoriesService.updateSubcategoryName(testAccountId, categoryName, oldSubcategoryName, newSubcategoryName);
        });
    }

    @Test
    @DisplayName("Should throw exception when updating to duplicate subcategory name")
    void shouldThrowExceptionWhenUpdatingToDuplicateSubcategoryName() {
        // Given: Existing subcategory and new name that already exists
        String categoryName = "Technology";
        String oldSubcategoryName = "Software";
        String newSubcategoryName = "Hardware";
        when(subcategoriesRepository.findSubcategoryIdByAccountIdAndCategoryNameAndSubcategoryName(
            testAccountId, categoryName, oldSubcategoryName))
            .thenReturn(1);
        when(subcategoriesRepository.findSubcategoryIdByAccountIdAndCategoryNameAndSubcategoryName(
            testAccountId, categoryName, newSubcategoryName))
            .thenReturn(2);

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            subcategoriesService.updateSubcategoryName(testAccountId, categoryName, oldSubcategoryName, newSubcategoryName);
        });
    }

    @Test
    @DisplayName("Should remove subcategory successfully")
    void shouldRemoveSubcategorySuccessfully() {
        // Given: Existing category and subcategory
        String categoryName = "Technology";
        String subcategoryName = "Software";
        List<Category> remainingSubcategories = createSampleSubcategories();
        
        when(categoriesRepository.findCategoryIdByAccountIdAndCategoryName(testAccountId, categoryName))
            .thenReturn(1);
        when(categoriesRepository.findCategoryIdByAccountIdAndCategoryName(testAccountId, subcategoryName))
            .thenReturn(10);
        when(subcategoriesRepository.findSubcategoriesByParentId(testAccountId, 1))
            .thenReturn(remainingSubcategories);

        // When: Removing the subcategory
        subcategoriesService.removeSubcategory(testAccountId, categoryName, subcategoryName);

        // Then: Should delete related data and reassign priorities
        verify(categoriesRepository).findCategoryIdByAccountIdAndCategoryName(testAccountId, categoryName);
        verify(categoriesRepository).findCategoryIdByAccountIdAndCategoryName(testAccountId, subcategoryName);
        verify(holdingsCategoriesRepository).updateSubcategoryToNull(testAccountId, subcategoryName, 1);
        verify(categoriesRepository).deleteByCategoryId(10);
        verify(subcategoriesRepository).findSubcategoriesByParentId(testAccountId, 1);
    }

    @Test
    @DisplayName("Should throw exception when removing subcategory from non-existent category")
    void shouldThrowExceptionWhenRemovingSubcategoryFromNonExistentCategory() {
        // Given: Non-existent category name
        String categoryName = "NonExistent";
        String subcategoryName = "Software";
        when(categoriesRepository.findCategoryIdByAccountIdAndCategoryName(testAccountId, categoryName))
            .thenReturn(null);

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            subcategoriesService.removeSubcategory(testAccountId, categoryName, subcategoryName);
        });
    }

    @Test
    @DisplayName("Should throw exception when removing non-existent subcategory")
    void shouldThrowExceptionWhenRemovingNonExistentSubcategory() {
        // Given: Existing category but non-existent subcategory
        String categoryName = "Technology";
        String subcategoryName = "NonExistent";
        when(categoriesRepository.findCategoryIdByAccountIdAndCategoryName(testAccountId, categoryName))
            .thenReturn(1);
        when(categoriesRepository.findCategoryIdByAccountIdAndCategoryName(testAccountId, subcategoryName))
            .thenReturn(null);

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            subcategoriesService.removeSubcategory(testAccountId, categoryName, subcategoryName);
        });
    }

    @Test
    @DisplayName("Should update subcategory color successfully")
    void shouldUpdateSubcategoryColorSuccessfully() {
        // Given: Existing subcategory and valid color
        String categoryName = "Technology";
        String subcategoryName = "Software";
        String hexCode = "#FF0000";
        when(subcategoriesRepository.findSubcategoryIdByAccountIdAndCategoryNameAndSubcategoryName(
            testAccountId, categoryName, subcategoryName))
            .thenReturn(10);

        // When: Updating subcategory color
        subcategoriesService.updateSubcategoryColor(testAccountId, categoryName, subcategoryName, hexCode);

        // Then: Should update the subcategory color
        verify(subcategoriesRepository).findSubcategoryIdByAccountIdAndCategoryNameAndSubcategoryName(
            testAccountId, categoryName, subcategoryName);
        verify(categoriesRepository).updateCategoryColor(testAccountId, 10, hexCode.toUpperCase());
    }

    @Test
    @DisplayName("Should throw exception when updating color for non-existent subcategory")
    void shouldThrowExceptionWhenUpdatingColorForNonExistentSubcategory() {
        // Given: Non-existent subcategory name
        String categoryName = "Technology";
        String subcategoryName = "NonExistent";
        String hexCode = "#FF0000";
        when(subcategoriesRepository.findSubcategoryIdByAccountIdAndCategoryNameAndSubcategoryName(
            testAccountId, categoryName, subcategoryName))
            .thenReturn(null);

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            subcategoriesService.updateSubcategoryColor(testAccountId, categoryName, subcategoryName, hexCode);
        });
    }

    @Test
    @DisplayName("Should throw exception when updating subcategory with invalid color")
    void shouldThrowExceptionWhenUpdatingSubcategoryWithInvalidColor() {
        // Given: Existing subcategory with invalid color
        String categoryName = "Technology";
        String subcategoryName = "Software";
        String invalidHexCode = "#INVALID";

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            subcategoriesService.updateSubcategoryColor(testAccountId, categoryName, subcategoryName, invalidHexCode);
        });
    }

    @Test
    @DisplayName("Should get subcategory color map successfully")
    void shouldGetSubcategoryColorMapSuccessfully() {
        // Given: Existing category with subcategories
        String categoryName = "Technology";
        List<Category> subcategories = createSampleSubcategoriesWithColors();
        
        when(categoriesRepository.findCategoryIdByAccountIdAndCategoryName(testAccountId, categoryName))
            .thenReturn(1);
        when(subcategoriesRepository.findSubcategoriesByParentId(testAccountId, 1))
            .thenReturn(subcategories);

        // When: Getting subcategory color map
        Map<String, Object> result = subcategoriesService.getSubcategoryColorMap(testAccountId, categoryName);

        // Then: Should return map with subcategory colors
        assertNotNull(result);
        assertTrue(result.containsKey("subcategoryColors"));
        assertTrue(result.containsKey("categoryName"));
        assertEquals(categoryName, result.get("categoryName"));
        
        @SuppressWarnings("unchecked")
        Map<String, String> subcategoryColors = (Map<String, String>) result.get("subcategoryColors");
        assertEquals(2, subcategoryColors.size());
        assertEquals("#FF0000", subcategoryColors.get("Software"));
        assertEquals("#0000FF", subcategoryColors.get("Hardware")); // Default color for null
    }

    @Test
    @DisplayName("Should throw exception when getting color map for non-existent category")
    void shouldThrowExceptionWhenGettingColorMapForNonExistentCategory() {
        // Given: Non-existent category name
        String categoryName = "NonExistent";
        when(categoriesRepository.findCategoryIdByAccountIdAndCategoryName(testAccountId, categoryName))
            .thenReturn(null);

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            subcategoriesService.getSubcategoryColorMap(testAccountId, categoryName);
        });
    }

    @Test
    @DisplayName("Should throw exception when getting color map with empty category name")
    void shouldThrowExceptionWhenGettingColorMapWithEmptyCategoryName() {
        // Given: Empty category name
        String categoryName = "";

        // When & Then: Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            subcategoriesService.getSubcategoryColorMap(testAccountId, categoryName);
        });
    }

    @Test
    @DisplayName("Should handle trimming of subcategory names")
    void shouldHandleTrimmingOfSubcategoryNames() {
        // Given: Subcategory name with leading/trailing spaces
        String categoryName = "Technology";
        String subcategoryName = "  Software  ";
        String trimmedSubcategoryName = "Software";
        when(subcategoriesRepository.findSubcategoryIdByAccountIdAndCategoryNameAndSubcategoryName(
            testAccountId, categoryName, trimmedSubcategoryName))
            .thenReturn(null);
        when(subcategoriesRepository.findMaxSubcategoryPriorityByAccountIdAndCategoryName(testAccountId, categoryName))
            .thenReturn(0);
        doNothing().when(subcategoriesRepository).insertSubcategory(testAccountId, categoryName, trimmedSubcategoryName, 1);

        // When: Adding a subcategory with spaces
        subcategoriesService.addSubcategory(testAccountId, categoryName, subcategoryName);

        // Then: Should trim the name and insert with trimmed version
        verify(subcategoriesRepository).findSubcategoryIdByAccountIdAndCategoryNameAndSubcategoryName(
            testAccountId, categoryName, trimmedSubcategoryName);
        verify(subcategoriesRepository).insertSubcategory(testAccountId, categoryName, trimmedSubcategoryName, 1);
    }

    // Helper methods to create test data
    private List<Category> createSampleSubcategories() {
        List<Category> subcategories = new ArrayList<>();
        
        Category subcategory1 = new Category();
        subcategory1.setCategoryId(10);
        subcategory1.setAccountId(testAccountId);
        subcategory1.setCategoryName("Software");
        subcategory1.setParentId(1);
        subcategory1.setColor("#FF0000");
        subcategories.add(subcategory1);

        Category subcategory2 = new Category();
        subcategory2.setCategoryId(11);
        subcategory2.setAccountId(testAccountId);
        subcategory2.setCategoryName("Hardware");
        subcategory2.setParentId(1);
        subcategory2.setColor("#00FF00");
        subcategories.add(subcategory2);

        return subcategories;
    }

    private List<Category> createSampleSubcategoriesWithColors() {
        List<Category> subcategories = new ArrayList<>();
        
        Category subcategory1 = new Category();
        subcategory1.setCategoryId(10);
        subcategory1.setAccountId(testAccountId);
        subcategory1.setCategoryName("Software");
        subcategory1.setParentId(1);
        subcategory1.setColor("#FF0000");
        subcategories.add(subcategory1);

        Category subcategory2 = new Category();
        subcategory2.setCategoryId(11);
        subcategory2.setAccountId(testAccountId);
        subcategory2.setCategoryName("Hardware");
        subcategory2.setParentId(1);
        subcategory2.setColor(null); // Will use default color
        subcategories.add(subcategory2);

        return subcategories;
    }
} 