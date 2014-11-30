package org.wallet;

import org.wallet.utils.BaseUI;

import javafx.animation.RotateTransition;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

public class ShutDownController  extends BaseUI{
	public ImageView ivSpinner;
	
	public void initialize() {
        super.initialize(ShutDownController.class);
        startSyncRotation();
	}
	
	private RotateTransition rt;
    private void startSyncRotation(){
    	if(rt == null){
            rt = new RotateTransition(Duration.millis(2000),ivSpinner);
            rt.setByAngle(360);
            rt.setCycleCount(10000);
    	}
        rt.play();
    }
}
