package com.finmate.ml;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인 없이 시연 가능한 두 가지 보조 모델 API와 모델 카드. */
@RestController
@RequestMapping("/ml")
public class MlController {

    private final StyleInferenceService style;
    private final TermRetrievalService terms;
    private final MlModelCatalog catalog;

    public MlController(StyleInferenceService style, TermRetrievalService terms, MlModelCatalog catalog) {
        this.style = style;
        this.terms = terms;
        this.catalog = catalog;
    }

    /** 학습 데이터·평가 지표·한계를 공개한다. 숫자는 ml/train.py가 만든 model-card.json 그대로다. */
    @GetMapping("/models")
    public MlDtos.ModelsResponse models() {
        Map<String, Object> runtime = new LinkedHashMap<>();
        runtime.put("styleModelVersion", style.version());
        runtime.putAll(terms.runtime());
        return new MlDtos.ModelsResponse(catalog.modelCard(), runtime);
    }

    @PostMapping("/style-check")
    public MlDtos.StyleCheckResponse styleCheck(@RequestBody(required = false) MlDtos.StyleCheckRequest request) {
        return style.check(request);
    }

    @PostMapping("/term-search")
    public MlDtos.TermSearchResponse termSearch(@RequestBody(required = false) MlDtos.TermSearchRequest request) {
        return terms.search(request);
    }
}
