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

package com.android.ike.eap.statemachine;

import static com.android.ike.eap.message.EapTestMessageDefinitions.EAP_REQUEST_AKA_IDENTITY_PACKET;
import static com.android.ike.eap.message.EapTestMessageDefinitions.EAP_REQUEST_IDENTITY_PACKET;
import static com.android.ike.eap.message.EapTestMessageDefinitions.EAP_REQUEST_NAK_PACKET;
import static com.android.ike.eap.message.EapTestMessageDefinitions.EAP_REQUEST_NOTIFICATION_PACKET;
import static com.android.ike.eap.message.EapTestMessageDefinitions.EAP_RESPONSE_NOTIFICATION_PACKET;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.android.ike.eap.EapResult;
import com.android.ike.eap.EapResult.EapError;
import com.android.ike.eap.EapResult.EapResponse;
import com.android.ike.eap.exceptions.EapInvalidRequestException;
import com.android.ike.eap.statemachine.EapStateMachine.CreatedState;
import com.android.ike.eap.statemachine.EapStateMachine.IdentityState;
import com.android.ike.eap.statemachine.EapStateMachine.MethodState;

import org.junit.Before;
import org.junit.Test;

public class CreatedStateTest extends EapStateTest {
    private EapStateMachine mEapStateMachine;

    @Before
    @Override
    public void setUp() {
        mEapStateMachine = new EapStateMachine();

        // EapStateMachine always starts on CreatedState
        mEapState = (CreatedState) mEapStateMachine.getState();
    }

    @Test
    public void testProcessIdentityRequest() {
        mEapState.process(EAP_REQUEST_IDENTITY_PACKET);
        assertTrue(mEapStateMachine.getState() instanceof IdentityState);
    }

    @Test
    public void testProcessNotificationRequest() {
        EapResult eapResult = mEapState.process(EAP_REQUEST_NOTIFICATION_PACKET);

        // state shouldn't change after Notification request
        assertSame(mEapState, mEapStateMachine.getState());
        assertTrue(eapResult instanceof EapResponse);
        EapResponse eapResponse = (EapResponse) eapResult;
        assertArrayEquals(EAP_RESPONSE_NOTIFICATION_PACKET, eapResponse.packet);
    }

    @Test
    public void testProcessNakRequest() {
        EapResult eapResult = mEapState.process(EAP_REQUEST_NAK_PACKET);

        assertTrue(eapResult instanceof EapError);
        EapError eapError = (EapError) eapResult;
        assertTrue(eapError.cause instanceof EapInvalidRequestException);
    }

    @Test
    public void testProcessAkaIdentity() {
        mEapState.process(EAP_REQUEST_AKA_IDENTITY_PACKET);

        // EapStateMachine should change to MethodState for method-type packet
        assertTrue(mEapStateMachine.getState() instanceof MethodState);
    }

    @Test
    public void testProcessNonRequestMessage() {
        EapResult eapResult = mEapState.process(EAP_RESPONSE_NOTIFICATION_PACKET);

        assertTrue(eapResult instanceof EapError);
        EapError eapError = (EapError) eapResult;
        assertTrue(eapError.cause instanceof EapInvalidRequestException);
    }
}