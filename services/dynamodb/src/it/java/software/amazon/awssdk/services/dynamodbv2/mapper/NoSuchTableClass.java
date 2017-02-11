package software.amazon.awssdk.services.dynamodbv2.mapper;

import software.amazon.awssdk.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import software.amazon.awssdk.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "tableNotExist")
public class NoSuchTableClass {

    private String key;

    @DynamoDBHashKey
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

}
