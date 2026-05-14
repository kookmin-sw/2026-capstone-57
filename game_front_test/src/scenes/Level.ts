// You can write more code here

/* START OF COMPILED CODE */

/* START-USER-IMPORTS */
/* END-USER-IMPORTS */

export default class Level extends Phaser.Scene {

	constructor() {
		super("Level");
	}

	editorCreate(): void {

		// level
		const level = this.add.tilemap("level02");
		level.addTilesetImage("moonlight_tileset_32x32", "moonlight_tileset_32x32");

		// game_bg4
		const game_bg4 = this.add.image(198, 350, "game_bg4");
		game_bg4.scaleX = 0.5058780246943825;
		game_bg4.scaleY = 0.5502273107896416;

		// ground_1
		const groundLayer = level.createLayer("Ground", ["moonlight_tileset_32x32"], -15, -129)!;

		// game_bg
		const game_bg = this.add.image(846, 859, "game_bg4");
		game_bg.scaleX = 0.5058780246943825;
		game_bg.scaleY = 0.5502273107896416;
		game_bg.setOrigin(1, 1);

		// game_bg_1
		const game_bg_1 = this.add.image(-237, 351, "game_bg4");
		game_bg_1.scaleX = 0.5058780246943825;
		game_bg_1.scaleY = 0.5502273107896416;

		// player1
		this.player1 = this.physics.add.sprite(30, 542, "blue_cloud_idle", 0);
		this.player1.name = "player1";
		this.player1.body.setSize(32, 30, false);

		// player2
		this.player2 = this.physics.add.sprite(102, 646, "pink_cloud_idle", 0);
		this.player2.name = "player2";
		this.player2.body.setSize(32, 30, false);

		this.level = level;
		this.groundLayer = groundLayer;

		this.events.emit("scene-awake");
	}

	private level!: Phaser.Tilemaps.Tilemap;
	private groundLayer!: Phaser.Tilemaps.TilemapLayer;

	private player1!: Phaser.Physics.Arcade.Sprite;
	private player2!: Phaser.Physics.Arcade.Sprite;
	private switch1!: Phaser.GameObjects.Rectangle;
	private switch2!: Phaser.GameObjects.Rectangle;

	private platform1!: Phaser.GameObjects.Rectangle;
	private platform2!: Phaser.GameObjects.Rectangle;

	private platform1BaseY = 0;
	private platform2BaseY = 0;

	private switch1Pressed = false;
	private switch2Pressed = false;
	/* START-USER-CODE */

	private cursors!: Phaser.Types.Input.Keyboard.CursorKeys;

	private wasd!: {
		left: Phaser.Input.Keyboard.Key;
		right: Phaser.Input.Keyboard.Key;
		up: Phaser.Input.Keyboard.Key;
	};

	private coins!: Phaser.Physics.Arcade.StaticGroup;
	private score = 0;
	private scoreText!: Phaser.GameObjects.Text;

	//탈출 문 조건 체크 변수
	private totalCoins = 21;
	private door!: Phaser.GameObjects.Rectangle;
	private doorOpen = false;
	private player1AtDoor = false;
	private player2AtDoor = false;
	//

