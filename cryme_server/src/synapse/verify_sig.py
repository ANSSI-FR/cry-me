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
import requests
import urllib3
import json
from Crypto.Hash import SHA1
from Crypto.Signature import pss
from Crypto.PublicKey import RSA

if len(sys.argv) != 2:
	print(f"Usage: {sys.argv[0]} domain")
	exit(1)

SYNAPSE_SERVER_NAME = sys.argv[1]
message = f"https://{SYNAPSE_SERVER_NAME}/".encode()

urllib3.disable_warnings()
URL = f"https://{SYNAPSE_SERVER_NAME}/_matrix/client/r0/login?accreditation=True"
r = requests.get(URL, verify = False)
s = bytes.fromhex(r.json()["signature"])

sk = RSA.importKey(open("server-private-key.pem").read())
key = sk.publickey()
h = SHA1.new(message)
try:
	pss.new(key).verify(h, s)
	print("The signature is authentic.")
except (ValueError, TypeError):
    print("The signature is not authentic.")
