package com.battlecoach.diagnosis.sequence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Needleman-Wunsch 전역 정렬.
 * 불일치 점수를 간격 두 개보다 낮게 둬서 서로 다른 스킬을 한 줄에 억지로 맞추지 않는다.
 * 그래서 결과는 "같은 스킬" 또는 "한쪽에만 있는 스킬" 줄로만 나오며, 순서가 바뀐 스킬은 양쪽에 따로 드러난다.
 */
@Component
public class NeedlemanWunschAligner implements SequenceAligner {

    static final int MATCH = 2;
    static final int MISMATCH = -3;
    static final int GAP = -1;

    @Override
    public List<AlignedPair> align(List<String> left, List<String> right) {
        int n = left.size();
        int m = right.size();
        int[][] score = new int[n + 1][m + 1];
        for (int i = 1; i <= n; i++) {
            score[i][0] = i * GAP;
        }
        for (int j = 1; j <= m; j++) {
            score[0][j] = j * GAP;
        }
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                score[i][j] = Math.max(
                        score[i - 1][j - 1] + substitution(left.get(i - 1), right.get(j - 1)),
                        Math.max(score[i - 1][j] + GAP, score[i][j - 1] + GAP));
            }
        }
        return traceback(left, right, score);
    }

    private static List<AlignedPair> traceback(List<String> left, List<String> right, int[][] score) {
        List<AlignedPair> pairs = new ArrayList<>();
        int i = left.size();
        int j = right.size();
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0
                    && score[i][j] == score[i - 1][j - 1] + substitution(left.get(i - 1), right.get(j - 1))) {
                pairs.add(new AlignedPair(left.get(i - 1), right.get(j - 1)));
                i--;
                j--;
            } else if (i > 0 && score[i][j] == score[i - 1][j] + GAP) {
                pairs.add(new AlignedPair(left.get(i - 1), null));
                i--;
            } else {
                pairs.add(new AlignedPair(null, right.get(j - 1)));
                j--;
            }
        }
        Collections.reverse(pairs);
        return List.copyOf(pairs);
    }

    private static int substitution(String a, String b) {
        return a.equals(b) ? MATCH : MISMATCH;
    }
}
