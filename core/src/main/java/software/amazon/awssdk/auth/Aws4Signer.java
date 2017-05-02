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

package software.amazon.awssdk.auth;

import static software.amazon.awssdk.util.StringUtils.lowerCase;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.ReadLimitInfo;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.SignableRequest;
import software.amazon.awssdk.annotation.SdkTestInternalApi;
import software.amazon.awssdk.auth.internal.Aws4SignerRequestParams;
import software.amazon.awssdk.auth.internal.Aws4SignerUtils;
import software.amazon.awssdk.auth.internal.SignerConstants;
import software.amazon.awssdk.auth.internal.SignerKey;
import software.amazon.awssdk.internal.collections.FifoCache;
import software.amazon.awssdk.log.InternalLogApi;
import software.amazon.awssdk.log.InternalLogFactory;
import software.amazon.awssdk.util.DateUtils;
import software.amazon.awssdk.util.SdkHttpUtils;
import software.amazon.awssdk.util.StringUtils;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * Signer implementation that signs requests with the AWS4 signing protocol.
 */
public class Aws4Signer extends AbstractAwsSigner implements
                                                  ServiceAwareSigner, RegionAwareSigner, Presigner {

    protected static final InternalLogApi LOG = InternalLogFactory.getLog(Aws4Signer.class);

    private static final int SIGNER_CACHE_MAX_SIZE = 300;
    private static final FifoCache<SignerKey> SIGNER_CACHE = new FifoCache<SignerKey>(SIGNER_CACHE_MAX_SIZE);
    private static final List<String> LIST_OF_HEADERS_TO_IGNORE_IN_LOWER_CASE = Arrays.asList("connection", "x-amzn-trace-id");

    /**
     * Service name override for use when the endpoint can't be used to
     * determine the service name.
     */
    protected String serviceName;

    /**
     * Region name override for use when the endpoint can't be used to determine
     * the region name.
     */
    protected String regionName;

    /** Date override for testing only. */
    protected Date overriddenDate;

    /**
     * Whether double url-encode the resource path when constructing the
     * canonical request. By default, we enable double url-encoding.
     *
     * TODO: Different sigv4 services seem to be inconsistent on this. So for
     * services that want to suppress this, they should use new
     * AWS4Signer(false).
     */
    protected boolean doubleUrlEncode;

    private final SdkClock clock;

    /**
     * Construct a new AWS4 signer instance. By default, enable double
     * url-encoding.
     */
    public Aws4Signer() {
        this(true);
    }

    /**
     * Construct a new AWS4 signer instance.
     *
     * @param doubleUrlEncoding
     *            Whether double url-encode the resource path when constructing
     *            the canonical request.
     */
    public Aws4Signer(boolean doubleUrlEncoding) {
        this(doubleUrlEncoding, SdkClock.STANDARD);
    }

    @SdkTestInternalApi
    public Aws4Signer(SdkClock clock) {
        this(true, clock);
    }

    private Aws4Signer(boolean doubleUrlEncode, SdkClock clock) {
        this.doubleUrlEncode = doubleUrlEncode;
        this.clock = clock;
    }

    /**
     * Sets the date that overrides the signing date in the request. This method
     * is internal and should be used only for testing purposes.
     */
    @SdkTestInternalApi
    public void setOverrideDate(Date overriddenDate) {
        if (overriddenDate != null) {
            this.overriddenDate = new Date(overriddenDate.getTime());
        } else {
            this.overriddenDate = null;
        }
    }

    /**
     * Returns the region name that is used when calculating the signature.
     */
    public String getRegionName() {
        return regionName;
    }

    /**
     * Sets the region name that this signer should use when calculating request
     * signatures. This can almost always be determined directly from the
     * request's end point, so you shouldn't need this method, but it's provided
     * for the edge case where the information is not in the endpoint.
     *
     * @param regionName
     *            The region name to use when calculating signatures in this
     *            signer.
     */
    @Override
    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    /**
     * Returns the service name that is used when calculating the signature.
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Sets the service name that this signer should use when calculating
     * request signatures. This can almost always be determined directly from
     * the request's end point, so you shouldn't need this method, but it's
     * provided for the edge case where the information is not in the endpoint.
     *
     * @param serviceName
     *            The service name to use when calculating signatures in this
     *            signer.
     */
    @Override
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * Returns a copy of date that overrides the signing date in the request.
     * Return null by default.
     */
    public Date getOverriddenDate() {
        return overriddenDate == null ? null : new Date(
                overriddenDate.getTime());
    }

    @Override
    public void sign(SignableRequest<?> request, AwsCredentials credentials) {
        // anonymous credentials, don't sign
        if (isAnonymous(credentials)) {
            return;
        }

        AwsCredentials sanitizedCredentials = sanitizeCredentials(credentials);
        if (sanitizedCredentials instanceof AwsSessionCredentials) {
            addSessionCredentials(request,
                                  (AwsSessionCredentials) sanitizedCredentials);
        }

        final Aws4SignerRequestParams signerParams = new Aws4SignerRequestParams(
                request, overriddenDate, regionName, serviceName,
                SignerConstants.AWS4_SIGNING_ALGORITHM);

        addHostHeader(request);
        request.addHeader(SignerConstants.X_AMZ_DATE,
                          signerParams.getFormattedSigningDateTime());

        String contentSha256 = calculateContentHash(request);

        if ("required".equals(request.getHeaders().get(SignerConstants.X_AMZ_CONTENT_SHA256))) {
            request.addHeader(SignerConstants.X_AMZ_CONTENT_SHA256, contentSha256);
        }

        final String canonicalRequest = createCanonicalRequest(request,
                                                               contentSha256);

        final String stringToSign = createStringToSign(canonicalRequest,
                                                       signerParams);

        final byte[] signingKey = deriveSigningKey(sanitizedCredentials,
                                                   signerParams);

        final byte[] signature = computeSignature(stringToSign, signingKey,
                                                  signerParams);

        request.addHeader(
                SignerConstants.AUTHORIZATION,
                buildAuthorizationHeader(request, signature,
                                         sanitizedCredentials, signerParams));

        processRequestPayload(request, signature, signingKey,
                              signerParams);
    }

    @Override
    public void presignRequest(SignableRequest<?> request, AwsCredentials credentials,
                               Date userSpecifiedExpirationDate) {

        // anonymous credentials, don't sign
        if (isAnonymous(credentials)) {
            return;
        }

        long expirationInSeconds = generateExpirationDate(userSpecifiedExpirationDate);

        addHostHeader(request);

        AwsCredentials sanitizedCredentials = sanitizeCredentials(credentials);
        if (sanitizedCredentials instanceof AwsSessionCredentials) {
            // For SigV4 pre-signing URL, we need to add "X-Amz-Security-Token"
            // as a query string parameter, before constructing the canonical
            // request.
            request.addParameter(SignerConstants.X_AMZ_SECURITY_TOKEN,
                                 ((AwsSessionCredentials) sanitizedCredentials)
                                         .getSessionToken());
        }

        final Aws4SignerRequestParams signerRequestParams = new Aws4SignerRequestParams(
                request, overriddenDate, regionName, serviceName,
                SignerConstants.AWS4_SIGNING_ALGORITHM);

        // Add the important parameters for v4 signing
        final String timeStamp = signerRequestParams.getFormattedSigningDateTime();

        addPreSignInformationToRequest(request, sanitizedCredentials,
                                       signerRequestParams, timeStamp, expirationInSeconds);

        final String contentSha256 = calculateContentHashPresign(request);

        final String canonicalRequest = createCanonicalRequest(request,
                                                               contentSha256);

        final String stringToSign = createStringToSign(canonicalRequest,
                                                       signerRequestParams);

        final byte[] signingKey = deriveSigningKey(sanitizedCredentials,
                                                   signerRequestParams);

        final byte[] signature = computeSignature(stringToSign, signingKey,
                                                  signerRequestParams);

        request.addParameter(SignerConstants.X_AMZ_SIGNATURE, BinaryUtils.toHex(signature));
    }

    /**
     * Step 1 of the AWS Signature version 4 calculation. Refer to
     * http://docs.aws
     * .amazon.com/general/latest/gr/sigv4-create-canonical-request.html to
     * generate the canonical request.
     */
    protected String createCanonicalRequest(SignableRequest<?> request,
                                            String contentSha256) {
        /* This would url-encode the resource path for the first time. */
        final String path = SdkHttpUtils.appendUri(
                request.getEndpoint().getPath(), request.getResourcePath());

        final StringBuilder canonicalRequestBuilder = new StringBuilder(request.getHttpMethod().toString());

        canonicalRequestBuilder.append(SignerConstants.LINE_SEPARATOR)
                               // This would optionally double url-encode the resource path
                               .append(getCanonicalizedResourcePath(path, doubleUrlEncode))
                               .append(SignerConstants.LINE_SEPARATOR)
                               .append(getCanonicalizedQueryString(request.getParameters()))
                               .append(SignerConstants.LINE_SEPARATOR)
                               .append(getCanonicalizedHeaderString(request))
                               .append(SignerConstants.LINE_SEPARATOR)
                               .append(getSignedHeadersString(request)).append(SignerConstants.LINE_SEPARATOR)
                               .append(contentSha256);

        final String canonicalRequest = canonicalRequestBuilder.toString();

        if (LOG.isDebugEnabled()) {
            LOG.debug("AWS4 Canonical Request: '\"" + canonicalRequest + "\"");
        }

        return canonicalRequest;
    }

    /**
     * Step 2 of the AWS Signature version 4 calculation. Refer to
     * http://docs.aws
     * .amazon.com/general/latest/gr/sigv4-create-string-to-sign.html.
     */
    protected String createStringToSign(String canonicalRequest,
                                        Aws4SignerRequestParams signerParams) {

        final StringBuilder stringToSignBuilder = new StringBuilder(
                signerParams.getSigningAlgorithm());
        stringToSignBuilder.append(SignerConstants.LINE_SEPARATOR)
                           .append(signerParams.getFormattedSigningDateTime())
                           .append(SignerConstants.LINE_SEPARATOR)
                           .append(signerParams.getScope())
                           .append(SignerConstants.LINE_SEPARATOR)
                           .append(BinaryUtils.toHex(hash(canonicalRequest)));

        final String stringToSign = stringToSignBuilder.toString();

        if (LOG.isDebugEnabled()) {
            LOG.debug("AWS4 String to Sign: '\"" + stringToSign + "\"");
        }

        return stringToSign;
    }

    /**
     * Step 3 of the AWS Signature version 4 calculation. It involves deriving
     * the signing key and computing the signature. Refer to
     * http://docs.aws.amazon
     * .com/general/latest/gr/sigv4-calculate-signature.html
     */
    private final byte[] deriveSigningKey(AwsCredentials credentials,
                                          Aws4SignerRequestParams signerRequestParams) {

        final String cacheKey = computeSigningCacheKeyName(credentials,
                                                           signerRequestParams);
        final long daysSinceEpochSigningDate = DateUtils
                .numberOfDaysSinceEpoch(signerRequestParams
                                                .getSigningDateTimeMilli());

        SignerKey signerKey = SIGNER_CACHE.get(cacheKey);

        if (signerKey != null) {
            if (daysSinceEpochSigningDate == signerKey
                    .getNumberOfDaysSinceEpoch()) {
                return signerKey.getSigningKey();
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Generating a new signing key as the signing key not available in the cache for the date "
                      + TimeUnit.DAYS.toMillis(daysSinceEpochSigningDate));
        }
        byte[] signingKey = newSigningKey(credentials,
                                          signerRequestParams.getFormattedSigningDate(),
                                          signerRequestParams.getRegionName(),
                                          signerRequestParams.getServiceName());
        SIGNER_CACHE.add(cacheKey, new SignerKey(
                daysSinceEpochSigningDate, signingKey));
        return signingKey;
    }

    /**
     * Computes the name to be used to reference the signing key in the cache.
     */
    private final String computeSigningCacheKeyName(AwsCredentials credentials,
                                                    Aws4SignerRequestParams signerRequestParams) {
        final StringBuilder hashKeyBuilder = new StringBuilder(
                credentials.getAwsSecretKey());

        return hashKeyBuilder.append("-")
                             .append(signerRequestParams.getRegionName())
                             .append("-")
                             .append(signerRequestParams.getServiceName()).toString();
    }

    /**
     * Step 3 of the AWS Signature version 4 calculation. It involves deriving
     * the signing key and computing the signature. Refer to
     * http://docs.aws.amazon
     * .com/general/latest/gr/sigv4-calculate-signature.html
     */
    protected final byte[] computeSignature(String stringToSign,
                                            byte[] signingKey, Aws4SignerRequestParams signerRequestParams) {
        return sign(stringToSign.getBytes(Charset.forName("UTF-8")), signingKey,
                    SigningAlgorithm.HmacSHA256);
    }

    /**
     * Creates the authorization header to be included in the request.
     */
    private String buildAuthorizationHeader(SignableRequest<?> request,
                                            byte[] signature, AwsCredentials credentials,
                                            Aws4SignerRequestParams signerParams) {
        final String signingCredentials = credentials.getAwsAccessKeyId() + "/"
                                          + signerParams.getScope();

        final String credential = "Credential="
                                  + signingCredentials;
        final String signerHeaders = "SignedHeaders="
                                     + getSignedHeadersString(request);
        final String signatureHeader = "Signature="
                                       + BinaryUtils.toHex(signature);

        final StringBuilder authHeaderBuilder = new StringBuilder();

        authHeaderBuilder.append(SignerConstants.AWS4_SIGNING_ALGORITHM)
                         .append(" ")
                         .append(credential)
                         .append(", ")
                         .append(signerHeaders)
                         .append(", ")
                         .append(signatureHeader);

        return authHeaderBuilder.toString();
    }

    /**
     * Includes all the signing headers as request parameters for pre-signing.
     */
    private void addPreSignInformationToRequest(SignableRequest<?> request,
                                                AwsCredentials credentials, Aws4SignerRequestParams signerParams,
                                                String timeStamp, long expirationInSeconds) {

        String signingCredentials = credentials.getAwsAccessKeyId() + "/"
                                    + signerParams.getScope();

        request.addParameter(SignerConstants.X_AMZ_ALGORITHM, SignerConstants.AWS4_SIGNING_ALGORITHM);
        request.addParameter(SignerConstants.X_AMZ_DATE, timeStamp);
        request.addParameter(SignerConstants.X_AMZ_SIGNED_HEADER,
                             getSignedHeadersString(request));
        request.addParameter(SignerConstants.X_AMZ_EXPIRES,
                             Long.toString(expirationInSeconds));
        request.addParameter(SignerConstants.X_AMZ_CREDENTIAL, signingCredentials);
    }

    @Override
    protected void addSessionCredentials(SignableRequest<?> request,
                                         AwsSessionCredentials credentials) {
        request.addHeader(SignerConstants.X_AMZ_SECURITY_TOKEN, credentials.getSessionToken());
    }

    protected String getCanonicalizedHeaderString(SignableRequest<?> request) {
        final List<String> sortedHeaders = new ArrayList<String>(request.getHeaders()
                                                                        .keySet());
        Collections.sort(sortedHeaders, String.CASE_INSENSITIVE_ORDER);

        final Map<String, String> requestHeaders = request.getHeaders();
        StringBuilder buffer = new StringBuilder();
        for (String header : sortedHeaders) {
            if (shouldExcludeHeaderFromSigning(header)) {
                continue;
            }
            String key = lowerCase(header);
            String value = requestHeaders.get(header);

            StringUtils.appendCompactedString(buffer, key);
            buffer.append(":");
            if (value != null) {
                StringUtils.appendCompactedString(buffer, value);
            }

            buffer.append("\n");
        }

        return buffer.toString();
    }

    protected String getSignedHeadersString(SignableRequest<?> request) {
        final List<String> sortedHeaders = new ArrayList<String>(request
                                                                         .getHeaders().keySet());
        Collections.sort(sortedHeaders, String.CASE_INSENSITIVE_ORDER);

        StringBuilder buffer = new StringBuilder();
        for (String header : sortedHeaders) {
            if (shouldExcludeHeaderFromSigning(header)) {
                continue;
            }
            if (buffer.length() > 0) {
                buffer.append(";");
            }
            buffer.append(lowerCase(header));
        }

        return buffer.toString();
    }

    protected boolean shouldExcludeHeaderFromSigning(String header) {
        return LIST_OF_HEADERS_TO_IGNORE_IN_LOWER_CASE.contains(lowerCase(header));
    }

    protected void addHostHeader(SignableRequest<?> request) {
        // AWS4 requires that we sign the Host header so we
        // have to have it in the request by the time we sign.

        final URI endpoint = request.getEndpoint();
        final StringBuilder hostHeaderBuilder = new StringBuilder(
                endpoint.getHost());
        if (SdkHttpUtils.isUsingNonDefaultPort(endpoint)) {
            hostHeaderBuilder.append(":").append(endpoint.getPort());
        }

        request.addHeader(SignerConstants.HOST, hostHeaderBuilder.toString());
    }

    /**
     * Calculate the hash of the request's payload. Subclass could override this
     * method to provide different values for "x-amz-content-sha256" header or
     * do any other necessary set-ups on the request headers. (e.g. aws-chunked
     * uses a pre-defined header value, and needs to change some headers
     * relating to content-encoding and content-length.)
     */
    protected String calculateContentHash(SignableRequest<?> request) {
        InputStream payloadStream = getBinaryRequestPayloadStream(request);
        ReadLimitInfo info = request.getReadLimitInfo();
        payloadStream.mark(info == null ? -1 : info.getReadLimit());
        String contentSha256 = BinaryUtils.toHex(hash(payloadStream));
        try {
            payloadStream.reset();
        } catch (IOException e) {
            throw new SdkClientException(
                    "Unable to reset stream after calculating AWS4 signature",
                    e);
        }
        return contentSha256;
    }

    /**
     * Subclass could override this method to perform any additional procedure
     * on the request payload, with access to the result from signing the
     * header. (e.g. Signing the payload by chunk-encoding). The default
     * implementation doesn't need to do anything.
     */
    protected void processRequestPayload(SignableRequest<?> request, byte[] signature,
                                         byte[] signingKey, Aws4SignerRequestParams signerRequestParams) {
        return;
    }

    /**
     * Calculate the hash of the request's payload. In case of pre-sign, the
     * existing code would generate the hash of an empty byte array and returns
     * it. This method can be overridden by sub classes to provide different
     * values (e.g) For S3 pre-signing, the content hash calculation is
     * different from the general implementation.
     *
     */
    protected String calculateContentHashPresign(SignableRequest<?> request) {
        return calculateContentHash(request);
    }

    /**
     * Checks if the credentials is an instance of {@link AnonymousAwsCredentials}
     */
    private boolean isAnonymous(AwsCredentials credentials) {
        return credentials instanceof AnonymousAwsCredentials;
    }

    /**
     * Generates an expiration date for the presigned url. If user has specified
     * an expiration date, check if it is in the given limit.
     */
    private long generateExpirationDate(Date expirationDate) {

        long expirationInSeconds = expirationDate != null ? ((expirationDate
                                                                      .getTime() - clock.currentTimeMillis()) / 1000L)
                                                          : SignerConstants.PRESIGN_URL_MAX_EXPIRATION_SECONDS;

        if (expirationInSeconds > SignerConstants.PRESIGN_URL_MAX_EXPIRATION_SECONDS) {
            throw new SdkClientException(
                    "Requests that are pre-signed by SigV4 algorithm are valid for at most 7 days. "
                    + "The expiration date set on the current request ["
                    + Aws4SignerUtils.formatTimestamp(expirationDate
                                                              .getTime()) + "] has exceeded this limit.");
        }
        return expirationInSeconds;
    }

    /**
     * Generates a new signing key from the given parameters and returns it.
     */
    protected byte[] newSigningKey(AwsCredentials credentials,
                                   String dateStamp, String regionName, String serviceName) {
        byte[] kSecret = ("AWS4" + credentials.getAwsSecretKey())
                .getBytes(Charset.forName("UTF-8"));
        byte[] kDate = sign(dateStamp, kSecret, SigningAlgorithm.HmacSHA256);
        byte[] kRegion = sign(regionName, kDate, SigningAlgorithm.HmacSHA256);
        byte[] kService = sign(serviceName, kRegion,
                               SigningAlgorithm.HmacSHA256);
        return sign(SignerConstants.AWS4_TERMINATOR, kService, SigningAlgorithm.HmacSHA256);
    }
}
