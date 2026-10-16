package com.finmate.ml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finmate.glossary.GlossarySeedData;
import com.finmate.learning.LearningCatalog;
import com.finmate.learning.LearningSeedData;
import com.finmate.learning.LearningSignalCatalog;

/** 스프링 없이 모델·카탈로그를 한 번만 읽어 여러 테스트가 공유한다. */
final class MlTestFixtures {

    static final ObjectMapper MAPPER = new ObjectMapper();
    static final MlModelCatalog MODELS;
    static final GlossarySeedData GLOSSARY;
    static final LearningSignalCatalog SIGNALS;

    static {
        MODELS = new MlModelCatalog(MAPPER);
        MODELS.load();
        GLOSSARY = new GlossarySeedData(MAPPER);
        LearningSeedData seedConcepts = new LearningSeedData(MAPPER);
        SIGNALS = new LearningSignalCatalog(MAPPER, seedConcepts, new LearningCatalog(seedConcepts::tree), GLOSSARY);
    }

    private MlTestFixtures() {}

    static StyleInferenceService style() {
        return new StyleInferenceService(MODELS, SIGNALS);
    }

    static TermRetrievalService seedTermSearch() {
        return new TermRetrievalService(MODELS, GLOSSARY::terms);
    }
}
