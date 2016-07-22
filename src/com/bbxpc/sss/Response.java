package com.bbxpc.sss;

public class Response {
	public final static char CR = (char) 0x0D;
	public final static char LF = (char) 0x0A;
	public final static String CRLF = "" + CR + LF;

	public static String mix(String body) {
		StringBuilder sbf = new StringBuilder();
		sbf.append("HTTP/1.1 200 OK");
		sbf.append(CRLF);
		sbf.append("Content-Type:text/html; charset=utf-8");
		sbf.append(CRLF);
		sbf.append("Connection:Keep-Alive");
		sbf.append(CRLF);
		sbf.append("Content-Length: ");
		sbf.append(body.getBytes().length);
		sbf.append(CRLF);
		sbf.append("Server:SSS1");
		sbf.append(CRLF);
		sbf.append(CRLF);
		sbf.append(body);
		return sbf.toString();
	}
}