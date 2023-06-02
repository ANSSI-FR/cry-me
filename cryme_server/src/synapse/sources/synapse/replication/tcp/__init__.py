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
# Copyright 2017 Vector Creations Ltd
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""This module implements the TCP replication protocol used by synapse to
communicate between the master process and its workers (when they're enabled).

Further details can be found in docs/tcp_replication.rst


Structure of the module:
 * handler.py  - the classes used to handle sending/receiving commands to
                 replication
 * command.py  - the definitions of all the valid commands
 * protocol.py - the TCP protocol classes
 * resource.py - handles streaming stream updates to replications
 * streams/    - the definitions of all the valid streams


The general interaction of the classes are:

        +---------------------+
        | ReplicationStreamer |
        +---------------------+
                    |
                    v
        +---------------------------+     +----------------------+
        | ReplicationCommandHandler |---->|ReplicationDataHandler|
        +---------------------------+     +----------------------+
                    | ^
                    v |
            +-------------+
            | Protocols   |
            | (TCP/redis) |
            +-------------+

Where the ReplicationDataHandler (or subclasses) handles incoming stream
updates.
"""
