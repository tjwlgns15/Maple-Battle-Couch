package com.battlecoach.nexon.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** GET /maplestory/v1/id */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OcidResponse(
        @JsonProperty("ocid") String ocid
) {
}
