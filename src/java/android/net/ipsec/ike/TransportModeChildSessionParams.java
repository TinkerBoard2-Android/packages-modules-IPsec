/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.net.ipsec.ike;

import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.SystemApi;

import java.util.Objects;

/**
 * TransportModeChildSessionParams represents proposed configurations for negotiating a transport
 * mode Child Session.
 *
 * @hide
 */
@SystemApi
public final class TransportModeChildSessionParams extends ChildSessionParams {
    private TransportModeChildSessionParams(
            IkeTrafficSelector[] inboundTs,
            IkeTrafficSelector[] outboundTs,
            ChildSaProposal[] proposals,
            int hardLifetimeSec,
            int softLifetimeSec) {
        super(
                inboundTs,
                outboundTs,
                proposals,
                hardLifetimeSec,
                softLifetimeSec,
                true /*isTransport*/);
    }

    /**
     * This class can be used to incrementally construct a {@link TransportModeChildSessionParams}.
     */
    public static final class Builder extends ChildSessionParams.Builder {
        /** Create a Builder for negotiating a transport mode Child Session. */
        public Builder() {
            super();
        }

        /**
         * Adds a Child SA proposal to the {@link TransportModeChildSessionParams} being built.
         *
         * @param proposal Child SA proposal.
         * @return Builder this, to facilitate chaining.
         */
        @NonNull
        public Builder addSaProposal(@NonNull ChildSaProposal proposal) {
            addProposal(proposal);
            return this;
        }

        /**
         * Adds an inbound {@link IkeTrafficSelector} to the {@link TransportModeChildSessionParams}
         * being built.
         *
         * <p>This method allows callers to limit the inbound traffic transmitted over the Child
         * Session to the given range. The IKE server may further narrow the range. Callers should
         * refer to {@link ChildSessionConfiguration} for the negotiated traffic selectors.
         *
         * <p>If no inbound {@link IkeTrafficSelector} is provided, a default value will be used
         * that covers all IP addresses and ports.
         *
         * @param trafficSelector the inbound {@link IkeTrafficSelector}.
         * @return Builder this, to facilitate chaining.
         */
        @NonNull
        public Builder addInboundTrafficSelectors(@NonNull IkeTrafficSelector trafficSelector) {
            Objects.requireNonNull(trafficSelector, "Required argument not provided");
            addInboundTs(trafficSelector);
            return this;
        }

        /**
         * Adds an outbound {@link IkeTrafficSelector} to the {@link
         * TransportModeChildSessionParams} being built.
         *
         * <p>This method allows callers to limit the outbound traffic transmitted over the Child
         * Session to the given range. The IKE server may further narrow the range. Callers should
         * refer to {@link ChildSessionConfiguration} for the negotiated traffic selectors.
         *
         * <p>If no outbound {@link IkeTrafficSelector} is provided, a default value will be used
         * that covers all IP addresses and ports.
         *
         * @param trafficSelector the outbound {@link IkeTrafficSelector}.
         * @return Builder this, to facilitate chaining.
         */
        @NonNull
        public Builder addOutboundTrafficSelectors(@NonNull IkeTrafficSelector trafficSelector) {
            Objects.requireNonNull(trafficSelector, "Required argument not provided");
            addOutboundTs(trafficSelector);
            return this;
        }

        /**
         * Sets hard and soft lifetimes.
         *
         * <p>Lifetimes will not be negotiated with the remote IKE server.
         *
         * @param hardLifetimeSeconds number of seconds after which Child SA will expire. Defaults
         *     to 7200 seconds (2 hours). Considering IPsec packet lifetime, IKE library requires
         *     hard lifetime to be a value from 300 seconds (5 minutes) to 14400 seconds (4 hours),
         *     inclusive.
         * @param softLifetimeSeconds number of seconds after which Child SA will request rekey.
         *     Defaults to 3600 seconds (1 hour). MUST be at least 120 seconds (2 minutes), and at
         *     least 60 seconds (1 minute) shorter than the hard lifetime.
         */
        @NonNull
        public Builder setLifetimeSeconds(
                @IntRange(
                                from = CHILD_HARD_LIFETIME_SEC_MINIMUM,
                                to = CHILD_HARD_LIFETIME_SEC_MAXIMUM)
                        int hardLifetimeSeconds,
                @IntRange(
                                from = CHILD_SOFT_LIFETIME_SEC_MINIMUM,
                                to = CHILD_HARD_LIFETIME_SEC_MAXIMUM)
                        int softLifetimeSeconds) {
            validateAndSetLifetime(hardLifetimeSeconds, softLifetimeSeconds);
            mHardLifetimeSec = hardLifetimeSeconds;
            mSoftLifetimeSec = softLifetimeSeconds;
            return this;
        }

        /**
         * Validates and builds the {@link TransportModeChildSessionParams}.
         *
         * @return the validated {@link TransportModeChildSessionParams}.
         */
        @NonNull
        public TransportModeChildSessionParams build() {
            addDefaultTsIfNotConfigured();
            validateOrThrow();

            return new TransportModeChildSessionParams(
                    mInboundTsList.toArray(new IkeTrafficSelector[0]),
                    mOutboundTsList.toArray(new IkeTrafficSelector[0]),
                    mSaProposalList.toArray(new ChildSaProposal[0]),
                    mHardLifetimeSec,
                    mSoftLifetimeSec);
        }
    }
}
