package com.mastertechsoftware.tasker;

import java.util.List;

/**
 * Implement this interface when all tasks have finished
 */
public interface TaskFinisher {
	void finished(List<Exception> errors);
}
