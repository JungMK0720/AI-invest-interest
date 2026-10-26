package com.finmate.glossary;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlossaryCategoryRepository extends JpaRepository<GlossaryCategory, String> {
    List<GlossaryCategory> findAllByOrderBySortOrderAsc();
}
