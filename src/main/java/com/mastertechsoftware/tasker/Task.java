package com.mastertechsoftware.tasker;

/**
 * All tasks need to implement this interface
 */
public interface Task {
	Object run();
	boolean shouldContinue();
	boolean hasError();
	Exception getError();
	void setError(Exception error);
	void setResult(Object result);
	Object getResult();
	void setPauseable(Pausable pauseable);
	void setCondition(Condition condition);
	Condition getCondition();
	boolean hasCondition();
	Tasker.THREAD_TYPE runType();
	void setRunType(Tasker.THREAD_TYPE type);
}
