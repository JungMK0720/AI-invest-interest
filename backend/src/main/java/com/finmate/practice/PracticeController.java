package com.finmate.practice;

import com.finmate.practice.PracticeDtos.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/practice")
@Validated
public class PracticeController {
    private final PracticeCatalog catalog; private final PracticeEngine engine;
    public PracticeController(PracticeCatalog catalog, PracticeEngine engine) { this.catalog=catalog; this.engine=engine; }
    @GetMapping("/products") public List<ProductSpec> products() { return catalog.products(); }
    @GetMapping("/scenarios") public List<Scenario> scenarios() { return catalog.scenarios(); }
    @GetMapping("/methodology") public Methodology methodology() { return catalog.methodology(); }
    @PostMapping("/analyze") public Analysis analyze(@Valid @RequestBody AnalyzeRequest request) { return engine.analyze(request); }
    @PostMapping("/compare") public Comparison compare(@Valid @RequestBody CompareRequest request) { return engine.compare(request); }
}
