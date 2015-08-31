package com.kac.common;

import java.io.Serializable;
import java.util.ArrayList;

public class MultiQuery
  implements Serializable
{
  static final long serialVersionUID = 8007L;
  private ArrayList<Query> querys = new ArrayList();
  
  private int timeOut = 5000;
  
  public int size()
  {
    return this.querys.size();
  }
  
  public void addQuery(Query q)
    throws DispatchException
  {
    if (q == null)
      throw new DispatchException("Query null");
    this.querys.add(q);
  }
  
  public Query getQuery(int i)
  {
    return (Query)this.querys.get(i);
  }
  
  public void setTimeOut(int timeout) {
    this.timeOut = timeout;
  }
  
  public int getTimeOut() {
    return this.timeOut;
  }
  
  public boolean equals(Object obj) {
    if ((obj instanceof MultiQuery)) {
      MultiQuery q = (MultiQuery)obj;
      
      if (q.getTimeOut() != getTimeOut()) {
        return false;
      }
      
      if (q.size() != size()) {
        return false;
      }
      
      for (int i = 0; i < size(); i++) {
        Query q1 = getQuery(i);
        Query q2 = q.getQuery(i);
        if (!q1.equals(q2))
          return false;
      }
      return true;
    }
    return false;
  }
  
  public String toString() {
    StringBuffer buff = new StringBuffer();
    buff.append(this.timeOut + "\r\n");
    for (Query query : this.querys) {
      buff.append(query.toString() + "\r\n");
    }
    buff.append("\r\n");
    return buff.toString();
  }
}
