#/*************************** The CRY.ME project (2023) *************************************************
# *
# *  This file is part of the CRY.ME project (https://github.com/ANSSI-FR/cry-me).
# *  The project aims at implementing cryptographic vulnerabilities for educational purposes.
# *  Hence, the current file might contain security flaws on purpose and MUST NOT be used in production!
# *  Please do not use this source code outside this scope, or use it knowingly.
# *
# *  Many files come from the Android element (https://github.com/vector-im/element-android), the
# *  Matrix SDK (https://github.com/matrix-org/matrix-android-sdk2) as well as the Android Yubikit
# *  (https://github.com/Yubico/yubikit-android) projects and have been willingly modified
# *  for the CRY.ME project purposes. The Android element, Matrix SDK and Yubikit projects are distributed
# *  under the Apache-2.0 license, and so is the CRY.ME project.
# *
# ***************************  (END OF CRY.ME HEADER)   *************************************************/
#
import os
from Crypto.Hash import SHA1
from Crypto.Signature import pss
from Crypto.PublicKey import RSA

with open('/app/synapse/server-private-key.pem') as fp:
    key = RSA.import_key(fp.read())

SYNAPSE_SERVER_NAME = os.getenv("SYNAPSE_SERVER_NAME")
message = f"https://{SYNAPSE_SERVER_NAME}/".encode()
h = SHA1.new(message)
signature = pss.new(key).sign(h)

with open('/app/server-signature.txt', 'w') as fp:
    fp.write(signature.hex() + "\n")
