package com.kac.test.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.kac.server.EventCenter;
import com.kac.server.ServiceFactory;

public class Server1
{
  private static final Logger logger = Logger.getLogger(Server1.class);
  
  private static String servicesXmlFile = "conf1" + File.separator + "services.xml";
  
  private static String agentPropertyFile = "conf1" + File.separator + "agent.properties";
  
  static void getValidServices(List<String> servicesList, List<String> reqTypesList)
    throws ParserConfigurationException, SAXException, IOException
  {
    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    
    DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
    Document doc = docBuilder.parse( Server1.class.getClassLoader().getResourceAsStream(servicesXmlFile));
    doc.getDocumentElement().normalize();
    NodeList listOfServices = doc.getElementsByTagName("service");
    int totalServices = listOfServices.getLength();
    for (int s = 0; s < totalServices; s++) {
      Node serviceNode = listOfServices.item(s);
      if (serviceNode.getNodeType() == Node.ELEMENT_NODE) {
        Element serviceElement = (Element)serviceNode;
        NodeList entryList = serviceElement.getElementsByTagName("entry");
        for (int e = 0; e < entryList.getLength(); e++) {
        Element entryEle = (Element)entryList.item(e);
        reqTypesList.add(entryEle.getAttribute("key").trim());
        servicesList.add(entryEle.getAttribute("value").trim());
        }
      }
    }
  }
  
  public static void main(String[] args)
  {
    try
    {
    	InputStream fis = Server1.class.getClassLoader().getResourceAsStream(agentPropertyFile);
      Properties p = new Properties();
      p.load(fis);
      
      int netCoreThreadNum = Integer.parseInt(p.getProperty("netCoreThreadNum"));
      
      int processCoreThreadNum = Integer.parseInt(p.getProperty("processCoreThreadNum"));
      
      int netPoolThreadMaxNum = Integer.parseInt(p.getProperty("netPoolThreadMaxNum"));
      
      int processPoolThreadMaxNum = Integer.parseInt(p.getProperty("processPoolThreadMaxNum"));
      
      int netPoolQueueLen = Integer.parseInt(p.getProperty("netPoolQueueLen"));
      
      int processPoolQueueLen = Integer.parseInt(p.getProperty("processPoolQueueLen"));
      
      short listenPort = Short.parseShort(p.getProperty("listenPort"));
      
      int listenTimeout = Integer.parseInt(p.getProperty("listenTimeout"));
      
      String listenIP = p.getProperty("localIP");
      fis.close();
      List<String> servicesList = new LinkedList();
      List<String> reqTypesList = new LinkedList();
      getValidServices(servicesList, reqTypesList);
      
      logger.info("本机地址:" + listenIP);
      logger.info("服务器代理监听端口:" + listenPort);
      logger.info("服务器监听超时时间:" + listenTimeout + "毫秒");
      
      logger.info("服务器处理核心线程数:" + processCoreThreadNum);
      logger.info("服务器处理最大核心线程数:" + processPoolThreadMaxNum);
      logger.info("服务器处理线程池队列大小:" + processPoolQueueLen);
      
      logger.info("服务器网络处理线程池核心线程数:" + netCoreThreadNum);
      logger.info("服务器网络处理线程池最大核心线程数:" + netPoolThreadMaxNum);
      logger.info("服务器网络处理线程池队列大小:" + netPoolQueueLen);
      
      logger.info("共有" + servicesList.size() + "类服务");
      
      EventCenter eventMonitor = new EventCenter();
      ServiceFactory.setPoolQueueLen(processPoolQueueLen);
      ServiceFactory.setCoreThreadNum(processCoreThreadNum);
      ServiceFactory.setMaxCoreThreadNum(processPoolThreadMaxNum);
      eventMonitor.setThreadName("Music suggestion group");
      eventMonitor.setTimeout(listenTimeout);
      eventMonitor.setThreadpoolPara(netCoreThreadNum, netPoolThreadMaxNum, 2 * (netPoolThreadMaxNum < 5 ? 1 : 2), netPoolQueueLen);
      
      eventMonitor.startAgent((String[])servicesList.toArray(new String[0]), (String[])reqTypesList.toArray(new String[0]), listenIP, listenPort);
      
      for (;;)
      {
        Thread.sleep(Long.MAX_VALUE);
      }
    } catch (Exception e) {
    	
      logger.error("start agent failed", e);
      System.exit(-1);
    }
  }
}
