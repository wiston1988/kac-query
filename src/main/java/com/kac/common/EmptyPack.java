package com.kac.common;

public class EmptyPack
  extends Package
{
  public EmptyPack(short control)
  {
    setControl(control);
  }
  
  public void build()
    throws DispatchException
  {}
  
  public void extract() throws DispatchException
  {}
  
  public int calcPacketDataLength() throws DispatchException
  {
    return 0;
  }
}
