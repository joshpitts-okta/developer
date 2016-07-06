/*
**  Copyright (c) 2016, Cascade Computer Consulting.
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
package com.ccc.tools.app.serviceUtility.status;




/**
 * A configurable application status tracking interface.
 * </p>
 * This interface extends the <code>StatusTracker</code> interface with an init method
 * which takes a custom configuration object.  The configuration object is typically 
 * declared and typed by the <code>Service <code>T</code> implementation. 
 * @param <T> the custom <code>Service</code> configuration implementation object .
 * @see Service
 */
public interface CustomStatusTracker<T> extends StatusTracker
{
    /**
     * Initialize the status tracker with the given custom configuration object. 
     * @param config the custom configuration object.  Can be declared <code>&ltVoid&gt</code>.
     */
    public void init(T config);
}
