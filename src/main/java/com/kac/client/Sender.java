package com.kac.client;

import com.kac.common.DispatchException;
import com.kac.common.MultiRequestPack;

import org.apache.log4j.Logger;

public class Sender
  extends Thread
{
  private static final Logger logger = Logger.getLogger(Sender.class);
  
  private Client client = null;
  
  public Sender(Client client) {
    this.client = client;
  }
  
  public void run() {
    while (this.client.isClientClose() != true) {
      try {
        Request req = this.client.fetchRequest();
        if (null != req) {
          if (null != req.getMultiQuery()) {
            MultiRequestPack multiReqPack = new MultiRequestPack(req.getMultiQuery());
            
            multiReqPack.setID(req.getRequestID());
            
            multiReqPack.encode();
            
            try
            {
              this.client.sendPack(multiReqPack);
            }
            catch (DispatchException e1) {
              if (this.client.isClientClose() != true) {
                logger.error("send packet fail");
              }
              this.client.reInit();
            }
          } else {
            logger.error("query is null.");
          }
        } else {
          try {
            Thread.currentThread();Thread.sleep(1L);
          } catch (InterruptedException e) {
            logger.error("", e);
          }
        }
      }
      catch (Exception e) {
        logger.error("send error.", e);
      }
    }
  }
}
