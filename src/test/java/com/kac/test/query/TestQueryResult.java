package com.kac.test.query;

import com.kac.common.Result;


public class TestQueryResult extends Result {
    private static final long serialVersionUID = 2250643523646299188L;
    private String result;

    public TestQueryResult(String result) {
        this.result = result;
    }

    public TestQueryResult() {
    }


    public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

    @Override
    public String toString() {
        return "AppAdGetResult [result=" + result + "]";
    }
}
