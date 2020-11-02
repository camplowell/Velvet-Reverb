import beads.*;
import java.lang.Math.*;

class Ducker extends Function {
  private float[] buffer;
  private int bufferIndex;
  private float gain;
  private float targetGain;
  private int delaySinceClip;
  private int attack;
  private int sustain;
  private float decay;

  // Create a Ducker with a given target maximum value
  public Ducker(UGen input) {
    super(input);
    this.gain = 1;
    this.targetGain = 1;
    this.delaySinceClip = 0;
    this.attack = 1024;
    this.sustain = 8192;
    this.decay = 0.5f / context.getSampleRate();
    this.buffer = new float[attack];
  }

  public float calculate() {
    // Gather input
    float input = processInput();
    // Detect clipping, and set target gain accordingly.
    if (input * targetGain > 1.0) {
      this.targetGain = 1.0f / input;
      this.delaySinceClip = 0;
    }
    // Modify real gain towards or away from target gain.
    if (gain != targetGain) {
      // Gain needs to be modified
      if (gain > targetGain) {
        // Use attack velocity
        float velocity = (targetGain - 1) / attack; // Should be negative
        if((gain+velocity) < targetGain){
          gain = targetGain;
        }else{
          gain += velocity;
        }
      }else{
        // Use decay velocity
        if(gain+decay > targetGain){
          gain = targetGain;
        }else{
          gain += decay;
        }
      }
    } else {
      if(delaySinceClip == attack+sustain){
        targetGain = 1;
      }
    }

    // Apply delay
    float sample = this.buffer[bufferIndex];
    this.buffer[bufferIndex] = input;

    // Iterate buffer index
    this.bufferIndex ++;
    if (this.bufferIndex == buffer.length) {
      this.bufferIndex = 0;
    }
    delaySinceClip++;

    // Return delayed sample modified by undelayed gain.
    return sample * gain;
  }

  // Compile all inputs into a single channel
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
}
