package wallettemplate.utils.dialogs;

import org.apache.tools.ant.Main;

public class BADialog extends BADialogBase{
	static int STANDARD_WIDTH 	= 550;
	static int STANDARD_HEIGHT  = 140;
	
	public static BADialog info(Class<?> resourceClass, String title, String details) {
		return DialogFactory.infoDialog(resourceClass, title, details);
	}
	
	public void show() {
		this.showDialog();
	}
}
