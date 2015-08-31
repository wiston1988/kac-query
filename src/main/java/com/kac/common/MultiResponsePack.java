package com.kac.common;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MultiResponsePack
  extends Package
{
  private MultiResult multiResult = null;
  
  private byte[] content = null;
  
  private int len = -1;
  
  public MultiResponsePack(MultiResult multiResult) {
    this.multiResult = multiResult;
    setControl((short)505);
  }
  
  public MultiResponsePack() {
    setControl((short)505);
  }
  
  public void build() throws DispatchException {
    writeByteArray(this.content);
  }
  
  public void setMultiResult(MultiResult multiResult) {
    this.multiResult = multiResult;
    setControl((short)505);
  }
  
  public MultiResult getMultiResult() {
    return this.multiResult;
  }
  
  public void extract() throws DispatchException {
    ByteBuffer buffer = ByteBuffer.wrap(readByteArray(remain()));
    try
    {
      Object[] resolved = SerializeHelper.decodeObject(buffer);
      if (resolved.length > 1) {
        throw new DispatchException("decode error");
      }
      
      this.multiResult = ((MultiResult)resolved[0]);
    } catch (Exception e) {
      throw new DispatchException(e.getMessage(), e);
    }
  }
  
  public boolean equals(Object s) {
    if (s == null) {
      return false;
    }
    if (!(s instanceof MultiResponsePack)) {
      return false;
    }
    return (super.equals(s)) || (this.multiResult.equals(((MultiResponsePack)s).multiResult));
  }
  
  public int calcPacketDataLength() throws DispatchException
  {
    if (this.len == -1) {
      try {
        this.content = SerializeHelper.encodeObject(new MultiResult[] { this.multiResult }).array();
        
        this.len = this.content.length;
      } catch (IOException e) {
        throw new DispatchException(e);
      }
    }
    return this.len;
  }
}
