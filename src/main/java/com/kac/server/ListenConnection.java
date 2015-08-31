package com.kac.server;

import com.kac.common.DispatchException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

public class ListenConnection
  implements ConnectionAction
{
  private static final Logger logger = Logger.getLogger(ListenConnection.class);
  
  private ServerSocketChannel m_serverSocketChannel;
  
  private int timeout;
  
  private EventCenter eventCenter;
  
  private SelectionKey selectionKey;
  
  private static final int STARTED = 0;
  
  private static final int STOPPED = 1;
  
  private static final int FAILED = 2;
  
  private int state = 1;
  
  private short listenPort;
  private String listenAddr;
  
  public ServerSocketChannel getServerSocketChannel()
  {
    return this.m_serverSocketChannel;
  }
  
  public void setServerSocketChannel(ServerSocketChannel serverSocketChannel) throws DispatchException
  {
    if ((this.state == 0) || (this.state == 2)) {
      throw new DispatchException("Protocol already started");
    }
    this.m_serverSocketChannel = serverSocketChannel;
  }
  
  public int getTimeOut() {
    return this.timeout;
  }
  
  public void setTimeOut(int timeout) throws DispatchException {
    if ((this.state == 0) || (this.state == 2)) {
      throw new DispatchException("Protocol already started");
    }
    this.timeout = timeout;
  }
  
  public EventCenter getEventCenter() {
    return this.eventCenter;
  }
  
  public void setEventCenter(EventCenter eCenter) throws DispatchException {
    if ((this.state == 0) || (this.state == 2)) {
      throw new DispatchException("Protocol already started");
    }
    this.eventCenter = eCenter;
  }
  
  public void init() throws Exception
  {
    this.m_serverSocketChannel = ServerSocketChannel.open();
    this.m_serverSocketChannel.socket().bind(new InetSocketAddress(this.listenAddr, this.listenPort));
    
    this.m_serverSocketChannel.socket().setSoTimeout(this.timeout);
    this.m_serverSocketChannel.configureBlocking(false);
    this.selectionKey = this.eventCenter.register(this.m_serverSocketChannel, 16, this);
    
    this.state = 0;
    this.eventCenter.startService();
    logger.info("监听" + this.listenAddr + ":" + this.listenPort + "......");
  }
  
  public ListenConnection(String ip, short port, EventCenter eCenter)
    throws DispatchException
  {
    this.state = 1;
    if (eCenter == null) {
      throw new DispatchException("initial error");
    }
    this.eventCenter = eCenter;
    this.listenPort = port;
    this.listenAddr = ip;
  }
  
  public SelectableChannel getRegChannel() {
    return null;
  }
  
  public void close() {
    this.selectionKey.cancel();
    try {
      this.m_serverSocketChannel.close();
    } catch (IOException e) {
      logger.error("close error", e);
    }
    this.state = 1;
  }
  
  public void tagStop() {
    this.state = 1;
  }
  
  public void invokeMessage(EventCenter.Message message)
  {
    if (message.isAcceptable()) {
      try {
        ServerSocketChannel server = (ServerSocketChannel)message.getSelectionKey().channel();
        
        SocketChannel channel = server.accept();
        if (channel == null)
          return;
        logger.info("接收到来自：" + channel + "客户端的连接");
        ClientConnection connection = new ClientConnection(getEventCenter(), channel, false);
        
        this.eventCenter.addToRegQueue(connection);
        this.eventCenter.addWantto(this.selectionKey, 16);
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
    }
  }
  
  public void setRegKey(SelectionKey key) {}
  
  public boolean isValid()
  {
    return true;
  }
}
