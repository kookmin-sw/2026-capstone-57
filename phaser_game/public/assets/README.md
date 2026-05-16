# 달빛찾기 (Moonlight Finder) - 에셋 가이드

## 게임 개요

2인 협동 퍼즐 플랫포머. 두 플레이어(파란 구름, 핑크 구름)가 맵의 달빛(코인)을 모두 수집하고, 스위치로 플랫폼을 이동시켜 탈출 문에 함께 도달하면 클리어.

## 해상도

- 논리 해상도: 390 x 844 (모바일 세로)
- Scale 모드: `Phaser.Scale.FIT`
- 타일 크기: 32 x 32

## 에셋 목록

### 타일맵

| 파일 | 설명 |
|------|------|
| `maps/level01.json` | 레벨 1 Tiled JSON |
| `maps/level02.json` | 레벨 2 Tiled JSON (현재 사용) |
| `maps/level02.tmj` | 레벨 2 Tiled 원본 |
| `tiles/` | 타일셋 관련 파일 |

### 타일셋

| 파일 | 설명 |
|------|------|
| `moonlight_tileset_32x32.png` | 32x32 메인 타일셋 |
| `moonlight_tileset_32x32.tsx` | Tiled 타일셋 정의 |

**타일셋 인덱스 (GID 기준):**
1. ground_top
2. dirt
3. left_wall
4. right_wall
5. platform
6. box
7. switch_off
8. switch_on
9. ladder
10. moon
11. door_closed
12. door_open
13. sign
14. bush
15. cloud_deco
16. sparkle

### 스프라이트 (64x64 스프라이트시트)

| 파일 | 설명 | 프레임 |
|------|------|--------|
| `blue_cloud_idle.png` | Player 1 대기 | 4 프레임 |
| `blue_cloud_walk.png` | Player 1 이동 | 4 프레임 |
| `blue_cloud_jump.png` | Player 1 점프 | 4 프레임 |
| `pink_cloud_idle.png` | Player 2 대기 | 4 프레임 |
| `pink_cloud_walk.png` | Player 2 이동 | 4 프레임 |
| `pink_cloud_jump.png` | Player 2 점프 | 4 프레임 |

### 아이템

| 파일 | 설명 |
|------|------|
| `moon_32x32_optimized.png` | 수집 아이템 (달빛) |

### UI

| 파일 | 설명 |
|------|------|
| `game_bg4.png` | 게임 배경 이미지 |
| `pl_background.png` | 프리로드 씬 배경 |
| `pl_logo.png` | 프리로드 씬 로고 |
| `clear_logo.png` | 클리어 로고 |
| `clear_back_button.png` | 클리어 후 뒤로가기 버튼 |

### 에셋 팩 (Phaser Loader용)

| 파일 | 설명 |
|------|------|
| `preload-asset-pack.json` | Boot 씬에서 로드 (배경, 로고) |
| `asset-pack.json` | Preload 씬에서 로드 (게임 에셋 전체) |

## 씬 구조

```
Boot → Preload → Level
```

- **Boot**: `preload-asset-pack.json` 로드 후 Preload로 전환
- **Preload**: `asset-pack.json` 로드 + 로딩 UI 표시 후 Level로 전환
- **Level**: 게임 플레이 (타일맵 + 물리 + 입력 + 스위치/플랫폼/코인/문)

## 조작

| 키 | Player 1 (파란 구름) | Player 2 (핑크 구름) |
|----|---------------------|---------------------|
| 이동 | ← → 방향키 | A / D |
| 점프 | ↑ 방향키 | W |

## 게임 메커니즘

1. **코인 수집**: 맵에 배치된 달빛(21개) 모두 수집 → 탈출 문 열림
2. **스위치**: 플레이어가 밟으면 연결된 플랫폼이 아래로 이동 (협동 필요)
3. **탈출 문**: 코인 전부 수집 후 두 플레이어 모두 문 위치에 도달 → 클리어

## 물리 설정

- 중력: `{ y: 800 }`
- 이동 속도: 200 px/s
- 점프 속도: -400 px/s
- 월드 바운드 충돌: 활성화

## 주의사항

- Phaser Editor 2D로 씬 편집 시 `/* START OF COMPILED CODE */` ~ `/* END OF COMPILED CODE */` 영역은 에디터가 자동 생성하므로 수동 수정 금지
- 커스텀 로직은 `/* START-USER-CODE */` ~ `/* END-USER-CODE */` 블록 안에 작성
- `ObjectLayer` 오브젝트 좌표 기준으로 충돌/수집/스위치 로직 구현 권장
