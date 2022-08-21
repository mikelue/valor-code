package guru.mikelue.misc.testlib.assertj;

import org.assertj.core.api.ThrowableAssert;

public interface AssertjUtils {
	@FunctionalInterface
	public interface ThrowingCallableLambda extends ThrowableAssert.ThrowingCallable {}

	public static ThrowableAssert.ThrowingCallable asThrowingCallable(ThrowingCallableLambda r)
	{
		return r;
	}
}
