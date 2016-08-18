package com.mastertechsoftware.tasker;

/**
 * All tasks need to implement this interface
 */
public interface Task<T> {
	Object run();
	boolean shouldContinue();
	boolean hasError();
	Exception getError();
	void setError(Exception error);
	void setResult(T result);
	T getResult();
	void setPreviousResult(T result);
	T getPreviousResult();
	void setPauseable(Pausable pauseable);
	void setCondition(Condition condition);
	Condition getCondition();
	boolean hasCondition();
	Tasker.THREAD_TYPE runType();
	void setRunType(Tasker.THREAD_TYPE type);
}
