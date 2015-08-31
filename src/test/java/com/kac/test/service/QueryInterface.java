package com.kac.test.service;

public class QueryInterface {


    private static class SingletonHolder {
        private static final QueryInterface INSTANCE = new QueryInterface();
    }
    
    private QueryInterface() {}
    
    public static QueryInterface getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public String queryMethod(String conditionA,String conditionB, int conditionC) {
        System.out.println("condition:"+conditionA+" "+conditionB+" "+conditionC);
        return "Success";
    }
}
