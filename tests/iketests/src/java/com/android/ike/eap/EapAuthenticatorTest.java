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

package com.android.ike.eap;

import static com.android.ike.TestUtils.hexStringToByteArray;
import static com.android.ike.eap.message.EapTestMessageDefinitions.EAP_FAILURE_PACKET;
import static com.android.ike.eap.message.EapTestMessageDefinitions.EAP_REQUEST_SIM_START_PACKET;
import static com.android.ike.eap.message.EapTestMessageDefinitions.EAP_SIM_RESPONSE_PACKET;
import static com.android.ike.eap.message.EapTestMessageDefinitions.EAP_SUCCESS_PACKET;
import static com.android.ike.eap.message.EapTestMessageDefinitions.REQUEST_UNSUPPORTED_TYPE_PACKET;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.os.Handler;
import android.os.Looper;
import android.os.test.TestLooper;

import com.android.ike.eap.EapResult.EapError;
import com.android.ike.eap.EapResult.EapFailure;
import com.android.ike.eap.EapResult.EapResponse;
import com.android.ike.eap.EapResult.EapSuccess;
import com.android.ike.eap.exceptions.EapInvalidRequestException;
import com.android.ike.eap.statemachine.EapStateMachine;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeoutException;

public class EapAuthenticatorTest {
    private static final long AUTHENTICATOR_TIMEOUT_MILLIS = 250L;
    private static final long TEST_TIMEOUT_MILLIS = 2 * AUTHENTICATOR_TIMEOUT_MILLIS;
    private static final byte[] MSK = hexStringToByteArray(
            "00112233445566778899AABBCCDDEEFF"
            + "00112233445566778899AABBCCDDEEFF"
            + "00112233445566778899AABBCCDDEEFF"
            + "00112233445566778899AABBCCDDEEFF");
    private static final byte[] EMSK = hexStringToByteArray(
            "FFEEDDCCBBAA99887766554433221100"
            + "FFEEDDCCBBAA99887766554433221100"
            + "FFEEDDCCBBAA99887766554433221100"
            + "FFEEDDCCBBAA99887766554433221100");

    private EapStateMachine mMockEapStateMachine;

    private Handler mCallbackHandler;
    private TestLooper mTestLooper;
    private boolean mCallbackFired;

    @Before
    public void setUp() {
        if (Looper.myLooper() == null) Looper.prepare();

        mMockEapStateMachine = mock(EapStateMachine.class);

        mTestLooper = new TestLooper();
        mCallbackHandler = new Handler(mTestLooper.getLooper());
        mCallbackFired = false;
    }

    @Test
    public void testProcessEapMessageResponse() {
        EapCallback eapCallback = new EapCallback() {
            @Override
            public void onResponse(byte[] eapMsg) {
                assertArrayEquals(EAP_SIM_RESPONSE_PACKET, eapMsg);
                assertFalse("Callback has already been fired", mCallbackFired);
                mCallbackFired = true;
            }
        };

        EapResponse eapResponse = new EapResponse(EAP_SIM_RESPONSE_PACKET);
        when(mMockEapStateMachine.process(eq(EAP_REQUEST_SIM_START_PACKET)))
                .thenReturn(eapResponse);

        getEapAuthenticatorWithCallback(eapCallback)
                .processEapMessage(EAP_REQUEST_SIM_START_PACKET);
        mTestLooper.dispatchAll();

        assertTrue("Callback didn't fire", mCallbackFired);
        verify(mMockEapStateMachine).process(eq(EAP_REQUEST_SIM_START_PACKET));
        verifyNoMoreInteractions(mMockEapStateMachine);
    }

    @Test
    public void testProcessEapMessageError() {
        EapCallback eapCallback = new EapCallback() {
            @Override
            public void onError(Throwable cause) {
                assertTrue(cause instanceof EapInvalidRequestException);
                assertFalse("Callback has already been fired", mCallbackFired);
                mCallbackFired = true;
            }
        };
        Exception cause = new EapInvalidRequestException("Error");
        EapError eapError = new EapError(cause);
        when(mMockEapStateMachine.process(eq(REQUEST_UNSUPPORTED_TYPE_PACKET)))
                .thenReturn(eapError);

        getEapAuthenticatorWithCallback(eapCallback)
                .processEapMessage(REQUEST_UNSUPPORTED_TYPE_PACKET);
        mTestLooper.dispatchAll();

        assertTrue("Callback didn't fire", mCallbackFired);
        verify(mMockEapStateMachine).process(eq(REQUEST_UNSUPPORTED_TYPE_PACKET));
        verifyNoMoreInteractions(mMockEapStateMachine);
    }

