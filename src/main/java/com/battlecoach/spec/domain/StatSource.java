package com.battlecoach.spec.domain;

/**
 * 합산 스탯 하나를 이루는 출처 한 곳.
 *
 * @param label 출처 이름 ("모자 잠재능력", "유니온 공격대원")
 * @param value 그 출처가 더한 값 (초 또는 %)
 */
public record StatSource(String label, double value) {

    public static StatSource of(String label, double value) {
        return new StatSource(label, value);
    }
}
