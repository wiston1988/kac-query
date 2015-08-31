package com.kac.test.service;

import com.kac.common.Query;
import com.kac.common.Result;
import com.kac.test.query.TestQuery;
import com.kac.test.query.TestQueryResult;


public class TestQueryService extends ServiceBase {

    
    private QueryInterface queryInterface = QueryInterface.getInstance();

    public Result GetResult(Query query) {
        TestQuery testQuery = (TestQuery)query;
        String result = null;
        try{
        	result =queryInterface.queryMethod(testQuery.getConditionA(), testQuery.getConditionB(), testQuery.getConditionC());
        } catch (Exception e){
            result = "Error";
        }
        if(result == null){
            result = "Error";
        }
        return new TestQueryResult(result);
    }

    public String getServiceName() {
        return "testQueryService";
    }
}
