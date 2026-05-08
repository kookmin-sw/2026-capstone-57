---
layout: default
title: 일기예보
---

<style>
  @import url('https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@400;500;700;900&display=swap');

  body { background-color: #fdf8f2; font-family: 'Noto Sans KR', sans-serif; scroll-behavior: smooth; }

  [id] { scroll-margin-top: 20px; }

  /* ── 스크롤 페이드인 ── */
  .iy-section, .iy-hero { opacity: 0; transform: translateY(30px); transition: opacity 0.7s ease, transform 0.7s ease; }
  .iy-section.visible, .iy-hero.visible { opacity: 1; transform: translateY(0); }

  /* ── 숫자 하이라이트 ── */
  .iy-stats {
    display: flex; justify-content: space-around; gap: 8px;
    background: linear-gradient(135deg, #e8eeff, #f3eeff);
    border: 1px solid #ddd8f0;
    border-radius: 16px; padding: 28px 16px; margin-top: 28px;
    box-shadow: 0 2px 12px rgba(124,58,237,0.06);
  }
  .iy-stat { text-align: center; }
  .iy-stat .num { font-size: 32px; font-weight: 900; color: #4338ca; line-height: 1.2; }
  .iy-stat .label { font-size: 13px; color: #888; margin-top: 6px; }

  /* ── 네비 active 하이라이트 ── */
  .iy-nav a.now { color: #1a56db; }
  .iy-nav a.now .nav-icon { transform: scale(1.15); }
  .iy-nav a { transition: color 0.2s; }
  .iy-nav a .nav-icon { transition: transform 0.2s; }

  .markdown-body {
    background-color: transparent;
    max-width: 780px;
    margin: 0 auto;
    padding: 0 24px 100px;
  }

  .iy-section { margin-top: 48px; }

  .iy-section-header {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    margin-bottom: 18px;
  }
  .iy-section-header .title   { font-size: 24px; font-weight: 700; color: #1a1a1a; }
  .iy-section-header .subtitle{ font-size: 15px; color: #aaa; margin-top: 3px; }

  /* ── 히어로 ── */
  .iy-hero {
    background: linear-gradient(135deg, #eaf4ff 0%, #fff8ee 100%);
    border-radius: 24px; padding: 48px 36px; margin-top: 20px;
    text-align: center; box-shadow: 0 4px 20px rgba(59,138,222,0.08);
  }
  .iy-hero .tagline { font-size: 16px; color: #3b8ade; font-weight: 500; margin-bottom: 14px; }
  .iy-hero .hero-logo {
    font-size: 64px; margin-bottom: 8px; line-height: 1;
  }
  .iy-hero .hero-logo-text {
    font-size: 40px; font-weight: 900;
    background: linear-gradient(135deg, #1a56db, #7c3aed);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    margin-bottom: 16px; letter-spacing: -0.02em;
  }
  .iy-hero h2 { font-size: 28px; font-weight: 700; color: #1a1a1a; line-height: 1.4; margin: 0 0 16px; border: none; }
  .iy-hero .desc { font-size: 16px; color: #666; line-height: 1.8; }
  .iy-hero .badges { display: flex; justify-content: center; gap: 10px; flex-wrap: wrap; margin-top: 24px; }
  .iy-hero .badge {
    background: #fff; border-radius: 99px; padding: 8px 18px;
    font-size: 15px; color: #555; box-shadow: 0 2px 8px rgba(0,0,0,0.07);
    transition: transform 0.2s ease, box-shadow 0.2s ease;
  }
  .iy-hero .badge:hover { transform: scale(1.07); box-shadow: 0 4px 14px rgba(0,0,0,0.12); }

  /* ── 매칭 카드 ── */
  .iy-card {
    background: #fff; border-radius: 18px; padding: 18px 22px; margin-bottom: 12px;
    box-shadow: 0 2px 10px rgba(0,0,0,0.06);
    display: flex; align-items: center; gap: 16px; position: relative;
  }
  .iy-card.pending   { background: #fff; border: 1.5px dashed #ddd; box-shadow: none; }
  .iy-card.completed { background: #fffbf0; box-shadow: 0 2px 10px rgba(255,200,50,0.10); }

  .iy-avatar {
    width: 52px; height: 52px; border-radius: 50%; background: #fdecd0;
    display: flex; align-items: center; justify-content: center; font-size: 24px; flex-shrink: 0;
  }
  .iy-avatar.gray   { background: #f0f0f0; color: #bbb; font-size: 20px; }
  .iy-avatar.yellow { background: #ffd84d; color: #fff; }

  .iy-card-body { flex: 1; min-width: 0; }
  .iy-card-body .name           { font-size: 17px; font-weight: 700; color: #1a1a1a; margin-bottom: 2px; }
  .iy-card-body .stage          { font-size: 14px; color: #3b8ade; margin-bottom: 8px; }
  .iy-card-body .pending-text   { font-size: 16px; color: #bbb; }
  .iy-card-body .completed-text { font-size: 17px; font-weight: 700; color: #1a1a1a; }

  .iy-progress { display: flex; gap: 5px; margin-top: 6px; }
  .iy-progress .bar { flex: 1; height: 6px; border-radius: 99px; background: #e8e8e8; }
  .iy-progress .bar.done   { background: #3b8ade; }
  .iy-progress .bar.active { background: #a8c8f0; }

  .iy-badge {
    position: absolute; right: 16px; top: 16px;
    display: flex; flex-direction: column; align-items: center; gap: 4px;
  }
  .iy-badge .icon {
    width: 32px; height: 32px; border-radius: 50%; background: #f5f5f5;
    display: flex; align-items: center; justify-content: center; font-size: 16px;
  }
  .iy-badge .icon.yellow { background: #ffd84d; color: #fff; }
  .iy-badge .label { font-size: 10px; color: #aaa; }

  /* ── 핵심 기능 ── */
  .iy-feature-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
  .iy-feature-card {
    background: #fff; border-radius: 18px; padding: 26px 22px;
    box-shadow: 0 2px 10px rgba(0,0,0,0.05);
    transition: transform 0.2s ease, box-shadow 0.2s ease;
  }
  .iy-feature-card:hover { transform: scale(1.04); box-shadow: 0 6px 20px rgba(0,0,0,0.1); }
  .iy-feature-card .f-icon  { font-size: 34px; margin-bottom: 12px; }
  .iy-feature-card .f-title { font-size: 18px; font-weight: 700; color: #1a1a1a; margin-bottom: 10px; }
  .iy-feature-card .f-desc  { font-size: 15px; color: #888; line-height: 1.7; }

  /* ── 이용 방법 (스텝) ── */
  .iy-steps { display: flex; flex-direction: column; }
  .iy-step  { display: flex; gap: 16px; align-items: flex-start; position: relative; }
  .iy-step:not(:last-child)::before {
    content: ''; position: absolute; left: 23px; top: 48px;
    width: 2px; height: calc(100% - 8px); background: #e8e8e8;
  }
  .iy-step .step-num {
    width: 48px; height: 48px; border-radius: 50%; background: #3b8ade; color: #fff;
    display: flex; align-items: center; justify-content: center;
    font-size: 18px; font-weight: 700; flex-shrink: 0; z-index: 1;
  }
  .iy-step .step-body {
    background: #fff; border-radius: 16px; padding: 20px 22px;
    margin-bottom: 14px; flex: 1; box-shadow: 0 2px 8px rgba(0,0,0,0.05);
    transition: transform 0.2s ease, box-shadow 0.2s ease;
  }
  .iy-step .step-body:hover { transform: scale(1.03); box-shadow: 0 6px 20px rgba(0,0,0,0.1); }
  .iy-step .step-body .s-title { font-size: 17px; font-weight: 700; color: #1a1a1a; margin-bottom: 6px; }
  .iy-step .step-body .s-desc  { font-size: 15px; color: #888; line-height: 1.7; }

  /* ── 사용자 흐름 ── */
  .iy-flow { background: #fff; border-radius: 18px; padding: 24px; box-shadow: 0 2px 10px rgba(0,0,0,0.05); }
  .iy-flow-stages {
    display: flex; align-items: center; justify-content: space-between;
    gap: 4px; margin-bottom: 20px;
  }
  .iy-flow-stage { flex: 1; text-align: center; }
  .iy-flow-stage .fs-icon {
    width: 56px; height: 56px; border-radius: 50%; background: #eaf4ff;
    display: flex; align-items: center; justify-content: center;
    font-size: 24px; margin: 0 auto 6px;
  }
  .iy-flow-stage .fs-label { font-size: 13px; color: #555; font-weight: 600; }
  .iy-flow-arrow { color: #ccc; font-size: 20px; flex-shrink: 0; }
  .iy-flow-desc {
    border-top: 1px solid #f5f5f5; padding-top: 18px;
    font-size: 15px; color: #888; line-height: 1.8; text-align: center;
  }

  /* ── 아키텍처 ── */
  .iy-arch { background: #fff; border-radius: 18px; padding: 28px; box-shadow: 0 2px 10px rgba(0,0,0,0.05); }
  .iy-arch-layer { margin-bottom: 16px; }
  .iy-arch-layer .layer-label {
    font-size: 12px; font-weight: 700; color: #aaa;
    text-transform: uppercase; letter-spacing: 0.08em; margin-bottom: 8px;
  }
  .iy-arch-boxes { display: flex; flex-wrap: wrap; gap: 8px; }
  .iy-arch-box   { border-radius: 10px; padding: 8px 14px; font-size: 14px; font-weight: 500; }
  .iy-arch-box.client  { background: #eaf4ff; color: #2c6fbd; }
  .iy-arch-box.gateway { background: #fff3e0; color: #9a4f00; }
  .iy-arch-box.service { background: #f0faf5; color: #1a7a50; }
  .iy-arch-box.data    { background: #fdf2ff; color: #7a1aaa; }
  .iy-arch-box.ai      { background: #fff8e6; color: #9a6200; }
  .iy-arch-divider {
    display: flex; align-items: center; gap: 8px; margin: 10px 0; color: #ddd; font-size: 12px;
  }
  .iy-arch-divider::before, .iy-arch-divider::after {
    content: ''; flex: 1; height: 1px; background: #f0f0f0;
  }

  /* ── 기술 스택 ── */
  .iy-stack-group { margin-bottom: 16px; }
  .iy-stack-group .sg-label { font-size: 12px; font-weight: 700; color: #aaa; margin-bottom: 8px; }
  .iy-chips { display: flex; flex-wrap: wrap; gap: 7px; }
  .iy-chip {
    background: #fff; border: 1px solid #e8e8e8; border-radius: 99px;
    padding: 7px 16px; font-size: 14px; color: #555;
    box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  }
  .iy-chip.blue   { background: #edf4ff; border-color: #b8d4f8; color: #2c6fbd; }
  .iy-chip.amber  { background: #fff8e6; border-color: #ffd98a; color: #9a6200; }
  .iy-chip.green  { background: #edfaf4; border-color: #9de8c3; color: #1a7a50; }
  .iy-chip.purple { background: #f5f0ff; border-color: #c8aff8; color: #6020c0; }

  /* ── 팀 소개 ── */
  .iy-team-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
  .iy-team-card { background: #fff; border-radius: 16px; padding: 22px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); transition: transform 0.2s ease, box-shadow 0.2s ease; }
  .iy-team-card:hover { transform: scale(1.04); box-shadow: 0 6px 20px rgba(0,0,0,0.1); }
  .iy-team-card .t-avatar {
    width: 44px; height: 44px; border-radius: 50%; background: #eaf4ff;
    display: flex; align-items: center; justify-content: center;
    font-size: 22px; margin-bottom: 12px;
  }
  .iy-team-card .t-name { font-size: 17px; font-weight: 700; color: #1a1a1a; margin-bottom: 4px; }
  .iy-team-card .t-role { font-size: 13px; color: #3b8ade; font-weight: 600; margin-bottom: 8px; }
  .iy-team-card .t-desc { font-size: 13px; color: #888; line-height: 1.7; }

  /* ── 깃허브 링크 ── */
  .iy-links { display: flex; gap: 14px; margin-top: 16px; }
  .iy-link-btn {
    flex: 1; display: flex; align-items: center; justify-content: center; gap: 10px;
    background: #fff; border-radius: 16px; padding: 18px;
    box-shadow: 0 2px 10px rgba(0,0,0,0.06);
    text-decoration: none; font-size: 15px; font-weight: 700; color: #1a1a1a;
  }
  .iy-link-btn .lb-icon { font-size: 26px; }
  .iy-link-btn.primary  { background: #1a1a1a; color: #fff; }

  /* ── 하단 네비 ── */
  .iy-nav {
    position: fixed; bottom: 0; left: 50%; transform: translateX(-50%);
    width: 100%; max-width: 780px;
    background: #fff; border-top: 1px solid #f0f0f0;
    display: flex; justify-content: space-around;
    padding: 12px 0 16px;
    box-shadow: 0 -4px 16px rgba(0,0,0,0.06); z-index: 100;
  }
  .iy-nav a {
    display: flex; flex-direction: column; align-items: center; gap: 4px;
    text-decoration: none; color: #bbb; font-size: 12px;
    transition: transform 0.2s ease, color 0.2s ease;
  }
  .iy-nav a:hover { transform: scale(1.15); color: #3b8ade; }
  .iy-nav a.active { color: #3b8ade; }
  .iy-nav a .nav-icon { font-size: 24px; }
</style>

<!-- ── 히어로 ── -->
<div id="home" class="iy-hero">
  <div class="tagline">"일기로 예견하는 보석같은 만남"</div>
  <div class="hero-logo">📔</div>
  <div class="hero-logo-text">일기예보</div>
  <h2>캠퍼스 동선으로<br>자연스럽게 연결되다</h2>
  <div class="desc">코로나19 이후 새로운 사람을 만나는 것이 부담스러운 대학생들을 위해,<br>수업 시간표와 교내 이동 동선의 교집합을 분석하여<br>자연스러운 만남의 기회를 제공하는 소셜 매칭 서비스입니다.</div>
  <div class="badges">
    <span class="badge">🎓 대학생 전용</span>
    <span class="badge">📍 동선 기반 매칭</span>
    <span class="badge">🛡️ 5단계 안전 시스템</span>
  </div>
</div>

<!-- ── 핵심 숫자 ── -->
<div class="iy-stats">
  <div class="iy-stat"><div class="num">5</div><div class="label">단계별 상호작용</div></div>
  <div class="iy-stat"><div class="num">AI</div><div class="label">퀴즈·미션 생성</div></div>
  <div class="iy-stat"><div class="num">📍</div><div class="label">캠퍼스 기반</div></div>
  <div class="iy-stat"><div class="num">LV.UP</div><div class="label">경험치 성장</div></div>
</div>

---

<!-- ── 데모 UI ── -->
<div class="iy-section">
  <div class="iy-section-header">
    <div>
      <div class="title">📱 예시 화면</div>
      <div class="subtitle">이런 화면이 펼쳐집니다</div>
    </div>
  </div>
  <div class="iy-card">
    <div class="iy-avatar">🌤️</div>
    <div class="iy-card-body">
      <div class="name">하늘빛</div>
      <div class="stage">📖 회고 단계 진행 중</div>
      <div class="iy-progress">
        <div class="bar done"></div><div class="bar done"></div>
        <div class="bar done"></div><div class="bar done"></div>
        <div class="bar active"></div>
      </div>
    </div>
    <div class="iy-badge"><div class="icon">⭐</div><div class="label">취미</div></div>
  </div>
  <div class="iy-card pending">
    <div class="iy-avatar gray">🕐</div>
    <div class="iy-card-body"><div class="pending-text">월요일에 새로운 만남</div></div>
    <div class="iy-badge"><div class="icon">🕐</div><div class="label">관심사</div></div>
  </div>
  <div class="iy-card completed">
    <div class="iy-avatar yellow">✓</div>
    <div class="iy-card-body"><div class="completed-text">매칭 완료 ⚡</div></div>
    <div class="iy-badge"><div class="icon yellow">✓</div><div class="label">이상형</div></div>
  </div>
  <p style="font-size:15px;color:#888;line-height:1.8;margin-top:14px;">
    각 카드는 <strong>슬롯</strong>입니다.<br>슬롯마다 취미·관심사·이상형 등 원하는 속성으로 바꿀 수 있어 나만의 조건에 맞는 상대를 추천받을 수 있습니다.
  </p>
</div>

---

<!-- ── 핵심 기능 ── -->
<div id="features" class="iy-section">
  <div class="iy-section-header"><div><div class="title">✨ 핵심 기능</div></div></div>
  <div class="iy-feature-grid">
    <div class="iy-feature-card">
      <div class="f-icon">🗺️</div>
      <div class="f-title">동선 기반 매칭</div>
      <div class="f-desc">시간표와 캠퍼스 공간 데이터를 분석해 자연스럽게 마주칠 수 있는 상대를 추천합니다.</div>
    </div>
    <div class="iy-feature-card">
      <div class="f-icon">🛡️</div>
      <div class="f-title">5단계 안전 시스템</div>
      <div class="f-desc">퀴즈→채팅→게임→미션→회고,<br>서로 동의 하에만 다음 단계로 진행합니다.</div>
    </div>
    <div class="iy-feature-card">
      <div class="f-icon">🤖</div>
      <div class="f-title">AI 콘텐츠 생성</div>
      <div class="f-desc">Amazon Bedrock LLM이 프로필 기반 퀴즈, 미션, 회고 질문을 자동으로 생성합니다.</div>
    </div>
    <div class="iy-feature-card">
      <div class="f-icon">🌱</div>
      <div class="f-title">성장형 소셜</div>
      <div class="f-desc">일기·플래너 작성과 상호작용으로 경험치를 쌓고 새로운 슬롯을 해금합니다.</div>
    </div>
  </div>
</div>

---

<!-- ── 서비스 이용 방법 ── -->
<div id="howto" class="iy-section">
  <div class="iy-section-header"><div><div class="title">📋 서비스 이용 방법</div></div></div>
  <div class="iy-steps">
    <div class="iy-step">
      <div class="step-num">1</div>
      <div class="step-body">
        <div class="s-title">대학 이메일로 가입</div>
        <div class="s-desc">학교 이메일 인증으로 재학생 여부를 확인합니다.<br>취미·관심사·성격 유형 등 프로필을 설정하면 초기 슬롯 1개가 부여됩니다.</div>
      </div>
    </div>
    <div class="iy-step">
      <div class="step-num">2</div>
      <div class="step-body">
        <div class="s-title">시간표 등록</div>
        <div class="s-desc">학기 초 수업 시간표를 등록합니다. 매일 플래너를 작성하면 더 정확한 동선 기반 매칭이 가능합니다.</div>
      </div>
    </div>
    <div class="iy-step">
      <div class="step-num">3</div>
      <div class="step-body">
        <div class="s-title">매주 월요일 자정, 매칭 성사</div>
        <div class="s-desc">배치 매칭 시스템이 동선이 겹치는 상대를 자동으로 찾아 매칭합니다.<br>매칭 주기는 월요일~금요일 5일입니다.</div>
      </div>
    </div>
    <div class="iy-step">
      <div class="step-num">4</div>
      <div class="step-body">
        <div class="s-title">5단계 상호작용 진행</div>
        <div class="s-desc">퀴즈→채팅(30분)→협동 게임→오프라인 미션→회고 순서로 점진적으로 관계를 쌓아갑니다.</div>
      </div>
    </div>
    <div class="iy-step">
      <div class="step-num">5</div>
      <div class="step-body">
        <div class="s-title">경험치 획득 & 레벨업</div>
        <div class="s-desc">활동마다 경험치를 쌓고 레벨업하면 새로운 매칭 슬롯이 해금됩니다. 더 많은 인연을 만나보세요.</div>
      </div>
    </div>
  </div>
</div>

---

<!-- ── 사용자 흐름 ── -->
<div id="flow" class="iy-section">
  <div class="iy-section-header"><div><div class="title">🔄 사용자 흐름</div></div></div>
  <div class="iy-flow">
    <div class="iy-flow-stages">
      <div class="iy-flow-stage">
        <div class="fs-icon">❓</div><div class="fs-label">퀴즈</div>
      </div>
      <div class="iy-flow-arrow">›</div>
      <div class="iy-flow-stage">
        <div class="fs-icon">💬</div><div class="fs-label">채팅</div>
      </div>
      <div class="iy-flow-arrow">›</div>
      <div class="iy-flow-stage">
        <div class="fs-icon">🎮</div><div class="fs-label">게임</div>
      </div>
      <div class="iy-flow-arrow">›</div>
      <div class="iy-flow-stage">
        <div class="fs-icon">🗺️</div><div class="fs-label">미션</div>
      </div>
      <div class="iy-flow-arrow">›</div>
      <div class="iy-flow-stage">
        <div class="fs-icon">📖</div><div class="fs-label">회고</div>
      </div>
    </div>
    <div class="iy-flow-desc">
      매 단계마다 <strong>양쪽 모두 동의</strong>해야 다음 단계로 진행됩니다.<br>
      거부 시 매칭이 안전하게 종료되며 퀴즈 단계에서는 힌트 질문으로 상대를 탐색할 수 있습니다.<br>
      신고·차단 시스템으로 언제든 안전하게 매칭을 종료할 수 있습니다.
    </div>
  </div>
</div>

---

<!-- ── 아키텍처 ── -->
<div class="iy-section">
  <div class="iy-section-header">
    <div>
      <div class="title">🏗️ 아키텍처</div>
      <div class="subtitle">단일 Spring Boot 앱 · AWS 클라우드 네이티브</div>
    </div>
  </div>
  <div class="iy-arch">
    <div class="iy-arch-layer">
      <div class="layer-label">Client</div>
      <div class="iy-arch-boxes">
        <div class="iy-arch-box client">React Native 모바일 앱</div>
      </div>
    </div>
    <div class="iy-arch-divider">↓ HTTPS</div>
    <div class="iy-arch-layer">
      <div class="layer-label">API Gateway</div>
      <div class="iy-arch-boxes">
        <div class="iy-arch-box gateway">AWS ALB (Application Load Balancer)</div>
      </div>
    </div>
    <div class="iy-arch-divider">↓</div>
    <div class="iy-arch-layer">
      <div class="layer-label">Backend · ECS Fargate</div>
      <div class="iy-arch-boxes">
        <div class="iy-arch-box service">인증 / 사용자</div>
        <div class="iy-arch-box service">매칭 엔진</div>
        <div class="iy-arch-box service">상호작용</div>
        <div class="iy-arch-box service">채팅 WebSocket</div>
        <div class="iy-arch-box service">게임 WebSocket</div>
        <div class="iy-arch-box service">경험치 / 안전 / 알림</div>
        <div class="iy-arch-box service">캠퍼스 공간 데이터</div>
      </div>
    </div>
    <div class="iy-arch-divider">↓</div>
    <div class="iy-arch-layer">
      <div class="layer-label">Data Layer</div>
      <div class="iy-arch-boxes">
        <div class="iy-arch-box data">MySQL Aurora</div>
        <div class="iy-arch-box data">Redis ElastiCache</div>
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

---

<!-- ── 기술 스택 ── -->
<div class="iy-section">
  <div class="iy-section-header"><div><div class="title">🛠️ 기술 스택</div></div></div>

  <div class="iy-stack-group">
    <div class="sg-label">Backend</div>
    <div class="iy-chips">
      <span class="iy-chip blue">Spring Boot 3</span>
      <span class="iy-chip blue">Spring Security</span>
      <span class="iy-chip blue">Spring Data JPA</span>
      <span class="iy-chip blue">Spring WebSocket</span>
      <span class="iy-chip blue">Spring Batch</span>
      <span class="iy-chip blue">Spring Scheduler</span>
    </div>
  </div>
  <div class="iy-stack-group">
    <div class="sg-label">AWS & Infra</div>
    <div class="iy-chips">
      <span class="iy-chip amber">Amazon Bedrock</span>
      <span class="iy-chip amber">Amazon SQS</span>
      <span class="iy-chip amber">ECS Fargate</span>
      <span class="iy-chip amber">ALB</span>
      <span class="iy-chip amber">RDS Aurora</span>
      <span class="iy-chip amber">ElastiCache</span>
      <span class="iy-chip amber">S3</span>
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
    <div class="sg-label">Client & Docs</div>
    <div class="iy-chips">
      <span class="iy-chip">React Native</span>
      <span class="iy-chip">Springdoc Swagger UI</span>
      <span class="iy-chip">JWT</span>
    </div>
  </div>
</div>

---

<!-- ── 팀 소개 ── -->
<div id="team" class="iy-section">
  <div class="iy-section-header"><div><div class="title">👥 팀 소개</div></div></div>
  <div class="iy-team-grid">
    <div class="iy-team-card">
      <div class="t-avatar">🌸</div>
      <div class="t-name">김아리</div>
      <div class="t-role">매칭 & 사용자 인증</div>
      <div class="t-desc">대학 이메일 인증 · JWT 보안 · 월요일 자정 배치 매칭 스케줄러 · 슬롯 관리</div>
    </div>
    <div class="iy-team-card">
      <div class="t-avatar">⚡</div>
      <div class="t-name">선현승</div>
      <div class="t-role">실시간 상호작용</div>
      <div class="t-desc">WebSocket 게임 세션 · Redis 상태 동기화 · FSM 전환 파이프라인</div>
    </div>
    <div class="iy-team-card">
      <div class="t-avatar">📔</div>
      <div class="t-name">정영미</div>
      <div class="t-role">일상 기록 & 퀴즈</div>
      <div class="t-desc">플래너·일기 도메인 · SQS 힌트 질문 처리 · 경험치 성장 시스템</div>
    </div>
    <div class="iy-team-card">
      <div class="t-avatar">🤖</div>
      <div class="t-name">황찬우</div>
      <div class="t-role">AI & 공간 데이터</div>
      <div class="t-desc">Bedrock LLM 연동 · 프롬프트 엔지니어링 · 캠퍼스 동선 추론 설계</div>
    </div>
  </div>
</div>

---

<!-- ── 깃허브 링크 ── -->
<div id="links" class="iy-section">
  <div class="iy-section-header"><div><div class="title">🔗 링크</div></div></div>
  <div class="iy-links">
    <a class="iy-link-btn primary" href="https://github.com/kookmin-sw/2026-capstone-57" target="_blank">
      <span class="lb-icon">🐙</span>
      <div>
        <div style="font-size:11px;opacity:.6;font-weight:400;">소스 코드</div>
        GitHub
      </div>
    </a>
    <a class="iy-link-btn" href="https://kookmin-sw.github.io/2026-capstone-57/" target="_blank">
      <span class="lb-icon">🏠</span>
      <div>
        <div style="font-size:11px;color:#aaa;font-weight:400;">팀 페이지</div>
        홈페이지
      </div>
    </a>
  </div>
</div>

<br>
<p style="text-align:center;font-size:12px;color:#bbb;margin-top:24px;">
  국민대학교 2026 캡스톤 디자인 프로젝트 · 57팀
</p>

<!-- ── 하단 네비게이션 ── -->
<nav class="iy-nav">
  <a href="#home" class="active"><span class="nav-icon">🏠</span>홈</a>
  <a href="#features"><span class="nav-icon">✨</span>기능</a>
  <a href="#howto"><span class="nav-icon">📋</span>이용방법</a>
  <a href="#flow"><span class="nav-icon">🔄</span>사용자흐름</a>
  <a href="#team"><span class="nav-icon">👥</span>팀소개</a>
</nav>

<script>
// 새로고침 시 맨 위로
if (history.scrollRestoration) history.scrollRestoration = 'manual';
window.scrollTo(0, 0);

// 스크롤 페이드인
var observer = new IntersectionObserver(function(entries) {
  entries.forEach(function(e) {
    if (e.isIntersecting) e.target.classList.add('visible');
  });
}, { threshold: 0.1 });
document.querySelectorAll('.iy-section, .iy-hero').forEach(function(el) {
  observer.observe(el);
});

// 네비 하이라이트
var sections = ['home','features','howto','flow','team'];
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
    a.classList.remove('active','now');
    if (a.getAttribute('href') === '#' + current) {
      a.classList.add('active','now');
    }
  });
}
window.addEventListener('scroll', updateNav);
updateNav();
</script>
