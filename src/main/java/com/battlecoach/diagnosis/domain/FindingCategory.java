package com.battlecoach.diagnosis.domain;

/** 진단을 유저가 바로 고칠 수 있는 것과 스펙을 올려야 하는 것으로 나눈다. */
public enum FindingCategory {

    OPERATION("운용"),
    LOADOUT("구성·스펙");

    private final String label;

    FindingCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
