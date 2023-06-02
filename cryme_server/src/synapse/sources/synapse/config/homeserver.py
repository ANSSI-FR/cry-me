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
# Copyright 2021 The Matrix.org Foundation C.I.C.
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
from ._base import RootConfig
from .account_validity import AccountValidityConfig
from .api import ApiConfig
from .appservice import AppServiceConfig
from .auth import AuthConfig
from .cache import CacheConfig
from .captcha import CaptchaConfig
from .cas import CasConfig
from .consent import ConsentConfig
from .database import DatabaseConfig
from .emailconfig import EmailConfig
from .experimental import ExperimentalConfig
from .federation import FederationConfig
from .groups import GroupsConfig
from .jwt import JWTConfig
from .key import KeyConfig
from .logger import LoggingConfig
from .metrics import MetricsConfig
from .modules import ModulesConfig
from .oembed import OembedConfig
from .oidc import OIDCConfig
from .password_auth_providers import PasswordAuthProviderConfig
from .push import PushConfig
from .ratelimiting import RatelimitConfig
from .redis import RedisConfig
from .registration import RegistrationConfig
from .repository import ContentRepositoryConfig
from .retention import RetentionConfig
from .room import RoomConfig
from .room_directory import RoomDirectoryConfig
from .saml2 import SAML2Config
from .server import ServerConfig
from .server_notices import ServerNoticesConfig
from .spam_checker import SpamCheckerConfig
from .sso import SSOConfig
from .stats import StatsConfig
from .third_party_event_rules import ThirdPartyRulesConfig
from .tls import TlsConfig
from .tracer import TracerConfig
from .user_directory import UserDirectoryConfig
from .voip import VoipConfig
from .workers import WorkerConfig


class HomeServerConfig(RootConfig):

    config_classes = [
        ModulesConfig,
        ServerConfig,
        RetentionConfig,
        TlsConfig,
        FederationConfig,
        CacheConfig,
        DatabaseConfig,
        LoggingConfig,
        RatelimitConfig,
        ContentRepositoryConfig,
        OembedConfig,
        CaptchaConfig,
        VoipConfig,
        RegistrationConfig,
        AccountValidityConfig,
        MetricsConfig,
        ApiConfig,
        AppServiceConfig,
        KeyConfig,
        SAML2Config,
        OIDCConfig,
        CasConfig,
        SSOConfig,
        JWTConfig,
        AuthConfig,
        EmailConfig,
        PasswordAuthProviderConfig,
        PushConfig,
        SpamCheckerConfig,
        RoomConfig,
        GroupsConfig,
        UserDirectoryConfig,
        ConsentConfig,
        StatsConfig,
        ServerNoticesConfig,
        RoomDirectoryConfig,
        ThirdPartyRulesConfig,
        TracerConfig,
        WorkerConfig,
        RedisConfig,
        ExperimentalConfig,
    ]
