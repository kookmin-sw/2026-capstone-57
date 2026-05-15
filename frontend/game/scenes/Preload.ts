
// You can write more code here

/* START OF COMPILED CODE */

/* START-USER-IMPORTS */
/* END-USER-IMPORTS */

export default class Preload extends Phaser.Scene {

	constructor() {
		super("Preload");

		/* START-USER-CTR-CODE */
		// Write your code here.
		/* END-USER-CTR-CODE */
	}

	editorCreate(): void {

		// pl_background
		const pl_background = this.add.image(194, 412, "pl_background");
		pl_background.scaleX = 0.48;
		pl_background.scaleY = 0.47;

		// loadingText
		const loadingText = this.add.text(140, 458, "", {});
		loadingText.text = "상대방을 기다리고 있어요!!!";
		loadingText.setStyle({ "align": "center", "color": "#000000ff", "fontFamily": "arial", "fontSize": "15", "stroke": "#000000ff", "shadow.color": "#000000ff" });

		// progressBar
		const progressBar = this.add.rectangle(75, 430, 256, 20);
		progressBar.setOrigin(0, 0);
		progressBar.isFilled = true;
		progressBar.fillColor = 14737632;

		// progressBarBg
		const progressBarBg = this.add.rectangle(75, 430, 256, 20);
		progressBarBg.setOrigin(0, 0);
		progressBarBg.fillColor = 14737632;
		progressBarBg.isStroked = true;

		// pl_logo
		const pl_logo = this.add.image(197, 276, "pl_logo");
		pl_logo.scaleX = 0.2062755467640674;
		pl_logo.scaleY = 0.20223052603679498;

		this.progressBar = progressBar;

		this.events.emit("scene-awake");
	}

	private progressBar!: Phaser.GameObjects.Rectangle;

	/* START-USER-CODE */

	// Write your code here

	preload() {

		this.editorCreate();

		this.load.path = "/game/";
		this.load.pack("asset-pack", "/game/assets/asset-pack.json");

		const width = this.progressBar.width;

		this.load.on("progress", (value: number) => {

			this.progressBar.width = width * value;
		});
	}

	create() {

		if (process.env.NODE_ENV === "development") {

			const start = new URLSearchParams(location.search).get("start");

			if (start) {

				console.log(`Development: jump to ${start}`);
				this.scene.start(start);

				return;
			}
		}

		// Check URL for sessionId parameter to determine routing
		const params = new URLSearchParams(window.location.search);
		const sessionId = params.get("sessionId");

		if (sessionId) {
			// Online mode: route to Lobby for connection setup
			this.scene.start("Lobby");
		} else if (process.env.NEXT_PUBLIC_LOCAL_MODE === "true") {
			// Dev-only: local 2-player mode when no sessionId
			this.scene.start("Level");
		} else {
			// Production: show error when no sessionId
			this.add.text(195, 422, "세션 연결 실패", {
				fontSize: "18px",
				color: "#ff6666",
				align: "center",
			}).setOrigin(0.5);
		}
	}

	/* END-USER-CODE */
}

/* END OF COMPILED CODE */

// You can write more code here
