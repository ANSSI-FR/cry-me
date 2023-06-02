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
# Copyright 2014-2016 OpenMarket Ltd
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
from functools import wraps
from inspect import getcallargs
from typing import Callable, TypeVar, cast

_TIME_FUNC_ID = 0


def _log_debug_as_f(f, msg, msg_args):
    name = f.__module__
    logger = logging.getLogger(name)

    if logger.isEnabledFor(logging.DEBUG):
        lineno = f.__code__.co_firstlineno
        pathname = f.__code__.co_filename

        record = logger.makeRecord(
            name=name,
            level=logging.DEBUG,
            fn=pathname,
            lno=lineno,
            msg=msg,
            args=msg_args,
            exc_info=None,
        )

        logger.handle(record)


F = TypeVar("F", bound=Callable)


def log_function(f: F) -> F:
    """Function decorator that logs every call to that function."""
    func_name = f.__name__

    @wraps(f)
    def wrapped(*args, **kwargs):
        name = f.__module__
        logger = logging.getLogger(name)
        level = logging.DEBUG

        if logger.isEnabledFor(level):
            bound_args = getcallargs(f, *args, **kwargs)

            def format(value):
                r = str(value)
                if len(r) > 50:
                    r = r[:50] + "..."
                return r

            func_args = ["%s=%s" % (k, format(v)) for k, v in bound_args.items()]

            msg_args = {"func_name": func_name, "args": ", ".join(func_args)}

            _log_debug_as_f(f, "Invoked '%(func_name)s' with args: %(args)s", msg_args)

        return f(*args, **kwargs)

    wrapped.__name__ = func_name
    return cast(F, wrapped)
