package com.kac.client;

import com.kac.common.DispatchException;
import com.kac.common.EmptyPack;
import com.kac.common.MultiQuery;
import com.kac.common.MultiResult;
import com.kac.common.Package;
import com.kac.common.Query;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

public class Client
{
  private ServerConnection connection;
  private String ip;
  private short port;
  private int timeOut;
  private Map<Long, Request> resultmap;
  private Map<Long, Request> waitmap;
  private LinkedBlockingQueue<Request> requestqueue;
  private Object m_lock = new Object();
  
  private final Logger logger = Logger.getLogger(Client.class);
  
  private Thread sender = null;
  
  private Thread collector = null;
  
  private boolean closeClient = false;
  
  private IDFactory idFactory = null;
  
  private Global global = new Global();
  
  public Global getGlobal() {
    return this.global;
  }
  
  public void setMaxQueueLen(int len)
  {
    this.global.MAX_REQQUEUE_LEN = len;
  }
  
  public int getMaxQueueLen() {
    return this.global.MAX_REQQUEUE_LEN;
  }
  
  public void setConnTimeOut(int timeout)
  {
    this.global.MAX_TIME_OUT = timeout;
  }
  
  public int getConnTimeOut()
  {
    return this.global.MAX_TIME_OUT;
  }
  
  public int getSocketState()
  {
    return this.connection.getSocketState();
  }
  
  public boolean isClientClose()
  {
    return this.closeClient;
  }
  
  public void close()
  {
    this.closeClient = true;
    try {
      this.connection.close();
      this.sender.interrupt();
      this.collector.interrupt();
    }
    catch (Exception e)
    {
      this.logger.error("Client: close client error");
    }
    this.resultmap = null;
    this.waitmap = null;
    this.requestqueue = null;
  }
  
  public String getIP()
  {
    return this.ip;
  }
  
  public short getPort() {
    return this.port;
  }
  
  public synchronized void init(String ip, short port)
    throws DispatchException
  {
    if ((this.connection != null) && (getSocketState() == 0)) {
      throw new DispatchException("A connection already exists");
    }
    this.ip = ip;
    this.port = port;
    this.timeOut = this.global.MAX_TIME_OUT;
    
    this.logger.info("client init begin..., ip is : " + ip + " port is : " + port);
    this.idFactory = new IDFactory();
    this.connection = new ServerConnection(ip, port, this.timeOut);
    boolean isDone = false;
    int tryCount = 0;
    
    while (!isDone) {
      if (tryCount >= 3)
      {
        this.requestqueue = new LinkedBlockingQueue(this.global.MAX_REQQUEUE_LEN);
        this.waitmap = new ConcurrentHashMap();
        this.resultmap = new ConcurrentHashMap();
        this.connection.setSocketState(2);
        
        this.closeClient = false;
        this.sender = new Sender(this);
        this.sender.setName("Sender-" + this.ip + ":" + this.port);
        this.collector = new Collector(this);
        this.collector.setName("Collector-" + this.ip + ":" + this.port);
        this.sender.start();
        this.collector.start();
        
        this.logger.info("connect timeout, connected state is bad, ip is : " + ip + " port is : " + port);
        throw new DispatchException("initialize client failed, please check socket");
      }
      
      try
      {
        connectServer();
        
        joinServer();
        isDone = true;
      } catch (Throwable e) {
        tryCount++;
        this.logger.error("connect fail, already try " + tryCount + " times ip is : " + ip + " port is : " + port);
        isDone = false;
        try {
          Thread.sleep(5000L);
        } catch (Exception e2) {
          this.logger.error("connect fail, sleep error.");
        }
        
        this.connection.close();
        this.connection = new ServerConnection(ip, port, this.timeOut);
      }
    }
    
    this.requestqueue = new LinkedBlockingQueue(this.global.MAX_REQQUEUE_LEN);
    this.waitmap = new ConcurrentHashMap();
    this.resultmap = new ConcurrentHashMap();
    this.connection.setSocketState(0);
    this.logger.info("connect succeed");
    
    this.closeClient = false;
    this.sender = new Sender(this);
    this.sender.setName("Sender-" + this.ip + ":" + this.port);
    this.collector = new Collector(this);
    this.collector.setName("Collector-" + this.ip + ":" + this.port);
    this.sender.start();
    this.collector.start();
  }
  
