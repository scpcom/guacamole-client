/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole.auth.privacyidea.user;

import java.security.SecureRandom;
import java.util.Random;

/**
 * The key used to generate TOTP codes for a particular user.
 */
public class UserTOTPKey {

    /**
     * Secure source of random bytes.
     */
    private static final Random RANDOM = new SecureRandom();

    /**
     * The username of the user associated with this key.
     */
    private final String username;

    /**
     * Whether the associated secret key has been confirmed by the user. A key
     * is confirmed once the user has successfully entered a valid TOTP
     * derived from that key.
     */
    private boolean confirmed;

    /**
     * The base32-encoded TOTP key associated with the user.
     */
    private byte[] secret;

    /**
     * Generates the given number of random bytes.
     *
     * @param length
     *     The number of random bytes to generate.
     *
     * @return
     *     A new array of exactly the given number of random bytes.
     */
    private static byte[] generateBytes(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    /**
     * Creates a new, unconfirmed, randomly-generated TOTP key having the given
     * length.
     *
     * @param username
     *     The username of the user associated with this key.
     *
     * @param length
     *     The length of the key to generate, in bytes.
     */
    public UserTOTPKey(String username, int length) {
        this(username, generateBytes(length), false);
    }

    /**
     * Creates a new UserTOTPKey containing the given key and having the given
     * confirmed state.
     *
     * @param username
     *     The username of the user associated with this key.
     *
     * @param secret
     *     The raw binary secret key to be used to generate TOTP codes.
     *
     * @param confirmed
     *     true if the user associated with the key has confirmed that they can
     *     successfully generate the corresponding TOTP codes (the user has
     *     been "enrolled"), false otherwise.
     */
    public UserTOTPKey(String username, byte[] secret, boolean confirmed) {
        this.username = username;
        this.confirmed = confirmed;
        this.secret = secret;
    }

    /**
     * Returns the username of the user associated with this key.
     *
     * @return
     *     The username of the user associated with this key.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the raw binary secret key to be used to generate TOTP codes.
     *
     * @return
     *     The raw binary secret key to be used to generate TOTP codes.
     */
    public byte[] getSecret() {
        return secret;
    }

    /**
     * Returns whether the user associated with the key has confirmed that they
     * can successfully generate the corresponding TOTP codes (the user has
     * been "enrolled").
     *
     * @return
     *     true if the user has confirmed that they can successfully generate
     *     the TOTP codes generated by this key, false otherwise.
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Sets whether the user associated with the key has confirmed that they
     * can successfully generate the corresponding TOTP codes (the user has
     * been "enrolled").
     *
     * @param confirmed
     *     true if the user has confirmed that they can successfully generate
     *     the TOTP codes generated by this key, false otherwise.
     */
    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

}
