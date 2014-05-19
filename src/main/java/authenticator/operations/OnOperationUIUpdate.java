package authenticator.operations;

import javax.annotation.Nullable;

public interface OnOperationUIUpdate {
	public void onBegin(String str);
	public void statusReport(String report);
	public void onFinished(String str);
	public void onError(@Nullable Exception e, @Nullable Throwable t);
}
