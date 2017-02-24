/*
 * Copyright 2012-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.UUID;

import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.auth.AWSCredentials;
import software.amazon.awssdk.auth.BasicAWSCredentials;
import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * This sample demonstrates how to use the encryption support for Amazon S3
 * in the AWS SDK for Java.
 * <p>
 * The Amazon S3 encryption client requires an extra parameter during construction
 * to specify the encryption materials used to encrypt/decrypt object data.  After
 * that, you can use it just like the regular Amazon S3 client, and your object
 * data will be automatically encrypted/decrypted as it's streamed to/from Amazon S3.
 * <p>
 * <b>Prerequisites:</b> You must have a valid Amazon Web Services developer
 * account, and be signed up to use Amazon S3. For more information on
 * Amazon S3, see http://aws.amazon.com/s3.
 * <p>
 * <b>Important:</b> Be sure to fill in your AWS access credentials in the code below
 *                   before you try to run this sample.
 *                   http://aws.amazon.com/security-credentials
 * <p>
 * To run this file, make sure the AWS SDK for Java (and its dependencies from the
 * third-party directory in the SDK) are on your classpath.
 */
public class S3EncryptionSample {

    public static void main(String[] args) throws Exception {
        /*
         * Important: Be sure to fill in your AWS access credentials here
         *            before you try to run this sample.
         *            http://aws.amazon.com/security-credentials
         */
    	AWSCredentials credentials = new BasicAWSCredentials("", "");
    	EncryptionMaterials encryptionMaterials = new EncryptionMaterials(generateAsymmetricKeyPair());
		AmazonS3 s3 = new AmazonS3EncryptionClient(credentials, encryptionMaterials);

        String bucketName = "my-encrypted-s3-bucket-" + UUID.randomUUID();
        String key = "MyObjectKey";

        System.out.println("===========================================");
        System.out.println("Amazon S3 Encryption Sample");
        System.out.println("===========================================\n");

        try {
            /*
             * Create a new S3 bucket - Amazon S3 bucket names are globally unique,
             * so once a bucket name has been taken by any user, you can't create
             * another bucket with that same name.
             *
             * There isn't any data being transferred here, so there's no encryption
             * or decryption being performed.
             */
            if (s3.doesBucketExist(bucketName) == false) {
            	System.out.println("Creating bucket " + bucketName + "\n");
            	s3.createBucket(bucketName);
            }

            /*
             * When we use the putObject method, the data in the file or InputStream
             * we specify is encrypted on the fly as it's uploaded to Amazon S3.
             *
             * You can test this by going to Amazon S3 tab in the AWS Management Console
             * (https://console.aws.amazon.com/s3) and verifying that the content
             * of the file has been encrypted.
             */
        	System.out.println("Uploading and encrypting data to Amazon S3\n");
			s3.putObject(bucketName, key, createSampleFile());

			/*
			 * When you use the getObject method, the data retrieved from Amazon S3
			 * is automatically decrypted on the fly.
			 */
        	System.out.println("Downloading and decrypting data from Amazon S3\n");
			S3Object downloadedObject = s3.getObject(bucketName, key);

        	System.out.println("Decrypted data:");
			displayTextInputStream(downloadedObject.getObjectContent());

        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon S3, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with S3, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
    }

    /**
     * Generates a sample asymmetric key pair for use in encrypting and decrypting.
     * <p>
     * For real applications, you'll want to save the key pair somewhere so
     * you can share it.
     * <p>
     * Several good online sources also explain how to create an RSA key pair
     * from the command line using OpenSSL, for example:
     * http://en.wikibooks.org/wiki/Transwiki:Generate_a_keypair_using_OpenSSL
     */
    private static KeyPair generateAsymmetricKeyPair() throws Exception {
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
        keyGenerator.initialize(1024, new SecureRandom());
        return keyGenerator.generateKeyPair();
    }

    /**
     * Creates a temporary file with text data to demonstrate uploading a file
     * to Amazon S3
     *
     * @return A newly created temporary file with text data.
     */
    private static File createSampleFile() throws IOException {
        File file = File.createTempFile("aws-java-sdk-", ".txt");
        file.deleteOnExit();

        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
        writer.write("abcdefghijklmnopqrstuvwxyz\n");
        writer.write("01234567890112345678901234\n");
        writer.write("!@#$%^&*()-=[]{};':',.<>/?\n");
        writer.write("01234567890112345678901234\n");
        writer.write("abcdefghijklmnopqrstuvwxyz\n");
        writer.close();
        return file;
    }

    /**
     * Displays the contents of the specified input stream as text.
     *
     * @param input The input stream to display as text.
     */
    private static void displayTextInputStream(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        while (true) {
            String line = reader.readLine();
            if (line == null) break;
            System.out.println("    " + line);
        }
        System.out.println();
    }
}