  public boolean reInit()
  {
    if (this.closeClient == true) {
      return true;
    }
    
    synchronized (this.m_lock) {
      this.timeOut = this.global.MAX_TIME_OUT;
      
      if ((this.connection != null) && (this.connection.getSocketState() == 0))
      {
        return true;
      }
      
      if (this.connection == null) {
        this.connection = new ServerConnection(this.ip, this.port, this.timeOut);
      }
      this.connection.setSocketState(1);
      this.logger.info("reconnect begin");
      
      boolean isDone = false;
      int nTryCount = 0;
      while (!isDone) {
        try {
          if (isClientClose() == true) {
            return false;
          }
          this.connection.close();
          
          connectServer();
          
          joinServer();
          isDone = true;
        } catch (Exception e) {
          nTryCount++;
          this.logger.error("reconnect fail, already try " + nTryCount + " times ip is : " + this.ip + " port is : " + this.port);
          
          isDone = false;
          try {
            Thread.sleep(5000L);
          } catch (Exception e2) {
            this.logger.error("reconnect fail, sleep error ");
          }
        }
      }
      
      this.waitmap = new ConcurrentHashMap();
      this.resultmap = new ConcurrentHashMap();
      this.connection.setSocketState(0);
      this.logger.info("reconnect succeed");
      return true;
    }
  }
  
  private void connectServer()
    throws DispatchException
  {
    if (this.connection != null) {
      this.connection.connect();
    } else {
      throw new DispatchException("connection is null");
    }
  }
  
  private void joinServer()
    throws DispatchException
  {
    EmptyPack pack = new EmptyPack((short)501);
    try
    {
      pack.encode();
    } catch (DispatchException e) {
      throw new DispatchException("joinServer encode error");
    }
    
    this.connection.send(pack);
    
    Package succ = this.connection.recv();
    if (succ.getControl() != 502) {
      throw new DispatchException("join server failed.");
    }
  }
  
  public void sendPack(Package pack)
    throws DispatchException
  {
    this.connection.send(pack);
  }
  
  public Package recvPack()
    throws DispatchException
  {
    Package objResponse = this.connection.recv();
    
    return objResponse;
  }
  
  public Request fetchRequest()
  {
    Request req = null;
    try {
      req = (Request)this.requestqueue.take();
    } catch (InterruptedException e) {
      this.logger.error("");
    }
    
    return req;
  }
  
  public Request fetchMiddle(long reqID)
  {
    Long tmp = new Long(reqID);
    Request req = (Request)this.waitmap.get(tmp);
    
    return req;
  }
  
  public void addMiddle(Request req)
  {
    Long tmp = Long.valueOf(req.getRequestID());
    this.waitmap.put(tmp, req);
  }
  
  public Request fetchResult(long id) {
    Long tmp = new Long(id);
    Request req = (Request)this.resultmap.get(tmp);
    this.resultmap.remove(tmp);
    return req;
  }
  
  public void addResult(Request req) {
    Long tmp = new Long(req.getRequestID());
    this.resultmap.put(tmp, req);
  }
  
  public MultiResult getResults(MultiQuery multiQuery) throws DispatchException
  {
    if (multiQuery == null) {
      throw new DispatchException("multiQuery is null error");
    }
    if (multiQuery.size() == 0) {
      throw new DispatchException("multiQuery size is 0");
    }
    int size = multiQuery.size();
    for (int i = 0; i < size; i++) {
      Query qry = multiQuery.getQuery(i);
      if (qry == null) {
        throw new DispatchException("some query in multiQuery is null.");
      }
    }
    if (this.connection == null) {
      throw new DispatchException("connection is null error");
    }
    
    if (this.connection.getSocketState() != 0) {
      int state = this.connection.getSocketState();
      throw new DispatchException("socket state error, state: " + state);
    }
    
    Request req = new Request(multiQuery, Thread.currentThread(), this.idFactory.next());
    
    if (this.requestqueue.size() > this.global.MAX_REQQUEUE_LEN) {
      throw new DispatchException("requestQueue is full.");
    }
    
    this.requestqueue.add(req);
    addMiddle(req);
    try
    {
      synchronized (req) {
        req.wait(multiQuery.getTimeOut());
      }
    } catch (InterruptedException e) {
      throw new DispatchException(e);
    } finally {
      this.waitmap.remove(Long.valueOf(req.getRequestID()));
    }
    
    Request request = fetchResult(req.getRequestID());
    
    if (null == request) {
      throw new DispatchException("request is  null");
    }
    if (null == request.getMultiResult()) {
      throw new DispatchException("multiResult is null");
    }
    MultiResult res = request.getMultiResult();
    
    return res;
  }
}
