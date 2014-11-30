package org.wallet.ControllerHelpers;

import javafx.application.Platform;

/**
 *
 * @author Victor Oliveira
 */
public abstract class AsyncTask {

    private boolean daemon = true;
    
    abstract protected void onPreExecute();

    abstract protected void doInBackground();

    /**
     * This method runs on the UI thread
     */
    abstract protected void onPostExecute();
    
    abstract protected void progressCallback(Object... params);
    
    public void publishProgress(final Object... params) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                progressCallback(params);
            }
        });
    }
    
    private final Thread backGroundThread = new Thread(new Runnable() {
        @Override
        public void run() {

            doInBackground();

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    onPostExecute();
                }
            });
        }
    });

    public void execute() {

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                
                onPreExecute();

                backGroundThread.setDaemon(daemon);
                backGroundThread.start();
            }
        });
    }

    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }
    
    public void interrupt(){
        this.backGroundThread.interrupt();
   }
}
