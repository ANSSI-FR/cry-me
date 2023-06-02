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
# Copyright 2021 Šimon Brandner <simon.bra.ag@gmail.com>
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


from typing import List

from synapse.api.constants import ReadReceiptEventFields
from synapse.types import JsonDict

from tests import unittest


class ReceiptsTestCase(unittest.HomeserverTestCase):
    def prepare(self, reactor, clock, hs):
        self.event_source = hs.get_event_sources().sources.receipt

    # In the first param of _test_filters_hidden we use "hidden" instead of
    # ReadReceiptEventFields.MSC2285_HIDDEN. We do this because we're mocking
    # the data from the database which doesn't use the prefix

    def test_filters_out_hidden_receipt(self):
        self._test_filters_hidden(
            [
                {
                    "content": {
                        "$1435641916114394fHBLK:matrix.org": {
                            "m.read": {
                                "@rikj:jki.re": {
                                    "ts": 1436451550453,
                                    "hidden": True,
                                }
                            }
                        }
                    },
                    "room_id": "!jEsUZKDJdhlrceRyVU:example.org",
                    "type": "m.receipt",
                }
            ],
            [],
        )

    def test_does_not_filter_out_our_hidden_receipt(self):
        self._test_filters_hidden(
            [
                {
                    "content": {
                        "$1435641916hfgh4394fHBLK:matrix.org": {
                            "m.read": {
                                "@me:server.org": {
                                    "ts": 1436451550453,
                                    "hidden": True,
                                },
                            }
                        }
                    },
                    "room_id": "!jEsUZKDJdhlrceRyVU:example.org",
                    "type": "m.receipt",
                }
            ],
            [
                {
                    "content": {
                        "$1435641916hfgh4394fHBLK:matrix.org": {
                            "m.read": {
                                "@me:server.org": {
                                    "ts": 1436451550453,
                                    ReadReceiptEventFields.MSC2285_HIDDEN: True,
                                },
                            }
                        }
                    },
                    "room_id": "!jEsUZKDJdhlrceRyVU:example.org",
                    "type": "m.receipt",
                }
            ],
        )

    def test_filters_out_hidden_receipt_and_ignores_rest(self):
        self._test_filters_hidden(
            [
                {
                    "content": {
                        "$1dgdgrd5641916114394fHBLK:matrix.org": {
                            "m.read": {
                                "@rikj:jki.re": {
                                    "ts": 1436451550453,
                                    "hidden": True,
                                },
                                "@user:jki.re": {
                                    "ts": 1436451550453,
                                },
                            }
                        }
                    },
                    "room_id": "!jEsUZKDJdhlrceRyVU:example.org",
                    "type": "m.receipt",
                }
            ],
            [
                {
                    "content": {
                        "$1dgdgrd5641916114394fHBLK:matrix.org": {
                            "m.read": {
                                "@user:jki.re": {
                                    "ts": 1436451550453,
                                }
                            }
                        }
                    },
                    "room_id": "!jEsUZKDJdhlrceRyVU:example.org",
                    "type": "m.receipt",
                }
            ],
        )

    def test_filters_out_event_with_only_hidden_receipts_and_ignores_the_rest(self):
        self._test_filters_hidden(
            [
                {
                    "content": {
                        "$14356419edgd14394fHBLK:matrix.org": {
                            "m.read": {
                                "@rikj:jki.re": {
                                    "ts": 1436451550453,
                                    "hidden": True,
                                },
                            }
                        },
                        "$1435641916114394fHBLK:matrix.org": {
                            "m.read": {
                                "@user:jki.re": {
                                    "ts": 1436451550453,
                                }
                            }
                        },
                    },
                    "room_id": "!jEsUZKDJdhlrceRyVU:example.org",
                    "type": "m.receipt",
                }
            ],
            [
                {
                    "content": {
                        "$1435641916114394fHBLK:matrix.org": {
                            "m.read": {
                                "@user:jki.re": {
                                    "ts": 1436451550453,
                                }
                            }
                        }
                    },
                    "room_id": "!jEsUZKDJdhlrceRyVU:example.org",
                    "type": "m.receipt",
                }
            ],
        )

    def test_handles_missing_content_of_m_read(self):
        self._test_filters_hidden(
            [
                {
                    "content": {
                        "$14356419ggffg114394fHBLK:matrix.org": {"m.read": {}},
                        "$1435641916114394fHBLK:matrix.org": {
                            "m.read": {
                                "@user:jki.re": {
                                    "ts": 1436451550453,
                                }
                            }
                        },
                    },
                    "room_id": "!jEsUZKDJdhlrceRyVU:example.org",
                    "type": "m.receipt",
                }
            ],
            [
                {
                    "content": {
                        "$14356419ggffg114394fHBLK:matrix.org": {"m.read": {}},
                        "$1435641916114394fHBLK:matrix.org": {
                            "m.read": {
                                "@user:jki.re": {
                                    "ts": 1436451550453,
                                }
                            }
                        },
                    },
                    "room_id": "!jEsUZKDJdhlrceRyVU:example.org",
                    "type": "m.receipt",
                }
            ],
        )

    def test_handles_empty_event(self):
        self._test_filters_hidden(
            [
                {
                    "content": {
                        "$143564gdfg6114394fHBLK:matrix.org": {},
                        "$1435641916114394fHBLK:matrix.org": {
                            "m.read": {
                                "@user:jki.re": {
                                    "ts": 1436451550453,
                                }
                            }
                        },
                    },
                    "room_id": "!jEsUZKDJdhlrceRyVU:example.org",
                    "type": "m.receipt",
                }
            ],
            [
                {
                    "content": {
                        "$143564gdfg6114394fHBLK:matrix.org": {},
                        "$1435641916114394fHBLK:matrix.org": {
                            "m.read": {
                                "@user:jki.re": {
                                    "ts": 1436451550453,
                                }
                            }
                        },
                    },
                    "room_id": "!jEsUZKDJdhlrceRyVU:example.org",
                    "type": "m.receipt",
                }
            ],
        )

    def test_filters_out_receipt_event_with_only_hidden_receipt_and_ignores_rest(self):
        self._test_filters_hidden(
            [
                {
                    "content": {
                        "$14356419edgd14394fHBLK:matrix.org": {
                            "m.read": {
                                "@rikj:jki.re": {
                                    "ts": 1436451550453,
                                    "hidden": True,
                                },
                            }
                        },
                    },
                    "room_id": "!jEsUZKDJdhlrceRyVU:example.org",
                    "type": "m.receipt",
                },
                {
                    "content": {
                        "$1435641916114394fHBLK:matrix.org": {
                            "m.read": {
                                "@user:jki.re": {
                                    "ts": 1436451550453,
                                }
                            }
                        },
                    },
                    "room_id": "!jEsUZKDJdhlrceRyVU:example.org",
                    "type": "m.receipt",
                },
            ],
            [
                {
                    "content": {
                        "$1435641916114394fHBLK:matrix.org": {
                            "m.read": {
                                "@user:jki.re": {
                                    "ts": 1436451550453,
                                }
                            }
                        }
                    },
                    "room_id": "!jEsUZKDJdhlrceRyVU:example.org",
                    "type": "m.receipt",
                }
            ],
        )

    def test_handles_string_data(self):
        """
        Tests that an invalid shape for read-receipts is handled.
        Context: https://github.com/matrix-org/synapse/issues/10603
        """

        self._test_filters_hidden(
            [
                {
                    "content": {
                        "$14356419edgd14394fHBLK:matrix.org": {
                            "m.read": {
                                "@rikj:jki.re": "string",
                            }
                        },
                    },
                    "room_id": "!jEsUZKDJdhlrceRyVU:example.org",
                    "type": "m.receipt",
                },
            ],
            [],
        )

    def _test_filters_hidden(
        self, events: List[JsonDict], expected_output: List[JsonDict]
    ):
        """Tests that the _filter_out_hidden returns the expected output"""
        filtered_events = self.event_source.filter_out_hidden(events, "@me:server.org")
        self.assertEquals(filtered_events, expected_output)
