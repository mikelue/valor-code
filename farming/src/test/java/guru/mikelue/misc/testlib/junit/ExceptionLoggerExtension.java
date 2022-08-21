package guru.mikelue.misc.testlib.junit;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.slf4j.LoggerFactory;

/**
 * When testing, this extension output stack trace to log of SLF4.
 *
 * The stack trace is emitted until this extension reaching to "java.base" module or
 * "org.junit" package.
 */
public class ExceptionLoggerExtension implements TestExecutionExceptionHandler {
	private final static int MIN_TRACE_SIZE = 3;

	public ExceptionLoggerExtension() {}

	@Override
	public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable
	{
		var classOfTested = context.getRequiredTestClass();
		var logger = LoggerFactory.getLogger(classOfTested);

		if (AssertionError.class.isInstance(throwable)) {
			throw throwable;
		}

		logger.error(
			"{} Test: {}{}",
			throwable,
			formatContext(context),
			formatUntilSystemType(throwable)
		);

		throw throwable;
	}

	private String formatUntilSystemType(Throwable throwable)
	{
		var formattedTrace = new StringBuilder();

		var shownThrowable = ExceptionUtils.getRootCause(throwable);
		if (shownThrowable != null) {
			formattedTrace.append("\nCaused(Root) by: ");
			formattedTrace.append(shownThrowable);
		} else {
			shownThrowable = throwable;
		}

		appendStackTrace(formattedTrace, throwable);

		return formattedTrace.toString();
	}
	private void appendStackTrace(StringBuilder builder, Throwable throwable)
	{
		int traceSize = 0;
		var stackTrace = throwable.getStackTrace();
		for (var traceEle: stackTrace) {
			var moduleName = traceEle.getModuleName();
			if (moduleName != null && moduleName.startsWith("java.") && traceSize >= MIN_TRACE_SIZE) {
				break;
			}

			var className = traceEle.getClassName();
			if (className != null && className.startsWith("org.junit.") && traceSize >= MIN_TRACE_SIZE) {
				break;
			}

			builder.append("\n\tat ");
			builder.append(traceEle);

			traceSize++;
		}

		if (stackTrace.length > traceSize) {
			builder.append(String.format("\n\t(Skip %d other stack traces ...)", stackTrace.length - traceSize));
		}
	}

	private String formatContext(ExtensionContext context)
	{
		var lifecycle = context.getTestInstanceLifecycle();
		Object lifecycleInfo = lifecycle.isPresent() ?
			lifecycle.get() : "!Unknown Lifecycle!";

		Object testEntity = getTestEntity(context);

		var displayName = context.getDisplayName();

		if (!"".equals(displayName)) {
			return String.format(
				"%s\n>>>>> [%s] Test entity: [%s].",
				displayName, lifecycleInfo, testEntity
			);
		}

		return String.format(
			"[%s] Test entity: [%s].",
			lifecycleInfo, testEntity
		);
	}

	private Object getTestEntity(ExtensionContext context)
	{
		var testMethod = context.getTestMethod();

		if (testMethod.isPresent()) {
			return testMethod.get();
		}

		var testClass = context.getTestClass();

		return testClass.isPresent() ?
			testClass.get() : "!Unknown class/method!";
	}
}
