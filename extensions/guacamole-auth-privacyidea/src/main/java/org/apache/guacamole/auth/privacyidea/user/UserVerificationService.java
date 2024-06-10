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

import com.google.common.io.BaseEncoding;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.security.InvalidKeyException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleSecurityException;
import org.apache.guacamole.GuacamoleUnsupportedException;
import org.apache.guacamole.auth.privacyidea.conf.ConfigurationService;
import org.apache.guacamole.auth.privacyidea.form.AuthenticationCodeField;
import org.apache.guacamole.form.Field;
import org.apache.guacamole.language.TranslatableGuacamoleClientException;
import org.apache.guacamole.language.TranslatableGuacamoleInsufficientCredentialsException;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.User;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.privacyidea.PrivacyIDEA;
import org.privacyidea.PIResponse;
import org.privacyidea.RolloutInfo;
import org.privacyidea.TokenInfo;

/**
 * Service for verifying the identity of a user using PrivacyIDEA.
 */
public class UserVerificationService {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(UserVerificationService.class);

    /**
     * BaseEncoding instance which decoded/encodes base32.
     */
    private static final BaseEncoding BASE32 = BaseEncoding.base32();

    /**
     * Service for retrieving configuration information.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * Service for tracking whether TOTP codes have been used.
     */
    @Inject
    private CodeUsageTrackingService codeService;

    /**
     * Provider for AuthenticationCodeField instances.
     */
    @Inject
    private Provider<AuthenticationCodeField> codeFieldProvider;

    private int maxretries = 120;

    private PrivacyIDEA privacyIDEA;

    private String transactionID = "";

    private String tokenRollout(String username, String secret) {
        if (!privacyIDEA.serviceAccountAvailable())
            return secret;
        RolloutInfo rolloutInfo = privacyIDEA.tokenRollout(username, "totp");
        if (rolloutInfo == null)
            return secret;
        return rolloutInfo.otpkey.value_b32;
    }

    /**
     * Retrieves and decodes the base32-encoded TOTP key associated with user
     * having the given UserContext. If no TOTP key is associated with the user,
     * a random key is generated and associated with the user. If the extension
     * storing the user does not support storage of the TOTP key, null is
     * returned.
     *
     * @param context
     *     The UserContext of the user whose TOTP key should be retrieved.
     *
     * @param username
     *     The username of the user associated with the given UserContext.
     *
     * @return
     *     The TOTP key associated with the user having the given UserContext,
     *     or null if the extension storing the user does not support storage
     *     of the TOTP key.
     *
     * @throws GuacamoleException
     *     If a new key is generated, but the extension storing the associated
     *     user fails while updating the user account.
     */
    private UserTOTPKey getKey(UserContext context,
            String username, int tokenInfo) throws GuacamoleException {

        // Retrieve attributes from current user
        User self = context.self();
        Map<String, String> attributes = context.self().getAttributes();

        // If no key is defined, attempt to generate a new key
        String secret = attributes.get(PrivacyIDEAUser.TOTP_KEY_SECRET_ATTRIBUTE_NAME);
        transactionID = attributes.get(PrivacyIDEAUser.PRIVACYIDEA_TRANSACTION_ID_ATTRIBUTE_NAME);

        if (tokenInfo == 0) {
            // Let PrivacyIDEA generate the new key
            secret = tokenRollout(username, secret);
        }

        if (secret == null || secret.isEmpty()) {

            // Generate random key for user
            UserTOTPKey generated = new UserTOTPKey(username,20);
            if (setKey(context, generated))
                return generated;

            // Fail if key cannot be set
            return null;

        }

        // Parse retrieved base32 key value
        byte[] key;
        try {
            key = BASE32.decode(secret);
        }

        // If key is not valid base32, warn but otherwise pretend the key does
        // not exist
        catch (IllegalArgumentException e) {
            logger.warn("TOTP key of user \"{}\" is not valid base32.", self.getIdentifier());
            logger.debug("TOTP key is not valid base32.", e);
            return null;
        }

        // Otherwise, parse value from attributes
        boolean confirmed = "true".equals(attributes.get(PrivacyIDEAUser.TOTP_KEY_CONFIRMED_ATTRIBUTE_NAME));
        UserTOTPKey outkey = new UserTOTPKey(username, key, confirmed);

        if (tokenInfo == 0)
            setKey(context, outkey);

        return outkey;

    }

