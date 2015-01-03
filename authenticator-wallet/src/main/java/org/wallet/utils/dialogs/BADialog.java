package org.wallet.utils.dialogs;

import javax.annotation.Nullable;

public class BADialog extends BADialogBase{
	
	public enum BADialogResponse {
		Yes,
		No,
		Ok,
		Cancel;
	}
	
	public interface BADialogResponseListner {
		public void onResponse(BADialogResponse response, @Nullable String input);
	}
	
	public static BADialog info(Class<?> resourceClass, String title, String desc) {
		return DialogFactory.infoDialog(resourceClass, title, desc);
	}
	
	public static BADialog confirm(Class<?> resourceClass, String title, String desc, BADialogResponseListner response) {
		return DialogFactory.confirmDialog(resourceClass, response, title, desc);
	}
	
	public static BADialog input(Class<?> resourceClass, String title, String desc, BADialogResponseListner response) {
		return DialogFactory.inputDialog(resourceClass, response, title, desc);
	}
	
	public void show() {
		this.showDialog();
	}
}
