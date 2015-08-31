package com.kac.client;

import com.kac.common.DispatchException;
import com.kac.common.Package;
import com.kac.common.PackageFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

public class ServerConnection
{
  private static final Logger logger = Logger.getLogger(ServerConnection.class);
  
  private ByteBuffer head;
  
  private ByteBuffer body;
  
  private Socket socket;
  
  private String ip;
  
  private short port;
  
  private int timeOut;
  
  private int socketState;
  
  public ServerConnection()
  {
    this.head = null;
    this.body = null;
    this.socket = null;
  }
  
  public ServerConnection(String ip, short port, int timeOut) {
    this.ip = ip;
    this.port = port;
    this.timeOut = timeOut;
    this.socketState = 2;
  }
  
  public int getSocketState() {
    return this.socketState;
  }
  
  public void setSocketState(int state) {
    this.socketState = state;
  }
  
  public String getIP() {
    return this.ip;
  }
  
  public void setIP(String ip) {
    this.ip = ip;
  }
  
  public short getPort() {
    return this.port;
  }
  
  public void setPort(short port) {
    this.port = port;
  }
  
  public int getTimeOut() {
    return this.timeOut;
  }
  
  public void setTimeOut(int time) {
    this.timeOut = time;
  }
  
  public void connect() throws DispatchException {
    if (this.ip == null) {
      throw new DispatchException("ServerConnection: Connect sIP为null");
    }
    if (this.port <= 0) {
      throw new DispatchException("ServerConnection: Connect nPort <= 0");
    }
    if (this.timeOut < 0) {
      throw new DispatchException("ServerConnection: Connect nTimeOut < 0");
    }
    try
    {
      this.socket = new Socket(this.ip, this.port);
    }
    catch (Exception e)
    {
      try {
        if (this.socket != null) {
          this.socket.close();
        }
      } catch (IOException e1) {
        logger.error("close socket error.");
      }
      throw new DispatchException("connect to agent failed.", e);
    }
  }
  
  public void close() throws DispatchException {
    try {
      if (this.socket != null) {
        this.socket.close();
      }
    } catch (IOException e) {
      throw new DispatchException("close socket error.", e);
    }
  }
  
  public Package recv() throws DispatchException
  {
    int MAX_TRY_COUNT = 3;
    int try_count = 0;
    
    if (this.socket == null) {
      setSocketState(2);
      throw new DispatchException("ServerConnection: Recv m_socket is null");
    }
    
    DataInputStream input = null;
    
    try
    {
      input = new DataInputStream(this.socket.getInputStream());
    } catch (IOException e) {
      setSocketState(2);
      throw new DispatchException(e);
    }
    
    int headlen = 22;
    if (headlen > 134217728) {
      throw new DispatchException("Header Size invalid ");
    }
    this.head = ByteBuffer.allocate(headlen);
    byte[] byteBuffer = new byte[headlen];
    try
    {
      input.readFully(byteBuffer, 0, headlen);
    }
    catch (SocketTimeoutException e) {
      for (;;) {
        try {
          Thread.sleep(200L);
          logger.error("", e);
        }
        catch (Exception e2) {}
        
        try_count++;
        if (try_count > MAX_TRY_COUNT) {
          setSocketState(2);
          throw new DispatchException("read head error and try to many times", e);
        }
      }
    }
    catch (Exception e)
    {
      setSocketState(2);
      throw new DispatchException("read head error", e);
    }
    
    this.head.put(byteBuffer);
    
    this.head.flip();
    this.head.getShort();
    this.head.getLong();
    this.head.getLong();
    int bodysize = this.head.getInt();
    if (bodysize > 134217728) {
      throw new DispatchException("Body Size invalid ");
    }
    
    this.body = ByteBuffer.allocate(bodysize);
    byteBuffer = new byte[bodysize];
    try {
      input.readFully(byteBuffer, 0, bodysize);
    } catch (IOException e) {
      setSocketState(2);
      throw new DispatchException("read body error.", e);
    }
    
    this.body.put(byteBuffer);
    
    Package pack = null;
    try {
      pack = PackageFactory.genPackage(this.head, this.body);
    } catch (DispatchException e) {
      setSocketState(2);
      throw new DispatchException("ServerConnection: Recv generatePackage error", e);
    }
    
    if (pack == null) {
      throw new DispatchException("ServerConnection: pack is null error");
    }
    
    return pack;
  }
  
  public void send(Package pack) throws DispatchException
  {
    send(pack.getBuffer());
  }
  
  private void send(ByteBuffer buf) throws DispatchException {
    try {
      DataOutputStream out = new DataOutputStream(this.socket.getOutputStream());
      
      out.write(buf.array());
    } catch (IOException e) {
      setSocketState(2);
      throw new DispatchException("send data error.", e);
    }
  }
}
