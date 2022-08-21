package guru.mikelue.misc.testlib;

import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import guru.mikelue.misc.testlib.junit.ExceptionLoggerExtension;

@ExtendWith(ExceptionLoggerExtension.class)
public abstract class AbstractTestBase {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	protected AbstractTestBase() {}

	public Logger getLogger()
	{
		return logger;
	}
}
