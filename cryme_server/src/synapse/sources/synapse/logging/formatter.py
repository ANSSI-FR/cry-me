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
# Copyright 2017 New Vector Ltd
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


import logging
import traceback
from io import StringIO


class LogFormatter(logging.Formatter):
    """Log formatter which gives more detail for exceptions

    This is the same as the standard log formatter, except that when logging
    exceptions [typically via log.foo("msg", exc_info=1)], it prints the
    sequence that led up to the point at which the exception was caught.
    (Normally only stack frames between the point the exception was raised and
    where it was caught are logged).
    """

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)

    def formatException(self, ei):
        sio = StringIO()
        (typ, val, tb) = ei

        # log the stack above the exception capture point if possible, but
        # check that we actually have an f_back attribute to work around
        # https://twistedmatrix.com/trac/ticket/9305

        if tb and hasattr(tb.tb_frame, "f_back"):
            sio.write("Capture point (most recent call last):\n")
            traceback.print_stack(tb.tb_frame.f_back, None, sio)

        traceback.print_exception(typ, val, tb, None, sio)
        s = sio.getvalue()
        sio.close()
        if s[-1:] == "\n":
            s = s[:-1]
        return s
