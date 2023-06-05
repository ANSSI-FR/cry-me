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
# Copyright (c) 2012, 2013, 2014 Ilya Otyutskiy <ilya.otyutskiy@icloud.com>
# Copyright 2020 The Matrix.org Foundation C.I.C.
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

import atexit
import fcntl
import logging
import os
import signal
import sys
from types import FrameType, TracebackType
from typing import NoReturn, Type


def daemonize_process(pid_file: str, logger: logging.Logger, chdir: str = "/") -> None:
    """daemonize the current process

    This calls fork(), and has the main process exit. When it returns we will be
    running in the child process.
    """

    # If pidfile already exists, we should read pid from there; to overwrite it, if
    # locking will fail, because locking attempt somehow purges the file contents.
    if os.path.isfile(pid_file):
        with open(pid_file) as pid_fh:
            old_pid = pid_fh.read()

    # Create a lockfile so that only one instance of this daemon is running at any time.
    try:
        lock_fh = open(pid_file, "w")
    except OSError:
        print("Unable to create the pidfile.")
        sys.exit(1)

    try:
        # Try to get an exclusive lock on the file. This will fail if another process
        # has the file locked.
        fcntl.flock(lock_fh, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except OSError:
        print("Unable to lock on the pidfile.")
        # We need to overwrite the pidfile if we got here.
        #
        # XXX better to avoid overwriting it, surely. this looks racey as the pid file
        # could be created between us trying to read it and us trying to lock it.
        with open(pid_file, "w") as pid_fh:
            pid_fh.write(old_pid)
        sys.exit(1)

    # Fork, creating a new process for the child.
    process_id = os.fork()

    if process_id != 0:
        # parent process: exit.

        # we use os._exit to avoid running the atexit handlers. In particular, that
        # means we don't flush the logs. This is important because if we are using
        # a MemoryHandler, we could have logs buffered which are now buffered in both
        # the main and the child process, so if we let the main process flush the logs,
        # we'll get two copies.
        os._exit(0)

    # This is the child process. Continue.

    # Stop listening for signals that the parent process receives.
    # This is done by getting a new process id.
    # setpgrp() is an alternative to setsid().
    # setsid puts the process in a new parent group and detaches its controlling
    # terminal.

    os.setsid()

    # point stdin, stdout, stderr at /dev/null
    devnull = "/dev/null"
    if hasattr(os, "devnull"):
        # Python has set os.devnull on this system, use it instead as it might be
        # different than /dev/null.
        devnull = os.devnull

    devnull_fd = os.open(devnull, os.O_RDWR)
    os.dup2(devnull_fd, 0)
    os.dup2(devnull_fd, 1)
    os.dup2(devnull_fd, 2)
    os.close(devnull_fd)

    # now that we have redirected stderr to /dev/null, any uncaught exceptions will
    # get sent to /dev/null, so make sure we log them.
    #
    # (we don't normally expect reactor.run to raise any exceptions, but this will
    # also catch any other uncaught exceptions before we get that far.)

    def excepthook(
        type_: Type[BaseException], value: BaseException, traceback: TracebackType
    ) -> None:
        logger.critical("Unhanded exception", exc_info=(type_, value, traceback))

    sys.excepthook = excepthook

    # Set umask to default to safe file permissions when running as a root daemon. 027
    # is an octal number which we are typing as 0o27 for Python3 compatibility.
    os.umask(0o27)

    # Change to a known directory. If this isn't done, starting a daemon in a
    # subdirectory that needs to be deleted results in "directory busy" errors.
    os.chdir(chdir)

    try:
        lock_fh.write("%s" % (os.getpid()))
        lock_fh.flush()
    except OSError:
        logger.error("Unable to write pid to the pidfile.")
        print("Unable to write pid to the pidfile.")
        sys.exit(1)

    # write a log line on SIGTERM.
    def sigterm(signum: signal.Signals, frame: FrameType) -> NoReturn:
        logger.warning("Caught signal %s. Stopping daemon." % signum)
        sys.exit(0)

    signal.signal(signal.SIGTERM, sigterm)

    # Cleanup pid file at exit.
    def exit() -> None:
        logger.warning("Stopping daemon.")
        os.remove(pid_file)
        sys.exit(0)

    atexit.register(exit)

    logger.warning("Starting daemon.")
