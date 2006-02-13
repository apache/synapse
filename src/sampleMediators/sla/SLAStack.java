package sampleMediators.sla;
import java.util.Collections;
import java.util.ArrayList;

public class SLAStack extends ArrayList
{
  public SLAStack()
  {
  }
  public void addRequest(SLAObject slaObject)
  {
      this.add(slaObject);
      if(this.size() > 1){
          Collections.sort(this);
      }
  }
}