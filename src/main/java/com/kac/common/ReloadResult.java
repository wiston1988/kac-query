package com.kac.common;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ReloadResult
  extends Result
{
  private static final long serialVersionUID = 8101L;
  public static final int RELOAD_SUCC = 0;
  public static final int RELOAD_FAIL = 1;
  public static final int RELOAD_GETRESULTS_EXCEPTION = 2;
  private transient String ip = "";
  
  private transient short port;
  
  private Map<String, ReloadResultInfo> typeAckResultMap = new HashMap();
  
  public Map<String, ReloadResultInfo> getTypeAckResultMap() {
    return this.typeAckResultMap;
  }
  
  public void addTypeResult(String type, ReloadResultInfo reloadResultInfo) {
    this.typeAckResultMap.put(type, reloadResultInfo);
  }
  
  public int getSize() {
    return this.typeAckResultMap.size();
  }
  
  public String getIp() {
    return this.ip;
  }
  
  public void setIp(String ip) {
    this.ip = ip;
  }
  
  public short getPort() {
    return this.port;
  }
  
  public void setPort(short port) {
    this.port = port;
  }
  
  public String toString() {
    StringBuffer buff = new StringBuffer();
    buff.append("ReloadResult: \n");
    buff.append("IP : " + this.ip + "\t" + " Port : " + this.port + "\n");
    
    Iterator it = this.typeAckResultMap.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry entry = (Map.Entry)it.next();
      buff.append(entry.getKey() + "\t" + entry.getValue());
      buff.append("\n");
    }
    return buff.toString();
  }
}
