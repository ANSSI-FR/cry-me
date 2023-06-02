#! /usr/bin/python
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

import argparse
import ast
import os
import sys

import yaml

PATTERNS_V1 = []
PATTERNS_V2 = []

RESULT = {"v1": PATTERNS_V1, "v2": PATTERNS_V2}


class CallVisitor(ast.NodeVisitor):
    def visit_Call(self, node):
        if isinstance(node.func, ast.Name):
            name = node.func.id
        else:
            return

        if name == "client_patterns":
            PATTERNS_V2.append(node.args[0].s)


def find_patterns_in_code(input_code):
    input_ast = ast.parse(input_code)
    visitor = CallVisitor()
    visitor.visit(input_ast)


def find_patterns_in_file(filepath):
    with open(filepath) as f:
        find_patterns_in_code(f.read())


parser = argparse.ArgumentParser(description="Find url patterns.")

parser.add_argument(
    "directories",
    nargs="+",
    metavar="DIR",
    help="Directories to search for definitions",
)

args = parser.parse_args()


for directory in args.directories:
    for root, _, files in os.walk(directory):
        for filename in files:
            if filename.endswith(".py"):
                filepath = os.path.join(root, filename)
                find_patterns_in_file(filepath)

PATTERNS_V1.sort()
PATTERNS_V2.sort()

yaml.dump(RESULT, sys.stdout, default_flow_style=False)
