package com.kac.server;

import com.kac.common.DispatchException;
import com.kac.common.Query;
import com.kac.common.Result;

public abstract interface Service
{
  public abstract void init()
    throws DispatchException;
  
  public abstract Result GetResult(Query paramQuery);
  
  public abstract String getServiceName();
  
  public abstract void reload()
    throws DispatchException;
  
  public abstract void close();
}