    @Test
    public void testProcessEapMessageSuccess() {
        EapCallback eapCallback = new EapCallback() {
            @Override
            public void onSuccess(byte[] msk, byte[] emsk) {
                assertArrayEquals(MSK, msk);
                assertArrayEquals(EMSK, emsk);
                assertFalse("Callback has already been fired", mCallbackFired);
                mCallbackFired = true;
            }
        };
        EapSuccess eapSuccess = new EapSuccess(MSK, EMSK);
        when(mMockEapStateMachine.process(eq(EAP_SUCCESS_PACKET)))
                .thenReturn(eapSuccess);

        getEapAuthenticatorWithCallback(eapCallback)
                .processEapMessage(EAP_SUCCESS_PACKET);
        mTestLooper.dispatchAll();

        assertTrue("Callback didn't fire", mCallbackFired);
        verify(mMockEapStateMachine).process(eq(EAP_SUCCESS_PACKET));
        verifyNoMoreInteractions(mMockEapStateMachine);
    }

    @Test
    public void testProcessEapMessageFailure() {
        EapCallback eapCallback = new EapCallback() {
            @Override
            public void onFail() {
                // nothing to check here
                assertFalse("Callback has already been fired", mCallbackFired);
                mCallbackFired = true;
            }
        };
        when(mMockEapStateMachine.process(eq(EAP_FAILURE_PACKET)))
                .thenReturn(new EapFailure());

        getEapAuthenticatorWithCallback(eapCallback)
                .processEapMessage(EAP_FAILURE_PACKET);
        mTestLooper.dispatchAll();

        assertTrue("Callback didn't fire", mCallbackFired);
        verify(mMockEapStateMachine).process(eq(EAP_FAILURE_PACKET));
        verifyNoMoreInteractions(mMockEapStateMachine);
    }

    @Test
    public void testProcessEapMessageExceptionThrown() {
        EapCallback eapCallback = new EapCallback() {
            @Override
            public void onError(Throwable cause) {
                assertTrue(cause instanceof NullPointerException);
                assertFalse("Callback has already been fired", mCallbackFired);
                mCallbackFired = true;
            }
        };
        when(mMockEapStateMachine.process(EAP_REQUEST_SIM_START_PACKET))
                .thenThrow(new NullPointerException());

        getEapAuthenticatorWithCallback(eapCallback)
                .processEapMessage(EAP_REQUEST_SIM_START_PACKET);
        mTestLooper.dispatchAll();

        assertTrue("Callback didn't fire", mCallbackFired);
        verify(mMockEapStateMachine).process(eq(EAP_REQUEST_SIM_START_PACKET));
        verifyNoMoreInteractions(mMockEapStateMachine);
    }

    @Test
    public void testProcessEapMessageStateMachineTimeout() {
        EapCallback eapCallback = new EapCallback() {
            @Override
            public void onError(Throwable cause) {
                assertTrue(cause instanceof TimeoutException);
                assertFalse("Callback has already been fired", mCallbackFired);
                mCallbackFired = true;
            }
        };
        EapResponse eapResponse = new EapResponse(EAP_SIM_RESPONSE_PACKET);
        when(mMockEapStateMachine.process(eq(EAP_REQUEST_SIM_START_PACKET)))
                .then((invocation) -> {
                    // move time forward to trigger the timeout
                    mTestLooper.moveTimeForward(TEST_TIMEOUT_MILLIS);
                    return eapResponse;
                });

        getEapAuthenticatorWithCallback(eapCallback)
                .processEapMessage(EAP_REQUEST_SIM_START_PACKET);
        mTestLooper.dispatchAll();

        assertTrue("Callback didn't fire", mCallbackFired);
        verify(mMockEapStateMachine).process(eq(EAP_REQUEST_SIM_START_PACKET));
        verifyNoMoreInteractions(mMockEapStateMachine);
    }

    private EapAuthenticator getEapAuthenticatorWithCallback(EapCallback eapCallback) {
        return new EapAuthenticator(
                mTestLooper.getLooper(),
                mCallbackHandler,
                eapCallback,
                mMockEapStateMachine,
                (runnable) -> runnable.run(),
                AUTHENTICATOR_TIMEOUT_MILLIS);
    }

    /**
     * Default {@link IEapCallback} implementation that throws {@link UnsupportedOperationException}
     * for all calls.
     */
    private abstract static class EapCallback implements IEapCallback {
        @Override
        public void onSuccess(byte[] msk, byte[] emsk) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onFail() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onResponse(byte[] eapMsg) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onError(Throwable cause) {
            throw new UnsupportedOperationException();
        }
    }
}