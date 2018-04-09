/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.dynamodb.pojos;

import java.util.Date;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbAttribute;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbAutoGenerateStrategy;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbAutoGeneratedTimestamp;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbFlattened;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbVersionAttribute;

@DynamoDbFlattened(attributes = {@DynamoDbAttribute(mappedBy = "createdDate", attributeName = "RCD"),
                                 @DynamoDbAttribute(mappedBy = "lastModifiedDate", attributeName = "RMD"),
                                 @DynamoDbAttribute(mappedBy = "versionNumber", attributeName = "RVN")})
public class AuditRecord {

    private Date createdDate;
    private Date lastModifiedDate;
    private Long versionNumber;

    @DynamoDbAutoGeneratedTimestamp(strategy = DynamoDbAutoGenerateStrategy.CREATE)
    public Date getCreatedDate() {
        return this.createdDate;
    }

    public void setCreatedDate(final Date createdDate) {
        this.createdDate = createdDate;
    }

    @DynamoDbAutoGeneratedTimestamp(strategy = DynamoDbAutoGenerateStrategy.ALWAYS)
    public Date getLastModifiedDate() {
        return this.lastModifiedDate;
    }

    public void setLastModifiedDate(final Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    @DynamoDbVersionAttribute
    public Long getVersionNumber() {
        return this.versionNumber;
    }

    public void setVersionNumber(final Long versionNumber) {
        this.versionNumber = versionNumber;
    }

    @Override
    public final boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AuditRecord)) {
            return false;
        }
        AuditRecord that = (AuditRecord) o;
        return (createdDate == null ? that.createdDate == null : createdDate.equals(that.createdDate)) &&
               (lastModifiedDate == null ? that.lastModifiedDate == null : lastModifiedDate.equals(that.lastModifiedDate)) &&
               (versionNumber == null ? that.versionNumber == null : versionNumber.equals(that.versionNumber));
    }

    @Override
    public final int hashCode() {
        return 1 + (createdDate == null ? 0 : createdDate.hashCode()) +
               (lastModifiedDate == null ? 0 : lastModifiedDate.hashCode()) +
               (versionNumber == null ? 0 : versionNumber.hashCode());
    }

    @Override
    public final String toString() {
        return getClass().getName() + "{" +
               "createdDate=" + createdDate + "," +
               "lastModifiedDate=" + lastModifiedDate + "," +
               "versionNumber=" + versionNumber +
               "}";
    }

}
