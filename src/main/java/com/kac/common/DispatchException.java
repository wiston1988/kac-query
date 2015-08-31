package com.kac.common;

public class DispatchException
  extends Exception
{
  private static final long serialVersionUID = -8916548284658012472L;
  
  public DispatchException() {}
  
  public DispatchException(String message)
  {
    super(message);
  }
  
  public DispatchException(Throwable t) {
    super(t);
  }
  
  public DispatchException(String message, Throwable cause) {
    super(message, cause);
  }
}
