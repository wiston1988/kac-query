package com.kac.common;

import java.util.ArrayList;

public class ReloadQuery
  extends Query
{
  private static final long serialVersionUID = 8100L;
  private ArrayList<String> typeList = new ArrayList();
  
  public ArrayList<String> getTypeList()
  {
    return this.typeList;
  }
  
  public void setTypeList(ArrayList<String> typeList) {
    this.typeList = typeList;
  }
  
  public void addType(String type)
    throws DispatchException
  {
    if (type == null) {
      throw new DispatchException("type can't be null");
    }
    
    this.typeList.add(type);
  }
  
  public String getType(int i)
  {
    return (String)this.typeList.get(i);
  }
  
  public int size() {
    return this.typeList.size();
  }
  
  public boolean equals(Object obj) {
    if ((obj instanceof ReloadQuery)) {
      ReloadQuery other = (ReloadQuery)obj;
      if (size() != other.size()) {
        return false;
      }
      
      int size = size();
      for (int i = 0; i < size; i++) {
        String a = getType(i);
        String b = other.getType(i);
        if (!a.equals(b)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }
  
  public String toString()
  {
    StringBuffer buff = new StringBuffer();
    buff.append("ReloadQuery: \n");
    for (String type : this.typeList) {
      buff.append(type.toString() + "\t");
    }
    buff.append("\n");
    return buff.toString();
  }
}
