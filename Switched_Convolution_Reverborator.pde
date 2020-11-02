import beads.*;
import java.util.ArrayList;


AudioContext ac;
SamplePlayer player;
EarlyReflections er;
BiquadComponent attenuation;
Gain g;
ModularReverb scr;
File file;


static final int bufferSize = 2048;
float[] settings;
String[] settingNames;
float[][] minmax;
int changing;
int categoryWidth;


void setup() {
  size(1024, 1000);
  frameRate(30);
  // Set up initial settings
  settings = new float[6];
  settingNames = new String[6];
  minmax = new float[6][2];
  // Room x, y, z
  settings[0] = 20;
  settingNames[0] = "Room Length";
  minmax[0] = new float[] {0, 200};
  settings[1] = 45;
  settingNames[1] = "Room Width";
  minmax[1] = new float[] {0, 200};
  settings[2] = 9;
  settingNames[2] = "Room Height";
  minmax[2] = new float[] {0, 200};

  // Reverberance
  settings[3] = 0.5;
  settingNames[3] = "Echoiness";
  minmax[3] = new float[] {0, 0.99};

  settings[4] = 0.7;
  settingNames[4] = "Damping";
  minmax[4] = new float[] {0, 1};

  settings[5] = 0.0;
  settingNames[5] = "Openness";
  minmax[5] = new float[] {0, 1};

  changing = -1;

  categoryWidth = width / settings.length;

  // Set up audio context
  ac = new AudioContext();


  // Select audio file to use
  selectInput("Select an audio file to use.", "fileSelected");
}

void draw() {
  background(0);
  // Visual adjustments for room size
  int prevX = 0;
  for (int i = 0; i < settings.length; i++) {
    // Check if we're editing this thing.
    if (changing == i) {
      // Set the corresponding variable
      float realValue = lerp(minmax[i][0], minmax[i][1], constrain(1-((float)mouseY / height), 0, 1));
      switch (i) {
      case 0:
        er.setSize(round(realValue), round(settings[1]), round(settings[2]));
        break;
      case 1:
        er.setSize(round(settings[0]), round(realValue), round(settings[2]));
        break;
      case 2:
        er.setSize(round(settings[0]), round(settings[1]), round(realValue));
        break;
      case 3:
        er.setFactor(realValue * (1 - settings[5]));
        break;
      case 4:
        attenuation.setFrequency(lerp(18000, 100, pow(realValue, 0.1)));
        break;
      case 5:
        scr.setOpenness(realValue);
        er.setFactor((1 - realValue) * settings[3]);
      }
      settings[i] = realValue;
      stroke(255, 0, 0);
    } else {
      stroke(255);
    }
    float y = map(settings[i], minmax[i][0], minmax[i][1], height, 0);
    int x = categoryWidth * (i+1);
    line(prevX, y, x, y);
    String label = settingNames[i]+": "+String.format(java.util.Locale.US, "%.2f", settings[i]);
    text(label, prevX + 10, 30);
    prevX = x;
  }
}

void fileSelected(File f) {
  println("File selected.");
  file = f;
  println(f.getAbsolutePath());
  Sample s = SampleManager.sample(f.getAbsolutePath());
  player = new SamplePlayer(ac, s);

  attenuation = new BiquadComponent(BiquadComponent.LOWPASS, ac.getSampleRate(), lerp(10000, 100, pow(settings[4], 0.1)), 1, 1);
  er = new EarlyReflections(player, (int)settings[0], (int)settings[1], (int)settings[2], 0.5, attenuation);
  scr = new ModularReverb(player, er.reverbWindow, 200, 1.0, attenuation);

  scr.setResonance(er.resonances);
  g = new Gain(ac, 1, 0.3);

  g.addInput(scr);
  g.addInput(er);

  ac.out.addInput(g);
  println("Starting.");
  ac.start();
}

void fileUpdate(File f) {
  player.pause(true);

  Sample s = SampleManager.sample(f.getAbsolutePath());
  player.setSample(s);
  player.pause(false);
  player.reTrigger();
}

void keyPressed() {
  if (key == ' ' && player != null) {
    player.reTrigger();
  }
  if (key == 'o') {
    selectInput("Select an audio file to use.", "fileUpdate");
  }
}

void mousePressed() {
  // Determine the variable you are changing
  for (int i = 0; i < settings.length; i++) {
    if (mouseX < (i+1) * categoryWidth) {
      changing = i;
      return;
    }
  }
}

void mouseReleased() {
  changing = -1;
}
