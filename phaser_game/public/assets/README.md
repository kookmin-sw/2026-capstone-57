# 달빛찾기 Phaser 실사용 에셋

## 파일 구성
- `assets/maps/level01.json`: Tiled JSON 타일맵
- `assets/tiles/moonlight_tileset_32x32.png`: 32x32 타일셋
- `assets/sprites/blue_cloud_sheet_64.png`: 파란 구름 64x64 스프라이트시트
- `assets/sprites/pink_cloud_sheet_64.png`: 핑크 구름 64x64 스프라이트시트
- `assets/ui/game_bg_390x844.png`: 모바일 배경
- `MoonlightScene.js`: Phaser 3 로딩/타일맵/플레이어 이동 예시

## 타일셋 인덱스
Tiled/Phaser GID 기준:
1 ground_top, 2 dirt, 3 left_wall, 4 right_wall, 5 platform, 6 box, 7 switch_off, 8 switch_on, 9 ladder, 10 moon, 11 door_closed, 12 door_open, 13 sign, 14 bush, 15 cloud_deco, 16 sparkle

## 주의
- 이 에셋은 바로 프로토타입에 넣을 수 있는 실사용 PNG/JSON입니다.
- 실제 게임에서는 `ObjectsPreview` 레이어는 시각 확인용으로 두고, 충돌/수집/스위치/문 로직은 `ObjectLayer` 오브젝트 좌표를 기준으로 구현하는 걸 추천합니다.
