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

package org.apache.guacamole.auth.totp.form;

import org.apache.guacamole.form.Field;

/**
 * The invisible field that stores the state of the privacyIDEA
 * connection.  The state is simply a placeholder that helps
 * the client and server pick back up the conversation
 * at the correct spot during challenge/response.
 */
public class PrivacyIDEAStateField extends Field {

    /**
     * The parameter returned by the privacyIDEA state.
     */
    public static final String PARAMETER_NAME = "guac-privacyidea-state";

    /**
     * The type of field to initialize for the state.
     */
    private static final String PRIVACYIDEA_FIELD_TYPE = "GUAC_PRIVACYIDEA_STATE";

    /**
     * The state of the connection passed by the previous privacyIDEA attempt.
     */
    private final String privacyIDEAState;

    /**
     * Initialize the field with the state returned by the privacyIDEA server.
     *
     * @param privacyIDEAState
     *     The state returned by the privacyIDEA server.
     */
    public PrivacyIDEAStateField(String privacyIDEAState) {
        super(PARAMETER_NAME, PRIVACYIDEA_FIELD_TYPE);
        this.privacyIDEAState = privacyIDEAState;

    }

    /**
     * Get the state provided by the privacyIDEA server.
     *
     * @return
     *     The state provided by the privacyIDEA server.
     */
    public String getPrivacyIDEAState() {
        return privacyIDEAState;
    }

}
