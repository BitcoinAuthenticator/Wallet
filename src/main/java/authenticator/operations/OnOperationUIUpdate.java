package authenticator.operations;

import javax.annotation.Nullable;

public interface OnOperationUIUpdate {
	// progress
	public void onBegin(String str);
	public void statusReport(String report);
	public void onFinished(String str);
	// User interaction
	public void onUserCancel(String reason);
	public void onUserOk(String msg);
	// Error
	public void onError(@Nullable Exception e, @Nullable Throwable t);
}
