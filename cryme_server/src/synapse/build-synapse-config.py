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
import sys
import yaml
import secrets
import string

def secret_token(length = 64):
	CHARSET = string.ascii_uppercase + string.ascii_lowercase + string.punctuation
	return ''.join(secrets.choice(CHARSET) for _ in range(length))

if len(sys.argv) != 3:
	print(f"Usage: {sys.argv[0]} homeserver-template.yaml homeserver.yaml")
	exit(1)

SYNAPSE_SERVER_NAME = os.getenv("SYNAPSE_SERVER_NAME")

config = yaml.load(open(sys.argv[1]), Loader = yaml.Loader)

config["server_name"] = SYNAPSE_SERVER_NAME
config["log_config"] = f"/app/{SYNAPSE_SERVER_NAME}.log.config"
config["signing_key_path"] = f"/app/{SYNAPSE_SERVER_NAME}.key"
config["trusted_key_servers"][0]["server_name"] = SYNAPSE_SERVER_NAME

config["registration_shared_secret"] = secret_token()
config["macaroon_secret_key"] = secret_token()
config["form_secret"] = secret_token()

with open(sys.argv[2], "w") as fp:
	fp.write(yaml.dump(config))
