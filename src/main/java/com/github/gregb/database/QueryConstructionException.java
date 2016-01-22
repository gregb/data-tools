package com.github.gregb.database;

import org.springframework.dao.UncategorizedDataAccessException;

@SuppressWarnings("serial")
public class QueryConstructionException extends UncategorizedDataAccessException {

	public QueryConstructionException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
