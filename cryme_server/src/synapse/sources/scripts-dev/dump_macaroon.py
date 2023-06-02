#!/usr/bin/env python
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

import sys

import pymacaroons

if len(sys.argv) == 1:
    sys.stderr.write("usage: %s macaroon [key]\n" % (sys.argv[0],))
    sys.exit(1)

macaroon_string = sys.argv[1]
key = sys.argv[2] if len(sys.argv) > 2 else None

macaroon = pymacaroons.Macaroon.deserialize(macaroon_string)
print(macaroon.inspect())

print("")

verifier = pymacaroons.Verifier()
verifier.satisfy_general(lambda c: True)
try:
    verifier.verify(macaroon, key)
    print("Signature is correct")
except Exception as e:
    print(str(e))
