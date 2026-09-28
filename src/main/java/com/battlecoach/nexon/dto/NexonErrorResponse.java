package com.battlecoach.nexon.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NexonErrorResponse(
        @JsonProperty("error") Error error
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Error(
            @JsonProperty("name") String name,
            @JsonProperty("message") String message
    ) {
    }
}
