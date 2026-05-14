
// MoonlightScene.js - Phaser 3 예시
export default class MoonlightScene extends Phaser.Scene {
  constructor() { super('MoonlightScene'); }

  preload() {
    this.load.image('gameBg', 'assets/ui/game_bg_390x844.png');
    this.load.image('moonTiles', 'assets/tiles/moonlight_tileset_32x32.png');
    this.load.tilemapTiledJSON('level01', 'assets/maps/level01.json');
    this.load.spritesheet('blueCloud', 'assets/sprites/blue_cloud_sheet_64.png', { frameWidth: 64, frameHeight: 64 });
    this.load.spritesheet('pinkCloud', 'assets/sprites/pink_cloud_sheet_64.png', { frameWidth: 64, frameHeight: 64 });
  }

  create() {
    this.add.image(195, 422, 'gameBg').setScrollFactor(0).setDepth(-10);

    const map = this.make.tilemap({ key: 'level01' });
    const tileset = map.addTilesetImage('moonlight_tileset_32x32', 'moonTiles');
    const ground = map.createLayer('Ground', tileset, 0, 0);
    const ladders = map.createLayer('Ladders', tileset, 0, 0);
    const preview = map.createLayer('ObjectsPreview', tileset, 0, 0);

    ground.setCollisionByExclusion([-1, 0]);
    // 미리보기 레이어에 박스/스위치/문이 그려져 있음. 실제 상호작용은 ObjectLayer 기준으로 만들면 됨.

    this.anims.create({ key: 'blue-idle', frames: this.anims.generateFrameNumbers('blueCloud', { start: 0, end: 1 }), frameRate: 2, repeat: -1 });
    this.anims.create({ key: 'blue-walk', frames: this.anims.generateFrameNumbers('blueCloud', { start: 2, end: 3 }), frameRate: 6, repeat: -1 });
    this.anims.create({ key: 'pink-idle', frames: this.anims.generateFrameNumbers('pinkCloud', { start: 0, end: 1 }), frameRate: 2, repeat: -1 });
    this.anims.create({ key: 'pink-walk', frames: this.anims.generateFrameNumbers('pinkCloud', { start: 2, end: 3 }), frameRate: 6, repeat: -1 });

    const spawns = map.getObjectLayer('ObjectLayer').objects.filter(o => o.type === 'spawn');
    const p1Spawn = spawns.find(o => o.name === 'player1');
    const p2Spawn = spawns.find(o => o.name === 'player2');

    this.p1 = this.physics.add.sprite(p1Spawn.x, p1Spawn.y, 'blueCloud').setScale(0.72).play('blue-idle');
    this.p2 = this.physics.add.sprite(p2Spawn.x, p2Spawn.y, 'pinkCloud').setScale(0.72).play('pink-idle');
    for (const p of [this.p1, this.p2]) {
      p.setCollideWorldBounds(true);
      p.body.setSize(44, 34).setOffset(10, 24);
      this.physics.add.collider(p, ground);
    }

    this.cameras.main.setBounds(0, 0, map.widthInPixels, map.heightInPixels);
    this.physics.world.setBounds(0, 0, map.widthInPixels, map.heightInPixels);
    this.cameras.main.startFollow(this.p1, true, 0.08, 0.08);
    this.cameras.main.setZoom(390 / map.widthInPixels); // 390px 폭에 맞춤

    this.cursors = this.input.keyboard.createCursorKeys();
    this.keys = this.input.keyboard.addKeys('A,D,W,SPACE');
  }

  update() {
    this.movePlayer(this.p1, this.cursors.left.isDown, this.cursors.right.isDown, Phaser.Input.Keyboard.JustDown(this.cursors.up), 'blue');
    this.movePlayer(this.p2, this.keys.A.isDown, this.keys.D.isDown, Phaser.Input.Keyboard.JustDown(this.keys.W), 'pink');
  }

  movePlayer(player, left, right, jumpPressed, prefix) {
    const speed = 130;
    if (left) { player.setVelocityX(-speed); player.setFlipX(true); player.play(`${prefix}-walk`, true); }
    else if (right) { player.setVelocityX(speed); player.setFlipX(false); player.play(`${prefix}-walk`, true); }
    else { player.setVelocityX(0); player.play(`${prefix}-idle`, true); }
    if (jumpPressed && player.body.blocked.down) player.setVelocityY(-285);
  }
}
