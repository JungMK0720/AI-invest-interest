package com.finmate.glossary;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlossaryTermRepository extends JpaRepository<GlossaryTerm, String> {
    List<GlossaryTerm> findByStatus(GlossaryStatus status);

    long countByStatus(GlossaryStatus status);
}
