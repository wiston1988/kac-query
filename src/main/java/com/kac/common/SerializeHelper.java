package com.kac.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class SerializeHelper
{
  public static ByteBuffer encodeObject(Object[] objs)
    throws IOException
  {
    ByteArrayOutputStream bao = new ByteArrayOutputStream();
    ByteBuffer buffer = null;
    ObjectOutputStream oo = new ObjectOutputStream(bao);
    for (int i = 0; i < objs.length; i++) {
      oo.writeObject(objs[i]);
    }
    buffer = ByteBuffer.wrap(bao.toByteArray());
    return buffer;
  }
  
  public static Object[] decodeObject(ByteBuffer buffer)
    throws IOException, ClassNotFoundException
  {
    ByteArrayInputStream bai = new ByteArrayInputStream(buffer.array());
    ObjectInputStream oi = new ObjectInputStream(bai);
    ArrayList<Object> list = new ArrayList();
    try {
      for (;;) {
        Object b = oi.readObject();
        list.add(b);
      }
    }
    catch (EOFException e) {}
    
    return (Object[])list.toArray();
  }
}
