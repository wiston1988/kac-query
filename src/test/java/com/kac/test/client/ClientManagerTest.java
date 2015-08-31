package com.kac.test.client;

import com.kac.client.ClientManager;
import com.kac.common.DispatchException;
import com.kac.common.MultiQuery;
import com.kac.common.MultiResult;
import com.kac.test.query.TestQuery;
import com.kac.test.query.TestQueryResult;

public class ClientManagerTest {

	public static void main(String[] args) throws DispatchException {
		ClientManager clientManager = new ClientManager();
		String[] ipArr = new String[2];
		ipArr[0] = "127.0.0.1";
		ipArr[1] = "127.0.0.1";
		short[] portArr = new short[2];
		portArr[0]=8886;
		portArr[1]=8887;
		clientManager.init(ipArr, portArr);
		
		TestQuery query1 = new TestQuery();
		query1.setConditionA("conditionA");
		query1.setConditionB("conditionB");
		query1.setConditionC(1);
		
        MultiQuery multiQuery1 = new MultiQuery();
        multiQuery1.setTimeOut(5 * 1000);
        multiQuery1.addQuery(query1);
        
        MultiResult multiResult1 = clientManager.getResults(multiQuery1);
        TestQueryResult result1 = (TestQueryResult)multiResult1.getResult(0);
        System.out.println(result1.toString());
        
		TestQuery query2 = new TestQuery();
		query2.setConditionA("conditionA");
		query2.setConditionB("conditionB");
		query2.setConditionC(2);
		
        MultiQuery multiQuery2 = new MultiQuery();
        multiQuery2.setTimeOut(5 * 1000);
        multiQuery2.addQuery(query2);
        
        MultiResult multiResult2 = clientManager.getResults(multiQuery2);
        TestQueryResult result2 = (TestQueryResult)multiResult2.getResult(0);
        System.out.println(result2.toString());
	}

}
