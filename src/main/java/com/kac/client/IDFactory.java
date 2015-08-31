package com.kac.client;

public class IDFactory
{
  private long id = 0L;
  
  public synchronized long next()
  {
    return ++this.id % Long.MAX_VALUE;
  }
}
