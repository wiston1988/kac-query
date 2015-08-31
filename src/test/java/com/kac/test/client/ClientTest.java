package com.kac.test.client;

import com.kac.client.Client;
import com.kac.common.DispatchException;
import com.kac.common.MultiQuery;
import com.kac.common.MultiResult;
import com.kac.test.query.TestQuery;
import com.kac.test.query.TestQueryResult;

public class ClientTest {

	public static void main(String[] args) throws DispatchException {
		Client client = new Client();
		String ip = "127.0.0.1";
		short port = 8886;
		client.init(ip, port);
		
		TestQuery query = new TestQuery();
		query.setConditionA("conditionA");
		query.setConditionB("conditionB");
		query.setConditionC(1);
		
        MultiQuery multiQuery = new MultiQuery();
        multiQuery.setTimeOut(5 * 1000);
        multiQuery.addQuery(query);
        
        MultiResult multiResult = client.getResults(multiQuery);
        TestQueryResult result = (TestQueryResult)multiResult.getResult(0);
        System.out.println(result.toString());
	}

}
