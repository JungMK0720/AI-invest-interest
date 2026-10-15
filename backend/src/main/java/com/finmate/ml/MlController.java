package com.finmate.ml;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인 없이 시연 가능한 두 가지 보조 모델 API. */
@RestController
@RequestMapping("/ml")
public class MlController {

    private final StyleInferenceService style;
    private final TermRetrievalService terms;

    public MlController(StyleInferenceService style, TermRetrievalService terms) {
        this.style = style;
        this.terms = terms;
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
