package com.mastertechsoftware.tasker;


import com.mastertechsoftware.logging.Logger;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Class for Sequentially running tasks in the background
 */
public class Tasker {
	public enum THREAD_TYPE {
		UI,
		BACKGROUND
	}

	private static final String TAG = Tasker.class.getSimpleName();
	protected Handler handler;
	protected ExecutorService mExecutor = Executors.newSingleThreadExecutor();
	protected LinkedBlockingDeque<Task> tasks = new LinkedBlockingDeque<>();
	protected TaskFinisher finisher;
	protected boolean noErrors = true;
	protected boolean debugging = true;
	protected Task lastAddedTask;
	protected Map<ThreadRunnable, Future> runableMap = new ConcurrentHashMap<>();
	protected List<Exception> errors = new ArrayList<>();
	protected Object previousResult;

	/**
	 * Simple create task to get us started
	 * @return Tasker
	 */
	static public Tasker create() {
		return new Tasker();
	}


	/**
	 * Add a new task
	 * @param task
	 * @return Tasker
	 */
	public Tasker addTask(Task task) {
		tasks.add(task);
		lastAddedTask = task;
		return this;
	}

	/**
	 * Create the handler as late as possible to make sure the main thread is ready
	 */
	protected void createHandler() {
		if (handler != null) return;
		try {
			handler = new Handler(Looper.getMainLooper());
		} catch (RuntimeException e) {

		}
	}

	/**
	 * Add a new UI task
	 * @param task
	 * @return Tasker
	 */
	public Tasker addUITask(Task task) {
		tasks.add(task);
		task.setRunType(THREAD_TYPE.UI);
		lastAddedTask = task;
		return this;
	}

	/**
	 * Add a class that will handle the final ending moment
	 * @param finisher
	 * @return Tasker
	 */
	public Tasker addFinisher(TaskFinisher finisher) {
		this.finisher = finisher;
		return this;
	}

	/**
	 * Add a condition to the last added task.
	 * Task will only be run if this task is true
	 * @param condition
	 * @return Tasker
	 */
	public Tasker withCondition(Condition condition) {
		lastAddedTask.setCondition(condition); // Note: This will crash if you haven't added at least 1 task
		return this;
	}

	/**
	 * Cancel all running tasks
	 */
	public void cancelAll() {
		for (Future future : runableMap.values()) {
			future.cancel(true);
		}
		runableMap.clear();
		shutdown();
	}

	/**
	 * Shutdown the executor
	 */
	private void shutdown() {
		if (!mExecutor.isShutdown() && !mExecutor.isTerminated()) {
			mExecutor.shutdown();
			try {
				mExecutor.awaitTermination(2, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				Logger.error("Problems awaiting Executor shutdown");
			}
		}
	}

	/**
	 * Cancel a single task
	 * @param task
	 * @return true if we found and cancelled the task
	 */
	public boolean cancelTask(Task task) {
		for (ThreadRunnable threadRunnable : runableMap.keySet()) {
			if (threadRunnable.task == task) {
				Future future = runableMap.remove(threadRunnable);
				if (future != null) {
					future.cancel(true);
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * Start running all tasks
	 * @return true if everything started ok
	 */
	public boolean run() {
		// Wrap the whole thing so we can make sure to unlock in
		// case something throws.
		try {

			for (Task task : tasks) {
				final ThreadRunnable threadRunnable = new ThreadRunnable(task);
				recreateExecutorIfNecessary();
				final Future future = mExecutor.submit(threadRunnable);
				runableMap.put(threadRunnable, future);
			}

			tasks.clear();

		} catch (Exception RejectedExecutionException) {
			Logger.error("Tasker:run - RejectedExecutionException", RejectedExecutionException);
			return false;
		}
		return true;
	}

	private void recreateExecutorIfNecessary() {
		// If we're shutdown or terminated we can't accept any new requests.
		if (mExecutor == null || mExecutor.isShutdown() || mExecutor.isTerminated()) {
			if (mExecutor != null) {
				Logger.error("Tasker:run - Executor is shutdown. Recreating");
			}
			mExecutor = Executors.newSingleThreadExecutor();
		}
	}

	/**
	 * Remove the runnable from our map and call the finisher if it's the last one
	 * @param threadRunnable
	 */
	protected void runnableFinished(ThreadRunnable threadRunnable) {
		if (debugging) {
			Log.d(TAG, "runnableFinished");
		}
		runableMap.remove(threadRunnable);
		final Task task = threadRunnable.getTask();
		previousResult = task.getResult();
		if (runableMap.isEmpty() && finisher != null) {
			createHandler();
			if (debugging) {
				Log.d(TAG, "Calling finisher");
			}
			handler.post(new Runnable() {
				@Override
				public void run() {
					finisher.finished(errors);
				}
			});
		}

		// If we have no more tasks, then shutdown the executor
		if (runableMap.isEmpty()) {
			shutdown();
		}
	}


	/**
	 * Runnable that uses our callback and then runs the result on UI thread
	 * or background thread (which is what it is on)
	 */
	class ThreadRunnable implements Callable, Pausable {
		protected Task task;
		protected Object result;
		protected CountDownLatch uiWait = new CountDownLatch(1);
		protected CountDownLatch pauseWait = new CountDownLatch(1);
		protected boolean paused = false;

		ThreadRunnable(Task task) {
			this.task = task;
			task.setPauseable(this);
		}

		public Task getTask() {
			return task;
		}

		@Override
		public Object call() throws Exception {
			try {
				if (task.hasCondition() && !task.getCondition().shouldExecute()) {
					if (debugging) {
						Log.d(TAG, "Tasks condition failed. Not running task");
					}
					runnableFinished(this);
					return null;
				}

				createHandler();

				if (previousResult != null) {
					task.setPreviousResult(previousResult);
					previousResult = null;
				}
				if (task.runType() == THREAD_TYPE.BACKGROUND) {
					if (debugging) {
						Log.d(TAG, "Running task in background");
					}
					result = task.run();
					task.setResult(result);
				} else {
					// Make UI Thread calls
					handler.post(new Runnable() {
						@Override
						public void run() {
							try {
								if (debugging) {
									Log.d(TAG, "Running task on UI Thread");
								}
								result = task.run();
								task.setResult(result);
								uiWait.countDown();
							} catch (Exception e) {
								noErrors = false;
								task.setError(e);
								if (task.runType() == THREAD_TYPE.UI) {
									uiWait.countDown();
								}
							}
						}
					});
					// Wait until UI is finished
					uiWait.await();
				}
				if (paused) {
					pauseWait.await();
				}
				runnableFinished(this);
				if (!task.shouldContinue()) {
					cancelAll();
				}
				return result;
			} catch (final Exception e) {
				Logger.error("run caught exception", e);
				noErrors = false;
				handler.post(new Runnable() {
					@Override
					public void run() {
						try {
							errors.add(e);
							task.setError(e);
						} catch (Exception e2) {
							Logger.error("run caught exception in setError", e2);
						}
					}
				});
			}
			runnableFinished(this);
			return null;
		}

		@Override
		public boolean isPaused() {
			return paused;
		}

		@Override
		public void setPaused(boolean paused) {
			this.paused = paused;
			if (!paused) {
				pauseWait.countDown();
			}
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			ThreadRunnable that = (ThreadRunnable) o;

			return task != null ? task.equals(that.task) : that.task == null;

		}

		@Override
		public int hashCode() {
			return task != null ? task.hashCode() : 0;
		}

	}
}
