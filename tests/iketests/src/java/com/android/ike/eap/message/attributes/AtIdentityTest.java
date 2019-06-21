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

package com.android.ike.eap.message.attributes;

import static com.android.ike.eap.message.EapSimAttribute.EAP_AT_IDENTITY;
import static com.android.ike.eap.message.attributes.EapTestAttributeDefinitions.AT_IDENTITY;
import static com.android.ike.eap.message.attributes.EapTestAttributeDefinitions.IDENTITY;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.android.ike.eap.message.EapSimAttribute;
import com.android.ike.eap.message.EapSimAttribute.AtIdentity;
import com.android.ike.eap.message.EapSimAttributeFactory;

import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;

public class AtIdentityTest {
    private EapSimAttributeFactory mEapSimAttributeFactory;

    @Before
    public void setUp() {
        mEapSimAttributeFactory = EapSimAttributeFactory.getInstance();
    }

    @Test
    public void testDecode() throws Exception {
        ByteBuffer input = ByteBuffer.wrap(AT_IDENTITY);
        EapSimAttribute result = mEapSimAttributeFactory.getEapSimAttribute(input);

        assertFalse(input.hasRemaining());
        assertTrue(result instanceof AtIdentity);
        AtIdentity atIdentity = (AtIdentity) result;
        assertEquals(EAP_AT_IDENTITY, atIdentity.attributeType);
        assertEquals(AT_IDENTITY.length, atIdentity.lengthInBytes);
        assertArrayEquals(IDENTITY, atIdentity.identity);
    }

    @Test
    public void testEncode() throws Exception {
        AtIdentity atIdentity = new AtIdentity(AT_IDENTITY.length, IDENTITY);
        ByteBuffer result = ByteBuffer.allocate(AT_IDENTITY.length);
        atIdentity.encode(result);

        assertArrayEquals(AT_IDENTITY, result.array());
    }
}