package com.battlecoach.diagnosis.domain;

/**
 * 놓친 시전을 "쿨이 돈 시각부터 쿨마다 썼다면" 어디에 떨어졌을지로 나눈 것.
 * 영향도는 평균 1회 데미지로 계산하므로, 극딜 밖 놓침은 실제 손해가 표시보다 작고 극딜 안 놓침은 클 수 있다.
 * (result 는 스킬별 합계만 줘서 극딜 안팎 1회 데미지를 나눌 수 없다)
 *
 * @param unplaced 공백 하나만으로는 1회가 안 되지만 여러 공백을 합쳐 1회가 된 몫
 */
public record MissedPlacement(int inBurst, int outOfBurst, int unplaced) {

    public static MissedPlacement none() {
        return new MissedPlacement(0, 0, 0);
    }
}
