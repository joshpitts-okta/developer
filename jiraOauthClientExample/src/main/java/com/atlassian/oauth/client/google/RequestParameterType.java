/*
**  Copyright (c) 2010-2015, Panasonic Corporation.
**
**  Permission to use, copy, modify, and/or distribute this software for any
**  purpose with or without fee is hereby granted, provided that the above
**  copyright notice and this permission notice appear in all copies.
**
**  THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
**  WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
**  MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
**  ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
**  WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
**  ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
**  OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
*/
package com.atlassian.oauth.client.google;

/**
 * Where to place OAuth parameters in an HTTP message. The alternatives are
 * summarized in <a href="http://oauth.net/documentation/spec">OAuth Core</a>
 * under <a href="http://oauth.net/core/1.0a#consumer_req_param">Consumer
 * Request Parameters</a>.
 */
public enum RequestParameterType
{
    /**
     * Send parameters whose names begin with "oauth_" in an HTTP header, and
     * other parameters (whose names don't begin with "oauth_") in either the
     * message body or URL query string. The header formats are specified by
     * OAuth Core under <a href="http://oauth.net/core/1.0a#auth_header">OAuth
     * HTTP Authorization Scheme</a>.
     */
    AuthorizationHeader,

    /**
     * Send all parameters in the message body, with a Content-Type of
     * application/x-www-form-urlencoded.
     */
    Body,

    /** Send all parameters in the query string part of the URL. */
    QueryString;
}
