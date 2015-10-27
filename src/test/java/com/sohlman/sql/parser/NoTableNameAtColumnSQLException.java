package com.sohlman.sql.parser;

public class NoTableNameAtColumnSQLException extends Exception {
	public NoTableNameAtColumnSQLException(String message) {
		super(message);
	}
}
