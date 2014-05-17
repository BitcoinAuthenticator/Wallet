package authenticator.operations;

public interface OnOperationUIUpdate {
	public void onBegin(String str);
	public void statusReport(String report);
	public void onFinished(String str);
	public void onError(Exception e);
}
