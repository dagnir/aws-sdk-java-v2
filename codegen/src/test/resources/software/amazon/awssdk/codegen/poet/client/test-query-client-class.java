package software.amazon.awssdk.services.iam;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import org.w3c.dom.Node;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.AmazonWebServiceResponse;
import software.amazon.awssdk.client.AwsSyncClientParams;
import software.amazon.awssdk.client.ClientExecutionParams;
import software.amazon.awssdk.client.ClientHandler;
import software.amazon.awssdk.client.ClientHandlerParams;
import software.amazon.awssdk.client.SdkClientHandler;
import software.amazon.awssdk.http.DefaultErrorResponseHandler;
import software.amazon.awssdk.http.StaxResponseHandler;
import software.amazon.awssdk.runtime.transform.StandardErrorUnmarshaller;
import software.amazon.awssdk.runtime.transform.Unmarshaller;
import software.amazon.awssdk.services.iam.model.AddClientIDToOpenIDConnectProviderRequest;
import software.amazon.awssdk.services.iam.model.AddClientIDToOpenIDConnectProviderResult;
import software.amazon.awssdk.services.iam.model.AddRoleToInstanceProfileRequest;
import software.amazon.awssdk.services.iam.model.AddRoleToInstanceProfileResult;
import software.amazon.awssdk.services.iam.model.AddUserToGroupRequest;
import software.amazon.awssdk.services.iam.model.AddUserToGroupResult;
import software.amazon.awssdk.services.iam.model.AttachGroupPolicyRequest;
import software.amazon.awssdk.services.iam.model.AttachGroupPolicyResult;
import software.amazon.awssdk.services.iam.model.AttachRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.AttachRolePolicyResult;
import software.amazon.awssdk.services.iam.model.AttachUserPolicyRequest;
import software.amazon.awssdk.services.iam.model.AttachUserPolicyResult;
import software.amazon.awssdk.services.iam.model.ChangePasswordRequest;
import software.amazon.awssdk.services.iam.model.ChangePasswordResult;
import software.amazon.awssdk.services.iam.model.CreateAccessKeyRequest;
import software.amazon.awssdk.services.iam.model.CreateAccessKeyResult;
import software.amazon.awssdk.services.iam.model.CreateAccountAliasRequest;
import software.amazon.awssdk.services.iam.model.CreateAccountAliasResult;
import software.amazon.awssdk.services.iam.model.CreateGroupRequest;
import software.amazon.awssdk.services.iam.model.CreateGroupResult;
import software.amazon.awssdk.services.iam.model.CreateInstanceProfileRequest;
import software.amazon.awssdk.services.iam.model.CreateInstanceProfileResult;
import software.amazon.awssdk.services.iam.model.CreateLoginProfileRequest;
import software.amazon.awssdk.services.iam.model.CreateLoginProfileResult;
import software.amazon.awssdk.services.iam.model.CreateOpenIDConnectProviderRequest;
import software.amazon.awssdk.services.iam.model.CreateOpenIDConnectProviderResult;
import software.amazon.awssdk.services.iam.model.CreatePolicyRequest;
import software.amazon.awssdk.services.iam.model.CreatePolicyResult;
import software.amazon.awssdk.services.iam.model.CreatePolicyVersionRequest;
import software.amazon.awssdk.services.iam.model.CreatePolicyVersionResult;
import software.amazon.awssdk.services.iam.model.CreateRoleRequest;
import software.amazon.awssdk.services.iam.model.CreateRoleResult;
import software.amazon.awssdk.services.iam.model.CreateSAMLProviderRequest;
import software.amazon.awssdk.services.iam.model.CreateSAMLProviderResult;
import software.amazon.awssdk.services.iam.model.CreateServiceSpecificCredentialRequest;
import software.amazon.awssdk.services.iam.model.CreateServiceSpecificCredentialResult;
import software.amazon.awssdk.services.iam.model.CreateUserRequest;
import software.amazon.awssdk.services.iam.model.CreateUserResult;
import software.amazon.awssdk.services.iam.model.CreateVirtualMFADeviceRequest;
import software.amazon.awssdk.services.iam.model.CreateVirtualMFADeviceResult;
import software.amazon.awssdk.services.iam.model.DeactivateMFADeviceRequest;
import software.amazon.awssdk.services.iam.model.DeactivateMFADeviceResult;
import software.amazon.awssdk.services.iam.model.DeleteAccessKeyRequest;
import software.amazon.awssdk.services.iam.model.DeleteAccessKeyResult;
import software.amazon.awssdk.services.iam.model.DeleteAccountAliasRequest;
import software.amazon.awssdk.services.iam.model.DeleteAccountAliasResult;
import software.amazon.awssdk.services.iam.model.DeleteAccountPasswordPolicyRequest;
import software.amazon.awssdk.services.iam.model.DeleteAccountPasswordPolicyResult;
import software.amazon.awssdk.services.iam.model.DeleteGroupPolicyRequest;
import software.amazon.awssdk.services.iam.model.DeleteGroupPolicyResult;
import software.amazon.awssdk.services.iam.model.DeleteGroupRequest;
import software.amazon.awssdk.services.iam.model.DeleteGroupResult;
import software.amazon.awssdk.services.iam.model.DeleteInstanceProfileRequest;
import software.amazon.awssdk.services.iam.model.DeleteInstanceProfileResult;
import software.amazon.awssdk.services.iam.model.DeleteLoginProfileRequest;
import software.amazon.awssdk.services.iam.model.DeleteLoginProfileResult;
import software.amazon.awssdk.services.iam.model.DeleteOpenIDConnectProviderRequest;
import software.amazon.awssdk.services.iam.model.DeleteOpenIDConnectProviderResult;
import software.amazon.awssdk.services.iam.model.DeletePolicyRequest;
import software.amazon.awssdk.services.iam.model.DeletePolicyResult;
import software.amazon.awssdk.services.iam.model.DeletePolicyVersionRequest;
import software.amazon.awssdk.services.iam.model.DeletePolicyVersionResult;
import software.amazon.awssdk.services.iam.model.DeleteRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.DeleteRolePolicyResult;
import software.amazon.awssdk.services.iam.model.DeleteRoleRequest;
import software.amazon.awssdk.services.iam.model.DeleteRoleResult;
import software.amazon.awssdk.services.iam.model.DeleteSAMLProviderRequest;
import software.amazon.awssdk.services.iam.model.DeleteSAMLProviderResult;
import software.amazon.awssdk.services.iam.model.DeleteSSHPublicKeyRequest;
import software.amazon.awssdk.services.iam.model.DeleteSSHPublicKeyResult;
import software.amazon.awssdk.services.iam.model.DeleteServerCertificateRequest;
import software.amazon.awssdk.services.iam.model.DeleteServerCertificateResult;
import software.amazon.awssdk.services.iam.model.DeleteServiceSpecificCredentialRequest;
import software.amazon.awssdk.services.iam.model.DeleteServiceSpecificCredentialResult;
import software.amazon.awssdk.services.iam.model.DeleteSigningCertificateRequest;
import software.amazon.awssdk.services.iam.model.DeleteSigningCertificateResult;
import software.amazon.awssdk.services.iam.model.DeleteUserPolicyRequest;
import software.amazon.awssdk.services.iam.model.DeleteUserPolicyResult;
import software.amazon.awssdk.services.iam.model.DeleteUserRequest;
import software.amazon.awssdk.services.iam.model.DeleteUserResult;
import software.amazon.awssdk.services.iam.model.DeleteVirtualMFADeviceRequest;
import software.amazon.awssdk.services.iam.model.DeleteVirtualMFADeviceResult;
import software.amazon.awssdk.services.iam.model.DetachGroupPolicyRequest;
import software.amazon.awssdk.services.iam.model.DetachGroupPolicyResult;
import software.amazon.awssdk.services.iam.model.DetachRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.DetachRolePolicyResult;
import software.amazon.awssdk.services.iam.model.DetachUserPolicyRequest;
import software.amazon.awssdk.services.iam.model.DetachUserPolicyResult;
import software.amazon.awssdk.services.iam.model.EnableMFADeviceRequest;
import software.amazon.awssdk.services.iam.model.EnableMFADeviceResult;
import software.amazon.awssdk.services.iam.model.GenerateCredentialReportRequest;
import software.amazon.awssdk.services.iam.model.GenerateCredentialReportResult;
import software.amazon.awssdk.services.iam.model.GetAccessKeyLastUsedRequest;
import software.amazon.awssdk.services.iam.model.GetAccessKeyLastUsedResult;
import software.amazon.awssdk.services.iam.model.GetAccountAuthorizationDetailsRequest;
import software.amazon.awssdk.services.iam.model.GetAccountAuthorizationDetailsResult;
import software.amazon.awssdk.services.iam.model.GetAccountPasswordPolicyRequest;
import software.amazon.awssdk.services.iam.model.GetAccountPasswordPolicyResult;
import software.amazon.awssdk.services.iam.model.GetAccountSummaryRequest;
import software.amazon.awssdk.services.iam.model.GetAccountSummaryResult;
import software.amazon.awssdk.services.iam.model.GetContextKeysForCustomPolicyRequest;
import software.amazon.awssdk.services.iam.model.GetContextKeysForCustomPolicyResult;
import software.amazon.awssdk.services.iam.model.GetContextKeysForPrincipalPolicyRequest;
import software.amazon.awssdk.services.iam.model.GetContextKeysForPrincipalPolicyResult;
import software.amazon.awssdk.services.iam.model.GetCredentialReportRequest;
import software.amazon.awssdk.services.iam.model.GetCredentialReportResult;
import software.amazon.awssdk.services.iam.model.GetGroupPolicyRequest;
import software.amazon.awssdk.services.iam.model.GetGroupPolicyResult;
import software.amazon.awssdk.services.iam.model.GetGroupRequest;
import software.amazon.awssdk.services.iam.model.GetGroupResult;
import software.amazon.awssdk.services.iam.model.GetInstanceProfileRequest;
import software.amazon.awssdk.services.iam.model.GetInstanceProfileResult;
import software.amazon.awssdk.services.iam.model.GetLoginProfileRequest;
import software.amazon.awssdk.services.iam.model.GetLoginProfileResult;
import software.amazon.awssdk.services.iam.model.GetOpenIDConnectProviderRequest;
import software.amazon.awssdk.services.iam.model.GetOpenIDConnectProviderResult;
import software.amazon.awssdk.services.iam.model.GetPolicyRequest;
import software.amazon.awssdk.services.iam.model.GetPolicyResult;
import software.amazon.awssdk.services.iam.model.GetPolicyVersionRequest;
import software.amazon.awssdk.services.iam.model.GetPolicyVersionResult;
import software.amazon.awssdk.services.iam.model.GetRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.GetRolePolicyResult;
import software.amazon.awssdk.services.iam.model.GetRoleRequest;
import software.amazon.awssdk.services.iam.model.GetRoleResult;
import software.amazon.awssdk.services.iam.model.GetSAMLProviderRequest;
import software.amazon.awssdk.services.iam.model.GetSAMLProviderResult;
import software.amazon.awssdk.services.iam.model.GetSSHPublicKeyRequest;
import software.amazon.awssdk.services.iam.model.GetSSHPublicKeyResult;
import software.amazon.awssdk.services.iam.model.GetServerCertificateRequest;
import software.amazon.awssdk.services.iam.model.GetServerCertificateResult;
import software.amazon.awssdk.services.iam.model.GetUserPolicyRequest;
import software.amazon.awssdk.services.iam.model.GetUserPolicyResult;
import software.amazon.awssdk.services.iam.model.GetUserRequest;
import software.amazon.awssdk.services.iam.model.GetUserResult;
import software.amazon.awssdk.services.iam.model.IAMClientException;
import software.amazon.awssdk.services.iam.model.ListAccessKeysRequest;
import software.amazon.awssdk.services.iam.model.ListAccessKeysResult;
import software.amazon.awssdk.services.iam.model.ListAccountAliasesRequest;
import software.amazon.awssdk.services.iam.model.ListAccountAliasesResult;
import software.amazon.awssdk.services.iam.model.ListAttachedGroupPoliciesRequest;
import software.amazon.awssdk.services.iam.model.ListAttachedGroupPoliciesResult;
import software.amazon.awssdk.services.iam.model.ListAttachedRolePoliciesRequest;
import software.amazon.awssdk.services.iam.model.ListAttachedRolePoliciesResult;
import software.amazon.awssdk.services.iam.model.ListAttachedUserPoliciesRequest;
import software.amazon.awssdk.services.iam.model.ListAttachedUserPoliciesResult;
import software.amazon.awssdk.services.iam.model.ListEntitiesForPolicyRequest;
import software.amazon.awssdk.services.iam.model.ListEntitiesForPolicyResult;
import software.amazon.awssdk.services.iam.model.ListGroupPoliciesRequest;
import software.amazon.awssdk.services.iam.model.ListGroupPoliciesResult;
import software.amazon.awssdk.services.iam.model.ListGroupsForUserRequest;
import software.amazon.awssdk.services.iam.model.ListGroupsForUserResult;
import software.amazon.awssdk.services.iam.model.ListGroupsRequest;
import software.amazon.awssdk.services.iam.model.ListGroupsResult;
import software.amazon.awssdk.services.iam.model.ListInstanceProfilesForRoleRequest;
import software.amazon.awssdk.services.iam.model.ListInstanceProfilesForRoleResult;
import software.amazon.awssdk.services.iam.model.ListInstanceProfilesRequest;
import software.amazon.awssdk.services.iam.model.ListInstanceProfilesResult;
import software.amazon.awssdk.services.iam.model.ListMFADevicesRequest;
import software.amazon.awssdk.services.iam.model.ListMFADevicesResult;
import software.amazon.awssdk.services.iam.model.ListOpenIDConnectProvidersRequest;
import software.amazon.awssdk.services.iam.model.ListOpenIDConnectProvidersResult;
import software.amazon.awssdk.services.iam.model.ListPoliciesRequest;
import software.amazon.awssdk.services.iam.model.ListPoliciesResult;
import software.amazon.awssdk.services.iam.model.ListPolicyVersionsRequest;
import software.amazon.awssdk.services.iam.model.ListPolicyVersionsResult;
import software.amazon.awssdk.services.iam.model.ListRolePoliciesRequest;
import software.amazon.awssdk.services.iam.model.ListRolePoliciesResult;
import software.amazon.awssdk.services.iam.model.ListRolesRequest;
import software.amazon.awssdk.services.iam.model.ListRolesResult;
import software.amazon.awssdk.services.iam.model.ListSAMLProvidersRequest;
import software.amazon.awssdk.services.iam.model.ListSAMLProvidersResult;
import software.amazon.awssdk.services.iam.model.ListSSHPublicKeysRequest;
import software.amazon.awssdk.services.iam.model.ListSSHPublicKeysResult;
import software.amazon.awssdk.services.iam.model.ListServerCertificatesRequest;
import software.amazon.awssdk.services.iam.model.ListServerCertificatesResult;
import software.amazon.awssdk.services.iam.model.ListServiceSpecificCredentialsRequest;
import software.amazon.awssdk.services.iam.model.ListServiceSpecificCredentialsResult;
import software.amazon.awssdk.services.iam.model.ListSigningCertificatesRequest;
import software.amazon.awssdk.services.iam.model.ListSigningCertificatesResult;
import software.amazon.awssdk.services.iam.model.ListUserPoliciesRequest;
import software.amazon.awssdk.services.iam.model.ListUserPoliciesResult;
import software.amazon.awssdk.services.iam.model.ListUsersRequest;
import software.amazon.awssdk.services.iam.model.ListUsersResult;
import software.amazon.awssdk.services.iam.model.ListVirtualMFADevicesRequest;
import software.amazon.awssdk.services.iam.model.ListVirtualMFADevicesResult;
import software.amazon.awssdk.services.iam.model.PutGroupPolicyRequest;
import software.amazon.awssdk.services.iam.model.PutGroupPolicyResult;
import software.amazon.awssdk.services.iam.model.PutRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.PutRolePolicyResult;
import software.amazon.awssdk.services.iam.model.PutUserPolicyRequest;
import software.amazon.awssdk.services.iam.model.PutUserPolicyResult;
import software.amazon.awssdk.services.iam.model.RemoveClientIDFromOpenIDConnectProviderRequest;
import software.amazon.awssdk.services.iam.model.RemoveClientIDFromOpenIDConnectProviderResult;
import software.amazon.awssdk.services.iam.model.RemoveRoleFromInstanceProfileRequest;
import software.amazon.awssdk.services.iam.model.RemoveRoleFromInstanceProfileResult;
import software.amazon.awssdk.services.iam.model.RemoveUserFromGroupRequest;
import software.amazon.awssdk.services.iam.model.RemoveUserFromGroupResult;
import software.amazon.awssdk.services.iam.model.ResetServiceSpecificCredentialRequest;
import software.amazon.awssdk.services.iam.model.ResetServiceSpecificCredentialResult;
import software.amazon.awssdk.services.iam.model.ResyncMFADeviceRequest;
import software.amazon.awssdk.services.iam.model.ResyncMFADeviceResult;
import software.amazon.awssdk.services.iam.model.SetDefaultPolicyVersionRequest;
import software.amazon.awssdk.services.iam.model.SetDefaultPolicyVersionResult;
import software.amazon.awssdk.services.iam.model.SimulateCustomPolicyRequest;
import software.amazon.awssdk.services.iam.model.SimulateCustomPolicyResult;
import software.amazon.awssdk.services.iam.model.SimulatePrincipalPolicyRequest;
import software.amazon.awssdk.services.iam.model.SimulatePrincipalPolicyResult;
import software.amazon.awssdk.services.iam.model.UpdateAccessKeyRequest;
import software.amazon.awssdk.services.iam.model.UpdateAccessKeyResult;
import software.amazon.awssdk.services.iam.model.UpdateAccountPasswordPolicyRequest;
import software.amazon.awssdk.services.iam.model.UpdateAccountPasswordPolicyResult;
import software.amazon.awssdk.services.iam.model.UpdateAssumeRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.UpdateAssumeRolePolicyResult;
import software.amazon.awssdk.services.iam.model.UpdateGroupRequest;
import software.amazon.awssdk.services.iam.model.UpdateGroupResult;
import software.amazon.awssdk.services.iam.model.UpdateLoginProfileRequest;
import software.amazon.awssdk.services.iam.model.UpdateLoginProfileResult;
import software.amazon.awssdk.services.iam.model.UpdateOpenIDConnectProviderThumbprintRequest;
import software.amazon.awssdk.services.iam.model.UpdateOpenIDConnectProviderThumbprintResult;
import software.amazon.awssdk.services.iam.model.UpdateSAMLProviderRequest;
import software.amazon.awssdk.services.iam.model.UpdateSAMLProviderResult;
import software.amazon.awssdk.services.iam.model.UpdateSSHPublicKeyRequest;
import software.amazon.awssdk.services.iam.model.UpdateSSHPublicKeyResult;
import software.amazon.awssdk.services.iam.model.UpdateServerCertificateRequest;
import software.amazon.awssdk.services.iam.model.UpdateServerCertificateResult;
import software.amazon.awssdk.services.iam.model.UpdateServiceSpecificCredentialRequest;
import software.amazon.awssdk.services.iam.model.UpdateServiceSpecificCredentialResult;
import software.amazon.awssdk.services.iam.model.UpdateSigningCertificateRequest;
import software.amazon.awssdk.services.iam.model.UpdateSigningCertificateResult;
import software.amazon.awssdk.services.iam.model.UpdateUserRequest;
import software.amazon.awssdk.services.iam.model.UpdateUserResult;
import software.amazon.awssdk.services.iam.model.UploadSSHPublicKeyRequest;
import software.amazon.awssdk.services.iam.model.UploadSSHPublicKeyResult;
import software.amazon.awssdk.services.iam.model.UploadServerCertificateRequest;
import software.amazon.awssdk.services.iam.model.UploadServerCertificateResult;
import software.amazon.awssdk.services.iam.model.UploadSigningCertificateRequest;
import software.amazon.awssdk.services.iam.model.UploadSigningCertificateResult;
import software.amazon.awssdk.services.iam.model.transform.AddClientIDToOpenIDConnectProviderRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.AddClientIDToOpenIDConnectProviderResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.AddRoleToInstanceProfileRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.AddRoleToInstanceProfileResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.AddUserToGroupRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.AddUserToGroupResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.AttachGroupPolicyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.AttachGroupPolicyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.AttachRolePolicyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.AttachRolePolicyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.AttachUserPolicyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.AttachUserPolicyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ChangePasswordRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.ChangePasswordResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.CreateAccessKeyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.CreateAccessKeyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.CreateAccountAliasRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.CreateAccountAliasResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.CreateGroupRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.CreateGroupResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.CreateInstanceProfileRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.CreateInstanceProfileResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.CreateLoginProfileRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.CreateLoginProfileResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.CreateOpenIDConnectProviderRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.CreateOpenIDConnectProviderResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.CreatePolicyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.CreatePolicyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.CreatePolicyVersionRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.CreatePolicyVersionResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.CreateRoleRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.CreateRoleResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.CreateSAMLProviderRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.CreateSAMLProviderResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.CreateServiceSpecificCredentialRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.CreateServiceSpecificCredentialResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.CreateUserRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.CreateUserResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.CreateVirtualMFADeviceRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.CreateVirtualMFADeviceResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.CredentialReportExpiredExceptionUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.CredentialReportNotPresentExceptionUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.CredentialReportNotReadyExceptionUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeactivateMFADeviceRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeactivateMFADeviceResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteAccessKeyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteAccessKeyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteAccountAliasRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteAccountAliasResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteAccountPasswordPolicyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteAccountPasswordPolicyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteConflictExceptionUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteGroupPolicyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteGroupPolicyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteGroupRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteGroupResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteInstanceProfileRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteInstanceProfileResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteLoginProfileRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteLoginProfileResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteOpenIDConnectProviderRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteOpenIDConnectProviderResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeletePolicyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeletePolicyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeletePolicyVersionRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeletePolicyVersionResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteRolePolicyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteRolePolicyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteRoleRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteRoleResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteSAMLProviderRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteSAMLProviderResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteSSHPublicKeyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteSSHPublicKeyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteServerCertificateRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteServerCertificateResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteServiceSpecificCredentialRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteServiceSpecificCredentialResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteSigningCertificateRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteSigningCertificateResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteUserPolicyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteUserPolicyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteUserRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteUserResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteVirtualMFADeviceRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.DeleteVirtualMFADeviceResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.DetachGroupPolicyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.DetachGroupPolicyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.DetachRolePolicyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.DetachRolePolicyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.DetachUserPolicyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.DetachUserPolicyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.DuplicateCertificateExceptionUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.DuplicateSSHPublicKeyExceptionUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.EnableMFADeviceRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.EnableMFADeviceResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.EntityAlreadyExistsExceptionUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.EntityTemporarilyUnmodifiableExceptionUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.GenerateCredentialReportRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.GenerateCredentialReportResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetAccessKeyLastUsedRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetAccessKeyLastUsedResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetAccountAuthorizationDetailsRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetAccountAuthorizationDetailsResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetAccountPasswordPolicyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetAccountPasswordPolicyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetAccountSummaryRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetAccountSummaryResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetContextKeysForCustomPolicyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetContextKeysForCustomPolicyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetContextKeysForPrincipalPolicyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetContextKeysForPrincipalPolicyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetCredentialReportRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetCredentialReportResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetGroupPolicyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetGroupPolicyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetGroupRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetGroupResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetInstanceProfileRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetInstanceProfileResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetLoginProfileRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetLoginProfileResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetOpenIDConnectProviderRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetOpenIDConnectProviderResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetPolicyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetPolicyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetPolicyVersionRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetPolicyVersionResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetRolePolicyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetRolePolicyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetRoleRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetRoleResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetSAMLProviderRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetSAMLProviderResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetSSHPublicKeyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetSSHPublicKeyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetServerCertificateRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetServerCertificateResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetUserPolicyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetUserPolicyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetUserRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.GetUserResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.InvalidAuthenticationCodeExceptionUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.InvalidCertificateExceptionUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.InvalidInputExceptionUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.InvalidPublicKeyExceptionUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.InvalidUserTypeExceptionUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.KeyPairMismatchExceptionUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.LimitExceededExceptionUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListAccessKeysRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListAccessKeysResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListAccountAliasesRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListAccountAliasesResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListAttachedGroupPoliciesRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListAttachedGroupPoliciesResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListAttachedRolePoliciesRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListAttachedRolePoliciesResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListAttachedUserPoliciesRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListAttachedUserPoliciesResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListEntitiesForPolicyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListEntitiesForPolicyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListGroupPoliciesRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListGroupPoliciesResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListGroupsForUserRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListGroupsForUserResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListGroupsRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListGroupsResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListInstanceProfilesForRoleRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListInstanceProfilesForRoleResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListInstanceProfilesRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListInstanceProfilesResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListMFADevicesRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListMFADevicesResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListOpenIDConnectProvidersRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListOpenIDConnectProvidersResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListPoliciesRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListPoliciesResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListPolicyVersionsRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListPolicyVersionsResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListRolePoliciesRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListRolePoliciesResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListRolesRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListRolesResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListSAMLProvidersRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListSAMLProvidersResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListSSHPublicKeysRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListSSHPublicKeysResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListServerCertificatesRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListServerCertificatesResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListServiceSpecificCredentialsRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListServiceSpecificCredentialsResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListSigningCertificatesRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListSigningCertificatesResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListUserPoliciesRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListUserPoliciesResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListUsersRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListUsersResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListVirtualMFADevicesRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.ListVirtualMFADevicesResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.MalformedCertificateExceptionUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.MalformedPolicyDocumentExceptionUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.NoSuchEntityExceptionUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.PasswordPolicyViolationExceptionUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.PolicyEvaluationExceptionUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.PutGroupPolicyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.PutGroupPolicyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.PutRolePolicyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.PutRolePolicyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.PutUserPolicyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.PutUserPolicyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.RemoveClientIDFromOpenIDConnectProviderRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.RemoveClientIDFromOpenIDConnectProviderResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.RemoveRoleFromInstanceProfileRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.RemoveRoleFromInstanceProfileResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.RemoveUserFromGroupRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.RemoveUserFromGroupResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ResetServiceSpecificCredentialRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.ResetServiceSpecificCredentialResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ResyncMFADeviceRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.ResyncMFADeviceResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ServiceFailureExceptionUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.ServiceNotSupportedExceptionUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.SetDefaultPolicyVersionRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.SetDefaultPolicyVersionResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.SimulateCustomPolicyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.SimulateCustomPolicyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.SimulatePrincipalPolicyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.SimulatePrincipalPolicyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.UnrecognizedPublicKeyEncodingExceptionUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.UpdateAccessKeyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.UpdateAccessKeyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.UpdateAccountPasswordPolicyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.UpdateAccountPasswordPolicyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.UpdateAssumeRolePolicyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.UpdateAssumeRolePolicyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.UpdateGroupRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.UpdateGroupResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.UpdateLoginProfileRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.UpdateLoginProfileResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.UpdateOpenIDConnectProviderThumbprintRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.UpdateOpenIDConnectProviderThumbprintResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.UpdateSAMLProviderRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.UpdateSAMLProviderResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.UpdateSSHPublicKeyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.UpdateSSHPublicKeyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.UpdateServerCertificateRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.UpdateServerCertificateResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.UpdateServiceSpecificCredentialRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.UpdateServiceSpecificCredentialResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.UpdateSigningCertificateRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.UpdateSigningCertificateResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.UpdateUserRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.UpdateUserResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.UploadSSHPublicKeyRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.UploadSSHPublicKeyResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.UploadServerCertificateRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.UploadServerCertificateResultUnmarshaller;
import software.amazon.awssdk.services.iam.model.transform.UploadSigningCertificateRequestMarshaller;
import software.amazon.awssdk.services.iam.model.transform.UploadSigningCertificateResultUnmarshaller;
import software.amazon.awssdk.services.iam.waiters.IAMClientWaiters;

