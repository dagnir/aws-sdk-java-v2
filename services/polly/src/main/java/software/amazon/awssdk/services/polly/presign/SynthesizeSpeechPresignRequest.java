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

package software.amazon.awssdk.services.polly.presign;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.core.auth.AwsCredentialsProvider;
import software.amazon.awssdk.services.polly.model.OutputFormat;
import software.amazon.awssdk.services.polly.model.PollyRequest;
import software.amazon.awssdk.services.polly.model.SpeechMarkType;
import software.amazon.awssdk.services.polly.model.TextType;
import software.amazon.awssdk.services.polly.model.VoiceId;

/**
 * Presigning input for {@link PollyClientPresigners#getPresignedSynthesizeSpeechUrl(SynthesizeSpeechPresignRequest)}.
 */
@ReviewBeforeRelease("Immutable? Generate?")
public class SynthesizeSpeechPresignRequest extends PollyRequest implements Serializable {

    private Date expirationDate;

    private AwsCredentialsProvider signingCredentials;

    private java.util.List<String> lexiconNames;

    private String outputFormat;

    private String sampleRate;

    private String text;

    private String textType;

    private String voiceId;

    private java.util.List<String> speechMarkTypes;

    public SynthesizeSpeechPresignRequest(Builder builder) {
        super(builder);
        this.expirationDate = builder.expirationDate();
        this.signingCredentials = builder.signingCredentials();
        this.lexiconNames = builder.lexiconNames();
        this.outputFormat = builder.outputFormat();
        this.sampleRate = builder.sampleRate();
        this.text = builder.text();
        this.textType = builder.textType();
        this.voiceId = builder.voiceId();
        this.speechMarkTypes = builder.speechMarkTypes();
    }

    /**
     * @return Expiration of the presigned request. Default is
     *     {@link PollyClientPresigners#SYNTHESIZE_SPEECH_DEFAULT_EXPIRATION_MINUTES} minutes if not overridden.
     */
    public Date expirationDate() {
        return expirationDate;
    }

    /**
     * @return Credentials to use in presigning the request. If not provided, client credentials are used.
     */
    public AwsCredentialsProvider signingCredentials() {
        return signingCredentials;
    }

    public java.util.List<String> lexiconNames() {
        return lexiconNames;
    }

    /**
     * @see OutputFormat
     */
    public String outputFormat() {
        return this.outputFormat;
    }

    /**
     */
    public String sampleRate() {
        return this.sampleRate;
    }

    /**
     */
    public String text() {
        return this.text;
    }

    public String textType() {
        return this.textType;
    }

    /**
     * @see VoiceId
     */
    public String voiceId() {
        return this.voiceId;
    }

