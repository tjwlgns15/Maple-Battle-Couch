package com.battlecoach.replay.web;

import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

import lombok.RequiredArgsConstructor;

/**
 * 차트 데이터를 {@code <script type="application/json">} 안에 그대로 넣기 위한 JSON 직렬화.
 * 시퀀스 이름은 유저가 입력한 값이라 "</script>" 로 태그를 닫고 스크립트를 주입할 수 있으므로 '<' 를 이스케이프한다.
 */
@Component
@RequiredArgsConstructor
class ScriptJsonWriter {

    private final JsonMapper jsonMapper;

    String write(Object value) {
        return jsonMapper.writeValueAsString(value).replace("<", "\\u003c");
    }
}