@Generated("software.amazon.awssdk:aws-java-sdk-code-generator")
public class DefaultIAMClient implements IAMClient {
    private final ClientHandler clientHandler;

    private List<Unmarshaller<AmazonServiceException, Node>> exceptionUnmarshallers = new ArrayList<Unmarshaller<AmazonServiceException, Node>>();

    private final AwsSyncClientParams clientParams;

    private volatile IAMClientWaiters waiters;

    protected DefaultIAMClient(AwsSyncClientParams clientParams) {
        this.clientHandler = new SdkClientHandler(new ClientHandlerParams().withClientParams(clientParams)
                .withCalculateCrc32FromCompressedDataEnabled(false));
        this.clientParams = clientParams;
        this.exceptionUnmarshallers = init();
    }

    @Override
    public AddClientIDToOpenIDConnectProviderResult addClientIDToOpenIDConnectProvider(
            AddClientIDToOpenIDConnectProviderRequest addClientIDToOpenIDConnectProviderRequest) {

        StaxResponseHandler<AddClientIDToOpenIDConnectProviderResult> responseHandler = new StaxResponseHandler<AddClientIDToOpenIDConnectProviderResult>(
                new AddClientIDToOpenIDConnectProviderResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<AddClientIDToOpenIDConnectProviderRequest, AmazonWebServiceResponse<AddClientIDToOpenIDConnectProviderResult>>()
                                .withMarshaller(new AddClientIDToOpenIDConnectProviderRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(addClientIDToOpenIDConnectProviderRequest)).getResult();
    }

    @Override
    public AddRoleToInstanceProfileResult addRoleToInstanceProfile(AddRoleToInstanceProfileRequest addRoleToInstanceProfileRequest) {

        StaxResponseHandler<AddRoleToInstanceProfileResult> responseHandler = new StaxResponseHandler<AddRoleToInstanceProfileResult>(
                new AddRoleToInstanceProfileResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<AddRoleToInstanceProfileRequest, AmazonWebServiceResponse<AddRoleToInstanceProfileResult>>()
                                .withMarshaller(new AddRoleToInstanceProfileRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(addRoleToInstanceProfileRequest)).getResult();
    }

    @Override
    public AddUserToGroupResult addUserToGroup(AddUserToGroupRequest addUserToGroupRequest) {

        StaxResponseHandler<AddUserToGroupResult> responseHandler = new StaxResponseHandler<AddUserToGroupResult>(
                new AddUserToGroupResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<AddUserToGroupRequest, AmazonWebServiceResponse<AddUserToGroupResult>>()
                        .withMarshaller(new AddUserToGroupRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(addUserToGroupRequest)).getResult();
    }

    @Override
    public AttachGroupPolicyResult attachGroupPolicy(AttachGroupPolicyRequest attachGroupPolicyRequest) {

        StaxResponseHandler<AttachGroupPolicyResult> responseHandler = new StaxResponseHandler<AttachGroupPolicyResult>(
                new AttachGroupPolicyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<AttachGroupPolicyRequest, AmazonWebServiceResponse<AttachGroupPolicyResult>>()
                        .withMarshaller(new AttachGroupPolicyRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(attachGroupPolicyRequest)).getResult();
    }

    @Override
    public AttachRolePolicyResult attachRolePolicy(AttachRolePolicyRequest attachRolePolicyRequest) {

        StaxResponseHandler<AttachRolePolicyResult> responseHandler = new StaxResponseHandler<AttachRolePolicyResult>(
                new AttachRolePolicyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<AttachRolePolicyRequest, AmazonWebServiceResponse<AttachRolePolicyResult>>()
                        .withMarshaller(new AttachRolePolicyRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(attachRolePolicyRequest)).getResult();
    }

    @Override
    public AttachUserPolicyResult attachUserPolicy(AttachUserPolicyRequest attachUserPolicyRequest) {

        StaxResponseHandler<AttachUserPolicyResult> responseHandler = new StaxResponseHandler<AttachUserPolicyResult>(
                new AttachUserPolicyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<AttachUserPolicyRequest, AmazonWebServiceResponse<AttachUserPolicyResult>>()
                        .withMarshaller(new AttachUserPolicyRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(attachUserPolicyRequest)).getResult();
    }

    @Override
    public ChangePasswordResult changePassword(ChangePasswordRequest changePasswordRequest) {

        StaxResponseHandler<ChangePasswordResult> responseHandler = new StaxResponseHandler<ChangePasswordResult>(
                new ChangePasswordResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<ChangePasswordRequest, AmazonWebServiceResponse<ChangePasswordResult>>()
                        .withMarshaller(new ChangePasswordRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(changePasswordRequest)).getResult();
    }

    @Override
    public CreateAccessKeyResult createAccessKey(CreateAccessKeyRequest createAccessKeyRequest) {

        StaxResponseHandler<CreateAccessKeyResult> responseHandler = new StaxResponseHandler<CreateAccessKeyResult>(
                new CreateAccessKeyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<CreateAccessKeyRequest, AmazonWebServiceResponse<CreateAccessKeyResult>>()
                        .withMarshaller(new CreateAccessKeyRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(createAccessKeyRequest)).getResult();
    }

    @Override
    public CreateAccountAliasResult createAccountAlias(CreateAccountAliasRequest createAccountAliasRequest) {

        StaxResponseHandler<CreateAccountAliasResult> responseHandler = new StaxResponseHandler<CreateAccountAliasResult>(
                new CreateAccountAliasResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<CreateAccountAliasRequest, AmazonWebServiceResponse<CreateAccountAliasResult>>()
                        .withMarshaller(new CreateAccountAliasRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(createAccountAliasRequest)).getResult();
    }

    @Override
    public CreateGroupResult createGroup(CreateGroupRequest createGroupRequest) {

        StaxResponseHandler<CreateGroupResult> responseHandler = new StaxResponseHandler<CreateGroupResult>(
                new CreateGroupResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<CreateGroupRequest, AmazonWebServiceResponse<CreateGroupResult>>()
                        .withMarshaller(new CreateGroupRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(createGroupRequest)).getResult();
    }

    @Override
    public CreateInstanceProfileResult createInstanceProfile(CreateInstanceProfileRequest createInstanceProfileRequest) {

        StaxResponseHandler<CreateInstanceProfileResult> responseHandler = new StaxResponseHandler<CreateInstanceProfileResult>(
                new CreateInstanceProfileResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<CreateInstanceProfileRequest, AmazonWebServiceResponse<CreateInstanceProfileResult>>()
                        .withMarshaller(new CreateInstanceProfileRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(createInstanceProfileRequest)).getResult();
    }

    @Override
    public CreateLoginProfileResult createLoginProfile(CreateLoginProfileRequest createLoginProfileRequest) {

        StaxResponseHandler<CreateLoginProfileResult> responseHandler = new StaxResponseHandler<CreateLoginProfileResult>(
                new CreateLoginProfileResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<CreateLoginProfileRequest, AmazonWebServiceResponse<CreateLoginProfileResult>>()
                        .withMarshaller(new CreateLoginProfileRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(createLoginProfileRequest)).getResult();
    }

    @Override
    public CreateOpenIDConnectProviderResult createOpenIDConnectProvider(
            CreateOpenIDConnectProviderRequest createOpenIDConnectProviderRequest) {

        StaxResponseHandler<CreateOpenIDConnectProviderResult> responseHandler = new StaxResponseHandler<CreateOpenIDConnectProviderResult>(
                new CreateOpenIDConnectProviderResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<CreateOpenIDConnectProviderRequest, AmazonWebServiceResponse<CreateOpenIDConnectProviderResult>>()
                                .withMarshaller(new CreateOpenIDConnectProviderRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(createOpenIDConnectProviderRequest)).getResult();
    }

    @Override
    public CreatePolicyResult createPolicy(CreatePolicyRequest createPolicyRequest) {

        StaxResponseHandler<CreatePolicyResult> responseHandler = new StaxResponseHandler<CreatePolicyResult>(
                new CreatePolicyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<CreatePolicyRequest, AmazonWebServiceResponse<CreatePolicyResult>>()
                        .withMarshaller(new CreatePolicyRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(createPolicyRequest)).getResult();
    }

    @Override
    public CreatePolicyVersionResult createPolicyVersion(CreatePolicyVersionRequest createPolicyVersionRequest) {

        StaxResponseHandler<CreatePolicyVersionResult> responseHandler = new StaxResponseHandler<CreatePolicyVersionResult>(
                new CreatePolicyVersionResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<CreatePolicyVersionRequest, AmazonWebServiceResponse<CreatePolicyVersionResult>>()
                        .withMarshaller(new CreatePolicyVersionRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(createPolicyVersionRequest)).getResult();
    }

    @Override
    public CreateRoleResult createRole(CreateRoleRequest createRoleRequest) {

        StaxResponseHandler<CreateRoleResult> responseHandler = new StaxResponseHandler<CreateRoleResult>(
                new CreateRoleResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<CreateRoleRequest, AmazonWebServiceResponse<CreateRoleResult>>()
                        .withMarshaller(new CreateRoleRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(createRoleRequest)).getResult();
    }

    @Override
    public CreateSAMLProviderResult createSAMLProvider(CreateSAMLProviderRequest createSAMLProviderRequest) {

        StaxResponseHandler<CreateSAMLProviderResult> responseHandler = new StaxResponseHandler<CreateSAMLProviderResult>(
                new CreateSAMLProviderResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<CreateSAMLProviderRequest, AmazonWebServiceResponse<CreateSAMLProviderResult>>()
                        .withMarshaller(new CreateSAMLProviderRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(createSAMLProviderRequest)).getResult();
    }

    @Override
    public CreateServiceSpecificCredentialResult createServiceSpecificCredential(
            CreateServiceSpecificCredentialRequest createServiceSpecificCredentialRequest) {

        StaxResponseHandler<CreateServiceSpecificCredentialResult> responseHandler = new StaxResponseHandler<CreateServiceSpecificCredentialResult>(
                new CreateServiceSpecificCredentialResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<CreateServiceSpecificCredentialRequest, AmazonWebServiceResponse<CreateServiceSpecificCredentialResult>>()
                                .withMarshaller(new CreateServiceSpecificCredentialRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(createServiceSpecificCredentialRequest)).getResult();
    }

    @Override
    public CreateUserResult createUser(CreateUserRequest createUserRequest) {

        StaxResponseHandler<CreateUserResult> responseHandler = new StaxResponseHandler<CreateUserResult>(
                new CreateUserResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<CreateUserRequest, AmazonWebServiceResponse<CreateUserResult>>()
                        .withMarshaller(new CreateUserRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(createUserRequest)).getResult();
    }

    @Override
    public CreateVirtualMFADeviceResult createVirtualMFADevice(CreateVirtualMFADeviceRequest createVirtualMFADeviceRequest) {

        StaxResponseHandler<CreateVirtualMFADeviceResult> responseHandler = new StaxResponseHandler<CreateVirtualMFADeviceResult>(
                new CreateVirtualMFADeviceResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<CreateVirtualMFADeviceRequest, AmazonWebServiceResponse<CreateVirtualMFADeviceResult>>()
                                .withMarshaller(new CreateVirtualMFADeviceRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(createVirtualMFADeviceRequest)).getResult();
    }

    @Override
    public DeactivateMFADeviceResult deactivateMFADevice(DeactivateMFADeviceRequest deactivateMFADeviceRequest) {

        StaxResponseHandler<DeactivateMFADeviceResult> responseHandler = new StaxResponseHandler<DeactivateMFADeviceResult>(
                new DeactivateMFADeviceResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<DeactivateMFADeviceRequest, AmazonWebServiceResponse<DeactivateMFADeviceResult>>()
                        .withMarshaller(new DeactivateMFADeviceRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(deactivateMFADeviceRequest)).getResult();
    }

    @Override
    public DeleteAccessKeyResult deleteAccessKey(DeleteAccessKeyRequest deleteAccessKeyRequest) {

        StaxResponseHandler<DeleteAccessKeyResult> responseHandler = new StaxResponseHandler<DeleteAccessKeyResult>(
                new DeleteAccessKeyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<DeleteAccessKeyRequest, AmazonWebServiceResponse<DeleteAccessKeyResult>>()
                        .withMarshaller(new DeleteAccessKeyRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(deleteAccessKeyRequest)).getResult();
    }

    @Override
    public DeleteAccountAliasResult deleteAccountAlias(DeleteAccountAliasRequest deleteAccountAliasRequest) {

        StaxResponseHandler<DeleteAccountAliasResult> responseHandler = new StaxResponseHandler<DeleteAccountAliasResult>(
                new DeleteAccountAliasResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<DeleteAccountAliasRequest, AmazonWebServiceResponse<DeleteAccountAliasResult>>()
                        .withMarshaller(new DeleteAccountAliasRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(deleteAccountAliasRequest)).getResult();
    }

    @Override
    public DeleteAccountPasswordPolicyResult deleteAccountPasswordPolicy(
            DeleteAccountPasswordPolicyRequest deleteAccountPasswordPolicyRequest) {

        StaxResponseHandler<DeleteAccountPasswordPolicyResult> responseHandler = new StaxResponseHandler<DeleteAccountPasswordPolicyResult>(
                new DeleteAccountPasswordPolicyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<DeleteAccountPasswordPolicyRequest, AmazonWebServiceResponse<DeleteAccountPasswordPolicyResult>>()
                                .withMarshaller(new DeleteAccountPasswordPolicyRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(deleteAccountPasswordPolicyRequest)).getResult();
    }

    @Override
    public DeleteGroupResult deleteGroup(DeleteGroupRequest deleteGroupRequest) {

        StaxResponseHandler<DeleteGroupResult> responseHandler = new StaxResponseHandler<DeleteGroupResult>(
                new DeleteGroupResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<DeleteGroupRequest, AmazonWebServiceResponse<DeleteGroupResult>>()
                        .withMarshaller(new DeleteGroupRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(deleteGroupRequest)).getResult();
    }

    @Override
    public DeleteGroupPolicyResult deleteGroupPolicy(DeleteGroupPolicyRequest deleteGroupPolicyRequest) {

        StaxResponseHandler<DeleteGroupPolicyResult> responseHandler = new StaxResponseHandler<DeleteGroupPolicyResult>(
                new DeleteGroupPolicyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<DeleteGroupPolicyRequest, AmazonWebServiceResponse<DeleteGroupPolicyResult>>()
                        .withMarshaller(new DeleteGroupPolicyRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(deleteGroupPolicyRequest)).getResult();
    }

    @Override
    public DeleteInstanceProfileResult deleteInstanceProfile(DeleteInstanceProfileRequest deleteInstanceProfileRequest) {

        StaxResponseHandler<DeleteInstanceProfileResult> responseHandler = new StaxResponseHandler<DeleteInstanceProfileResult>(
                new DeleteInstanceProfileResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<DeleteInstanceProfileRequest, AmazonWebServiceResponse<DeleteInstanceProfileResult>>()
                        .withMarshaller(new DeleteInstanceProfileRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(deleteInstanceProfileRequest)).getResult();
    }

    @Override
    public DeleteLoginProfileResult deleteLoginProfile(DeleteLoginProfileRequest deleteLoginProfileRequest) {

        StaxResponseHandler<DeleteLoginProfileResult> responseHandler = new StaxResponseHandler<DeleteLoginProfileResult>(
                new DeleteLoginProfileResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<DeleteLoginProfileRequest, AmazonWebServiceResponse<DeleteLoginProfileResult>>()
                        .withMarshaller(new DeleteLoginProfileRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(deleteLoginProfileRequest)).getResult();
    }

    @Override
    public DeleteOpenIDConnectProviderResult deleteOpenIDConnectProvider(
            DeleteOpenIDConnectProviderRequest deleteOpenIDConnectProviderRequest) {

        StaxResponseHandler<DeleteOpenIDConnectProviderResult> responseHandler = new StaxResponseHandler<DeleteOpenIDConnectProviderResult>(
                new DeleteOpenIDConnectProviderResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<DeleteOpenIDConnectProviderRequest, AmazonWebServiceResponse<DeleteOpenIDConnectProviderResult>>()
                                .withMarshaller(new DeleteOpenIDConnectProviderRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(deleteOpenIDConnectProviderRequest)).getResult();
    }

    @Override
    public DeletePolicyResult deletePolicy(DeletePolicyRequest deletePolicyRequest) {

        StaxResponseHandler<DeletePolicyResult> responseHandler = new StaxResponseHandler<DeletePolicyResult>(
                new DeletePolicyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<DeletePolicyRequest, AmazonWebServiceResponse<DeletePolicyResult>>()
                        .withMarshaller(new DeletePolicyRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(deletePolicyRequest)).getResult();
    }

    @Override
    public DeletePolicyVersionResult deletePolicyVersion(DeletePolicyVersionRequest deletePolicyVersionRequest) {

        StaxResponseHandler<DeletePolicyVersionResult> responseHandler = new StaxResponseHandler<DeletePolicyVersionResult>(
                new DeletePolicyVersionResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<DeletePolicyVersionRequest, AmazonWebServiceResponse<DeletePolicyVersionResult>>()
                        .withMarshaller(new DeletePolicyVersionRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(deletePolicyVersionRequest)).getResult();
    }

    @Override
    public DeleteRoleResult deleteRole(DeleteRoleRequest deleteRoleRequest) {

        StaxResponseHandler<DeleteRoleResult> responseHandler = new StaxResponseHandler<DeleteRoleResult>(
                new DeleteRoleResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<DeleteRoleRequest, AmazonWebServiceResponse<DeleteRoleResult>>()
                        .withMarshaller(new DeleteRoleRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(deleteRoleRequest)).getResult();
    }

    @Override
    public DeleteRolePolicyResult deleteRolePolicy(DeleteRolePolicyRequest deleteRolePolicyRequest) {

        StaxResponseHandler<DeleteRolePolicyResult> responseHandler = new StaxResponseHandler<DeleteRolePolicyResult>(
                new DeleteRolePolicyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<DeleteRolePolicyRequest, AmazonWebServiceResponse<DeleteRolePolicyResult>>()
                        .withMarshaller(new DeleteRolePolicyRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(deleteRolePolicyRequest)).getResult();
    }

    @Override
    public DeleteSAMLProviderResult deleteSAMLProvider(DeleteSAMLProviderRequest deleteSAMLProviderRequest) {

        StaxResponseHandler<DeleteSAMLProviderResult> responseHandler = new StaxResponseHandler<DeleteSAMLProviderResult>(
                new DeleteSAMLProviderResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<DeleteSAMLProviderRequest, AmazonWebServiceResponse<DeleteSAMLProviderResult>>()
                        .withMarshaller(new DeleteSAMLProviderRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(deleteSAMLProviderRequest)).getResult();
    }

    @Override
    public DeleteSSHPublicKeyResult deleteSSHPublicKey(DeleteSSHPublicKeyRequest deleteSSHPublicKeyRequest) {

        StaxResponseHandler<DeleteSSHPublicKeyResult> responseHandler = new StaxResponseHandler<DeleteSSHPublicKeyResult>(
                new DeleteSSHPublicKeyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<DeleteSSHPublicKeyRequest, AmazonWebServiceResponse<DeleteSSHPublicKeyResult>>()
                        .withMarshaller(new DeleteSSHPublicKeyRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(deleteSSHPublicKeyRequest)).getResult();
    }

    @Override
    public DeleteServerCertificateResult deleteServerCertificate(DeleteServerCertificateRequest deleteServerCertificateRequest) {

        StaxResponseHandler<DeleteServerCertificateResult> responseHandler = new StaxResponseHandler<DeleteServerCertificateResult>(
                new DeleteServerCertificateResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<DeleteServerCertificateRequest, AmazonWebServiceResponse<DeleteServerCertificateResult>>()
                                .withMarshaller(new DeleteServerCertificateRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(deleteServerCertificateRequest)).getResult();
    }

    @Override
    public DeleteServiceSpecificCredentialResult deleteServiceSpecificCredential(
            DeleteServiceSpecificCredentialRequest deleteServiceSpecificCredentialRequest) {

        StaxResponseHandler<DeleteServiceSpecificCredentialResult> responseHandler = new StaxResponseHandler<DeleteServiceSpecificCredentialResult>(
                new DeleteServiceSpecificCredentialResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<DeleteServiceSpecificCredentialRequest, AmazonWebServiceResponse<DeleteServiceSpecificCredentialResult>>()
                                .withMarshaller(new DeleteServiceSpecificCredentialRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(deleteServiceSpecificCredentialRequest)).getResult();
    }

    @Override
    public DeleteSigningCertificateResult deleteSigningCertificate(DeleteSigningCertificateRequest deleteSigningCertificateRequest) {

        StaxResponseHandler<DeleteSigningCertificateResult> responseHandler = new StaxResponseHandler<DeleteSigningCertificateResult>(
                new DeleteSigningCertificateResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<DeleteSigningCertificateRequest, AmazonWebServiceResponse<DeleteSigningCertificateResult>>()
                                .withMarshaller(new DeleteSigningCertificateRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(deleteSigningCertificateRequest)).getResult();
    }

    @Override
    public DeleteUserResult deleteUser(DeleteUserRequest deleteUserRequest) {

        StaxResponseHandler<DeleteUserResult> responseHandler = new StaxResponseHandler<DeleteUserResult>(
                new DeleteUserResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<DeleteUserRequest, AmazonWebServiceResponse<DeleteUserResult>>()
                        .withMarshaller(new DeleteUserRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(deleteUserRequest)).getResult();
    }

    @Override
    public DeleteUserPolicyResult deleteUserPolicy(DeleteUserPolicyRequest deleteUserPolicyRequest) {

        StaxResponseHandler<DeleteUserPolicyResult> responseHandler = new StaxResponseHandler<DeleteUserPolicyResult>(
                new DeleteUserPolicyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<DeleteUserPolicyRequest, AmazonWebServiceResponse<DeleteUserPolicyResult>>()
                        .withMarshaller(new DeleteUserPolicyRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(deleteUserPolicyRequest)).getResult();
    }

    @Override
    public DeleteVirtualMFADeviceResult deleteVirtualMFADevice(DeleteVirtualMFADeviceRequest deleteVirtualMFADeviceRequest) {

        StaxResponseHandler<DeleteVirtualMFADeviceResult> responseHandler = new StaxResponseHandler<DeleteVirtualMFADeviceResult>(
                new DeleteVirtualMFADeviceResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<DeleteVirtualMFADeviceRequest, AmazonWebServiceResponse<DeleteVirtualMFADeviceResult>>()
                                .withMarshaller(new DeleteVirtualMFADeviceRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(deleteVirtualMFADeviceRequest)).getResult();
    }

    @Override
    public DetachGroupPolicyResult detachGroupPolicy(DetachGroupPolicyRequest detachGroupPolicyRequest) {

        StaxResponseHandler<DetachGroupPolicyResult> responseHandler = new StaxResponseHandler<DetachGroupPolicyResult>(
                new DetachGroupPolicyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<DetachGroupPolicyRequest, AmazonWebServiceResponse<DetachGroupPolicyResult>>()
                        .withMarshaller(new DetachGroupPolicyRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(detachGroupPolicyRequest)).getResult();
    }

    @Override
    public DetachRolePolicyResult detachRolePolicy(DetachRolePolicyRequest detachRolePolicyRequest) {

        StaxResponseHandler<DetachRolePolicyResult> responseHandler = new StaxResponseHandler<DetachRolePolicyResult>(
                new DetachRolePolicyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<DetachRolePolicyRequest, AmazonWebServiceResponse<DetachRolePolicyResult>>()
                        .withMarshaller(new DetachRolePolicyRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(detachRolePolicyRequest)).getResult();
    }

    @Override
    public DetachUserPolicyResult detachUserPolicy(DetachUserPolicyRequest detachUserPolicyRequest) {

        StaxResponseHandler<DetachUserPolicyResult> responseHandler = new StaxResponseHandler<DetachUserPolicyResult>(
                new DetachUserPolicyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<DetachUserPolicyRequest, AmazonWebServiceResponse<DetachUserPolicyResult>>()
                        .withMarshaller(new DetachUserPolicyRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(detachUserPolicyRequest)).getResult();
    }

    @Override
    public EnableMFADeviceResult enableMFADevice(EnableMFADeviceRequest enableMFADeviceRequest) {

        StaxResponseHandler<EnableMFADeviceResult> responseHandler = new StaxResponseHandler<EnableMFADeviceResult>(
                new EnableMFADeviceResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<EnableMFADeviceRequest, AmazonWebServiceResponse<EnableMFADeviceResult>>()
                        .withMarshaller(new EnableMFADeviceRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(enableMFADeviceRequest)).getResult();
    }

    @Override
    public GenerateCredentialReportResult generateCredentialReport(GenerateCredentialReportRequest generateCredentialReportRequest) {

        StaxResponseHandler<GenerateCredentialReportResult> responseHandler = new StaxResponseHandler<GenerateCredentialReportResult>(
                new GenerateCredentialReportResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<GenerateCredentialReportRequest, AmazonWebServiceResponse<GenerateCredentialReportResult>>()
                                .withMarshaller(new GenerateCredentialReportRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(generateCredentialReportRequest)).getResult();
    }

    @Override
    public GetAccessKeyLastUsedResult getAccessKeyLastUsed(GetAccessKeyLastUsedRequest getAccessKeyLastUsedRequest) {

        StaxResponseHandler<GetAccessKeyLastUsedResult> responseHandler = new StaxResponseHandler<GetAccessKeyLastUsedResult>(
                new GetAccessKeyLastUsedResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<GetAccessKeyLastUsedRequest, AmazonWebServiceResponse<GetAccessKeyLastUsedResult>>()
                        .withMarshaller(new GetAccessKeyLastUsedRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(getAccessKeyLastUsedRequest)).getResult();
    }

    @Override
    public GetAccountAuthorizationDetailsResult getAccountAuthorizationDetails(
            GetAccountAuthorizationDetailsRequest getAccountAuthorizationDetailsRequest) {

        StaxResponseHandler<GetAccountAuthorizationDetailsResult> responseHandler = new StaxResponseHandler<GetAccountAuthorizationDetailsResult>(
                new GetAccountAuthorizationDetailsResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<GetAccountAuthorizationDetailsRequest, AmazonWebServiceResponse<GetAccountAuthorizationDetailsResult>>()
                                .withMarshaller(new GetAccountAuthorizationDetailsRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(getAccountAuthorizationDetailsRequest)).getResult();
    }

    @Override
    public GetAccountPasswordPolicyResult getAccountPasswordPolicy(GetAccountPasswordPolicyRequest getAccountPasswordPolicyRequest) {

        StaxResponseHandler<GetAccountPasswordPolicyResult> responseHandler = new StaxResponseHandler<GetAccountPasswordPolicyResult>(
                new GetAccountPasswordPolicyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<GetAccountPasswordPolicyRequest, AmazonWebServiceResponse<GetAccountPasswordPolicyResult>>()
                                .withMarshaller(new GetAccountPasswordPolicyRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(getAccountPasswordPolicyRequest)).getResult();
    }

    @Override
    public GetAccountSummaryResult getAccountSummary(GetAccountSummaryRequest getAccountSummaryRequest) {

        StaxResponseHandler<GetAccountSummaryResult> responseHandler = new StaxResponseHandler<GetAccountSummaryResult>(
                new GetAccountSummaryResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<GetAccountSummaryRequest, AmazonWebServiceResponse<GetAccountSummaryResult>>()
                        .withMarshaller(new GetAccountSummaryRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(getAccountSummaryRequest)).getResult();
    }

    @Override
    public GetContextKeysForCustomPolicyResult getContextKeysForCustomPolicy(
            GetContextKeysForCustomPolicyRequest getContextKeysForCustomPolicyRequest) {

        StaxResponseHandler<GetContextKeysForCustomPolicyResult> responseHandler = new StaxResponseHandler<GetContextKeysForCustomPolicyResult>(
                new GetContextKeysForCustomPolicyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<GetContextKeysForCustomPolicyRequest, AmazonWebServiceResponse<GetContextKeysForCustomPolicyResult>>()
                                .withMarshaller(new GetContextKeysForCustomPolicyRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(getContextKeysForCustomPolicyRequest)).getResult();
    }

    @Override
    public GetContextKeysForPrincipalPolicyResult getContextKeysForPrincipalPolicy(
            GetContextKeysForPrincipalPolicyRequest getContextKeysForPrincipalPolicyRequest) {

        StaxResponseHandler<GetContextKeysForPrincipalPolicyResult> responseHandler = new StaxResponseHandler<GetContextKeysForPrincipalPolicyResult>(
                new GetContextKeysForPrincipalPolicyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<GetContextKeysForPrincipalPolicyRequest, AmazonWebServiceResponse<GetContextKeysForPrincipalPolicyResult>>()
                                .withMarshaller(new GetContextKeysForPrincipalPolicyRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(getContextKeysForPrincipalPolicyRequest)).getResult();
    }

    @Override
    public GetCredentialReportResult getCredentialReport(GetCredentialReportRequest getCredentialReportRequest) {

        StaxResponseHandler<GetCredentialReportResult> responseHandler = new StaxResponseHandler<GetCredentialReportResult>(
                new GetCredentialReportResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<GetCredentialReportRequest, AmazonWebServiceResponse<GetCredentialReportResult>>()
                        .withMarshaller(new GetCredentialReportRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(getCredentialReportRequest)).getResult();
    }

    @Override
    public GetGroupResult getGroup(GetGroupRequest getGroupRequest) {

        StaxResponseHandler<GetGroupResult> responseHandler = new StaxResponseHandler<GetGroupResult>(
                new GetGroupResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<GetGroupRequest, AmazonWebServiceResponse<GetGroupResult>>()
                        .withMarshaller(new GetGroupRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(getGroupRequest)).getResult();
    }

    @Override
    public GetGroupPolicyResult getGroupPolicy(GetGroupPolicyRequest getGroupPolicyRequest) {

        StaxResponseHandler<GetGroupPolicyResult> responseHandler = new StaxResponseHandler<GetGroupPolicyResult>(
                new GetGroupPolicyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<GetGroupPolicyRequest, AmazonWebServiceResponse<GetGroupPolicyResult>>()
                        .withMarshaller(new GetGroupPolicyRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(getGroupPolicyRequest)).getResult();
    }

    @Override
    public GetInstanceProfileResult getInstanceProfile(GetInstanceProfileRequest getInstanceProfileRequest) {

        StaxResponseHandler<GetInstanceProfileResult> responseHandler = new StaxResponseHandler<GetInstanceProfileResult>(
                new GetInstanceProfileResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<GetInstanceProfileRequest, AmazonWebServiceResponse<GetInstanceProfileResult>>()
                        .withMarshaller(new GetInstanceProfileRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(getInstanceProfileRequest)).getResult();
    }

    @Override
    public GetLoginProfileResult getLoginProfile(GetLoginProfileRequest getLoginProfileRequest) {

        StaxResponseHandler<GetLoginProfileResult> responseHandler = new StaxResponseHandler<GetLoginProfileResult>(
                new GetLoginProfileResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<GetLoginProfileRequest, AmazonWebServiceResponse<GetLoginProfileResult>>()
                        .withMarshaller(new GetLoginProfileRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(getLoginProfileRequest)).getResult();
    }

    @Override
    public GetOpenIDConnectProviderResult getOpenIDConnectProvider(GetOpenIDConnectProviderRequest getOpenIDConnectProviderRequest) {

        StaxResponseHandler<GetOpenIDConnectProviderResult> responseHandler = new StaxResponseHandler<GetOpenIDConnectProviderResult>(
                new GetOpenIDConnectProviderResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<GetOpenIDConnectProviderRequest, AmazonWebServiceResponse<GetOpenIDConnectProviderResult>>()
                                .withMarshaller(new GetOpenIDConnectProviderRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(getOpenIDConnectProviderRequest)).getResult();
    }

    @Override
    public GetPolicyResult getPolicy(GetPolicyRequest getPolicyRequest) {

        StaxResponseHandler<GetPolicyResult> responseHandler = new StaxResponseHandler<GetPolicyResult>(
                new GetPolicyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<GetPolicyRequest, AmazonWebServiceResponse<GetPolicyResult>>()
                        .withMarshaller(new GetPolicyRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(getPolicyRequest)).getResult();
    }

    @Override
    public GetPolicyVersionResult getPolicyVersion(GetPolicyVersionRequest getPolicyVersionRequest) {

        StaxResponseHandler<GetPolicyVersionResult> responseHandler = new StaxResponseHandler<GetPolicyVersionResult>(
                new GetPolicyVersionResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<GetPolicyVersionRequest, AmazonWebServiceResponse<GetPolicyVersionResult>>()
                        .withMarshaller(new GetPolicyVersionRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(getPolicyVersionRequest)).getResult();
    }

    @Override
    public GetRoleResult getRole(GetRoleRequest getRoleRequest) {

        StaxResponseHandler<GetRoleResult> responseHandler = new StaxResponseHandler<GetRoleResult>(
                new GetRoleResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<GetRoleRequest, AmazonWebServiceResponse<GetRoleResult>>()
                        .withMarshaller(new GetRoleRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(getRoleRequest)).getResult();
    }

    @Override
    public GetRolePolicyResult getRolePolicy(GetRolePolicyRequest getRolePolicyRequest) {

        StaxResponseHandler<GetRolePolicyResult> responseHandler = new StaxResponseHandler<GetRolePolicyResult>(
                new GetRolePolicyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<GetRolePolicyRequest, AmazonWebServiceResponse<GetRolePolicyResult>>()
                        .withMarshaller(new GetRolePolicyRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(getRolePolicyRequest)).getResult();
    }

    @Override
    public GetSAMLProviderResult getSAMLProvider(GetSAMLProviderRequest getSAMLProviderRequest) {

        StaxResponseHandler<GetSAMLProviderResult> responseHandler = new StaxResponseHandler<GetSAMLProviderResult>(
                new GetSAMLProviderResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<GetSAMLProviderRequest, AmazonWebServiceResponse<GetSAMLProviderResult>>()
                        .withMarshaller(new GetSAMLProviderRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(getSAMLProviderRequest)).getResult();
    }

    @Override
    public GetSSHPublicKeyResult getSSHPublicKey(GetSSHPublicKeyRequest getSSHPublicKeyRequest) {

        StaxResponseHandler<GetSSHPublicKeyResult> responseHandler = new StaxResponseHandler<GetSSHPublicKeyResult>(
                new GetSSHPublicKeyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<GetSSHPublicKeyRequest, AmazonWebServiceResponse<GetSSHPublicKeyResult>>()
                        .withMarshaller(new GetSSHPublicKeyRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(getSSHPublicKeyRequest)).getResult();
    }

    @Override
    public GetServerCertificateResult getServerCertificate(GetServerCertificateRequest getServerCertificateRequest) {

        StaxResponseHandler<GetServerCertificateResult> responseHandler = new StaxResponseHandler<GetServerCertificateResult>(
                new GetServerCertificateResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<GetServerCertificateRequest, AmazonWebServiceResponse<GetServerCertificateResult>>()
                        .withMarshaller(new GetServerCertificateRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(getServerCertificateRequest)).getResult();
    }

    @Override
    public GetUserResult getUser(GetUserRequest getUserRequest) {

        StaxResponseHandler<GetUserResult> responseHandler = new StaxResponseHandler<GetUserResult>(
                new GetUserResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<GetUserRequest, AmazonWebServiceResponse<GetUserResult>>()
                        .withMarshaller(new GetUserRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(getUserRequest)).getResult();
    }

    @Override
    public GetUserPolicyResult getUserPolicy(GetUserPolicyRequest getUserPolicyRequest) {

        StaxResponseHandler<GetUserPolicyResult> responseHandler = new StaxResponseHandler<GetUserPolicyResult>(
                new GetUserPolicyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<GetUserPolicyRequest, AmazonWebServiceResponse<GetUserPolicyResult>>()
                        .withMarshaller(new GetUserPolicyRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(getUserPolicyRequest)).getResult();
    }

    @Override
    public ListAccessKeysResult listAccessKeys(ListAccessKeysRequest listAccessKeysRequest) {

        StaxResponseHandler<ListAccessKeysResult> responseHandler = new StaxResponseHandler<ListAccessKeysResult>(
                new ListAccessKeysResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<ListAccessKeysRequest, AmazonWebServiceResponse<ListAccessKeysResult>>()
                        .withMarshaller(new ListAccessKeysRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(listAccessKeysRequest)).getResult();
    }

    @Override
    public ListAccountAliasesResult listAccountAliases(ListAccountAliasesRequest listAccountAliasesRequest) {

        StaxResponseHandler<ListAccountAliasesResult> responseHandler = new StaxResponseHandler<ListAccountAliasesResult>(
                new ListAccountAliasesResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<ListAccountAliasesRequest, AmazonWebServiceResponse<ListAccountAliasesResult>>()
                        .withMarshaller(new ListAccountAliasesRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(listAccountAliasesRequest)).getResult();
    }

    @Override
    public ListAttachedGroupPoliciesResult listAttachedGroupPolicies(
            ListAttachedGroupPoliciesRequest listAttachedGroupPoliciesRequest) {

        StaxResponseHandler<ListAttachedGroupPoliciesResult> responseHandler = new StaxResponseHandler<ListAttachedGroupPoliciesResult>(
                new ListAttachedGroupPoliciesResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<ListAttachedGroupPoliciesRequest, AmazonWebServiceResponse<ListAttachedGroupPoliciesResult>>()
                                .withMarshaller(new ListAttachedGroupPoliciesRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(listAttachedGroupPoliciesRequest)).getResult();
    }

    @Override
    public ListAttachedRolePoliciesResult listAttachedRolePolicies(ListAttachedRolePoliciesRequest listAttachedRolePoliciesRequest) {

        StaxResponseHandler<ListAttachedRolePoliciesResult> responseHandler = new StaxResponseHandler<ListAttachedRolePoliciesResult>(
                new ListAttachedRolePoliciesResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<ListAttachedRolePoliciesRequest, AmazonWebServiceResponse<ListAttachedRolePoliciesResult>>()
                                .withMarshaller(new ListAttachedRolePoliciesRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(listAttachedRolePoliciesRequest)).getResult();
    }

    @Override
    public ListAttachedUserPoliciesResult listAttachedUserPolicies(ListAttachedUserPoliciesRequest listAttachedUserPoliciesRequest) {

        StaxResponseHandler<ListAttachedUserPoliciesResult> responseHandler = new StaxResponseHandler<ListAttachedUserPoliciesResult>(
                new ListAttachedUserPoliciesResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<ListAttachedUserPoliciesRequest, AmazonWebServiceResponse<ListAttachedUserPoliciesResult>>()
                                .withMarshaller(new ListAttachedUserPoliciesRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(listAttachedUserPoliciesRequest)).getResult();
    }

    @Override
    public ListEntitiesForPolicyResult listEntitiesForPolicy(ListEntitiesForPolicyRequest listEntitiesForPolicyRequest) {

        StaxResponseHandler<ListEntitiesForPolicyResult> responseHandler = new StaxResponseHandler<ListEntitiesForPolicyResult>(
                new ListEntitiesForPolicyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<ListEntitiesForPolicyRequest, AmazonWebServiceResponse<ListEntitiesForPolicyResult>>()
                        .withMarshaller(new ListEntitiesForPolicyRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(listEntitiesForPolicyRequest)).getResult();
    }

    @Override
    public ListGroupPoliciesResult listGroupPolicies(ListGroupPoliciesRequest listGroupPoliciesRequest) {

        StaxResponseHandler<ListGroupPoliciesResult> responseHandler = new StaxResponseHandler<ListGroupPoliciesResult>(
                new ListGroupPoliciesResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<ListGroupPoliciesRequest, AmazonWebServiceResponse<ListGroupPoliciesResult>>()
                        .withMarshaller(new ListGroupPoliciesRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(listGroupPoliciesRequest)).getResult();
    }

    @Override
    public ListGroupsResult listGroups(ListGroupsRequest listGroupsRequest) {

        StaxResponseHandler<ListGroupsResult> responseHandler = new StaxResponseHandler<ListGroupsResult>(
                new ListGroupsResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<ListGroupsRequest, AmazonWebServiceResponse<ListGroupsResult>>()
                        .withMarshaller(new ListGroupsRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(listGroupsRequest)).getResult();
    }

    @Override
    public ListGroupsForUserResult listGroupsForUser(ListGroupsForUserRequest listGroupsForUserRequest) {

        StaxResponseHandler<ListGroupsForUserResult> responseHandler = new StaxResponseHandler<ListGroupsForUserResult>(
                new ListGroupsForUserResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<ListGroupsForUserRequest, AmazonWebServiceResponse<ListGroupsForUserResult>>()
                        .withMarshaller(new ListGroupsForUserRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(listGroupsForUserRequest)).getResult();
    }

    @Override
    public ListInstanceProfilesResult listInstanceProfiles(ListInstanceProfilesRequest listInstanceProfilesRequest) {

        StaxResponseHandler<ListInstanceProfilesResult> responseHandler = new StaxResponseHandler<ListInstanceProfilesResult>(
                new ListInstanceProfilesResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<ListInstanceProfilesRequest, AmazonWebServiceResponse<ListInstanceProfilesResult>>()
                        .withMarshaller(new ListInstanceProfilesRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(listInstanceProfilesRequest)).getResult();
    }

    @Override
    public ListInstanceProfilesForRoleResult listInstanceProfilesForRole(
            ListInstanceProfilesForRoleRequest listInstanceProfilesForRoleRequest) {

        StaxResponseHandler<ListInstanceProfilesForRoleResult> responseHandler = new StaxResponseHandler<ListInstanceProfilesForRoleResult>(
                new ListInstanceProfilesForRoleResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<ListInstanceProfilesForRoleRequest, AmazonWebServiceResponse<ListInstanceProfilesForRoleResult>>()
                                .withMarshaller(new ListInstanceProfilesForRoleRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(listInstanceProfilesForRoleRequest)).getResult();
    }

    @Override
    public ListMFADevicesResult listMFADevices(ListMFADevicesRequest listMFADevicesRequest) {

        StaxResponseHandler<ListMFADevicesResult> responseHandler = new StaxResponseHandler<ListMFADevicesResult>(
                new ListMFADevicesResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<ListMFADevicesRequest, AmazonWebServiceResponse<ListMFADevicesResult>>()
                        .withMarshaller(new ListMFADevicesRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(listMFADevicesRequest)).getResult();
    }

    @Override
    public ListOpenIDConnectProvidersResult listOpenIDConnectProviders(
            ListOpenIDConnectProvidersRequest listOpenIDConnectProvidersRequest) {

        StaxResponseHandler<ListOpenIDConnectProvidersResult> responseHandler = new StaxResponseHandler<ListOpenIDConnectProvidersResult>(
                new ListOpenIDConnectProvidersResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<ListOpenIDConnectProvidersRequest, AmazonWebServiceResponse<ListOpenIDConnectProvidersResult>>()
                                .withMarshaller(new ListOpenIDConnectProvidersRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(listOpenIDConnectProvidersRequest)).getResult();
    }

    @Override
    public ListPoliciesResult listPolicies(ListPoliciesRequest listPoliciesRequest) {

        StaxResponseHandler<ListPoliciesResult> responseHandler = new StaxResponseHandler<ListPoliciesResult>(
                new ListPoliciesResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<ListPoliciesRequest, AmazonWebServiceResponse<ListPoliciesResult>>()
                        .withMarshaller(new ListPoliciesRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(listPoliciesRequest)).getResult();
    }

    @Override
    public ListPolicyVersionsResult listPolicyVersions(ListPolicyVersionsRequest listPolicyVersionsRequest) {

        StaxResponseHandler<ListPolicyVersionsResult> responseHandler = new StaxResponseHandler<ListPolicyVersionsResult>(
                new ListPolicyVersionsResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<ListPolicyVersionsRequest, AmazonWebServiceResponse<ListPolicyVersionsResult>>()
                        .withMarshaller(new ListPolicyVersionsRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(listPolicyVersionsRequest)).getResult();
    }

    @Override
    public ListRolePoliciesResult listRolePolicies(ListRolePoliciesRequest listRolePoliciesRequest) {

        StaxResponseHandler<ListRolePoliciesResult> responseHandler = new StaxResponseHandler<ListRolePoliciesResult>(
                new ListRolePoliciesResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<ListRolePoliciesRequest, AmazonWebServiceResponse<ListRolePoliciesResult>>()
                        .withMarshaller(new ListRolePoliciesRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(listRolePoliciesRequest)).getResult();
    }

    @Override
    public ListRolesResult listRoles(ListRolesRequest listRolesRequest) {

        StaxResponseHandler<ListRolesResult> responseHandler = new StaxResponseHandler<ListRolesResult>(
                new ListRolesResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<ListRolesRequest, AmazonWebServiceResponse<ListRolesResult>>()
                        .withMarshaller(new ListRolesRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(listRolesRequest)).getResult();
    }

    @Override
    public ListSAMLProvidersResult listSAMLProviders(ListSAMLProvidersRequest listSAMLProvidersRequest) {

        StaxResponseHandler<ListSAMLProvidersResult> responseHandler = new StaxResponseHandler<ListSAMLProvidersResult>(
                new ListSAMLProvidersResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<ListSAMLProvidersRequest, AmazonWebServiceResponse<ListSAMLProvidersResult>>()
                        .withMarshaller(new ListSAMLProvidersRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(listSAMLProvidersRequest)).getResult();
    }

    @Override
    public ListSSHPublicKeysResult listSSHPublicKeys(ListSSHPublicKeysRequest listSSHPublicKeysRequest) {

        StaxResponseHandler<ListSSHPublicKeysResult> responseHandler = new StaxResponseHandler<ListSSHPublicKeysResult>(
                new ListSSHPublicKeysResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<ListSSHPublicKeysRequest, AmazonWebServiceResponse<ListSSHPublicKeysResult>>()
                        .withMarshaller(new ListSSHPublicKeysRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(listSSHPublicKeysRequest)).getResult();
    }

    @Override
    public ListServerCertificatesResult listServerCertificates(ListServerCertificatesRequest listServerCertificatesRequest) {

        StaxResponseHandler<ListServerCertificatesResult> responseHandler = new StaxResponseHandler<ListServerCertificatesResult>(
                new ListServerCertificatesResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<ListServerCertificatesRequest, AmazonWebServiceResponse<ListServerCertificatesResult>>()
                                .withMarshaller(new ListServerCertificatesRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(listServerCertificatesRequest)).getResult();
    }

    @Override
    public ListServiceSpecificCredentialsResult listServiceSpecificCredentials(
            ListServiceSpecificCredentialsRequest listServiceSpecificCredentialsRequest) {

        StaxResponseHandler<ListServiceSpecificCredentialsResult> responseHandler = new StaxResponseHandler<ListServiceSpecificCredentialsResult>(
                new ListServiceSpecificCredentialsResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<ListServiceSpecificCredentialsRequest, AmazonWebServiceResponse<ListServiceSpecificCredentialsResult>>()
                                .withMarshaller(new ListServiceSpecificCredentialsRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(listServiceSpecificCredentialsRequest)).getResult();
    }

    @Override
    public ListSigningCertificatesResult listSigningCertificates(ListSigningCertificatesRequest listSigningCertificatesRequest) {

        StaxResponseHandler<ListSigningCertificatesResult> responseHandler = new StaxResponseHandler<ListSigningCertificatesResult>(
                new ListSigningCertificatesResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<ListSigningCertificatesRequest, AmazonWebServiceResponse<ListSigningCertificatesResult>>()
                                .withMarshaller(new ListSigningCertificatesRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(listSigningCertificatesRequest)).getResult();
    }

    @Override
    public ListUserPoliciesResult listUserPolicies(ListUserPoliciesRequest listUserPoliciesRequest) {

        StaxResponseHandler<ListUserPoliciesResult> responseHandler = new StaxResponseHandler<ListUserPoliciesResult>(
                new ListUserPoliciesResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<ListUserPoliciesRequest, AmazonWebServiceResponse<ListUserPoliciesResult>>()
                        .withMarshaller(new ListUserPoliciesRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(listUserPoliciesRequest)).getResult();
    }

    @Override
    public ListUsersResult listUsers(ListUsersRequest listUsersRequest) {

        StaxResponseHandler<ListUsersResult> responseHandler = new StaxResponseHandler<ListUsersResult>(
                new ListUsersResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<ListUsersRequest, AmazonWebServiceResponse<ListUsersResult>>()
                        .withMarshaller(new ListUsersRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(listUsersRequest)).getResult();
    }

    @Override
    public ListVirtualMFADevicesResult listVirtualMFADevices(ListVirtualMFADevicesRequest listVirtualMFADevicesRequest) {

        StaxResponseHandler<ListVirtualMFADevicesResult> responseHandler = new StaxResponseHandler<ListVirtualMFADevicesResult>(
                new ListVirtualMFADevicesResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<ListVirtualMFADevicesRequest, AmazonWebServiceResponse<ListVirtualMFADevicesResult>>()
                        .withMarshaller(new ListVirtualMFADevicesRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(listVirtualMFADevicesRequest)).getResult();
    }

    @Override
    public PutGroupPolicyResult putGroupPolicy(PutGroupPolicyRequest putGroupPolicyRequest) {

        StaxResponseHandler<PutGroupPolicyResult> responseHandler = new StaxResponseHandler<PutGroupPolicyResult>(
                new PutGroupPolicyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<PutGroupPolicyRequest, AmazonWebServiceResponse<PutGroupPolicyResult>>()
                        .withMarshaller(new PutGroupPolicyRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(putGroupPolicyRequest)).getResult();
    }

    @Override
    public PutRolePolicyResult putRolePolicy(PutRolePolicyRequest putRolePolicyRequest) {

        StaxResponseHandler<PutRolePolicyResult> responseHandler = new StaxResponseHandler<PutRolePolicyResult>(
                new PutRolePolicyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<PutRolePolicyRequest, AmazonWebServiceResponse<PutRolePolicyResult>>()
                        .withMarshaller(new PutRolePolicyRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(putRolePolicyRequest)).getResult();
    }

    @Override
    public PutUserPolicyResult putUserPolicy(PutUserPolicyRequest putUserPolicyRequest) {

        StaxResponseHandler<PutUserPolicyResult> responseHandler = new StaxResponseHandler<PutUserPolicyResult>(
                new PutUserPolicyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<PutUserPolicyRequest, AmazonWebServiceResponse<PutUserPolicyResult>>()
                        .withMarshaller(new PutUserPolicyRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(putUserPolicyRequest)).getResult();
    }

    @Override
    public RemoveClientIDFromOpenIDConnectProviderResult removeClientIDFromOpenIDConnectProvider(
            RemoveClientIDFromOpenIDConnectProviderRequest removeClientIDFromOpenIDConnectProviderRequest) {

        StaxResponseHandler<RemoveClientIDFromOpenIDConnectProviderResult> responseHandler = new StaxResponseHandler<RemoveClientIDFromOpenIDConnectProviderResult>(
                new RemoveClientIDFromOpenIDConnectProviderResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<RemoveClientIDFromOpenIDConnectProviderRequest, AmazonWebServiceResponse<RemoveClientIDFromOpenIDConnectProviderResult>>()
                                .withMarshaller(new RemoveClientIDFromOpenIDConnectProviderRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(removeClientIDFromOpenIDConnectProviderRequest)).getResult();
    }

    @Override
    public RemoveRoleFromInstanceProfileResult removeRoleFromInstanceProfile(
            RemoveRoleFromInstanceProfileRequest removeRoleFromInstanceProfileRequest) {

        StaxResponseHandler<RemoveRoleFromInstanceProfileResult> responseHandler = new StaxResponseHandler<RemoveRoleFromInstanceProfileResult>(
                new RemoveRoleFromInstanceProfileResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<RemoveRoleFromInstanceProfileRequest, AmazonWebServiceResponse<RemoveRoleFromInstanceProfileResult>>()
                                .withMarshaller(new RemoveRoleFromInstanceProfileRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(removeRoleFromInstanceProfileRequest)).getResult();
    }

    @Override
    public RemoveUserFromGroupResult removeUserFromGroup(RemoveUserFromGroupRequest removeUserFromGroupRequest) {

        StaxResponseHandler<RemoveUserFromGroupResult> responseHandler = new StaxResponseHandler<RemoveUserFromGroupResult>(
                new RemoveUserFromGroupResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<RemoveUserFromGroupRequest, AmazonWebServiceResponse<RemoveUserFromGroupResult>>()
                        .withMarshaller(new RemoveUserFromGroupRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(removeUserFromGroupRequest)).getResult();
    }

    @Override
    public ResetServiceSpecificCredentialResult resetServiceSpecificCredential(
            ResetServiceSpecificCredentialRequest resetServiceSpecificCredentialRequest) {

        StaxResponseHandler<ResetServiceSpecificCredentialResult> responseHandler = new StaxResponseHandler<ResetServiceSpecificCredentialResult>(
                new ResetServiceSpecificCredentialResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<ResetServiceSpecificCredentialRequest, AmazonWebServiceResponse<ResetServiceSpecificCredentialResult>>()
                                .withMarshaller(new ResetServiceSpecificCredentialRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(resetServiceSpecificCredentialRequest)).getResult();
    }

    @Override
    public ResyncMFADeviceResult resyncMFADevice(ResyncMFADeviceRequest resyncMFADeviceRequest) {

        StaxResponseHandler<ResyncMFADeviceResult> responseHandler = new StaxResponseHandler<ResyncMFADeviceResult>(
                new ResyncMFADeviceResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<ResyncMFADeviceRequest, AmazonWebServiceResponse<ResyncMFADeviceResult>>()
                        .withMarshaller(new ResyncMFADeviceRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(resyncMFADeviceRequest)).getResult();
    }

    @Override
    public SetDefaultPolicyVersionResult setDefaultPolicyVersion(SetDefaultPolicyVersionRequest setDefaultPolicyVersionRequest) {

        StaxResponseHandler<SetDefaultPolicyVersionResult> responseHandler = new StaxResponseHandler<SetDefaultPolicyVersionResult>(
                new SetDefaultPolicyVersionResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<SetDefaultPolicyVersionRequest, AmazonWebServiceResponse<SetDefaultPolicyVersionResult>>()
                                .withMarshaller(new SetDefaultPolicyVersionRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(setDefaultPolicyVersionRequest)).getResult();
    }

    @Override
    public SimulateCustomPolicyResult simulateCustomPolicy(SimulateCustomPolicyRequest simulateCustomPolicyRequest) {

        StaxResponseHandler<SimulateCustomPolicyResult> responseHandler = new StaxResponseHandler<SimulateCustomPolicyResult>(
                new SimulateCustomPolicyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<SimulateCustomPolicyRequest, AmazonWebServiceResponse<SimulateCustomPolicyResult>>()
                        .withMarshaller(new SimulateCustomPolicyRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(simulateCustomPolicyRequest)).getResult();
    }

    @Override
    public SimulatePrincipalPolicyResult simulatePrincipalPolicy(SimulatePrincipalPolicyRequest simulatePrincipalPolicyRequest) {

        StaxResponseHandler<SimulatePrincipalPolicyResult> responseHandler = new StaxResponseHandler<SimulatePrincipalPolicyResult>(
                new SimulatePrincipalPolicyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<SimulatePrincipalPolicyRequest, AmazonWebServiceResponse<SimulatePrincipalPolicyResult>>()
                                .withMarshaller(new SimulatePrincipalPolicyRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(simulatePrincipalPolicyRequest)).getResult();
    }

    @Override
    public UpdateAccessKeyResult updateAccessKey(UpdateAccessKeyRequest updateAccessKeyRequest) {

        StaxResponseHandler<UpdateAccessKeyResult> responseHandler = new StaxResponseHandler<UpdateAccessKeyResult>(
                new UpdateAccessKeyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<UpdateAccessKeyRequest, AmazonWebServiceResponse<UpdateAccessKeyResult>>()
                        .withMarshaller(new UpdateAccessKeyRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(updateAccessKeyRequest)).getResult();
    }

    @Override
    public UpdateAccountPasswordPolicyResult updateAccountPasswordPolicy(
            UpdateAccountPasswordPolicyRequest updateAccountPasswordPolicyRequest) {

        StaxResponseHandler<UpdateAccountPasswordPolicyResult> responseHandler = new StaxResponseHandler<UpdateAccountPasswordPolicyResult>(
                new UpdateAccountPasswordPolicyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<UpdateAccountPasswordPolicyRequest, AmazonWebServiceResponse<UpdateAccountPasswordPolicyResult>>()
                                .withMarshaller(new UpdateAccountPasswordPolicyRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(updateAccountPasswordPolicyRequest)).getResult();
    }

    @Override
    public UpdateAssumeRolePolicyResult updateAssumeRolePolicy(UpdateAssumeRolePolicyRequest updateAssumeRolePolicyRequest) {

        StaxResponseHandler<UpdateAssumeRolePolicyResult> responseHandler = new StaxResponseHandler<UpdateAssumeRolePolicyResult>(
                new UpdateAssumeRolePolicyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<UpdateAssumeRolePolicyRequest, AmazonWebServiceResponse<UpdateAssumeRolePolicyResult>>()
                                .withMarshaller(new UpdateAssumeRolePolicyRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(updateAssumeRolePolicyRequest)).getResult();
    }

    @Override
    public UpdateGroupResult updateGroup(UpdateGroupRequest updateGroupRequest) {

        StaxResponseHandler<UpdateGroupResult> responseHandler = new StaxResponseHandler<UpdateGroupResult>(
                new UpdateGroupResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<UpdateGroupRequest, AmazonWebServiceResponse<UpdateGroupResult>>()
                        .withMarshaller(new UpdateGroupRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(updateGroupRequest)).getResult();
    }

    @Override
    public UpdateLoginProfileResult updateLoginProfile(UpdateLoginProfileRequest updateLoginProfileRequest) {

        StaxResponseHandler<UpdateLoginProfileResult> responseHandler = new StaxResponseHandler<UpdateLoginProfileResult>(
                new UpdateLoginProfileResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<UpdateLoginProfileRequest, AmazonWebServiceResponse<UpdateLoginProfileResult>>()
                        .withMarshaller(new UpdateLoginProfileRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(updateLoginProfileRequest)).getResult();
    }

    @Override
    public UpdateOpenIDConnectProviderThumbprintResult updateOpenIDConnectProviderThumbprint(
            UpdateOpenIDConnectProviderThumbprintRequest updateOpenIDConnectProviderThumbprintRequest) {

        StaxResponseHandler<UpdateOpenIDConnectProviderThumbprintResult> responseHandler = new StaxResponseHandler<UpdateOpenIDConnectProviderThumbprintResult>(
                new UpdateOpenIDConnectProviderThumbprintResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<UpdateOpenIDConnectProviderThumbprintRequest, AmazonWebServiceResponse<UpdateOpenIDConnectProviderThumbprintResult>>()
                                .withMarshaller(new UpdateOpenIDConnectProviderThumbprintRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(updateOpenIDConnectProviderThumbprintRequest)).getResult();
    }

    @Override
    public UpdateSAMLProviderResult updateSAMLProvider(UpdateSAMLProviderRequest updateSAMLProviderRequest) {

        StaxResponseHandler<UpdateSAMLProviderResult> responseHandler = new StaxResponseHandler<UpdateSAMLProviderResult>(
                new UpdateSAMLProviderResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<UpdateSAMLProviderRequest, AmazonWebServiceResponse<UpdateSAMLProviderResult>>()
                        .withMarshaller(new UpdateSAMLProviderRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(updateSAMLProviderRequest)).getResult();
    }

    @Override
    public UpdateSSHPublicKeyResult updateSSHPublicKey(UpdateSSHPublicKeyRequest updateSSHPublicKeyRequest) {

        StaxResponseHandler<UpdateSSHPublicKeyResult> responseHandler = new StaxResponseHandler<UpdateSSHPublicKeyResult>(
                new UpdateSSHPublicKeyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<UpdateSSHPublicKeyRequest, AmazonWebServiceResponse<UpdateSSHPublicKeyResult>>()
                        .withMarshaller(new UpdateSSHPublicKeyRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(updateSSHPublicKeyRequest)).getResult();
    }

    @Override
    public UpdateServerCertificateResult updateServerCertificate(UpdateServerCertificateRequest updateServerCertificateRequest) {

        StaxResponseHandler<UpdateServerCertificateResult> responseHandler = new StaxResponseHandler<UpdateServerCertificateResult>(
                new UpdateServerCertificateResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<UpdateServerCertificateRequest, AmazonWebServiceResponse<UpdateServerCertificateResult>>()
                                .withMarshaller(new UpdateServerCertificateRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(updateServerCertificateRequest)).getResult();
    }

    @Override
    public UpdateServiceSpecificCredentialResult updateServiceSpecificCredential(
            UpdateServiceSpecificCredentialRequest updateServiceSpecificCredentialRequest) {

        StaxResponseHandler<UpdateServiceSpecificCredentialResult> responseHandler = new StaxResponseHandler<UpdateServiceSpecificCredentialResult>(
                new UpdateServiceSpecificCredentialResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<UpdateServiceSpecificCredentialRequest, AmazonWebServiceResponse<UpdateServiceSpecificCredentialResult>>()
                                .withMarshaller(new UpdateServiceSpecificCredentialRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(updateServiceSpecificCredentialRequest)).getResult();
    }

    @Override
    public UpdateSigningCertificateResult updateSigningCertificate(UpdateSigningCertificateRequest updateSigningCertificateRequest) {

        StaxResponseHandler<UpdateSigningCertificateResult> responseHandler = new StaxResponseHandler<UpdateSigningCertificateResult>(
                new UpdateSigningCertificateResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<UpdateSigningCertificateRequest, AmazonWebServiceResponse<UpdateSigningCertificateResult>>()
                                .withMarshaller(new UpdateSigningCertificateRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(updateSigningCertificateRequest)).getResult();
    }

    @Override
    public UpdateUserResult updateUser(UpdateUserRequest updateUserRequest) {

        StaxResponseHandler<UpdateUserResult> responseHandler = new StaxResponseHandler<UpdateUserResult>(
                new UpdateUserResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<UpdateUserRequest, AmazonWebServiceResponse<UpdateUserResult>>()
                        .withMarshaller(new UpdateUserRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(updateUserRequest)).getResult();
    }

    @Override
    public UploadSSHPublicKeyResult uploadSSHPublicKey(UploadSSHPublicKeyRequest uploadSSHPublicKeyRequest) {

        StaxResponseHandler<UploadSSHPublicKeyResult> responseHandler = new StaxResponseHandler<UploadSSHPublicKeyResult>(
                new UploadSSHPublicKeyResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler.execute(
                new ClientExecutionParams<UploadSSHPublicKeyRequest, AmazonWebServiceResponse<UploadSSHPublicKeyResult>>()
                        .withMarshaller(new UploadSSHPublicKeyRequestMarshaller()).withResponseHandler(responseHandler)
                        .withErrorResponseHandler(errorResponseHandler).withInput(uploadSSHPublicKeyRequest)).getResult();
    }

    @Override
    public UploadServerCertificateResult uploadServerCertificate(UploadServerCertificateRequest uploadServerCertificateRequest) {

        StaxResponseHandler<UploadServerCertificateResult> responseHandler = new StaxResponseHandler<UploadServerCertificateResult>(
                new UploadServerCertificateResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<UploadServerCertificateRequest, AmazonWebServiceResponse<UploadServerCertificateResult>>()
                                .withMarshaller(new UploadServerCertificateRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(uploadServerCertificateRequest)).getResult();
    }

    @Override
    public UploadSigningCertificateResult uploadSigningCertificate(UploadSigningCertificateRequest uploadSigningCertificateRequest) {

        StaxResponseHandler<UploadSigningCertificateResult> responseHandler = new StaxResponseHandler<UploadSigningCertificateResult>(
                new UploadSigningCertificateResultUnmarshaller());

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(exceptionUnmarshallers);

        return clientHandler
                .execute(
                        new ClientExecutionParams<UploadSigningCertificateRequest, AmazonWebServiceResponse<UploadSigningCertificateResult>>()
                                .withMarshaller(new UploadSigningCertificateRequestMarshaller())
                                .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                                .withInput(uploadSigningCertificateRequest)).getResult();
    }

    private List init() {
        List unmarshallers = new ArrayList();
        unmarshallers.add(new MalformedPolicyDocumentExceptionUnmarshaller());
        unmarshallers.add(new DeleteConflictExceptionUnmarshaller());
        unmarshallers.add(new InvalidCertificateExceptionUnmarshaller());
        unmarshallers.add(new PasswordPolicyViolationExceptionUnmarshaller());
        unmarshallers.add(new CredentialReportExpiredExceptionUnmarshaller());
        unmarshallers.add(new LimitExceededExceptionUnmarshaller());
        unmarshallers.add(new InvalidUserTypeExceptionUnmarshaller());
        unmarshallers.add(new NoSuchEntityExceptionUnmarshaller());
        unmarshallers.add(new EntityTemporarilyUnmodifiableExceptionUnmarshaller());
        unmarshallers.add(new ServiceNotSupportedExceptionUnmarshaller());
        unmarshallers.add(new DuplicateSSHPublicKeyExceptionUnmarshaller());
        unmarshallers.add(new DuplicateCertificateExceptionUnmarshaller());
        unmarshallers.add(new KeyPairMismatchExceptionUnmarshaller());
        unmarshallers.add(new CredentialReportNotReadyExceptionUnmarshaller());
        unmarshallers.add(new EntityAlreadyExistsExceptionUnmarshaller());
        unmarshallers.add(new ServiceFailureExceptionUnmarshaller());
        unmarshallers.add(new InvalidPublicKeyExceptionUnmarshaller());
        unmarshallers.add(new PolicyEvaluationExceptionUnmarshaller());
        unmarshallers.add(new InvalidAuthenticationCodeExceptionUnmarshaller());
        unmarshallers.add(new InvalidInputExceptionUnmarshaller());
        unmarshallers.add(new CredentialReportNotPresentExceptionUnmarshaller());
        unmarshallers.add(new UnrecognizedPublicKeyEncodingExceptionUnmarshaller());
        unmarshallers.add(new MalformedCertificateExceptionUnmarshaller());
        unmarshallers.add(new StandardErrorUnmarshaller(IAMClientException.class));
        return unmarshallers;
    }

    public IAMClientWaiters waiters() {
        if (waiters == null) {
            synchronized (this) {
                if (waiters == null) {
                    waiters = new IAMClientWaiters(this);
                }
            }
        }
        return waiters;
    }

    @Override
    public void close() throws Exception {
        clientHandler.close();
    }
}