    /**
     * @see SpeechMarkType
     */
    public java.util.List<String> speechMarkTypes() {
        return speechMarkTypes;
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends PollyRequest.Builder {
        Date expirationDate();

        Builder expirationDate(Date date);

        AwsCredentialsProvider signingCredentials();

        Builder signingCredentials(AwsCredentialsProvider awsCredentialsProvider);

        List<String> lexiconNames();

        Builder lexiconNames(Collection<String> lexiconNames);

        String outputFormat();

        Builder outputFormat(String outputFormat);

        String sampleRate();

        Builder sampleRate(String sampleRate);

        String text();

        Builder text(String text);

        String textType();

        Builder textType(String textType);

        String voiceId();

        Builder voiceId(String voiceId);

        List<String> speechMarkTypes();

        Builder speechMarkTypes(Collection<String> speechMarkTypes);

        @Override
        SynthesizeSpeechPresignRequest build();
    }

    private static class BuilderImpl extends PollyRequest.BuilderImpl implements Builder {
        private Date expirationDate;

        private AwsCredentialsProvider signingCredentials;

        private java.util.List<String> lexiconNames;

        private String outputFormat;

        private String sampleRate;

        private String text;

        private String textType;

        private String voiceId;

        private java.util.List<String> speechMarkTypes;

        private BuilderImpl() {
        }

        private BuilderImpl(SynthesizeSpeechPresignRequest request) {
            super(request);
        }

        @Override
        public Date expirationDate() {
            return expirationDate;
        }

        /**
         * Sets the expiration of the presigned request. Default is
         * {@link PollyClientPresigners#SYNTHESIZE_SPEECH_DEFAULT_EXPIRATION_MINUTES} minutes if not overridden.
         *
         * @return This object for method chaining.
         */
        public Builder expirationDate(Date date) {
            setExpirationDate(date);
            return this;
        }

        /**
         * Sets the expiration of the presigned request. Default is
         * {@link PollyClientPresigners#SYNTHESIZE_SPEECH_DEFAULT_EXPIRATION_MINUTES} minutes if not overridden.
         */
        public void setExpirationDate(Date expirationDate) {
            this.expirationDate = expirationDate;
        }

        @Override
        public AwsCredentialsProvider signingCredentials() {
            return signingCredentials;
        }

        /**
         * @param signingCredentials Credentials to use in presigning the request. If not provided, client credentials are used.
         * @return This object for method chaining.
         */
        public Builder signingCredentials(AwsCredentialsProvider signingCredentials) {
            this.signingCredentials = signingCredentials;
            return this;
        }

        /**
         * @param signingCredentials Credentials to use in presigning the request. If not provided, client credentials are used.
         */
        public void setSigningCredentials(AwsCredentialsProvider signingCredentials) {
            this.signingCredentials = signingCredentials;
        }

        @Override
        public List<String> lexiconNames() {
            return lexiconNames;
        }

        public void setLexiconNames(java.util.Collection<String> lexiconNames) {
            if (lexiconNames == null) {
                this.lexiconNames = null;
                return;
            }

            this.lexiconNames = new java.util.ArrayList<>(lexiconNames);
        }

        /**
         * <p> <b>NOTE:</b> This method appends the values to the existing list (if any). Use {@link
         * #setLexiconNames(java.util.Collection)} or {@link #withLexiconNames(java.util.Collection)} if you want to override the
         * existing values. </p>
         *
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder lexiconNames(String... lexiconNames) {
            if (this.lexiconNames == null) {
                setLexiconNames(new java.util.ArrayList<>(lexiconNames.length));
            }
            for (String ele : lexiconNames) {
                this.lexiconNames.add(ele);
            }
            return this;
        }

        /**
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder lexiconNames(java.util.Collection<String> lexiconNames) {
            setLexiconNames(lexiconNames);
            return this;
        }

        @Override
        public String outputFormat() {
            return outputFormat;
        }

        /**
         * @see OutputFormat
         */
        public void setOutputFormat(String outputFormat) {
            this.outputFormat = outputFormat;
        }

        /**
         * @see OutputFormat
         */
        public void setOutputFormat(OutputFormat outputFormat) {
            this.outputFormat = outputFormat.toString();
        }

        /**
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see OutputFormat
         */
        public Builder outputFormat(String outputFormat) {
            setOutputFormat(outputFormat);
            return this;
        }

        /**
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see software.amazon.awssdk.services.polly.model.OutputFormat
         */
        public Builder outputFormat(OutputFormat outputFormat) {
            setOutputFormat(outputFormat);
            return this;
        }

        @Override
        public String sampleRate() {
            return sampleRate;
        }

        /**
         */
        public void setSampleRate(String sampleRate) {
            this.sampleRate = sampleRate;
        }

        /**
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder sampleRate(String sampleRate) {
            setSampleRate(sampleRate);
            return this;
        }

        @Override
        public String text() {
            return text;
        }

        /**
         */
        public void setText(String text) {
            this.text = text;
        }

        /**
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder text(String text) {
            setText(text);
            return this;
        }

        /**
         * @see TextType
         */
        public String textType() {
            return this.textType;
        }

        /**
         * @see TextType
         */
        public void setTextType(String textType) {
            this.textType = textType;
        }

        /**
         * @see TextType
         */
        public void setTextType(TextType textType) {
            this.textType = textType.toString();
        }

        /**
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see TextType
         */
        public Builder textType(String textType) {
            setTextType(textType);
            return this;
        }

        /**
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see TextType
         */
        public Builder textType(TextType textType) {
            setTextType(textType);
            return this;
        }

        @Override
        public String voiceId() {
            return voiceId;
        }

        /**
         * @see VoiceId
         */
        public void setVoiceId(String voiceId) {
            this.voiceId = voiceId;
        }

        /**
         * @see VoiceId
         */
        public void setVoiceId(VoiceId voiceId) {
            this.voiceId = voiceId.toString();
        }

        /**
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see VoiceId
         */
        public Builder voiceId(String voiceId) {
            setVoiceId(voiceId);
            return this;
        }

        /**
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see VoiceId
         */
        public Builder voiceId(VoiceId voiceId) {
            setVoiceId(voiceId);
            return this;
        }

        @Override
        public List<String> speechMarkTypes() {
            return speechMarkTypes;
        }

        /**
         * @see SpeechMarkType
         */
        public void setSpeechMarkTypes(java.util.Collection<String> speechMarkTypes) {
            if (speechMarkTypes == null) {
                this.speechMarkTypes = null;
                return;
            }

            this.speechMarkTypes = new java.util.ArrayList<String>(speechMarkTypes);
        }

        /**
         * <p> <b>NOTE:</b> This method appends the values to the existing list (if any). Use {@link
         * #setSpeechMarkTypes(Collection)} or {@link #withSpeechMarkTypes(java.util.Collection)} if you want to override the
         * existing values. </p>
         *
         * @return Returns a reference to this object so that method calls can be chained together.
         *  @see SpeechMarkType
         */
        public Builder speechMarkTypes(String... speechMarkTypes) {
            if (this.speechMarkTypes == null) {
                setSpeechMarkTypes(new java.util.ArrayList<String>(speechMarkTypes.length));
            }
            for (String ele : speechMarkTypes) {
                this.speechMarkTypes.add(ele);
            }
            return this;
        }

        /**
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see SpeechMarkType
         */
        public Builder speechMarkTypes(java.util.Collection<String> speechMarkTypes) {
            setSpeechMarkTypes(speechMarkTypes);
            return this;
        }

        /**
         * <p> <b>NOTE:</b> This method appends the values to the existing list (if any). Use {@link
         * #setSpeechMarkTypes(Collection)} or {@link #withSpeechMarkTypes(java.util.Collection)} if you want to override the
         * existing values. </p>
         *
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder speechMarkTypes(SpeechMarkType... speechMarkTypes) {
            java.util.ArrayList<String> speechMarkTypesCopy = new java.util.ArrayList<String>(speechMarkTypes.length);
            for (SpeechMarkType value : speechMarkTypes) {
                speechMarkTypesCopy.add(value.toString());
            }
            if (this.speechMarkTypes == null) {
                setSpeechMarkTypes(speechMarkTypesCopy);
            } else {
                this.speechMarkTypes.addAll(speechMarkTypesCopy);
            }
            return this;
        }

        @Override
        public SynthesizeSpeechPresignRequest build() {
            return new SynthesizeSpeechPresignRequest(this);
        }
    }
}
