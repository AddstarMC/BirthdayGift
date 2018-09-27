package au.com.addstar.birthdaygift;

public interface ResultCallback<T>
{
	void onCompleted(boolean success, T value, Throwable error);
}
