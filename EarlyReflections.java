import beads.*;
import java.lang.Math.*;
import java.util.ArrayList;
import java.util.Random;

// Able to handle any room up to the size of a football field.
class EarlyReflections extends Function {
  private static final double SOUNDSPD = 1125.33;
  private static final int BUFFERSIZE = 16384;
  
  // Pseudo-constants
  private final double samplesPerFoot;
  
  // Buffer variables
  private float[] buffer;
  private int bufferIndex;
  
  // Delay arrays
  private int[] delays;
  private int[] delays2;
  private float refDelay;
  private float transition;
  
  // Exporting for modular reverb
  ArrayList<Integer> resonances;
  public float[] reverbWindow;
  
  // Room statistics
  private float l, w, h;
  private float goalL, goalW, goalH;
  private float echoFactor;
  BiquadComponent attenuation;
  
  // For transitioning between room sizes
  private float reverbVelocity;
  private float velocity;
  private boolean aToB;
  
  // Stereo width stuff
  private float fbJitter;
  private float lrJitter;
  
  // Feedback
  private float prevOut;

  

  public EarlyReflections(UGen input, int l, int w, int h, float stereoWidth, BiquadComponent attenuation) {
    super(input);
    this.buffer = new float[BUFFERSIZE];
    this.delays = new int[6];
    this.delays2 = new int[6];
    
    // Quasi-constants
    this.reverbVelocity = 0.005f;
    this.samplesPerFoot = SOUNDSPD / context.getSampleRate();
    this.velocity = 40 / context.getSampleRate(); // Set velocity to match 1/40th of a second.
    this.refDelay = (float)(100 / samplesPerFoot);
    
    this.l = l;
    this.w = w;
    this.h = h;
    this.goalL = l;
    this.goalW = w;
    this.goalH = h;
    
    this.echoFactor = 1.0f;
    this.resonances = new ArrayList<Integer>();
    this.reverbWindow = new float[2];
    this.attenuation = attenuation.register(this);
    
    Random r = new Random();

    this.fbJitter = (float)((r.nextFloat() - 0.5) * stereoWidth * 2 + 0.5);
    this.lrJitter = (float)((r.nextFloat() - 0.5) * stereoWidth * 2 + 0.5);
    
    
    this.aToB = true;
    initDelays();
    this.aToB = false;
    initDelays();
  }

  public float calculate() {
    // Buffer inputt
    float in = processInput();
    buffer[bufferIndex] = in;
    // Sample both echo tracks
    float sample = 0;
    float sample2 = 0;
    // Determine if fading between delay sequences
    if (transition < 1) {
      for (int delay : delays) {
        sample += get(-delay) / Math.pow((refDelay + delay) / refDelay, 2);
      }
    }
    if (transition > 0) {
      for (int delay : delays2) {
        sample2 += get(-delay) / Math.pow((refDelay + delay) / refDelay, 2);
      }
    }
    if(transition < 1 && transition > 0){
      // Transitioning. Change the mix.
      if(aToB){
        transition += velocity;
        if(transition > 1){
          transition = 1;
        }
      }else{
        transition -= velocity;
        if(transition < 0){
          transition = 0;
        }
      }
    }
    // Equal power mix the delay sets
    sample *= Math.pow(1-transition, 0.5);
    sample2 *= Math.pow(transition, 0.5);
    // Combine echoes into single track
    sample = sample + sample2;

    sample = attenuation.calculate(sample, this);

    // Iterate buffer
    bufferIndex++;
    if (bufferIndex == buffer.length) {
      bufferIndex = 0;
    }

    // Determine if changing reverb window
    boolean changed = false;
    if (goalL != l) {
      l += MathUtils.constrain(goalL - l, -reverbVelocity, reverbVelocity);
      changed = true;
    }
    if (goalW != w) {
      w += MathUtils.constrain(goalW - w, -reverbVelocity, reverbVelocity);
      changed = true;
    }
    if (goalH != h) {
      h += MathUtils.constrain(goalH - h, -reverbVelocity, reverbVelocity);
      changed = true;
    }
    if (changed) {
      updateReverbWindow();
    }
    sample = (sample + prevOut) / 2;
    prevOut = sample;
    return ((sample * echoFactor) + in);
  }

  public void setSize(int l, int w, int h) {
    this.goalL = Math.max(l, 1);
    this.goalW = Math.max(w, 1);
    this.goalH = Math.max(h, 1);
    aToB = !aToB;
    if(aToB){
      transition += velocity;
    }else{
      transition -= velocity;
    }
    initDelays();
  }
  
  public void setFactor(float factor){
    this.echoFactor = factor;
  }

  public ArrayList<Integer> getDelays() {
    return resonances;
  }

  public float minDelay() {
    return (float)(Math.min(Math.min(goalL, goalW), goalH) / SOUNDSPD);
  }

  public float maxDelay() {
    return (float)(Math.max(Math.max(goalL, goalW), goalH) / SOUNDSPD);
  }

  private float processInput() {
    if (x.length == 1) {
      return x[0];
    } else {
      float sum = 0;
      for (float f : x) {
        sum += f;
      }
      return sum;
    }
  }

  private void initDelays() {
    int[] using = new int[6];
    
    while(resonances.size() < 3){
      resonances.add(0);
    }

    // Floor / ceiling delay
    float pos = Math.min(goalH-1, 6);
    using[0] = (int)(Math.round(pos / samplesPerFoot) * 2);
    using[1] = (int)(Math.round((goalH - pos) / samplesPerFoot) * 2);
    this.resonances.set(0, (int)Math.round(goalH / samplesPerFoot));

    // Front / Back wall delays
    pos = goalL * fbJitter;
    using[2] = (int)Math.round(pos / samplesPerFoot) * 2;
    using[3] = (int)Math.round((goalL - pos) / samplesPerFoot) * 2;
    this.resonances.set(1, (int)Math.round(goalL / samplesPerFoot));

    // Left / right wall delays
    pos = goalW * lrJitter;
    using[4] =(int)Math.round(pos / samplesPerFoot) * 2;
    using[5] = (int)Math.round((goalW - pos) / samplesPerFoot) * 2;
    this.resonances.set(2, (int)Math.round(goalW / samplesPerFoot));
    
    if (aToB) {
      this.delays2 = using;
    } else {
      this.delays = using;
    }

    updateReverbWindow();
  }

  // Gets a value from the circular buffer a given offset from the current index.
  // Only supports negative offsets, as this system does not support forward-looking filters.
  private float get(int offset) {
    int index = bufferIndex + offset;
    if (index < 0) {
      index += buffer.length;
    }
    return buffer[index];
  }

  // Update the minimum and maximum delays for the corresponding reverb
  private void updateReverbWindow() {
    this.reverbWindow[0] = (float)(((l + w + h) / 3.0) / samplesPerFoot);
    //this.reverbWindow[1] = ((l+w+h) / samplesPerFoot) * 2;
    this.reverbWindow[1] = (float)(Math.pow(l*w*h, 0.3) * 10 / samplesPerFoot);
    
    //println(this.reverbWindow);
  }
}
