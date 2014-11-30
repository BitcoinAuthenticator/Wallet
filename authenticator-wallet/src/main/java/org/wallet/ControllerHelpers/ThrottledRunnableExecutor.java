package org.wallet.ControllerHelpers;

import java.util.Timer;
import java.util.TimerTask;

public class ThrottledRunnableExecutor {
	private boolean shouldExecute;
	private int delay;
	private int period;
	private Runnable runnable;
	
	Timer timer;
	TimerTask task;
	
	public ThrottledRunnableExecutor(int delay, int period, Runnable runnable){
		this.period = period;
		this.delay = delay;
		this.runnable = runnable;
	}
	public ThrottledRunnableExecutor(int period, Runnable runnable){
		this(0, period, runnable);
	}
	
	public void start(){
		shouldExecute = false;
		
		task = new TimerTask(){
			@Override
			public void run() {
				if(shouldExecute){
					 runnable.run();
					 shouldExecute = false;
				 }
			}
	     };
		
		timer = new Timer("Update Timer");
	    timer.scheduleAtFixedRate(task, delay, period);
	}
	
	public void execute(){
		shouldExecute = true;
	}
	
}
