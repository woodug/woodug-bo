package com.woodugserver.domain.game.entity;

public enum GameStatus {
    SCHEDULED,    // 경기 예정
    IN_PROGRESS,  // 진행 중
    SUSPENDED,    // 중단 (우천 등)
    FINISHED,     // 종료
    CANCELLED,    // 당일 취소
    POSTPONED     // 연기 (타 날짜로 이동)
}
