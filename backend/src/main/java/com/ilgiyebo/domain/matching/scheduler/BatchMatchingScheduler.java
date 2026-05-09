package com.ilgiyebo.domain.matching.scheduler;

import com.ilgiyebo.dto.BatchMatchingResultDto;
import com.ilgiyebo.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 배치 매칭 스케줄러.
 * 매주 월요일 자정(00:00:00)에 배치 매칭을 실행한다.
 * 중간에 매칭이 일찍 끝나도 다음 월요일까지 재매칭하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class BatchMatchingScheduler {

    private static final Logger log = LoggerFactory.getLogger(BatchMatchingScheduler.class);

    private final MatchingService matchingService;

    /**
     * 매주 월요일 자정에 배치 매칭을 실행한다.
     * cron: 초 분 시 일 월 요일
     * "0 0 0 * * MON" = 매주 월요일 00:00:00
     */
    @Scheduled(cron = "0 0 0 * * MON")
    public void runBatchMatching() {
        log.info("=== 배치 매칭 스케줄러 시작 ===");

        try {
            BatchMatchingResultDto result = matchingService.executeBatchMatching();

            log.info("=== 배치 매칭 스케줄러 완료 === " +
                            "처리된 슬롯: {}, 생성된 매칭: {}, 실패한 슬롯: {}",
                    result.totalProcessed(),
                    result.matchesCreated(),
                    result.failedSlots().size());
        } catch (Exception e) {
            log.error("배치 매칭 실행 중 오류 발생", e);
        }
    }
}
