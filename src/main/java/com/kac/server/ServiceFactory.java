package com.kac.server;

import com.kac.common.DispatchException;
import com.kac.common.EmptyPack;
import com.kac.common.MultiQuery;
import com.kac.common.MultiRequestPack;
import com.kac.common.MultiResponsePack;
import com.kac.common.MultiResult;
import com.kac.common.Package;
import com.kac.common.Query;
import com.kac.common.ReloadQuery;
import com.kac.common.ReloadResult;
import com.kac.common.ReloadResultInfo;
import com.kac.common.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

public class ServiceFactory
  implements Service
{
  private static final Logger logger = Logger.getLogger(ServiceFactory.class);
  
  public static final int REPORTINTERVAL = 10000;
  
  private ThreadPoolExecutor processThreads = null;
  
  private static int threadpoolQueueSize = 50;
  
  private static int threadNum = 30;
  
  private static int threadMaxNum = 30;
  
  private EventCenter monitor = null;
  
  private Map<String, Service> servicesMap = new HashMap();
  
  private long requestCount = 0L;
  
  private long startTime = 0L;
  
  private String[] serviceNames = null;
  
  private String[] reqNames = null;
  
  public ServiceFactory(EventCenter monitor, String[] fileNames, String[] reqTypes) throws DispatchException
  {
    if (monitor == null) {
      throw new NullPointerException("monitor can not be null");
    }
    if (fileNames == null) {
      throw new NullPointerException("serviceNames can not be null");
    }
    if (reqTypes == null) {
      throw new NullPointerException("reqNames can not be null");
    }
    if (fileNames.length != reqTypes.length) {
      throw new DispatchException("length of fileNames must be equal with length of reqtypes");
    }
    
    this.monitor = monitor;
    this.serviceNames = fileNames;
    this.reqNames = reqTypes;
  }
  
  public static void setCoreThreadNum(int num) {
    if (num > 0) {
      threadNum = num;
    } else {
      throw new IllegalArgumentException("the number of thread must be positive");
    }
  }
  
  public static void setMaxCoreThreadNum(int num)
  {
    if (num > 0) {
      threadMaxNum = num;
    } else {
      throw new IllegalArgumentException("max number of thread must be positive");
    }
  }
  
  public static void setPoolQueueLen(int len)
  {
    if (len > 0) {
      threadpoolQueueSize = len;
    } else {
      throw new IllegalArgumentException("size of queque must be positive");
    }
  }
  
  public void init()
    throws DispatchException
  {
    String tmpClassName = null;
    Class tmpClass = null;
    
    Service service = null;
    try {
      for (int i = 0; i < this.serviceNames.length; i++)
      {
        tmpClassName = this.serviceNames[i];
        
        tmpClass = ServiceFactory.class.getClassLoader().loadClass(tmpClassName);
        
        service = (Service)tmpClass.newInstance();
        service.init();
        this.servicesMap.put(this.reqNames[i], service);
      }
      this.servicesMap.put(ReloadQuery.class.getName(), this);
    } catch (Exception e) {
      logger.error("", e);
      logger.error(new StringBuilder().append(service.getServiceName()).append("初始化失败").toString());
      logger.error("正在关闭已开启服务...");
      Iterator<Map.Entry<String, Service>> iter = this.servicesMap.entrySet().iterator();
      
      while (iter.hasNext()) {
        ((Service)((Map.Entry)iter.next()).getValue()).close();
      }
      throw new DispatchException(e);
    }
    logger.info("");
    logger.info("全部服务初始化成功");
    this.startTime = System.currentTimeMillis();
  }
  
  protected void finalize() throws InterruptedException {
    if (this.processThreads != null) {
      this.processThreads.shutdown();
    }
  }
  
  public void startAgent() {
    logger.info(new StringBuilder().append("核心线程数：").append(threadNum).append(";最大并发线程数: ").append(threadMaxNum).append(";停留时间:").append(threadNum < 10 ? 10 : 2).append("秒；队列长度:").append(threadpoolQueueSize).toString());
    
    ThreadFactory threadFactory = EventCenter.getNamedThreadFactory("ProcessThread");
    
    this.processThreads = new ThreadPoolExecutor(threadNum, threadMaxNum, threadMaxNum < 10 ? 10L : 2L, TimeUnit.SECONDS, new ArrayBlockingQueue(threadpoolQueueSize), threadFactory, new ThreadPoolExecutor.AbortPolicy());
  }
  
  public void shutdown()
  {
    if (this.processThreads != null) {
      this.processThreads.shutdown();
    }
    this.requestCount = 0L;
  }
  
  public void invokeProcess(Package req, ClientConnection conn) {
    try {
      this.processThreads.execute(new ProcessThread(req, conn));
      logger.debug(new StringBuilder().append(this.processThreads.getActiveCount()).append(" active threads").toString());
    } catch (RejectedExecutionException e) {
      Package failRes = makeFailPack(req);
      try {
        failRes.encode();
        logger.info(new StringBuilder().append(conn.toString()).append(":load high, send timeout to client").toString());
        
        if (conn != null) {
          conn.pushToSend(failRes);
        }
      } catch (Exception e1) {
        logger.error(new StringBuilder().append(conn.toString()).append(":sendQueue full with adding ").append(failRes).toString(), e);
      }
    }
    
    if (++this.requestCount % 10000L == 0L) {
      logger.info(new StringBuilder().append("have processed ").append(this.requestCount).append(" requests, using ").append((System.currentTimeMillis() - this.startTime) / 1000L).append(" seconds").toString());
    }
  }
  
  private void process(Package rs_req, ClientConnection conn)
  {
    if (rs_req != null) {
      Package rs_res = null;
      switch (rs_req.getControl()) {
      case 501: 
        rs_res = makeJoinAck(rs_req);
        logger.debug(new StringBuilder().append("发送joinAck给").append(conn.toString()).toString());
        break;
      case 504: 
        long start = 0L;
        if (logger.isDebugEnabled()) {
          start = System.currentTimeMillis();
          logger.debug(new StringBuilder().append("receive ").append(rs_req.getID()).append(" request").toString());
        }
        MultiQuery multiQuery = ((MultiRequestPack)rs_req).getMultiQuery();
        
        int size = multiQuery.size();
        Query query = null;
        MultiResult multiResult = new MultiResult();
        Service service = null;
        for (int i = 0; i < size; i++) {
          query = multiQuery.getQuery(i);
          service = (Service)this.servicesMap.get(query.getClass().getName());
          if (service == null) {
            logger.error(new StringBuilder().append("unkown service type:").append(query.getClass().toString()).toString());
          } else {
            try
            {
              multiResult.addResult(service.GetResult(query));
            } catch (DispatchException e) {
              logger.error("", e);
            }
          }
        }
        rs_res = new MultiResponsePack();
        ((MultiResponsePack)rs_res).setMultiResult(multiResult);
        if (logger.isDebugEnabled())
          logger.debug(new StringBuilder().append("send ").append(rs_req.getID()).append(" result using time: ").append(System.currentTimeMillis() - start).append(" ms").toString()); break;
      
      default: 
        rs_res = makeFailPack(rs_req);
        logger.error(new StringBuilder().append(conn.toString()).append(":unknown control ").append(rs_req).toString());
      }
      try
      {
        rs_res.setID(rs_req.getID());
        rs_res.encode();
      } catch (DispatchException e1) {
        logger.error(new StringBuilder().append(conn.toString()).append(":encode error ").append(rs_req).toString(), e1);
        return;
      }
      if (conn != null) {
        conn.pushToSend(rs_res);
      }
      try
      {
        this.monitor.regWriteEvent(conn);
      } catch (InterruptedException e) {
        logger.error(new StringBuilder().append(conn.toString()).append(":error in reg write event").toString(), e);
      }
    }
  }
  
  private static Package makeFailPack(Package req) {
    if (req == null) {
      throw new NullPointerException("request can not be null");
    }
    
    Package res = null;
    switch (req.getControl()) {
    case 504: 
      res = new EmptyPack((short)506);
      break;
    default: 
      throw new IllegalArgumentException(new StringBuilder().append("wrong control with ").append(req).toString());
    }
    return res;
  }
  
  public class ProcessThread implements Runnable {
    private Package rs_req = null;
    
    private ClientConnection connection = null;
    
    public ProcessThread(Package req, ClientConnection conn)
    {
      this.rs_req = req;
      this.connection = conn;
    }
    
    public void run() {
      ServiceFactory.this.process(this.rs_req, this.connection);
    }
  }
  
  private static Package makeJoinAck(Package req) {
    if (req == null) {
      throw new NullPointerException("request can not be null");
    }
    if (req.getControl() != 501) {
      throw new IllegalArgumentException(new StringBuilder().append("wrong control with ").append(req).toString());
    }
    EmptyPack res = new EmptyPack((short)502);
    return res;
  }
  
  public Result GetResult(Query q) {
    ReloadQuery query = (ReloadQuery)q;
    ArrayList<String> typeList = query.getTypeList();
    Service service = null;
    ReloadResult result = new ReloadResult();
    String message = null;
    int errorCount = 0;
    for (int i = 0; i < typeList.size(); i++) {
      service = (Service)this.servicesMap.get(typeList.get(i));
      if (service == null) {
        message = new StringBuilder().append("unknown service type:").append((String)typeList.get(i)).toString();
        logger.error(message);
        result.addTypeResult((String)typeList.get(i), new ReloadResultInfo(1, message));
      }
      else
      {
        try {
          logger.info("");
          logger.info("");
          logger.info(new StringBuilder().append("reloading ").append(service.getServiceName()).toString());
          service.reload();
          result.addTypeResult((String)typeList.get(i), new ReloadResultInfo(0, "reload ok"));
        }
        catch (Exception e) {
          errorCount++;
          message = new StringBuilder().append("fail to reload ").append(service.getServiceName()).append(" with message ").append(e.getMessage()).toString();
          
          logger.error(message);
          result.addTypeResult((String)typeList.get(i), new ReloadResultInfo(1, message));
        }
      }
    }
    logger.info("forcible garbage collection");
    System.gc();
    if (errorCount == 0) {
      logger.info("reload successfully");
      this.requestCount = 0L;
    } else {
      logger.info(new StringBuilder().append(errorCount).append(" errors").toString());
    }
    return result;
  }
  
  public String getServiceName() {
    return "serviceFactory";
  }
  
  public void close() {
    Iterator<Map.Entry<String, Service>> iter = this.servicesMap.entrySet().iterator();
    
    Service service = null;
    while (iter.hasNext()) {
      service = (Service)((Map.Entry)iter.next()).getValue();
      logger.info(new StringBuilder().append("closing ").append(service.getServiceName()).toString());
      service.close();
    }
    this.servicesMap.clear();
    this.servicesMap = null;
    logger.info("closing servicefactory");
  }
  
  public void reload() throws DispatchException {
    logger.error("not support factory reload");
  }
}