    /**
     * Attempts to store the given TOTP key within the user account of the user
     * having the given UserContext. As not all extensions will support storage
     * of arbitrary attributes, this operation may fail.
     *
     * @param context
     *     The UserContext associated with the user whose TOTP key is to be
     *     stored.
     *
     * @param key
     *     The TOTP key to store.
     *
     * @return
     *     true if the TOTP key was successfully stored, false if the extension
     *     handling storage does not support storage of the key.
     *
     * @throws GuacamoleException
     *     If the extension handling storage fails internally while attempting
     *     to update the user.
     */
    private boolean setKey(UserContext context, UserTOTPKey key)
            throws GuacamoleException {

        // Get mutable set of attributes
        User self = context.self();
        Map<String, String> attributes = new HashMap<>();

        // Set/overwrite current TOTP key state
        attributes.put(PrivacyIDEAUser.TOTP_KEY_SECRET_ATTRIBUTE_NAME, BASE32.encode(key.getSecret()));
        attributes.put(PrivacyIDEAUser.PRIVACYIDEA_TRANSACTION_ID_ATTRIBUTE_NAME, transactionID);
        attributes.put(PrivacyIDEAUser.TOTP_KEY_CONFIRMED_ATTRIBUTE_NAME, key.isConfirmed() ? "true" : "false");
        self.setAttributes(attributes);

        // Confirm that attributes have actually been set
        Map<String, String> setAttributes = self.getAttributes();
        if (!setAttributes.containsKey(PrivacyIDEAUser.TOTP_KEY_SECRET_ATTRIBUTE_NAME)
                || !setAttributes.containsKey(PrivacyIDEAUser.TOTP_KEY_CONFIRMED_ATTRIBUTE_NAME))
            return false;

        // Update user object
        try {
            context.getPrivileged().getUserDirectory().update(self);
        }
        catch (GuacamoleSecurityException e) {
            logger.info("User \"{}\" cannot store their TOTP key as they "
                    + "lack permission to update their own account and the "
                    + "PrivacyIDEA extension was unable to obtain privileged access. "
                    + "TOTP will be disabled for this user.",
                    self.getIdentifier());
            logger.debug("Permission denied to set TOTP key of user "
                    + "account.", e);
            return false;
        }
        catch (GuacamoleUnsupportedException e) {
            logger.debug("Extension storage for user is explicitly read-only. "
                    + "Cannot update attributes to store TOTP key.", e);
            return false;
        }

        // TOTP key successfully stored/updated
        return true;

    }

    public boolean doPushRequest(String username, String otp) {
        if (transactionID == "timeout")
            return false;

        PIResponse initialResponse = privacyIDEA.validateCheck(username, otp);

        if (initialResponse == null)
            return false;

        List<String> triggeredTypes = initialResponse.triggeredTokenTypes();

        if (initialResponse.type.startsWith("totp") && initialResponse.status && initialResponse.value)
            transactionID = "totp-ok";
        else if (triggeredTypes.contains("push"))
            transactionID = initialResponse.transactionID;

        if (transactionID == null || transactionID.isEmpty())
            return false;

        return true;
    }

    public boolean doPushSynchronous(String otp) {
        boolean authok = false;
        int retries = 0;

        if (otp != null && !otp.isEmpty())
            return false;
        if (transactionID == null || transactionID.isEmpty())
            return false;
        if (transactionID == "timeout")
            return false;

        while (!authok) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {}
            authok = privacyIDEA.pollTransaction(transactionID);
            retries++;
            if (retries > maxretries) break;
        }

        if (authok)
            return true;

        transactionID = "timeout";

