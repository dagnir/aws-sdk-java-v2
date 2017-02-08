/*
 * Copyright 2013-2017 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */

package software.amazon.awssdk.services.securitytoken.auth.policy;

import software.amazon.awssdk.auth.policy.Action;
import software.amazon.awssdk.auth.policy.Statement;
import software.amazon.awssdk.services.securitytoken.AWSSecurityTokenService;

/**
 * The available AWS access control policy actions for Amazon Security Token Service.
 *
 * @see Statement#setActions(java.util.Collection)
 * @deprecated in favor of {@link software.amazon.awssdk.auth.policy.actions.SecurityTokenServiceActions}
 */
@Deprecated
public enum STSActions implements Action {

    /**
     * Action for assuming role to do cross-account access or federation.
     *
     * @see AWSSecurityTokenService#assumeRole(software.amazon.awssdk.services.securitytoken.model.AssumeRoleRequest)
     */
    AssumeRole("sts:AssumeRole"),


    /**
     * Action for assuming role with web federation to get a set of temporary
     * security credentials for users who have been authenticated in a mobile or
     * web application with a web identity provider.
     *
     * @see AWSSecurityTokenService#assumeRoleWithWebIdentity(software.amazon.awssdk.services.securitytoken.model.AssumeRoleWithWebIdentityRequest)
     */
    AssumeRoleWithWebIdentity("sts:AssumeRoleWithWebIdentity");

    private final String action;

    private STSActions(String action) {
        this.action = action;
    }

    /* (non-Javadoc)
     * @see software.amazon.awssdk.auth.policy.Action#getId()
     */
    public String getActionName() {
        return this.action;
    }

}
