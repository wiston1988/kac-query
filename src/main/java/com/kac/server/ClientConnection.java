package com.kac.server;

import com.kac.common.DispatchException;
import com.kac.common.Package;
import com.kac.common.PackageFactory;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

public class ClientConnection
  implements ConnectionAction
{
  private static final Logger logger = Logger.getLogger(ClientConnection.class);
  
  private static final int HEAD_LENGTH = 22;
  
  private static final int SERVED = 0;
  
  private static final int STARTED = 1;
  
  private static final int STOPPED = 2;
  
  private SocketChannel socketChannel;
  
  private String ip;
  
  private EventCenter eventCenter;
  
  private SelectionKey selectionKey;
  
  private int status = 2;
  
  private ByteBuffer headerBuffer;
  
  private ByteBuffer bodyBuffer;
  
  private Object readLock;
  
  private boolean directReg = false;
  
  private LinkedBlockingQueue<ByteBuffer> sendQueue = new LinkedBlockingQueue();
  
  private ByteBuffer[] defaultBuffer = new ByteBuffer[0];
  
  private long startTime = 0L;
  
  private int closeCount = 0;
  
  private void clearAll() {
    if (this.sendQueue != null) {
      synchronized (this.sendQueue) {
        this.sendQueue.clear();
      }
      this.sendQueue = null;
    }
    if (this.closeCount > 1)
    {
      if (this.headerBuffer != null) {
        this.headerBuffer.clear();
      }
      if (this.bodyBuffer != null) {
        this.bodyBuffer.clear();
      }
    } else {
      synchronized (this.readLock) {
        if (this.headerBuffer != null) {
          this.headerBuffer.clear();
        }
        if (this.bodyBuffer != null) {
          this.bodyBuffer.clear();
        }
      }
    }
    if (this.selectionKey != null) {
      this.selectionKey.cancel();
      this.selectionKey = null;
    }
  }
  
  private void setAddr(String add) {
    this.ip = add.substring(add.indexOf("/") + 1);
    this.ip = this.ip.substring(0, this.ip.indexOf(":"));
  }
  
  public void pushToSend(Package rs_res)
  {
    if (this.status == 2) {
      return;
    }
    synchronized (this.sendQueue) {
      try {
        this.sendQueue.put(rs_res.getBuffer());
      } catch (InterruptedException e) {
        logger.error(toString() + ":sendQueue full with adding ", e);
      }
    }
  }
  
  public ClientConnection(EventCenter ecenter, SocketChannel chan, boolean reg) throws DispatchException
  {
    this.socketChannel = chan;
    
    this.directReg = reg;
    this.eventCenter = ecenter;
  }
  
  public SelectionKey getKey() {
    return this.socketChannel.keyFor(this.eventCenter.getSelector());
  }
  
  public SocketChannel getSocketChannel() {
    return this.socketChannel;
  }
  
  public void setSocketChannel(SocketChannel socketChannel) {
    this.socketChannel = socketChannel;
  }
  
  public EventCenter getEventCenter() {
    return this.eventCenter;
  }
  
  public void setEventCenter(EventCenter eventCenter) throws DispatchException
  {
    if (this.status == 1) {
      throw new DispatchException("service already started");
    }
    this.eventCenter = eventCenter;
  }
  
  public boolean isValid() {
    if (this.status == 0) {
      return true;
    }
    if ((this.startTime > 0L) && (System.currentTimeMillis() - this.startTime > FTRelateSearchParam.getGSTime() * 1000L))
    {
      return false;
    }
    return true;
  }
  
  public void invokeMessage(EventCenter.Message message)
  {
    try
    {
      if (message.isReadable()) {
        synchronized (this.readLock) {
          startRead();
        }
      }
    } catch (CancelledKeyException e) {
      logger.error(toString() + ":[exception]:  " + e.getMessage(), e);
    }
  }
  
  public void close() {
    if (this.closeCount > 0) {
      return;
    }
    this.closeCount += 1;
    synchronized (this) {
      if ((this.status == 1) || (this.status == 0)) {
        if (this.ip != null) {
          int sum = getEventCenter().subClient();
          logger.info(this.ip + " 退出,当前共有" + sum + "客户端连接");
        } else {
          logger.info(toString() + ":垃圾连接被清除:" + this.socketChannel);
        }
        try {
          this.eventCenter.closeChannel(this.socketChannel);
        } catch (Throwable e) {
          logger.error(toString() + ":[information]:  " + e.getMessage(), e);
        }
        
        if (logger.isDebugEnabled()) {
          logger.debug(toString() + ":[information]:  closing......");
        }
      }
      this.status = 2;
    }
    clearAll();
  }
  
  public SelectableChannel getRegChannel() {
    return this.socketChannel;
  }
  
  private void init() throws DispatchException {
    this.headerBuffer = ByteBuffer.allocate(22);
    this.readLock = new Object();
    try {
      if (this.directReg) {
        this.socketChannel.configureBlocking(false);
        
        this.selectionKey = this.eventCenter.register(this.socketChannel, 1, this);
      }
    }
    catch (ClosedChannelException e) {
      this.status = 2;
      throw new DispatchException(e);
    } catch (IOException e) {
      this.status = 2;
      throw new DispatchException(e);
    }
    this.status = 1;
    this.startTime = System.currentTimeMillis();
  }
  
  public void startRead()
  {
    try
    {
      for (;;)
      {
        int size = 0;
        if (this.headerBuffer.hasRemaining()) {
          long count = this.socketChannel.read(this.headerBuffer);
          if (count == -1L) {
            close();
            return;
          }
          
          if (this.headerBuffer.hasRemaining()) {
            this.eventCenter.addWantto(this.selectionKey, 1);
            
            break;
          }
          
          this.headerBuffer.flip();
          short control = this.headerBuffer.getShort();
          this.headerBuffer.getLong();
          this.headerBuffer.getLong();
          size = this.headerBuffer.getInt();
          
          if (size == 0)
          {
            if (control == 501) {
              setAddr(this.socketChannel.socket().getRemoteSocketAddress().toString());
              
              Package req = PackageFactory.genPackage(this.headerBuffer, this.bodyBuffer);
              
              getEventCenter().getServiceFactory().invokeProcess(req, this);
              
              int sum = getEventCenter().addClient();
              logger.info("客户端:" + this.ip + " 成功加入,当前共有" + sum + "客户端连接");
              
              this.status = 0;
              this.headerBuffer.clear();
              this.eventCenter.addWantto(this.selectionKey, 1);
            }
            else {
              logger.info("unknown control" + control);
              close();
            }
            return;
          }
          
          this.bodyBuffer = ByteBuffer.allocate(size);
          this.bodyBuffer.clear();
          this.bodyBuffer.limit(size);
        }
        
        if (this.bodyBuffer.hasRemaining()) {
          long count = this.socketChannel.read(this.bodyBuffer);
          if (count == -1L) {
            close();
            return;
          }
          if (this.bodyBuffer.hasRemaining()) {
            this.eventCenter.addWantto(this.selectionKey, 1);
            
            break;
          }
          
          this.bodyBuffer.flip();
          
          Package req = PackageFactory.genPackage(this.headerBuffer, this.bodyBuffer);
          
          getEventCenter().getServiceFactory().invokeProcess(req, this);
          
          this.bodyBuffer = null;
          this.headerBuffer.clear();
          this.eventCenter.addWantto(this.selectionKey, 1);
          return;
        }
      }
      
      this.eventCenter.addWantto(this.selectionKey, 1);
    } catch (CancelledKeyException e) {
      logger.error(toString() + ":key has been canceled, closing connection: " + e.getMessage(), e);
      
      close();
    } catch (ClosedChannelException e) {
      logger.error(toString() + ":channel unavailable, closing connection: " + e.getMessage(), e);
      
      close();
    } catch (IOException e) {
      logger.error(toString() + ":Communications error, closing connection: " + e.getMessage(), e);
      
      close();
    } catch (Throwable e) {
      StringBuffer message = new StringBuffer();
      StackTraceElement[] elem = e.getStackTrace();
      for (int i = 0; i < elem.length; i++)
        message.append("\n" + elem[i]);
      elem = null;
      logger.error(toString() + ":Unhandled error, closing connection" + message.toString(), e);
      
      message = null;
      close();
    }
  }
  
  public String toString() {
    return this.socketChannel == null ? "null connection" : this.socketChannel.toString();
  }
  
  public synchronized void realSend()
  {
    if (this.status == 2) {
      if (this.sendQueue == null) {
        return;
      }
      synchronized (this.sendQueue) {
        this.sendQueue.clear();
        return;
      }
    }
    ByteBuffer[] sendContent = null;
    
    if (this.sendQueue.size() == 0) {
      return;
    }
    
    synchronized (this.sendQueue) {
      sendContent = (ByteBuffer[])this.sendQueue.toArray(this.defaultBuffer);
      this.sendQueue.clear();
    }
    
    for (int i = 0; i < sendContent.length; i++) {
      if (sendContent[i] != null)
      {
        while (sendContent[i].hasRemaining()) {
          int count = 0;
          try {
            count = this.socketChannel.write(sendContent[i]);
          } catch (IOException e1) {
            logger.error(toString() + ":io exception:" + e1.getMessage(), e1);
            
            close();
            return;
          }
          if (count == 0)
            try {
              Thread.sleep(1L);
            } catch (InterruptedException e) {
              logger.error(e.getMessage(), e);
            }
        }
      }
    }
  }
  
  public void setRegKey(SelectionKey key) {
    this.selectionKey = key;
    try {
      init();
    } catch (DispatchException e) {
      logger.error(toString() + ":error in socket", e);
      close();
    }
  }
}
