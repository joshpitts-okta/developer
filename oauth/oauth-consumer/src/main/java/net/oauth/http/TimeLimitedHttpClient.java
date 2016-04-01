/*
 * Copyright 2009 John Kristian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.oauth.http;

import java.util.concurrent.Future;

import java.io.InterruptedIOException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A decorator for an HttpClient, to limit the amount of time to send a request
 * and receive the response. Typical usage: <code>
 * OAuthClient client = new OAuthClient(new TimeLimitedHttpClient(new HttpClient3()));
 * client.getHttpParameters().put(TimeLimitedHttpClient.EXECUTE_TIMEOUT, 3000);
 * client.access(...
 * </code>
 * 
 * @author John Kristian
 */
public class TimeLimitedHttpClient implements HttpClient {

    /**
     * The name of the HTTP parameter that is the maximum time to wait for the
     * execute method. (Long msec)
     */
    public static final String EXECUTE_TIMEOUT = "executeTimeout";

    public TimeLimitedHttpClient(HttpClient client) {
        this(client, Executors.newCachedThreadPool(DAEMON_THREAD_FACTORY));
    }

    public TimeLimitedHttpClient(HttpClient client, Executor executor) {
        this.client = client;
        this.executor = executor;
    }

    protected final HttpClient client;
    protected final Executor executor;

    protected static final ThreadFactory DAEMON_THREAD_FACTORY = new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }
    };

    /**
     * Send an HTTP request and return the response. If httpParameters contains
     * an integer value for EXECUTE_TIMEOUT, either return or propagate an
     * exception within that many milliseconds (approximately). If there is no
     * such value in httpParameters, simply call the underlying
     * HttpClient.execute.
     * 
     * @throws SocketTimeoutException
     *             the EXECUTE_TIMEOUT was exceeded
     * @throws InterruptedIOException
     *             this thread was interrupted
     * @throws IOException
     *             from the underlying HttpClient
     */
    public HttpResponseMessage execute(HttpMessage request, Map<String, Object> httpParameters) throws IOException {
        Object et = httpParameters.get(EXECUTE_TIMEOUT);
        if (et == null) {
            return client.execute(request, httpParameters);
        }
        long timeout = Long.parseLong(et.toString());
        Future<HttpResponseMessage> response = start(request, httpParameters);
        try {
            return response.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            IOException e2 = new SocketTimeoutException("no response after " + timeout + " msec");
            e2.initCause(e);
            throw e2;
        } catch (InterruptedException e) {
            IOException e2 = new InterruptedIOException(e.getMessage());
            e2.initCause(e);
            throw e2;
        } catch (ExecutionException e) {
            throw wrap(e.getCause());
        } catch (Exception e) {
            throw wrap(e);
        }
    }

    /**
     * At some future time, send the given HTTP request and return the response.
     * 
     * @return the future response
     */
    protected Future<HttpResponseMessage> start(HttpMessage request, Map<String, Object> httpParameters) {
        FutureTask<HttpResponseMessage> task = new FutureTask<HttpResponseMessage>(new Execute(client, request,
                httpParameters));
        executor.execute(task);
        return task;
    }

    protected static class Execute implements Callable<HttpResponseMessage> {

        protected Execute(HttpClient client, HttpMessage request, Map<String, Object> httpParameters) {
            this.client = client;
            this.request = request;
            this.httpParameters = httpParameters;
        }

        protected final HttpClient client;
        protected final HttpMessage request;
        protected final Map<String, Object> httpParameters;

        /** Send the given HTTP request and return the response. */
        public HttpResponseMessage call() throws IOException {
            return client.execute(request, httpParameters);
        }
    }

    /**
     * Return an IOException that either is the given exception, or contains the
     * given exception as its cause.
     */
    protected IOException wrap(Throwable e) {
        if (e instanceof IOException) {
            return (IOException) e;
        }
        IOException e2 = new IOException(e.getMessage());
        e2.initCause(e);
        return e2;
    }

}
