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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.form.Form;
import org.apache.guacamole.net.auth.DecoratingDirectory;
import org.apache.guacamole.net.auth.DelegatingUserContext;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.net.auth.User;
import org.apache.guacamole.net.auth.UserContext;

/**
 * PrivacyIDEA-specific UserContext implementation which wraps the UserContext of
 * some other extension, providing (or hiding) additional data.
 */
public class PrivacyIDEAUserContext extends DelegatingUserContext {

    /**
     * Creates a new PrivacyIDEAUserContext which wraps the given UserContext,
     * providing (or hiding) additional PrivacyIDEA-specific data.
     *
     * @param userContext
     *     The UserContext to wrap.
     */
    public PrivacyIDEAUserContext(UserContext userContext) {
        super(userContext);
    }

    @Override
    public Directory<User> getUserDirectory() throws GuacamoleException {
        return new DecoratingDirectory<User>(super.getUserDirectory()) {

            @Override
            protected User decorate(User object) {
                return new PrivacyIDEAUser(object);
            }

            @Override
            protected User undecorate(User object) {
                assert(object instanceof PrivacyIDEAUser);
                return ((PrivacyIDEAUser) object).getUndecorated();
            }

        };
    }

    @Override
    public Collection<Form> getUserAttributes() {
        Collection<Form> userAttrs = new HashSet<>(super.getUserAttributes());
        userAttrs.add(PrivacyIDEAUser.PRIVACYIDEA_CONFIG_FORM);
        return Collections.unmodifiableCollection(userAttrs);
    }

}