	create() {

		this.editorCreate();

		/* ---------------------------------------------------------------------- */
		/* INPUT */
		/* ---------------------------------------------------------------------- */

		this.cursors = this.input.keyboard!.createCursorKeys();

		this.wasd = {
			left: this.input.keyboard!.addKey(Phaser.Input.Keyboard.KeyCodes.A),
			right: this.input.keyboard!.addKey(Phaser.Input.Keyboard.KeyCodes.D),
			up: this.input.keyboard!.addKey(Phaser.Input.Keyboard.KeyCodes.W)
		};

		/* ---------------------------------------------------------------------- */
		/* TILEMAP COLLISION */
		/* ---------------------------------------------------------------------- */

		this.groundLayer.setCollisionByExclusion([-1]);

		this.groundLayer.renderDebug(this.add.graphics(), {
			tileColor: null,
			collidingTileColor: new Phaser.Display.Color(255, 0, 0, 80),
			faceColor: new Phaser.Display.Color(0, 255, 0, 255)
		});

		/* ---------------------------------------------------------------------- */
		/* PLAYER PHYSICS */
		/* ---------------------------------------------------------------------- */

		this.player1.body!.setSize(20, 12, false);
		this.player1.body!.setOffset(22, 40);
		this.player1.setCollideWorldBounds(true);

		this.player2.body!.setSize(28, 18, false);
		this.player2.body!.setOffset(18, 40);
		this.player2.setCollideWorldBounds(true);

		this.physics.add.collider(this.player1, this.groundLayer);
		this.physics.add.collider(this.player2, this.groundLayer);

		/* ---------------------------------------------------------------------- */
		/* SCORE UI */
		/* ---------------------------------------------------------------------- */

		this.scoreText = this.add.text(16, 16, "달: 0", {
			fontSize: "20px",
			color: "#000000"
			});

		/* ---------------------------------------------------------------------- */
		/* COINS */
		/* ---------------------------------------------------------------------- */

		this.coins = this.physics.add.staticGroup();

		const coinPositions = [
			{ x: 67, y: 329 },
			{ x: 98, y: 329 },
			{ x: 130, y: 329 },
			{ x: 160, y: 329 },
			{ x: 190, y: 329 },
			{ x: 220, y: 329 },
			{ x: 250, y: 329 },
			{ x: 195, y: 656 },
			{ x: 226, y: 656 },
			{ x: 258, y: 656 },
			{ x: 290, y: 656 },
			{ x: 322, y: 656 },
			{ x: 161, y: 593 },
			{ x: 193, y: 560 },
			{ x: 226, y: 528 },
			{ x: 258, y: 495 },
			{ x: 321, y: 463 },
			{ x: 290, y: 463 },
			{ x: 353, y: 463 },
			{ x: 32, y: 205 },
			{ x: 65, y: 205 },

		];

		coinPositions.forEach(pos => {

			const coin = this.coins.create(
				pos.x,
				pos.y,
				"moon_32x32_optimized"
			) as Phaser.Physics.Arcade.Image;

			coin.body.setSize(32, 32, false);
		});

		this.physics.add.overlap(this.player1, this.coins, this.collectCoin, undefined, this);
		this.physics.add.overlap(this.player2, this.coins, this.collectCoin, undefined, this);

		/* ---------------------------------------------------------------------- */
		/* SWITCHES */
		/* ---------------------------------------------------------------------- */

		this.switch1 = this.add.rectangle(20, 343, 32, 16, 0x1f2a60);
		this.physics.add.existing(this.switch1, true);

		this.switch2 = this.add.rectangle(354, 665, 32, 16, 0x1f2a60);
		this.physics.add.existing(this.switch2, true);

		/* ---------------------------------------------------------------------- */
		/* MOVING PLATFORMS */
		/* ---------------------------------------------------------------------- */

		this.platform1 = this.add.rectangle(141, 241, 32, 32, 0xd9d9d9);
		this.physics.add.existing(this.platform1, true);

		this.platform2 = this.add.rectangle(323, 367, 32, 32, 0xd9d9d9);
		this.physics.add.existing(this.platform2, true);

		this.platform1BaseY = this.platform1.y;
		this.platform2BaseY = this.platform2.y;

		this.physics.add.collider(this.player1, this.platform1);
		this.physics.add.collider(this.player2, this.platform1);

		this.physics.add.collider(this.player1, this.platform2);
		this.physics.add.collider(this.player2, this.platform2);

		/* ---------------------------------------------------------------------- */
		/* PLAYER1 ANIMATIONS */
		/* ---------------------------------------------------------------------- */

		this.anims.create({
			key: "blue-idle",
			frames: this.anims.generateFrameNumbers("blue_cloud_idle", {
				start: 0,
				end: 3
			}),
			frameRate: 6,
			repeat: -1
		});

		this.anims.create({
			key: "blue-walk",
			frames: this.anims.generateFrameNumbers("blue_cloud_walk", {
				start: 0,
				end: 3
			}),
			frameRate: 10,
			repeat: -1
		});

		this.anims.create({
			key: "blue-jump",
			frames: this.anims.generateFrameNumbers("blue_cloud_jump", {
				start: 0,
				end: 3
			}),
			frameRate: 10,
			repeat: -1
		});

		this.player1.play("blue-idle");

		/* ---------------------------------------------------------------------- */
		/* PLAYER2 ANIMATIONS */
		/* ---------------------------------------------------------------------- */

		this.anims.create({
			key: "pink-idle",
			frames: this.anims.generateFrameNumbers("pink_cloud_idle", {
				start: 0,
				end: 3
			}),
			frameRate: 6,
			repeat: -1
		});

		this.anims.create({
			key: "pink-walk",
			frames: this.anims.generateFrameNumbers("pink_cloud_walk", {
				start: 0,
				end: 3
			}),
			frameRate: 10,
			repeat: -1
		});

		this.anims.create({
			key: "pink-jump",
			frames: this.anims.generateFrameNumbers("pink_cloud_jump", {
				start: 0,
				end: 3
			}),
			frameRate: 10,
			repeat: -1
		});
				/* ---------------------------------------------------------------------- */
		/* EXIT DOOR */
		/* ---------------------------------------------------------------------- */
		//TODO: 좌표 수정
		this.door = this.add.rectangle(354, 225, 32, 64, 0x999999);
		this.door.alpha = 0.4;

		this.physics.add.existing(this.door, true);

		this.physics.add.overlap(this.player1, this.door, () => {
			this.player1AtDoor = true;
			this.checkClear();
		});

		this.physics.add.overlap(this.player2, this.door, () => {
			this.player2AtDoor = true;
			this.checkClear();
		});

		this.player2.play("pink-idle");
	}

