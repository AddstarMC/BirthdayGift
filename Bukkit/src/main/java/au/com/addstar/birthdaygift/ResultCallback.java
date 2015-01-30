package au.com.addstar.birthdaygift;

public interface ResultCallback<T>
{
	public void onCompleted(boolean success, T value, Throwable error);
}
