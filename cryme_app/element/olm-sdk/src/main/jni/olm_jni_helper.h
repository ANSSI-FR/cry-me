/*************************** The CRY.ME project (2023) *************************************************
 *
 *  This file is part of the CRY.ME project (https://github.com/ANSSI-FR/cry-me).
 *  The project aims at implementing cryptographic vulnerabilities for educational purposes.
 *  Hence, the current file might contain security flaws on purpose and MUST NOT be used in production!
 *  Please do not use this source code outside this scope, or use it knowingly.
 *
 *  Many files come from the Android element (https://github.com/vector-im/element-android), the
 *  Matrix SDK (https://github.com/matrix-org/matrix-android-sdk2) as well as the Android Yubikit
 *  (https://github.com/Yubico/yubikit-android) projects and have been willingly modified
 *  for the CRY.ME project purposes. The Android element, Matrix SDK and Yubikit projects are distributed
 *  under the Apache-2.0 license, and so is the CRY.ME project.
 *
 ***************************  (END OF CRY.ME HEADER)   *************************************************/

/*
 * Copyright 2016 OpenMarket Ltd
 * Copyright 2016,2018,2019 Vector Creations Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "olm_jni.h"

// constant strings
namespace AndroidOlmSdk
{
    static const char *CLASS_OLM_INBOUND_GROUP_SESSION = "org/matrix/olm/OlmInboundGroupSession";
    static const char *CLASS_OLM_OUTBOUND_GROUP_SESSION = "org/matrix/olm/OlmOutboundGroupSession";
    static const char *CLASS_OLM_SESSION = "org/matrix/olm/OlmSession";
    static const char *CLASS_OLM_ACCOUNT = "org/matrix/olm/OlmAccount";
    static const char *CLASS_OLM_UTILITY = "org/matrix/olm/OlmUtility";
    static const char *CLASS_OLM_ATTACHMENT_UTILITY = "org/matrix/olm/OlmAttachmentUtility";
    static const char *CLASS_OLM_RSA_UTILITY = "org/matrix/olm/OlmRSAUtility";
    static const char *CLASS_OLM_PRG_UTILITY = "org/matrix/olm/OlmPRGUtility";
    static const char *CLASS_OLM_PK_ENCRYPTION = "org/matrix/olm/OlmPkEncryption";
    static const char *CLASS_OLM_PK_DECRYPTION = "org/matrix/olm/OlmPkDecryption";
    static const char *CLASS_OLM_PK_SIGNING = "org/matrix/olm/OlmPkSigning";
    static const char *CLASS_OLM_SAS = "org/matrix/olm/OlmSAS";
}
