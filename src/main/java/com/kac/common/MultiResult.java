package com.kac.common;

import java.io.Serializable;
import java.util.ArrayList;

public class MultiResult
  implements Serializable
{
  static final long serialVersionUID = 8008L;
  private ArrayList<Result> results = new ArrayList();
  
  public int size()
  {
    return this.results.size();
  }
  
  public void addResult(Result q)
    throws DispatchException
  {
    if (q == null)
      throw new DispatchException("result null");
    this.results.add(q);
  }
  
  public Result getResult(int i)
  {
    return (Result)this.results.get(i);
  }
  
  public String toString() {
    StringBuffer buff = new StringBuffer();
    for (Result result : this.results) {
      buff.append(result.toString() + "\r\n");
    }
    buff.append("\r\n");
    return buff.toString();
  }
}
