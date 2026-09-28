package com.battlecoach.diagnosis.sequence;

/** 정렬 결과 한 줄. 한쪽에만 있으면 다른 쪽은 null 이다. */
public record AlignedPair(String left, String right) {

    public enum Status {
        MATCH, MISMATCH, LEFT_ONLY, RIGHT_ONLY
    }

    public AlignedPair {
        if (left == null && right == null) {
            throw new IllegalArgumentException("양쪽이 모두 비어 있을 수 없습니다.");
        }
    }

    public Status status() {
        if (left == null) {
            return Status.RIGHT_ONLY;
        }
        if (right == null) {
            return Status.LEFT_ONLY;
        }
        return left.equals(right) ? Status.MATCH : Status.MISMATCH;
    }
}
