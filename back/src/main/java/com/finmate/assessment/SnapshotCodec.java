package com.finmate.assessment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finmate.diagnosis.Question;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SnapshotCodec {
    private final ObjectMapper mapper;
    public SnapshotCodec(ObjectMapper mapper) { this.mapper = mapper; }

    public String encode(DecisionSnapshot snapshot) { return json(snapshot); }
    public DecisionSnapshot decode(String json) {
        if (json == null) return null; // 기존 데이터의 당시 근거는 추정하여 복원하지 않는다.
        try { return mapper.readValue(json, DecisionSnapshot.class); }
        catch (JsonProcessingException e) { throw new IllegalStateException("저장된 진단 근거를 읽을 수 없습니다."); }
    }

    public String questionnaireVersion(List<Question> questions) {
        var canonical = questions.stream().sorted(Comparator.comparing(Question::getId))
                .map(q -> List.of(q.getId(), q.getContent(), q.getWeight(), q.getOrderNo(),
                        q.getChoices().stream().sorted(Comparator.comparing(c -> c.getId()))
                                .map(c -> List.of(c.getId(), c.getLabel(), c.getAnalysisKey(), c.getScore(), c.getOrderNo()))
                                .toList())).toList();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(json(canonical).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException("진단 근거를 저장할 수 없습니다."); }
    }
}
