package com.kac.test.query;

import com.kac.common.Query;

public class TestQuery extends Query{
    private static final long serialVersionUID = 1001L;

    private String conditionA;
    private String conditionB;
    private int conditionC;

    public TestQuery() {
    }

	public String getConditionA() {
		return conditionA;
	}

	public void setConditionA(String conditionA) {
		this.conditionA = conditionA;
	}

	public String getConditionB() {
		return conditionB;
	}

	public void setConditionB(String conditionB) {
		this.conditionB = conditionB;
	}

	public int getConditionC() {
		return conditionC;
	}

	public void setConditionC(int conditionC) {
		this.conditionC = conditionC;
	}

}