        return false;
    }

    private int getTokenInfo(String username) {
        if (!privacyIDEA.serviceAccountAvailable())
            return -1;

	List<TokenInfo> tokenInfoList = privacyIDEA.getTokenInfo(username);
	if (tokenInfoList == null)
            return -2;

        // TODO: Get TokenInfo details

        return tokenInfoList.size();
    }

    /**
     * Verifies the identity of the given user using PrivacyIDEA. If a authentication
     * code from the user's PrivacyIDEA device has not already been provided, a code is
     * requested in the form of additional expected credentials. Any provided
     * code is cryptographically verified. If no code is present, or the
     * received code is invalid, an exception is thrown.
     *
     * @param context
     *     The UserContext provided for the user by another authentication
     *     extension.
     *
     * @param authenticatedUser
     *     The user whose identity should be verified using PrivacyIDEA.
     *
     * @throws GuacamoleException
     *     If required PrivacyIDEA-specific configuration options are missing or
     *     malformed, or if the user's identity cannot be verified.
     */
    public void verifyIdentity(UserContext context,
            AuthenticatedUser authenticatedUser) throws GuacamoleException {

        // Ignore anonymous users
        String username = authenticatedUser.getIdentifier();
        if (username.equals(AuthenticatedUser.ANONYMOUS_IDENTIFIER))
            return;

        String PrivacyIDEAHost = confService.getPrivacyIDEAHost();
        int tokenInfo = -3;

        if (PrivacyIDEAHost != null) {
            String piServiceAccount = confService.getPrivacyIDEAServiceAccount();
            String piServicePassword = confService.getPrivacyIDEAServicePassword();
            String piServiceRealm = confService.getPrivacyIDEAServiceRealm();

            privacyIDEA = PrivacyIDEA.newBuilder(PrivacyIDEAHost, "guacamole")
                                 .serviceAccount(piServiceAccount, piServicePassword)
                                 .serviceRealm(piServiceRealm)
                                 .sslVerify(false)
                                 .logger(new PILogImplementation())
                                 .simpleLogger(System.out::println)
                                 .build();

            tokenInfo = getTokenInfo(username);
        }

        // Ignore users which do not have an associated key
        UserTOTPKey key = getKey(context, username, tokenInfo);
        if (key == null)
            return;

        // Pull the original HTTP request used to authenticate
        Credentials credentials = authenticatedUser.getCredentials();
        HttpServletRequest request = credentials.getRequest();

        // Retrieve TOTP from request
        String code = request.getParameter(AuthenticationCodeField.PARAMETER_NAME);

        if (PrivacyIDEAHost != null) {
            // If the user hasn't completed enrollment, request that they do
            if (tokenInfo == 0) {
                AuthenticationCodeField field = codeFieldProvider.get();

                field.exposeKey(key);
                throw new TranslatableGuacamoleInsufficientCredentialsException(
                        "TOTP enrollment must be completed before "
                        + "authentication can continue",
                        "PRIVACYIDEA.INFO_ENROLL_REQUIRED", new CredentialsInfo(
                            Collections.<Field>singletonList(field)
                        ));
            }

            if (doPushSynchronous(code)) {
                transactionID = null;
                if (!key.isConfirmed())
                    key.setConfirmed(true);
                setKey(context, key);
                return;
            }
            if (transactionID == "timeout") {
                transactionID = null;
                setKey(context, key);
            }

            if (!doPushRequest(username, code)) {
                transactionID = null;
            }
            if (transactionID == "totp-ok") {
                transactionID = null;
                if (!key.isConfirmed())
                    key.setConfirmed(true);
                setKey(context, key);
                return;
            }

            setKey(context, key);
        }

        // If no TOTP provided, request one
        if (code == null) {

            AuthenticationCodeField field = codeFieldProvider.get();

            // If the user hasn't completed enrollment, request that they do
            // Todo: We could get and show a QR code from privacyIDEA here

            if (transactionID != null) {
                // Otherwise simply request the user's confirmation
                throw new TranslatableGuacamoleInsufficientCredentialsException(
                        "A TOTP confirmation is required before login can "
                        + "continue", "PRIVACYIDEA.INFO_CONFIRMATION_REQUIRED", new CredentialsInfo(
                            Collections.<Field>singletonList(field)
                        ));
            }

            // Otherwise simply request the user's authentication code
            throw new TranslatableGuacamoleInsufficientCredentialsException(
                    "A TOTP authentication code is required before login can "
                    + "continue", "PRIVACYIDEA.INFO_CODE_REQUIRED", new CredentialsInfo(
                        Collections.<Field>singletonList(field)
                    ));

        }

        // Provided code is not valid
        throw new TranslatableGuacamoleClientException("Provided TOTP code "
                + "is not valid.", "PRIVACYIDEA.INFO_VERIFICATION_FAILED");

    }

}
