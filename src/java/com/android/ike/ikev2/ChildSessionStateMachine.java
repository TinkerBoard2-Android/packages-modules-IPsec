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
package com.android.ike.ikev2;

import android.content.Context;
import android.net.IpSecManager;
import android.net.IpSecManager.SecurityParameterIndex;
import android.os.Looper;
import android.os.Message;
import android.util.Pair;

import com.android.ike.ikev2.IkeSessionStateMachine.IChildSessionCallback;
import com.android.ike.ikev2.SaRecord.ChildSaRecord;
import com.android.ike.ikev2.crypto.IkeCipher;
import com.android.ike.ikev2.crypto.IkeMacIntegrity;
import com.android.ike.ikev2.crypto.IkeMacPrf;
import com.android.ike.ikev2.exceptions.IkeProtocolException;
import com.android.ike.ikev2.message.IkePayload;
import com.android.ike.ikev2.message.IkeSaPayload;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * ChildSessionStateMachine tracks states and manages exchanges of this Child Session.
 *
 * <p>ChildSessionStateMachine has two types of states. One type are states where there is no
 * ongoing procedure affecting Child Session (non-procedure state), including Initial, Closed, Idle
 * and Receiving. All other states are "procedure" states which are named as follows:
 *
 * <pre>
 * State Name = [Procedure Type] + [Exchange Initiator] + [Exchange Type].
 * - An IKE procedure consists of one or two IKE exchanges:
 *      Procedure Type = {CreateChild | DeleteChild | Info | RekeyChild | SimulRekeyChild}.
 * - Exchange Initiator indicates whether local or remote peer is the exchange initiator:
 *      Exchange Initiator = {Local | Remote}
 * - Exchange type defines the function of this exchange.
 *      Exchange Type = {Create | Delete}
 * </pre>
 */
public class ChildSessionStateMachine extends StateMachine {
    private static final String TAG = "ChildSessionStateMachine";

    /** Receive request for negotiating first Child SA. */
    private static final int CMD_HANDLE_FIRST_CHILD_EXCHANGE = 1;

    private final Context mContext;
    private final IpSecManager mIpSecManager;

    private final ChildSessionOptions mChildSessionOptions;

    /** Local address assigned on device. */
    private final InetAddress mLocalAddress;
    /** Remote address configured by users. */
    private final InetAddress mRemoteAddress;

    private final IkeMacPrf mIkePrf;

    /** Package private SaProposal that represents the negotiated Child SA proposal. */
    @VisibleForTesting SaProposal mSaProposal;

    private IkeCipher mChildCipher;
    private IkeMacIntegrity mChildIntegrity;

    /** SK_d is renewed when IKE SA is rekeyed. */
    private byte[] mSkD;

    /** Package private */
    @VisibleForTesting ChildSaRecord mCurrentChildSaRecord;

    private final State mInitial = new Initial();
    private final State mClosed = new Closed();
    private final State mIdle = new Idle();

    /** Package private */
    ChildSessionStateMachine(
            String name,
            Looper looper,
            Context context,
            IpSecManager ipSecManager,
            ChildSessionOptions sessionOptions,
            InetAddress localAddress,
            InetAddress remoteAddress,
            IkeMacPrf ikePrf,
            byte[] skD) {
        super(name, looper);

        mContext = context;
        mIpSecManager = ipSecManager;

        mChildSessionOptions = sessionOptions;
        mLocalAddress = localAddress;
        mRemoteAddress = remoteAddress;
        mIkePrf = ikePrf;
        mSkD = skD;

        addState(mInitial);
        addState(mClosed);
        addState(mIdle);

        setInitialState(mInitial);
    }

    /**
     * Receive requesting and responding payloads for negotiating first Child SA.
     *
     * <p>This method is called synchronously from IkeStateMachine. It proxies the synchronous call
     * as an asynchronous job to the ChildStateMachine handler.
     *
     * @param reqPayloads SA negotiation related payloads in IKE_AUTH request.
     * @param respPayloads SA negotiation related payloads in IKE_AUTH response.
     * @param callback callback for notifying IkeSessionStateMachine the negotiation result.
     */
    public void handleFirstChildExchange(
            List<IkePayload> reqPayloads,
            List<IkePayload> respPayloads,
            IChildSessionCallback callback) {
        registerProvisionalChildSession(respPayloads, callback);

        sendMessage(
                CMD_HANDLE_FIRST_CHILD_EXCHANGE,
                new FirstChildNegotiationData(reqPayloads, respPayloads, callback));
    }

    /**
     * Update SK_d with provided value when IKE SA is rekeyed.
     *
     * @param skD the new skD in byte array.
     */
    public void setSkD(byte[] skD) {
        mSkD = skD;
    }

