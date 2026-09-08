package com.finmate.product;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, String> {

    List<ProductCategory> findAllByOrderByRiskGradeAsc();
}
