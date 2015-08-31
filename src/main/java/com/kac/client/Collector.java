package com.kac.client;

import com.kac.common.MultiResponsePack;
import com.kac.common.MultiResult;
import com.kac.common.Package;

import org.apache.log4j.Logger;

public class Collector
  extends Thread
{
  private static final Logger m_logger = Logger.getLogger(Collector.class);
  
  private Client client = null;
  
  public Collector(Client client) {
    this.client = client;
  }
  
  public void run() {
    while (!this.client.isClientClose()) {
      try {
        Package response = this.client.recvPack();
        if (response.getControl() == 505) {
          HandleSucc(response);
        } else {
          HandleFail(response);
        }
      }
      catch (Exception e) {
        if (!this.client.isClientClose()) {
          m_logger.error("receive packet error.", e);
          this.client.reInit();
        }
      }
    }
  }
  
  private void HandleFail(Package response)
  {
    m_logger.error("getsub fail, check servers");
    
    long reqID = response.getID();
    m_logger.error(getName() + " HandleFail for " + reqID + ", control is " + response.getControl());
    Request req = this.client.fetchMiddle(reqID);
    if (req == null) {
      m_logger.error("req is null");
      return;
    }
    req.setMultiResult(new MultiResult());
    
    this.client.addResult(req);
    
    synchronized (req) {
      req.notify();
    }
  }
  
  private void HandleSucc(Package response)
  {
    MultiResponsePack hit = (MultiResponsePack)response;
    if (hit == null)
      return;
    if (hit.getControl() != 505) {
      m_logger.error("cmd not match");
      return;
    }
    long reqID = hit.getID();
    Request req = this.client.fetchMiddle(reqID);
    if (req == null) {
      m_logger.error("req is null");
      return;
    }
    
    MultiResult multires = hit.getMultiResult();
    
    if (multires != null) {
      req.setMultiResult(multires);
    } else {
      m_logger.error("result is null");
      req.setMultiResult(new MultiResult());
    }
    
    this.client.addResult(req);
    
    synchronized (req) {
      req.notify();
    }
  }
}
