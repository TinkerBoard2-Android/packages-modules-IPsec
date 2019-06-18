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

package com.android.ike.eap.message;

import static com.android.ike.eap.message.EapSimAttribute.LENGTH_SCALING;
import static com.android.ike.eap.message.EapSimAttribute.SKIPPABLE_ATTRIBUTE_RANGE_START;

import android.annotation.Nullable;

import com.android.ike.eap.exceptions.EapSimInvalidAttributeException;
import com.android.ike.eap.exceptions.EapSimUnsupportedAttributeException;
import com.android.ike.eap.message.EapSimAttribute.EapSimUnsupportedAttribute;

import java.nio.ByteBuffer;

/**
 * EapSimAttributeFactory is used for creating EapSimAttributes according to their type.
 *
 * @see <a href="https://tools.ietf.org/html/rfc3748#section-4">RFC 3748, Extensible Authentication
 * Protocol (EAP)</a>
 */
public class EapSimAttributeFactory {
    private static EapSimAttributeFactory sInstance = new EapSimAttributeFactory();

    private EapSimAttributeFactory() {
    }

    public static EapSimAttributeFactory getInstance() {
        return sInstance;
    }

    /**
     * Decodes a single EapSimAttribute object from the given ByteBuffer.
     *
     * <p>Decoding logic is based on Attribute definitions in RFC 4186 Section 10.
     *
     * @param byteBuffer The ByteBuffer to parse the current attribute from
     * @return The current EapSimAttribute to be parsed, or EapSimUnsupportedAttribute if the given
     *         attributeType is skippable and unsupported
     * @throws EapSimUnsupportedAttributeException when an unsupported, unskippable Attribute is
     *         attempted to be decoded
     */
    @Nullable
    public EapSimAttribute getEapSimAttribute(ByteBuffer byteBuffer) throws
            EapSimInvalidAttributeException, EapSimUnsupportedAttributeException {
        int attributeType = Byte.toUnsignedInt(byteBuffer.get());

        // Length is given as a multiple of 4x bytes (RFC 4186 Section 8.1)
        int lengthInBytes = Byte.toUnsignedInt(byteBuffer.get()) * LENGTH_SCALING;

        switch (attributeType) {
            // TODO(b/134670528): add case statements for all EAP-SIM attributes
            default:
                if (attributeType >= SKIPPABLE_ATTRIBUTE_RANGE_START) {
                    return new EapSimUnsupportedAttribute(attributeType, lengthInBytes, byteBuffer);
                }

                throw new EapSimUnsupportedAttributeException(
                        "Unexpected EAP Attribute=" + attributeType);
        }
    }
}