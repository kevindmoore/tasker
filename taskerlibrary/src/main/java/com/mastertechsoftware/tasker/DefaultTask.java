package com.mastertechsoftware.tasker;

/**
 * Abstract class if you want to ignore onSuccess or onError
 */
public abstract class DefaultTask<T> implements Task<T> {
	protected Tasker.THREAD_TYPE threadType = Tasker.THREAD_TYPE.BACKGROUND;

	protected boolean shouldContinue = true;
	protected Pausable pauseable;
	protected Exception error;
	protected Condition condition;
	protected T result;

	/**
	 * Constructors
	 */
	public DefaultTask() {
	}

	public DefaultTask(Tasker.THREAD_TYPE threadType) {
		this.threadType = threadType;
	}

	/**
	 * Set the class that implements the Pausable interface - Want to be able
	 * to pause the class that is running this task
	 * @param pauseable
	 */
	@Override
	public void setPauseable(Pausable pauseable) {
		this.pauseable = pauseable;
	}

	/**
	 * Pause this task. Once other conditions are finished, unpause
	 * @param paused
	 */
	public void setPaused(boolean paused) {
		if (pauseable != null) {
			pauseable.setPaused(paused);
		}
	}

	/**
	 * Get/Set Error
	 */
	@Override
	public Exception getError() {
		return error;
	}

	@Override
	public boolean hasError() {
		return error != null;
	}

	@Override
	public void setError(Exception error) {
		this.error = error;
	}

	/**
	 * Set a condition that will be executed before running the task
	 * If the condition returns false, the task won't be run
	 * @param condition
	 */
	@Override
	public void setCondition(Condition condition) {
		this.condition = condition;
	}

	@Override
	public Condition getCondition() {
		return condition;
	}

	@Override
	public boolean hasCondition() {
		return condition != null;
	}

	/**
	 * Get/Set Results
	 * @param result
	 */
	@Override
	public void setResult(T result) {
		this.result = result;
	}

	@Override
	public T getResult() {
		return result;
	}

	/**
	 * If this flag it set to false, the following tasks will not be run
	 * @return true runs the rest of the tasks
	 */
	@Override
	public boolean shouldContinue() {
		return shouldContinue;
	}

	public void setShouldContinue(boolean shouldContinue) {
		this.shouldContinue = shouldContinue;
	}

	/**
	 * Run type: UI or background
	 */
	@Override
	public Tasker.THREAD_TYPE runType() {
		return threadType;
	}

	@Override
	public void setRunType(Tasker.THREAD_TYPE threadType) {
		this.threadType = threadType;
	}

	/**
	 * All subclasses must implement this method.
	 * Run any task code here.
	 * @return result
	 */
	@Override
	public abstract Object run();
}
