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

/**
 * Controller for the "GUAC_PRIVACYIDEA_CODE" field which prompts the user to enter
 * the code generated by their authentication device.
 */
angular.module('guacPrivacyIDEA').controller('authenticationCodeFieldController', ['$scope', '$window',
    function authenticationCodeFieldController($scope, $window) {

    /**
     * The secret key split into groups of at most four characters each, or
     * null if the secret key is not exposed.
     *
     * @type String[]
     */
    $scope.groupedSecret = $scope.field.secret && $scope.field.secret.match(/.{1,4}/g);

    /**
     * Whether the raw details of the secret key and TOTP configuration should
     * be shown. By default, such details are hidden. If the secret key is not
     * exposed, this property has no effect.
     */
    $scope.detailsShown = false;

    /**
     * Shows the raw details of the secret key and TOTP configuration. If the
     * secret key is not exposed, or the details are already shown, this
     * function has no effect.
     */
    $scope.showDetails = function showDetails() {
        $scope.detailsShown = true;
    };

    /**
     * Hides the raw details of the secret key and TOTP configuration. If the
     * details are already hidden, this function has no effect.
     */
    $scope.hideDetails = function hideDetails() {
        $scope.detailsShown = false;
    };

    /**
     * Attempts to open the "otpauth" URI containing the user's TOTP key,
     * invoking whichever application may be installed locally for handling
     * multi-factor authentication.
     */
    $scope.openKeyURI = function openKeyURI() {
        $window.open($scope.field.keyUri);
    };

}]);
