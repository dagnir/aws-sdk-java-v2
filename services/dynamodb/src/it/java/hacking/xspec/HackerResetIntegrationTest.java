package hacking.xspec;


import org.junit.Test;

import software.amazon.awssdk.services.dynamodbv2.document.Item;
import software.amazon.awssdk.services.dynamodbv2.document.Table;
import software.amazon.awssdk.services.dynamodbv2.document.utils.ValueMap;

/**
 * Used to create or reset the hacker table.
 *
 */
public class HackerResetIntegrationTest extends HackerIntegrationTestBase {
    @Test
    public void resetHacker1_TwoItems() {
        Table table = dynamo.getTable(HACKER_TABLE_NAME);
        table.putItem(new Item()
            .withPrimaryKey(HASH_KEY_HACKER_ID, hacker_uuid_1, RANGE_KEY_START_DATE, 20140201)
            .withMap("photo", 
                new ValueMap()
                .withString("image_file_name", "hacker1.png")
                .withBinary("image_file", new byte[]{1,2,3}))
            .withBoolean("is_active", false)
            .withList("favorite_colors", "red", "green", "blue")
            .withStringSet("email_addresses", "hacker1@amazon.com", "hacker1@lab126.com")
            .withNumberSet("favoriate_numbers", 3.1415, 2.1828, 0.5772)
            .withBinarySet("personal_blobs", new byte[]{0,0,0}, new byte[]{1,1,1})
            .withNumber("lines_of_code", 1000)
            .withNumber("end_yyyymmdd", 20140301)
        );
        table.putItem(new Item()
            .withPrimaryKey(HASH_KEY_HACKER_ID, hacker_uuid_1, RANGE_KEY_START_DATE, 20140501)
            .withMap("photo", 
                new ValueMap()
                .withString("image_file_name", "hacker1.png")
                .withBinary("image_file", new byte[]{1,2,3}))
            .withBoolean("is_active", true)
            .withList("favorite_colors", "red", "green", "blue")
            .withStringSet("email_addresses", "hacker1@amazon.com", "hacker1@lab126.com")
            .withNumberSet("favoriate_numbers", 3.1415, 2.1828, 0.5772)
            .withBinarySet("personal_blobs", new byte[]{0,0,0}, new byte[]{1,1,1})
            .withNumber("lines_of_code", 1000)
            .withNull("end_yyyymmdd")
        );
        Item item = table.getItem(HASH_KEY_HACKER_ID, hacker_uuid_1, RANGE_KEY_START_DATE, 20140201);
        System.out.println(item.toJSONPretty());
        /*
            {
              "personal_blobs" : [ "AAAA", "AQEB" ],
              "start_yyyymmdd" : 20140201,
              "end_yyyymmdd" : 20140301,
              "is_active" : false,
              "photo" : {
                "image_file" : "AQID",
                "image_file_name" : "hacker1.png"
              },
              "lines_of_code" : 1000,
              "favorite_colors" : [ "red", "green", "blue" ],
              "favoriate_numbers" : [ 2.1828, 3.1415, 0.5772 ],
              "hacker_id" : "067e6162-3b6f-4ae2-a171-2470b63dff00",
              "email_addresses" : [ "hacker1@amazon.com", "hacker1@lab126.com" ]
            }
        */
        item = table.getItem(HASH_KEY_HACKER_ID, hacker_uuid_1, RANGE_KEY_START_DATE, 20140501);
        System.out.println(item.toJSONPretty());
        /*
            {
              "personal_blobs" : [ "AAAA", "AQEB" ],
              "start_yyyymmdd" : 20140501,
              "end_yyyymmdd" : null,
              "is_active" : true,
              "photo" : {
                "image_file" : "AQID",
                "image_file_name" : "hacker1.png"
              },
              "lines_of_code" : 1000,
              "favorite_colors" : [ "red", "green", "blue" ],
              "favoriate_numbers" : [ 2.1828, 3.1415, 0.5772 ],
              "hacker_id" : "067e6162-3b6f-4ae2-a171-2470b63dff00",
              "email_addresses" : [ "hacker1@amazon.com", "hacker1@lab126.com" ]
            }
         */
    }

    @Test
    public void resetHacker2_OneItem() {
        Table table = dynamo.getTable(HACKER_TABLE_NAME);
        table.putItem(new Item()
            .withPrimaryKey(HASH_KEY_HACKER_ID, hacker_uuid_2, RANGE_KEY_START_DATE, 20130101)
            .withMap("photo", 
                new ValueMap()
                .withString("image_file_name", "hacker2.png")
                .withBinary("image_file", new byte[]{0,0,0}))
            .withBoolean("is_active", true)
            .withList("favorite_colors", "black", "white")
            .withStringSet("email_addresses", "hacker2@amazon.com", "hacker2@lab126.com")
            .withNumberSet("favoriate_numbers", 2, 3, 5)
            .withBinarySet("personal_blobs", new byte[]{2,2,2}, new byte[]{3,3,3})
            .withNumber("lines_of_code", 1000)
            .withNull("end_yyyymmdd")
        );
        Item item = table.getItem(HASH_KEY_HACKER_ID, hacker_uuid_2, RANGE_KEY_START_DATE, 20130101);
        System.out.println(item.toJSONPretty());
        /*
            {
              "personal_blobs" : [ "AgIC", "AwMD" ],
              "start_yyyymmdd" : 20130101,
              "end_yyyymmdd" : null,
              "is_active" : true,
              "photo" : {
                "image_file" : "AAAA",
                "image_file_name" : "hacker2.png"
              },
              "lines_of_code" : 1000,
              "favorite_colors" : [ "black", "white" ],
              "favoriate_numbers" : [ 3, 2, 5 ],
              "hacker_id" : "067e6162-0e9e-4471-a2f9-9af509fb5889",
              "email_addresses" : [ "hacker2@amazon.com", "hacker2@lab126.com" ]
            }
         */
    }
}
