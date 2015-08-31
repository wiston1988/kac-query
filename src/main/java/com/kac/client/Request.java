package com.kac.client;

import com.kac.common.MultiQuery;
import com.kac.common.MultiResult;

public class Request
{
  private MultiQuery multiQuery = null;
  
  private MultiResult multiResult = null;
  
  private Thread thread;
  private long RequestID;
  
  public Request(MultiQuery multiQuery, Thread thread, long requestID)
  {
    this.multiQuery = multiQuery;
    this.multiResult = new MultiResult();
    this.RequestID = requestID;
    this.thread = thread;
  }
  
  public MultiQuery getMultiQuery() {
    return this.multiQuery;
  }
  
  public void setMultiQuery(MultiQuery multiQuery) {
    this.multiQuery = multiQuery;
  }
  
  public MultiResult getMultiResult() {
    return this.multiResult;
  }
  
  public void setMultiResult(MultiResult multiResult) {
    this.multiResult = multiResult;
  }
  
  public Thread getThread() {
    return this.thread;
  }
  
  public void setThread(Thread thread) {
    this.thread = thread;
  }
  
  public long getRequestID() {
    return this.RequestID;
  }
  
  public void setRequestID(long requestID) {
    this.RequestID = requestID;
  }
}
