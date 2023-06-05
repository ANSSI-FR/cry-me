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

# Copyright 2014-2017 OpenMarket Ltd
# Copyright 2017 Vector Creations Ltd
# Copyright 2017-2018 New Vector Ltd
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
import glob
import os
from typing import Any, Dict

from setuptools import Command, find_packages, setup

here = os.path.abspath(os.path.dirname(__file__))


# Some notes on `setup.py test`:
#
# Once upon a time we used to try to make `setup.py test` run `tox` to run the
# tests. That's a bad idea for three reasons:
#
# 1: `setup.py test` is supposed to find out whether the tests work in the
#    *current* environmentt, not whatever tox sets up.
# 2: Empirically, trying to install tox during the test run wasn't working ("No
#    module named virtualenv").
# 3: The tox documentation advises against it[1].
#
# Even further back in time, we used to use setuptools_trial [2]. That has its
# own set of issues: for instance, it requires installation of Twisted to build
# an sdist (because the recommended mode of usage is to add it to
# `setup_requires`). That in turn means that in order to successfully run tox
# you have to have the python header files installed for whichever version of
# python tox uses (which is python3 on recent ubuntus, for example).
#
# So, for now at least, we stick with what appears to be the convention among
# Twisted projects, and don't attempt to do anything when someone runs
# `setup.py test`; instead we direct people to run `trial` directly if they
# care.
#
# [1]: http://tox.readthedocs.io/en/2.5.0/example/basic.html#integration-with-setup-py-test-command
# [2]: https://pypi.python.org/pypi/setuptools_trial
class TestCommand(Command):
    def initialize_options(self):
        pass

    def finalize_options(self):
        pass

    def run(self):
        print(
            """Synapse's tests cannot be run via setup.py. To run them, try:
     PYTHONPATH="." trial tests
"""
        )


def read_file(path_segments):
    """Read a file from the package. Takes a list of strings to join to
    make the path"""
    file_path = os.path.join(here, *path_segments)
    with open(file_path) as f:
        return f.read()


def exec_file(path_segments):
    """Execute a single python file to get the variables defined in it"""
    result: Dict[str, Any] = {}
    code = read_file(path_segments)
    exec(code, result)
    return result


version = exec_file(("synapse", "__init__.py"))["__version__"]
dependencies = exec_file(("synapse", "python_dependencies.py"))
long_description = read_file(("README.rst",))

REQUIREMENTS = dependencies["REQUIREMENTS"]
CONDITIONAL_REQUIREMENTS = dependencies["CONDITIONAL_REQUIREMENTS"]
ALL_OPTIONAL_REQUIREMENTS = dependencies["ALL_OPTIONAL_REQUIREMENTS"]

# Make `pip install matrix-synapse[all]` install all the optional dependencies.
CONDITIONAL_REQUIREMENTS["all"] = list(ALL_OPTIONAL_REQUIREMENTS)

# Developer dependencies should not get included in "all".
#
# We pin black so that our tests don't start failing on new releases.
CONDITIONAL_REQUIREMENTS["lint"] = [
    "isort==5.7.0",
    "black==21.12b0",
    "flake8-comprehensions",
    "flake8-bugbear==21.3.2",
    "flake8",
]

CONDITIONAL_REQUIREMENTS["mypy"] = [
    "mypy==0.910",
    "mypy-zope==0.3.2",
    "types-bleach>=4.1.0",
    "types-jsonschema>=3.2.0",
    "types-opentracing>=2.4.2",
    "types-Pillow>=8.3.4",
    "types-pyOpenSSL>=20.0.7",
    "types-PyYAML>=5.4.10",
    "types-requests>=2.26.0",
    "types-setuptools>=57.4.0",
]

# Dependencies which are exclusively required by unit test code. This is
# NOT a list of all modules that are necessary to run the unit tests.
# Tests assume that all optional dependencies are installed.
#
# parameterized_class decorator was introduced in parameterized 0.7.0
CONDITIONAL_REQUIREMENTS["test"] = ["parameterized>=0.7.0"]

CONDITIONAL_REQUIREMENTS["dev"] = (
    CONDITIONAL_REQUIREMENTS["lint"]
    + CONDITIONAL_REQUIREMENTS["mypy"]
    + CONDITIONAL_REQUIREMENTS["test"]
    + [
        # The following are used by the release script
        "click==7.1.2",
        "redbaron==0.9.2",
        "GitPython==3.1.14",
        "commonmark==0.9.1",
        "pygithub==1.55",
        # The following are executed as commands by the release script.
        "twine",
        "towncrier",
    ]
)

setup(
    name="matrix-synapse",
    version=version,
    packages=find_packages(exclude=["tests", "tests.*"]),
    description="Reference homeserver for the Matrix decentralised comms protocol",
    install_requires=REQUIREMENTS,
    extras_require=CONDITIONAL_REQUIREMENTS,
    include_package_data=True,
    zip_safe=False,
    long_description=long_description,
    long_description_content_type="text/x-rst",
    python_requires="~=3.6",
    entry_points={
        "console_scripts": [
            "synapse_homeserver = synapse.app.homeserver:main",
            "synapse_worker = synapse.app.generic_worker:main",
        ]
    },
    classifiers=[
        "Development Status :: 5 - Production/Stable",
        "Topic :: Communications :: Chat",
        "License :: OSI Approved :: Apache Software License",
        "Programming Language :: Python :: 3 :: Only",
        "Programming Language :: Python :: 3.7",
        "Programming Language :: Python :: 3.8",
        "Programming Language :: Python :: 3.9",
    ],
    scripts=["synctl"] + glob.glob("scripts/*"),
    cmdclass={"test": TestCommand},
)