	update() {

		const body1 = this.player1.body as Phaser.Physics.Arcade.Body;
		const body2 = this.player2.body as Phaser.Physics.Arcade.Body;

		/* ---------------------------------------------------------------------- */
		/* PLAYER1 MOVEMENT */
		/* ---------------------------------------------------------------------- */

		body1.setVelocityX(0);

		if (this.cursors.left.isDown) {
			body1.setVelocityX(-200);
			this.player1.setFlipX(true);
		}
		else if (this.cursors.right.isDown) {
			body1.setVelocityX(200);
			this.player1.setFlipX(false);
		}

		if (this.cursors.up.isDown && body1.blocked.down) {
			body1.setVelocityY(-400);
		}

		/* ---------------------------------------------------------------------- */
		/* PLAYER2 MOVEMENT */
		/* ---------------------------------------------------------------------- */

		body2.setVelocityX(0);

		if (this.wasd.left.isDown) {
			body2.setVelocityX(-200);
			this.player2.setFlipX(true);
		}
		else if (this.wasd.right.isDown) {
			body2.setVelocityX(200);
			this.player2.setFlipX(false);
		}

		if (this.wasd.up.isDown && body2.blocked.down) {
			body2.setVelocityY(-400);
		}

		/* ---------------------------------------------------------------------- */
		/* PLAYER1 ANIMATION STATE */
		/* ---------------------------------------------------------------------- */

		if (!body1.blocked.down) {
			this.playPlayer1Anim("blue-jump");
		}
		else if (body1.velocity.x !== 0) {
			this.playPlayer1Anim("blue-walk");
		}
		else {
			this.playPlayer1Anim("blue-idle");
		}

		/* ---------------------------------------------------------------------- */
		/* PLAYER2 ANIMATION STATE */
		/* ---------------------------------------------------------------------- */

		if (!body2.blocked.down) {
			this.playPlayer2Anim("pink-jump");
		}
		else if (body2.velocity.x !== 0) {
			this.playPlayer2Anim("pink-walk");
		}
		else {
			this.playPlayer2Anim("pink-idle");
		}
		this.updateSwitchPlatforms();
	}

	/* -------------------------------------------------------------------------- */
	/* COIN COLLECT */
	/* -------------------------------------------------------------------------- */

	private collectCoin(
		_player: Phaser.Types.Physics.Arcade.GameObjectWithBody,
		coin: Phaser.Types.Physics.Arcade.GameObjectWithBody
	) {

		coin.destroy();

		this.score += 1;
		this.scoreText.setText(`달: ${this.score}`);

		if (this.score >= this.totalCoins) {
			this.openDoor();
		}
	}

	/* -------------------------------------------------------------------------- */
	/* ANIMATION HELPERS */
	/* -------------------------------------------------------------------------- */

	private playPlayer1Anim(key: string) {

		if (this.player1.anims.currentAnim?.key !== key) {
			this.player1.play(key);
		}
	}

	private playPlayer2Anim(key: string) {

		if (this.player2.anims.currentAnim?.key !== key) {
			this.player2.play(key);
		}
	}
	private updateSwitchPlatforms() {

	this.switch1Pressed =
		this.physics.overlap(this.player1, this.switch1) ||
		this.physics.overlap(this.player2, this.switch1);

	this.switch2Pressed =
		this.physics.overlap(this.player1, this.switch2) ||
		this.physics.overlap(this.player2, this.switch2);

	this.switch1.fillColor = this.switch1Pressed ? 0x4f6cff : 0x1f2a60;
	this.switch2.fillColor = this.switch2Pressed ? 0x4f6cff : 0x1f2a60;

	const platform1TargetY = this.switch1Pressed
		? this.platform1BaseY + 90
		: this.platform1BaseY;

	const platform2TargetY = this.switch2Pressed || this.switch1Pressed
		? this.platform2BaseY + 90
		: this.platform2BaseY;

	this.moveStaticPlatform(this.platform1, platform1TargetY);
	this.moveStaticPlatform(this.platform2, platform2TargetY);
}

private moveStaticPlatform(
	platform: Phaser.GameObjects.Rectangle,
	targetY: number
) {

	platform.y = Phaser.Math.Linear(platform.y, targetY, 0.008);

	const body = platform.body as Phaser.Physics.Arcade.StaticBody;
	body.updateFromGameObject();
}
private openDoor() {

	if (this.doorOpen) {
		return;
	}

	this.doorOpen = true;
	this.door.fillColor = 0x66ccff;
	this.door.alpha = 1;
}

private checkClear() {

	if (!this.doorOpen) {
		return;
	}

	if (this.player1AtDoor && this.player2AtDoor) {
		console.log("CLEAR!");

		this.add.text(120, 390, "CLEAR!", {
			fontSize: "48px",
			color: "#000000"
		});
	}
}
	/* END-USER-CODE */
}

/* END OF COMPILED CODE */

// You can write more code here