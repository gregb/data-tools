package com.github.gregb.database;

@SuppressWarnings("serial")
public class AccessException extends RuntimeException {

	public Class<?> klass;
	public final long requestedObjectId;

	public AccessException(final Class<?> klass, final long requestedObjectId) {
		this(klass, requestedObjectId, "Unable to retrive " + klass.getSimpleName() + " with id = " + requestedObjectId);
	}

	public AccessException(final Class<?> klass, final long requestedObjectId, final String message) {
		super(message);
		this.klass = klass;
		this.requestedObjectId = requestedObjectId;
	}

	public AccessException(final Class<?> klass, final long requestedObjectId, final Throwable e) {
		super("Unable to retrive " + klass.getSimpleName() + " with id = " + requestedObjectId, e);
		this.klass = klass;
		this.requestedObjectId = requestedObjectId;
	}

}
