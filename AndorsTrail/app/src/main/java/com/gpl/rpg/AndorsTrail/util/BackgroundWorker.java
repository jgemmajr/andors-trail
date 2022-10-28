package com.gpl.rpg.AndorsTrail.util;

import java.util.concurrent.Executors;

public final class BackgroundWorker<T> {
	boolean cancelled = false;
	worker task;
	BackgroundWorkerCallback callback;

	public void setTask(worker task) {
		this.task = task;
	}

	public void setCallback(BackgroundWorkerCallback callback) {
		this.callback = callback;
	}

	public void cancel() {
		cancelled = true;
	}

	interface worker<T> {
		void doWork(BackgroundWorkerCallback callback);
	}

	interface BackgroundWorkerCallback<T> {
		void onInitialize();

		default void onProgress(float progress) {
		}

		void onFailure(Exception e);

		void onComplete(T result);
	}

	public void run() {
		Executors.newSingleThreadExecutor().execute(() -> {
			task.doWork(callback);
		});
	}

	public boolean isCancelled() {
		return cancelled;
	}
}
