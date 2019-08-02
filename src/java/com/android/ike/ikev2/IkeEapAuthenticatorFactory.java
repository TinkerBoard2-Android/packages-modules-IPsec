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
import android.os.Looper;

import com.android.ike.eap.EapAuthenticator;
import com.android.ike.eap.EapSessionConfig;
import com.android.ike.eap.IEapCallback;

/** Package private factory for building EapAuthenticator instances. */
final class IkeEapAuthenticatorFactory {
    /**
     * Builds and returns a new EapAuthenticator
     *
     * @param looper Looper for running a message loop
     * @param cbHandler Handler for posting callbacks to the given IEapCallback
     * @param cb IEapCallback for callbacks to the client
     * @param context Context for the EapAuthenticator
     */
    public EapAuthenticator newEapAuthenticator(
            Looper looper, IEapCallback cb, Context context, EapSessionConfig eapSessionConfig) {
        return new EapAuthenticator(looper, cb, context, eapSessionConfig);
    }
}