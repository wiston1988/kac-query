package com.kac.common;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MultiRequestPack
  extends Package
{
  private MultiQuery multiQuery = null;
  
  private byte[] content = null;
  
  private int len = -1;
  
  public void build() throws DispatchException {
    writeByteArray(this.content);
  }
  
  public MultiRequestPack() {
    setControl((short)504);
  }
  
  public MultiRequestPack(MultiQuery multiQuery) {
    this.multiQuery = multiQuery;
    setControl((short)504);
  }
  
  public void setMultiQuery(MultiQuery multiQuery) {
    this.multiQuery = multiQuery;
    setControl((short)504);
  }
  
  public MultiQuery getMultiQuery() {
    return this.multiQuery;
  }
  
  public void extract() throws DispatchException {
    ByteBuffer buffer = ByteBuffer.wrap(readByteArray(remain()));
    try {
      Object[] resolved = SerializeHelper.decodeObject(buffer);
      if (resolved.length > 1) {
        throw new DispatchException("extract error");
      }
      this.multiQuery = ((MultiQuery)resolved[0]);
    } catch (Exception e) {
      throw new DispatchException(e.getMessage(), e);
    }
  }
  
  public boolean equals(Object s) {
    if (!(s instanceof MultiRequestPack)) {
      return false;
    }
    return (super.equals(s)) || (this.multiQuery.equals(((MultiRequestPack)s).getMultiQuery()));
  }
  
  public int calcPacketDataLength() throws DispatchException
  {
    if (this.len == -1) {
      try {
        this.content = SerializeHelper.encodeObject(new MultiQuery[] { this.multiQuery }).array();
        
        this.len = this.content.length;
      } catch (IOException e) {
        throw new DispatchException(e.getMessage(), e);
      }
    }
    return this.len;
  }
}
