package com.kac.server;

public class FTRelateSearchParam
{
  static final String VERSION = "1.0.0.0";
  
  static final long MAXTIMEOUT = 60000L;
  
  static final int MAXCAPACITY = 1000;
  
  static final long MINSLEEPINTERNAL = 1L;
  
  private String listenIP;
  
  private short listenPort = 7774;
  
  private String ccAddr = null;
  
  private short ccport = 7777;
  
  private int corePoolSize = 20;
  
  private int maxRange = 1000;
  
  private int maximumPoolSize = 20;
  
  private int keepAliveTime = 30;
  
  private static int queueLen = 2000;
  
  private int reqQueueSize = 1000;
  
  private long realSentTimout = 5000L;
  
  private long searchTimeOut = this.realSentTimout + 10000L;
  
  private long insertTimeOut = this.realSentTimout + 5000L;
  
  private long deleteTimeOut = this.realSentTimout + 5000L;
  
  private int acceptTimeOut = new Long(this.realSentTimout + 2000L).intValue();
  
  private long listenTimeout = this.realSentTimout + 8000L;
  
  private String appName = "FTIRS";
  
  private int MaxServerConnect = 10000;
  
  private long connCCSleepInterval = 5000L;
  
  private long sendInterval = 5L;
  
  private long retryInterval = 1000L;
  
  private long checkConditionInterval = 10000L;
  
  private int maxRetryNum = 100000;
  
  private long initSleep = 1000L;
  
  private long MAXPOPVALUE = 1000000L;
  
  private int maxQueryLen = 32;
  
  private static boolean needStopWord = true;
  
  private static long gabageSocketTime = 1800L;
  
  static long getGSTime() {
    return gabageSocketTime;
  }
  
  static void setGSTime(long gstime) {
    if (gabageSocketTime > 0L)
      gabageSocketTime = gstime;
  }
  
  public static boolean needStopWord() {
    return needStopWord;
  }
  
  public int getQueryLen() {
    return this.maxQueryLen;
  }
  
  public void setQueryLen(int len) {
    this.maxQueryLen = len;
  }
  
  public long getInitSleep() {
    return this.initSleep;
  }
  
  public void setInitTime(int time) {
    if (time > 0) {
      this.initSleep = (time * 1000);
    }
  }
  
  public void setMaxPopValue(long value) {
    this.MAXPOPVALUE = value;
  }
  
  public long getMaxPopValue() {
    return this.MAXPOPVALUE;
  }
  
  public void setRetryNum(int maxRetryNum) {
    this.maxRetryNum = maxRetryNum;
  }
  
  public int getRetryNum() {
    return this.maxRetryNum;
  }
  
  public String getAppName() {
    return this.appName;
  }
  
  public String getCCAddr() {
    return this.ccAddr;
  }
  
  public long getCheckInterval() {
    return this.checkConditionInterval;
  }
  
  public long getConnCCInterval() {
    return this.connCCSleepInterval;
  }
  
  public short getListenPort() {
    return this.listenPort;
  }
  
  public String getListenIp() {
    return this.listenIP;
  }
  
  public long getSendInterval() {
    return this.sendInterval;
  }
  
  public long getRetryInterval() {
    return this.retryInterval;
  }
  
  public boolean setConnCCInterval(long interval) {
    if (interval < 1L)
      return false;
    this.connCCSleepInterval = (interval * 1000L);
    return true;
  }
  
  public boolean setSendInterval(long interval) {
    if (interval < 1L)
      return false;
    this.sendInterval = interval;
    return true;
  }
  
  public boolean setRetryInterval(int interval) {
    if (interval < 1L)
      return false;
    this.retryInterval = (interval * 1000);
    return true;
  }
  
  public int getMaxServerConnect() {
    return this.MaxServerConnect;
  }
  
  public boolean setMaxServerConnect(int conSize) {
    if (conSize <= 0)
      return false;
    this.MaxServerConnect = conSize;
    return true;
  }
  
  public long getSentTimout() {
    return this.realSentTimout;
  }
  
  public long getSearchTimout() {
    return this.searchTimeOut;
  }
  
  public boolean setMaxScaleValue(int value) {
    if (value < 1) {
      return false;
    }
    this.maxRange = value;
    return true;
  }
  
  public short getCCPort() {
    return this.ccport;
  }
  
  public int getMaxScaleValue() {
    return this.maxRange;
  }
  
  public long getInsertTimout() {
    return this.insertTimeOut;
  }
  
  public long getDeleteTimout() {
    return this.deleteTimeOut;
  }
  
  public int getAcceptTimout() {
    return this.acceptTimeOut;
  }
  
  public long getListenTimout() {
    return this.listenTimeout;
  }
  
  public int getcorePoolSize() {
    return this.corePoolSize;
  }
  
  public int getmaxPoolSize() {
    return this.maximumPoolSize;
  }
  
  public int getkeepTime() {
    return this.keepAliveTime;
  }
  
  public static int getqueueLen() {
    return queueLen;
  }
  
  public int getreqQueueSize() {
    return this.reqQueueSize;
  }
  
  private boolean setCoreCapacity(int len) {
    if ((len < 1) || (len > 1000)) {
      throw new IllegalArgumentException("capacity set error, it must be positive and less than 1000");
    }
    
    this.maximumPoolSize = len;
    
    this.corePoolSize = (len < 5 ? 1 : 2);
    
    this.keepAliveTime = (2 * len);
    
    queueLen = len + 1000;
    
    return true;
  }
  
  public void setReqQueueLen(int reqQueueLen)
  {
    if (reqQueueLen < 1) {
      throw new IllegalArgumentException("queue length error, it must be larger than 0");
    }
    
    this.reqQueueSize = reqQueueLen;
  }
  
  private void setTimeout(int time)
  {
    if ((time <= 0) || (time > 60000L)) {
      throw new IllegalArgumentException("time set error, it must be positive and less than 60000");
    }
    
    this.realSentTimout = (time * 1000);
    this.searchTimeOut = (this.realSentTimout + 1000L);
    this.insertTimeOut = (this.realSentTimout + 5000L);
    this.deleteTimeOut = (this.realSentTimout + 5000L);
    this.acceptTimeOut = new Long(this.realSentTimout + 2000L).intValue();
    this.listenTimeout = (this.realSentTimout + 8000L);
    this.realSentTimout = 1000L;
  }
  
  public FTRelateSearchParam(String listenIP, short listenPort, String ccAddress, short ccPort)
  {
    if (listenPort == ccPort) {
      throw new IllegalArgumentException("listen port must be different from cc port");
    }
    if ((listenPort <= 0) || (listenPort > 65535)) {
      throw new IllegalArgumentException("listen port has been out of range");
    }
    this.listenPort = listenPort;
    this.listenIP = listenIP;
    this.ccAddr = ccAddress;
    this.ccport = ccPort;
  }
  
  public FTRelateSearchParam(String listenIP, short listenPort, String ccAddress, short ccPort, int realSentTimout)
  {
    this(listenIP, listenPort, ccAddress, ccPort);
    setTimeout(realSentTimout);
  }
  
  public FTRelateSearchParam(String listenIP, short listenPort, String ccAddress, short ccPort, int realSentTimout, int maximumPoolSize)
  {
    this(listenIP, listenPort, ccAddress, ccPort);
    setTimeout(realSentTimout);
    setCoreCapacity(maximumPoolSize);
  }
}
