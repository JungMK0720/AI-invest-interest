package com.finmate.product;

import com.finmate.common.ApiException;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;

    public ProductController(ProductRepository productRepository,
                             ProductCategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/product-categories")
    @Transactional(readOnly = true)
    public List<ProductDtos.CategoryResponse> listCategories() {
        return categoryRepository.findAllByOrderByRiskGradeAsc().stream()
                .map(ProductDtos::from)
                .toList();
    }

    @GetMapping("/products")
    @Transactional(readOnly = true)
    public List<ProductDtos.ProductResponse> listProducts(
            @RequestParam(name = "categoryId", required = false) String categoryId) {
        List<Product> products = (categoryId == null || categoryId.isBlank())
                ? productRepository.findAllByOrderByRiskGradeAscNameAsc()
                : productRepository.findByCategoryIdOrderByRiskGradeAscNameAsc(categoryId);
        return products.stream().map(ProductDtos::from).toList();
    }

    @GetMapping("/products/{productId}")
    @Transactional(readOnly = true)
    public ProductDtos.ProductResponse getProduct(@PathVariable String productId) {
        return productRepository.findById(productId)
                .map(ProductDtos::from)
                .orElseThrow(() -> ApiException.notFound("상품을 찾을 수 없습니다."));
    }
}
