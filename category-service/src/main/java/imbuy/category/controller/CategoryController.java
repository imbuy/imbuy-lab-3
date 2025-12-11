package imbuy.category.controller;

import imbuy.category.dto.*;
import imbuy.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Category management APIs")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/tree")
    @Operation(summary = "Get category tree")
    public Mono<CategoryTreeDto> getCategoryTree() {
        return categoryService.getCategoryTree();
    }

    @GetMapping
    @Operation(summary = "Get all categories with pagination")
    public Flux<CategoryResponse> getAllCategoriesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return categoryService.getAllCategories(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    public Mono<CategoryResponse> getCategoryById(@PathVariable Long id) {
        return categoryService.getCategoryById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new category", security = @SecurityRequirement(name = "bearerAuth"))
    public Mono<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest categoryRequest) {
        return categoryService.createCategory(categoryRequest);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update category", security = @SecurityRequirement(name = "bearerAuth"))
    public Mono<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest categoryRequest) {
        return categoryService.updateCategory(id, categoryRequest);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete category", security = @SecurityRequirement(name = "bearerAuth"))
    public Mono<Void> deleteCategory(@PathVariable Long id) {
        return categoryService.deleteCategory(id);
    }
}