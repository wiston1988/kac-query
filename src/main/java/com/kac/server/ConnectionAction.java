package com.kac.server;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

public abstract interface ConnectionAction
{
  public abstract void invokeMessage(EventCenter.Message paramMessage);
  
  public abstract void setRegKey(SelectionKey paramSelectionKey);
  
  public abstract SelectableChannel getRegChannel();
  
  public abstract boolean isValid();
  
  public abstract void close();
}
