package wallettemplate.utils.dialogs;

import org.apache.tools.ant.Main;

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
}
