package com.kac.server;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;

public class EventCenter
  implements Runnable
{
  private ThreadPoolExecutor sendthreadPool = null;
  
  private ThreadPoolExecutor readthreadPool = null;
  
  private ServiceFactory servicesFactory = null;
  
  private static final Logger logger = Logger.getLogger(EventCenter.class);
  
  private volatile boolean isRunning = false;
  
  private Selector selector = null;
  
  private long timeout;
  
  private static String localIp = null;
  
  private ThreadGroup threadGroup;
  
  private String threadName = "RSUBAGENT";
  
  private int startCounter;
  
  private Stack<SelectableChannel> bulk = new Stack();
  
  private int MaxConnect = 10000;
  
  private ArrayBlockingQueue<ConnectionAction> regQueue = new ArrayBlockingQueue(this.MaxConnect);
  
  private ArrayBlockingQueue<OpUnit> opQueue = new ArrayBlockingQueue(this.MaxConnect);
  
  private int corePoolSize = 5;
  
  private int maximumPoolSize = 10;
  
  private int keepAliveTime = 10;
  
  private int queueLen = 10000;
  
  private boolean isExit = false;
  
  private Map<SocketChannel, ClientConnection> writeEventMap = new ConcurrentHashMap();
  
  private Object sync = new Object();
  
  private int clientNum = 0;
  
  public synchronized int addClient() {
    return ++this.clientNum;
  }
  
  public synchronized int subClient() {
    return --this.clientNum;
  }
  
  public void regWriteEvent(ClientConnection connection)
    throws InterruptedException
  {
    if (connection != null) {
      synchronized (this.sync) {
        if (!this.writeEventMap.containsKey(connection.getSocketChannel())) {
          this.writeEventMap.put(connection.getSocketChannel(), connection);
        }
      }
      
      this.selector.wakeup();
    }
  }
  
  private void processWriteQueue()
  {
    if (this.writeEventMap.size() == 0) {
      return;
    }
    
    synchronized (this.sync) {
      Iterator<Map.Entry<SocketChannel, ClientConnection>> it = this.writeEventMap.entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry<SocketChannel, ClientConnection> entry = (Map.Entry)it.next();
        if (entry != null)
        {
          this.sendthreadPool.execute(new Message((ClientConnection)entry.getValue(), 4));
          
          it.remove();
        }
      }
    }
  }
  
  public void setThreadpoolPara(int core, int max, int alive, int queue) { this.corePoolSize = core;
    this.maximumPoolSize = max;
    this.keepAliveTime = alive;
    this.queueLen = queue;
  }
  
  public void setMaxServerConnect(int maxSize) { this.MaxConnect = maxSize; }
  
  public class OpUnit { private SelectionKey key;
    private int op;
    
    OpUnit(SelectionKey key, int op) { this.key = key;
      this.op = op;
    }
  }
  
  public class Message
    implements Runnable
  {
    private int operation;
    
    private SelectionKey key;
    
    ClientConnection conn = null;
    
    private Message(SelectionKey k, int op) {
      this.operation = op;
      this.key = k;
    }
    
    private Message(ClientConnection conn, int op) {
      this.conn = conn;
      this.operation = op;
    }
    
    public SelectionKey getSelectionKey() {
      return this.key;
    }
    
    public final boolean isReadable() {
      return (this.operation & 0x1) != 0;
    }
    
    public final boolean isWritable() {
      return (this.operation & 0x4) != 0;
    }
    
    public final boolean isAcceptable() {
      return (this.operation & 0x10) != 0;
    }
    
    public void run() {
      try {
        if (this.conn == null) {
          if (this.key.attachment() != null) {
            ((ConnectionAction)this.key.attachment()).invokeMessage(this);
          }
        }
        else {
          this.conn.realSend();
        }
      }
      catch (Throwable e) {
        EventCenter.logger.error("[exception in EventMonitor]", e);
      }
    }
  }
  
  public EventCenter() throws IOException {
    this.threadGroup = new ThreadGroup("DRDS");
    this.sendthreadPool = new ThreadPoolExecutor(this.corePoolSize, this.maximumPoolSize, this.keepAliveTime, TimeUnit.SECONDS, new ArrayBlockingQueue(this.queueLen), new NamedThreadFactory("Send-Thread"), new ThreadPoolExecutor.DiscardOldestPolicy());
    
    this.readthreadPool = new ThreadPoolExecutor(this.corePoolSize, this.maximumPoolSize, this.keepAliveTime, TimeUnit.SECONDS, new ArrayBlockingQueue(this.queueLen), new NamedThreadFactory("Read-Thread"), new ThreadPoolExecutor.DiscardOldestPolicy());
  }
  
  public static ThreadFactory getNamedThreadFactory(String namePrefix)
  {
    return new NamedThreadFactory(namePrefix);
  }
  
  static class NamedThreadFactory
    implements ThreadFactory
  {
    final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
    final AtomicInteger threadNumber = new AtomicInteger(1);
    final String namePrefix;
    
    public NamedThreadFactory(String namePrefix) {
      this.namePrefix = namePrefix;
    }
    
    public Thread newThread(Runnable r) {
      Thread thread = this.defaultFactory.newThread(r);
      thread.setName(this.namePrefix + "-" + this.threadNumber.getAndIncrement());
      return thread;
    }
  }
  
  public void addToRegQueue(ConnectionAction base)
    throws InterruptedException
  {
    synchronized (this.regQueue) {
      this.regQueue.put(base);
    }
    this.selector.wakeup();
  }
  
  private void registerQueue(Logger logger)
    throws IOException
  {
    synchronized (this.regQueue) {
      while (!this.regQueue.isEmpty()) {
        ConnectionAction connection = (ConnectionAction)this.regQueue.poll();
        
        if (connection != null) {
          SelectionKey key = register(connection.getRegChannel(), 1, connection);
          
          connection.setRegKey(key);
        }
      }
    }
  }
  
  private void registerOp()
  {
    synchronized (this.opQueue) {
      while (!this.opQueue.isEmpty()) {
        OpUnit option = (OpUnit)this.opQueue.poll();
        if ((option != null) && (option.key != null)) {
          option.key.interestOps(option.key.interestOps() | option.op);
        }
      }
    }
  }
  
  public long getTimeout()
  {
    return this.timeout;
  }
  
  public void setTimeout(long m_timeout) {
    this.timeout = m_timeout;
    logger.info(new StringBuilder().append("select 超时: ").append(m_timeout / 1000L).append(" 秒").toString());
  }
  
  public Selector getSelector() {
    return this.selector;
  }
  
  public String getThreadName() {
    return this.threadName;
  }
  
  public void closeChannel(SelectableChannel selectableChannel) throws IOException
  {
    synchronized (this.bulk) {
      this.selector.wakeup();
      this.bulk.push(selectableChannel);
    }
  }
  
  public void setThreadName(String m_threadName) {
    this.threadName = m_threadName;
  }
  
  public SelectionKey register(SelectableChannel selectableChannel, int ops, ConnectionAction connection) throws IOException
  {
    synchronized (this.bulk) {
      if (this.selector == null) {
        this.selector = Selector.open();
      } else {
        this.selector.wakeup();
      }
      selectableChannel.configureBlocking(false);
      SelectionKey key = selectableChannel.register(this.selector, ops, connection);
      
      return key;
    }
  }
  
  public void addWantto(SelectionKey selectorKey, int addOpts) throws InterruptedException
  {
    synchronized (this.opQueue) {
      this.opQueue.put(new OpUnit(selectorKey, addOpts));
    }
    this.selector.wakeup();
  }
  
  public void startService() throws Exception {
    this.startCounter += 1;
    if (this.startCounter == 1) {
      this.isRunning = true;
      new Thread(this.threadGroup, this, this.threadName).start();
    }
  }
  
  public void deleteOp(SelectionKey selectorKey, int subOpts) {
    synchronized (this.bulk) {
      this.selector.wakeup();
      selectorKey.interestOps(selectorKey.interestOps() & (subOpts ^ 0xFFFFFFFF));
    }
  }
  
  public void StopService() throws Exception {
    this.startCounter -= 1;
    if (this.startCounter == 0) {
      this.isRunning = false;
      this.selector.wakeup();
      while (!this.isExit) {
        Thread.sleep(10L);
      }
    }
  }
  
  private void processGarbageConn()
  {
    Iterator list = this.selector.keys().iterator();
    while (list.hasNext()) {
      SelectionKey key = (SelectionKey)list.next();
      ConnectionAction conn = (ConnectionAction)key.attachment();
      if ((conn != null) && (!conn.isValid())) {
        conn.close();
        conn = null;
      }
    }
  }
  
  private static void printKeyInfo(SelectionKey sk)
  {
    String s = new String();
    s = new StringBuilder().append("Att: ").append(sk.attachment() == null ? "no" : "yes").toString();
    s = new StringBuilder().append(s).append(", Read: ").append(sk.isReadable()).toString();
    s = new StringBuilder().append(s).append(", Acpt: ").append(sk.isAcceptable()).toString();
    s = new StringBuilder().append(s).append(", Cnct: ").append(sk.isConnectable()).toString();
    s = new StringBuilder().append(s).append(", Wrt: ").append(sk.isWritable()).toString();
    s = new StringBuilder().append(s).append(", Valid: ").append(sk.isValid()).toString();
    s = new StringBuilder().append(s).append(", Ops: ").append(sk.interestOps()).toString();
    logger.info(new StringBuilder().append("key is : ").append(s).toString());
  }
  
  public void run() {
    try {
      logger.debug(new StringBuilder().append("Selector m_timeout: ").append(this.timeout).toString());
      while (this.isRunning) {
        try
        {
          synchronized (this.bulk) {
            if (!this.bulk.isEmpty()) {
              Iterator iter = this.bulk.iterator();
              while (iter.hasNext()) {
                SelectableChannel selectableChannel = (SelectableChannel)iter.next();
                
                if (selectableChannel.keyFor(this.selector) != null) {
                  selectableChannel.keyFor(this.selector).cancel();
                }
                
                selectableChannel.close();
              }
              this.bulk.clear();
            }
          }
          
          if (this.selector.select(this.timeout) == 0) {
            Iterator list = this.selector.selectedKeys().iterator();
            while (list.hasNext()) {
              SelectionKey key = (SelectionKey)list.next();
              key.channel().close();
              key.cancel();
              list.remove();
            }
            processGarbageConn();
            registerQueue(logger);
            processWriteQueue();
            registerOp();
          }
          else
          {
            registerOp();
            registerQueue(logger);
            processWriteQueue();
            if (!this.isRunning) {
              Iterator iterator = this.selector.keys().iterator();
              while (iterator.hasNext()) {
                try {
                  SelectionKey key = (SelectionKey)iterator.next();
                  
                  key.channel().close();
                } catch (IOException e) {
                  logger.error(e.getMessage(), e);
                }
              }
              try {
                this.selector.close();
              } catch (IOException e) {
                logger.error(e.getMessage(), e);
              }
            }
            
            Set keys = this.selector.selectedKeys();
            
            for (Iterator i = keys.iterator(); i.hasNext();) {
              SelectionKey key = (SelectionKey)i.next();
              if (key.isReadable()) {
                key.interestOps(key.interestOps() & 0xFFFFFFFE);
                
                this.readthreadPool.execute(new Message(key, 1));
              }
              
              if (key.isAcceptable()) {
                logger.debug("accept event occur \n");
                logger.info(new StringBuilder().append("selector manage ").append(this.selector.keys().size()).append(" keys").toString());
                
                key.interestOps(key.interestOps() & 0xFFFFFFEF);
                
                this.readthreadPool.execute(new Message(key, 16));
              }
              
              i.remove();
            }
            if (this.sendthreadPool.getActiveCount() > this.corePoolSize) {
              logger.debug(new StringBuilder().append("total ").append(this.sendthreadPool.getActiveCount()).append(" send threads").toString());
            }
            
            if (this.readthreadPool.getActiveCount() > this.corePoolSize) {
              logger.debug(new StringBuilder().append("total ").append(this.readthreadPool.getActiveCount()).append(" read threads").toString());
            }
          }
        } catch (CancelledKeyException e) {
          logger.error(new StringBuilder().append("Key has Been Cancelled: ").append(e.getMessage()).toString(), e);
        }
        catch (ClosedSelectorException e)
        {
          logger.error(new StringBuilder().append("Key has Been closed:").append(e.getMessage()).toString(), e);
        } catch (RuntimeException e) {
          logger.error("exception:", e);
        }
      }
      this.sendthreadPool.shutdown();
      this.readthreadPool.shutdown();
      if (this.servicesFactory != null) {
        this.servicesFactory.shutdown();
      }
    } catch (Throwable e) {
      logger.error(new StringBuilder().append("Exception occured ").append(e.getMessage()).toString(), e);
    } finally {
      logger.info("Selector Work thread has stopped");
    }
    this.isExit = true;
  }
  
  public ServiceFactory getServiceFactory() {
    return this.servicesFactory;
  }
  
  public void startAgent(String[] fileNames, String[] reqTypes, String ip, short port) throws Exception
  {
    if ((fileNames == null) || (fileNames.length == 0)) {
      throw new IllegalArgumentException("no service can be provided");
    }
    if (reqTypes.length != fileNames.length) {
      throw new IllegalArgumentException("size of array named types must be equal with of fileNames");
    }
    
    localIp = ip;
    this.servicesFactory = new ServiceFactory(this, fileNames, reqTypes);
    this.servicesFactory.init();
    this.servicesFactory.startAgent();
    new ListenConnection(ip, port, this).init();
  }
  
  static String getLocalIp() {
    return localIp;
  }
}
