package com.ilgiyebo.common.config;

import com.ilgiyebo.domain.user.entity.UserEntity;
import com.ilgiyebo.domain.user.repository.ScheduleRepository;
import com.ilgiyebo.domain.user.repository.UserRepository;
import com.ilgiyebo.domain.user.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 앱 시작 시 에브리타임 identifier로 각 테스트 유저의 시간표를 자동 등록한다.
 * 시간표 등록 → campus_building 매핑 → plan_entry 자동 생성까지 한 번에 처리된다.
 * 이미 시간표가 있는 유저는 건너뛴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DevScheduleInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleService scheduleService;

    private static final Map<String, String> USER_IDENTIFIERS = new LinkedHashMap<>();

    static {
        USER_IDENTIFIERS.put("user1@kookmin.ac.kr", "DmB8VEsLuiH7xlMxEv5b");
        USER_IDENTIFIERS.put("user2@kookmin.ac.kr", "ap12VflKFxFjLwfyf7NI");
        USER_IDENTIFIERS.put("user3@kookmin.ac.kr", "m5uAgNy2M0e90gKgYSay");
        USER_IDENTIFIERS.put("user4@kookmin.ac.kr", "b9deSEcrstVtCSeEyP75");
        USER_IDENTIFIERS.put("user5@kookmin.ac.kr", "MBQx75sjkIvjrxfFvG5W");
        USER_IDENTIFIERS.put("user6@kookmin.ac.kr", "Kcs4BJPIVecdb0woRZLG");
        USER_IDENTIFIERS.put("user7@kookmin.ac.kr", "xWYURmbdNVIy4qOdOqjz");
        USER_IDENTIFIERS.put("user8@kookmin.ac.kr", "cITSPPBWWYHMCRtFPQRZ");
        USER_IDENTIFIERS.put("user9@kookmin.ac.kr", "cvSpVB6VToBPRdNTDIgh");
        USER_IDENTIFIERS.put("user10@kookmin.ac.kr", "eRZx4agqapelTi9LxsWt");
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== 시간표 자동 등록 시작 ===");

        int success = 0;
        int skipped = 0;
        int failed = 0;

        for (Map.Entry<String, String> entry : USER_IDENTIFIERS.entrySet()) {
            String email = entry.getKey();
            String identifier = entry.getValue();

            Optional<UserEntity> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                log.warn("유저 없음, 건너뜀: {}", email);
                failed++;
                continue;
            }

            UserEntity user = userOpt.get();

            // 이미 시간표가 있으면 건너뜀
            if (!scheduleRepository.findAllByUserId(user.getId()).isEmpty()) {
                log.debug("이미 시간표 존재, 건너뜀: {}", email);
                skipped++;
                continue;
            }

            try {
                scheduleService.upsertMySchedule(user.getId(), identifier);
                log.info("시간표 등록 완료: {}", email);
                success++;
            } catch (Exception e) {
                log.warn("시간표 등록 실패: {} - {}", email, e.getMessage());
                failed++;
            }
        }

        log.info("=== 시간표 자동 등록 완료: 성공={}, 건너뜀={}, 실패={} ===", success, skipped, failed);
    }
}
