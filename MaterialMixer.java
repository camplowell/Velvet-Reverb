import java.util.HashMap;


// Weighted mixer for multiple BiquadComponents.
// Weights are normalized internally, so feel free to put crazy weights on.
class MaterialMixer {
  private HashMap<String, BiquadComponent> components;
  private HashMap<String, Float> weights;
  private float totalWeight;
  
  public float calculate(float in){
    float sample = 0;
    for(String name: components.keySet()){
      // Pass the input through the biquad, then multiply it by its weight, then divide it by the total weights.
      sample += components.get(name).calculate(in, this) * weights.get(name) / totalWeight;
    }
    return sample;
  }
  
  public void put(String name, BiquadComponent filter, float weight){
    components.put(name, filter);
    weights.put(name, weight);
    updateTotal();
  }
  
  public void remove(String name){
    components.remove(name);
    weights.remove(name);
    updateTotal();
  }
  
  public void setWeight(String name, float weight){
    if(weights.containsKey(name)){
      weights.put(name, weight);
    }
    updateTotal();
  }
  
  // Private methods
  
  // Update the total weight of the mixer.
  private void updateTotal(){
    float sum = 0;
    for(float weight : weights.values()){
      sum += weight;
    }
    totalWeight = sum;
  }
}
