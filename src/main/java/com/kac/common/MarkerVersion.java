package com.kac.common;

public class MarkerVersion
{
  static int makeInt(byte b1, byte b2, byte b3, byte b4)
  {
    int i1 = b1 << 24;
    int i2 = b2 << 16;
    int i3 = b3 << 8;
    int i4 = b4;
    return i1 + i2 + i3 + i4;
  }
  
  static long makeLong(int i1, int i2)
  {
    long l1 = i1 << 32;
    long l2 = i2;
    return l1 + l2;
  }
  
  static final int MARKER = makeInt((byte)77, (byte)77, (byte)83, (byte)83);
  
  static final int VERSION = makeInt((byte)50, (byte)48, (byte)48, (byte)48);
  
  static String getMarker()
  {
    int remain = MARKER;
    char[] letters = new char[4];
    letters[0] = ((char)(remain >> 24));
    remain &= 16777215;
    letters[1] = ((char)(remain >> 16));
    remain &= 65535;
    letters[2] = ((char)(remain >> 8));
    remain &= 255;
    letters[3] = ((char)remain);
    
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < 4; i++) {
      if (letters[i] != 0)
        buf.append(letters[i]);
    }
    return buf.toString();
  }
  
  static String getVersion() {
    int remain = VERSION;
    char c1 = (char)(remain >> 24);
    remain &= 16777215;
    char c2 = (char)(remain >> 16);
    remain &= 65535;
    char c3 = (char)(remain >> 8);
    remain &= 255;
    char c4 = (char)remain;
    return c1 + "." + c2 + "." + c3 + "." + c4;
  }
}
