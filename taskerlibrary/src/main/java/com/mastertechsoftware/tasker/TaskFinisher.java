package com.mastertechsoftware.tasker;

/**
 * Implement this interface when all tasks have finished
 */
public interface TaskFinisher {
	void onSuccess();
	void onError();
}
