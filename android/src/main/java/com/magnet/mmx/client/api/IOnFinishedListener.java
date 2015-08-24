package com.magnet.mmx.client.api;

interface IOnFinishedListener<T, U extends MMX.FailureCode> {
    /**
     * Invoked if the operation succeeded
     *
     * @param result the result of the operation
     */
    void onSuccess(T result);

    /**
     * Invoked if the operation failed
     *
     * @param code the failure code
     * @param ex the exception, null if no exception
     */
    void onFailure(U code, Throwable ex);
}
