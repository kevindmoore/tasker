package com.mastertechsoftware.tasker;

/**
 * Interface for classes that set/get pause flag
 */
public interface Pausable {
	boolean isPaused();
	void setPaused(boolean paused);
}
