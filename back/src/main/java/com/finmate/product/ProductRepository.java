package com.finmate.product;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findByCategoryIdOrderByRiskGradeAscNameAsc(String categoryId);

    List<Product> findAllByOrderByRiskGradeAscNameAsc();
}
