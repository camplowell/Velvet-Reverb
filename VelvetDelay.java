import java.lang.Math.*;
import java.util.ArrayList;
import java.util.Random;

class VelvetDelay {
  private static final int REFDIST = 300;
  private static final float SOUNDSPD = 1125.33f;
  private static final float RESONANCEFAC = 0.1f;
  float[] buffer;
  int bufferIndex;
  ArrayList<ArrayList<Integer>> ticks;
  ArrayList<ArrayList<Float>> polarities;
  ArrayList<Integer> guranteedTicks;
  ArrayList<Integer> guranteedTicks2;
  float refOffset;
  float theta;
  float[] window;
  float velocity;

  float sampleRate;

  public VelvetDelay(int bufferSize, int rotations, float[] window, int binLength, float sampleRate) {
    this.buffer = new float[bufferSize];
    this.velocity = 1;
    this.window = window;
    this.sampleRate = sampleRate;
    this.ticks = new ArrayList<ArrayList<Integer>>();
    this.polarities = new ArrayList<ArrayList<Float>>();
    this.guranteedTicks = new ArrayList<Integer>();
    this.guranteedTicks2 = new ArrayList<Integer>();
    this.refOffset = (REFDIST / SOUNDSPD) * sampleRate;
    initTicks(rotations, binLength);
  }

  public float calculate(float in) {
    // Buffer input
    buffer[bufferIndex] = in;

    // Establish tick position
    int rotIndex = (int)(theta);
    int rotIndex2 = (rotIndex + 1) % ticks.size();
    float fade = theta - rotIndex;
    float sum = 0;

    // Perform and sum velvet delays
    if (fade != 1) {
      // Loop through first ticks and polarities
      sum += velvet(rotIndex) * Math.pow(1-fade, 0.5);
    }
    if (fade != 0) {
      // Loop through second ticks and polarities
      sum += velvet(rotIndex2) * Math.pow(fade, 0.5);
    }
    
    // Loop through guranteed ticks
    sum += guranteedVelvet();
    
    // Increment buffer and theta
    bufferIndex++;
    if (bufferIndex == buffer.length) {
      bufferIndex = 0;
    }
    theta += 0.5 / (window[0]);
    if (theta >= ticks.size()) {
      theta = 0;
    }
    return sum;
  }
  
  public void setResonances(ArrayList<Integer> resonances){
    this.guranteedTicks = resonances;
  }

  // Velvet delay for only a single rotated index.
  private float velvet(int rotation) {
    ArrayList<Integer> tickList = ticks.get(rotation);
    ArrayList<Float> polarityList = polarities.get(rotation);
    float sum = 0;
  
    for (int i = 0; i < tickList.size(); i++) {
      int offset = tickList.get(i);
      if (offset > window[0]) {
        sum += get(-1 * offset) * polarityList.get(i);
      }
      if (offset >= window[1]) {
        break;
      }
    }
    return sum;
  }
  
  // Add guranteed delays
  private float guranteedVelvet() {
    float sum = 0;
    int index = 0;
    for (Integer echo : guranteedTicks) {
        sum += get(-echo) / (1 + echo / refOffset);
        index++;
    }
    return sum * RESONANCEFAC;
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

  // Initialize tick and polarity arrays
  private void initTicks(int rotations, int binLength) {
    Random random = new Random();
    // Establish this rotation
    ArrayList<Integer> tickList = new ArrayList<Integer>();
    ArrayList<Float> polarityList = new ArrayList<Float>();
    // Loop through bins, adding jittered echoes
    int bins = (int)(sampleRate / binLength);
    for (int bin = 0; bin < bins; bin++) {
      int binStart = bin * binLength;
      // Randomly set offset within bin
      int offset = random.nextInt(binLength);
      // Set a random polarity
      float polarity = random.nextInt(2) * 2 - 1;

      float attenuation = 1+((binStart + offset) / refOffset);
      // Add the delay to the tick list
      tickList.add((int)(binStart+offset));
      polarityList.add((float)((polarity / attenuation) * Math.pow(binLength / sampleRate, 0.45)));
    }
    // Add to collection
    ticks.add(tickList);
    polarities.add(polarityList);
    // Check if stopping. If not, recur
    if (rotations > 1) {
      initTicks(rotations-1, binLength);
    }
  }
}
