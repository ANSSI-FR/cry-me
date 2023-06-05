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
# Copyright 2016 OpenMarket Ltd
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

import copy
from typing import Any, Dict, List, Optional

from synapse.push.rulekinds import PRIORITY_CLASS_INVERSE_MAP, PRIORITY_CLASS_MAP
from synapse.types import UserID


def format_push_rules_for_user(
    user: UserID, ruleslist: List
) -> Dict[str, Dict[str, list]]:
    """Converts a list of rawrules and a enabled map into nested dictionaries
    to match the Matrix client-server format for push rules"""

    # We're going to be mutating this a lot, so do a deep copy
    ruleslist = copy.deepcopy(ruleslist)

    rules: Dict[str, Dict[str, List[Dict[str, Any]]]] = {
        "global": {},
        "device": {},
    }

    rules["global"] = _add_empty_priority_class_arrays(rules["global"])

    for r in ruleslist:
        template_name = _priority_class_to_template_name(r["priority_class"])

        # Remove internal stuff.
        for c in r["conditions"]:
            c.pop("_id", None)

            pattern_type = c.pop("pattern_type", None)
            if pattern_type == "user_id":
                c["pattern"] = user.to_string()
            elif pattern_type == "user_localpart":
                c["pattern"] = user.localpart

        rulearray = rules["global"][template_name]

        template_rule = _rule_to_template(r)
        if template_rule:
            if "enabled" in r:
                template_rule["enabled"] = r["enabled"]
            else:
                template_rule["enabled"] = True
            rulearray.append(template_rule)

    return rules


def _add_empty_priority_class_arrays(d: Dict[str, list]) -> Dict[str, list]:
    for pc in PRIORITY_CLASS_MAP.keys():
        d[pc] = []
    return d


def _rule_to_template(rule: Dict[str, Any]) -> Optional[Dict[str, Any]]:
    unscoped_rule_id = None
    if "rule_id" in rule:
        unscoped_rule_id = _rule_id_from_namespaced(rule["rule_id"])

    template_name = _priority_class_to_template_name(rule["priority_class"])
    if template_name in ["override", "underride"]:
        templaterule = {k: rule[k] for k in ["conditions", "actions"]}
    elif template_name in ["sender", "room"]:
        templaterule = {"actions": rule["actions"]}
        unscoped_rule_id = rule["conditions"][0]["pattern"]
    elif template_name == "content":
        if len(rule["conditions"]) != 1:
            return None
        thecond = rule["conditions"][0]
        if "pattern" not in thecond:
            return None
        templaterule = {"actions": rule["actions"]}
        templaterule["pattern"] = thecond["pattern"]
    else:
        # This should not be reached unless this function is not kept in sync
        # with PRIORITY_CLASS_INVERSE_MAP.
        raise ValueError("Unexpected template_name: %s" % (template_name,))

    if unscoped_rule_id:
        templaterule["rule_id"] = unscoped_rule_id
    if "default" in rule:
        templaterule["default"] = rule["default"]
    return templaterule


def _rule_id_from_namespaced(in_rule_id: str) -> str:
    return in_rule_id.split("/")[-1]


def _priority_class_to_template_name(pc: int) -> str:
    return PRIORITY_CLASS_INVERSE_MAP[pc]
