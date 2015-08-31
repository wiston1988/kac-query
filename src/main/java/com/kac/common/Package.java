package com.kac.common;

import java.nio.ByteBuffer;

public abstract class Package
{
  private short control = 0;
  
  private static long magic = ProtocolConstants.MAGIC;
  
  public abstract void build()
    throws DispatchException;
  
  private long id = 0L;
  
  public abstract void extract() throws DispatchException;
  
  public void setID(long id) {
    this.id = id; }
  
  public long getID()
  {
    return this.id;
  }
  
  public abstract int calcPacketDataLength()
    throws DispatchException;
  
  public void encode()
    throws DispatchException
  {
    if (this.buffer != null) {
      this.buffer.clear();
    }
    this.buffer = ByteBuffer.allocate(22 + calcPacketDataLength());
    
    buildFullHead();
    build();
    this.buffer.flip();
  }
  
  private ByteBuffer buffer;
  
  private void buildFullHead()
    throws DispatchException
  {
    writeShort(getControl());
    writeLong(magic);
    writeLong(this.id);
    writeInt(calcPacketDataLength());
  }
  
  protected void decode(ByteBuffer buffer)
    throws DispatchException
  {
    if (buffer == null) {
      return;
    }
    if (buffer.remaining() == 0) {
      return;
    }
    if (this.buffer != null) {
      this.buffer.clear();
      this.buffer = null;
    }
    this.buffer = buffer;
    buffer.rewind();
    extract();
  }
  
  public int remain() {
    return this.buffer.remaining();
  }
  
  public ByteBuffer getBuffer() {
    return this.buffer;
  }
  
  public short getControl() {
    return this.control;
  }
  
  public void setControl(short control) {
    this.control = control;
  }
  
  public void writeShort(short in) throws DispatchException {
    if (this.buffer != null) {
      this.buffer.putShort(in);
    } else {
      throw new DispatchException("package has not been initialized");
    }
  }
  
  public short readShort() throws DispatchException {
    if (this.buffer != null) {
      return this.buffer.getShort();
    }
    throw new DispatchException("package has not been initialized");
  }
  
  public void writeInt(int in) throws DispatchException
  {
    if (this.buffer != null) {
      this.buffer.putInt(in);
    } else {
      throw new DispatchException("package has not been initialized");
    }
  }
  
  public int readInt() throws DispatchException {
    if (this.buffer != null) {
      return this.buffer.getInt();
    }
    throw new DispatchException("package has not been initialized");
  }
  
  public void writeLong(long in) throws DispatchException
  {
    if (this.buffer != null) {
      this.buffer.putLong(in);
    } else {
      throw new DispatchException("package has not been initialized");
    }
  }
  
  public long readLong() throws DispatchException {
    if (this.buffer != null) {
      return this.buffer.getLong();
    }
    throw new DispatchException("package has not been initialized");
  }
  
  public void writeByte(byte in) throws DispatchException
  {
    if (this.buffer != null) {
      this.buffer.put(in);
    } else {
      throw new DispatchException("package has not been initialized");
    }
  }
  
  public byte readByte() throws DispatchException {
    if (this.buffer != null) {
      return this.buffer.get();
    }
    throw new DispatchException("package has not been initialized");
  }
  
  public byte[] readByteArray(int len) throws DispatchException
  {
    if (this.buffer != null) {
      int actualLen = len < this.buffer.remaining() ? len : this.buffer.remaining();
      
      byte[] result = new byte[actualLen];
      this.buffer.get(result);
      return result;
    }
    throw new DispatchException("package has not been initialized");
  }
  
  public void writeString(String in) throws DispatchException
  {
    if (this.buffer != null) {
      if (in == null)
        throw new NullPointerException("string is null");
      int len = in.length();
      writeInt(len);
      for (int i = 0; i < in.length(); i++) {
        this.buffer.putChar(in.charAt(i));
      }
    } else {
      throw new DispatchException("package has not been initialized");
    }
  }
  
  public void writeByteArray(byte[] in) throws DispatchException {
    if (this.buffer != null) {
      if (in == null)
        throw new NullPointerException("byte array is null");
      this.buffer.put(in);
    } else {
      throw new DispatchException("package has not been initialized");
    }
  }
  
  public String readString() throws DispatchException {
    if (this.buffer != null) {
      int len = readInt();
      if (len == 0)
        return "";
      StringBuffer buf = null;
      buf = new StringBuffer(len);
      for (int i = 0; i < len; i++) {
        buf.append(this.buffer.getChar());
      }
      return buf.toString();
    }
    throw new DispatchException("package has not been initialized");
  }
  
  public boolean equals(Object s)
  {
    if (s == null) {
      return false;
    }
    if (!(s instanceof Package)) {
      return false;
    }
    Package o = (Package)s;
    return getControl() == o.getControl();
  }
}
