package com.kac.client;

import com.kac.common.DispatchException;
import com.kac.common.MultiQuery;
import com.kac.common.MultiResult;
import com.kac.common.ReloadQuery;
import com.kac.common.ReloadResult;
import com.kac.common.ReloadResultInfo;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class ClientManager
{
  private static final Logger m_logger = Logger.getLogger(ClientManager.class);
  
  private List<Client> m_clientList = new ArrayList();
  
  private IDFactory m_requestIDFactory = null;
  
  private int m_clientReqQueueLen = new Global().MAX_REQQUEUE_LEN;
  
  public void init(String[] ipArr, short[] portArr)
    throws DispatchException
  {
    if ((ipArr == null) || (portArr == null)) {
      throw new DispatchException("ipArr or portArr can't be null");
    }
    
    if ((ipArr.length == 0) || (portArr.length == 0)) {
      throw new DispatchException("ipArr's length or portArr's length can't be zero");
    }
    
    if (ipArr.length != portArr.length) {
      throw new DispatchException("the length of ipArr is not equal to the length of portArr");
    }
    
    for (int i = 0; i < ipArr.length; i++) {
      Client c = new Client();
      try {
        c.setMaxQueueLen(this.m_clientReqQueueLen);
        m_logger.info("clientManager start to init client, ip is : " + ipArr[i] + " port is : " + portArr[i]);
        c.init(ipArr[i], portArr[i]);
      }
      catch (DispatchException e)
      {
        m_logger.error("initialize client failed, ip is : " + ipArr[i] + " port is : " + portArr[i]);
      }
      
      this.m_clientList.add(c);
    }
    
    this.m_requestIDFactory = new IDFactory();
    m_logger.info("client manager has been initialized, total good client num is : " + this.m_clientList.size());
  }
  
  public void setMaxQueueLen(int len)
  {
    this.m_clientReqQueueLen = len;
  }
  
  public int getMaxQueueLen()
  {
    return this.m_clientReqQueueLen;
  }
  
  private Client getOneGoodClent()
  {
    int listSize = this.m_clientList.size();
    
    List<Client> goodClientList = new ArrayList();
    for (int i = 0; i < listSize; i++) {
      Client client = (Client)this.m_clientList.get(i);
      if ((client != null) && (client.getSocketState() == 0)) {
        goodClientList.add(client);
      }
    }
    
    int goodListSize = goodClientList.size();
    if (goodListSize == 0) {
      return null;
    }
    int index = (int)(this.m_requestIDFactory.next() % goodListSize);
    return (Client)goodClientList.get(index);
  }
  
  public List<Client> getGoodClientList()
  {
    int listSize = this.m_clientList.size();
    List<Client> goodClientList = new ArrayList();
    for (int i = 0; i < listSize; i++) {
      Client client = (Client)this.m_clientList.get(i);
      if ((client != null) && (client.getSocketState() == 0)) {
        goodClientList.add(client);
      }
    }
    
    return goodClientList;
  }
  
  public MultiResult getResults(MultiQuery q, List<Client> clientList, int clientIndex)
    throws DispatchException
  {
    if ((clientList == null) || (clientList.size() == 0) || (clientIndex >= clientList.size()))
    {
      throw new DispatchException("now, clientList is null or the list is empty or the clientIndex is invalid, please check params. ");
    }
    
    Client client = (Client)clientList.get(clientIndex);
    m_logger.debug("agent ip : " + client.getIP());
    return client.getResults(q);
  }
  
  public MultiResult getResults(MultiQuery q)
    throws DispatchException
  {
    Client client = getOneGoodClent();
    if (client == null) {
      throw new DispatchException("now, no good client is available, please check socket. ");
    }
    
    m_logger.debug("agent ip : " + client.getIP());
    return client.getResults(q);
  }
  
  public List<ReloadResult> sendReloadQuery(ReloadQuery q)
    throws DispatchException
  {
    MultiQuery mq = new MultiQuery();
    mq.addQuery(q);
    mq.setTimeOut(600000);
    
    List<ReloadResult> reloadResultList = new ArrayList();
    
    int listSize = this.m_clientList.size();
    for (int i = 0; i < listSize; i++)
    {
      Client client = (Client)this.m_clientList.get(i);
      if (client != null)
      {
        m_logger.info("send reload query to agent { " + client.getIP() + " : " + client.getPort() + " } ");
        
        ReloadResult result = new ReloadResult();
        result.setIp(client.getIP());
        result.setPort(client.getPort());
        
        MultiResult res = new MultiResult();
        try {
          res = client.getResults(mq);
        }
        catch (DispatchException e) {
          ReloadResultInfo resultInfo = new ReloadResultInfo();
          ArrayList<String> typeList = q.getTypeList();
          int typeListSize = typeList.size();
          for (int j = 0; j < typeListSize; j++) {
            resultInfo.setStatus(2);
            
            resultInfo.setMessage(e.getMessage());
            result.addTypeResult((String)typeList.get(j), resultInfo);
          }
          reloadResultList.add(result);
          
          m_logger.error("get results from agent { " + client.getIP() + " : " + client.getPort() + " } " + "error", e);
          
          continue;
        }
        
        int resSize = res.size();
        if (resSize == 0) {
          reloadResultList.add(result);
        } else {
          for (int j = 0; j < resSize; j++) {
            if ((res.getResult(j) instanceof ReloadResult)) {
              ReloadResult reloadResult = (ReloadResult)res.getResult(j);
              
              reloadResult.setIp(client.getIP());
              reloadResult.setPort(client.getPort());
              reloadResultList.add(reloadResult);
            }
          }
        }
      }
    }
    
    return reloadResultList;
  }
  
  public void close()
  {
    int listSize = this.m_clientList.size();
    for (int i = 0; i < listSize; i++) {
      Client client = (Client)this.m_clientList.get(i);
      if (client != null) {
        client.close();
      }
    }
  }
}
