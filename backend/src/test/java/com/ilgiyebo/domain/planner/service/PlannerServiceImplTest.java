package com.ilgiyebo.domain.planner.service;

import com.ilgiyebo.common.exception.BusinessException;
import com.ilgiyebo.domain.planner.dto.CreatePlanEntryRequest;
import com.ilgiyebo.domain.planner.dto.PlanEntryResponse;
import com.ilgiyebo.domain.planner.dto.UpdatePlanEntryRequest;
import com.ilgiyebo.domain.planner.entity.PlanEntryEntity;
import com.ilgiyebo.domain.planner.entity.PlanItemType;
import com.ilgiyebo.domain.planner.entity.PlanSource;
import com.ilgiyebo.domain.planner.repository.PlanEntryRepository;
import com.ilgiyebo.domain.user.entity.ScheduleEntity;
import com.ilgiyebo.domain.user.entity.SemesterEntity;
import com.ilgiyebo.domain.user.entity.SemesterTerm;
import com.ilgiyebo.domain.user.entity.UserEntity;
import com.ilgiyebo.domain.user.repository.ScheduleRepository;
import com.ilgiyebo.domain.user.repository.SemesterRepository;
import com.ilgiyebo.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlannerServiceImplTest {

    @Mock
    private PlanEntryRepository planEntryRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PlannerServiceImpl plannerService;

    private UUID userId;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = UserEntity.builder()
                .email("test@university.ac.kr")
                .passwordHash("hash")
                .nickname("테스트")
                .university("테스트대학교")
                .build();
        setId(user, userId);
    }

    @Nested
    @DisplayName("createPlanEntry")
    class CreatePlanEntryTests {

        @Test
        @DisplayName("유효한 요청으로 MANUAL 일정을 생성한다")
        void shouldCreateManualEntry() {
            CreatePlanEntryRequest request = new CreatePlanEntryRequest(
                    LocalDate.of(2025, 3, 10),
                    LocalTime.of(9, 0),
                    LocalTime.of(10, 30),
                    "도서관",
                    "자습",
                    PlanItemType.FREE
            );

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(planEntryRepository.findConflicting(eq(userId), any(), any(), any(), isNull()))
                    .thenReturn(List.of());
            when(planEntryRepository.save(any(PlanEntryEntity.class)))
                    .thenAnswer(invocation -> {
                        PlanEntryEntity entity = invocation.getArgument(0);
                        setId(entity, UUID.randomUUID());
                        return entity;
                    });

            PlanEntryResponse response = plannerService.createPlanEntry(userId, request);

            assertThat(response.date()).isEqualTo(LocalDate.of(2025, 3, 10));
            assertThat(response.startTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(response.endTime()).isEqualTo(LocalTime.of(10, 30));
            assertThat(response.location()).isEqualTo("도서관");
            assertThat(response.name()).isEqualTo("자습");
            assertThat(response.type()).isEqualTo(PlanItemType.FREE);
            assertThat(response.source()).isEqualTo(PlanSource.MANUAL);
            assertThat(response.sourceScheduleId()).isNull();
        }

        @Test
        @DisplayName("30분 단위가 아닌 시작 시간은 거부한다")
        void shouldRejectNon30MinuteStartTime() {
            CreatePlanEntryRequest request = new CreatePlanEntryRequest(
                    LocalDate.of(2025, 3, 10),
                    LocalTime.of(9, 15),
                    LocalTime.of(10, 30),
                    "도서관",
                    "자습",
                    PlanItemType.FREE
            );

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> plannerService.createPlanEntry(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("30분 단위");
        }

        @Test
        @DisplayName("30분 단위가 아닌 종료 시간은 거부한다")
        void shouldRejectNon30MinuteEndTime() {
            CreatePlanEntryRequest request = new CreatePlanEntryRequest(
                    LocalDate.of(2025, 3, 10),
                    LocalTime.of(9, 0),
                    LocalTime.of(10, 45),
                    "도서관",
                    "자습",
                    PlanItemType.FREE
            );

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> plannerService.createPlanEntry(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("30분 단위");
        }

        @Test
        @DisplayName("종료 시간이 시작 시간보다 이전이면 거부한다")
        void shouldRejectEndTimeBeforeStartTime() {
            CreatePlanEntryRequest request = new CreatePlanEntryRequest(
                    LocalDate.of(2025, 3, 10),
                    LocalTime.of(10, 30),
                    LocalTime.of(9, 0),
                    "도서관",
                    "자습",
                    PlanItemType.FREE
            );

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> plannerService.createPlanEntry(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("종료 시간");
        }

        @Test
        @DisplayName("시간 충돌이 있으면 거부한다")
        void shouldRejectTimeConflict() {
            CreatePlanEntryRequest request = new CreatePlanEntryRequest(
                    LocalDate.of(2025, 3, 10),
                    LocalTime.of(9, 0),
                    LocalTime.of(10, 30),
                    "도서관",
                    "자습",
                    PlanItemType.FREE
            );

            PlanEntryEntity existing = PlanEntryEntity.builder()
                    .user(user)
                    .date(LocalDate.of(2025, 3, 10))
                    .startTime(LocalTime.of(9, 30))
                    .endTime(LocalTime.of(11, 0))
                    .source(PlanSource.MANUAL)
                    .type(PlanItemType.CLASS)
                    .build();
            setId(existing, UUID.randomUUID());

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(planEntryRepository.findConflicting(eq(userId), any(), any(), any(), isNull()))
                    .thenReturn(List.of(existing));

            assertThatThrownBy(() -> plannerService.createPlanEntry(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("이미 일정이 존재");
        }

        @Test
        @DisplayName("종료시간=시작시간 맞닿는 경우는 충돌이 아니다 (쿼리가 빈 결과 반환)")
        void shouldAllowAdjacentEntries() {
            CreatePlanEntryRequest request = new CreatePlanEntryRequest(
                    LocalDate.of(2025, 3, 10),
                    LocalTime.of(10, 30),
                    LocalTime.of(12, 0),
                    "강의실",
                    "수업",
                    PlanItemType.CLASS
            );

            // 충돌 쿼리가 빈 결과를 반환 (맞닿는 경우 DB 쿼리에서 제외됨)
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(planEntryRepository.findConflicting(eq(userId), any(), any(), any(), isNull()))
                    .thenReturn(List.of());
            when(planEntryRepository.save(any(PlanEntryEntity.class)))
                    .thenAnswer(invocation -> {
                        PlanEntryEntity entity = invocation.getArgument(0);
                        setId(entity, UUID.randomUUID());
                        return entity;
                    });

            PlanEntryResponse response = plannerService.createPlanEntry(userId, request);

            assertThat(response.startTime()).isEqualTo(LocalTime.of(10, 30));
        }
    }

    @Nested
    @DisplayName("updatePlanEntry")
    class UpdatePlanEntryTests {

        @Test
        @DisplayName("MANUAL 일정을 수정한다")
        void shouldUpdateManualEntry() {
            UUID entryId = UUID.randomUUID();
            PlanEntryEntity existing = PlanEntryEntity.builder()
                    .user(user)
                    .date(LocalDate.of(2025, 3, 10))
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(10, 30))
                    .location("도서관")
                    .name("자습")
                    .type(PlanItemType.FREE)
                    .source(PlanSource.MANUAL)
                    .build();
            setId(existing, entryId);

            UpdatePlanEntryRequest request = new UpdatePlanEntryRequest(
                    LocalDate.of(2025, 3, 10),
                    LocalTime.of(10, 0),
                    LocalTime.of(11, 30),
                    "카페",
                    "스터디",
                    PlanItemType.ACTIVITY
            );

            when(planEntryRepository.findById(entryId)).thenReturn(Optional.of(existing));
            when(planEntryRepository.findConflicting(eq(userId), any(), any(), any(), eq(entryId)))
                    .thenReturn(List.of());
            when(planEntryRepository.save(any(PlanEntryEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            PlanEntryResponse response = plannerService.updatePlanEntry(userId, entryId, request);

            assertThat(response.startTime()).isEqualTo(LocalTime.of(10, 0));
            assertThat(response.endTime()).isEqualTo(LocalTime.of(11, 30));
            assertThat(response.location()).isEqualTo("카페");
            assertThat(response.name()).isEqualTo("스터디");
        }

        @Test
        @DisplayName("SCHEDULE_AUTO 일정도 수정 가능하다")
        void shouldUpdateScheduleAutoEntry() {
            UUID entryId = UUID.randomUUID();
            PlanEntryEntity existing = PlanEntryEntity.builder()
                    .user(user)
                    .date(LocalDate.of(2025, 3, 10))
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(10, 30))
                    .location("공학관")
                    .name("데이터베이스")
                    .type(PlanItemType.CLASS)
                    .source(PlanSource.SCHEDULE_AUTO)
                    .sourceSchedule(ScheduleEntity.builder().name("mock").dayOfWeek(DayOfWeek.MONDAY).startedAt(LocalTime.of(9,0)).endedAt(LocalTime.of(10,30)).place("공학관").user(user).build())
                    .build();
            setId(existing, entryId);

            UpdatePlanEntryRequest request = new UpdatePlanEntryRequest(
                    LocalDate.of(2025, 3, 10),
                    LocalTime.of(9, 30),
                    LocalTime.of(11, 0),
                    "공학관 201호",
                    "데이터베이스 (변경)",
                    PlanItemType.CLASS
            );

            when(planEntryRepository.findById(entryId)).thenReturn(Optional.of(existing));
            when(planEntryRepository.findConflicting(eq(userId), any(), any(), any(), eq(entryId)))
                    .thenReturn(List.of());
            when(planEntryRepository.save(any(PlanEntryEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            PlanEntryResponse response = plannerService.updatePlanEntry(userId, entryId, request);

            assertThat(response.location()).isEqualTo("공학관 201호");
            assertThat(response.name()).isEqualTo("데이터베이스 (변경)");
        }

        @Test
        @DisplayName("존재하지 않는 일정 수정 시 예외를 던진다")
        void shouldRejectUpdateOfNonExistentEntry() {
            UUID entryId = UUID.randomUUID();

            when(planEntryRepository.findById(entryId)).thenReturn(Optional.empty());

            UpdatePlanEntryRequest request = new UpdatePlanEntryRequest(
                    LocalDate.of(2025, 3, 10),
                    LocalTime.of(9, 0),
                    LocalTime.of(10, 30),
                    "도서관",
                    "자습",
                    PlanItemType.FREE
            );

            assertThatThrownBy(() -> plannerService.updatePlanEntry(userId, entryId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("일정을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("다른 사용자의 일정은 수정할 수 없다")
        void shouldRejectUpdateOfOtherUsersEntry() {
            UUID entryId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();
            UserEntity otherUser = UserEntity.builder()
                    .email("other@university.ac.kr")
                    .passwordHash("hash")
                    .nickname("다른유저")
                    .university("테스트대학교")
                    .build();
            setId(otherUser, otherUserId);

            PlanEntryEntity existing = PlanEntryEntity.builder()
                    .user(otherUser)
                    .date(LocalDate.of(2025, 3, 10))
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(10, 30))
                    .source(PlanSource.MANUAL)
                    .type(PlanItemType.FREE)
                    .build();
            setId(existing, entryId);

            when(planEntryRepository.findById(entryId)).thenReturn(Optional.of(existing));

            UpdatePlanEntryRequest request = new UpdatePlanEntryRequest(
                    LocalDate.of(2025, 3, 10),
                    LocalTime.of(9, 0),
                    LocalTime.of(10, 30),
                    "도서관",
                    "자습",
                    PlanItemType.FREE
            );

            assertThatThrownBy(() -> plannerService.updatePlanEntry(userId, entryId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("권한이 없습니다");
        }
    }

    @Nested
    @DisplayName("deletePlanEntry")
    class DeletePlanEntryTests {

        @Test
        @DisplayName("본인의 일정을 삭제한다")
        void shouldDeleteOwnEntry() {
            UUID entryId = UUID.randomUUID();
            PlanEntryEntity existing = PlanEntryEntity.builder()
                    .user(user)
                    .date(LocalDate.of(2025, 3, 10))
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(10, 30))
                    .source(PlanSource.MANUAL)
                    .type(PlanItemType.FREE)
                    .build();
            setId(existing, entryId);

            when(planEntryRepository.findById(entryId)).thenReturn(Optional.of(existing));

            plannerService.deletePlanEntry(userId, entryId);

            verify(planEntryRepository).delete(existing);
        }

        @Test
        @DisplayName("존재하지 않는 일정 삭제 시 예외를 던진다")
        void shouldThrowWhenEntryNotFound() {
            UUID entryId = UUID.randomUUID();
            when(planEntryRepository.findById(entryId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> plannerService.deletePlanEntry(userId, entryId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("일정을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("getPlanEntries")
    class GetPlanEntriesTests {

        @Test
        @DisplayName("특정 날짜의 일정 목록을 시작 시간 순으로 조회한다")
        void shouldReturnEntriesOrderedByStartTime() {
            LocalDate date = LocalDate.of(2025, 3, 10);

            PlanEntryEntity entry1 = PlanEntryEntity.builder()
                    .user(user)
                    .date(date)
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(10, 30))
                    .location("강의실")
                    .name("수업")
                    .type(PlanItemType.CLASS)
                    .source(PlanSource.SCHEDULE_AUTO)
                    .sourceSchedule(ScheduleEntity.builder().name("수업").dayOfWeek(DayOfWeek.MONDAY).startedAt(LocalTime.of(9,0)).endedAt(LocalTime.of(10,30)).place("강의실").user(user).build())
                    .build();
            PlanEntryEntity entry2 = PlanEntryEntity.builder()
                    .user(user)
                    .date(date)
                    .startTime(LocalTime.of(14, 0))
                    .endTime(LocalTime.of(15, 30))
                    .location("도서관")
                    .name("자습")
                    .type(PlanItemType.FREE)
                    .source(PlanSource.MANUAL)
                    .build();
            setId(entry1, UUID.randomUUID());
            setId(entry2, UUID.randomUUID());

            when(planEntryRepository.findByUserIdAndDateOrderByStartTimeAsc(userId, date))
                    .thenReturn(List.of(entry1, entry2));

            List<PlanEntryResponse> responses = plannerService.getPlanEntries(userId, date);

            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).startTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(responses.get(1).startTime()).isEqualTo(LocalTime.of(14, 0));
        }
    }

    @Nested
    @DisplayName("regenerateScheduleAutoEntries")
    class RegenerateScheduleAutoEntriesTests {

        @Test
        @DisplayName("시간표 기반으로 SCHEDULE_AUTO 일정을 자동 생성한다")
        void shouldGenerateScheduleAutoEntries() {
            UUID scheduleId = UUID.randomUUID();
            LocalDate today = LocalDate.now();
            SemesterEntity semester = SemesterEntity.builder()
                    .year(today.getYear())
                    .term(SemesterTerm.FIRST)
                    .startedAt(today.minusDays(30))
                    .endedAt(today.plusDays(60))
                    .build();
            setId(semester, UUID.randomUUID());

            ScheduleEntity schedule = ScheduleEntity.builder()
                    .name("데이터베이스")
                    .dayOfWeek(DayOfWeek.MONDAY)
                    .startedAt(LocalTime.of(9, 0))
                    .endedAt(LocalTime.of(10, 30))
                    .place("공학관 301호")
                    .user(user)
                    .semester(semester)
                    .build();
            setId(schedule, scheduleId);

            when(semesterRepository.findCurrentByDate(any(LocalDate.class)))
                    .thenReturn(Optional.of(semester));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(scheduleRepository.findAllByUserId(userId)).thenReturn(List.of(schedule));
            when(planEntryRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

            plannerService.regenerateScheduleAutoEntries(userId);

            // Verify: 기존 미래 SCHEDULE_AUTO 삭제
            verify(planEntryRepository).deleteByUserIdAndSourceAndDateAfter(
                    eq(userId), eq(PlanSource.SCHEDULE_AUTO), any(LocalDate.class));

            // Verify: saveAll 호출됨
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<PlanEntryEntity>> captor = ArgumentCaptor.forClass(List.class);
            verify(planEntryRepository).saveAll(captor.capture());

            List<PlanEntryEntity> savedEntries = captor.getValue();
            assertThat(savedEntries).isNotEmpty();

            // 모든 생성된 엔트리가 SCHEDULE_AUTO이고 sourceSchedule이 설정됨
            for (PlanEntryEntity entry : savedEntries) {
                assertThat(entry.getSource()).isEqualTo(PlanSource.SCHEDULE_AUTO);
                assertThat(entry.getSourceSchedule()).isNotNull();
                assertThat(entry.getSourceSchedule().getId()).isEqualTo(scheduleId);
                assertThat(entry.getDate().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
                assertThat(entry.getStartTime()).isEqualTo(LocalTime.of(9, 0));
                assertThat(entry.getEndTime()).isEqualTo(LocalTime.of(10, 30));
                assertThat(entry.getName()).isEqualTo("데이터베이스");
                assertThat(entry.getType()).isEqualTo(PlanItemType.CLASS);
            }
        }

        @Test
        @DisplayName("시간표가 없으면 자동 생성을 건너뛴다")
        void shouldSkipWhenNoSchedules() {
            LocalDate today = LocalDate.now();
            SemesterEntity semester = SemesterEntity.builder()
                    .year(today.getYear())
                    .term(SemesterTerm.FIRST)
                    .startedAt(today.minusDays(30))
                    .endedAt(today.plusDays(60))
                    .build();
            setId(semester, UUID.randomUUID());

            when(semesterRepository.findCurrentByDate(any(LocalDate.class)))
                    .thenReturn(Optional.of(semester));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(scheduleRepository.findAllByUserId(userId)).thenReturn(List.of());

            plannerService.regenerateScheduleAutoEntries(userId);

            verify(planEntryRepository).deleteByUserIdAndSourceAndDateAfter(
                    eq(userId), eq(PlanSource.SCHEDULE_AUTO), any(LocalDate.class));
            verify(planEntryRepository, never()).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("shouldSendInactivityReminder")
    class InactivityReminderTests {

        @Test
        @DisplayName("3일 이상 MANUAL 일정 미작성 시 true를 반환한다")
        void shouldReturnTrueWhenInactive() {
            when(planEntryRepository.existsManualEntryBetween(eq(userId), any(), any()))
                    .thenReturn(false);

            assertThat(plannerService.shouldSendInactivityReminder(userId)).isTrue();
        }

        @Test
        @DisplayName("최근 3일 내 MANUAL 일정이 있으면 false를 반환한다")
        void shouldReturnFalseWhenActive() {
            when(planEntryRepository.existsManualEntryBetween(eq(userId), any(), any()))
                    .thenReturn(true);

            assertThat(plannerService.shouldSendInactivityReminder(userId)).isFalse();
        }
    }

    // ===== Helper =====

    private static void setId(Object entity, UUID id) {
        try {
            var clazz = entity.getClass();
            while (clazz != null) {
                try {
                    var field = clazz.getDeclaredField("id");
                    field.setAccessible(true);
                    field.set(entity, id);
                    return;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            throw new RuntimeException("id field not found");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
