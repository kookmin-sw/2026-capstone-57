---
layout: default
title: 일기예보
---

<style>
  @import url('https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@400;500;700;800;900&display=swap');

  body {
    background-color: #fef9ef;
    font-family: 'Noto Sans KR', sans-serif;
    scroll-behavior: smooth;
  }

  [id] { scroll-margin-top: 20px; }

  .markdown-body {
    background-color: transparent;
    max-width: 720px;
    margin: 0 auto;
    padding: 24px 20px 110px;
  }

  /* 모든 hr 숨김 */
  .markdown-body hr { display: none; }

  /* ── 페이드인 ── */
  .iy-card-section, .iy-hero { opacity: 0; transform: translateY(20px); transition: opacity 0.6s ease, transform 0.6s ease; }
  .iy-card-section.visible, .iy-hero.visible { opacity: 1; transform: translateY(0); }

  /* ════════════════════════════════════════ */
  /* ── 상단 로고 헤더 ── */
  /* ════════════════════════════════════════ */
  .iy-app-header {
    display: flex; flex-direction: column; align-items: center;
    padding: 12px 0 24px; position: relative;
  }
  .iy-app-header .bell {
    position: absolute; right: 4px; top: 16px;
    width: 36px; height: 36px; border-radius: 50%;
    background: #fff; display: flex; align-items: center; justify-content: center;
    font-size: 18px; box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  }
  .iy-app-logo {
    width: 140px; height: auto; margin-bottom: 4px;
    filter: drop-shadow(0 4px 12px rgba(0,0,0,0.1));
  }
  .iy-app-logo-emoji { font-size: 56px; margin-bottom: 4px; line-height: 1; }
  .iy-app-name {
    font-size: 36px; font-weight: 900; letter-spacing: -0.02em;
    background: linear-gradient(135deg, #5b9eea 0%, #f4b942 100%);
    -webkit-background-clip: text; background-clip: text;
    color: transparent;
  }

  /* ════════════════════════════════════════ */
  /* ── 인사 카드 (히어로) ── */
  /* ════════════════════════════════════════ */
  .iy-hero {
    background: linear-gradient(135deg,  #4696dcd8 0%, #71aee8 100%);
    border-radius: 22px; padding: 22px 24px; position: relative;
    color: #fff; box-shadow: 0 6px 20px rgba(91,158,234,0.18);
  }
  .iy-hero .sun {
    position: absolute; right: 20px; top: 20px;
    width: 22px; height: 22px; border-radius: 50%;
    background: #f4a82a; box-shadow: 0 0 14px rgba(244,168,42,0.7);
  }
  .iy-hero .greet-date { font-size: 13px; opacity: 0.95; font-weight: 600; }
  .iy-hero h1 {
    font-size: 22px; font-weight: 900; color: #fff;
    margin: 6px 0 4px; padding: 0; border: none;
  }
  .iy-hero .greet-desc { font-size: 13px; opacity: 0.95; line-height: 1.5; }
  .iy-hero .badges { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 14px; }
  .iy-hero .badge {
    background: rgba(255,255,255,0.85); color: #2c6fbd;
    border-radius: 99px; padding: 5px 12px;
    font-size: 11px; font-weight: 700;
  }

  /* ════════════════════════════════════════ */
  /* ── 섹션 헤더 ── */
  /* ════════════════════════════════════════ */
  .iy-card-section { margin-top: 28px; }
  .iy-sec-head { padding: 0 4px 12px; display: flex; justify-content: space-between; align-items: baseline; }
  .iy-sec-head .t { font-size: 17px; font-weight: 800; color: #1a1a1a; }
  .iy-sec-head .more { font-size: 12px; color: #999; }
  .iy-sec-sub { padding: 0 4px 8px; font-size: 12px; color: #888; }

  /* ════════════════════════════════════════ */
  /* ── 슬롯 카드 (오늘의 예보 스타일) ── */
  /* ════════════════════════════════════════ */
  .iy-slot {
    background: #fff; border-radius: 16px; padding: 14px 16px; margin-bottom: 10px;
    display: flex; align-items: center; gap: 12px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.04);
    transition: transform 0.2s ease, box-shadow 0.2s ease;
  }
  .iy-slot:hover { transform: translateY(-2px); box-shadow: 0 6px 18px rgba(0,0,0,0.08); }
  .iy-slot.pending { border: 1.5px dashed #e0e0e0; box-shadow: none; }
  .iy-slot-ico {
    width: 32px; height: 32px; border-radius: 50%; background: #fde9a3;
    display: flex; align-items: center; justify-content: center;
    font-size: 14px; color: #fff; flex-shrink: 0; font-weight: 700;
  }
  .iy-slot-ico.gray { background: #f0f0f0; color: #aaa; }
  .iy-slot-ico.blue { background: #aed4f5; }
  .iy-slot-text { flex: 1; font-size: 14px; color: #333; font-weight: 600; }
  .iy-slot-text.gray { color: #aaa; }
  .iy-slot-tag { font-size: 11px; color: #888; display: flex; align-items: center; gap: 4px; }

  /* ════════════════════════════════════════ */
  /* ── 기능 카드 (그리드) ── */
  /* ════════════════════════════════════════ */
  .iy-feature-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
  .iy-feature-card {
    background: #fff; border-radius: 16px; padding: 18px 16px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.04);
    transition: transform 0.2s ease, box-shadow 0.2s ease;
  }
  .iy-feature-card:hover { transform: translateY(-3px); box-shadow: 0 8px 20px rgba(0,0,0,0.1); }
  .iy-feature-card .f-ico {
    width: 40px; height: 40px; border-radius: 12px;
    display: flex; align-items: center; justify-content: center;
    font-size: 22px; margin-bottom: 12px;
  }
  .iy-feature-card.c1 .f-ico { background: #e6f0fa; }
  .iy-feature-card.c2 .f-ico { background: #fff3e0; }
  .iy-feature-card.c3 .f-ico { background: #f0faf5; }
  .iy-feature-card.c4 .f-ico { background: #fdf2ff; }
  .iy-feature-card .f-title { font-size: 14px; font-weight: 800; color: #1a1a1a; margin-bottom: 6px; }
  .iy-feature-card .f-desc { font-size: 12px; color: #888; line-height: 1.6; }

  /* ════════════════════════════════════════ */
  /* ── 5단계 흐름 ── */
  /* ════════════════════════════════════════ */
  .iy-flow-card {
    background: #fff; border-radius: 18px; padding: 20px 18px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  }
  .iy-stages {
    display: flex; justify-content: space-between; padding: 0 4px;
  }
  .iy-stage-item { display: flex; flex-direction: column; align-items: center; gap: 6px; flex: 1; position: relative; }
  .iy-stage-item:not(:last-child)::after {
    content: ''; position: absolute; top: 18px; right: -50%;
    width: 100%; height: 2px; background: #eee; z-index: 0;
  }
  .iy-stage-item.done:not(:last-child)::after { background: #aed4f5; }
  .iy-stage-circle {
    width: 38px; height: 38px; border-radius: 50%;
    background: #f5f5f5; color: #ccc;
    display: flex; align-items: center; justify-content: center; font-size: 16px;
    position: relative; z-index: 1;
  }
  .iy-stage-circle.done { background: #d4f0d4; color: #4ab84a; }
  .iy-stage-circle.active { background: linear-gradient(135deg, #aed4f5, #5b9eea); color: #fff; box-shadow: 0 4px 12px rgba(91,158,234,0.4); }
  .iy-stage-label { font-size: 11px; color: #555; font-weight: 700; }
  .iy-flow-desc {
    margin-top: 16px; padding-top: 16px; border-top: 1px dashed #f0f0f0;
    font-size: 13px; color: #888; line-height: 1.7; text-align: center;
  }

  /* ════════════════════════════════════════ */
  /* ── 이용 방법 (스텝) ── */
  /* ════════════════════════════════════════ */
  .iy-step {
    background: #fff; border-radius: 16px; padding: 14px 16px; margin-bottom: 10px;
    display: flex; gap: 14px; align-items: flex-start;
    box-shadow: 0 2px 8px rgba(0,0,0,0.04);
    transition: transform 0.2s ease, box-shadow 0.2s ease;
  }
  .iy-step:hover { transform: translateX(4px); box-shadow: 0 6px 18px rgba(0,0,0,0.08); }
  .iy-step-num {
    width: 36px; height: 36px; border-radius: 12px;
    background: linear-gradient(135deg, #aed4f5, #5b9eea);
    color: #fff; font-weight: 900; font-size: 16px;
    display: flex; align-items: center; justify-content: center;
    flex-shrink: 0;
  }
  .iy-step-body { flex: 1; }
  .iy-step-title { font-size: 14px; font-weight: 800; color: #1a1a1a; margin-bottom: 4px; }
  .iy-step-desc { font-size: 12px; color: #888; line-height: 1.6; }

  /* ════════════════════════════════════════ */
  /* ── 일정 카드 (다가오는 일정 스타일) ── */
  /* ════════════════════════════════════════ */
  .iy-event {
    background: #fff; border-radius: 16px; padding: 14px 16px; margin-bottom: 10px;
    display: flex; align-items: center; gap: 12px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  }
  .iy-event-ico {
    width: 40px; height: 40px; border-radius: 12px; background: #e6f0fa;
    display: flex; align-items: center; justify-content: center; font-size: 18px; flex-shrink: 0;
  }
  .iy-event-body { flex: 1; }
  .iy-event-title { font-size: 14px; font-weight: 800; color: #1a1a1a; }
  .iy-event-title .cnt { background: #fcd34d; color: #fff; font-size: 10px;
    padding: 2px 7px; border-radius: 8px; margin-left: 6px; font-weight: 700; }
  .iy-event-sub { font-size: 11px; color: #888; margin-top: 3px; }
  .iy-event-arrow { color: #ccc; font-size: 18px; }

  /* ════════════════════════════════════════ */
  /* ── 아키텍처 카드 ── */
  /* ════════════════════════════════════════ */
  .iy-arch {
    background: #fff; border-radius: 18px; padding: 18px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  }
  .iy-arch-layer { margin-bottom: 14px; }
  .iy-arch-layer:last-child { margin-bottom: 0; }
  .iy-arch-layer .layer-label {
    font-size: 11px; font-weight: 800; color: #aaa;
    text-transform: uppercase; letter-spacing: 0.08em; margin-bottom: 8px;
  }
  .iy-arch-boxes { display: flex; flex-wrap: wrap; gap: 6px; }
  .iy-arch-box {
    border-radius: 10px; padding: 7px 12px; font-size: 12px; font-weight: 700;
  }
  .iy-arch-box.client  { background: #e6f0fa; color: #2c6fbd; }
  .iy-arch-box.service { background: #f0faf5; color: #1a7a50; }
  .iy-arch-box.data    { background: #fdf2ff; color: #7a1aaa; }
  .iy-arch-box.ai      { background: #fff8e6; color: #9a6200; }
  .iy-arch-divider {
    display: flex; align-items: center; gap: 8px; margin: 8px 0; color: #ddd; font-size: 11px;
  }
  .iy-arch-divider::before, .iy-arch-divider::after {
    content: ''; flex: 1; height: 1px; background: #f0f0f0;
  }

  /* ════════════════════════════════════════ */
  /* ── 기술 스택 칩 ── */
  /* ════════════════════════════════════════ */
  .iy-stack-group { background: #fff; border-radius: 16px; padding: 14px 16px; margin-bottom: 10px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
  .iy-stack-group .sg-label { font-size: 11px; font-weight: 800; color: #aaa; margin-bottom: 8px; text-transform: uppercase; letter-spacing: 0.06em; }
  .iy-chips { display: flex; flex-wrap: wrap; gap: 6px; }
  .iy-chip {
    background: #f5f5f5; border-radius: 99px; padding: 5px 12px;
    font-size: 12px; color: #555; font-weight: 600;
  }
  .iy-chip.blue   { background: #e6f0fa; color: #2c6fbd; }
  .iy-chip.amber  { background: #fff3e0; color: #9a6200; }
  .iy-chip.green  { background: #f0faf5; color: #1a7a50; }
  .iy-chip.purple { background: #fdf2ff; color: #7a1aaa; }

  /* ════════════════════════════════════════ */
  /* ── 팀 카드 ── */
  /* ════════════════════════════════════════ */
  .iy-team-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
  .iy-team-card {
    background: #fff; border-radius: 16px; padding: 14px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.04);
    transition: transform 0.2s ease, box-shadow 0.2s ease;
  }
  .iy-team-card:hover { transform: translateY(-3px); box-shadow: 0 8px 20px rgba(0,0,0,0.1); }
  .iy-team-card .t-avatar {
    width: 44px; height: 44px; border-radius: 14px;
    display: flex; align-items: center; justify-content: center;
    font-size: 22px; margin-bottom: 10px;
  }
  .iy-team-card.c1 .t-avatar { background: #fde9a3; }
  .iy-team-card.c2 .t-avatar { background: #aed4f5; }
  .iy-team-card.c3 .t-avatar { background: #d4f0d4; }
  .iy-team-card.c4 .t-avatar { background: #f0d4ee; }
  .iy-team-card .t-name { font-size: 14px; font-weight: 800; color: #1a1a1a; margin-bottom: 2px; }
  .iy-team-card .t-role { font-size: 11px; color: #5b9eea; font-weight: 700; margin-bottom: 8px; }
  .iy-team-card .t-desc { font-size: 11px; color: #888; line-height: 1.6; }

  /* ════════════════════════════════════════ */
  /* ── 깃허브 링크 버튼 ── */
  /* ════════════════════════════════════════ */
  .iy-links { display: flex; gap: 10px; }
  .iy-link-btn {
    flex: 1; display: flex; align-items: center; justify-content: center; gap: 8px;
    background: #fff; border-radius: 16px; padding: 16px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.04);
    text-decoration: none; font-size: 13px; font-weight: 800; color: #1a1a1a;
    transition: transform 0.2s ease, box-shadow 0.2s ease;
  }
  .iy-link-btn:hover { transform: translateY(-2px); box-shadow: 0 6px 18px rgba(0,0,0,0.1); }
  .iy-link-btn .lb-icon { font-size: 22px; }
  .iy-link-btn.primary  { background: #1a1a1a; color: #fff; }

  /* ════════════════════════════════════════ */
  /* ── 하단 네비게이션 (모바일 앱 느낌) ── */
  /* ════════════════════════════════════════ */
  .iy-nav {
    position: fixed; bottom: 0; left: 50%; transform: translateX(-50%);
    width: 100%; max-width: 720px;
    background: #fff; border-top: 1px solid #f0f0f0;
    display: flex; justify-content: space-around;
    padding: 12px 0 18px;
    box-shadow: 0 -4px 20px rgba(0,0,0,0.06); z-index: 100;
  }
  .iy-nav a {
    display: flex; flex-direction: column; align-items: center; gap: 4px;
    text-decoration: none; color: #bbb; font-size: 11px; font-weight: 700;
    transition: color 0.2s ease, transform 0.2s ease;
    flex: 1;
  }
  .iy-nav a:hover { transform: translateY(-2px); color: #5b9eea; }
  .iy-nav a.active { color: #5b9eea; }
  .iy-nav a .nav-icon { font-size: 22px; }
  .iy-nav a.active .nav-icon { transform: scale(1.1); }

  /* ════════════════════════════════════════ */
  /* ── 반응형 ── */
  /* ════════════════════════════════════════ */
  @media (max-width: 480px) {
    .iy-feature-grid, .iy-team-grid { grid-template-columns: 1fr; }
    .iy-app-name { font-size: 30px; }
  }
</style>


<!-- ════════════════════════════════════════ -->
<!-- ── 상단 앱 헤더 (로고) ──                 -->
<!-- ════════════════════════════════════════ -->
<div id="home" class="iy-app-header">
  <img class="iy-app-logo" src="assets/logo.png" alt="일기예보 로고" />
  <div class="bell">🔔</div>
</div>


<!-- ════════════════════════════════════════ -->
<!-- ── 인사 카드 (히어로) ──                   -->
<!-- ════════════════════════════════════════ -->
<div class="iy-hero">
  <div class="sun"></div>
  <div class="greet-date">캡스톤 디자인 · 57팀</div>
  <h1>일기로 예견하는<br>보석같은 만남</h1>
  <div class="greet-desc">수업 시간표와 캠퍼스 동선 교집합으로<br>자연스러운 만남의 기회를 만들어드려요</div>
  <div class="badges">
    <span class="badge">🎓 대학생 전용</span>
    <span class="badge">📍 동선 기반</span>
    <span class="badge">🛡️ 5단계 안전</span>
  </div>
</div>


<!-- ════════════════════════════════════════ -->
<!-- ── 오늘의 예보 (예시 슬롯) ──              -->
<!-- ════════════════════════════════════════ -->
<div class="iy-card-section">
  <div class="iy-sec-head">
    <div class="t">오늘의 예보</div>
    <div class="more">더보기 ›</div>
  </div>
  <div class="iy-sec-sub">이렇게 매칭이 진행돼요</div>

  <div class="iy-slot">
    <div class="iy-slot-ico">✓</div>
    <div class="iy-slot-text">매칭 완료 · 5단계 진행 중</div>
    <div class="iy-slot-tag">취미 ✨</div>
  </div>

  <div class="iy-slot pending">
    <div class="iy-slot-ico gray">🕐</div>
    <div class="iy-slot-text gray">월요일에 새로운 만남</div>
    <div class="iy-slot-tag">관심사 🕐</div>
  </div>

  <div class="iy-slot">
    <div class="iy-slot-ico blue">⚡</div>
    <div class="iy-slot-text">퀵매칭 · 즉시 만남</div>
    <div class="iy-slot-tag">이상형 ✨</div>
  </div>
</div>


<!-- ════════════════════════════════════════ -->
<!-- ── 핵심 기능 ──                           -->
<!-- ════════════════════════════════════════ -->
<div id="features" class="iy-card-section">
  <div class="iy-sec-head"><div class="t">핵심 기능</div></div>

  <div class="iy-feature-grid">
    <div class="iy-feature-card c1">
      <div class="f-ico">🗺️</div>
      <div class="f-title">동선 기반 매칭</div>
      <div class="f-desc">시간표와 캠퍼스 공간 데이터로 자연스레 마주칠 상대를 추천해요</div>
    </div>
    <div class="iy-feature-card c2">
      <div class="f-ico">🛡️</div>
      <div class="f-title">5단계 안전</div>
      <div class="f-desc">서로 동의해야 다음 단계로 진행되는 점진적 시스템</div>
    </div>
    <div class="iy-feature-card c3">
      <div class="f-ico">🤖</div>
      <div class="f-title">AI 콘텐츠</div>
      <div class="f-desc">Amazon Bedrock이 프로필 기반 퀴즈·미션·회고를 자동 생성</div>
    </div>
    <div class="iy-feature-card c4">
      <div class="f-ico">🌱</div>
      <div class="f-title">성장형 소셜</div>
      <div class="f-desc">일기·플래너 작성과 상호작용으로 경험치를 쌓아요</div>
    </div>
  </div>
</div>


<!-- ════════════════════════════════════════ -->
<!-- ── 5단계 상호작용 ──                       -->
<!-- ════════════════════════════════════════ -->
<div id="flow" class="iy-card-section">
  <div class="iy-sec-head"><div class="t">5단계 상호작용</div></div>
  <div class="iy-sec-sub">한 단계씩 조심스럽게 가까워져요</div>

  <div class="iy-flow-card">
    <div class="iy-stages">
      <div class="iy-stage-item done">
        <div class="iy-stage-circle done">❓</div>
        <div class="iy-stage-label">퀴즈</div>
      </div>
      <div class="iy-stage-item done">
        <div class="iy-stage-circle done">💬</div>
        <div class="iy-stage-label">채팅</div>
      </div>
      <div class="iy-stage-item done">
        <div class="iy-stage-circle done">🎮</div>
        <div class="iy-stage-label">게임</div>
      </div>
      <div class="iy-stage-item done">
        <div class="iy-stage-circle done">🗺️</div>
        <div class="iy-stage-label">미션</div>
      </div>
      <div class="iy-stage-item">
        <div class="iy-stage-circle active">📖</div>
        <div class="iy-stage-label">회고</div>
      </div>
    </div>
    <div class="iy-flow-desc">
      매 단계마다 <strong>양쪽이 동의</strong>해야 다음으로 진행돼요<br>
      거부하면 매칭이 안전하게 종료되고, 신고·차단도 언제든 가능해요
    </div>
  </div>
</div>


<!-- ════════════════════════════════════════ -->
<!-- ── 이용 방법 ──                           -->
<!-- ════════════════════════════════════════ -->
<div id="howto" class="iy-card-section">
  <div class="iy-sec-head"><div class="t">이용 방법</div></div>

  <div class="iy-step">
    <div class="iy-step-num">1</div>
    <div class="iy-step-body">
      <div class="iy-step-title">대학 이메일로 가입</div>
      <div class="iy-step-desc">학교 이메일 인증으로 재학생 확인 · 취미·관심사 프로필 설정</div>
    </div>
  </div>

  <div class="iy-step">
    <div class="iy-step-num">2</div>
    <div class="iy-step-body">
      <div class="iy-step-title">시간표 등록</div>
      <div class="iy-step-desc">에브리타임 시간표 연동 · 매일 플래너로 더 정확한 동선 매칭</div>
    </div>
  </div>

  <div class="iy-step">
    <div class="iy-step-num">3</div>
    <div class="iy-step-body">
      <div class="iy-step-title">월요일 자정 매칭 성사</div>
      <div class="iy-step-desc">배치 매칭 시스템이 동선 겹치는 상대를 자동 매칭 (5일 주기)</div>
    </div>
  </div>

  <div class="iy-step">
    <div class="iy-step-num">4</div>
    <div class="iy-step-body">
      <div class="iy-step-title">5단계 상호작용 진행</div>
      <div class="iy-step-desc">퀴즈 → 채팅 → 게임 → 미션 → 회고 순으로 점진적 관계 형성</div>
    </div>
  </div>

  <div class="iy-step">
    <div class="iy-step-num">5</div>
    <div class="iy-step-body">
      <div class="iy-step-title">경험치로 레벨업</div>
      <div class="iy-step-desc">활동마다 경험치 축적 · 새로운 매칭 슬롯 해금</div>
    </div>
  </div>
</div>

<!-- ════════════════════════════════════════ -->
<!-- ── 핵심 지표 ──                           -->
<!-- ════════════════════════════════════════ -->
<div class="iy-card-section">
  <div class="iy-sec-head"><div class="t">핵심 지표</div></div>
  <div class="iy-sec-sub">일기예보가 만들어갈 숫자들</div>

  <div style="display:grid; grid-template-columns:1fr 1fr; gap:10px;">
    <div style="background:#fff; border-radius:16px; padding:18px 16px; box-shadow:0 2px 8px rgba(0,0,0,0.04); text-align:center;">
      <div style="font-size:28px; font-weight:900; color:#5b9eea; letter-spacing:-0.03em;">500+</div>
      <div style="font-size:11px; color:#aaa; font-weight:700; margin-top:4px;">타깃 초기 사용자</div>
    </div>
    <div style="background:#fff; border-radius:16px; padding:18px 16px; box-shadow:0 2px 8px rgba(0,0,0,0.04); text-align:center;">
      <div style="font-size:28px; font-weight:900; color:#f4b942; letter-spacing:-0.03em;">5단계</div>
      <div style="font-size:11px; color:#aaa; font-weight:700; margin-top:4px;">점진적 안전 시스템</div>
    </div>
    <div style="background:#fff; border-radius:16px; padding:18px 16px; box-shadow:0 2px 8px rgba(0,0,0,0.04); text-align:center;">
      <div style="font-size:28px; font-weight:900; color:#4ab84a; letter-spacing:-0.03em;">5일</div>
      <div style="font-size:11px; color:#aaa; font-weight:700; margin-top:4px;">매칭 주기 (월~금)</div>
    </div>
    <div style="background:#fff; border-radius:16px; padding:18px 16px; box-shadow:0 2px 8px rgba(0,0,0,0.04); text-align:center;">
      <div style="font-size:28px; font-weight:900; color:#b45bd4; letter-spacing:-0.03em;">100%</div>
      <div style="font-size:11px; color:#aaa; font-weight:700; margin-top:4px;">재학생 인증 매칭</div>
    </div>
  </div>
</div>


<!-- ════════════════════════════════════════ -->
<!-- ── 앱 화면 미리보기 ──                    -->
<!-- ════════════════════════════════════════ -->
<div class="iy-card-section">
  <div class="iy-sec-head"><div class="t">앱 화면 미리보기</div></div>
  <div class="iy-sec-sub">실제 앱 화면이에요</div>

  <div style="display:flex; gap:16px; overflow-x:auto; padding-bottom:8px; -webkit-overflow-scrolling:touch; justify-content:center;">
    <div style="flex-shrink:0; text-align:center;">
      <img src="assets/screen1.png" alt="홈 화면"
        style="width:200px; border-radius:22px; box-shadow:0 8px 28px rgba(0,0,0,0.12);" />
      <div style="font-size:11px; color:#888; font-weight:700; margin-top:8px;">홈 화면</div>
    </div>
    <div style="flex-shrink:0; text-align:center;">
      <img src="assets/screen2.png" alt="로그인 화면"
        style="width:200px; border-radius:22px; box-shadow:0 8px 28px rgba(0,0,0,0.12);" />
      <div style="font-size:11px; color:#888; font-weight:700; margin-top:8px;">로그인 화면</div>
    </div>
  </div>
</div>


<!-- ════════════════════════════════════════ -->
<!-- ── 차별점 비교 ──                         -->
<!-- ════════════════════════════════════════ -->
<div class="iy-card-section">
  <div class="iy-sec-head"><div class="t">기존 앱과의 차별점</div></div>
  <div class="iy-sec-sub">일기예보만의 특별함</div>

  <div style="background:#fff; border-radius:18px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.04);">
    <!-- 헤더 -->
    <div style="display:grid; grid-template-columns:2fr 1fr 1fr 1fr; background:#f8f8f8; padding:10px 14px; gap:4px;">
      <div style="font-size:11px; font-weight:800; color:#aaa;">항목</div>
      <div style="font-size:11px; font-weight:900; color:#1558a0; text-align:center;">일기예보</div>
      <div style="font-size:11px; font-weight:700; color:#aaa; text-align:center;">소개팅앱</div>
      <div style="font-size:11px; font-weight:700; color:#aaa; text-align:center;">에브리타임</div>
    </div>

    <!-- 행들 -->
    <div style="display:grid; grid-template-columns:2fr 1fr 1fr 1fr; padding:11px 14px; border-top:1px solid #f5f5f5; align-items:center; gap:4px;">
      <div style="font-size:12px; font-weight:700; color:#333;">재학생 인증</div>
      <div style="text-align:center; font-size:16px;">✅</div>
      <div style="text-align:center; font-size:16px;">❌</div>
      <div style="text-align:center; font-size:16px;">✅</div>
    </div>
    <div style="display:grid; grid-template-columns:2fr 1fr 1fr 1fr; padding:11px 14px; border-top:1px solid #f5f5f5; background:#f0f6ff; align-items:center; gap:4px;">
      <div style="font-size:12px; font-weight:700; color:#333;">동선 기반 매칭</div>
      <div style="text-align:center; font-size:16px;">✅</div>
      <div style="text-align:center; font-size:16px;">❌</div>
      <div style="text-align:center; font-size:16px;">❌</div>
    </div>
    <div style="display:grid; grid-template-columns:2fr 1fr 1fr 1fr; padding:11px 14px; border-top:1px solid #f5f5f5; align-items:center; gap:4px;">
      <div style="font-size:12px; font-weight:700; color:#333;">점진적 공개</div>
      <div style="text-align:center; font-size:16px;">✅</div>
      <div style="text-align:center; font-size:16px;">❌</div>
      <div style="text-align:center; font-size:16px;">❌</div>
    </div>
    <div style="display:grid; grid-template-columns:2fr 1fr 1fr 1fr; padding:11px 14px; border-top:1px solid #f5f5f5; background:#f0f6ff; align-items:center; gap:4px;">
      <div style="font-size:12px; font-weight:700; color:#333;">AI 콘텐츠 생성</div>
      <div style="text-align:center; font-size:16px;">✅</div>
      <div style="text-align:center; font-size:16px;">❌</div>
      <div style="text-align:center; font-size:16px;">❌</div>
    </div>
    <div style="display:grid; grid-template-columns:2fr 1fr 1fr 1fr; padding:11px 14px; border-top:1px solid #f5f5f5; align-items:center; gap:4px;">
      <div style="font-size:12px; font-weight:700; color:#333;">성장형 경험치</div>
      <div style="text-align:center; font-size:16px;">✅</div>
      <div style="text-align:center; font-size:16px;">❌</div>
      <div style="text-align:center; font-size:16px;">❌</div>
    </div>
    <div style="display:grid; grid-template-columns:2fr 1fr 1fr 1fr; padding:11px 14px; border-top:1px solid #f5f5f5; background:#f0f6ff; align-items:center; gap:4px;">
      <div style="font-size:12px; font-weight:700; color:#333;">신고 · 차단</div>
      <div style="text-align:center; font-size:16px;">✅</div>
      <div style="text-align:center; font-size:16px;">✅</div>
      <div style="text-align:center; font-size:16px;">✅</div>
    </div>
  </div>
</div>

<!-- ════════════════════════════════════════ -->
<!-- ── 아키텍처 ──                            -->
<!-- ════════════════════════════════════════ -->
<div class="iy-card-section">
  <div class="iy-sec-head"><div class="t">아키텍처</div></div>
  <div class="iy-sec-sub">단일 Spring Boot · AWS 클라우드 네이티브</div>

  <div class="iy-arch">
    <div class="iy-arch-layer">
      <div class="layer-label">Client</div>
      <div class="iy-arch-boxes">
        <div class="iy-arch-box client">React Native 모바일 앱</div>
      </div>
    </div>
    <div class="iy-arch-divider">↓</div>
    <div class="iy-arch-layer">
      <div class="layer-label">Backend · Spring Boot</div>
      <div class="iy-arch-boxes">
        <div class="iy-arch-box service">인증 · 사용자</div>
        <div class="iy-arch-box service">매칭 엔진</div>
        <div class="iy-arch-box service">상호작용</div>
        <div class="iy-arch-box service">채팅 WebSocket</div>
        <div class="iy-arch-box service">게임 WebSocket</div>
        <div class="iy-arch-box service">경험치 · 알림</div>
      </div>
    </div>
    <div class="iy-arch-divider">↓</div>
    <div class="iy-arch-layer">
      <div class="layer-label">Data Layer</div>
      <div class="iy-arch-boxes">
        <div class="iy-arch-box data">MySQL Aurora</div>
        <div class="iy-arch-box data">Redis</div>
        <div class="iy-arch-box data">Amazon SQS</div>
        <div class="iy-arch-box data">Amazon S3</div>
      </div>
    </div>
    <div class="iy-arch-divider">↕ AWS SDK v2</div>
    <div class="iy-arch-layer">
      <div class="layer-label">AI</div>
      <div class="iy-arch-boxes">
        <div class="iy-arch-box ai">Amazon Bedrock (LLM)</div>
      </div>
    </div>
  </div>
</div>


<!-- ════════════════════════════════════════ -->
<!-- ── 기술 스택 ──                           -->
<!-- ════════════════════════════════════════ -->
<div class="iy-card-section">
  <div class="iy-sec-head"><div class="t">기술 스택</div></div>

  <div class="iy-stack-group">
    <div class="sg-label">Backend</div>
    <div class="iy-chips">
      <span class="iy-chip blue">Spring Boot 3</span>
      <span class="iy-chip blue">Spring Security</span>
      <span class="iy-chip blue">Spring Data JPA</span>
      <span class="iy-chip blue">WebSocket</span>
      <span class="iy-chip blue">Spring Batch</span>
    </div>
  </div>

  <div class="iy-stack-group">
    <div class="sg-label">AWS & Infra</div>
    <div class="iy-chips">
      <span class="iy-chip amber">Amazon Bedrock</span>
      <span class="iy-chip amber">Amazon SQS</span>
      <span class="iy-chip amber">Amazon S3</span>
      <span class="iy-chip amber">AWS SDK v2</span>
    </div>
  </div>

  <div class="iy-stack-group">
    <div class="sg-label">Database</div>
    <div class="iy-chips">
      <span class="iy-chip green">MySQL 8.0</span>
      <span class="iy-chip green">Redis</span>
      <span class="iy-chip green">Flyway</span>
    </div>
  </div>

  <div class="iy-stack-group">
    <div class="sg-label">Testing</div>
    <div class="iy-chips">
      <span class="iy-chip purple">JUnit 5</span>
      <span class="iy-chip purple">Mockito</span>
      <span class="iy-chip purple">jqwik (PBT)</span>
      <span class="iy-chip purple">Testcontainers</span>
    </div>
  </div>

  <div class="iy-stack-group">
    <div class="sg-label">Client · Docs</div>
    <div class="iy-chips">
      <span class="iy-chip">React Native</span>
      <span class="iy-chip">Next.js</span>
      <span class="iy-chip">Springdoc Swagger</span>
      <span class="iy-chip">JWT</span>
    </div>
  </div>
</div>


<!-- ════════════════════════════════════════ -->
<!-- ── 팀 소개 ──                             -->
<!-- ════════════════════════════════════════ -->
<div id="team" class="iy-card-section">
  <div class="iy-sec-head"><div class="t">팀 소개</div></div>

  <div class="iy-team-grid">
    <div class="iy-team-card c1">
      <div class="t-avatar">🌸</div>
      <div class="t-name">김아리</div>
      <div class="t-role">매칭 · 인증</div>
      <div class="t-desc">이메일 인증 · JWT · 배치 매칭 스케줄러 · 슬롯 관리</div>
    </div>
    <div class="iy-team-card c2">
      <div class="t-avatar">⚡</div>
      <div class="t-name">선현승</div>
      <div class="t-role">실시간 상호작용</div>
      <div class="t-desc">WebSocket 게임 · Redis 상태 동기화 · FSM 파이프라인</div>
    </div>
    <div class="iy-team-card c3">
      <div class="t-avatar">📔</div>
      <div class="t-name">정영미</div>
      <div class="t-role">일상 기록 · 퀴즈</div>
      <div class="t-desc">플래너 · 일기 · 회고 · SQS 힌트 · 경험치 시스템</div>
    </div>
    <div class="iy-team-card c4">
      <div class="t-avatar">🤖</div>
      <div class="t-name">황찬우</div>
      <div class="t-role">AI · 공간 데이터</div>
      <div class="t-desc">Bedrock LLM · 프롬프트 엔지니어링 · AI 미션 생성</div>
    </div>
  </div>
</div>


<!-- ════════════════════════════════════════ -->
<!-- ── 링크 ──                                -->
<!-- ════════════════════════════════════════ -->
<div id="links" class="iy-card-section">
  <div class="iy-sec-head"><div class="t">링크</div></div>

  <div class="iy-links">
    <a class="iy-link-btn primary" href="https://github.com/kookmin-sw/2026-capstone-57" target="_blank">
      <span class="lb-icon">🐙</span>
      <div>
        <div style="font-size:10px;opacity:.6;font-weight:400;">소스 코드</div>
        GitHub
      </div>
    </a>
    <a class="iy-link-btn" href="https://kookmin-sw.github.io/2026-capstone-57/" target="_blank">
      <span class="lb-icon">🏠</span>
      <div>
        <div style="font-size:10px;color:#aaa;font-weight:400;">팀 페이지</div>
        홈페이지
      </div>
    </a>
  </div>
</div>

<p style="text-align:center;font-size:11px;color:#bbb;margin-top:24px;">
  국민대학교 2026 캡스톤 디자인 프로젝트 · 57팀
</p>


<!-- ════════════════════════════════════════ -->
<!-- ── 하단 네비게이션 ──                      -->
<!-- ════════════════════════════════════════ -->
<nav class="iy-nav">
  <a href="#home" class="active"><span class="nav-icon">🏠</span>홈</a>
  <a href="#features"><span class="nav-icon">💚</span>기능</a>
  <a href="#flow"><span class="nav-icon">📅</span>흐름</a>
  <a href="#howto"><span class="nav-icon">📔</span>방법</a>
  <a href="#team"><span class="nav-icon">👤</span>팀</a>
</nav>


<script>
// 새로고침 시 맨 위로
if (history.scrollRestoration) history.scrollRestoration = 'manual';
window.scrollTo(0, 0);

// 페이드인
var observer = new IntersectionObserver(function(entries) {
  entries.forEach(function(e) {
    if (e.isIntersecting) e.target.classList.add('visible');
  });
}, { threshold: 0.1 });
document.querySelectorAll('.iy-card-section, .iy-hero').forEach(function(el) {
  observer.observe(el);
});

// 네비 활성화
var sections = ['home','features','flow','howto','team'];
function updateNav() {
  var current = 'home';
  for (var i = sections.length - 1; i >= 0; i--) {
    var el = document.getElementById(sections[i]);
    if (el) {
      var rect = el.getBoundingClientRect();
      if (rect.top <= 150) { current = sections[i]; break; }
    }
  }
  document.querySelectorAll('.iy-nav a').forEach(function(a) {
    a.classList.remove('active');
    if (a.getAttribute('href') === '#' + current) {
      a.classList.add('active');
    }
  });
}
window.addEventListener('scroll', updateNav);
updateNav();
</script>
