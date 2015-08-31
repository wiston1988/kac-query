package com.kac.common;

import java.nio.ByteBuffer;

public class PackageFactory
{
  private static void checkMagic(long magic)
    throws DispatchException
  {
    if (magic != ProtocolConstants.MAGIC) {
      throw new DispatchException("Magic Error:" + magic);
    }
  }
  
  public static Package genPackage(ByteBuffer headbuffer, ByteBuffer bodybuffer)
    throws DispatchException
  {
    if (headbuffer == null)
      throw new NullPointerException();
    headbuffer.rewind();
    short control = headbuffer.getShort();
    long magic = headbuffer.getLong();
    checkMagic(magic);
    Package pack = null;
    switch (control) {
    case 504: 
      pack = new MultiRequestPack();
      break;
    
    case 502: 
      pack = new EmptyPack((short)502);
      break;
    case 501: 
      pack = new EmptyPack((short)501);
      break;
    
    case 506: 
      pack = new EmptyPack((short)506);
      break;
    case 505: 
      pack = new MultiResponsePack();
      break;
    case 503: default: 
      throw new DispatchException("unknown control:" + control);
    }
    pack.setControl(control);
    pack.setID(headbuffer.getLong());
    if (bodybuffer != null)
      bodybuffer.rewind();
    pack.decode(bodybuffer);
    return pack;
  }
}
