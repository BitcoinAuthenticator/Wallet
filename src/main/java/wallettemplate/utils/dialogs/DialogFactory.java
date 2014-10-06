package wallettemplate.utils.dialogs;

import org.apache.tools.ant.Main;

import wallettemplate.utils.dialogs.BADialog.BADialogResponse;
import wallettemplate.utils.dialogs.BADialog.BADialogResponseListner;

public class DialogFactory {
	static BADialog infoDialog(Class<?> resourceClass, String title, String desc) {
		BADialog ret = new BADialog();
		ret.setViewPath("utils/dialogs/infoDialog.fxml")
			.setResourceClass(resourceClass)
			.setWindowTitle("info")
			.setTitle(title)
			.setDesc(desc);
		return ret;
	}
	
	static BADialog confirmDialog(Class<?> resourceClass, BADialogResponseListner response, String title, String desc) {
		BADialog ret = new BADialog();
		ret.setViewPath("utils/dialogs/confirmDialog.fxml")
			.setResourceClass(resourceClass)
			.setWindowTitle("Confirm")
			.setTitle(title)
			.setDesc(desc)
			.setResponseListener(response);
		return ret;
	}
	
	static BADialog inputDialog(Class<?> resourceClass, BADialogResponseListner response, String title, String desc) {
		BADialog ret = new BADialog();
		ret.setViewPath("utils/dialogs/inputDialog.fxml")
			.setResourceClass(resourceClass)
			.setWindowTitle("Please Complete")
			.setTitle(title)
			.setDesc(desc)
			.setResponseListener(response);
		return ret;
	}
}
