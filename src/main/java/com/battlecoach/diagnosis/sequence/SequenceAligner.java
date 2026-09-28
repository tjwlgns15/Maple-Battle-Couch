package com.battlecoach.diagnosis.sequence;

import java.util.List;

/** 두 스킬 순서를 나란히 맞춘다. */
public interface SequenceAligner {

    List<AlignedPair> align(List<String> left, List<String> right);
}
