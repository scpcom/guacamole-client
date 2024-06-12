/*
 * Copyright 2023 NetKnights GmbH - lukas.matusiewicz@netknights.it
 * - Modified
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License here:
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.guacamole.auth.privacyidea.user;

import org.privacyidea.IPILogger;
import org.privacyidea.PrivacyIDEA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PILogImplementation implements IPILogger
{
    /**
     * Logger for PrivacyIDEA class.
     */
    private final Logger logger = LoggerFactory.getLogger(PrivacyIDEA.class);

    @Override
    public void log(String message)
    {
        logger.info(message);
    }

    @Override
    public void error(String message)
    {
        logger.error(message);
    }

    @Override
    public void log(Throwable t)
    {
        logger.info("privacyIDEA log", t);
    }

    @Override
    public void error(Throwable t)
    {
        logger.error("privacyIDEA error", t);
    }
}
