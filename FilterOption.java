class FilterOption{
  private float value;
  public final float min;
  public final float max;
  public final String name;
  public final String style;
  
  public FilterOption(String name, float min, float max, String style, float value){
    this.name = name;
    this.min = min;
    this.max = max;
    this.style = style;
    this.value = value;
  }
  
  // Sets the value of the input. Automatically clips to range.
  public float setValue(float value){
    this.value = MathUtils.constrain(value, this.min, this.max);
    return this.value;
  }
  
  // Accepts normalized (0-1) input, stores it as applied (min-max) space.
  public float setNormalized(float value){
    return  setValue(MathUtils.lerp(this.min, this.max, value));
  }
  
  // Gets the value of the input
  public float getValue(){
    return value;
  }
  
  // Returns the value of the input, normalized so that min is 0 and max is 1.
  public float getNormalized(){
    return (this.value - this.min) / (this.max - this.min);
  }
}
