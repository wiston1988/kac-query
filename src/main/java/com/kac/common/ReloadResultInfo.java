package com.kac.common;

import java.io.Serializable;

public class ReloadResultInfo
  implements Serializable
{
  private static final long serialVersionUID = 8102L;
  private int status;
  private String message = "";
  
  public ReloadResultInfo() {}
  
  public ReloadResultInfo(int status, String message)
  {
    this.status = status;
    this.message = message;
  }
  
  public String getMessage() {
    return this.message;
  }
  
  public void setMessage(String message) {
    this.message = message;
  }
  
  public int getStatus() {
    return this.status;
  }
  
  public void setStatus(int status) {
    this.status = status;
  }
  
  public boolean equals(Object obj) {
    if ((obj instanceof ReloadResultInfo)) {
      ReloadResultInfo info = (ReloadResultInfo)obj;
      return (info.getMessage().equals(getMessage())) && (info.getStatus() == getStatus());
    }
    
    return false;
  }
  
  public String toString() {
    StringBuffer buff = new StringBuffer();
    buff.append("ReloadResultInfo: \n");
    buff.append(this.status + " : " + "\t" + this.message);
    buff.append("\n");
    return buff.toString();
  }
}
