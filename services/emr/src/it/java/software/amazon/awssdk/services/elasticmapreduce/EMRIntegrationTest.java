/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.elasticmapreduce;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.services.elasticmapreduce.model.ActionOnFailure;
import software.amazon.awssdk.services.elasticmapreduce.model.AddTagsRequest;
import software.amazon.awssdk.services.elasticmapreduce.model.Cluster;
import software.amazon.awssdk.services.elasticmapreduce.model.ClusterSummary;
import software.amazon.awssdk.services.elasticmapreduce.model.DescribeClusterRequest;
import software.amazon.awssdk.services.elasticmapreduce.model.JobFlowInstancesConfig;
import software.amazon.awssdk.services.elasticmapreduce.model.ListClustersRequest;
import software.amazon.awssdk.services.elasticmapreduce.model.RemoveTagsRequest;
import software.amazon.awssdk.services.elasticmapreduce.model.RunJobFlowRequest;
import software.amazon.awssdk.services.elasticmapreduce.model.StepConfig;
import software.amazon.awssdk.services.elasticmapreduce.model.Tag;
import software.amazon.awssdk.services.elasticmapreduce.model.TerminateJobFlowsRequest;
import software.amazon.awssdk.services.elasticmapreduce.util.StepFactory;


/** Integration test for basic service operations. */
public class EMRIntegrationTest extends IntegrationTestBase {

    private String jobFlowId;

    /**
     * Cleans up any created tests resources.
     */
    @After
    public void tearDown() throws Exception {
        try {
            if (jobFlowId != null) {
                emr.terminateJobFlows(new TerminateJobFlowsRequest().withJobFlowIds(jobFlowId));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // See https://forums.aws.amazon.com/thread.jspa?threadID=158756
    @Test
    public void testListCluster() {
        emr.listClusters(
                new ListClustersRequest()
                        .withCreatedAfter(
                                new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)));
    }

    @Test
    public void testServiceOperation() throws Exception {
        jobFlowId = runTestJobFlow();

        emr.addTags(new AddTagsRequest(jobFlowId).withTags(new Tag("foo", "bar")));
        assertTrue(doesTagExist(jobFlowId, "foo", "bar"));

        emr.removeTags(new RemoveTagsRequest(jobFlowId).withTagKeys("foo"));
        assertFalse(doesTagExist(jobFlowId, "foo", "bar"));

        for (ClusterSummary cluster : emr.listClusters().getClusters()) {
            assertNotNull(cluster.getId());
            assertNotNull(cluster.getName());
            assertNotNull(cluster.getStatus());
        }
    }

    private boolean doesTagExist(String jobFlowId, String tagKey, String tagValue) {
        Cluster cluster = emr.describeCluster(new DescribeClusterRequest().withClusterId(jobFlowId)).getCluster();

        for (Tag tag : cluster.getTags()) {
            if (tag.getKey().equals(tagKey) &&
                tag.getValue().equals(tagValue)) {
                return true;
            }
        }

        return false;
    }

    private String runTestJobFlow() {
        StepFactory stepFactory = new StepFactory();

        StepConfig enabledebugging = new StepConfig()
                .withName("Enable debugging")
                .withActionOnFailure(ActionOnFailure.TERMINATE_JOB_FLOW)
                .withHadoopJarStep(stepFactory.newEnableDebuggingStep());

        StepConfig installHive = new StepConfig()
                .withName("Install Hive")
                .withActionOnFailure(ActionOnFailure.TERMINATE_JOB_FLOW)
                .withHadoopJarStep(stepFactory.newInstallHiveStep());

        RunJobFlowRequest request = new RunJobFlowRequest()
                .withName("Hive Interactive")
                .withAmiVersion("3.8.0")
                .withSteps(enabledebugging, installHive)
                .withServiceRole("EMR_DefaultRole")
                .withJobFlowRole("EMR_EC2_DefaultRole")
                .withInstances(new JobFlowInstancesConfig()
                                       .withHadoopVersion("2.4.0")
                                       .withInstanceCount(2)
                                       .withKeepJobFlowAliveWhenNoSteps(false)
                                       .withMasterInstanceType("m1.medium")
                                       .withSlaveInstanceType("m1.medium"));

        return emr.runJobFlow(request).getJobFlowId();
    }
}