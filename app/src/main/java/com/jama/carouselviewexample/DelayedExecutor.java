package com.jama.carouselviewexample;

import android.os.Handler;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class DelayedExecutor {

	/*-------------------------*/
	/*         FIELDS          */
	/*-------------------------*/
	private final long delayInMilliseconds;
	private final Handler handler;
	private final Queue<DelayedRunnable> waitingRunnables;
	private final Map<Runnable, Integer> executeOnceRunnablesCount;

	/*-------------------------*/
	/*       CONSTRUCTORS      */
	/*-------------------------*/
	/**
	 * Creates a delayed executor with specific delay for all runnables added to this object in a future 
	 * @param delayInMilliseconds long specifying delay which will each runnable have to wait
	 */
	public DelayedExecutor(long delayInMilliseconds) {
		this.delayInMilliseconds = delayInMilliseconds;
		this.handler = new Handler();
		this.waitingRunnables = new LinkedList<>();
		this.executeOnceRunnablesCount = new HashMap<>();
	}
	
	/*-------------------------*/
	/*     PUBLIC METHODS      */
	/*-------------------------*/
	/**
	 * Adds a runnable which will be executed only if the {@link #delayInMilliseconds} expires for this
	 * runnable instance and this runnable is the last in the queue of the same runnables.
	 * @param runnable which will be executed only once and only if it is the last instance of the same runnable
	 * @see #execute(Runnable)
	 */
	public void executeOnce(Runnable runnable) {
		Integer amount = this.executeOnceRunnablesCount.get(runnable);
		int newAmount = (amount == null) ? 1 : amount + 1;
		this.executeOnceRunnablesCount.put(runnable, newAmount);
		
		boolean executeOnce = true;
		this.addDelayedRunnable(runnable, executeOnce);
	}
	
	public void executeSingle(Runnable runnable) {
		SameRunnable sameRunnable = new SameRunnable(runnable);
		this.executeOnce(sameRunnable);
	}
	
	/**
	 * Adds a runnable which will be executed after the {@link #delayInMilliseconds} expires for this runnable instance.
	 * @param runnable which will be executed after a delay
	 * @see #executeOnce(Runnable)
	 */
	public void execute(Runnable runnable) {
		boolean executeOnce = false;
		this.addDelayedRunnable(runnable, executeOnce);
	}
	
	/**
	 * Executes all waiting (not executed) runnables in the order in which they were provided.
	 */
	public void executeImmediately() {
		while (!this.waitingRunnables.isEmpty()) {
			DelayedRunnable waitingRunnable = this.waitingRunnables.peek();
			handler.removeCallbacks(waitingRunnable);
			waitingRunnable.run();
		}
	}
	
	/**
	 * Removes all waiting runnables, therefore preventing their execution in the future.
	 */
	public void clear() {
		DelayedRunnable waitingRunnable;
		while ((waitingRunnable = waitingRunnables.poll()) != null) {
			handler.removeCallbacks(waitingRunnable);
		}
		this.executeOnceRunnablesCount.clear();
	}
	
	/*-------------------------*/
	/*       GET METHODS       */
	/*-------------------------*/
	/**
	 * Returns the delay which will each runnable have to wait in order to be executed
	 * @return long specifying how long will each runnable have to wait 
	 */
	public long getDelayInMilliseconds() {
		return this.delayInMilliseconds;
	}
	
	/*-------------------------*/
	/*     PRIVATE METHODS     */
	/*-------------------------*/
	private void addDelayedRunnable(Runnable runnable, boolean executeOnlyOnce) {
		DelayedRunnable delayedRunnable = new DelayedRunnable(runnable, executeOnlyOnce);
		this.waitingRunnables.offer(delayedRunnable);
		this.handler.postDelayed(delayedRunnable, this.delayInMilliseconds);
	}
	
	/*-------------------------*/
	/*     OBJECT METHODS      */
	/*-------------------------*/
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Waiting runnables: ").append(this.waitingRunnables).append(", ");
		sb.append("Execute once runnables count: ").append(this.executeOnceRunnablesCount);
		return sb.toString();
	}
	
	/*-------------------------*/
	/*      INNER CLASSES      */
	/*-------------------------*/
	private class DelayedRunnable implements Runnable {

		private final Runnable runnable;
		private final boolean executeOnlyOnce;
		
		private DelayedRunnable(Runnable runnable, boolean executeOnlyOnce) {
			this.runnable = runnable;
			this.executeOnlyOnce = executeOnlyOnce;
		}

		@Override
		public void run() {
			DelayedExecutor.this.waitingRunnables.poll();
			
			if (this.executeOnlyOnce) {
				Integer amount = DelayedExecutor.this.executeOnceRunnablesCount.get(this.runnable);
				int newAmount = (amount - 1);
				DelayedExecutor.this.executeOnceRunnablesCount.put(this.runnable, newAmount);
				
				if (newAmount == 0) {
					DelayedExecutor.this.executeOnceRunnablesCount.remove(this.runnable);
					this.runnable.run();
				}
			}
			else {
				this.runnable.run();	
			}
		}
		
		@Override
		public String toString() {
			return this.runnable.toString();
		}
	}
	
	private static class SameRunnable implements Runnable {
		
		private final Runnable runnable;
		
		private SameRunnable(Runnable runnable) {
			this.runnable = runnable;
		}
		
		@Override
		public void run() {
			this.runnable.run();
		}
		
		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			return object != null && getClass() == object.getClass();
		}
		
		@Override
		public int hashCode() {
			return 1;
		}
	}
}