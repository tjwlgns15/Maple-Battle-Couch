package com.battlecoach.diagnosis.rule;

/**
 * 스킬 이름 뒤 조사를 받침에 맞춘다("판데모니움과", "스틱스와").
 * 한글이 아닌 글자(로마 숫자 "VI" 등)로 끝나면 받침이 없는 쪽을 쓴다.
 */
final class KoreanJosa {

    private static final char HANGUL_FIRST = '가';
    private static final char HANGUL_LAST = '힣';
    private static final int FINAL_CONSONANTS = 28;

    private KoreanJosa() {
    }

    /** "와/과" */
    static String withAnd(String word) {
        return word + (hasFinalConsonant(word) ? "과" : "와");
    }

    /** "를/을" */
    static String withObject(String word) {
        return word + (hasFinalConsonant(word) ? "을" : "를");
    }

    static boolean hasFinalConsonant(String word) {
        String trimmed = word.strip();
        if (trimmed.isEmpty()) {
            return false;
        }
        char last = trimmed.charAt(trimmed.length() - 1);
        return last >= HANGUL_FIRST && last <= HANGUL_LAST && (last - HANGUL_FIRST) % FINAL_CONSONANTS != 0;
    }
}
