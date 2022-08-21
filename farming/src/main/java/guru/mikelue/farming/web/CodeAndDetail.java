package guru.mikelue.farming.web;

public class CodeAndDetail {
	private final int code;
	private final Object detail;

	public CodeAndDetail(int code)
	{
		this(code, null);
	}

	public CodeAndDetail(int newCode, Object newDetail)
	{
		code = newCode;
		detail = newDetail;
	}

	/**
	 * Gets value of code.<p>
	 *
	 * @return value of code
	 */
	public int getCode()
	{
		return code;
	}

	/**
	 * Gets detail information.<p>
	 *
	 * @return detail information
	 */
	public Object getDetail()
	{
		return detail;
	}
}

