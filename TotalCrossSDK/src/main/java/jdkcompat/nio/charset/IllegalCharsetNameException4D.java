/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package jdkcompat.nio.charset;

/**
 * An {@code IllegalCharsetNameException} is thrown when an illegal charset name
 * is encountered.
 */
public class IllegalCharsetNameException4D extends IllegalArgumentException {

    /*
     * This constant is used during deserialization to check the version
     * which created the serialized object.
     */
    private static final long serialVersionUID = 1457525358470002989L;

    // The illegal charset name
    private String charsetName;

    /**
     * Constructs a new {@code IllegalCharsetNameException} with the supplied
     * charset name.
     * 
     * @param charset
     *            the encountered illegal charset name.
     */
    public IllegalCharsetNameException4D(String charset) {
        // niochar.0F=The illegal charset name is "{0}".
        super("The illegal charset name is " + charset); //$NON-NLS-1$
        this.charsetName = charset;
    }

    /**
     * Gets the encountered illegal charset name.
     * 
     * @return the encountered illegal charset name.
     */
    public String getCharsetName() {
        return this.charsetName;
    }
}