    /**
     * Register provisioning ChildSessionStateMachine in IChildSessionCallback
     *
     * <p>This method is for avoiding CHILD_SA_NOT_FOUND error in IkeSessionStateMachine when remote
     * peer sends request for delete/rekey this Child SA before ChildSessionStateMachine sends
     * FirstChildNegotiationData to itself.
     */
    private void registerProvisionalChildSession(
            List<IkePayload> respPayloads, IChildSessionCallback callback) {
        // When decoding responding IkeSaPayload in IkeSessionStateMachine, it is validated that
        // IkeSaPayload has exactly one IkeSaPayload.Proposal.
        IkeSaPayload saPayload = null;
        for (IkePayload payload : respPayloads) {
            if (payload.payloadType == IkePayload.PAYLOAD_TYPE_SA) {
                saPayload = (IkeSaPayload) payload;
                break;
            }
        }
        if (saPayload == null) {
            throw new IllegalArgumentException(
                    "Receive no SA payload for first Child SA negotiation.");
        }
        // IkeSaPayload.Proposal stores SPI in long type so as to be applied to both 8-byte IKE SPI
        // and 4-byte Child SPI. Here we cast the stored SPI to int to represent a Child SPI.
        int remoteGenSpi = (int) (saPayload.proposalList.get(0).spi);
        callback.onCreateChildSa(remoteGenSpi, this);
    }

    /**
     * FirstChildNegotiationData contains payloads for negotiating first Child SA in IKE_AUTH
     * request and IKE_AUTH response and callback to notify IkeSessionStateMachine the SA
     * negotiation result.
     */
    private static class FirstChildNegotiationData {
        public final List<IkePayload> requestPayloads;
        public final List<IkePayload> responsePayloads;
        public final IChildSessionCallback childCallback;

        FirstChildNegotiationData(
                List<IkePayload> reqPayloads,
                List<IkePayload> respPayloads,
                IChildSessionCallback callback) {
            requestPayloads = reqPayloads;
            responsePayloads = respPayloads;
            childCallback = callback;
        }
    }

    /** Initial state of ChildSessionStateMachine. */
    class Initial extends State {
        @Override
        public boolean processMessage(Message message) {
            switch (message.what) {
                    // TODO: Handle local request for creating Child SA.
                case CMD_HANDLE_FIRST_CHILD_EXCHANGE:
                    boolean childSetUpSuccess = false;
                    Pair<SecurityParameterIndex, SecurityParameterIndex> childSpiPair = null;

                    FirstChildNegotiationData childNegotiationData =
                            (FirstChildNegotiationData) message.obj;
                    try {
                        List<IkePayload> reqPayloads = childNegotiationData.requestPayloads;
                        List<IkePayload> respPayloads = childNegotiationData.responsePayloads;
                        childSpiPair = validateCreateChildResp(reqPayloads, respPayloads);

                        mCurrentChildSaRecord =
                                ChildSaRecord.makeChildSaRecord(
                                        mContext,
                                        reqPayloads,
                                        respPayloads,
                                        childSpiPair.first,
                                        childSpiPair.second,
                                        mLocalAddress,
                                        mRemoteAddress,
                                        mIkePrf,
                                        mChildIntegrity,
                                        mChildCipher,
                                        mSkD,
                                        mChildSessionOptions.isTransportMode(),
                                        true /*isLocalInit*/);
                        // TODO: Add mCurrentChildSaRecord in mSpiToSaRecordMap.
                        childSetUpSuccess = true;
                        transitionTo(mIdle);
                    } catch (IkeProtocolException e) {
                        // TODO: Unregister remotely generated SPI and handle Child SA negotiation
                        // failure.
                    } catch (GeneralSecurityException e) {
                        // TODO: Handle DH shared key calculation failure.
                    } finally {
                        if (!childSetUpSuccess && childSpiPair != null) {
                            childSpiPair.first.close();
                            childSpiPair.second.close();
                        }
                    }
                    return HANDLED;
                default:
                    return NOT_HANDLED;
            }
        }

        private Pair<SecurityParameterIndex, SecurityParameterIndex> validateCreateChildResp(
                List<IkePayload> reqPayloads, List<IkePayload> respPayloads)
                throws IkeProtocolException {
            // TODO: Validate SA reponse against request and set negotiated SA in
            // mChildSessionOptions. Return non-null SPI pair only when validation succeeds,
            // otherwise release all allocated SPIs.
            return new Pair<SecurityParameterIndex, SecurityParameterIndex>(null, null);
        }
    }

    /**
     * Closed represents the state when this ChildSessionStateMachine is closed, and no further
     * actions can be performed on it.
     */
    class Closed extends State {
        // TODO: Implement it.
    }

    /**
     * Idle represents a state when there is no ongoing IKE exchange affecting established Child SA.
     */
    class Idle extends State {
        // TODO: Implement it.
    }

    // TODO: Add states to support creating additional Child SA, deleting Child SA and rekeying
    // Child SA.
}
