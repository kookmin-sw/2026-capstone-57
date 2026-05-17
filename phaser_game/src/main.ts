import Phaser from "phaser";
import Level from "./scenes/Level";
import Preload from "./scenes/Preload";
import Lobby from "./scenes/Lobby";
class Boot extends Phaser.Scene {
  constructor() {
    super("Boot");
  }

  preload() {
    this.load.pack("pack", "assets/preload-asset-pack.json");
  }

  create() {
    this.scene.start("Preload");
  }
}

window.addEventListener("load", function () {
  new Phaser.Game({
    width: 390,
    height: 844,
    backgroundColor: "#2f2f2f",
    parent: "game-container",
    
    pixelArt: true,
    antialias: false,
    roundPixels: true,
    physics: {
      default: "arcade",
      arcade: {
        gravity: { x: 0, y: 800 },
        debug: false
      }
    },

    scale: {
      mode: Phaser.Scale.FIT,
      autoCenter: Phaser.Scale.CENTER_BOTH
    },

    scene: [Boot, Lobby, Preload, Level]
  });
});