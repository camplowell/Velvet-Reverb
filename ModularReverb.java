import beads.*;
import java.lang.Math.*;
import java.util.ArrayList;

class ModularReverb extends Function {
  VelvetDelay delay;
  BiquadComponent materialAttenuation;
  BiquadComponent airAttenuation;
  float prevOut;
  float reverberance;
  float goalReverberance;
  float velocity;


  public ModularReverb(UGen input, float[] window, int density, float reverberance, BiquadComponent materialAttenuation) {
    super(input);
    float sampleRate = context.getSampleRate();
    delay = new VelvetDelay((int)context.getSampleRate(), 23, window, Math.round(sampleRate / density), context.getSampleRate());
    this.reverberance = reverberance;
    this.goalReverberance = reverberance;
    this.velocity = 1.0f;
    this.materialAttenuation = materialAttenuation.register(this);
  }

  public ModularReverb(UGen input, VelvetDelay delay, BiquadComponent attenuation) {
    super(input);
    this.delay = delay;
    this.materialAttenuation = attenuation;
    this.prevOut = 0;
  }

  public float calculate() {
    // Register input
    float in = processInput();
    float sample = in + prevOut;
    
    // Delay
    sample = delay.calculate(sample);
    
    // Spectral decay
    sample = materialAttenuation.calculate(sample, this);
    
    // Artificial decay
    sample *= reverberance;
    
    if(goalReverberance != reverberance){
      reverberance += MathUtils.constrain(goalReverberance - reverberance, -velocity, velocity);
    }
    
    // Output
    prevOut = sample;
    return sample;
  }
  
  // Artificially change the reverberance of the room
  public void setOpenness(float openness){
    goalReverberance = 1 - openness;
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
  
  // Set the guranteed delays, which manifest as resonances
  public void setResonance(ArrayList<Integer> resonances){
    delay.setResonances(resonances);
  }
}
