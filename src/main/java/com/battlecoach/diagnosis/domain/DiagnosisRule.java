package com.battlecoach.diagnosis.domain;

import java.util.List;

/** 진단 규칙. 새 규칙은 이 인터페이스를 구현한 빈을 추가하면 엔진에 자동으로 붙는다. */
public interface DiagnosisRule {

    List<Finding> evaluate(AnalysisContext context);
}
