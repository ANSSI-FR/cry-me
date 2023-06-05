Synapse 1.49.2 (2021-12-21)
===========================

This release fixes a regression introduced in Synapse 1.49.0 which could cause `/sync` requests to take significantly longer. This would particularly affect "initial" syncs for users participating in a large number of rooms, and in extreme cases, could make it impossible for such users to log in on a new client.

**Note:** in line with our [deprecation policy](https://matrix-org.github.io/synapse/latest/deprecation_policy.html) for platform dependencies, this will be the last release to support Python 3.6 and PostgreSQL 9.6, both of which have now reached upstream end-of-life. Synapse will require Python 3.7+ and PostgreSQL 10+.

**Note:** We will also stop producing packages for Ubuntu 18.04 (Bionic Beaver) after this release, as it uses Python 3.6.

Bugfixes
--------

- Fix a performance regression in `/sync` handling, introduced in 1.49.0. ([\#11583](https://github.com/matrix-org/synapse/issues/11583))

Internal Changes
----------------

- Work around a build problem on Debian Buster. ([\#11625](https://github.com/matrix-org/synapse/issues/11625))


Synapse 1.49.1 (2021-12-21)
===========================

Not released due to problems building the debian packages.


Synapse 1.49.0 (2021-12-14)
===========================

No significant changes since version 1.49.0rc1.


Support for Ubuntu 21.04 ends next month on the 20th of January
---------------------------------------------------------------

For users of Ubuntu 21.04 (Hirsute Hippo), please be aware that [upstream support for this version of Ubuntu will end next month][Ubuntu2104EOL].
We will stop producing packages for Ubuntu 21.04 after upstream support ends.

[Ubuntu2104EOL]: https://lists.ubuntu.com/archives/ubuntu-announce/2021-December/000275.html


The wiki has been migrated to the documentation website
-------------------------------------------------------

We've decided to move the existing, somewhat stagnant pages from the GitHub wiki
to the [documentation website](https://matrix-org.github.io/synapse/latest/).

This was done for two reasons. The first was to ensure that changes are checked by
multiple authors before being committed (everyone makes mistakes!) and the second
was visibility of the documentation. Not everyone knows that Synapse has some very
useful information hidden away in its GitHub wiki pages. Bringing them to the
documentation website should help with visibility, as well as keep all Synapse documentation
in one, easily-searchable location.

Note that contributions to the documentation website happen through [GitHub pull
requests](https://github.com/matrix-org/synapse/pulls). Please visit [#synapse-dev:matrix.org](https://matrix.to/#/#synapse-dev:matrix.org)
if you need help with the process!


Synapse 1.49.0rc1 (2021-12-07)
==============================

Features
--------

- Add [MSC3030](https://github.com/matrix-org/matrix-doc/pull/3030) experimental client and federation API endpoints to get the closest event to a given timestamp. ([\#9445](https://github.com/matrix-org/synapse/issues/9445))
- Include bundled relation aggregations during a limited `/sync` request and `/relations` request, per [MSC2675](https://github.com/matrix-org/matrix-doc/pull/2675). ([\#11284](https://github.com/matrix-org/synapse/issues/11284), [\#11478](https://github.com/matrix-org/synapse/issues/11478))
- Add plugin support for controlling database background updates. ([\#11306](https://github.com/matrix-org/synapse/issues/11306), [\#11475](https://github.com/matrix-org/synapse/issues/11475), [\#11479](https://github.com/matrix-org/synapse/issues/11479))
- Support the stable API endpoints for [MSC2946](https://github.com/matrix-org/matrix-doc/pull/2946): the room `/hierarchy` endpoint. ([\#11329](https://github.com/matrix-org/synapse/issues/11329))
- Add admin API to get some information about federation status with remote servers. ([\#11407](https://github.com/matrix-org/synapse/issues/11407))
- Support expiry of refresh tokens and expiry of the overall session when refresh tokens are in use. ([\#11425](https://github.com/matrix-org/synapse/issues/11425))
- Stabilise support for [MSC2918](https://github.com/matrix-org/matrix-doc/blob/main/proposals/2918-refreshtokens.md#msc2918-refresh-tokens) refresh tokens as they have now been merged into the Matrix specification. ([\#11435](https://github.com/matrix-org/synapse/issues/11435), [\#11522](https://github.com/matrix-org/synapse/issues/11522))
- Update [MSC2918 refresh token](https://github.com/matrix-org/matrix-doc/blob/main/proposals/2918-refreshtokens.md#msc2918-refresh-tokens) support to confirm with the latest revision: accept the `refresh_tokens` parameter in the request body rather than in the URL parameters. ([\#11430](https://github.com/matrix-org/synapse/issues/11430))
- Support configuring the lifetime of non-refreshable access tokens separately to refreshable access tokens. ([\#11445](https://github.com/matrix-org/synapse/issues/11445))
- Expose `synapse_homeserver` and `synapse_worker` commands as entry points to run Synapse's main process and worker processes, respectively. Contributed by @Ma27. ([\#11449](https://github.com/matrix-org/synapse/issues/11449))
- `synctl stop` will now wait for Synapse to exit before returning. ([\#11459](https://github.com/matrix-org/synapse/issues/11459), [\#11490](https://github.com/matrix-org/synapse/issues/11490))
- Extend the "delete room" admin api to work correctly on rooms which have previously been partially deleted. ([\#11523](https://github.com/matrix-org/synapse/issues/11523))
- Add support for the `/_matrix/client/v3/login/sso/redirect/{idpId}` API from Matrix v1.1. This endpoint was overlooked when support for v3 endpoints was added in Synapse 1.48.0rc1. ([\#11451](https://github.com/matrix-org/synapse/issues/11451))


Bugfixes
--------

- Fix using [MSC2716](https://github.com/matrix-org/matrix-doc/pull/2716) batch sending in combination with event persistence workers. Contributed by @tulir at Beeper. ([\#11220](https://github.com/matrix-org/synapse/issues/11220))
- Fix a long-standing bug where all requests that read events from the database could get stuck as a result of losing the database connection, properly this time. Also fix a race condition introduced in the previous insufficient fix in Synapse 1.47.0. ([\#11376](https://github.com/matrix-org/synapse/issues/11376))
- The `/send_join` response now includes the stable `event` field instead of the unstable field from [MSC3083](https://github.com/matrix-org/matrix-doc/pull/3083). ([\#11413](https://github.com/matrix-org/synapse/issues/11413))
- Fix a bug introduced in Synapse 1.47.0 where `send_join` could fail due to an outdated `ijson` version. ([\#11439](https://github.com/matrix-org/synapse/issues/11439), [\#11441](https://github.com/matrix-org/synapse/issues/11441), [\#11460](https://github.com/matrix-org/synapse/issues/11460))
- Fix a bug introduced in Synapse 1.36.0 which could cause problems fetching event-signing keys from trusted key servers. ([\#11440](https://github.com/matrix-org/synapse/issues/11440))
- Fix a bug introduced in Synapse 1.47.1 where the media repository would fail to work if the media store path contained any symbolic links. ([\#11446](https://github.com/matrix-org/synapse/issues/11446))
- Fix an `LruCache` corruption bug, introduced in Synapse 1.38.0, that would cause certain requests to fail until the next Synapse restart. ([\#11454](https://github.com/matrix-org/synapse/issues/11454))
- Fix a long-standing bug where invites from ignored users were included in incremental syncs. ([\#11511](https://github.com/matrix-org/synapse/issues/11511))
- Fix a regression in Synapse 1.48.0 where presence workers would not clear their presence updates over replication on shutdown. ([\#11518](https://github.com/matrix-org/synapse/issues/11518))
- Fix a regression in Synapse 1.48.0 where the module API's `looping_background_call` method would spam errors to the logs when given a non-async function. ([\#11524](https://github.com/matrix-org/synapse/issues/11524))


Updates to the Docker image
---------------------------

- Update `Dockerfile-workers` to healthcheck all workers in the container. ([\#11429](https://github.com/matrix-org/synapse/issues/11429))


Improved Documentation
----------------------

- Update the media repository documentation. ([\#11415](https://github.com/matrix-org/synapse/issues/11415))
- Update section about backward extremities in the room DAG concepts doc to correct the misconception about backward extremities indicating whether we have fetched an events' `prev_events`. ([\#11469](https://github.com/matrix-org/synapse/issues/11469))


Internal Changes
----------------

- Add `Final` annotation to string constants in `synapse.api.constants` so that they get typed as `Literal`s. ([\#11356](https://github.com/matrix-org/synapse/issues/11356))
- Add a check to ensure that users cannot start the Synapse master process when `worker_app` is set. ([\#11416](https://github.com/matrix-org/synapse/issues/11416))
- Add a note about postgres memory management and hugepages to postgres doc. ([\#11467](https://github.com/matrix-org/synapse/issues/11467))
- Add missing type hints to `synapse.config` module. ([\#11465](https://github.com/matrix-org/synapse/issues/11465))
- Add missing type hints to `synapse.federation`. ([\#11483](https://github.com/matrix-org/synapse/issues/11483))
- Add type annotations to `tests.storage.test_appservice`. ([\#11488](https://github.com/matrix-org/synapse/issues/11488), [\#11492](https://github.com/matrix-org/synapse/issues/11492))
- Add type annotations to some of the configuration surrounding refresh tokens. ([\#11428](https://github.com/matrix-org/synapse/issues/11428))
- Add type hints to `synapse/tests/rest/admin`. ([\#11501](https://github.com/matrix-org/synapse/issues/11501))
- Add type hints to storage classes. ([\#11411](https://github.com/matrix-org/synapse/issues/11411))
- Add wiki pages to documentation website. ([\#11402](https://github.com/matrix-org/synapse/issues/11402))
- Clean up `tests.storage.test_main` to remove use of legacy code. ([\#11493](https://github.com/matrix-org/synapse/issues/11493))
- Clean up `tests.test_visibility` to remove legacy code. ([\#11495](https://github.com/matrix-org/synapse/issues/11495))
- Convert status codes to `HTTPStatus` in `synapse.rest.admin`. ([\#11452](https://github.com/matrix-org/synapse/issues/11452), [\#11455](https://github.com/matrix-org/synapse/issues/11455))
- Extend the `scripts-dev/sign_json` script to support signing events. ([\#11486](https://github.com/matrix-org/synapse/issues/11486))
- Improve internal types in push code. ([\#11409](https://github.com/matrix-org/synapse/issues/11409))
- Improve type annotations in `synapse.module_api`. ([\#11029](https://github.com/matrix-org/synapse/issues/11029))
- Improve type hints for `LruCache`. ([\#11453](https://github.com/matrix-org/synapse/issues/11453))
- Preparation for database schema simplifications: disambiguate queries on `state_key`. ([\#11497](https://github.com/matrix-org/synapse/issues/11497))
- Refactor `backfilled` into specific behavior function arguments (`_persist_events_and_state_updates` and downstream calls). ([\#11417](https://github.com/matrix-org/synapse/issues/11417))
- Refactor `get_version_string` to fix-up types and duplicated code. ([\#11468](https://github.com/matrix-org/synapse/issues/11468))
- Refactor various parts of the `/sync` handler. ([\#11494](https://github.com/matrix-org/synapse/issues/11494), [\#11515](https://github.com/matrix-org/synapse/issues/11515))
- Remove unnecessary `json.dumps` from `tests.rest.admin`. ([\#11461](https://github.com/matrix-org/synapse/issues/11461))
- Save the OpenID Connect session ID on login. ([\#11482](https://github.com/matrix-org/synapse/issues/11482))
- Update and clean up recently ported documentation pages. ([\#11466](https://github.com/matrix-org/synapse/issues/11466))


Synapse 1.48.0 (2021-11-30)
===========================

This release removes support for the long-deprecated `trust_identity_server_for_password_resets` configuration flag.

This release also fixes some performance issues with some background database updates introduced in Synapse 1.47.0.

No significant changes since 1.48.0rc1.

Synapse 1.48.0rc1 (2021-11-25)
==============================

Features
--------

- Experimental support for the thread relation defined in [MSC3440](https://github.com/matrix-org/matrix-doc/pull/3440). ([\#11161](https://github.com/matrix-org/synapse/issues/11161))
- Support filtering by relation senders & types per [MSC3440](https://github.com/matrix-org/matrix-doc/pull/3440). ([\#11236](https://github.com/matrix-org/synapse/issues/11236))
- Add support for the `/_matrix/client/v3` and `/_matrix/media/v3` APIs from Matrix v1.1. ([\#11318](https://github.com/matrix-org/synapse/issues/11318), [\#11371](https://github.com/matrix-org/synapse/issues/11371))
- Support the stable version of [MSC2778](https://github.com/matrix-org/matrix-doc/pull/2778): the `m.login.application_service` login type. Contributed by @tulir. ([\#11335](https://github.com/matrix-org/synapse/issues/11335))
- Add a new version of delete room admin API `DELETE /_synapse/admin/v2/rooms/<room_id>` to run it in the background. Contributed by @dklimpel. ([\#11223](https://github.com/matrix-org/synapse/issues/11223))
- Allow the admin [Delete Room API](https://matrix-org.github.io/synapse/latest/admin_api/rooms.html#delete-room-api) to block a room without the need to join it. ([\#11228](https://github.com/matrix-org/synapse/issues/11228))
- Add an admin API to un-shadow-ban a user. ([\#11347](https://github.com/matrix-org/synapse/issues/11347))
- Add an admin API to run background database schema updates. ([\#11352](https://github.com/matrix-org/synapse/issues/11352))
- Add an admin API for blocking a room. ([\#11324](https://github.com/matrix-org/synapse/issues/11324))
- Update the JWT login type to support custom a `sub` claim. ([\#11361](https://github.com/matrix-org/synapse/issues/11361))
- Store and allow querying of arbitrary event relations. ([\#11391](https://github.com/matrix-org/synapse/issues/11391))


Bugfixes
--------

- Fix a long-standing bug wherein display names or avatar URLs containing null bytes cause an internal server error when stored in the DB. ([\#11230](https://github.com/matrix-org/synapse/issues/11230))
- Prevent [MSC2716](https://github.com/matrix-org/matrix-doc/pull/2716) historical state events from being pushed to an application service via `/transactions`. ([\#11265](https://github.com/matrix-org/synapse/issues/11265))
- Fix a long-standing bug where uploading extremely thin images (e.g. 1000x1) would fail. Contributed by @Neeeflix. ([\#11288](https://github.com/matrix-org/synapse/issues/11288))
- Fix a bug, introduced in Synapse 1.46.0, which caused the `check_3pid_auth` and `on_logged_out` callbacks in legacy password authentication provider modules to not be registered. Modules using the generic module interface were not affected. ([\#11340](https://github.com/matrix-org/synapse/issues/11340))
- Fix a bug introduced in 1.41.0 where space hierarchy responses would be incorrectly reused if multiple users were to make the same request at the same time. ([\#11355](https://github.com/matrix-org/synapse/issues/11355))
- Fix a bug introduced in 1.45.0 where the `read_templates` method of the module API would error. ([\#11377](https://github.com/matrix-org/synapse/issues/11377))
- Fix an issue introduced in 1.47.0 which prevented servers re-joining rooms they had previously left, if their signing keys were replaced. ([\#11379](https://github.com/matrix-org/synapse/issues/11379))
- Fix a bug introduced in 1.13.0 where creating and publishing a room could cause errors if `room_list_publication_rules` is configured. ([\#11392](https://github.com/matrix-org/synapse/issues/11392))
- Improve performance of various background database updates. ([\#11421](https://github.com/matrix-org/synapse/issues/11421), [\#11422](https://github.com/matrix-org/synapse/issues/11422))


Improved Documentation
----------------------

- Suggest users of the Debian packages add configuration to `/etc/matrix-synapse/conf.d/` to prevent, upon upgrade, being asked to choose between their configuration and the maintainer's. ([\#11281](https://github.com/matrix-org/synapse/issues/11281))
- Fix typos in the documentation for the `username_available` admin API. Contributed by Stanislav Motylkov. ([\#11286](https://github.com/matrix-org/synapse/issues/11286))
- Add Single Sign-On, SAML and CAS pages to the documentation. ([\#11298](https://github.com/matrix-org/synapse/issues/11298))
- Change the word 'Home server' as one word 'homeserver' in documentation. ([\#11320](https://github.com/matrix-org/synapse/issues/11320))
- Fix missing quotes for wildcard domains in `federation_certificate_verification_whitelist`. ([\#11381](https://github.com/matrix-org/synapse/issues/11381))


Deprecations and Removals
-------------------------

- Remove deprecated `trust_identity_server_for_password_resets` configuration flag. ([\#11333](https://github.com/matrix-org/synapse/issues/11333), [\#11395](https://github.com/matrix-org/synapse/issues/11395))


Internal Changes
----------------

- Add type annotations to `synapse.metrics`. ([\#10847](https://github.com/matrix-org/synapse/issues/10847))
- Split out federated PDU retrieval function into a non-cached version. ([\#11242](https://github.com/matrix-org/synapse/issues/11242))
- Clean up code relating to to-device messages and sending ephemeral events to application services. ([\#11247](https://github.com/matrix-org/synapse/issues/11247))
- Fix a small typo in the error response when a relation type other than 'm.annotation' is passed to `GET /rooms/{room_id}/aggregations/{event_id}`. ([\#11278](https://github.com/matrix-org/synapse/issues/11278))
- Drop unused database tables `room_stats_historical` and `user_stats_historical`. ([\#11280](https://github.com/matrix-org/synapse/issues/11280))
- Require all files in synapse/ and tests/ to pass mypy unless specifically excluded. ([\#11282](https://github.com/matrix-org/synapse/issues/11282), [\#11285](https://github.com/matrix-org/synapse/issues/11285), [\#11359](https://github.com/matrix-org/synapse/issues/11359))
- Add missing type hints to `synapse.app`. ([\#11287](https://github.com/matrix-org/synapse/issues/11287))
- Remove unused parameters on `FederationEventHandler._check_event_auth`. ([\#11292](https://github.com/matrix-org/synapse/issues/11292))
- Add type hints to `synapse._scripts`. ([\#11297](https://github.com/matrix-org/synapse/issues/11297))
- Fix an issue which prevented the `remove_deleted_devices_from_device_inbox` background database schema update from running when updating from a recent Synapse version. ([\#11303](https://github.com/matrix-org/synapse/issues/11303))
- Add type hints to storage classes. ([\#11307](https://github.com/matrix-org/synapse/issues/11307), [\#11310](https://github.com/matrix-org/synapse/issues/11310), [\#11311](https://github.com/matrix-org/synapse/issues/11311), [\#11312](https://github.com/matrix-org/synapse/issues/11312), [\#11313](https://github.com/matrix-org/synapse/issues/11313), [\#11314](https://github.com/matrix-org/synapse/issues/11314), [\#11316](https://github.com/matrix-org/synapse/issues/11316), [\#11322](https://github.com/matrix-org/synapse/issues/11322), [\#11332](https://github.com/matrix-org/synapse/issues/11332), [\#11339](https://github.com/matrix-org/synapse/issues/11339), [\#11342](https://github.com/matrix-org/synapse/issues/11342))
- Add type hints to `synapse.util`. ([\#11321](https://github.com/matrix-org/synapse/issues/11321), [\#11328](https://github.com/matrix-org/synapse/issues/11328))
- Improve type annotations in Synapse's test suite. ([\#11323](https://github.com/matrix-org/synapse/issues/11323), [\#11330](https://github.com/matrix-org/synapse/issues/11330))
- Test that room alias deletion works as intended. ([\#11327](https://github.com/matrix-org/synapse/issues/11327))
- Add type annotations for some methods and properties in the module API. ([\#11341](https://github.com/matrix-org/synapse/issues/11341))
- Fix running `scripts-dev/complement.sh`, which was broken in v1.47.0rc1. ([\#11368](https://github.com/matrix-org/synapse/issues/11368))
- Rename internal functions for token generation to better reflect what they do. ([\#11369](https://github.com/matrix-org/synapse/issues/11369), [\#11370](https://github.com/matrix-org/synapse/issues/11370))
- Add type hints to configuration classes. ([\#11377](https://github.com/matrix-org/synapse/issues/11377))
- Publish a `develop` image to Docker Hub. ([\#11380](https://github.com/matrix-org/synapse/issues/11380))
- Keep fallback key marked as used if it's re-uploaded. ([\#11382](https://github.com/matrix-org/synapse/issues/11382))
- Use `auto_attribs` on the `attrs` class `RefreshTokenLookupResult`. ([\#11386](https://github.com/matrix-org/synapse/issues/11386))
- Rename unstable `access_token_lifetime` configuration option to `refreshable_access_token_lifetime` to make it clear it only concerns refreshable access tokens. ([\#11388](https://github.com/matrix-org/synapse/issues/11388))
- Do not run the broken MSC2716 tests when running `scripts-dev/complement.sh`. ([\#11389](https://github.com/matrix-org/synapse/issues/11389))
- Remove dead code from supporting ACME. ([\#11393](https://github.com/matrix-org/synapse/issues/11393))
- Refactor including the bundled relations when serializing an event. ([\#11408](https://github.com/matrix-org/synapse/issues/11408))


Synapse 1.47.1 (2021-11-23)
===========================

This release fixes a security issue in the media store, affecting all prior releases of Synapse. Server administrators are encouraged to update Synapse as soon as possible. We are not aware of these vulnerabilities being exploited in the wild.

Server administrators who are unable to update Synapse may use the workarounds described in the linked GitHub Security Advisory below.

Security advisory
-----------------

The following issue is fixed in 1.47.1.

- **[GHSA-3hfw-x7gx-437c](https://github.com/matrix-org/synapse/security/advisories/GHSA-3hfw-x7gx-437c) / [CVE-2021-41281](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-41281): Path traversal when downloading remote media.**

  Synapse instances with the media repository enabled can be tricked into downloading a file from a remote server into an arbitrary directory, potentially outside the media store directory.

  The last two directories and file name of the path are chosen randomly by Synapse and cannot be controlled by an attacker, which limits the impact.

  Homeservers with the media repository disabled are unaffected. Homeservers configured with a federation whitelist are also unaffected.

  Fixed by [91f2bd090](https://github.com/matrix-org/synapse/commit/91f2bd090).


Synapse 1.47.0 (2021-11-17)
===========================

No significant changes since 1.47.0rc3.


Synapse 1.47.0rc3 (2021-11-16)
==============================

Bugfixes
--------

- Fix a bug introduced in 1.47.0rc1 which caused worker processes to not halt startup in the presence of outstanding database migrations. ([\#11346](https://github.com/matrix-org/synapse/issues/11346))
- Fix a bug introduced in 1.47.0rc1 which prevented the 'remove deleted devices from `device_inbox` column' background process from running when updating from a recent Synapse version. ([\#11303](https://github.com/matrix-org/synapse/issues/11303), [\#11353](https://github.com/matrix-org/synapse/issues/11353))


Synapse 1.47.0rc2 (2021-11-10)
==============================

This fixes an issue with publishing the Debian packages for 1.47.0rc1.
It is otherwise identical to 1.47.0rc1.


Synapse 1.47.0rc1 (2021-11-09)
==============================

Deprecations and Removals
-------------------------

- The `user_may_create_room_with_invites` module callback is now deprecated. Please refer to the [upgrade notes](https://matrix-org.github.io/synapse/develop/upgrade#upgrading-to-v1470) for more information. ([\#11206](https://github.com/matrix-org/synapse/issues/11206))
- Remove deprecated admin API to delete rooms (`POST /_synapse/admin/v1/rooms/<room_id>/delete`). ([\#11213](https://github.com/matrix-org/synapse/issues/11213))


Features
--------

- Advertise support for Client-Server API r0.6.1. ([\#11097](https://github.com/matrix-org/synapse/issues/11097))
- Add search by room ID and room alias to the List Room admin API. ([\#11099](https://github.com/matrix-org/synapse/issues/11099))
- Add an `on_new_event` third-party rules callback to allow Synapse modules to act after an event has been sent into a room. ([\#11126](https://github.com/matrix-org/synapse/issues/11126))
- Add a module API method to update a user's membership in a room. ([\#11147](https://github.com/matrix-org/synapse/issues/11147))
- Add metrics for thread pool usage. ([\#11178](https://github.com/matrix-org/synapse/issues/11178))
- Support the stable room type field for [MSC3288](https://github.com/matrix-org/matrix-doc/pull/3288). ([\#11187](https://github.com/matrix-org/synapse/issues/11187))
- Add a module API method to retrieve the current state of a room. ([\#11204](https://github.com/matrix-org/synapse/issues/11204))
- Calculate a default value for `public_baseurl` based on `server_name`. ([\#11210](https://github.com/matrix-org/synapse/issues/11210))
- Add support for serving `/.well-known/matrix/server` files, to redirect federation traffic to port 443. ([\#11211](https://github.com/matrix-org/synapse/issues/11211))
- Add admin APIs to pause, start and check the status of background updates. ([\#11263](https://github.com/matrix-org/synapse/issues/11263))


Bugfixes
--------

- Fix a long-standing bug which allowed hidden devices to receive to-device messages, resulting in unnecessary database bloat. ([\#10097](https://github.com/matrix-org/synapse/issues/10097))
- Fix a long-standing bug where messages in the `device_inbox` table for deleted devices would persist indefinitely. Contributed by @dklimpel and @JohannesKleine. ([\#10969](https://github.com/matrix-org/synapse/issues/10969), [\#11212](https://github.com/matrix-org/synapse/issues/11212))
- Do not accept events if a third-party rule `check_event_allowed` callback raises an exception. ([\#11033](https://github.com/matrix-org/synapse/issues/11033))
- Fix long-standing bug where verification requests could fail in certain cases if a federation whitelist was in place but did not include your own homeserver. ([\#11129](https://github.com/matrix-org/synapse/issues/11129))
- Allow an empty list of `state_events_at_start` to be sent when using the [MSC2716](https://github.com/matrix-org/matrix-doc/pull/2716) `/batch_send` endpoint and the author of the historical messages is already part of the current room state at the given `?prev_event_id`. ([\#11188](https://github.com/matrix-org/synapse/issues/11188))
- Fix a bug introduced in Synapse 1.45.0 which prevented the `synapse_review_recent_signups` script from running. Contributed by @samuel-p. ([\#11191](https://github.com/matrix-org/synapse/issues/11191))
- Delete `to_device` messages for hidden devices that will never be read, reducing database size. ([\#11199](https://github.com/matrix-org/synapse/issues/11199))
- Fix a long-standing bug wherein a missing `Content-Type` header when downloading remote media would cause Synapse to throw an error. ([\#11200](https://github.com/matrix-org/synapse/issues/11200))
- Fix a long-standing bug which could result in serialization errors and potentially duplicate transaction data when sending ephemeral events to application services. Contributed by @Fizzadar at Beeper. ([\#11207](https://github.com/matrix-org/synapse/issues/11207))
- Fix a bug introduced in Synapse 1.35.0 which made it impossible to join rooms that return a `send_join` response containing floats. ([\#11217](https://github.com/matrix-org/synapse/issues/11217))
- Fix long-standing bug where cross signing keys were not included in the response to `/r0/keys/query` the first time a remote user was queried. ([\#11234](https://github.com/matrix-org/synapse/issues/11234))
- Fix a long-standing bug where all requests that read events from the database could get stuck as a result of losing the database connection. ([\#11240](https://github.com/matrix-org/synapse/issues/11240))
- Fix a bug preventing Synapse from being rolled back to an earlier version when using workers. ([\#11255](https://github.com/matrix-org/synapse/issues/11255), [\#11276](https://github.com/matrix-org/synapse/issues/11276))
- Fix a bug introduced in Synapse 1.37.1 which caused a remote event being processed by a worker to not get processed on restart if the worker was killed. ([\#11262](https://github.com/matrix-org/synapse/issues/11262))
- Only allow old Element/Riot Android clients to send read receipts without a request body. All other clients must include a request body as required by the specification. Contributed by @rogersheu. ([\#11157](https://github.com/matrix-org/synapse/issues/11157))


Updates to the Docker image
---------------------------

- Avoid changing user ID when started as a non-root user, and no explicit `UID` is set. ([\#11209](https://github.com/matrix-org/synapse/issues/11209))


Improved Documentation
----------------------

- Improve example HAProxy config in the docs to properly handle HTTP `Host` headers with port information. This is required for federation over port 443 to work correctly. ([\#11128](https://github.com/matrix-org/synapse/issues/11128))
- Add documentation for using Authentik as an OpenID Connect Identity Provider. Contributed by @samip5. ([\#11151](https://github.com/matrix-org/synapse/issues/11151))
- Clarify lack of support for Windows. ([\#11198](https://github.com/matrix-org/synapse/issues/11198))
- Improve code formatting and fix a few typos in docs. Contributed by @sumnerevans at Beeper. ([\#11221](https://github.com/matrix-org/synapse/issues/11221))
- Add documentation for using LemonLDAP as an OpenID Connect Identity Provider. Contributed by @l00ptr. ([\#11257](https://github.com/matrix-org/synapse/issues/11257))


Internal Changes
----------------

- Add type annotations for the `log_function` decorator. ([\#10943](https://github.com/matrix-org/synapse/issues/10943))
- Add type hints to `synapse.events`. ([\#11098](https://github.com/matrix-org/synapse/issues/11098))
- Remove and document unnecessary `RoomStreamToken` checks in application service ephemeral event code. ([\#11137](https://github.com/matrix-org/synapse/issues/11137))
- Add type hints so that `synapse.http` passes `mypy` checks. ([\#11164](https://github.com/matrix-org/synapse/issues/11164))
- Update scripts to pass Shellcheck lints. ([\#11166](https://github.com/matrix-org/synapse/issues/11166))
- Add knock information in admin export. Contributed by Rafael Gonçalves. ([\#11171](https://github.com/matrix-org/synapse/issues/11171))
- Add tests to check that `ClientIpStore.get_last_client_ip_by_device` and `get_user_ip_and_agents` combine database and in-memory data correctly. ([\#11179](https://github.com/matrix-org/synapse/issues/11179))
- Refactor `Filter` to check different fields depending on the data type. ([\#11194](https://github.com/matrix-org/synapse/issues/11194))
- Improve type hints for the relations datastore. ([\#11205](https://github.com/matrix-org/synapse/issues/11205))
- Replace outdated links in the pull request checklist with links to the rendered documentation. ([\#11225](https://github.com/matrix-org/synapse/issues/11225))
- Fix a bug in unit test `test_block_room_and_not_purge`. ([\#11226](https://github.com/matrix-org/synapse/issues/11226))
- In `ObservableDeferred`, run observers in the order they were registered. ([\#11229](https://github.com/matrix-org/synapse/issues/11229))
- Minor speed up to start up times and getting updates for groups by adding missing index to `local_group_updates.stream_id`. ([\#11231](https://github.com/matrix-org/synapse/issues/11231))
- Add `twine` and `towncrier` as dev dependencies, as they're used by the release script. ([\#11233](https://github.com/matrix-org/synapse/issues/11233))
- Allow `stream_writers.typing` config to be a list of one worker. ([\#11237](https://github.com/matrix-org/synapse/issues/11237))
- Remove debugging statement in tests. ([\#11239](https://github.com/matrix-org/synapse/issues/11239))
- Fix [MSC2716](https://github.com/matrix-org/matrix-doc/pull/2716) historical messages backfilling in random order on remote homeservers. ([\#11244](https://github.com/matrix-org/synapse/issues/11244))
- Add an additional test for the `cachedList` method decorator. ([\#11246](https://github.com/matrix-org/synapse/issues/11246))
- Make minor correction to the type of `auth_checkers` callbacks. ([\#11253](https://github.com/matrix-org/synapse/issues/11253))
- Clean up trivial aspects of the Debian package build tooling. ([\#11269](https://github.com/matrix-org/synapse/issues/11269), [\#11273](https://github.com/matrix-org/synapse/issues/11273))
- Blacklist new SyTest that checks that key uploads are valid pending the validation being implemented in Synapse. ([\#11270](https://github.com/matrix-org/synapse/issues/11270))


Synapse 1.46.0 (2021-11-02)
===========================

The cause of the [performance regression affecting Synapse 1.44](https://github.com/matrix-org/synapse/issues/11049) has been identified and fixed. ([\#11177](https://github.com/matrix-org/synapse/issues/11177))

Bugfixes
--------

- Fix a bug introduced in v1.46.0rc1 where URL previews of some XML documents would fail. ([\#11196](https://github.com/matrix-org/synapse/issues/11196))


Synapse 1.46.0rc1 (2021-10-27)
==============================

Features
--------

- Add support for Ubuntu 21.10 "Impish Indri". ([\#11024](https://github.com/matrix-org/synapse/issues/11024))
- Port the Password Auth Providers module interface to the new generic interface. ([\#10548](https://github.com/matrix-org/synapse/issues/10548), [\#11180](https://github.com/matrix-org/synapse/issues/11180))
- Experimental support for the thread relation defined in [MSC3440](https://github.com/matrix-org/matrix-doc/pull/3440). ([\#11088](https://github.com/matrix-org/synapse/issues/11088), [\#11181](https://github.com/matrix-org/synapse/issues/11181), [\#11192](https://github.com/matrix-org/synapse/issues/11192))
- Users admin API can now also modify user type in addition to allowing it to be set on user creation. ([\#11174](https://github.com/matrix-org/synapse/issues/11174))


Bugfixes
--------

- Newly-created public rooms are now only assigned an alias if the room's creation has not been blocked by permission settings. Contributed by @AndrewFerr. ([\#10930](https://github.com/matrix-org/synapse/issues/10930))
- Fix a long-standing bug which meant that events received over federation were sometimes incorrectly accepted into the room state. ([\#11001](https://github.com/matrix-org/synapse/issues/11001), [\#11009](https://github.com/matrix-org/synapse/issues/11009), [\#11012](https://github.com/matrix-org/synapse/issues/11012))
- Fix 500 error on `/messages` when the server accumulates more than 5 backwards extremities at a given depth for a room. ([\#11027](https://github.com/matrix-org/synapse/issues/11027))
- Fix a bug where setting a user's `external_id` via the admin API returns 500 and deletes user's existing external mappings if that external ID is already mapped. ([\#11051](https://github.com/matrix-org/synapse/issues/11051))
- Fix a long-standing bug where users excluded from the user directory were added into the directory if they belonged to a room which became public or private. ([\#11075](https://github.com/matrix-org/synapse/issues/11075))
- Fix a long-standing bug when attempting to preview URLs which are in the `windows-1252` character encoding. ([\#11077](https://github.com/matrix-org/synapse/issues/11077), [\#11089](https://github.com/matrix-org/synapse/issues/11089))
- Fix broken export-data admin command and add test script checking the command to CI. ([\#11078](https://github.com/matrix-org/synapse/issues/11078))
- Show an error when timestamp in seconds is provided to the `/purge_media_cache` Admin API. ([\#11101](https://github.com/matrix-org/synapse/issues/11101))
- Fix local users who left all their rooms being removed from the user directory, even if the `search_all_users` config option was enabled. ([\#11103](https://github.com/matrix-org/synapse/issues/11103))
- Fix a bug which caused the module API's `get_user_ip_and_agents` function to always fail on workers. `get_user_ip_and_agents` was introduced in 1.44.0 and did not function correctly on worker processes at the time. ([\#11112](https://github.com/matrix-org/synapse/issues/11112))
- Identity server connection is no longer ignoring `ip_range_whitelist`. ([\#11120](https://github.com/matrix-org/synapse/issues/11120))
- Fix a bug introduced in Synapse 1.45.0 breaking the configuration file parsing script. ([\#11145](https://github.com/matrix-org/synapse/issues/11145))
- Fix a performance regression introduced in 1.44.0 which could cause client requests to time out when making large numbers of outbound requests. ([\#11177](https://github.com/matrix-org/synapse/issues/11177), [\#11190](https://github.com/matrix-org/synapse/issues/11190))
- Resolve and share `state_groups` for all [MSC2716](https://github.com/matrix-org/matrix-doc/pull/2716) historical events in batch. ([\#10975](https://github.com/matrix-org/synapse/issues/10975))


Improved Documentation
----------------------

- Fix broken links relating to module API deprecation in the upgrade notes. ([\#11069](https://github.com/matrix-org/synapse/issues/11069))
- Add more information about what happens when a user is deactivated. ([\#11083](https://github.com/matrix-org/synapse/issues/11083))
- Clarify the the sample log config can be copied from the documentation without issue. ([\#11092](https://github.com/matrix-org/synapse/issues/11092))
- Update the admin API documentation with an updated list of the characters allowed in registration tokens. ([\#11093](https://github.com/matrix-org/synapse/issues/11093))
- Document Synapse's behaviour when dealing with multiple modules registering the same callbacks and/or handlers for the same HTTP endpoints. ([\#11096](https://github.com/matrix-org/synapse/issues/11096))
- Fix instances of `[example]{.title-ref}` in the upgrade documentation as a result of prior RST to Markdown conversion. ([\#11118](https://github.com/matrix-org/synapse/issues/11118))
- Document the version of Synapse each module callback was introduced in. ([\#11132](https://github.com/matrix-org/synapse/issues/11132))
- Document the version of Synapse that introduced each module API method. ([\#11183](https://github.com/matrix-org/synapse/issues/11183))


Internal Changes
----------------
- Fix spurious warnings about losing the logging context on the `ReplicationCommandHandler` when losing the replication connection. ([\#10984](https://github.com/matrix-org/synapse/issues/10984))
- Include rejected status when we log events. ([\#11008](https://github.com/matrix-org/synapse/issues/11008))
- Add some extra logging to the event persistence code. ([\#11014](https://github.com/matrix-org/synapse/issues/11014))
- Rearrange the internal workings of the incremental user directory updates. ([\#11035](https://github.com/matrix-org/synapse/issues/11035))
- Fix a long-standing bug where users excluded from the directory could still be added to the `users_who_share_private_rooms` table after a regular user joins a private room. ([\#11143](https://github.com/matrix-org/synapse/issues/11143))
- Add and improve type hints. ([\#10972](https://github.com/matrix-org/synapse/issues/10972), [\#11055](https://github.com/matrix-org/synapse/issues/11055), [\#11066](https://github.com/matrix-org/synapse/issues/11066), [\#11076](https://github.com/matrix-org/synapse/issues/11076), [\#11095](https://github.com/matrix-org/synapse/issues/11095), [\#11109](https://github.com/matrix-org/synapse/issues/11109), [\#11121](https://github.com/matrix-org/synapse/issues/11121), [\#11146](https://github.com/matrix-org/synapse/issues/11146))
- Mark the Synapse package as containing type annotations and fix export declarations so that Synapse pluggable modules may be type checked against Synapse. ([\#11054](https://github.com/matrix-org/synapse/issues/11054))
- Remove dead code from `MediaFilePaths`. ([\#11056](https://github.com/matrix-org/synapse/issues/11056))
- Be more lenient when parsing oEmbed response versions. ([\#11065](https://github.com/matrix-org/synapse/issues/11065))
- Create a separate module for the retention configuration. ([\#11070](https://github.com/matrix-org/synapse/issues/11070))
- Clean up some of the federation event authentication code for clarity. ([\#11115](https://github.com/matrix-org/synapse/issues/11115), [\#11116](https://github.com/matrix-org/synapse/issues/11116), [\#11122](https://github.com/matrix-org/synapse/issues/11122))
- Add docstrings and comments to the application service ephemeral event sending code. ([\#11138](https://github.com/matrix-org/synapse/issues/11138))
- Update the `sign_json` script to support inline configuration of the signing key. ([\#11139](https://github.com/matrix-org/synapse/issues/11139))
- Fix broken link in the docker image README. ([\#11144](https://github.com/matrix-org/synapse/issues/11144))
- Always dump logs from unit tests during CI runs. ([\#11068](https://github.com/matrix-org/synapse/issues/11068))
- Add tests for `MediaFilePaths` class. ([\#11057](https://github.com/matrix-org/synapse/issues/11057))
- Simplify the user admin API tests. ([\#11048](https://github.com/matrix-org/synapse/issues/11048))
- Add a test for the workaround introduced in [\#11042](https://github.com/matrix-org/synapse/pull/11042) concerning the behaviour of third-party rule modules and `SynapseError`s. ([\#11071](https://github.com/matrix-org/synapse/issues/11071))


Synapse 1.45.1 (2021-10-20)
===========================

Bugfixes
--------

- Revert change to counting of deactivated users towards the monthly active users limit, introduced in 1.45.0rc1. ([\#11127](https://github.com/matrix-org/synapse/issues/11127))


Synapse 1.45.0 (2021-10-19)
===========================

No functional changes since Synapse 1.45.0rc2.

Known Issues
------------

- A suspected [performance regression](https://github.com/matrix-org/synapse/issues/11049) which was first reported after the release of 1.44.0 remains unresolved.

  We have not been able to identify a probable cause. Affected users report that setting up a federation sender worker appears to alleviate symptoms of the regression.

Improved Documentation
----------------------

- Reword changelog to clarify concerns about a suspected performance regression in 1.44.0. ([\#11117](https://github.com/matrix-org/synapse/issues/11117))


Synapse 1.45.0rc2 (2021-10-14)
==============================

This release candidate [fixes](https://github.com/matrix-org/synapse/issues/11053) a user directory [bug](https://github.com/matrix-org/synapse/issues/11025) present in 1.45.0rc1.

Known Issues
------------

- A suspected [performance regression](https://github.com/matrix-org/synapse/issues/11049) which was first reported after the release of 1.44.0 remains unresolved.

  We have not been able to identify a probable cause. Affected users report that setting up a federation sender worker appears to alleviate symptoms of the regression.

Bugfixes
--------

- Fix a long-standing bug when using multiple event persister workers where events were not correctly sent down `/sync` due to a race. ([\#11045](https://github.com/matrix-org/synapse/issues/11045))
- Fix a bug introduced in Synapse 1.45.0rc1 where the user directory would stop updating if it processed an event from a
  user not in the `users` table. ([\#11053](https://github.com/matrix-org/synapse/issues/11053))
- Fix a bug introduced in Synapse 1.44.0 when logging errors during oEmbed processing. ([\#11061](https://github.com/matrix-org/synapse/issues/11061))


Internal Changes
----------------

- Add an 'approximate difference' method to `StateFilter`. ([\#10825](https://github.com/matrix-org/synapse/issues/10825))
- Fix inconsistent behavior of `get_last_client_by_ip` when reporting data that has not been stored in the database yet. ([\#10970](https://github.com/matrix-org/synapse/issues/10970))
- Fix a bug introduced in Synapse 1.21.0 that causes opentracing and Prometheus metrics for replication requests to be measured incorrectly. ([\#10996](https://github.com/matrix-org/synapse/issues/10996))
- Ensure that cache config tests do not share state. ([\#11036](https://github.com/matrix-org/synapse/issues/11036))


Synapse 1.45.0rc1 (2021-10-12)
==============================

**Note:** Media storage providers module that read from Synapse's configuration need changes as of this version, see the [upgrade notes](https://matrix-org.github.io/synapse/develop/upgrade#upgrading-to-v1450) for more information.

Known Issues
------------

- We are investigating [a performance issue](https://github.com/matrix-org/synapse/issues/11049) which was reported after the release of 1.44.0.
- We are aware of [a bug](https://github.com/matrix-org/synapse/issues/11025) with the user directory when using application services. A second release candidate is expected which will resolve this.

Features
--------

- Add [MSC3069](https://github.com/matrix-org/matrix-doc/pull/3069) support to `/account/whoami`. ([\#9655](https://github.com/matrix-org/synapse/issues/9655))
- Support autodiscovery of oEmbed previews. ([\#10822](https://github.com/matrix-org/synapse/issues/10822))
- Add a `user_may_send_3pid_invite` spam checker callback for modules to allow or deny 3PID invites. ([\#10894](https://github.com/matrix-org/synapse/issues/10894))
- Add a spam checker callback to allow or deny room joins. ([\#10910](https://github.com/matrix-org/synapse/issues/10910))
- Include an `update_synapse_database` script in the distribution. Contributed by @Fizzadar at Beeper. ([\#10954](https://github.com/matrix-org/synapse/issues/10954))
- Include exception information in JSON logging output. Contributed by @Fizzadar at Beeper. ([\#11028](https://github.com/matrix-org/synapse/issues/11028))


Bugfixes
--------

- Fix a minor bug in the response to `/_matrix/client/r0/voip/turnServer`. Contributed by @lukaslihotzki. ([\#10922](https://github.com/matrix-org/synapse/issues/10922))
- Fix a bug where empty `yyyy-mm-dd/` directories would be left behind in the media store's `url_cache_thumbnails/` directory. ([\#10924](https://github.com/matrix-org/synapse/issues/10924))
- Fix a bug introduced in Synapse v1.40.0 where the signature checks for room version 8 and 9 could be applied to earlier room versions in some situations. ([\#10927](https://github.com/matrix-org/synapse/issues/10927))
- Fix a long-standing bug wherein deactivated users still count towards the monthly active users limit. ([\#10947](https://github.com/matrix-org/synapse/issues/10947))
- Fix a long-standing bug which meant that events received over federation were sometimes incorrectly accepted into the room state. ([\#10956](https://github.com/matrix-org/synapse/issues/10956))
- Fix a long-standing bug where rebuilding the user directory wouldn't exclude support and deactivated users. ([\#10960](https://github.com/matrix-org/synapse/issues/10960))
- Fix [MSC2716](https://github.com/matrix-org/matrix-doc/pull/2716) `/batch_send` endpoint rejecting subsequent batches with unknown batch ID error in existing room versions from the room creator. ([\#10962](https://github.com/matrix-org/synapse/issues/10962))
- Fix a bug that could leak local users' per-room nicknames and avatars when the user directory is rebuilt. ([\#10981](https://github.com/matrix-org/synapse/issues/10981))
- Fix a long-standing bug where the remainder of a batch of user directory changes would be silently dropped if the server left a room early in the batch. ([\#10982](https://github.com/matrix-org/synapse/issues/10982))
- Correct a bugfix introduced in Synapse v1.44.0 that would catch the wrong error if a connection is lost before a response could be written to it. ([\#10995](https://github.com/matrix-org/synapse/issues/10995))
- Fix a long-standing bug where local users' per-room nicknames/avatars were visible to anyone who could see you in the user directory. ([\#11002](https://github.com/matrix-org/synapse/issues/11002))
- Fix a long-standing bug where a user's per-room nickname/avatar would overwrite their profile in the user directory when a room was made public. ([\#11003](https://github.com/matrix-org/synapse/issues/11003))
- Work around a regression, introduced in Synapse v1.39.0, that caused `SynapseError`s raised by the experimental third-party rules module callback `check_event_allowed` to be ignored. ([\#11042](https://github.com/matrix-org/synapse/issues/11042))
- Fix a bug in [MSC2716](https://github.com/matrix-org/matrix-doc/pull/2716) insertion events in rooms that could cause cross-talk/conflicts between batches. ([\#10877](https://github.com/matrix-org/synapse/issues/10877))


Improved Documentation
----------------------

- Change wording ("reference homeserver") in Synapse repository documentation. Contributed by @maxkratz. ([\#10971](https://github.com/matrix-org/synapse/issues/10971))
- Fix a dead URL in development documentation (SAML) and change wording from "Riot" to "Element". Contributed by @maxkratz. ([\#10973](https://github.com/matrix-org/synapse/issues/10973))
- Add additional content to the Welcome and Overview page of the documentation. ([\#10990](https://github.com/matrix-org/synapse/issues/10990))
- Update links to MSCs in documentation. Contributed by @dklimpel. ([\#10991](https://github.com/matrix-org/synapse/issues/10991))


Internal Changes
----------------

- Improve type hinting in `synapse.util`. ([\#10888](https://github.com/matrix-org/synapse/issues/10888))
- Add further type hints to `synapse.storage.util`. ([\#10892](https://github.com/matrix-org/synapse/issues/10892))
- Fix type hints to be compatible with an upcoming change to Twisted. ([\#10895](https://github.com/matrix-org/synapse/issues/10895))
- Update utility code to handle C implementations of frozendict. ([\#10902](https://github.com/matrix-org/synapse/issues/10902))
- Drop old functionality which maintained database compatibility with Synapse versions before v1.31. ([\#10903](https://github.com/matrix-org/synapse/issues/10903))
- Clean-up configuration helper classes for the `ServerConfig` class. ([\#10915](https://github.com/matrix-org/synapse/issues/10915))
- Use direct references to config flags. ([\#10916](https://github.com/matrix-org/synapse/issues/10916), [\#10959](https://github.com/matrix-org/synapse/issues/10959), [\#10985](https://github.com/matrix-org/synapse/issues/10985))
- Clean up some of the federation event authentication code for clarity. ([\#10926](https://github.com/matrix-org/synapse/issues/10926), [\#10940](https://github.com/matrix-org/synapse/issues/10940), [\#10986](https://github.com/matrix-org/synapse/issues/10986), [\#10987](https://github.com/matrix-org/synapse/issues/10987), [\#10988](https://github.com/matrix-org/synapse/issues/10988), [\#11010](https://github.com/matrix-org/synapse/issues/11010), [\#11011](https://github.com/matrix-org/synapse/issues/11011))
- Refactor various parts of the codebase to use `RoomVersion` objects instead of room version identifier strings. ([\#10934](https://github.com/matrix-org/synapse/issues/10934))
- Refactor user directory tests in preparation for upcoming changes. ([\#10935](https://github.com/matrix-org/synapse/issues/10935))
- Include the event id in the logcontext when handling PDUs received over federation. ([\#10936](https://github.com/matrix-org/synapse/issues/10936))
- Fix logged errors in unit tests. ([\#10939](https://github.com/matrix-org/synapse/issues/10939))
- Fix a broken test to ensure that consent configuration works during registration. ([\#10945](https://github.com/matrix-org/synapse/issues/10945))
- Add type hints to filtering classes. ([\#10958](https://github.com/matrix-org/synapse/issues/10958))
- Add type-hint to `HomeserverTestcase.setup_test_homeserver`. ([\#10961](https://github.com/matrix-org/synapse/issues/10961))
- Fix the test utility function `create_room_as` so that `is_public=True` will explicitly set the `visibility` parameter of room creation requests to `public`. Contributed by @AndrewFerr. ([\#10963](https://github.com/matrix-org/synapse/issues/10963))
- Make the release script more robust and transparent. ([\#10966](https://github.com/matrix-org/synapse/issues/10966))
- Refactor [MSC2716](https://github.com/matrix-org/matrix-doc/pull/2716) `/batch_send` mega function into smaller handler functions. ([\#10974](https://github.com/matrix-org/synapse/issues/10974))
- Log stack traces when a missing opentracing span is detected. ([\#10983](https://github.com/matrix-org/synapse/issues/10983))
- Update GHA config to run tests against Python 3.10 and PostgreSQL 14. ([\#10992](https://github.com/matrix-org/synapse/issues/10992))
- Fix a long-standing bug where `ReadWriteLock`s could drop logging contexts on exit. ([\#10993](https://github.com/matrix-org/synapse/issues/10993))
- Add a `CODEOWNERS` file to automatically request reviews from the `@matrix-org/synapse-core` team on new pull requests. ([\#10994](https://github.com/matrix-org/synapse/issues/10994))
- Add further type hints to `synapse.state`. ([\#11004](https://github.com/matrix-org/synapse/issues/11004))
- Remove the deprecated `BaseHandler` object. ([\#11005](https://github.com/matrix-org/synapse/issues/11005))
- Bump mypy version for CI to 0.910, and pull in new type stubs for dependencies. ([\#11006](https://github.com/matrix-org/synapse/issues/11006))
- Fix CI to run the unit tests without optional deps. ([\#11017](https://github.com/matrix-org/synapse/issues/11017))
- Ensure that cache config tests do not share state. ([\#11019](https://github.com/matrix-org/synapse/issues/11019))
- Add additional type hints to `synapse.server_notices`. ([\#11021](https://github.com/matrix-org/synapse/issues/11021))
- Add additional type hints for `synapse.push`. ([\#11023](https://github.com/matrix-org/synapse/issues/11023))
- When installing the optional developer dependencies, also include the dependencies needed for type-checking and unit testing. ([\#11034](https://github.com/matrix-org/synapse/issues/11034))
- Remove unnecessary list comprehension from `synapse_port_db` to satisfy code style requirements. ([\#11043](https://github.com/matrix-org/synapse/issues/11043))


Synapse 1.44.0 (2021-10-05)
===========================

No significant changes since 1.44.0rc3.


Synapse 1.44.0rc3 (2021-10-04)
==============================

Bugfixes
--------

- Fix a bug introduced in Synapse v1.40.0 where changing a user's display name or avatar in a restricted room would cause an authentication error. ([\#10933](https://github.com/matrix-org/synapse/issues/10933))
- Fix `/admin/whois/{user_id}` endpoint, which was broken in v1.44.0rc1. ([\#10968](https://github.com/matrix-org/synapse/issues/10968))


Synapse 1.44.0rc2 (2021-09-30)
==============================

Bugfixes
--------

- Fix a bug introduced in v1.44.0rc1 which caused the experimental [MSC2716](https://github.com/matrix-org/matrix-doc/pull/2716) `/batch_send` endpoint to return a 500 error. ([\#10938](https://github.com/matrix-org/synapse/issues/10938))
- Fix a bug introduced in v1.44.0rc1 which prevented sending presence events to application services. ([\#10944](https://github.com/matrix-org/synapse/issues/10944))


Improved Documentation
----------------------

- Minor updates to the installation instructions. ([\#10919](https://github.com/matrix-org/synapse/issues/10919))


Synapse 1.44.0rc1 (2021-09-29)
==============================

Features
--------

- Only allow the [MSC2716](https://github.com/matrix-org/matrix-doc/pull/2716) `/batch_send?chunk_id=xxx` endpoint to connect to an already existing insertion event. ([\#10776](https://github.com/matrix-org/synapse/issues/10776))
- Improve oEmbed URL previews by processing the author name, photo, and video information. ([\#10814](https://github.com/matrix-org/synapse/issues/10814), [\#10819](https://github.com/matrix-org/synapse/issues/10819))
- Speed up responding with large JSON objects to requests. ([\#10868](https://github.com/matrix-org/synapse/issues/10868), [\#10905](https://github.com/matrix-org/synapse/issues/10905))
- Add a `user_may_create_room_with_invites` spam checker callback to allow modules to allow or deny a room creation request based on the invites and/or 3PID invites it includes. ([\#10898](https://github.com/matrix-org/synapse/issues/10898))


Bugfixes
--------

- Fix a long-standing bug that caused an `AssertionError` when purging history in certain rooms. Contributed by @Kokokokoka. ([\#10690](https://github.com/matrix-org/synapse/issues/10690))
- Fix a long-standing bug which caused deactivated users that were later reactivated to be missing from the user directory. ([\#10782](https://github.com/matrix-org/synapse/issues/10782))
- Fix a long-standing bug that caused unbanning a user by sending a membership event to fail. Contributed by @aaronraimist. ([\#10807](https://github.com/matrix-org/synapse/issues/10807))
- Fix a long-standing bug where logging contexts would go missing when federation requests time out. ([\#10810](https://github.com/matrix-org/synapse/issues/10810))
- Fix a long-standing bug causing an error in the deprecated `/initialSync` endpoint when using the undocumented `from` and `to` parameters. ([\#10827](https://github.com/matrix-org/synapse/issues/10827))
- Fix a bug causing the `remove_stale_pushers` background job to repeatedly fail and log errors. This bug affected Synapse servers that had been upgraded from version 1.28 or older and are using SQLite. ([\#10843](https://github.com/matrix-org/synapse/issues/10843))
- Fix a long-standing bug in Unicode support of the room search admin API breaking search for rooms with non-ASCII characters. ([\#10859](https://github.com/matrix-org/synapse/issues/10859))
- Fix a bug introduced in Synapse 1.37.0 which caused `knock` membership events which we sent to remote servers to be incorrectly stored in the local database. ([\#10873](https://github.com/matrix-org/synapse/issues/10873))
- Fix invalidating one-time key count cache after claiming keys. The bug was introduced in Synapse v1.41.0. Contributed by Tulir at Beeper. ([\#10875](https://github.com/matrix-org/synapse/issues/10875))
- Fix a long-standing bug causing application service users to be subject to MAU blocking if the MAU limit had been reached, even if configured not to be blocked. ([\#10881](https://github.com/matrix-org/synapse/issues/10881))
- Fix a long-standing bug which could cause events pulled over federation to be incorrectly rejected. ([\#10907](https://github.com/matrix-org/synapse/issues/10907))
- Fix a long-standing bug causing URL cache files to be stored in storage providers. Server admins may safely delete the `url_cache/` and `url_cache_thumbnails/` directories from any configured storage providers to reclaim space. ([\#10911](https://github.com/matrix-org/synapse/issues/10911))
- Fix a long-standing bug leading to race conditions when creating media store and config directories. ([\#10913](https://github.com/matrix-org/synapse/issues/10913))


Improved Documentation
----------------------

- Fix some crashes in the Module API example code, by adding JSON encoding/decoding. ([\#10845](https://github.com/matrix-org/synapse/issues/10845))
- Add developer documentation about experimental configuration flags. ([\#10865](https://github.com/matrix-org/synapse/issues/10865))
- Properly remove deleted files from GitHub pages when generating the documentation. ([\#10869](https://github.com/matrix-org/synapse/issues/10869))


Internal Changes
----------------

- Fix GitHub Actions config so we can run sytest on synapse from parallel branches. ([\#10659](https://github.com/matrix-org/synapse/issues/10659))
- Split out [MSC2716](https://github.com/matrix-org/matrix-doc/pull/2716) meta events to their own fields in the `/batch_send` response. ([\#10777](https://github.com/matrix-org/synapse/issues/10777))
- Add missing type hints to REST servlets. ([\#10785](https://github.com/matrix-org/synapse/issues/10785), [\#10817](https://github.com/matrix-org/synapse/issues/10817))
- Simplify the internal logic which maintains the user directory database tables. ([\#10796](https://github.com/matrix-org/synapse/issues/10796))
- Use direct references to config flags. ([\#10812](https://github.com/matrix-org/synapse/issues/10812), [\#10885](https://github.com/matrix-org/synapse/issues/10885), [\#10893](https://github.com/matrix-org/synapse/issues/10893), [\#10897](https://github.com/matrix-org/synapse/issues/10897))
- Specify the type of token in generic "Invalid token" error messages. ([\#10815](https://github.com/matrix-org/synapse/issues/10815))
- Make `StateFilter` frozen so it is hashable. ([\#10816](https://github.com/matrix-org/synapse/issues/10816))
- Fix a long-standing bug where an `m.room.message` event containing a null byte would cause an internal server error. ([\#10820](https://github.com/matrix-org/synapse/issues/10820))
- Add type hints to the state database. ([\#10823](https://github.com/matrix-org/synapse/issues/10823))
- Opt out of cache expiry for `get_users_who_share_room_with_user`, to hopefully improve `/sync` performance when you
  haven't synced recently. ([\#10826](https://github.com/matrix-org/synapse/issues/10826))
- Track cache eviction rates more finely in Prometheus's monitoring. ([\#10829](https://github.com/matrix-org/synapse/issues/10829))
- Add missing type hints to `synapse.handlers`. ([\#10831](https://github.com/matrix-org/synapse/issues/10831), [\#10856](https://github.com/matrix-org/synapse/issues/10856))
- Extend the Module API to let plug-ins check whether an ID is local and to access IP + User Agent data. ([\#10833](https://github.com/matrix-org/synapse/issues/10833))
- Factor out PNG image data to a constant to be used in several tests. ([\#10834](https://github.com/matrix-org/synapse/issues/10834))
- Add a test to ensure state events sent by modules get persisted correctly. ([\#10835](https://github.com/matrix-org/synapse/issues/10835))
- Rename [MSC2716](https://github.com/matrix-org/matrix-doc/pull/2716) fields and event types from `chunk` to `batch` to match the `/batch_send` endpoint. ([\#10838](https://github.com/matrix-org/synapse/issues/10838))
- Rename [MSC2716](https://github.com/matrix-org/matrix-doc/pull/2716) `/batch_send` query parameter from `?prev_event` to more obvious usage with `?prev_event_id`. ([\#10839](https://github.com/matrix-org/synapse/issues/10839))
- Add type hints to `synapse.http.site`. ([\#10867](https://github.com/matrix-org/synapse/issues/10867))
- Include outlier status when we log V2 or V3 events. ([\#10879](https://github.com/matrix-org/synapse/issues/10879))
- Break down Grafana's cache expiry time series based on reason for eviction, c.f. [\#10829](https://github.com/matrix-org/synapse/issues/10829). ([\#10880](https://github.com/matrix-org/synapse/issues/10880))
- Clean up some of the federation event authentication code for clarity. ([\#10883](https://github.com/matrix-org/synapse/issues/10883), [\#10884](https://github.com/matrix-org/synapse/issues/10884), [\#10896](https://github.com/matrix-org/synapse/issues/10896), [\#10901](https://github.com/matrix-org/synapse/issues/10901))
- Allow the `.` and `~` characters when creating registration tokens as per the change to [MSC3231](https://github.com/matrix-org/matrix-doc/pull/3231). ([\#10887](https://github.com/matrix-org/synapse/issues/10887))
- Clean up some unnecessary parentheses in places around the codebase. ([\#10889](https://github.com/matrix-org/synapse/issues/10889))
- Improve type hinting in the user directory code. ([\#10891](https://github.com/matrix-org/synapse/issues/10891))
- Update development testing script `test_postgresql.sh` to use a supported Python version and make re-runs quicker. ([\#10906](https://github.com/matrix-org/synapse/issues/10906))
- Document and summarize changes in schema version `61` – `64`. ([\#10917](https://github.com/matrix-org/synapse/issues/10917))
- Update release script to sign the newly created git tags. ([\#10925](https://github.com/matrix-org/synapse/issues/10925))
- Fix Debian builds due to `dh-virtualenv` no longer being able to build their docs. ([\#10931](https://github.com/matrix-org/synapse/issues/10931))


Synapse 1.43.0 (2021-09-21)
===========================

This release drops support for the deprecated, unstable API for [MSC2858 (Multiple SSO Identity Providers)](https://github.com/matrix-org/matrix-doc/blob/master/proposals/2858-Multiple-SSO-Identity-Providers.md#unstable-prefix), as well as the undocumented `experimental.msc2858_enabled` config option. Client authors should update their clients to use the stable API, available since Synapse 1.30.

The documentation has been updated with configuration for routing `/spaces`, `/hierarchy` and `/summary` to workers. See [the upgrade notes](https://github.com/matrix-org/synapse/blob/release-v1.43/docs/upgrade.md#upgrading-to-v1430) for more details.

No significant changes since 1.43.0rc2.

Synapse 1.43.0rc2 (2021-09-17)
==============================

Bugfixes
--------

- Added opentracing logging to help debug [\#9424](https://github.com/matrix-org/synapse/issues/9424). ([\#10828](https://github.com/matrix-org/synapse/issues/10828))


Synapse 1.43.0rc1 (2021-09-14)
==============================

Features
--------

- Allow room creators to send historical events specified by [MSC2716](https://github.com/matrix-org/matrix-doc/pull/2716) in existing room versions. ([\#10566](https://github.com/matrix-org/synapse/issues/10566))
- Add config option to use non-default manhole password and keys. ([\#10643](https://github.com/matrix-org/synapse/issues/10643))
- Skip final GC at shutdown to improve restart performance. ([\#10712](https://github.com/matrix-org/synapse/issues/10712))
- Allow configuration of the oEmbed URLs used for URL previews. ([\#10714](https://github.com/matrix-org/synapse/issues/10714), [\#10759](https://github.com/matrix-org/synapse/issues/10759))
- Prefer [room version 9](https://github.com/matrix-org/matrix-doc/pull/3375) for restricted rooms per the [room version capabilities](https://github.com/matrix-org/matrix-doc/pull/3244) API. ([\#10772](https://github.com/matrix-org/synapse/issues/10772))


Bugfixes
--------

- Fix a long-standing bug where room avatars were not included in email notifications. ([\#10658](https://github.com/matrix-org/synapse/issues/10658))
- Fix a bug where the ordering algorithm was skipping the `origin_server_ts` step in the spaces summary resulting in unstable room orderings. ([\#10730](https://github.com/matrix-org/synapse/issues/10730))
- Fix edge case when persisting events into a room where there are multiple events we previously hadn't calculated auth chains for (and hadn't marked as needing to be calculated). ([\#10743](https://github.com/matrix-org/synapse/issues/10743))
- Fix a bug which prevented calls to `/createRoom` that included the `room_alias_name` parameter from being handled by worker processes. ([\#10757](https://github.com/matrix-org/synapse/issues/10757))
- Fix a bug which prevented user registration via SSO to require consent tracking for SSO mapping providers that don't prompt for Matrix ID selection. Contributed by @AndrewFerr. ([\#10733](https://github.com/matrix-org/synapse/issues/10733))
- Only return the stripped state events for the `m.space.child` events in a room for the spaces summary from [MSC2946](https://github.com/matrix-org/matrix-doc/pull/2946). ([\#10760](https://github.com/matrix-org/synapse/issues/10760))
- Properly handle room upgrades of spaces. ([\#10774](https://github.com/matrix-org/synapse/issues/10774))
- Fix a bug which generated invalid homeserver config when the `frontend_proxy` worker type was passed to the Synapse Worker-based Complement image. ([\#10783](https://github.com/matrix-org/synapse/issues/10783))


Improved Documentation
----------------------

- Minor fix to the `media_repository` developer documentation. Contributed by @cuttingedge1109. ([\#10556](https://github.com/matrix-org/synapse/issues/10556))
- Update the documentation to note that the `/spaces` and `/hierarchy` endpoints can be routed to workers. ([\#10648](https://github.com/matrix-org/synapse/issues/10648))
- Clarify admin API documentation on undoing room deletions. ([\#10735](https://github.com/matrix-org/synapse/issues/10735))
- Split up the modules documentation and add examples for module developers. ([\#10758](https://github.com/matrix-org/synapse/issues/10758))
- Correct 2 typographical errors in the [Log Contexts documentation](https://matrix-org.github.io/synapse/latest/log_contexts.html). ([\#10795](https://github.com/matrix-org/synapse/issues/10795))
- Fix a wording mistake in the sample configuration. Contributed by @bramvdnheuvel:nltrix.net. ([\#10804](https://github.com/matrix-org/synapse/issues/10804))


Deprecations and Removals
-------------------------

- Remove the [unstable MSC2858 API](https://github.com/matrix-org/matrix-doc/blob/master/proposals/2858-Multiple-SSO-Identity-Providers.md#unstable-prefix), including the undocumented `experimental.msc2858_enabled` config option. The unstable API has been deprecated since Synapse 1.35. Client authors should update their clients to use the stable API introduced in Synapse 1.30 if they have not already done so. ([\#10693](https://github.com/matrix-org/synapse/issues/10693))


Internal Changes
----------------

- Add OpenTracing logging to help debug stuck messages (as described by issue [#9424](https://github.com/matrix-org/synapse/issues/9424)). ([\#10704](https://github.com/matrix-org/synapse/issues/10704))
- Add type annotations to the `synapse.util` package. ([\#10601](https://github.com/matrix-org/synapse/issues/10601))
- Ensure `rooms.creator` field is always populated for easy lookup in [MSC2716](https://github.com/matrix-org/matrix-doc/pull/2716) usage later. ([\#10697](https://github.com/matrix-org/synapse/issues/10697))
- Add missing type hints to REST servlets. ([\#10707](https://github.com/matrix-org/synapse/issues/10707), [\#10728](https://github.com/matrix-org/synapse/issues/10728), [\#10736](https://github.com/matrix-org/synapse/issues/10736))
- Do not include rooms with unknown room versions in the spaces summary results. ([\#10727](https://github.com/matrix-org/synapse/issues/10727))
- Additional error checking for the `preset` field when creating a room. ([\#10738](https://github.com/matrix-org/synapse/issues/10738))
- Clean up some of the federation event authentication code for clarity. ([\#10744](https://github.com/matrix-org/synapse/issues/10744), [\#10745](https://github.com/matrix-org/synapse/issues/10745), [\#10746](https://github.com/matrix-org/synapse/issues/10746), [\#10771](https://github.com/matrix-org/synapse/issues/10771), [\#10773](https://github.com/matrix-org/synapse/issues/10773), [\#10781](https://github.com/matrix-org/synapse/issues/10781))
- Add an index to `presence_stream` to hopefully speed up startups a little. ([\#10748](https://github.com/matrix-org/synapse/issues/10748))
- Refactor event size checking code to simplify searching the codebase for the origins of certain error strings that are occasionally emitted. ([\#10750](https://github.com/matrix-org/synapse/issues/10750))
- Move tests relating to rooms having encryption out of the user directory tests. ([\#10752](https://github.com/matrix-org/synapse/issues/10752))
- Use `attrs` internally for the URL preview code & update documentation. ([\#10753](https://github.com/matrix-org/synapse/issues/10753))
- Minor speed ups when joining large rooms over federation. ([\#10754](https://github.com/matrix-org/synapse/issues/10754), [\#10755](https://github.com/matrix-org/synapse/issues/10755), [\#10756](https://github.com/matrix-org/synapse/issues/10756), [\#10780](https://github.com/matrix-org/synapse/issues/10780), [\#10784](https://github.com/matrix-org/synapse/issues/10784))
- Add a constant for `m.federate`. ([\#10775](https://github.com/matrix-org/synapse/issues/10775))
- Add a script to update the Debian changelog in a Docker container for systems that are not Debian-based. ([\#10778](https://github.com/matrix-org/synapse/issues/10778))
- Change the format of authenticated users in logs when a user is being puppeted by and admin user. ([\#10779](https://github.com/matrix-org/synapse/issues/10779))
- Remove fixed and flakey tests from the Sytest blacklist. ([\#10788](https://github.com/matrix-org/synapse/issues/10788))
- Improve internal details of the user directory code. ([\#10789](https://github.com/matrix-org/synapse/issues/10789))
- Use direct references to config flags. ([\#10798](https://github.com/matrix-org/synapse/issues/10798))
- Ensure the Rust reporter passes type checking with jaeger-client 4.7's type annotations. ([\#10799](https://github.com/matrix-org/synapse/issues/10799))


Synapse 1.42.0 (2021-09-07)
===========================

This version of Synapse removes deprecated room-management admin APIs, removes out-of-date email pushers, and improves error handling for fallback templates for user-interactive authentication. For more information on these points, server administrators are encouraged to read [the upgrade notes](docs/upgrade.md#upgrading-to-v1420).

No significant changes since 1.42.0rc2.


Synapse 1.42.0rc2 (2021-09-06)
==============================

Features
--------

- Support room version 9 from [MSC3375](https://github.com/matrix-org/matrix-doc/pull/3375). ([\#10747](https://github.com/matrix-org/synapse/issues/10747))


Internal Changes
----------------

- Print a warning when using one of the deprecated `template_dir` settings. ([\#10768](https://github.com/matrix-org/synapse/issues/10768))


Synapse 1.42.0rc1 (2021-09-01)
==============================

Features
--------

- Add support for [MSC3231](https://github.com/matrix-org/matrix-doc/pull/3231): Token authenticated registration. Users can be required to submit a token during registration to authenticate themselves. Contributed by Callum Brown. ([\#10142](https://github.com/matrix-org/synapse/issues/10142))
- Add support for [MSC3283](https://github.com/matrix-org/matrix-doc/pull/3283): Expose `enable_set_displayname` in capabilities. ([\#10452](https://github.com/matrix-org/synapse/issues/10452))
- Port the `PresenceRouter` module interface to the new generic interface. ([\#10524](https://github.com/matrix-org/synapse/issues/10524))
- Add pagination to the spaces summary based on updates to [MSC2946](https://github.com/matrix-org/matrix-doc/pull/2946). ([\#10613](https://github.com/matrix-org/synapse/issues/10613), [\#10725](https://github.com/matrix-org/synapse/issues/10725))


Bugfixes
--------

- Validate new `m.room.power_levels` events. Contributed by @aaronraimist. ([\#10232](https://github.com/matrix-org/synapse/issues/10232))
- Display an error on User-Interactive Authentication fallback pages when authentication fails. Contributed by Callum Brown. ([\#10561](https://github.com/matrix-org/synapse/issues/10561))
- Remove pushers when deleting an e-mail address from an account. Pushers for old unlinked emails will also be deleted. ([\#10581](https://github.com/matrix-org/synapse/issues/10581), [\#10734](https://github.com/matrix-org/synapse/issues/10734))
- Reject Client-Server `/keys/query` requests which provide `device_ids` incorrectly. ([\#10593](https://github.com/matrix-org/synapse/issues/10593))
- Rooms with unsupported room versions are no longer returned via `/sync`. ([\#10644](https://github.com/matrix-org/synapse/issues/10644))
- Enforce the maximum length for per-room display names and avatar URLs. ([\#10654](https://github.com/matrix-org/synapse/issues/10654))
- Fix a bug which caused the `synapse_user_logins_total` Prometheus metric not to be correctly initialised on restart. ([\#10677](https://github.com/matrix-org/synapse/issues/10677))
- Improve `ServerNoticeServlet` to avoid duplicate requests and add unit tests. ([\#10679](https://github.com/matrix-org/synapse/issues/10679))
- Fix long-standing issue which caused an error when a thumbnail is requested and there are multiple thumbnails with the same quality rating. ([\#10684](https://github.com/matrix-org/synapse/issues/10684))
- Fix a regression introduced in v1.41.0 which affected the performance of concurrent fetches of large sets of events, in extreme cases causing the process to hang. ([\#10703](https://github.com/matrix-org/synapse/issues/10703))
- Fix a regression introduced in Synapse 1.41 which broke email transmission on Systems using older versions of the Twisted library. ([\#10713](https://github.com/matrix-org/synapse/issues/10713))


Improved Documentation
----------------------

- Add documentation on how to connect Django with Synapse using OpenID Connect and django-oauth-toolkit. Contributed by @HugoDelval. ([\#10192](https://github.com/matrix-org/synapse/issues/10192))
- Advertise https://matrix-org.github.io/synapse documentation in the `README` and `CONTRIBUTING` files. ([\#10595](https://github.com/matrix-org/synapse/issues/10595))
- Fix some of the titles not rendering in the OpenID Connect documentation. ([\#10639](https://github.com/matrix-org/synapse/issues/10639))
- Minor clarifications to the documentation for reverse proxies. ([\#10708](https://github.com/matrix-org/synapse/issues/10708))
- Remove table of contents from the top of installation and contributing documentation pages. ([\#10711](https://github.com/matrix-org/synapse/issues/10711))


Deprecations and Removals
-------------------------

- Remove deprecated Shutdown Room and Purge Room Admin API. ([\#8830](https://github.com/matrix-org/synapse/issues/8830))


Internal Changes
----------------

- Improve type hints for the proxy agent and SRV resolver modules. Contributed by @dklimpel. ([\#10608](https://github.com/matrix-org/synapse/issues/10608))
- Clean up some of the federation event authentication code for clarity. ([\#10614](https://github.com/matrix-org/synapse/issues/10614), [\#10615](https://github.com/matrix-org/synapse/issues/10615), [\#10624](https://github.com/matrix-org/synapse/issues/10624), [\#10640](https://github.com/matrix-org/synapse/issues/10640))
- Add a comment asking developers to leave a reason when bumping the database schema version. ([\#10621](https://github.com/matrix-org/synapse/issues/10621))
- Remove not needed database updates in modify user admin API. ([\#10627](https://github.com/matrix-org/synapse/issues/10627))
- Convert room member storage tuples to `attrs` classes. ([\#10629](https://github.com/matrix-org/synapse/issues/10629), [\#10642](https://github.com/matrix-org/synapse/issues/10642))
- Use auto-attribs for the attrs classes used in sync. ([\#10630](https://github.com/matrix-org/synapse/issues/10630))
- Make `backfill` and `get_missing_events` use the same codepath. ([\#10645](https://github.com/matrix-org/synapse/issues/10645))
- Improve the performance of the `/hierarchy` API (from [MSC2946](https://github.com/matrix-org/matrix-doc/pull/2946)) by caching responses received over federation. ([\#10647](https://github.com/matrix-org/synapse/issues/10647))
- Run a nightly CI build against Twisted trunk. ([\#10651](https://github.com/matrix-org/synapse/issues/10651), [\#10672](https://github.com/matrix-org/synapse/issues/10672))
- Do not print out stack traces for network errors when fetching data over federation. ([\#10662](https://github.com/matrix-org/synapse/issues/10662))
- Simplify tests for device admin rest API. ([\#10664](https://github.com/matrix-org/synapse/issues/10664))
- Add missing type hints to REST servlets. ([\#10665](https://github.com/matrix-org/synapse/issues/10665), [\#10666](https://github.com/matrix-org/synapse/issues/10666), [\#10674](https://github.com/matrix-org/synapse/issues/10674))
- Flatten the `tests.synapse.rests` package by moving the contents of `v1` and `v2_alpha` into the parent. ([\#10667](https://github.com/matrix-org/synapse/issues/10667))
- Update `complement.sh` to rebuild the base Docker image when run with workers. ([\#10686](https://github.com/matrix-org/synapse/issues/10686))
- Split the event-processing methods in `FederationHandler` into a separate `FederationEventHandler`. ([\#10692](https://github.com/matrix-org/synapse/issues/10692))
- Remove unused `compare_digest` function. ([\#10706](https://github.com/matrix-org/synapse/issues/10706))


Synapse 1.41.1 (2021-08-31)
===========================

Due to the two security issues highlighted below, server administrators are encouraged to update Synapse. We are not aware of these vulnerabilities being exploited in the wild.

Security advisory
-----------------

The following issues are fixed in v1.41.1.

- **[GHSA-3x4c-pq33-4w3q](https://github.com/matrix-org/synapse/security/advisories/GHSA-3x4c-pq33-4w3q) / [CVE-2021-39164](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-39164): Enumerating a private room's list of members and their display names.**

  If an unauthorized user both knows the Room ID of a private room *and* that room's history visibility is set to `shared`, then they may be able to enumerate the room's members, including their display names.

  The unauthorized user must be on the same homeserver as a user who is a member of the target room.

  Fixed by [52c7a51cf](https://github.com/matrix-org/synapse/commit/52c7a51cf).

- **[GHSA-jj53-8fmw-f2w2](https://github.com/matrix-org/synapse/security/advisories/GHSA-jj53-8fmw-f2w2) / [CVE-2021-39163](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-39163): Disclosing a private room's name, avatar, topic, and number of members.**

  If an unauthorized user knows the Room ID of a private room, then its name, avatar, topic, and number of members may be disclosed through Group / Community features.

  The unauthorized user must be on the same homeserver as a user who is a member of the target room, and their homeserver must allow non-administrators to create groups (`enable_group_creation` in the Synapse configuration; off by default).

  Fixed by [cb35df940a](https://github.com/matrix-org/synapse/commit/cb35df940a), [\#10723](https://github.com/matrix-org/synapse/issues/10723).

Bugfixes
--------

- Fix a regression introduced in Synapse 1.41 which broke email transmission on systems using older versions of the Twisted library. ([\#10713](https://github.com/matrix-org/synapse/issues/10713))

Synapse 1.41.0 (2021-08-24)
===========================

This release adds support for Debian 12 (Bookworm), but **removes support for Ubuntu 20.10 (Groovy Gorilla)**, which reached End of Life last month.

Note that when using workers the `/_synapse/admin/v1/users/{userId}/media` must now be handled by media workers. See the [upgrade notes](https://matrix-org.github.io/synapse/latest/upgrade.html) for more information.


Features
--------

- Enable room capabilities ([MSC3244](https://github.com/matrix-org/matrix-doc/pull/3244)) by default and set room version 8 as the preferred room version when creating restricted rooms. ([\#10571](https://github.com/matrix-org/synapse/issues/10571))


Synapse 1.41.0rc1 (2021-08-18)
==============================

Features
--------

- Add `get_userinfo_by_id` method to ModuleApi. ([\#9581](https://github.com/matrix-org/synapse/issues/9581))
- Initial local support for [MSC3266](https://github.com/matrix-org/synapse/pull/10394), Room Summary over the unstable `/rooms/{roomIdOrAlias}/summary` API. ([\#10394](https://github.com/matrix-org/synapse/issues/10394))
- Experimental support for [MSC3288](https://github.com/matrix-org/matrix-doc/pull/3288), sending `room_type` to the identity server for 3pid invites over the `/store-invite` API. ([\#10435](https://github.com/matrix-org/synapse/issues/10435))
- Add support for sending federation requests through a proxy. Contributed by @Bubu and @dklimpel. See the [upgrade notes](https://matrix-org.github.io/synapse/latest/upgrade.html) for more information. ([\#10596](https://github.com/matrix-org/synapse/issues/10596)). ([\#10475](https://github.com/matrix-org/synapse/issues/10475))
- Add support for "marker" events which makes historical events discoverable for servers that already have all of the scrollback history (part of [MSC2716](https://github.com/matrix-org/matrix-doc/pull/2716)). ([\#10498](https://github.com/matrix-org/synapse/issues/10498))
- Add a configuration setting for the time a `/sync` response is cached for. ([\#10513](https://github.com/matrix-org/synapse/issues/10513))
- The default logging handler for new installations is now `PeriodicallyFlushingMemoryHandler`, a buffered logging handler which periodically flushes itself. ([\#10518](https://github.com/matrix-org/synapse/issues/10518))
- Add support for new redaction rules for historical events specified in [MSC2716](https://github.com/matrix-org/matrix-doc/pull/2716). ([\#10538](https://github.com/matrix-org/synapse/issues/10538))
- Add a setting to disable TLS when sending email. ([\#10546](https://github.com/matrix-org/synapse/issues/10546))
- Add pagination to the spaces summary based on updates to [MSC2946](https://github.com/matrix-org/matrix-doc/pull/2946). ([\#10549](https://github.com/matrix-org/synapse/issues/10549), [\#10560](https://github.com/matrix-org/synapse/issues/10560), [\#10569](https://github.com/matrix-org/synapse/issues/10569), [\#10574](https://github.com/matrix-org/synapse/issues/10574), [\#10575](https://github.com/matrix-org/synapse/issues/10575), [\#10579](https://github.com/matrix-org/synapse/issues/10579), [\#10583](https://github.com/matrix-org/synapse/issues/10583))
- Admin API to delete several media for a specific user. Contributed by @dklimpel. ([\#10558](https://github.com/matrix-org/synapse/issues/10558), [\#10628](https://github.com/matrix-org/synapse/issues/10628))
- Add support for routing `/createRoom` to workers. ([\#10564](https://github.com/matrix-org/synapse/issues/10564))
- Update the Synapse Grafana dashboard. ([\#10570](https://github.com/matrix-org/synapse/issues/10570))
- Add an admin API (`GET /_synapse/admin/username_available`) to check if a username is available (regardless of registration settings). ([\#10578](https://github.com/matrix-org/synapse/issues/10578))
- Allow editing a user's `external_ids` via the "Edit User" admin API. Contributed by @dklimpel. ([\#10598](https://github.com/matrix-org/synapse/issues/10598))
- The Synapse manhole no longer needs coroutines to be wrapped in `defer.ensureDeferred`. ([\#10602](https://github.com/matrix-org/synapse/issues/10602))
- Add option to allow modules to run periodic tasks on all instances, rather than just the one configured to run background tasks. ([\#10638](https://github.com/matrix-org/synapse/issues/10638))


Bugfixes
--------

- Add some clarification to the sample config file. Contributed by @Kentokamoto. ([\#10129](https://github.com/matrix-org/synapse/issues/10129))
- Fix a long-standing bug where protocols which are not implemented by any appservices were incorrectly returned via `GET /_matrix/client/r0/thirdparty/protocols`. ([\#10532](https://github.com/matrix-org/synapse/issues/10532))
- Fix exceptions in logs when failing to get remote room list. ([\#10541](https://github.com/matrix-org/synapse/issues/10541))
- Fix longstanding bug which caused the user's presence "status message" to be reset when the user went offline. Contributed by @dklimpel. ([\#10550](https://github.com/matrix-org/synapse/issues/10550))
- Allow public rooms to be previewed in the spaces summary APIs from [MSC2946](https://github.com/matrix-org/matrix-doc/pull/2946). ([\#10580](https://github.com/matrix-org/synapse/issues/10580))
- Fix a bug introduced in v1.37.1 where an error could occur in the asynchronous processing of PDUs when the queue was empty. ([\#10592](https://github.com/matrix-org/synapse/issues/10592))
- Fix errors on /sync when read receipt data is a string. Only affects homeservers with the experimental flag for [MSC2285](https://github.com/matrix-org/matrix-doc/pull/2285) enabled. Contributed by @SimonBrandner. ([\#10606](https://github.com/matrix-org/synapse/issues/10606))
- Additional validation for the spaces summary API to avoid errors like `ValueError: Stop argument for islice() must be None or an integer`. The missing validation has existed since v1.31.0. ([\#10611](https://github.com/matrix-org/synapse/issues/10611))
- Revert behaviour introduced in v1.38.0 that strips `org.matrix.msc2732.device_unused_fallback_key_types` from `/sync` when its value is empty. This field should instead always be present according to [MSC2732](https://github.com/matrix-org/matrix-doc/blob/master/proposals/2732-olm-fallback-keys.md). ([\#10623](https://github.com/matrix-org/synapse/issues/10623))


Improved Documentation
----------------------

- Add documentation for configuring a forward proxy. ([\#10443](https://github.com/matrix-org/synapse/issues/10443))
- Updated the reverse proxy documentation to highlight the homserver configuration that is needed to make Synapse aware that is is intentionally reverse proxied. ([\#10551](https://github.com/matrix-org/synapse/issues/10551))
- Update CONTRIBUTING.md to fix index links and the instructions for SyTest in docker. ([\#10599](https://github.com/matrix-org/synapse/issues/10599))


Deprecations and Removals
-------------------------

- No longer build `.deb` packages for Ubuntu 20.10 Groovy Gorilla, which has now EOLed. ([\#10588](https://github.com/matrix-org/synapse/issues/10588))
- The `template_dir` configuration settings in the `sso`, `account_validity` and `email` sections of the configuration file are now deprecated in favour of the global `templates.custom_template_directory` setting. See the [upgrade notes](https://matrix-org.github.io/synapse/latest/upgrade.html) for more information. ([\#10596](https://github.com/matrix-org/synapse/issues/10596))


Internal Changes
----------------

- Improve event caching mechanism to avoid having multiple copies of an event in memory at a time. ([\#10119](https://github.com/matrix-org/synapse/issues/10119))
- Reduce errors in PostgreSQL logs due to concurrent serialization errors. ([\#10504](https://github.com/matrix-org/synapse/issues/10504))
- Include room ID in ignored EDU log messages. Contributed by @ilmari. ([\#10507](https://github.com/matrix-org/synapse/issues/10507))
- Add pagination to the spaces summary based on updates to [MSC2946](https://github.com/matrix-org/matrix-doc/pull/2946). ([\#10527](https://github.com/matrix-org/synapse/issues/10527), [\#10530](https://github.com/matrix-org/synapse/issues/10530))
- Fix CI to not break when run against branches rather than pull requests. ([\#10529](https://github.com/matrix-org/synapse/issues/10529))
- Mark all events stemming from the [MSC2716](https://github.com/matrix-org/matrix-doc/pull/2716) `/batch_send` endpoint as historical. ([\#10537](https://github.com/matrix-org/synapse/issues/10537))
- Clean up some of the federation event authentication code for clarity. ([\#10539](https://github.com/matrix-org/synapse/issues/10539), [\#10591](https://github.com/matrix-org/synapse/issues/10591))
- Convert `Transaction` and `Edu` objects to attrs. ([\#10542](https://github.com/matrix-org/synapse/issues/10542))
- Update `/batch_send` endpoint to only return `state_events` created by the `state_events_from_before` passed in. ([\#10552](https://github.com/matrix-org/synapse/issues/10552))
- Update contributing.md to warn against rebasing an open PR. ([\#10563](https://github.com/matrix-org/synapse/issues/10563))
- Remove the unused public rooms replication stream. ([\#10565](https://github.com/matrix-org/synapse/issues/10565))
- Clarify error message when failing to join a restricted room. ([\#10572](https://github.com/matrix-org/synapse/issues/10572))
- Remove references to BuildKite in favour of GitHub Actions. ([\#10573](https://github.com/matrix-org/synapse/issues/10573))
- Move `/batch_send` endpoint defined by [MSC2716](https://github.com/matrix-org/matrix-doc/pull/2716) to the `/v2_alpha` directory. ([\#10576](https://github.com/matrix-org/synapse/issues/10576))
- Allow multiple custom directories in `read_templates`. ([\#10587](https://github.com/matrix-org/synapse/issues/10587))
- Re-organize the `synapse.federation.transport.server` module to create smaller files. ([\#10590](https://github.com/matrix-org/synapse/issues/10590))
- Flatten the `synapse.rest.client` package by moving the contents of `v1` and `v2_alpha` into the parent. ([\#10600](https://github.com/matrix-org/synapse/issues/10600))
- Build Debian packages for Debian 12 (Bookworm). ([\#10612](https://github.com/matrix-org/synapse/issues/10612))
- Fix up a couple of links to the database schema documentation. ([\#10620](https://github.com/matrix-org/synapse/issues/10620))
- Fix a broken link to the upgrade notes. ([\#10631](https://github.com/matrix-org/synapse/issues/10631))


Synapse 1.40.0 (2021-08-10)
===========================

No significant changes.


Synapse 1.40.0rc3 (2021-08-09)
==============================

Features
--------

- Support [MSC3289: room version 8](https://github.com/matrix-org/matrix-doc/pull/3289). ([\#10449](https://github.com/matrix-org/synapse/issues/10449))


Bugfixes
--------

- Mark the experimental room version from [MSC2716](https://github.com/matrix-org/matrix-doc/pull/2716) as unstable. ([\#10449](https://github.com/matrix-org/synapse/issues/10449))


Improved Documentation
----------------------

- Fix broken links in `upgrade.md`. Contributed by @dklimpel. ([\#10543](https://github.com/matrix-org/synapse/issues/10543))


Synapse 1.40.0rc2 (2021-08-04)
==============================

Bugfixes
--------

- Fix the `PeriodicallyFlushingMemoryHandler` inhibiting application shutdown because of its background thread. ([\#10517](https://github.com/matrix-org/synapse/issues/10517))
- Fix a bug introduced in Synapse v1.40.0rc1 that could cause Synapse to respond with an error when clients would update read receipts. ([\#10531](https://github.com/matrix-org/synapse/issues/10531))


Internal Changes
----------------

- Fix release script to open the correct URL for the release. ([\#10516](https://github.com/matrix-org/synapse/issues/10516))


Synapse 1.40.0rc1 (2021-08-03)
==============================

Features
--------

- Add support for [MSC2033](https://github.com/matrix-org/matrix-doc/pull/2033): `device_id` on `/account/whoami`. ([\#9918](https://github.com/matrix-org/synapse/issues/9918))
- Update support for [MSC2716 - Incrementally importing history into existing rooms](https://github.com/matrix-org/matrix-doc/pull/2716). ([\#10245](https://github.com/matrix-org/synapse/issues/10245), [\#10432](https://github.com/matrix-org/synapse/issues/10432), [\#10463](https://github.com/matrix-org/synapse/issues/10463))
- Update support for [MSC3083](https://github.com/matrix-org/matrix-doc/pull/3083) to consider changes in the MSC around which servers can issue join events. ([\#10254](https://github.com/matrix-org/synapse/issues/10254), [\#10447](https://github.com/matrix-org/synapse/issues/10447), [\#10489](https://github.com/matrix-org/synapse/issues/10489))
- Initial support for [MSC3244](https://github.com/matrix-org/matrix-doc/pull/3244), Room version capabilities over the /capabilities API. ([\#10283](https://github.com/matrix-org/synapse/issues/10283))
- Add a buffered logging handler which periodically flushes itself. ([\#10407](https://github.com/matrix-org/synapse/issues/10407), [\#10515](https://github.com/matrix-org/synapse/issues/10515))
- Add support for https connections to a proxy server. Contributed by @Bubu and @dklimpel. ([\#10411](https://github.com/matrix-org/synapse/issues/10411))
- Support for [MSC2285 (hidden read receipts)](https://github.com/matrix-org/matrix-doc/pull/2285). Contributed by @SimonBrandner. ([\#10413](https://github.com/matrix-org/synapse/issues/10413))
- Email notifications now state whether an invitation is to a room or a space. ([\#10426](https://github.com/matrix-org/synapse/issues/10426))
- Allow setting transaction limit for database connections. ([\#10440](https://github.com/matrix-org/synapse/issues/10440), [\#10511](https://github.com/matrix-org/synapse/issues/10511))
- Add `creation_ts` to "list users" admin API. ([\#10448](https://github.com/matrix-org/synapse/issues/10448))


Bugfixes
--------

- Improve character set detection in URL previews by supporting underscores (in addition to hyphens). Contributed by @srividyut. ([\#10410](https://github.com/matrix-org/synapse/issues/10410))
- Fix events being incorrectly rejected over federation if they reference auth events that the server needed to fetch. ([\#10439](https://github.com/matrix-org/synapse/issues/10439))
- Fix `synapse_federation_server_oldest_inbound_pdu_in_staging` Prometheus metric to not report a max age of 51 years when the queue is empty. ([\#10455](https://github.com/matrix-org/synapse/issues/10455))
- Fix a bug which caused an explicit assignment of power-level 0 to a user to be misinterpreted in rare circumstances. ([\#10499](https://github.com/matrix-org/synapse/issues/10499))


Improved Documentation
----------------------

- Fix hierarchy of providers on the OpenID page. ([\#10445](https://github.com/matrix-org/synapse/issues/10445))
- Consolidate development documentation to `docs/development/`. ([\#10453](https://github.com/matrix-org/synapse/issues/10453))
- Add some developer docs to explain room DAG concepts like `outliers`, `state_groups`, `depth`, etc. ([\#10464](https://github.com/matrix-org/synapse/issues/10464))
- Document how to use Complement while developing a new Synapse feature. ([\#10483](https://github.com/matrix-org/synapse/issues/10483))


Internal Changes
----------------

- Prune inbound federation queues for a room if they get too large. ([\#10390](https://github.com/matrix-org/synapse/issues/10390))
- Add type hints to `synapse.federation.transport.client` module. ([\#10408](https://github.com/matrix-org/synapse/issues/10408))
- Remove shebang line from module files. ([\#10415](https://github.com/matrix-org/synapse/issues/10415))
- Drop backwards-compatibility code that was required to support Ubuntu Xenial. ([\#10429](https://github.com/matrix-org/synapse/issues/10429))
- Use a docker image cache for the prerequisites for the debian package build. ([\#10431](https://github.com/matrix-org/synapse/issues/10431))
- Improve servlet type hints. ([\#10437](https://github.com/matrix-org/synapse/issues/10437), [\#10438](https://github.com/matrix-org/synapse/issues/10438))
- Replace usage of `or_ignore` in `simple_insert` with `simple_upsert` usage, to stop spamming postgres logs with spurious ERROR messages. ([\#10442](https://github.com/matrix-org/synapse/issues/10442))
- Update the `tests-done` Github Actions status. ([\#10444](https://github.com/matrix-org/synapse/issues/10444), [\#10512](https://github.com/matrix-org/synapse/issues/10512))
- Update type annotations to work with forthcoming Twisted 21.7.0 release. ([\#10446](https://github.com/matrix-org/synapse/issues/10446), [\#10450](https://github.com/matrix-org/synapse/issues/10450))
- Cancel redundant GHA workflows when a new commit is pushed. ([\#10451](https://github.com/matrix-org/synapse/issues/10451))
- Mitigate media repo XSS attacks on IE11 via the non-standard X-Content-Security-Policy header. ([\#10468](https://github.com/matrix-org/synapse/issues/10468))
- Additional type hints in the state handler. ([\#10482](https://github.com/matrix-org/synapse/issues/10482))
- Update syntax used to run complement tests. ([\#10488](https://github.com/matrix-org/synapse/issues/10488))
- Fix up type annotations to work with Twisted 21.7. ([\#10490](https://github.com/matrix-org/synapse/issues/10490))
- Improve type annotations for `ObservableDeferred`. ([\#10491](https://github.com/matrix-org/synapse/issues/10491))
- Extend release script to also tag and create GitHub releases. ([\#10496](https://github.com/matrix-org/synapse/issues/10496))
- Fix a bug which caused production debian packages to be incorrectly marked as 'prerelease'. ([\#10500](https://github.com/matrix-org/synapse/issues/10500))


Synapse 1.39.0 (2021-07-29)
===========================

No significant changes.


Synapse 1.39.0rc3 (2021-07-28)
==============================

Bugfixes
--------

- Fix a bug introduced in Synapse 1.38 which caused an exception at startup when SAML authentication was enabled. ([\#10477](https://github.com/matrix-org/synapse/issues/10477))
- Fix a long-standing bug where Synapse would not inform clients that a device had exhausted its one-time-key pool, potentially causing problems decrypting events. ([\#10485](https://github.com/matrix-org/synapse/issues/10485))
- Fix reporting old R30 stats as R30v2 stats. Introduced in v1.39.0rc1. ([\#10486](https://github.com/matrix-org/synapse/issues/10486))


Internal Changes
----------------

- Fix an error which prevented the Github Actions workflow to build the docker images from running. ([\#10461](https://github.com/matrix-org/synapse/issues/10461))
- Fix release script to correctly version debian changelog when doing RCs. ([\#10465](https://github.com/matrix-org/synapse/issues/10465))


Synapse 1.39.0rc2 (2021-07-22)
==============================

This release also includes the changes in v1.38.1.


Internal Changes
----------------

- Move docker image build to Github Actions. ([\#10416](https://github.com/matrix-org/synapse/issues/10416))


Synapse 1.38.1 (2021-07-22)
===========================

Bugfixes
--------

- Always include `device_one_time_keys_count` key in `/sync` response to work around a bug in Element Android that broke encryption for new devices. ([\#10457](https://github.com/matrix-org/synapse/issues/10457))


Synapse 1.39.0rc1 (2021-07-20)
==============================

The Third-Party Event Rules module interface has been deprecated in favour of the generic module interface introduced in Synapse v1.37.0. Support for the old interface is planned to be removed in September 2021. See the [upgrade notes](https://matrix-org.github.io/synapse/latest/upgrade.html#upgrading-to-v1390) for more information.

Features
--------

- Add the ability to override the account validity feature with a module. ([\#9884](https://github.com/matrix-org/synapse/issues/9884))
- The spaces summary API now returns any joinable rooms, not only rooms which are world-readable. ([\#10298](https://github.com/matrix-org/synapse/issues/10298), [\#10305](https://github.com/matrix-org/synapse/issues/10305))
- Add a new version of the R30 phone-home metric, which removes a false impression of retention given by the old R30 metric. ([\#10332](https://github.com/matrix-org/synapse/issues/10332), [\#10427](https://github.com/matrix-org/synapse/issues/10427))
- Allow providing credentials to `http_proxy`. ([\#10360](https://github.com/matrix-org/synapse/issues/10360))


Bugfixes
--------

- Fix error while dropping locks on shutdown. Introduced in v1.38.0. ([\#10433](https://github.com/matrix-org/synapse/issues/10433))
- Add base starting insertion event when no chunk ID is specified in the historical batch send API. ([\#10250](https://github.com/matrix-org/synapse/issues/10250))
- Fix historical batch send endpoint (MSC2716) rejecting batches with messages from multiple senders. ([\#10276](https://github.com/matrix-org/synapse/issues/10276))
- Fix purging rooms that other homeservers are still sending events for. Contributed by @ilmari. ([\#10317](https://github.com/matrix-org/synapse/issues/10317))
- Fix errors during backfill caused by previously purged redaction events. Contributed by Andreas Rammhold (@andir). ([\#10343](https://github.com/matrix-org/synapse/issues/10343))
- Fix the user directory becoming broken (and noisy errors being logged) when knocking and room statistics are in use. ([\#10344](https://github.com/matrix-org/synapse/issues/10344))
- Fix newly added `synapse_federation_server_oldest_inbound_pdu_in_staging` prometheus metric to measure age rather than timestamp. ([\#10355](https://github.com/matrix-org/synapse/issues/10355))
- Fix PostgreSQL sometimes using table scans for queries against `state_groups_state` table, taking a long time and a large amount of IO. ([\#10359](https://github.com/matrix-org/synapse/issues/10359))
- Fix `make_room_admin` failing for users that have left a private room. ([\#10367](https://github.com/matrix-org/synapse/issues/10367))
- Fix a number of logged errors caused by remote servers being down. ([\#10400](https://github.com/matrix-org/synapse/issues/10400), [\#10414](https://github.com/matrix-org/synapse/issues/10414))
- Responses from `/make_{join,leave,knock}` no longer include signatures, which will turn out to be invalid after events are returned to `/send_{join,leave,knock}`. ([\#10404](https://github.com/matrix-org/synapse/issues/10404))


Improved Documentation
----------------------

- Updated installation dependencies for newer macOS versions and ARM Macs. Contributed by Luke Walsh. ([\#9971](https://github.com/matrix-org/synapse/issues/9971))
- Simplify structure of room admin API. ([\#10313](https://github.com/matrix-org/synapse/issues/10313))
- Refresh the logcontext dev documentation. ([\#10353](https://github.com/matrix-org/synapse/issues/10353)), ([\#10337](https://github.com/matrix-org/synapse/issues/10337))
- Add delegation example for caddy in the reverse proxy documentation. Contributed by @moritzdietz. ([\#10368](https://github.com/matrix-org/synapse/issues/10368))
- Fix and clarify some links in `docs` and `contrib`. ([\#10370](https://github.com/matrix-org/synapse/issues/10370)), ([\#10322](https://github.com/matrix-org/synapse/issues/10322)), ([\#10399](https://github.com/matrix-org/synapse/issues/10399))
- Make deprecation notice of the spam checker doc more obvious. ([\#10395](https://github.com/matrix-org/synapse/issues/10395))
- Add instructions on installing Debian packages for release candidates. ([\#10396](https://github.com/matrix-org/synapse/issues/10396))


Deprecations and Removals
-------------------------

- Remove functionality associated with the unused `room_stats_historical` and `user_stats_historical` tables. Contributed by @xmunoz. ([\#9721](https://github.com/matrix-org/synapse/issues/9721))
- The third-party event rules module interface is deprecated in favour of the generic module interface introduced in Synapse v1.37.0. See the [upgrade notes](https://matrix-org.github.io/synapse/latest/upgrade.html#upgrading-to-v1390) for more information. ([\#10386](https://github.com/matrix-org/synapse/issues/10386))


Internal Changes
----------------

- Convert `room_depth.min_depth` column to a `BIGINT`. ([\#10289](https://github.com/matrix-org/synapse/issues/10289))
- Add tests to characterise the current behaviour of R30 phone-home metrics. ([\#10315](https://github.com/matrix-org/synapse/issues/10315))
- Rebuild event context and auth when processing specific results from `ThirdPartyEventRules` modules. ([\#10316](https://github.com/matrix-org/synapse/issues/10316))
- Minor change to the code that populates `user_daily_visits`. ([\#10324](https://github.com/matrix-org/synapse/issues/10324))
- Re-enable Sytests that were disabled for the 1.37.1 release. ([\#10345](https://github.com/matrix-org/synapse/issues/10345), [\#10357](https://github.com/matrix-org/synapse/issues/10357))
- Run `pyupgrade` on the codebase. ([\#10347](https://github.com/matrix-org/synapse/issues/10347), [\#10348](https://github.com/matrix-org/synapse/issues/10348))
- Switch `application_services_txns.txn_id` database column to `BIGINT`. ([\#10349](https://github.com/matrix-org/synapse/issues/10349))
- Convert internal type variable syntax to reflect wider ecosystem use. ([\#10350](https://github.com/matrix-org/synapse/issues/10350), [\#10380](https://github.com/matrix-org/synapse/issues/10380), [\#10381](https://github.com/matrix-org/synapse/issues/10381), [\#10382](https://github.com/matrix-org/synapse/issues/10382), [\#10418](https://github.com/matrix-org/synapse/issues/10418))
- Make the Github Actions workflow configuration more efficient. ([\#10383](https://github.com/matrix-org/synapse/issues/10383))
- Add type hints to `get_{domain,localpart}_from_id`. ([\#10385](https://github.com/matrix-org/synapse/issues/10385))
- When building Debian packages for prerelease versions, set the Section accordingly. ([\#10391](https://github.com/matrix-org/synapse/issues/10391))
- Add type hints and comments to event auth code. ([\#10393](https://github.com/matrix-org/synapse/issues/10393))
- Stagger sending of presence update to remote servers, reducing CPU spikes caused by starting many connections to remote servers at once. ([\#10398](https://github.com/matrix-org/synapse/issues/10398))
- Remove unused `events_by_room` code (tech debt). ([\#10421](https://github.com/matrix-org/synapse/issues/10421))
- Add a github actions job which records success of other jobs. ([\#10430](https://github.com/matrix-org/synapse/issues/10430))


Synapse 1.38.0 (2021-07-13)
===========================

This release includes a database schema update which could result in elevated disk usage. See the [upgrade notes](https://matrix-org.github.io/synapse/develop/upgrade#upgrading-to-v1380) for more information.

No significant changes since 1.38.0rc3.


Synapse 1.38.0rc3 (2021-07-13)
==============================

Internal Changes
----------------

- Build the Debian packages in CI. ([\#10247](https://github.com/matrix-org/synapse/issues/10247), [\#10379](https://github.com/matrix-org/synapse/issues/10379))


Synapse 1.38.0rc2 (2021-07-09)
==============================

Bugfixes
--------

- Fix bug where inbound federation in a room could be delayed due to not correctly dropping a lock. Introduced in v1.37.1. ([\#10336](https://github.com/matrix-org/synapse/issues/10336))


Improved Documentation
----------------------

- Update links to documentation in the sample config. Contributed by @dklimpel. ([\#10287](https://github.com/matrix-org/synapse/issues/10287))
- Fix broken links in [INSTALL.md](INSTALL.md). Contributed by @dklimpel. ([\#10331](https://github.com/matrix-org/synapse/issues/10331))


Synapse 1.38.0rc1 (2021-07-06)
==============================

Features
--------

- Implement refresh tokens as specified by [MSC2918](https://github.com/matrix-org/matrix-doc/pull/2918). ([\#9450](https://github.com/matrix-org/synapse/issues/9450))
- Add support for evicting cache entries based on last access time. ([\#10205](https://github.com/matrix-org/synapse/issues/10205))
- Omit empty fields from the `/sync` response. Contributed by @deepbluev7. ([\#10214](https://github.com/matrix-org/synapse/issues/10214))
- Improve validation on federation `send_{join,leave,knock}` endpoints. ([\#10225](https://github.com/matrix-org/synapse/issues/10225), [\#10243](https://github.com/matrix-org/synapse/issues/10243))
- Add SSO `external_ids` to the Query User Account admin API. ([\#10261](https://github.com/matrix-org/synapse/issues/10261))
- Mark events received over federation which fail a spam check as "soft-failed". ([\#10263](https://github.com/matrix-org/synapse/issues/10263))
- Add metrics for new inbound federation staging area. ([\#10284](https://github.com/matrix-org/synapse/issues/10284))
- Add script to print information about recently registered users. ([\#10290](https://github.com/matrix-org/synapse/issues/10290))


Bugfixes
--------

- Fix a long-standing bug which meant that invite rejections and knocks were not sent out over federation in a timely manner. ([\#10223](https://github.com/matrix-org/synapse/issues/10223))
- Fix a bug introduced in v1.26.0 where only users who have set profile information could be deactivated with erasure enabled. ([\#10252](https://github.com/matrix-org/synapse/issues/10252))
- Fix a long-standing bug where Synapse would return errors after 2<sup>31</sup> events were handled by the server. ([\#10264](https://github.com/matrix-org/synapse/issues/10264), [\#10267](https://github.com/matrix-org/synapse/issues/10267), [\#10282](https://github.com/matrix-org/synapse/issues/10282), [\#10286](https://github.com/matrix-org/synapse/issues/10286), [\#10291](https://github.com/matrix-org/synapse/issues/10291), [\#10314](https://github.com/matrix-org/synapse/issues/10314), [\#10326](https://github.com/matrix-org/synapse/issues/10326))
- Fix the prometheus `synapse_federation_server_pdu_process_time` metric. Broke in v1.37.1. ([\#10279](https://github.com/matrix-org/synapse/issues/10279))
- Ensure that inbound events from federation that were being processed when Synapse was restarted get promptly processed on start up. ([\#10303](https://github.com/matrix-org/synapse/issues/10303))


Improved Documentation
----------------------

- Move the upgrade notes to [docs/upgrade.md](https://github.com/matrix-org/synapse/blob/develop/docs/upgrade.md) and convert them to markdown. ([\#10166](https://github.com/matrix-org/synapse/issues/10166))
- Choose Welcome & Overview as the default page for synapse documentation website. ([\#10242](https://github.com/matrix-org/synapse/issues/10242))
- Adjust the URL in the README.rst file to point to irc.libera.chat. ([\#10258](https://github.com/matrix-org/synapse/issues/10258))
- Fix homeserver config option name in presence router documentation. ([\#10288](https://github.com/matrix-org/synapse/issues/10288))
- Fix link pointing at the wrong section in the modules documentation page. ([\#10302](https://github.com/matrix-org/synapse/issues/10302))


Internal Changes
----------------

- Drop `Origin` and `Accept` from the value of the `Access-Control-Allow-Headers` response header. ([\#10114](https://github.com/matrix-org/synapse/issues/10114))
- Add type hints to the federation servlets. ([\#10213](https://github.com/matrix-org/synapse/issues/10213))
- Improve the reliability of auto-joining remote rooms. ([\#10237](https://github.com/matrix-org/synapse/issues/10237))
- Update the release script to use the semver terminology and determine the release branch based on the next version. ([\#10239](https://github.com/matrix-org/synapse/issues/10239))
- Fix type hints for computing auth events. ([\#10253](https://github.com/matrix-org/synapse/issues/10253))
- Improve the performance of the spaces summary endpoint by only recursing into spaces (and not rooms in general). ([\#10256](https://github.com/matrix-org/synapse/issues/10256))
- Move event authentication methods from `Auth` to `EventAuthHandler`. ([\#10268](https://github.com/matrix-org/synapse/issues/10268))
- Re-enable a SyTest after it has been fixed. ([\#10292](https://github.com/matrix-org/synapse/issues/10292))


Synapse 1.37.1 (2021-06-30)
===========================

This release resolves issues (such as [#9490](https://github.com/matrix-org/synapse/issues/9490)) where one busy room could cause head-of-line blocking, starving Synapse from processing events in other rooms, and causing all federated traffic to fall behind. Synapse 1.37.1 processes inbound federation traffic asynchronously, ensuring that one busy room won't impact others. Please upgrade to Synapse 1.37.1 as soon as possible, in order to increase resilience to other traffic spikes.

No significant changes since v1.37.1rc1.


Synapse 1.37.1rc1 (2021-06-29)
==============================

Features
--------

- Handle inbound events from federation asynchronously. ([\#10269](https://github.com/matrix-org/synapse/issues/10269), [\#10272](https://github.com/matrix-org/synapse/issues/10272))


Synapse 1.37.0 (2021-06-29)
===========================

This release deprecates the current spam checker interface. See the [upgrade notes](https://matrix-org.github.io/synapse/develop/upgrade#deprecation-of-the-current-spam-checker-interface) for more information on how to update to the new generic module interface.

This release also removes support for fetching and renewing TLS certificates using the ACME v1 protocol, which has been fully decommissioned by Let's Encrypt on June 1st 2021. Admins previously using this feature should use a [reverse proxy](https://matrix-org.github.io/synapse/develop/reverse_proxy.html) to handle TLS termination, or use an external ACME client (such as [certbot](https://certbot.eff.org/)) to retrieve a certificate and key and provide them to Synapse using the `tls_certificate_path` and `tls_private_key_path` configuration settings.

Synapse 1.37.0rc1 (2021-06-24)
==============================

Features
--------

- Implement "room knocking" as per [MSC2403](https://github.com/matrix-org/matrix-doc/pull/2403). Contributed by @Sorunome and anoa. ([\#6739](https://github.com/matrix-org/synapse/issues/6739), [\#9359](https://github.com/matrix-org/synapse/issues/9359), [\#10167](https://github.com/matrix-org/synapse/issues/10167), [\#10212](https://github.com/matrix-org/synapse/issues/10212), [\#10227](https://github.com/matrix-org/synapse/issues/10227))
- Add experimental support for backfilling history into rooms ([MSC2716](https://github.com/matrix-org/matrix-doc/pull/2716)). ([\#9247](https://github.com/matrix-org/synapse/issues/9247))
- Implement a generic interface for third-party plugin modules. ([\#10062](https://github.com/matrix-org/synapse/issues/10062), [\#10206](https://github.com/matrix-org/synapse/issues/10206))
- Implement config option `sso.update_profile_information` to sync SSO users' profile information with the identity provider each time they login. Currently only displayname is supported. ([\#10108](https://github.com/matrix-org/synapse/issues/10108))
- Ensure that errors during startup are written to the logs and the console. ([\#10191](https://github.com/matrix-org/synapse/issues/10191))


Bugfixes
--------

- Fix a bug introduced in Synapse v1.25.0 that prevented the `ip_range_whitelist` configuration option from working for federation and identity servers. Contributed by @mikure. ([\#10115](https://github.com/matrix-org/synapse/issues/10115))
- Remove a broken import line in Synapse's `admin_cmd` worker. Broke in Synapse v1.33.0. ([\#10154](https://github.com/matrix-org/synapse/issues/10154))
- Fix a bug introduced in Synapse v1.21.0 which could cause `/sync` to return immediately with an empty response. ([\#10157](https://github.com/matrix-org/synapse/issues/10157), [\#10158](https://github.com/matrix-org/synapse/issues/10158))
- Fix a minor bug in the response to `/_matrix/client/r0/user/{user}/openid/request_token` causing `expires_in` to be a float instead of an integer. Contributed by @lukaslihotzki. ([\#10175](https://github.com/matrix-org/synapse/issues/10175))
- Always require users to re-authenticate for dangerous operations: deactivating an account, modifying an account password, and adding 3PIDs. ([\#10184](https://github.com/matrix-org/synapse/issues/10184))
- Fix a bug introduced in Synpase v1.7.2 where remote server count metrics collection would be incorrectly delayed on startup. Found by @heftig. ([\#10195](https://github.com/matrix-org/synapse/issues/10195))
- Fix a bug introduced in Synapse v1.35.1 where an `allow` key of a `m.room.join_rules` event could be applied for incorrect room versions and configurations. ([\#10208](https://github.com/matrix-org/synapse/issues/10208))
- Fix performance regression in responding to user key requests over federation. Introduced in Synapse v1.34.0rc1. ([\#10221](https://github.com/matrix-org/synapse/issues/10221))


Improved Documentation
----------------------

- Add a new guide to decoding request logs. ([\#8436](https://github.com/matrix-org/synapse/issues/8436))
- Mention in the sample homeserver config that you may need to configure max upload size in your reverse proxy. Contributed by @aaronraimist. ([\#10122](https://github.com/matrix-org/synapse/issues/10122))
- Fix broken links in documentation. ([\#10180](https://github.com/matrix-org/synapse/issues/10180))
- Deploy a snapshot of the documentation website upon each new Synapse release. ([\#10198](https://github.com/matrix-org/synapse/issues/10198))


Deprecations and Removals
-------------------------

- The current spam checker interface is deprecated in favour of a new generic modules system. See the [upgrade notes](https://matrix-org.github.io/synapse/develop/upgrade#deprecation-of-the-current-spam-checker-interface) for more information on how to update to the new system. ([\#10062](https://github.com/matrix-org/synapse/issues/10062), [\#10210](https://github.com/matrix-org/synapse/issues/10210), [\#10238](https://github.com/matrix-org/synapse/issues/10238))
- Stop supporting the unstable spaces prefixes from MSC1772. ([\#10161](https://github.com/matrix-org/synapse/issues/10161))
- Remove Synapse's support for automatically fetching and renewing certificates using the ACME v1 protocol. This protocol has been fully turned off by Let's Encrypt for existing installations on June 1st 2021. Admins previously using this feature should use a [reverse proxy](https://matrix-org.github.io/synapse/develop/reverse_proxy.html) to handle TLS termination, or use an external ACME client (such as [certbot](https://certbot.eff.org/)) to retrieve a certificate and key and provide them to Synapse using the `tls_certificate_path` and `tls_private_key_path` configuration settings. ([\#10194](https://github.com/matrix-org/synapse/issues/10194))


Internal Changes
----------------

- Update the database schema versioning to support gradual migration away from legacy tables. ([\#9933](https://github.com/matrix-org/synapse/issues/9933))
- Add type hints to the federation servlets. ([\#10080](https://github.com/matrix-org/synapse/issues/10080))
- Improve OpenTracing for event persistence. ([\#10134](https://github.com/matrix-org/synapse/issues/10134), [\#10193](https://github.com/matrix-org/synapse/issues/10193))
- Clean up the interface for injecting OpenTracing over HTTP. ([\#10143](https://github.com/matrix-org/synapse/issues/10143))
- Limit the number of in-flight `/keys/query` requests from a single device. ([\#10144](https://github.com/matrix-org/synapse/issues/10144))
- Refactor EventPersistenceQueue. ([\#10145](https://github.com/matrix-org/synapse/issues/10145))
- Document `SYNAPSE_TEST_LOG_LEVEL` to see the logger output when running tests. ([\#10148](https://github.com/matrix-org/synapse/issues/10148))
- Update the Complement build tags in GitHub Actions to test currently experimental features. ([\#10155](https://github.com/matrix-org/synapse/issues/10155))
- Add a `synapse_federation_soft_failed_events_total` metric to track how often events are soft failed. ([\#10156](https://github.com/matrix-org/synapse/issues/10156))
- Fetch the corresponding complement branch when performing CI. ([\#10160](https://github.com/matrix-org/synapse/issues/10160))
- Add some developer documentation about boolean columns in database schemas. ([\#10164](https://github.com/matrix-org/synapse/issues/10164))
- Add extra logging fields to better debug where events are being soft failed. ([\#10168](https://github.com/matrix-org/synapse/issues/10168))
- Add debug logging for when we enter and exit `Measure` blocks. ([\#10183](https://github.com/matrix-org/synapse/issues/10183))
- Improve comments in structured logging code. ([\#10188](https://github.com/matrix-org/synapse/issues/10188))
- Update [MSC3083](https://github.com/matrix-org/matrix-doc/pull/3083) support with modifications from the MSC. ([\#10189](https://github.com/matrix-org/synapse/issues/10189))
- Remove redundant DNS lookup limiter. ([\#10190](https://github.com/matrix-org/synapse/issues/10190))
- Upgrade `black` linting tool to 21.6b0. ([\#10197](https://github.com/matrix-org/synapse/issues/10197))
- Expose OpenTracing trace id in response headers. ([\#10199](https://github.com/matrix-org/synapse/issues/10199))


Synapse 1.36.0 (2021-06-15)
===========================

No significant changes.


Synapse 1.36.0rc2 (2021-06-11)
==============================

Bugfixes
--------

- Fix a bug which caused  presence updates to stop working some time after a restart, when using a presence writer worker. Broke in v1.33.0. ([\#10149](https://github.com/matrix-org/synapse/issues/10149))
- Fix a bug when using federation sender worker where it would send out more presence updates than necessary, leading to high resource usage. Broke in v1.33.0. ([\#10163](https://github.com/matrix-org/synapse/issues/10163))
- Fix a bug where Synapse could send the same presence update to a remote twice. ([\#10165](https://github.com/matrix-org/synapse/issues/10165))


Synapse 1.36.0rc1 (2021-06-08)
==============================

Features
--------

- Add new endpoint `/_matrix/client/r0/rooms/{roomId}/aliases` from Client-Server API r0.6.1 (previously [MSC2432](https://github.com/matrix-org/matrix-doc/pull/2432)). ([\#9224](https://github.com/matrix-org/synapse/issues/9224))
- Improve performance of incoming federation transactions in large rooms. ([\#9953](https://github.com/matrix-org/synapse/issues/9953), [\#9973](https://github.com/matrix-org/synapse/issues/9973))
- Rewrite logic around verifying JSON object and fetching server keys to be more performant and use less memory. ([\#10035](https://github.com/matrix-org/synapse/issues/10035))
- Add new admin APIs for unprotecting local media from quarantine. Contributed by @dklimpel. ([\#10040](https://github.com/matrix-org/synapse/issues/10040))
- Add new admin APIs to remove media by media ID from quarantine. Contributed by @dklimpel. ([\#10044](https://github.com/matrix-org/synapse/issues/10044))
- Make reason and score parameters optional for reporting content. Implements [MSC2414](https://github.com/matrix-org/matrix-doc/pull/2414). Contributed by Callum Brown. ([\#10077](https://github.com/matrix-org/synapse/issues/10077))
- Add support for routing more requests to workers. ([\#10084](https://github.com/matrix-org/synapse/issues/10084))
- Report OpenTracing spans for database activity. ([\#10113](https://github.com/matrix-org/synapse/issues/10113), [\#10136](https://github.com/matrix-org/synapse/issues/10136), [\#10141](https://github.com/matrix-org/synapse/issues/10141))
- Significantly reduce memory usage of joining large remote rooms. ([\#10117](https://github.com/matrix-org/synapse/issues/10117))


Bugfixes
--------

- Fixed a bug causing replication requests to fail when receiving a lot of events via federation. ([\#10082](https://github.com/matrix-org/synapse/issues/10082))
- Fix a bug in the `force_tracing_for_users` option introduced in Synapse v1.35 which meant that the OpenTracing spans produced were missing most tags. ([\#10092](https://github.com/matrix-org/synapse/issues/10092))
- Fixed a bug that could cause Synapse to stop notifying application services. Contributed by Willem Mulder. ([\#10107](https://github.com/matrix-org/synapse/issues/10107))
- Fix bug where the server would attempt to fetch the same history in the room from a remote server multiple times in parallel. ([\#10116](https://github.com/matrix-org/synapse/issues/10116))
- Fix a bug introduced in Synapse 1.33.0 which caused replication requests to fail when receiving a lot of very large events via federation. ([\#10118](https://github.com/matrix-org/synapse/issues/10118))
- Fix bug when using workers where pagination requests failed if a remote server returned zero events from `/backfill`. Introduced in 1.35.0. ([\#10133](https://github.com/matrix-org/synapse/issues/10133))


Improved Documentation
----------------------

- Clarify security note regarding hosting Synapse on the same domain as other web applications. ([\#9221](https://github.com/matrix-org/synapse/issues/9221))
- Update CAPTCHA documentation to mention turning off the verify origin feature. Contributed by @aaronraimist. ([\#10046](https://github.com/matrix-org/synapse/issues/10046))
- Tweak wording of database recommendation in `INSTALL.md`. Contributed by @aaronraimist. ([\#10057](https://github.com/matrix-org/synapse/issues/10057))
- Add initial infrastructure for rendering Synapse documentation with mdbook. ([\#10086](https://github.com/matrix-org/synapse/issues/10086))
- Convert the remaining Admin API documentation files to markdown. ([\#10089](https://github.com/matrix-org/synapse/issues/10089))
- Make a link in docs use HTTPS. Contributed by @RhnSharma. ([\#10130](https://github.com/matrix-org/synapse/issues/10130))
- Fix broken link in Docker docs. ([\#10132](https://github.com/matrix-org/synapse/issues/10132))


Deprecations and Removals
-------------------------

- Remove the experimental `spaces_enabled` flag. The spaces features are always available now. ([\#10063](https://github.com/matrix-org/synapse/issues/10063))


Internal Changes
----------------

- Tell CircleCI to build Docker images from `main` branch. ([\#9906](https://github.com/matrix-org/synapse/issues/9906))
- Simplify naming convention for release branches to only include the major and minor version numbers. ([\#10013](https://github.com/matrix-org/synapse/issues/10013))
- Add `parse_strings_from_args` for parsing an array from query parameters. ([\#10048](https://github.com/matrix-org/synapse/issues/10048), [\#10137](https://github.com/matrix-org/synapse/issues/10137))
- Remove some dead code regarding TLS certificate handling. ([\#10054](https://github.com/matrix-org/synapse/issues/10054))
- Remove redundant, unmaintained `convert_server_keys` script. ([\#10055](https://github.com/matrix-org/synapse/issues/10055))
- Improve the error message printed by synctl when synapse fails to start. ([\#10059](https://github.com/matrix-org/synapse/issues/10059))
- Fix GitHub Actions lint for newsfragments. ([\#10069](https://github.com/matrix-org/synapse/issues/10069))
- Update opentracing to inject the right context into the carrier. ([\#10074](https://github.com/matrix-org/synapse/issues/10074))
- Fix up `BatchingQueue` implementation. ([\#10078](https://github.com/matrix-org/synapse/issues/10078))
- Log method and path when dropping request due to size limit. ([\#10091](https://github.com/matrix-org/synapse/issues/10091))
- In Github Actions workflows, summarize the Sytest results in an easy-to-read format. ([\#10094](https://github.com/matrix-org/synapse/issues/10094))
- Make `/sync` do fewer state resolutions. ([\#10102](https://github.com/matrix-org/synapse/issues/10102))
- Add missing type hints to the admin API servlets. ([\#10105](https://github.com/matrix-org/synapse/issues/10105))
- Improve opentracing annotations for `Notifier`. ([\#10111](https://github.com/matrix-org/synapse/issues/10111))
- Enable Prometheus metrics for the jaeger client library. ([\#10112](https://github.com/matrix-org/synapse/issues/10112))
- Work to improve the responsiveness of `/sync` requests. ([\#10124](https://github.com/matrix-org/synapse/issues/10124))
- OpenTracing: use a consistent name for background processes. ([\#10135](https://github.com/matrix-org/synapse/issues/10135))


Synapse 1.35.1 (2021-06-03)
===========================

Bugfixes
--------

- Fix a bug introduced in v1.35.0 where invite-only rooms would be shown to all users in a space, regardless of if the user had access to it. ([\#10109](https://github.com/matrix-org/synapse/issues/10109))


Synapse 1.35.0 (2021-06-01)
===========================

Note that [the tag](https://github.com/matrix-org/synapse/releases/tag/v1.35.0rc3) and [docker images](https://hub.docker.com/layers/matrixdotorg/synapse/v1.35.0rc3/images/sha256-34ccc87bd99a17e2cbc0902e678b5937d16bdc1991ead097eee6096481ecf2c4?context=explore) for `v1.35.0rc3` were incorrectly built. If you are experiencing issues with either, it is recommended to upgrade to the equivalent tag or docker image for the `v1.35.0` release.

Deprecations and Removals
-------------------------

- The core Synapse development team plan to drop support for the [unstable API of MSC2858](https://github.com/matrix-org/matrix-doc/blob/master/proposals/2858-Multiple-SSO-Identity-Providers.md#unstable-prefix), including the undocumented `experimental.msc2858_enabled` config option, in August 2021. Client authors should ensure that their clients are updated to use the stable API (which has been supported since Synapse 1.30) well before that time, to give their users time to upgrade. ([\#10101](https://github.com/matrix-org/synapse/issues/10101))

Bugfixes
--------

- Fixed a bug causing replication requests to fail when receiving a lot of events via federation. Introduced in v1.33.0. ([\#10082](https://github.com/matrix-org/synapse/issues/10082))
- Fix HTTP response size limit to allow joining very large rooms over federation. Introduced in v1.33.0. ([\#10093](https://github.com/matrix-org/synapse/issues/10093))


Internal Changes
----------------

- Log method and path when dropping request due to size limit. ([\#10091](https://github.com/matrix-org/synapse/issues/10091))


Synapse 1.35.0rc2 (2021-05-27)
==============================

Bugfixes
--------

- Fix a bug introduced in v1.35.0rc1 when calling the spaces summary API via a GET request. ([\#10079](https://github.com/matrix-org/synapse/issues/10079))


Synapse 1.35.0rc1 (2021-05-25)
==============================

Features
--------

- Add experimental support to allow a user who could join a restricted room to view it in the spaces summary. ([\#9922](https://github.com/matrix-org/synapse/issues/9922), [\#10007](https://github.com/matrix-org/synapse/issues/10007), [\#10038](https://github.com/matrix-org/synapse/issues/10038))
- Reduce memory usage when joining very large rooms over federation. ([\#9958](https://github.com/matrix-org/synapse/issues/9958))
- Add a configuration option which allows enabling opentracing by user id. ([\#9978](https://github.com/matrix-org/synapse/issues/9978))
- Enable experimental support for [MSC2946](https://github.com/matrix-org/matrix-doc/pull/2946) (spaces summary API) and [MSC3083](https://github.com/matrix-org/matrix-doc/pull/3083) (restricted join rules) by default. ([\#10011](https://github.com/matrix-org/synapse/issues/10011))


Bugfixes
--------

- Fix a bug introduced in v1.26.0 which meant that `synapse_port_db` would not correctly initialise some postgres sequences, requiring manual updates afterwards. ([\#9991](https://github.com/matrix-org/synapse/issues/9991))
- Fix `synctl`'s `--no-daemonize` parameter to work correctly with worker processes. ([\#9995](https://github.com/matrix-org/synapse/issues/9995))
- Fix a validation bug introduced in v1.34.0 in the ordering of spaces in the space summary API. ([\#10002](https://github.com/matrix-org/synapse/issues/10002))
- Fixed deletion of new presence stream states from database. ([\#10014](https://github.com/matrix-org/synapse/issues/10014), [\#10033](https://github.com/matrix-org/synapse/issues/10033))
- Fixed a bug with very high resolution image uploads throwing internal server errors. ([\#10029](https://github.com/matrix-org/synapse/issues/10029))


Updates to the Docker image
---------------------------

- Fix bug introduced in Synapse 1.33.0 which caused a `Permission denied: '/homeserver.log'` error when starting Synapse with the generated log configuration. Contributed by Sergio Miguéns Iglesias. ([\#10045](https://github.com/matrix-org/synapse/issues/10045))


Improved Documentation
----------------------

- Add hardened systemd files as proposed in [#9760](https://github.com/matrix-org/synapse/issues/9760) and added them to `contrib/`. Change the docs to reflect the presence of these files. ([\#9803](https://github.com/matrix-org/synapse/issues/9803))
- Clarify documentation around SSO mapping providers generating unique IDs and localparts. ([\#9980](https://github.com/matrix-org/synapse/issues/9980))
- Updates to the PostgreSQL documentation (`postgres.md`). ([\#9988](https://github.com/matrix-org/synapse/issues/9988), [\#9989](https://github.com/matrix-org/synapse/issues/9989))
- Fix broken link in user directory documentation. Contributed by @junquera. ([\#10016](https://github.com/matrix-org/synapse/issues/10016))
- Add missing room state entry to the table of contents of room admin API. ([\#10043](https://github.com/matrix-org/synapse/issues/10043))


Deprecations and Removals
-------------------------

- Removed support for the deprecated `tls_fingerprints` configuration setting. Contributed by Jerin J Titus. ([\#9280](https://github.com/matrix-org/synapse/issues/9280))


Internal Changes
----------------

- Allow sending full presence to users via workers other than the one that called `ModuleApi.send_local_online_presence_to`. ([\#9823](https://github.com/matrix-org/synapse/issues/9823))
- Update comments in the space summary handler. ([\#9974](https://github.com/matrix-org/synapse/issues/9974))
- Minor enhancements to the `@cachedList` descriptor. ([\#9975](https://github.com/matrix-org/synapse/issues/9975))
- Split multipart email sending into a dedicated handler. ([\#9977](https://github.com/matrix-org/synapse/issues/9977))
- Run `black` on files in the `scripts` directory. ([\#9981](https://github.com/matrix-org/synapse/issues/9981))
- Add missing type hints to `synapse.util` module. ([\#9982](https://github.com/matrix-org/synapse/issues/9982))
- Simplify a few helper functions. ([\#9984](https://github.com/matrix-org/synapse/issues/9984), [\#9985](https://github.com/matrix-org/synapse/issues/9985), [\#9986](https://github.com/matrix-org/synapse/issues/9986))
- Remove unnecessary property from SQLBaseStore. ([\#9987](https://github.com/matrix-org/synapse/issues/9987))
- Remove `keylen` param on `LruCache`. ([\#9993](https://github.com/matrix-org/synapse/issues/9993))
- Update the Grafana dashboard in `contrib/`. ([\#10001](https://github.com/matrix-org/synapse/issues/10001))
- Add a batching queue implementation. ([\#10017](https://github.com/matrix-org/synapse/issues/10017))
- Reduce memory usage when verifying signatures on large numbers of events at once. ([\#10018](https://github.com/matrix-org/synapse/issues/10018))
- Properly invalidate caches for destination retry timings every (instead of expiring entries every 5 minutes). ([\#10036](https://github.com/matrix-org/synapse/issues/10036))
- Fix running complement tests with Synapse workers. ([\#10039](https://github.com/matrix-org/synapse/issues/10039))
- Fix typo in `get_state_ids_for_event` docstring where the return type was incorrect. ([\#10050](https://github.com/matrix-org/synapse/issues/10050))


Synapse 1.34.0 (2021-05-17)
===========================

This release deprecates the `room_invite_state_types` configuration setting. See the [upgrade notes](https://github.com/matrix-org/synapse/blob/release-v1.34.0/UPGRADE.rst#upgrading-to-v1340) for instructions on updating your configuration file to use the new `room_prejoin_state` setting.

This release also deprecates the `POST /_synapse/admin/v1/rooms/<room_id>/delete` admin API route. Server administrators are encouraged to update their scripts to use the new `DELETE /_synapse/admin/v1/rooms/<room_id>` route instead.


No significant changes since v1.34.0rc1.


Synapse 1.34.0rc1 (2021-05-12)
==============================

Features
--------

- Add experimental option to track memory usage of the caches. ([\#9881](https://github.com/matrix-org/synapse/issues/9881))
- Add support for `DELETE /_synapse/admin/v1/rooms/<room_id>`. ([\#9889](https://github.com/matrix-org/synapse/issues/9889))
- Add limits to how often Synapse will GC, ensuring that large servers do not end up GC thrashing if `gc_thresholds` has not been correctly set. ([\#9902](https://github.com/matrix-org/synapse/issues/9902))
- Improve performance of sending events for worker-based deployments using Redis. ([\#9905](https://github.com/matrix-org/synapse/issues/9905), [\#9950](https://github.com/matrix-org/synapse/issues/9950), [\#9951](https://github.com/matrix-org/synapse/issues/9951))
- Improve performance after joining a large room when presence is enabled. ([\#9910](https://github.com/matrix-org/synapse/issues/9910), [\#9916](https://github.com/matrix-org/synapse/issues/9916))
- Support stable identifiers for [MSC1772](https://github.com/matrix-org/matrix-doc/pull/1772) Spaces. `m.space.child` events will now be taken into account when populating the experimental spaces summary response. Please see [the upgrade notes](https://github.com/matrix-org/synapse/blob/release-v1.34.0/UPGRADE.rst#upgrading-to-v1340) if you have customised `room_invite_state_types` in your configuration. ([\#9915](https://github.com/matrix-org/synapse/issues/9915), [\#9966](https://github.com/matrix-org/synapse/issues/9966))
- Improve performance of backfilling in large rooms. ([\#9935](https://github.com/matrix-org/synapse/issues/9935))
- Add a config option to allow you to prevent device display names from being shared over federation. Contributed by @aaronraimist. ([\#9945](https://github.com/matrix-org/synapse/issues/9945))
- Update support for [MSC2946](https://github.com/matrix-org/matrix-doc/pull/2946): Spaces Summary. ([\#9947](https://github.com/matrix-org/synapse/issues/9947), [\#9954](https://github.com/matrix-org/synapse/issues/9954))


Bugfixes
--------

- Fix a bug introduced in v1.32.0 where the associated connection was improperly logged for SQL logging statements. ([\#9895](https://github.com/matrix-org/synapse/issues/9895))
- Correct the type hint for the `user_may_create_room_alias` method of spam checkers. It is provided a `RoomAlias`, not a `str`. ([\#9896](https://github.com/matrix-org/synapse/issues/9896))
- Fix bug where user directory could get out of sync if room visibility and membership changed in quick succession. ([\#9910](https://github.com/matrix-org/synapse/issues/9910))
- Include the `origin_server_ts` property in the experimental [MSC2946](https://github.com/matrix-org/matrix-doc/pull/2946) support to allow clients to properly sort rooms. ([\#9928](https://github.com/matrix-org/synapse/issues/9928))
- Fix bugs introduced in v1.23.0 which made the PostgreSQL port script fail when run with a newly-created SQLite database. ([\#9930](https://github.com/matrix-org/synapse/issues/9930))
- Fix a bug introduced in Synapse 1.29.0 which caused `m.room_key_request` to-device messages sent from one user to another to be dropped. ([\#9961](https://github.com/matrix-org/synapse/issues/9961), [\#9965](https://github.com/matrix-org/synapse/issues/9965))
- Fix a bug introduced in v1.27.0 preventing users and appservices exempt from ratelimiting from creating rooms with many invitees. ([\#9968](https://github.com/matrix-org/synapse/issues/9968))


Updates to the Docker image
---------------------------

- Add `startup_delay` to docker healthcheck to reduce waiting time for coming online and update the documentation with extra options. Contributed by @Maquis196. ([\#9913](https://github.com/matrix-org/synapse/issues/9913))


Improved Documentation
----------------------

- Add `port` argument to the Postgres database sample config section. ([\#9911](https://github.com/matrix-org/synapse/issues/9911))


Deprecations and Removals
-------------------------

- Mark as deprecated `POST /_synapse/admin/v1/rooms/<room_id>/delete`. ([\#9889](https://github.com/matrix-org/synapse/issues/9889))


Internal Changes
----------------

- Reduce the length of Synapse's access tokens. ([\#5588](https://github.com/matrix-org/synapse/issues/5588))
- Export jemalloc stats to Prometheus if it is being used. ([\#9882](https://github.com/matrix-org/synapse/issues/9882))
- Add type hints to presence handler. ([\#9885](https://github.com/matrix-org/synapse/issues/9885))
- Reduce memory usage of the LRU caches. ([\#9886](https://github.com/matrix-org/synapse/issues/9886))
- Add type hints to the `synapse.handlers` module. ([\#9896](https://github.com/matrix-org/synapse/issues/9896))
- Time response time for external cache requests. ([\#9904](https://github.com/matrix-org/synapse/issues/9904))
- Minor fixes to the `make_full_schema.sh` script. ([\#9931](https://github.com/matrix-org/synapse/issues/9931))
- Move database schema files into a common directory. ([\#9932](https://github.com/matrix-org/synapse/issues/9932))
- Add debug logging for lost/delayed to-device messages. ([\#9959](https://github.com/matrix-org/synapse/issues/9959))


Synapse 1.33.2 (2021-05-11)
===========================

Due to the security issue highlighted below, server administrators are encouraged to update Synapse. We are not aware of these vulnerabilities being exploited in the wild.

Security advisory
-----------------

This release fixes a denial of service attack ([CVE-2021-29471](https://github.com/matrix-org/synapse/security/advisories/GHSA-x345-32rc-8h85)) against Synapse's push rules implementation. Server admins are encouraged to upgrade.

Internal Changes
----------------

- Unpin attrs dependency. ([\#9946](https://github.com/matrix-org/synapse/issues/9946))


Synapse 1.33.1 (2021-05-06)
===========================

Bugfixes
--------

- Fix bug where `/sync` would break if using the latest version of `attrs` dependency, by pinning to a previous version. ([\#9937](https://github.com/matrix-org/synapse/issues/9937))


Synapse 1.33.0 (2021-05-05)
===========================

Features
--------

- Build Debian packages for Ubuntu 21.04 (Hirsute Hippo). ([\#9909](https://github.com/matrix-org/synapse/issues/9909))


Synapse 1.33.0rc2 (2021-04-29)
==============================

Bugfixes
--------

- Fix tight loop when handling presence replication when using workers. Introduced in v1.33.0rc1. ([\#9900](https://github.com/matrix-org/synapse/issues/9900))


Synapse 1.33.0rc1 (2021-04-28)
==============================

Features
--------

- Update experimental support for [MSC3083](https://github.com/matrix-org/matrix-doc/pull/3083): restricting room access via group membership. ([\#9800](https://github.com/matrix-org/synapse/issues/9800), [\#9814](https://github.com/matrix-org/synapse/issues/9814))
- Add experimental support for handling presence on a worker. ([\#9819](https://github.com/matrix-org/synapse/issues/9819), [\#9820](https://github.com/matrix-org/synapse/issues/9820), [\#9828](https://github.com/matrix-org/synapse/issues/9828), [\#9850](https://github.com/matrix-org/synapse/issues/9850))
- Return a new template when an user attempts to renew their account multiple times with the same token, stating that their account is set to expire. This replaces the invalid token template that would previously be shown in this case. This change concerns the optional account validity feature. ([\#9832](https://github.com/matrix-org/synapse/issues/9832))


Bugfixes
--------

- Fixes the OIDC SSO flow when using a `public_baseurl` value including a non-root URL path. ([\#9726](https://github.com/matrix-org/synapse/issues/9726))
- Fix thumbnail generation for some sites with non-standard content types. Contributed by @rkfg. ([\#9788](https://github.com/matrix-org/synapse/issues/9788))
- Add some sanity checks to identity server passed to 3PID bind/unbind endpoints. ([\#9802](https://github.com/matrix-org/synapse/issues/9802))
- Limit the size of HTTP responses read over federation. ([\#9833](https://github.com/matrix-org/synapse/issues/9833))
- Fix a bug which could cause Synapse to get stuck in a loop of resyncing device lists. ([\#9867](https://github.com/matrix-org/synapse/issues/9867))
- Fix a long-standing bug where errors from federation did not propagate to the client. ([\#9868](https://github.com/matrix-org/synapse/issues/9868))


Improved Documentation
----------------------

- Add a note to the docker docs mentioning that we mirror upstream's supported Docker platforms. ([\#9801](https://github.com/matrix-org/synapse/issues/9801))


Internal Changes
----------------

- Add a dockerfile for running Synapse in worker-mode under Complement. ([\#9162](https://github.com/matrix-org/synapse/issues/9162))
- Apply `pyupgrade` across the codebase. ([\#9786](https://github.com/matrix-org/synapse/issues/9786))
- Move some replication processing out of `generic_worker`. ([\#9796](https://github.com/matrix-org/synapse/issues/9796))
- Replace `HomeServer.get_config()` with inline references. ([\#9815](https://github.com/matrix-org/synapse/issues/9815))
- Rename some handlers and config modules to not duplicate the top-level module. ([\#9816](https://github.com/matrix-org/synapse/issues/9816))
- Fix a long-standing bug which caused `max_upload_size` to not be correctly enforced. ([\#9817](https://github.com/matrix-org/synapse/issues/9817))
- Reduce CPU usage of the user directory by reusing existing calculated room membership. ([\#9821](https://github.com/matrix-org/synapse/issues/9821))
- Small speed up for joining large remote rooms. ([\#9825](https://github.com/matrix-org/synapse/issues/9825))
- Introduce flake8-bugbear to the test suite and fix some of its lint violations. ([\#9838](https://github.com/matrix-org/synapse/issues/9838))
- Only store the raw data in the in-memory caches, rather than objects that include references to e.g. the data stores. ([\#9845](https://github.com/matrix-org/synapse/issues/9845))
- Limit length of accepted email addresses. ([\#9855](https://github.com/matrix-org/synapse/issues/9855))
- Remove redundant `synapse.types.Collection` type definition. ([\#9856](https://github.com/matrix-org/synapse/issues/9856))
- Handle recently added rate limits correctly when using `--no-rate-limit` with the demo scripts. ([\#9858](https://github.com/matrix-org/synapse/issues/9858))
- Disable invite rate-limiting by default when running the unit tests. ([\#9871](https://github.com/matrix-org/synapse/issues/9871))
- Pass a reactor into `SynapseSite` to make testing easier. ([\#9874](https://github.com/matrix-org/synapse/issues/9874))
- Make `DomainSpecificString` an `attrs` class. ([\#9875](https://github.com/matrix-org/synapse/issues/9875))
- Add type hints to `synapse.api.auth` and `synapse.api.auth_blocking` modules. ([\#9876](https://github.com/matrix-org/synapse/issues/9876))
- Remove redundant `_PushHTTPChannel` test class. ([\#9878](https://github.com/matrix-org/synapse/issues/9878))
- Remove backwards-compatibility code for Python versions < 3.6. ([\#9879](https://github.com/matrix-org/synapse/issues/9879))
- Small performance improvement around handling new local presence updates. ([\#9887](https://github.com/matrix-org/synapse/issues/9887))


Synapse 1.32.2 (2021-04-22)
===========================

This release includes a fix for a regression introduced in 1.32.0.

Bugfixes
--------

- Fix a regression in Synapse 1.32.0 and 1.32.1 which caused `LoggingContext` errors in plugins. ([\#9857](https://github.com/matrix-org/synapse/issues/9857))


Synapse 1.32.1 (2021-04-21)
===========================

This release fixes [a regression](https://github.com/matrix-org/synapse/issues/9853)
in Synapse 1.32.0 that caused connected Prometheus instances to become unstable.

However, as this release is still subject to the `LoggingContext` change in 1.32.0,
it is recommended to remain on or downgrade to 1.31.0.

Bugfixes
--------

- Fix a regression in Synapse 1.32.0 which caused Synapse to report large numbers of Prometheus time series, potentially overwhelming Prometheus instances. ([\#9854](https://github.com/matrix-org/synapse/issues/9854))


Synapse 1.32.0 (2021-04-20)
===========================

**Note:** This release introduces [a regression](https://github.com/matrix-org/synapse/issues/9853)
that can overwhelm connected Prometheus instances. This issue was not present in
1.32.0rc1. If affected, it is recommended to downgrade to 1.31.0 in the meantime, and
follow [these instructions](https://github.com/matrix-org/synapse/pull/9854#issuecomment-823472183)
to clean up any excess writeahead logs.

**Note:** This release also mistakenly included a change that may affected Synapse
modules that import `synapse.logging.context.LoggingContext`, such as
[synapse-s3-storage-provider](https://github.com/matrix-org/synapse-s3-storage-provider).
This will be fixed in a later Synapse version.

**Note:** This release requires Python 3.6+ and Postgres 9.6+ or SQLite 3.22+.

This release removes the deprecated `GET /_synapse/admin/v1/users/<user_id>` admin API. Please use the [v2 API](https://github.com/matrix-org/synapse/blob/develop/docs/admin_api/user_admin_api.rst#query-user-account) instead, which has improved capabilities.

This release requires Application Services to use type `m.login.application_service` when registering users via the `/_matrix/client/r0/register` endpoint to comply with the spec. Please ensure your Application Services are up to date.

If you are using the `packages.matrix.org` Debian repository for Synapse packages,
note that we have recently updated the expiry date on the gpg signing key. If you see an
error similar to `The following signatures were invalid: EXPKEYSIG F473DD4473365DE1`, you
will need to get a fresh copy of the keys. You can do so with:

```sh
sudo wget -O /usr/share/keyrings/matrix-org-archive-keyring.gpg https://packages.matrix.org/debian/matrix-org-archive-keyring.gpg
```

Bugfixes
--------

- Fix the log lines of nested logging contexts. Broke in 1.32.0rc1. ([\#9829](https://github.com/matrix-org/synapse/issues/9829))


Synapse 1.32.0rc1 (2021-04-13)
==============================

Features
--------

- Add a Synapse module for routing presence updates between users. ([\#9491](https://github.com/matrix-org/synapse/issues/9491))
- Add an admin API to manage ratelimit for a specific user. ([\#9648](https://github.com/matrix-org/synapse/issues/9648))
- Include request information in structured logging output. ([\#9654](https://github.com/matrix-org/synapse/issues/9654))
- Add `order_by` to the admin API `GET /_synapse/admin/v2/users`. Contributed by @dklimpel. ([\#9691](https://github.com/matrix-org/synapse/issues/9691))
- Replace the `room_invite_state_types` configuration setting with `room_prejoin_state`. ([\#9700](https://github.com/matrix-org/synapse/issues/9700))
- Add experimental support for [MSC3083](https://github.com/matrix-org/matrix-doc/pull/3083): restricting room access via group membership. ([\#9717](https://github.com/matrix-org/synapse/issues/9717), [\#9735](https://github.com/matrix-org/synapse/issues/9735))
- Update experimental support for Spaces: include `m.room.create` in the room state sent with room-invites. ([\#9710](https://github.com/matrix-org/synapse/issues/9710))
- Synapse now requires Python 3.6 or later. It also requires Postgres 9.6 or later or SQLite 3.22 or later. ([\#9766](https://github.com/matrix-org/synapse/issues/9766))


Bugfixes
--------

- Prevent `synapse_forward_extremities` and `synapse_excess_extremity_events` Prometheus metrics from initially reporting zero-values after startup. ([\#8926](https://github.com/matrix-org/synapse/issues/8926))
- Fix recently added ratelimits to correctly honour the application service `rate_limited` flag. ([\#9711](https://github.com/matrix-org/synapse/issues/9711))
- Fix longstanding bug which caused `duplicate key value violates unique constraint "remote_media_cache_thumbnails_media_origin_media_id_thumbna_key"` errors. ([\#9725](https://github.com/matrix-org/synapse/issues/9725))
- Fix bug where sharded federation senders could get stuck repeatedly querying the DB in a loop, using lots of CPU. ([\#9770](https://github.com/matrix-org/synapse/issues/9770))
- Fix duplicate logging of exceptions thrown during federation transaction processing. ([\#9780](https://github.com/matrix-org/synapse/issues/9780))


Updates to the Docker image
---------------------------

- Move opencontainers labels to the final Docker image such that users can inspect them. ([\#9765](https://github.com/matrix-org/synapse/issues/9765))


Improved Documentation
----------------------

- Make the `allowed_local_3pids` regex example in the sample config stricter. ([\#9719](https://github.com/matrix-org/synapse/issues/9719))


Deprecations and Removals
-------------------------

- Remove old admin API `GET /_synapse/admin/v1/users/<user_id>`. ([\#9401](https://github.com/matrix-org/synapse/issues/9401))
- Make `/_matrix/client/r0/register` expect a type of `m.login.application_service` when an Application Service registers a user, to align with [the relevant spec](https://spec.matrix.org/unstable/application-service-api/#server-admin-style-permissions). ([\#9548](https://github.com/matrix-org/synapse/issues/9548))


Internal Changes
----------------

- Replace deprecated `imp` module with successor `importlib`. Contributed by Cristina Muñoz. ([\#9718](https://github.com/matrix-org/synapse/issues/9718))
- Experiment with GitHub Actions for CI. ([\#9661](https://github.com/matrix-org/synapse/issues/9661))
- Introduce flake8-bugbear to the test suite and fix some of its lint violations. ([\#9682](https://github.com/matrix-org/synapse/issues/9682))
- Update `scripts-dev/complement.sh` to use a local checkout of Complement, allow running a subset of tests and have it use Synapse's Complement test blacklist. ([\#9685](https://github.com/matrix-org/synapse/issues/9685))
- Improve Jaeger tracing for `to_device` messages. ([\#9686](https://github.com/matrix-org/synapse/issues/9686))
- Add release helper script for automating part of the Synapse release process. ([\#9713](https://github.com/matrix-org/synapse/issues/9713))
- Add type hints to expiring cache. ([\#9730](https://github.com/matrix-org/synapse/issues/9730))
- Convert various testcases to `HomeserverTestCase`. ([\#9736](https://github.com/matrix-org/synapse/issues/9736))
- Start linting mypy with `no_implicit_optional`. ([\#9742](https://github.com/matrix-org/synapse/issues/9742))
- Add missing type hints to federation handler and server. ([\#9743](https://github.com/matrix-org/synapse/issues/9743))
- Check that a `ConfigError` is raised, rather than simply `Exception`, when appropriate in homeserver config file generation tests. ([\#9753](https://github.com/matrix-org/synapse/issues/9753))
- Fix incompatibility with `tox` 2.5. ([\#9769](https://github.com/matrix-org/synapse/issues/9769))
- Enable Complement tests for [MSC2946](https://github.com/matrix-org/matrix-doc/pull/2946): Spaces Summary API. ([\#9771](https://github.com/matrix-org/synapse/issues/9771))
- Use mock from the standard library instead of a separate package. ([\#9772](https://github.com/matrix-org/synapse/issues/9772))
- Update Black configuration to target Python 3.6. ([\#9781](https://github.com/matrix-org/synapse/issues/9781))
- Add option to skip unit tests when building Debian packages. ([\#9793](https://github.com/matrix-org/synapse/issues/9793))


Synapse 1.31.0 (2021-04-06)
===========================

**Note:** As announced in v1.25.0, and in line with the deprecation policy for platform dependencies, this is the last release to support Python 3.5 and PostgreSQL 9.5. Future versions of Synapse will require Python 3.6+ and PostgreSQL 9.6+, as per our [deprecation policy](docs/deprecation_policy.md).

This is also the last release that the Synapse team will be publishing packages for Debian Stretch and Ubuntu Xenial.


Improved Documentation
----------------------

- Add a document describing the deprecation policy for platform dependencies. ([\#9723](https://github.com/matrix-org/synapse/issues/9723))


Internal Changes
----------------

- Revert using `dmypy run` in lint script. ([\#9720](https://github.com/matrix-org/synapse/issues/9720))
- Pin flake8-bugbear's version. ([\#9734](https://github.com/matrix-org/synapse/issues/9734))


Synapse 1.31.0rc1 (2021-03-30)
==============================

Features
--------

- Add support to OpenID Connect login for requiring attributes on the `userinfo` response. Contributed by Hubbe King. ([\#9609](https://github.com/matrix-org/synapse/issues/9609))
- Add initial experimental support for a "space summary" API. ([\#9643](https://github.com/matrix-org/synapse/issues/9643), [\#9652](https://github.com/matrix-org/synapse/issues/9652), [\#9653](https://github.com/matrix-org/synapse/issues/9653))
- Add support for the busy presence state as described in [MSC3026](https://github.com/matrix-org/matrix-doc/pull/3026). ([\#9644](https://github.com/matrix-org/synapse/issues/9644))
- Add support for credentials for proxy authentication in the `HTTPS_PROXY` environment variable. ([\#9657](https://github.com/matrix-org/synapse/issues/9657))


Bugfixes
--------

- Fix a longstanding bug that could cause issues when editing a reply to a message. ([\#9585](https://github.com/matrix-org/synapse/issues/9585))
- Fix the `/capabilities` endpoint to return `m.change_password` as disabled if the local password database is not used for authentication. Contributed by @dklimpel. ([\#9588](https://github.com/matrix-org/synapse/issues/9588))
- Check if local passwords are enabled before setting them for the user. ([\#9636](https://github.com/matrix-org/synapse/issues/9636))
- Fix a bug where federation sending can stall due to `concurrent access` database exceptions when it falls behind. ([\#9639](https://github.com/matrix-org/synapse/issues/9639))
- Fix a bug introduced in Synapse 1.30.1 which meant the suggested `pip` incantation to install an updated `cryptography` was incorrect. ([\#9699](https://github.com/matrix-org/synapse/issues/9699))


Updates to the Docker image
---------------------------

- Speed up Docker builds and make it nicer to test against Complement while developing (install all dependencies before copying the project). ([\#9610](https://github.com/matrix-org/synapse/issues/9610))
- Include [opencontainers labels](https://github.com/opencontainers/image-spec/blob/master/annotations.md#pre-defined-annotation-keys) in the Docker image. ([\#9612](https://github.com/matrix-org/synapse/issues/9612))


Improved Documentation
----------------------

- Clarify that `register_new_matrix_user` is present also when installed via non-pip package. ([\#9074](https://github.com/matrix-org/synapse/issues/9074))
- Update source install documentation to mention platform prerequisites before the source install steps. ([\#9667](https://github.com/matrix-org/synapse/issues/9667))
- Improve worker documentation for fallback/web auth endpoints. ([\#9679](https://github.com/matrix-org/synapse/issues/9679))
- Update the sample configuration for OIDC authentication. ([\#9695](https://github.com/matrix-org/synapse/issues/9695))


Internal Changes
----------------

- Preparatory steps for removing redundant `outlier` data from `event_json.internal_metadata` column. ([\#9411](https://github.com/matrix-org/synapse/issues/9411))
- Add type hints to the caching module. ([\#9442](https://github.com/matrix-org/synapse/issues/9442))
- Introduce flake8-bugbear to the test suite and fix some of its lint violations. ([\#9499](https://github.com/matrix-org/synapse/issues/9499), [\#9659](https://github.com/matrix-org/synapse/issues/9659))
- Add additional type hints to the Homeserver object. ([\#9631](https://github.com/matrix-org/synapse/issues/9631), [\#9638](https://github.com/matrix-org/synapse/issues/9638), [\#9675](https://github.com/matrix-org/synapse/issues/9675), [\#9681](https://github.com/matrix-org/synapse/issues/9681))
- Only save remote cross-signing and device keys if they're different from the current ones. ([\#9634](https://github.com/matrix-org/synapse/issues/9634))
- Rename storage function to fix spelling and not conflict with another function's name. ([\#9637](https://github.com/matrix-org/synapse/issues/9637))
- Improve performance of federation catch up by sending the latest events in the room to the remote, rather than just the last event sent by the local server. ([\#9640](https://github.com/matrix-org/synapse/issues/9640), [\#9664](https://github.com/matrix-org/synapse/issues/9664))
- In the `federation_client` commandline client, stop automatically adding the URL prefix, so that servlets on other prefixes can be tested. ([\#9645](https://github.com/matrix-org/synapse/issues/9645))
- In the `federation_client` commandline client, handle inline `signing_key`s in `homeserver.yaml`. ([\#9647](https://github.com/matrix-org/synapse/issues/9647))
- Fixed some antipattern issues to improve code quality. ([\#9649](https://github.com/matrix-org/synapse/issues/9649))
- Add a storage method for pulling all current user presence state from the database. ([\#9650](https://github.com/matrix-org/synapse/issues/9650))
- Import `HomeServer` from the proper module. ([\#9665](https://github.com/matrix-org/synapse/issues/9665))
- Increase default join ratelimiting burst rate. ([\#9674](https://github.com/matrix-org/synapse/issues/9674))
- Add type hints to third party event rules and visibility modules. ([\#9676](https://github.com/matrix-org/synapse/issues/9676))
- Bump mypy-zope to 0.2.13 to fix "Cannot determine consistent method resolution order (MRO)" errors when running mypy a second time. ([\#9678](https://github.com/matrix-org/synapse/issues/9678))
- Use interpreter from `$PATH` via `/usr/bin/env` instead of absolute paths in various scripts. ([\#9689](https://github.com/matrix-org/synapse/issues/9689))
- Make it possible to use `dmypy`. ([\#9692](https://github.com/matrix-org/synapse/issues/9692))
- Suppress "CryptographyDeprecationWarning: int_from_bytes is deprecated". ([\#9698](https://github.com/matrix-org/synapse/issues/9698))
- Use `dmypy run` in lint script for improved performance in type-checking while developing. ([\#9701](https://github.com/matrix-org/synapse/issues/9701))
- Fix undetected mypy error when using Python 3.6. ([\#9703](https://github.com/matrix-org/synapse/issues/9703))
- Fix type-checking CI on develop. ([\#9709](https://github.com/matrix-org/synapse/issues/9709))


Synapse 1.30.1 (2021-03-26)
===========================

This release is identical to Synapse 1.30.0, with the exception of explicitly
setting a minimum version of Python's Cryptography library to ensure that users
of Synapse are protected from the recent [OpenSSL security advisories](https://mta.openssl.org/pipermail/openssl-announce/2021-March/000198.html),
especially CVE-2021-3449.

Note that Cryptography defaults to bundling its own statically linked copy of
OpenSSL, which means that you may not be protected by your operating system's
security updates.

It's also worth noting that Cryptography no longer supports Python 3.5, so
admins deploying to older environments may not be protected against this or
future vulnerabilities. Synapse will be dropping support for Python 3.5 at the
end of March.


Updates to the Docker image
---------------------------

- Ensure that the docker container has up to date versions of openssl. ([\#9697](https://github.com/matrix-org/synapse/issues/9697))


Internal Changes
----------------

- Enforce that `cryptography` dependency is up to date to ensure it has the most recent openssl patches. ([\#9697](https://github.com/matrix-org/synapse/issues/9697))


Synapse 1.30.0 (2021-03-22)
===========================

Note that this release deprecates the ability for appservices to
call `POST /_matrix/client/r0/register`  without the body parameter `type`. Appservice
developers should use a `type` value of `m.login.application_service` as
per [the spec](https://matrix.org/docs/spec/application_service/r0.1.2#server-admin-style-permissions).
In future releases, calling this endpoint with an access token - but without a `m.login.application_service`
type - will fail.


No significant changes.


Synapse 1.30.0rc1 (2021-03-16)
==============================

Features
--------

- Add prometheus metrics for number of users successfully registering and logging in. ([\#9510](https://github.com/matrix-org/synapse/issues/9510), [\#9511](https://github.com/matrix-org/synapse/issues/9511), [\#9573](https://github.com/matrix-org/synapse/issues/9573))
- Add `synapse_federation_last_sent_pdu_time` and `synapse_federation_last_received_pdu_time` prometheus metrics, which monitor federation delays by reporting the timestamps of messages sent and received to a set of remote servers. ([\#9540](https://github.com/matrix-org/synapse/issues/9540))
- Add support for generating JSON Web Tokens dynamically for use as OIDC client secrets. ([\#9549](https://github.com/matrix-org/synapse/issues/9549))
- Optimise handling of incomplete room history for incoming federation. ([\#9601](https://github.com/matrix-org/synapse/issues/9601))
- Finalise support for allowing clients to pick an SSO Identity Provider ([MSC2858](https://github.com/matrix-org/matrix-doc/pull/2858)). ([\#9617](https://github.com/matrix-org/synapse/issues/9617))
- Tell spam checker modules about the SSO IdP a user registered through if one was used. ([\#9626](https://github.com/matrix-org/synapse/issues/9626))


Bugfixes
--------

- Fix long-standing bug when generating thumbnails for some images with transparency: `TypeError: cannot unpack non-iterable int object`. ([\#9473](https://github.com/matrix-org/synapse/issues/9473))
- Purge chain cover indexes for events that were purged prior to Synapse v1.29.0. ([\#9542](https://github.com/matrix-org/synapse/issues/9542), [\#9583](https://github.com/matrix-org/synapse/issues/9583))
- Fix bug where federation requests were not correctly retried on 5xx responses. ([\#9567](https://github.com/matrix-org/synapse/issues/9567))
- Fix re-activating an account via the admin API when local passwords are disabled. ([\#9587](https://github.com/matrix-org/synapse/issues/9587))
- Fix a bug introduced in Synapse 1.20 which caused incoming federation transactions to stack up, causing slow recovery from outages. ([\#9597](https://github.com/matrix-org/synapse/issues/9597))
- Fix a bug introduced in v1.28.0 where the OpenID Connect callback endpoint could error with a `MacaroonInitException`. ([\#9620](https://github.com/matrix-org/synapse/issues/9620))
- Fix Internal Server Error on `GET /_synapse/client/saml2/authn_response` request. ([\#9623](https://github.com/matrix-org/synapse/issues/9623))


Updates to the Docker image
---------------------------

- Make use of an improved malloc implementation (`jemalloc`) in the docker image. ([\#8553](https://github.com/matrix-org/synapse/issues/8553))


Improved Documentation
----------------------

- Add relayd entry to reverse proxy example configurations. ([\#9508](https://github.com/matrix-org/synapse/issues/9508))
- Improve the SAML2 upgrade notes for 1.27.0. ([\#9550](https://github.com/matrix-org/synapse/issues/9550))
- Link to the "List user's media" admin API from the media admin API docs. ([\#9571](https://github.com/matrix-org/synapse/issues/9571))
- Clarify the spam checker modules documentation example to mention that `parse_config` is a required method. ([\#9580](https://github.com/matrix-org/synapse/issues/9580))
- Clarify the sample configuration for `stats` settings. ([\#9604](https://github.com/matrix-org/synapse/issues/9604))


Deprecations and Removals
-------------------------

- The `synapse_federation_last_sent_pdu_age` and `synapse_federation_last_received_pdu_age` prometheus metrics have been removed. They are replaced by `synapse_federation_last_sent_pdu_time` and `synapse_federation_last_received_pdu_time`. ([\#9540](https://github.com/matrix-org/synapse/issues/9540))
- Registering an Application Service user without using the `m.login.application_service` login type will be unsupported in an upcoming Synapse release. ([\#9559](https://github.com/matrix-org/synapse/issues/9559))


Internal Changes
----------------

- Add tests to ResponseCache. ([\#9458](https://github.com/matrix-org/synapse/issues/9458))
- Add type hints to purge room and server notice admin API. ([\#9520](https://github.com/matrix-org/synapse/issues/9520))
- Add extra logging to ObservableDeferred when callbacks throw exceptions. ([\#9523](https://github.com/matrix-org/synapse/issues/9523))
- Fix incorrect type hints. ([\#9528](https://github.com/matrix-org/synapse/issues/9528), [\#9543](https://github.com/matrix-org/synapse/issues/9543), [\#9591](https://github.com/matrix-org/synapse/issues/9591), [\#9608](https://github.com/matrix-org/synapse/issues/9608), [\#9618](https://github.com/matrix-org/synapse/issues/9618))
- Add an additional test for purging a room. ([\#9541](https://github.com/matrix-org/synapse/issues/9541))
- Add a `.git-blame-ignore-revs` file with the hashes of auto-formatting. ([\#9560](https://github.com/matrix-org/synapse/issues/9560))
- Increase the threshold before which outbound federation to a server goes into "catch up" mode, which is expensive for the remote server to handle. ([\#9561](https://github.com/matrix-org/synapse/issues/9561))
- Fix spurious errors reported by the `config-lint.sh` script. ([\#9562](https://github.com/matrix-org/synapse/issues/9562))
- Fix type hints and tests for BlacklistingAgentWrapper and BlacklistingReactorWrapper. ([\#9563](https://github.com/matrix-org/synapse/issues/9563))
- Do not have mypy ignore type hints from unpaddedbase64. ([\#9568](https://github.com/matrix-org/synapse/issues/9568))
- Improve efficiency of calculating the auth chain in large rooms. ([\#9576](https://github.com/matrix-org/synapse/issues/9576))
- Convert `synapse.types.Requester` to an `attrs` class. ([\#9586](https://github.com/matrix-org/synapse/issues/9586))
- Add logging for redis connection setup. ([\#9590](https://github.com/matrix-org/synapse/issues/9590))
- Improve logging when processing incoming transactions. ([\#9596](https://github.com/matrix-org/synapse/issues/9596))
- Remove unused `stats.retention` setting, and emit a warning if stats are disabled. ([\#9604](https://github.com/matrix-org/synapse/issues/9604))
- Prevent attempting to bundle aggregations for state events in /context APIs. ([\#9619](https://github.com/matrix-org/synapse/issues/9619))


Synapse 1.29.0 (2021-03-08)
===========================

Note that synapse now expects an `X-Forwarded-Proto` header when used with a reverse proxy. Please see the [upgrade notes](docs/upgrade.md#upgrading-to-v1290) for more details on this change.


No significant changes.


Synapse 1.29.0rc1 (2021-03-04)
==============================

Features
--------

- Add rate limiters to cross-user key sharing requests. ([\#8957](https://github.com/matrix-org/synapse/issues/8957))
- Add `order_by` to the admin API `GET /_synapse/admin/v1/users/<user_id>/media`. Contributed by @dklimpel. ([\#8978](https://github.com/matrix-org/synapse/issues/8978))
- Add some configuration settings to make users' profile data more private. ([\#9203](https://github.com/matrix-org/synapse/issues/9203))
- The `no_proxy` and `NO_PROXY` environment variables are now respected in proxied HTTP clients with the lowercase form taking precedence if both are present. Additionally, the lowercase `https_proxy` environment variable is now respected in proxied HTTP clients on top of existing support for the uppercase `HTTPS_PROXY` form and takes precedence if both are present. Contributed by Timothy Leung. ([\#9372](https://github.com/matrix-org/synapse/issues/9372))
- Add a configuration option, `user_directory.prefer_local_users`, which when enabled will make it more likely for users on the same server as you to appear above other users. ([\#9383](https://github.com/matrix-org/synapse/issues/9383), [\#9385](https://github.com/matrix-org/synapse/issues/9385))
- Add support for regenerating thumbnails if they have been deleted but the original image is still stored. ([\#9438](https://github.com/matrix-org/synapse/issues/9438))
- Add support for `X-Forwarded-Proto` header when using a reverse proxy. ([\#9472](https://github.com/matrix-org/synapse/issues/9472), [\#9501](https://github.com/matrix-org/synapse/issues/9501), [\#9512](https://github.com/matrix-org/synapse/issues/9512), [\#9539](https://github.com/matrix-org/synapse/issues/9539))


Bugfixes
--------

- Fix a bug where users' pushers were not all deleted when they deactivated their account. ([\#9285](https://github.com/matrix-org/synapse/issues/9285), [\#9516](https://github.com/matrix-org/synapse/issues/9516))
- Fix a bug where a lot of unnecessary presence updates were sent when joining a room. ([\#9402](https://github.com/matrix-org/synapse/issues/9402))
- Fix a bug that caused multiple calls to the experimental `shared_rooms` endpoint to return stale results. ([\#9416](https://github.com/matrix-org/synapse/issues/9416))
- Fix a bug in single sign-on which could cause a "No session cookie found" error. ([\#9436](https://github.com/matrix-org/synapse/issues/9436))
- Fix bug introduced in v1.27.0 where allowing a user to choose their own username when logging in via single sign-on did not work unless an `idp_icon` was defined. ([\#9440](https://github.com/matrix-org/synapse/issues/9440))
- Fix a bug introduced in v1.26.0 where some sequences were not properly configured when running `synapse_port_db`. ([\#9449](https://github.com/matrix-org/synapse/issues/9449))
- Fix deleting pushers when using sharded pushers. ([\#9465](https://github.com/matrix-org/synapse/issues/9465), [\#9466](https://github.com/matrix-org/synapse/issues/9466), [\#9479](https://github.com/matrix-org/synapse/issues/9479), [\#9536](https://github.com/matrix-org/synapse/issues/9536))
- Fix missing startup checks for the consistency of certain PostgreSQL sequences. ([\#9470](https://github.com/matrix-org/synapse/issues/9470))
- Fix a long-standing bug where the media repository could leak file descriptors while previewing media. ([\#9497](https://github.com/matrix-org/synapse/issues/9497))
- Properly purge the event chain cover index when purging history. ([\#9498](https://github.com/matrix-org/synapse/issues/9498))
- Fix missing chain cover index due to a schema delta not being applied correctly. Only affected servers that ran development versions. ([\#9503](https://github.com/matrix-org/synapse/issues/9503))
- Fix a bug introduced in v1.25.0 where `/_synapse/admin/join/` would fail when given a room alias. ([\#9506](https://github.com/matrix-org/synapse/issues/9506))
- Prevent presence background jobs from running when presence is disabled. ([\#9530](https://github.com/matrix-org/synapse/issues/9530))
- Fix rare edge case that caused a background update to fail if the server had rejected an event that had duplicate auth events. ([\#9537](https://github.com/matrix-org/synapse/issues/9537))


Improved Documentation
----------------------

- Update the example systemd config to propagate reloads to individual units. ([\#9463](https://github.com/matrix-org/synapse/issues/9463))


Internal Changes
----------------

- Add documentation and type hints to `parse_duration`. ([\#9432](https://github.com/matrix-org/synapse/issues/9432))
- Remove vestiges of `uploads_path` configuration setting. ([\#9462](https://github.com/matrix-org/synapse/issues/9462))
- Add a comment about systemd-python. ([\#9464](https://github.com/matrix-org/synapse/issues/9464))
- Test that we require validated email for email pushers. ([\#9496](https://github.com/matrix-org/synapse/issues/9496))
- Allow python to generate bytecode for synapse. ([\#9502](https://github.com/matrix-org/synapse/issues/9502))
- Fix incorrect type hints. ([\#9515](https://github.com/matrix-org/synapse/issues/9515), [\#9518](https://github.com/matrix-org/synapse/issues/9518))
- Add type hints to device and event report admin API. ([\#9519](https://github.com/matrix-org/synapse/issues/9519))
- Add type hints to user admin API. ([\#9521](https://github.com/matrix-org/synapse/issues/9521))
- Bump the versions of mypy and mypy-zope used for static type checking. ([\#9529](https://github.com/matrix-org/synapse/issues/9529))


Synapse 1.28.0 (2021-02-25)
===========================

Note that this release drops support for ARMv7 in the official Docker images, due to repeated problems building for ARMv7 (and the associated maintenance burden this entails).

This release also fixes the documentation included in v1.27.0 around the callback URI for SAML2 identity providers. If your server is configured to use single sign-on via a SAML2 IdP, you may need to make configuration changes. Please review the [upgrade notes](docs/upgrade.md) for more details on these changes.


Internal Changes
----------------

- Revert change in v1.28.0rc1 to remove the deprecated SAML endpoint. ([\#9474](https://github.com/matrix-org/synapse/issues/9474))


Synapse 1.28.0rc1 (2021-02-19)
==============================

Removal warning
---------------

The v1 list accounts API is deprecated and will be removed in a future release.
This API was undocumented and misleading. It can be replaced by the
[v2 list accounts API](https://github.com/matrix-org/synapse/blob/release-v1.28.0/docs/admin_api/user_admin_api.rst#list-accounts),
which has been available since Synapse 1.7.0 (2019-12-13).

Please check if you're using any scripts which use the admin API and replace
`GET /_synapse/admin/v1/users/<user_id>` with `GET /_synapse/admin/v2/users`.


Features
--------

- New admin API to get the context of an event: `/_synapse/admin/rooms/{roomId}/context/{eventId}`. ([\#9150](https://github.com/matrix-org/synapse/issues/9150))
- Further improvements to the user experience of registration via single sign-on. ([\#9300](https://github.com/matrix-org/synapse/issues/9300), [\#9301](https://github.com/matrix-org/synapse/issues/9301))
- Add hook to spam checker modules that allow checking file uploads and remote downloads. ([\#9311](https://github.com/matrix-org/synapse/issues/9311))
- Add support for receiving OpenID Connect authentication responses via form `POST`s rather than `GET`s. ([\#9376](https://github.com/matrix-org/synapse/issues/9376))
- Add the shadow-banning status to the admin API for user info. ([\#9400](https://github.com/matrix-org/synapse/issues/9400))


Bugfixes
--------

- Fix long-standing bug where sending email notifications would fail for rooms that the server had since left. ([\#9257](https://github.com/matrix-org/synapse/issues/9257))
- Fix bug introduced in Synapse 1.27.0rc1 which meant the "session expired" error page during SSO registration was badly formatted. ([\#9296](https://github.com/matrix-org/synapse/issues/9296))
- Assert a maximum length for some parameters for spec compliance. ([\#9321](https://github.com/matrix-org/synapse/issues/9321), [\#9393](https://github.com/matrix-org/synapse/issues/9393))
- Fix additional errors when previewing URLs: "AttributeError 'NoneType' object has no attribute 'xpath'" and "ValueError: Unicode strings with encoding declaration are not supported. Please use bytes input or XML fragments without declaration.". ([\#9333](https://github.com/matrix-org/synapse/issues/9333))
- Fix a bug causing Synapse to impose the wrong type constraints on fields when processing responses from appservices to `/_matrix/app/v1/thirdparty/user/{protocol}`. ([\#9361](https://github.com/matrix-org/synapse/issues/9361))
- Fix bug where Synapse would occasionally stop reconnecting to Redis after the connection was lost. ([\#9391](https://github.com/matrix-org/synapse/issues/9391))
- Fix a long-standing bug when upgrading a room: "TypeError: '>' not supported between instances of 'NoneType' and 'int'". ([\#9395](https://github.com/matrix-org/synapse/issues/9395))
- Reduce the amount of memory used when generating the URL preview of a file that is larger than the `max_spider_size`. ([\#9421](https://github.com/matrix-org/synapse/issues/9421))
- Fix a long-standing bug in the deduplication of old presence, resulting in no deduplication. ([\#9425](https://github.com/matrix-org/synapse/issues/9425))
- The `ui_auth.session_timeout` config option can now be specified in terms of number of seconds/minutes/etc/. Contributed by Rishabh Arya. ([\#9426](https://github.com/matrix-org/synapse/issues/9426))
- Fix a bug introduced in v1.27.0: "TypeError: int() argument must be a string, a bytes-like object or a number, not 'NoneType." related to the user directory. ([\#9428](https://github.com/matrix-org/synapse/issues/9428))


Updates to the Docker image
---------------------------

- Drop support for ARMv7 in Docker images. ([\#9433](https://github.com/matrix-org/synapse/issues/9433))


Improved Documentation
----------------------

- Reorganize CHANGELOG.md. ([\#9281](https://github.com/matrix-org/synapse/issues/9281))
- Add note to `auto_join_rooms` config option explaining existing rooms must be publicly joinable. ([\#9291](https://github.com/matrix-org/synapse/issues/9291))
- Correct name of Synapse's service file in TURN howto. ([\#9308](https://github.com/matrix-org/synapse/issues/9308))
- Fix the braces in the `oidc_providers` section of the sample config. ([\#9317](https://github.com/matrix-org/synapse/issues/9317))
- Update installation instructions on Fedora. ([\#9322](https://github.com/matrix-org/synapse/issues/9322))
- Add HTTP/2 support to the nginx example configuration. Contributed by David Vo. ([\#9390](https://github.com/matrix-org/synapse/issues/9390))
- Update docs for using Gitea as OpenID provider. ([\#9404](https://github.com/matrix-org/synapse/issues/9404))
- Document that pusher instances are shardable. ([\#9407](https://github.com/matrix-org/synapse/issues/9407))
- Fix erroneous documentation from v1.27.0 about updating the SAML2 callback URL. ([\#9434](https://github.com/matrix-org/synapse/issues/9434))


Deprecations and Removals
-------------------------

- Deprecate old admin API `GET /_synapse/admin/v1/users/<user_id>`. ([\#9429](https://github.com/matrix-org/synapse/issues/9429))


Internal Changes
----------------

- Fix 'object name reserved for internal use' errors with recent versions of SQLite. ([\#9003](https://github.com/matrix-org/synapse/issues/9003))
- Add experimental support for running Synapse with PyPy. ([\#9123](https://github.com/matrix-org/synapse/issues/9123))
- Deny access to additional IP addresses by default. ([\#9240](https://github.com/matrix-org/synapse/issues/9240))
- Update the `Cursor` type hints to better match PEP 249. ([\#9299](https://github.com/matrix-org/synapse/issues/9299))
- Add debug logging for SRV lookups. Contributed by @Bubu. ([\#9305](https://github.com/matrix-org/synapse/issues/9305))
- Improve logging for OIDC login flow. ([\#9307](https://github.com/matrix-org/synapse/issues/9307))
- Share the code for handling required attributes between the CAS and SAML handlers. ([\#9326](https://github.com/matrix-org/synapse/issues/9326))
- Clean up the code to load the metadata for OpenID Connect identity providers. ([\#9362](https://github.com/matrix-org/synapse/issues/9362))
- Convert tests to use `HomeserverTestCase`. ([\#9377](https://github.com/matrix-org/synapse/issues/9377), [\#9396](https://github.com/matrix-org/synapse/issues/9396))
- Update the version of black used to 20.8b1. ([\#9381](https://github.com/matrix-org/synapse/issues/9381))
- Allow OIDC config to override discovered values. ([\#9384](https://github.com/matrix-org/synapse/issues/9384))
- Remove some dead code from the acceptance of room invites path. ([\#9394](https://github.com/matrix-org/synapse/issues/9394))
- Clean up an unused method in the presence handler code. ([\#9408](https://github.com/matrix-org/synapse/issues/9408))


Synapse 1.27.0 (2021-02-16)
===========================

Note that this release includes a change in Synapse to use Redis as a cache ─ as well as a pub/sub mechanism ─ if Redis support is enabled for workers. No action is needed by server administrators, and we do not expect resource usage of the Redis instance to change dramatically.

This release also changes the callback URI for OpenID Connect (OIDC) and SAML2 identity providers. If your server is configured to use single sign-on via an OIDC/OAuth2 or SAML2 IdP, you may need to make configuration changes. Please review the [upgrade notes](docs/upgrade.md) for more details on these changes.

This release also changes escaping of variables in the HTML templates for SSO or email notifications. If you have customised these templates, please review the [upgrade notes](docs/upgrade.md) for more details on these changes.


Bugfixes
--------

- Fix building Docker images for armv7. ([\#9405](https://github.com/matrix-org/synapse/issues/9405))


Synapse 1.27.0rc2 (2021-02-11)
==============================

Features
--------

- Further improvements to the user experience of registration via single sign-on. ([\#9297](https://github.com/matrix-org/synapse/issues/9297))


Bugfixes
--------

- Fix ratelimiting introduced in v1.27.0rc1 for invites to respect the `ratelimit` flag on application services. ([\#9302](https://github.com/matrix-org/synapse/issues/9302))
- Do not automatically calculate `public_baseurl` since it can be wrong in some situations. Reverts behaviour introduced in v1.26.0. ([\#9313](https://github.com/matrix-org/synapse/issues/9313))


Improved Documentation
----------------------

- Clarify the sample configuration for changes made to the template loading code. ([\#9310](https://github.com/matrix-org/synapse/issues/9310))


Synapse 1.27.0rc1 (2021-02-02)
==============================

Features
--------

- Add an admin API for getting and deleting forward extremities for a room. ([\#9062](https://github.com/matrix-org/synapse/issues/9062))
- Add an admin API for retrieving the current room state of a room. ([\#9168](https://github.com/matrix-org/synapse/issues/9168))
- Add experimental support for allowing clients to pick an SSO Identity Provider ([MSC2858](https://github.com/matrix-org/matrix-doc/pull/2858)). ([\#9183](https://github.com/matrix-org/synapse/issues/9183), [\#9242](https://github.com/matrix-org/synapse/issues/9242))
- Add an admin API endpoint for shadow-banning users. ([\#9209](https://github.com/matrix-org/synapse/issues/9209))
- Add ratelimits to the 3PID `/requestToken` APIs. ([\#9238](https://github.com/matrix-org/synapse/issues/9238))
- Add support to the OpenID Connect integration for adding the user's email address. ([\#9245](https://github.com/matrix-org/synapse/issues/9245))
- Add ratelimits to invites in rooms and to specific users. ([\#9258](https://github.com/matrix-org/synapse/issues/9258))
- Improve the user experience of setting up an account via single-sign on. ([\#9262](https://github.com/matrix-org/synapse/issues/9262), [\#9272](https://github.com/matrix-org/synapse/issues/9272), [\#9275](https://github.com/matrix-org/synapse/issues/9275), [\#9276](https://github.com/matrix-org/synapse/issues/9276), [\#9277](https://github.com/matrix-org/synapse/issues/9277), [\#9286](https://github.com/matrix-org/synapse/issues/9286), [\#9287](https://github.com/matrix-org/synapse/issues/9287))
- Add phone home stats for encrypted messages. ([\#9283](https://github.com/matrix-org/synapse/issues/9283))
- Update the redirect URI for OIDC authentication. ([\#9288](https://github.com/matrix-org/synapse/issues/9288))


Bugfixes
--------

- Fix spurious errors in logs when deleting a non-existant pusher. ([\#9121](https://github.com/matrix-org/synapse/issues/9121))
- Fix a long-standing bug where Synapse would return a 500 error when a thumbnail did not exist (and auto-generation of thumbnails was not enabled). ([\#9163](https://github.com/matrix-org/synapse/issues/9163))
- Fix a long-standing bug where an internal server error was raised when attempting to preview an HTML document in an unknown character encoding. ([\#9164](https://github.com/matrix-org/synapse/issues/9164))
- Fix a long-standing bug where invalid data could cause errors when calculating the presentable room name for push. ([\#9165](https://github.com/matrix-org/synapse/issues/9165))
- Fix bug where we sometimes didn't detect that Redis connections had died, causing workers to not see new data. ([\#9218](https://github.com/matrix-org/synapse/issues/9218))
- Fix a bug where `None` was passed to Synapse modules instead of an empty dictionary if an empty module `config` block was provided in the homeserver config. ([\#9229](https://github.com/matrix-org/synapse/issues/9229))
- Fix a bug in the `make_room_admin` admin API where it failed if the admin with the greatest power level was not in the room. Contributed by Pankaj Yadav. ([\#9235](https://github.com/matrix-org/synapse/issues/9235))
- Prevent password hashes from getting dropped if a client failed threepid validation during a User Interactive Auth stage. Removes a workaround for an ancient bug in Riot Web <v0.7.4. ([\#9265](https://github.com/matrix-org/synapse/issues/9265))
- Fix single-sign-on when the endpoints are routed to synapse workers. ([\#9271](https://github.com/matrix-org/synapse/issues/9271))


Improved Documentation
----------------------

- Add docs for using Gitea as OpenID provider. ([\#9134](https://github.com/matrix-org/synapse/issues/9134))
- Add link to Matrix VoIP tester for turn-howto. ([\#9135](https://github.com/matrix-org/synapse/issues/9135))
- Add notes on integrating with Facebook for SSO login. ([\#9244](https://github.com/matrix-org/synapse/issues/9244))


Deprecations and Removals
-------------------------

- The `service_url` parameter in `cas_config` is deprecated in favor of `public_baseurl`. ([\#9199](https://github.com/matrix-org/synapse/issues/9199))
- Add new endpoint `/_synapse/client/saml2` for SAML2 authentication callbacks, and deprecate the old endpoint `/_matrix/saml2`. ([\#9289](https://github.com/matrix-org/synapse/issues/9289))


Internal Changes
----------------

- Add tests to `test_user.UsersListTestCase` for List Users Admin API. ([\#9045](https://github.com/matrix-org/synapse/issues/9045))
- Various improvements to the federation client. ([\#9129](https://github.com/matrix-org/synapse/issues/9129))
- Speed up chain cover calculation when persisting a batch of state events at once. ([\#9176](https://github.com/matrix-org/synapse/issues/9176))
- Add a `long_description_type` to the package metadata. ([\#9180](https://github.com/matrix-org/synapse/issues/9180))
- Speed up batch insertion when using PostgreSQL. ([\#9181](https://github.com/matrix-org/synapse/issues/9181), [\#9188](https://github.com/matrix-org/synapse/issues/9188))
- Emit an error at startup if different Identity Providers are configured with the same `idp_id`. ([\#9184](https://github.com/matrix-org/synapse/issues/9184))
- Improve performance of concurrent use of `StreamIDGenerators`. ([\#9190](https://github.com/matrix-org/synapse/issues/9190))
- Add some missing source directories to the automatic linting script. ([\#9191](https://github.com/matrix-org/synapse/issues/9191))
- Precompute joined hosts and store in Redis. ([\#9198](https://github.com/matrix-org/synapse/issues/9198), [\#9227](https://github.com/matrix-org/synapse/issues/9227))
- Clean-up template loading code. ([\#9200](https://github.com/matrix-org/synapse/issues/9200))
- Fix the Python 3.5 old dependencies build. ([\#9217](https://github.com/matrix-org/synapse/issues/9217))
- Update `isort` to v5.7.0 to bypass a bug where it would disagree with `black` about formatting. ([\#9222](https://github.com/matrix-org/synapse/issues/9222))
- Add type hints to handlers code. ([\#9223](https://github.com/matrix-org/synapse/issues/9223), [\#9232](https://github.com/matrix-org/synapse/issues/9232))
- Fix Debian package building on Ubuntu 16.04 LTS (Xenial). ([\#9254](https://github.com/matrix-org/synapse/issues/9254))
- Minor performance improvement during TLS handshake. ([\#9255](https://github.com/matrix-org/synapse/issues/9255))
- Refactor the generation of summary text for email notifications. ([\#9260](https://github.com/matrix-org/synapse/issues/9260))
- Restore PyPy compatibility by not calling CPython-specific GC methods when under PyPy. ([\#9270](https://github.com/matrix-org/synapse/issues/9270))


Synapse 1.26.0 (2021-01-27)
===========================

This release brings a new schema version for Synapse and rolling back to a previous
version is not trivial. Please review the [upgrade notes](docs/upgrade.md) for more details
on these changes and for general upgrade guidance.

No significant changes since 1.26.0rc2.


Synapse 1.26.0rc2 (2021-01-25)
==============================

Bugfixes
--------

- Fix receipts and account data not being sent down sync. Introduced in v1.26.0rc1. ([\#9193](https://github.com/matrix-org/synapse/issues/9193), [\#9195](https://github.com/matrix-org/synapse/issues/9195))
- Fix chain cover update to handle events with duplicate auth events. Introduced in v1.26.0rc1. ([\#9210](https://github.com/matrix-org/synapse/issues/9210))


Internal Changes
----------------

- Add an `oidc-` prefix to any `idp_id`s which are given in the `oidc_providers` configuration. ([\#9189](https://github.com/matrix-org/synapse/issues/9189))
- Bump minimum `psycopg2` version to v2.8. ([\#9204](https://github.com/matrix-org/synapse/issues/9204))


Synapse 1.26.0rc1 (2021-01-20)
==============================

This release brings a new schema version for Synapse and rolling back to a previous
version is not trivial. Please review the [upgrade notes](docs/upgrade.md) for more details
on these changes and for general upgrade guidance.

Features
--------

- Add support for multiple SSO Identity Providers. ([\#9015](https://github.com/matrix-org/synapse/issues/9015), [\#9017](https://github.com/matrix-org/synapse/issues/9017), [\#9036](https://github.com/matrix-org/synapse/issues/9036), [\#9067](https://github.com/matrix-org/synapse/issues/9067), [\#9081](https://github.com/matrix-org/synapse/issues/9081), [\#9082](https://github.com/matrix-org/synapse/issues/9082), [\#9105](https://github.com/matrix-org/synapse/issues/9105), [\#9107](https://github.com/matrix-org/synapse/issues/9107), [\#9109](https://github.com/matrix-org/synapse/issues/9109), [\#9110](https://github.com/matrix-org/synapse/issues/9110), [\#9127](https://github.com/matrix-org/synapse/issues/9127), [\#9153](https://github.com/matrix-org/synapse/issues/9153), [\#9154](https://github.com/matrix-org/synapse/issues/9154), [\#9177](https://github.com/matrix-org/synapse/issues/9177))
- During user-interactive authentication via single-sign-on, give a better error if the user uses the wrong account on the SSO IdP. ([\#9091](https://github.com/matrix-org/synapse/issues/9091))
- Give the `public_baseurl` a default value, if it is not explicitly set in the configuration file. ([\#9159](https://github.com/matrix-org/synapse/issues/9159))
- Improve performance when calculating ignored users in large rooms. ([\#9024](https://github.com/matrix-org/synapse/issues/9024))
- Implement [MSC2176](https://github.com/matrix-org/matrix-doc/pull/2176) in an experimental room version. ([\#8984](https://github.com/matrix-org/synapse/issues/8984))
- Add an admin API for protecting local media from quarantine. ([\#9086](https://github.com/matrix-org/synapse/issues/9086))
- Remove a user's avatar URL and display name when deactivated with the Admin API. ([\#8932](https://github.com/matrix-org/synapse/issues/8932))
- Update `/_synapse/admin/v1/users/<user_id>/joined_rooms` to work for both local and remote users. ([\#8948](https://github.com/matrix-org/synapse/issues/8948))
- Add experimental support for handling to-device messages on worker processes. ([\#9042](https://github.com/matrix-org/synapse/issues/9042), [\#9043](https://github.com/matrix-org/synapse/issues/9043), [\#9044](https://github.com/matrix-org/synapse/issues/9044), [\#9130](https://github.com/matrix-org/synapse/issues/9130))
- Add experimental support for handling `/keys/claim` and `/room_keys` APIs on worker processes. ([\#9068](https://github.com/matrix-org/synapse/issues/9068))
- Add experimental support for handling `/devices` API on worker processes. ([\#9092](https://github.com/matrix-org/synapse/issues/9092))
- Add experimental support for moving off receipts and account data persistence off master. ([\#9104](https://github.com/matrix-org/synapse/issues/9104), [\#9166](https://github.com/matrix-org/synapse/issues/9166))


Bugfixes
--------

- Fix a long-standing issue where an internal server error would occur when requesting a profile over federation that did not include a display name / avatar URL. ([\#9023](https://github.com/matrix-org/synapse/issues/9023))
- Fix a long-standing bug where some caches could grow larger than configured. ([\#9028](https://github.com/matrix-org/synapse/issues/9028))
- Fix error handling during insertion of client IPs into the database. ([\#9051](https://github.com/matrix-org/synapse/issues/9051))
- Fix bug where we didn't correctly record CPU time spent in `on_new_event` block. ([\#9053](https://github.com/matrix-org/synapse/issues/9053))
- Fix a minor bug which could cause confusing error messages from invalid configurations. ([\#9054](https://github.com/matrix-org/synapse/issues/9054))
- Fix incorrect exit code when there is an error at startup. ([\#9059](https://github.com/matrix-org/synapse/issues/9059))
- Fix `JSONDecodeError` spamming the logs when sending transactions to remote servers. ([\#9070](https://github.com/matrix-org/synapse/issues/9070))
- Fix "Failed to send request" errors when a client provides an invalid room alias. ([\#9071](https://github.com/matrix-org/synapse/issues/9071))
- Fix bugs in federation catchup logic that caused outbound federation to be delayed for large servers after start up. Introduced in v1.8.0 and v1.21.0. ([\#9114](https://github.com/matrix-org/synapse/issues/9114), [\#9116](https://github.com/matrix-org/synapse/issues/9116))
- Fix corruption of `pushers` data when a postgres bouncer is used. ([\#9117](https://github.com/matrix-org/synapse/issues/9117))
- Fix minor bugs in handling the `clientRedirectUrl` parameter for SSO login. ([\#9128](https://github.com/matrix-org/synapse/issues/9128))
- Fix "Unhandled error in Deferred: BodyExceededMaxSize" errors when .well-known files that are too large. ([\#9108](https://github.com/matrix-org/synapse/issues/9108))
- Fix "UnboundLocalError: local variable 'length' referenced before assignment" errors when the response body exceeds the expected size. This bug was introduced in v1.25.0. ([\#9145](https://github.com/matrix-org/synapse/issues/9145))
- Fix a long-standing bug "ValueError: invalid literal for int() with base 10" when `/publicRooms` is requested with an invalid `server` parameter. ([\#9161](https://github.com/matrix-org/synapse/issues/9161))


Improved Documentation
----------------------

- Add some extra docs for getting Synapse running on macOS. ([\#8997](https://github.com/matrix-org/synapse/issues/8997))
- Correct a typo in the `systemd-with-workers` documentation. ([\#9035](https://github.com/matrix-org/synapse/issues/9035))
- Correct a typo in `INSTALL.md`. ([\#9040](https://github.com/matrix-org/synapse/issues/9040))
- Add missing `user_mapping_provider` configuration to the Keycloak OIDC example. Contributed by @chris-ruecker. ([\#9057](https://github.com/matrix-org/synapse/issues/9057))
- Quote `pip install` packages when extras are used to avoid shells interpreting bracket characters. ([\#9151](https://github.com/matrix-org/synapse/issues/9151))


Deprecations and Removals
-------------------------

- Remove broken and unmaintained `demo/webserver.py` script. ([\#9039](https://github.com/matrix-org/synapse/issues/9039))


Internal Changes
----------------

- Improve efficiency of large state resolutions. ([\#8868](https://github.com/matrix-org/synapse/issues/8868), [\#9029](https://github.com/matrix-org/synapse/issues/9029), [\#9115](https://github.com/matrix-org/synapse/issues/9115), [\#9118](https://github.com/matrix-org/synapse/issues/9118), [\#9124](https://github.com/matrix-org/synapse/issues/9124))
- Various clean-ups to the structured logging and logging context code. ([\#8939](https://github.com/matrix-org/synapse/issues/8939))
- Ensure rejected events get added to some metadata tables. ([\#9016](https://github.com/matrix-org/synapse/issues/9016))
- Ignore date-rotated homeserver logs saved to disk. ([\#9018](https://github.com/matrix-org/synapse/issues/9018))
- Remove an unused column from `access_tokens` table. ([\#9025](https://github.com/matrix-org/synapse/issues/9025))
- Add a `-noextras` factor to `tox.ini`, to support running the tests with no optional dependencies. ([\#9030](https://github.com/matrix-org/synapse/issues/9030))
- Fix running unit tests when optional dependencies are not installed. ([\#9031](https://github.com/matrix-org/synapse/issues/9031))
- Allow bumping schema version when using split out state database. ([\#9033](https://github.com/matrix-org/synapse/issues/9033))
- Configure the linters to run on a consistent set of files. ([\#9038](https://github.com/matrix-org/synapse/issues/9038))
- Various cleanups to device inbox store. ([\#9041](https://github.com/matrix-org/synapse/issues/9041))
- Drop unused database tables. ([\#9055](https://github.com/matrix-org/synapse/issues/9055))
- Remove unused `SynapseService` class. ([\#9058](https://github.com/matrix-org/synapse/issues/9058))
- Remove unnecessary declarations in the tests for the admin API. ([\#9063](https://github.com/matrix-org/synapse/issues/9063))
- Remove `SynapseRequest.get_user_agent`. ([\#9069](https://github.com/matrix-org/synapse/issues/9069))
- Remove redundant `Homeserver.get_ip_from_request` method. ([\#9080](https://github.com/matrix-org/synapse/issues/9080))
- Add type hints to media repository. ([\#9093](https://github.com/matrix-org/synapse/issues/9093))
- Fix the wrong arguments being passed to `BlacklistingAgentWrapper` from `MatrixFederationAgent`. Contributed by Timothy Leung. ([\#9098](https://github.com/matrix-org/synapse/issues/9098))
- Reduce the scope of caught exceptions in `BlacklistingAgentWrapper`. ([\#9106](https://github.com/matrix-org/synapse/issues/9106))
- Improve `UsernamePickerTestCase`. ([\#9112](https://github.com/matrix-org/synapse/issues/9112))
- Remove dependency on `distutils`. ([\#9125](https://github.com/matrix-org/synapse/issues/9125))
- Enforce that replication HTTP clients are called with keyword arguments only. ([\#9144](https://github.com/matrix-org/synapse/issues/9144))
- Fix the Python 3.5 / old dependencies build in CI. ([\#9146](https://github.com/matrix-org/synapse/issues/9146))
- Replace the old `perspectives` option in the Synapse docker config file template with `trusted_key_servers`. ([\#9157](https://github.com/matrix-org/synapse/issues/9157))


Synapse 1.25.0 (2021-01-13)
===========================

Ending Support for Python 3.5 and Postgres 9.5
----------------------------------------------

With this release, the Synapse team is announcing a formal deprecation policy for our platform dependencies, like Python and PostgreSQL:

All future releases of Synapse will follow the upstream end-of-life schedules.

Which means:

* This is the last release which guarantees support for Python 3.5.
* We will end support for PostgreSQL 9.5 early next month.
* We will end support for Python 3.6 and PostgreSQL 9.6 near the end of the year.

Crucially, this means __we will not produce .deb packages for Debian 9 (Stretch) or Ubuntu 16.04 (Xenial)__ beyond the transition period described below.

The website https://endoflife.date/ has convenient summaries of the support schedules for projects like [Python](https://endoflife.date/python) and [PostgreSQL](https://endoflife.date/postgresql).

If you are unable to upgrade your environment to a supported version of Python or
Postgres, we encourage you to consider using the
[Synapse Docker images](https://matrix-org.github.io/synapse/latest/setup/installation.html#docker-images-and-ansible-playbooks)
instead.

### Transition Period

We will make a good faith attempt to avoid breaking compatibility in all releases through the end of March 2021. However, critical security vulnerabilities in dependencies or other unanticipated circumstances may arise which necessitate breaking compatibility earlier.

We intend to continue producing .deb packages for Debian 9 (Stretch) and Ubuntu 16.04 (Xenial) through the transition period.

Removal warning
---------------

The old [Purge Room API](https://github.com/matrix-org/synapse/tree/master/docs/admin_api/purge_room.md)
and [Shutdown Room API](https://github.com/matrix-org/synapse/tree/master/docs/admin_api/shutdown_room.md)
are deprecated and will be removed in a future release. They will be replaced by the
[Delete Room API](https://github.com/matrix-org/synapse/tree/master/docs/admin_api/rooms.md#delete-room-api).

`POST /_synapse/admin/v1/rooms/<room_id>/delete` replaces `POST /_synapse/admin/v1/purge_room` and
`POST /_synapse/admin/v1/shutdown_room/<room_id>`.

Bugfixes
--------

- Fix HTTP proxy support when using a proxy that is on a blacklisted IP. Introduced in v1.25.0rc1. Contributed by @Bubu. ([\#9084](https://github.com/matrix-org/synapse/issues/9084))


Synapse 1.25.0rc1 (2021-01-06)
==============================

Features
--------

- Add an admin API that lets server admins get power in rooms in which local users have power. ([\#8756](https://github.com/matrix-org/synapse/issues/8756))
- Add optional HTTP authentication to replication endpoints. ([\#8853](https://github.com/matrix-org/synapse/issues/8853))
- Improve the error messages printed as a result of configuration problems for extension modules. ([\#8874](https://github.com/matrix-org/synapse/issues/8874))
- Add the number of local devices to Room Details Admin API. Contributed by @dklimpel. ([\#8886](https://github.com/matrix-org/synapse/issues/8886))
- Add `X-Robots-Tag` header to stop web crawlers from indexing media. Contributed by Aaron Raimist. ([\#8887](https://github.com/matrix-org/synapse/issues/8887))
- Spam-checkers may now define their methods as `async`. ([\#8890](https://github.com/matrix-org/synapse/issues/8890))
- Add support for allowing users to pick their own user ID during a single-sign-on login. ([\#8897](https://github.com/matrix-org/synapse/issues/8897), [\#8900](https://github.com/matrix-org/synapse/issues/8900), [\#8911](https://github.com/matrix-org/synapse/issues/8911), [\#8938](https://github.com/matrix-org/synapse/issues/8938), [\#8941](https://github.com/matrix-org/synapse/issues/8941), [\#8942](https://github.com/matrix-org/synapse/issues/8942), [\#8951](https://github.com/matrix-org/synapse/issues/8951))
- Add an `email.invite_client_location` configuration option to send a web client location to the invite endpoint on the identity server which allows customisation of the email template. ([\#8930](https://github.com/matrix-org/synapse/issues/8930))
- The search term in the list room and list user Admin APIs is now treated as case-insensitive. ([\#8931](https://github.com/matrix-org/synapse/issues/8931))
- Apply an IP range blacklist to push and key revocation requests. ([\#8821](https://github.com/matrix-org/synapse/issues/8821), [\#8870](https://github.com/matrix-org/synapse/issues/8870), [\#8954](https://github.com/matrix-org/synapse/issues/8954))
- Add an option to allow re-use of user-interactive authentication sessions for a period of time. ([\#8970](https://github.com/matrix-org/synapse/issues/8970))
- Allow running the redact endpoint on workers. ([\#8994](https://github.com/matrix-org/synapse/issues/8994))


Bugfixes
--------

- Fix bug where we might not correctly calculate the current state for rooms with multiple extremities. ([\#8827](https://github.com/matrix-org/synapse/issues/8827))
- Fix a long-standing bug in the register admin endpoint (`/_synapse/admin/v1/register`) when the `mac` field was not provided. The endpoint now properly returns a 400 error. Contributed by @edwargix. ([\#8837](https://github.com/matrix-org/synapse/issues/8837))
- Fix a long-standing bug on Synapse instances supporting Single-Sign-On, where users would be prompted to enter their password to confirm certain actions, even though they have not set a password. ([\#8858](https://github.com/matrix-org/synapse/issues/8858))
- Fix a longstanding bug where a 500 error would be returned if the `Content-Length` header was not provided to the upload media resource. ([\#8862](https://github.com/matrix-org/synapse/issues/8862))
- Add additional validation to pusher URLs to be compliant with the specification. ([\#8865](https://github.com/matrix-org/synapse/issues/8865))
- Fix the error code that is returned when a user tries to register on a homeserver on which new-user registration has been disabled. ([\#8867](https://github.com/matrix-org/synapse/issues/8867))
- Fix a bug where `PUT /_synapse/admin/v2/users/<user_id>` failed to create a new user when `avatar_url` is specified. Bug introduced in Synapse v1.9.0. ([\#8872](https://github.com/matrix-org/synapse/issues/8872))
- Fix a 500 error when attempting to preview an empty HTML file. ([\#8883](https://github.com/matrix-org/synapse/issues/8883))
- Fix occasional deadlock when handling SIGHUP. ([\#8918](https://github.com/matrix-org/synapse/issues/8918))
- Fix login API to not ratelimit application services that have ratelimiting disabled. ([\#8920](https://github.com/matrix-org/synapse/issues/8920))
- Fix bug where we ratelimited auto joining of rooms on registration (using `auto_join_rooms` config). ([\#8921](https://github.com/matrix-org/synapse/issues/8921))
- Fix a bug where deactivated users appeared in the user directory when their profile information was updated. ([\#8933](https://github.com/matrix-org/synapse/issues/8933), [\#8964](https://github.com/matrix-org/synapse/issues/8964))
- Fix bug introduced in Synapse v1.24.0 which would cause an exception on startup if both `enabled` and `localdb_enabled` were set to `False` in the `password_config` setting of the configuration file. ([\#8937](https://github.com/matrix-org/synapse/issues/8937))
- Fix a bug where 500 errors would be returned if the `m.room_history_visibility` event had invalid content. ([\#8945](https://github.com/matrix-org/synapse/issues/8945))
- Fix a bug causing common English words to not be considered for a user directory search. ([\#8959](https://github.com/matrix-org/synapse/issues/8959))
- Fix bug where application services couldn't register new ghost users if the server had reached its MAU limit. ([\#8962](https://github.com/matrix-org/synapse/issues/8962))
- Fix a long-standing bug where a `m.image` event without a `url` would cause errors on push. ([\#8965](https://github.com/matrix-org/synapse/issues/8965))
- Fix a small bug in v2 state resolution algorithm, which could also cause performance issues for rooms with large numbers of power levels. ([\#8971](https://github.com/matrix-org/synapse/issues/8971))
- Add validation to the `sendToDevice` API to raise a missing parameters error instead of a 500 error. ([\#8975](https://github.com/matrix-org/synapse/issues/8975))
- Add validation of group IDs to raise a 400 error instead of a 500 eror. ([\#8977](https://github.com/matrix-org/synapse/issues/8977))


Improved Documentation
----------------------

- Fix the "Event persist rate" section of the included grafana dashboard by adding missing prometheus rules. ([\#8802](https://github.com/matrix-org/synapse/issues/8802))
- Combine related media admin API docs. ([\#8839](https://github.com/matrix-org/synapse/issues/8839))
- Fix an error in the documentation for the SAML username mapping provider. ([\#8873](https://github.com/matrix-org/synapse/issues/8873))
- Clarify comments around template directories in `sample_config.yaml`. ([\#8891](https://github.com/matrix-org/synapse/issues/8891))
- Move instructions for database setup, adjusted heading levels and improved syntax highlighting in [INSTALL.md](../INSTALL.md). Contributed by @fossterer. ([\#8987](https://github.com/matrix-org/synapse/issues/8987))
- Update the example value of `group_creation_prefix` in the sample configuration. ([\#8992](https://github.com/matrix-org/synapse/issues/8992))
- Link the Synapse developer room to the development section in the docs. ([\#9002](https://github.com/matrix-org/synapse/issues/9002))


Deprecations and Removals
-------------------------

- Deprecate Shutdown Room and Purge Room Admin APIs. ([\#8829](https://github.com/matrix-org/synapse/issues/8829))


Internal Changes
----------------

- Properly store the mapping of external ID to Matrix ID for CAS users. ([\#8856](https://github.com/matrix-org/synapse/issues/8856), [\#8958](https://github.com/matrix-org/synapse/issues/8958))
- Remove some unnecessary stubbing from unit tests. ([\#8861](https://github.com/matrix-org/synapse/issues/8861))
- Remove unused `FakeResponse` class from unit tests. ([\#8864](https://github.com/matrix-org/synapse/issues/8864))
- Pass `room_id` to `get_auth_chain_difference`. ([\#8879](https://github.com/matrix-org/synapse/issues/8879))
- Add type hints to push module. ([\#8880](https://github.com/matrix-org/synapse/issues/8880), [\#8882](https://github.com/matrix-org/synapse/issues/8882), [\#8901](https://github.com/matrix-org/synapse/issues/8901), [\#8940](https://github.com/matrix-org/synapse/issues/8940), [\#8943](https://github.com/matrix-org/synapse/issues/8943), [\#9020](https://github.com/matrix-org/synapse/issues/9020))
- Simplify logic for handling user-interactive-auth via single-sign-on servers. ([\#8881](https://github.com/matrix-org/synapse/issues/8881))
- Skip the SAML tests if the requirements (`pysaml2` and `xmlsec1`) aren't available. ([\#8905](https://github.com/matrix-org/synapse/issues/8905))
- Fix multiarch docker image builds. ([\#8906](https://github.com/matrix-org/synapse/issues/8906))
- Don't publish `latest` docker image until all archs are built. ([\#8909](https://github.com/matrix-org/synapse/issues/8909))
- Various clean-ups to the structured logging and logging context code. ([\#8916](https://github.com/matrix-org/synapse/issues/8916), [\#8935](https://github.com/matrix-org/synapse/issues/8935))
- Automatically drop stale forward-extremities under some specific conditions. ([\#8929](https://github.com/matrix-org/synapse/issues/8929))
- Refactor test utilities for injecting HTTP requests. ([\#8946](https://github.com/matrix-org/synapse/issues/8946))
- Add a maximum size of 50 kilobytes to .well-known lookups. ([\#8950](https://github.com/matrix-org/synapse/issues/8950))
- Fix bug in `generate_log_config` script which made it write empty files. ([\#8952](https://github.com/matrix-org/synapse/issues/8952))
- Clean up tox.ini file; disable coverage checking for non-test runs. ([\#8963](https://github.com/matrix-org/synapse/issues/8963))
- Add type hints to the admin and room list handlers. ([\#8973](https://github.com/matrix-org/synapse/issues/8973))
- Add type hints to the receipts and user directory handlers. ([\#8976](https://github.com/matrix-org/synapse/issues/8976))
- Drop the unused `local_invites` table. ([\#8979](https://github.com/matrix-org/synapse/issues/8979))
- Add type hints to the base storage code. ([\#8980](https://github.com/matrix-org/synapse/issues/8980))
- Support using PyJWT v2.0.0 in the test suite. ([\#8986](https://github.com/matrix-org/synapse/issues/8986))
- Fix `tests.federation.transport.RoomDirectoryFederationTests` and ensure it runs in CI. ([\#8998](https://github.com/matrix-org/synapse/issues/8998))
- Add type hints to the crypto module. ([\#8999](https://github.com/matrix-org/synapse/issues/8999))


Synapse 1.24.0 (2020-12-09)
===========================

Due to the two security issues highlighted below, server administrators are
encouraged to update Synapse. We are not aware of these vulnerabilities being
exploited in the wild.

Security advisory
-----------------

The following issues are fixed in v1.23.1 and v1.24.0.

- There is a denial of service attack
  ([CVE-2020-26257](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2020-26257))
  against the federation APIs in which future events will not be correctly sent
  to other servers over federation. This affects all servers that participate in
  open federation. (Fixed in [#8776](https://github.com/matrix-org/synapse/pull/8776)).

- Synapse may be affected by OpenSSL
  [CVE-2020-1971](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2020-1971).
  Synapse administrators should ensure that they have the latest versions of
  the cryptography Python package installed.

To upgrade Synapse along with the cryptography package:

* Administrators using the [`matrix.org` Docker
  image](https://hub.docker.com/r/matrixdotorg/synapse/) or the [Debian/Ubuntu
  packages from
  `matrix.org`](https://matrix-org.github.io/synapse/latest/setup/installation.html#matrixorg-packages)
  should ensure that they have version 1.24.0 or 1.23.1 installed: these images include
  the updated packages.
* Administrators who have [installed Synapse from
  source](https://matrix-org.github.io/synapse/latest/setup/installation.html#installing-from-source)
  should upgrade the cryptography package within their virtualenv by running:
  ```sh
  <path_to_virtualenv>/bin/pip install 'cryptography>=3.3'
  ```
* Administrators who have installed Synapse from distribution packages should
  consult the information from their distributions.

Internal Changes
----------------

- Add a maximum version for pysaml2 on Python 3.5. ([\#8898](https://github.com/matrix-org/synapse/issues/8898))


Synapse 1.23.1 (2020-12-09)
===========================

Due to the two security issues highlighted below, server administrators are
encouraged to update Synapse. We are not aware of these vulnerabilities being
exploited in the wild.

Security advisory
-----------------

The following issues are fixed in v1.23.1 and v1.24.0.

- There is a denial of service attack
  ([CVE-2020-26257](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2020-26257))
  against the federation APIs in which future events will not be correctly sent
  to other servers over federation. This affects all servers that participate in
  open federation. (Fixed in [#8776](https://github.com/matrix-org/synapse/pull/8776)).

- Synapse may be affected by OpenSSL
  [CVE-2020-1971](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2020-1971).
  Synapse administrators should ensure that they have the latest versions of
  the cryptography Python package installed.

To upgrade Synapse along with the cryptography package:

* Administrators using the [`matrix.org` Docker
  image](https://hub.docker.com/r/matrixdotorg/synapse/) or the [Debian/Ubuntu
  packages from
  `matrix.org`](https://matrix-org.github.io/synapse/latest/setup/installation.html#matrixorg-packages)
  should ensure that they have version 1.24.0 or 1.23.1 installed: these images include
  the updated packages.
* Administrators who have [installed Synapse from
  source](https://matrix-org.github.io/synapse/latest/setup/installation.html#installing-from-source)
  should upgrade the cryptography package within their virtualenv by running:
  ```sh
  <path_to_virtualenv>/bin/pip install 'cryptography>=3.3'
  ```
* Administrators who have installed Synapse from distribution packages should
  consult the information from their distributions.

Bugfixes
--------

- Fix a bug in some federation APIs which could lead to unexpected behaviour if different parameters were set in the URI and the request body. ([\#8776](https://github.com/matrix-org/synapse/issues/8776))


Internal Changes
----------------

- Add a maximum version for pysaml2 on Python 3.5. ([\#8898](https://github.com/matrix-org/synapse/issues/8898))


Synapse 1.24.0rc2 (2020-12-04)
==============================

Bugfixes
--------

- Fix a regression in v1.24.0rc1 which failed to allow SAML mapping providers which were unable to redirect users to an additional page. ([\#8878](https://github.com/matrix-org/synapse/issues/8878))


Internal Changes
----------------

- Add support for the `prometheus_client` newer than 0.9.0. Contributed by Jordan Bancino. ([\#8875](https://github.com/matrix-org/synapse/issues/8875))


Synapse 1.24.0rc1 (2020-12-02)
==============================

Features
--------

- Add admin API for logging in as a user. ([\#8617](https://github.com/matrix-org/synapse/issues/8617))
- Allow specification of the SAML IdP if the metadata returns multiple IdPs. ([\#8630](https://github.com/matrix-org/synapse/issues/8630))
- Add support for re-trying generation of a localpart for OpenID Connect mapping providers. ([\#8801](https://github.com/matrix-org/synapse/issues/8801), [\#8855](https://github.com/matrix-org/synapse/issues/8855))
- Allow the `Date` header through CORS. Contributed by Nicolas Chamo. ([\#8804](https://github.com/matrix-org/synapse/issues/8804))
- Add a config option, `push.group_by_unread_count`, which controls whether unread message counts in push notifications are defined as "the number of rooms with unread messages" or "total unread messages". ([\#8820](https://github.com/matrix-org/synapse/issues/8820))
- Add `force_purge` option to delete-room admin api. ([\#8843](https://github.com/matrix-org/synapse/issues/8843))


Bugfixes
--------

- Fix a bug where appservices may be sent an excessive amount of read receipts and presence. Broke in v1.22.0. ([\#8744](https://github.com/matrix-org/synapse/issues/8744))
- Fix a bug in some federation APIs which could lead to unexpected behaviour if different parameters were set in the URI and the request body. ([\#8776](https://github.com/matrix-org/synapse/issues/8776))
- Fix a bug where synctl could spawn duplicate copies of a worker. Contributed by Waylon Cude. ([\#8798](https://github.com/matrix-org/synapse/issues/8798))
- Allow per-room profiles to be used for the server notice user. ([\#8799](https://github.com/matrix-org/synapse/issues/8799))
- Fix a bug where logging could break after a call to SIGHUP. ([\#8817](https://github.com/matrix-org/synapse/issues/8817))
- Fix `register_new_matrix_user` failing with "Bad Request" when trailing slash is included in server URL. Contributed by @angdraug. ([\#8823](https://github.com/matrix-org/synapse/issues/8823))
- Fix a minor long-standing bug in login, where we would offer the `password` login type if a custom auth provider supported it, even if password login was disabled. ([\#8835](https://github.com/matrix-org/synapse/issues/8835))
- Fix a long-standing bug which caused Synapse to require unspecified parameters during user-interactive authentication. ([\#8848](https://github.com/matrix-org/synapse/issues/8848))
- Fix a bug introduced in v1.20.0 where the user-agent and IP address reported during user registration for CAS, OpenID Connect, and SAML were of the wrong form. ([\#8784](https://github.com/matrix-org/synapse/issues/8784))


Improved Documentation
----------------------

- Clarify the usecase for a msisdn delegate. Contributed by Adrian Wannenmacher. ([\#8734](https://github.com/matrix-org/synapse/issues/8734))
- Remove extraneous comma from JSON example in User Admin API docs. ([\#8771](https://github.com/matrix-org/synapse/issues/8771))
- Update `turn-howto.md` with troubleshooting notes. ([\#8779](https://github.com/matrix-org/synapse/issues/8779))
- Fix the example on how to set the `Content-Type` header in nginx for the Client Well-Known URI. ([\#8793](https://github.com/matrix-org/synapse/issues/8793))
- Improve the documentation for the admin API to list all media in a room with respect to encrypted events. ([\#8795](https://github.com/matrix-org/synapse/issues/8795))
- Update the formatting of the `push` section of the homeserver config file to better align with the [code style guidelines](https://github.com/matrix-org/synapse/blob/develop/docs/code_style.md#configuration-file-format). ([\#8818](https://github.com/matrix-org/synapse/issues/8818))
- Improve documentation how to configure prometheus for workers. ([\#8822](https://github.com/matrix-org/synapse/issues/8822))
- Update example prometheus console. ([\#8824](https://github.com/matrix-org/synapse/issues/8824))


Deprecations and Removals
-------------------------

- Remove old `/_matrix/client/*/admin` endpoints which were deprecated since Synapse 1.20.0. ([\#8785](https://github.com/matrix-org/synapse/issues/8785))
- Disable pretty printing JSON responses for curl. Users who want pretty-printed output should use [jq](https://stedolan.github.io/jq/) in combination with curl. Contributed by @tulir. ([\#8833](https://github.com/matrix-org/synapse/issues/8833))


Internal Changes
----------------

- Simplify the way the `HomeServer` object caches its internal attributes. ([\#8565](https://github.com/matrix-org/synapse/issues/8565), [\#8851](https://github.com/matrix-org/synapse/issues/8851))
- Add an example and documentation for clock skew to the SAML2 sample configuration to allow for clock/time difference between the homserver and IdP. Contributed by @localguru. ([\#8731](https://github.com/matrix-org/synapse/issues/8731))
- Generalise `RoomMemberHandler._locally_reject_invite` to apply to more flows than just invite. ([\#8751](https://github.com/matrix-org/synapse/issues/8751))
- Generalise `RoomStore.maybe_store_room_on_invite` to handle other, non-invite membership events. ([\#8754](https://github.com/matrix-org/synapse/issues/8754))
- Refactor test utilities for injecting HTTP requests. ([\#8757](https://github.com/matrix-org/synapse/issues/8757), [\#8758](https://github.com/matrix-org/synapse/issues/8758), [\#8759](https://github.com/matrix-org/synapse/issues/8759), [\#8760](https://github.com/matrix-org/synapse/issues/8760), [\#8761](https://github.com/matrix-org/synapse/issues/8761), [\#8777](https://github.com/matrix-org/synapse/issues/8777))
- Consolidate logic between the OpenID Connect and SAML code. ([\#8765](https://github.com/matrix-org/synapse/issues/8765))
- Use `TYPE_CHECKING` instead of magic `MYPY` variable. ([\#8770](https://github.com/matrix-org/synapse/issues/8770))
- Add a commandline script to sign arbitrary json objects. ([\#8772](https://github.com/matrix-org/synapse/issues/8772))
- Minor log line improvements for the SSO mapping code used to generate Matrix IDs from SSO IDs. ([\#8773](https://github.com/matrix-org/synapse/issues/8773))
- Add additional error checking for OpenID Connect and SAML mapping providers. ([\#8774](https://github.com/matrix-org/synapse/issues/8774), [\#8800](https://github.com/matrix-org/synapse/issues/8800))
- Add type hints to HTTP abstractions. ([\#8806](https://github.com/matrix-org/synapse/issues/8806), [\#8812](https://github.com/matrix-org/synapse/issues/8812))
- Remove unnecessary function arguments and add typing to several membership replication classes. ([\#8809](https://github.com/matrix-org/synapse/issues/8809))
- Optimise the lookup for an invite from another homeserver when trying to reject it. ([\#8815](https://github.com/matrix-org/synapse/issues/8815))
- Add tests for `password_auth_provider`s. ([\#8819](https://github.com/matrix-org/synapse/issues/8819))
- Drop redundant database index on `event_json`. ([\#8845](https://github.com/matrix-org/synapse/issues/8845))
- Simplify `uk.half-shot.msc2778.login.application_service` login handler. ([\#8847](https://github.com/matrix-org/synapse/issues/8847))
- Refactor `password_auth_provider` support code. ([\#8849](https://github.com/matrix-org/synapse/issues/8849))
- Add missing `ordering` to background database updates. ([\#8850](https://github.com/matrix-org/synapse/issues/8850))
- Allow for specifying a room version when creating a room in unit tests via `RestHelper.create_room_as`. ([\#8854](https://github.com/matrix-org/synapse/issues/8854))


Synapse 1.23.0 (2020-11-18)
===========================

This release changes the way structured logging is configured. See the [upgrade notes](docs/upgrade.md#upgrading-to-v1230) for details.

**Note**: We are aware of a trivially exploitable denial of service vulnerability in versions of Synapse prior to 1.20.0. Complete details will be disclosed on Monday, November 23rd. If you have not upgraded recently, please do so.

Bugfixes
--------

- Fix a dependency versioning bug in the Dockerfile that prevented Synapse from starting. ([\#8767](https://github.com/matrix-org/synapse/issues/8767))


Synapse 1.23.0rc1 (2020-11-13)
==============================

Features
--------

- Add a push rule that highlights when a jitsi conference is created in a room. ([\#8286](https://github.com/matrix-org/synapse/issues/8286))
- Add an admin api to delete a single file or files that were not used for a defined time from server. Contributed by @dklimpel. ([\#8519](https://github.com/matrix-org/synapse/issues/8519))
- Split admin API for reported events (`GET /_synapse/admin/v1/event_reports`) into detail and list endpoints. This is a breaking change to #8217 which was introduced in Synapse v1.21.0. Those who already use this API should check their scripts. Contributed by @dklimpel. ([\#8539](https://github.com/matrix-org/synapse/issues/8539))
- Support generating structured logs via the standard logging configuration. ([\#8607](https://github.com/matrix-org/synapse/issues/8607), [\#8685](https://github.com/matrix-org/synapse/issues/8685))
- Add an admin API to allow server admins to list users' pushers. Contributed by @dklimpel. ([\#8610](https://github.com/matrix-org/synapse/issues/8610), [\#8689](https://github.com/matrix-org/synapse/issues/8689))
- Add an admin API `GET /_synapse/admin/v1/users/<user_id>/media` to get information about uploaded media. Contributed by @dklimpel. ([\#8647](https://github.com/matrix-org/synapse/issues/8647))
- Add an admin API for local user media statistics. Contributed by @dklimpel. ([\#8700](https://github.com/matrix-org/synapse/issues/8700))
- Add `displayname` to Shared-Secret Registration for admins. ([\#8722](https://github.com/matrix-org/synapse/issues/8722))


Bugfixes
--------

- Fix fetching of E2E cross signing keys over federation when only one of the master key and device signing key is cached already. ([\#8455](https://github.com/matrix-org/synapse/issues/8455))
- Fix a bug where Synapse would blindly forward bad responses from federation to clients when retrieving profile information. ([\#8580](https://github.com/matrix-org/synapse/issues/8580))
- Fix a bug where the account validity endpoint would silently fail if the user ID did not have an expiration time. It now returns a 400 error. ([\#8620](https://github.com/matrix-org/synapse/issues/8620))
- Fix email notifications for invites without local state. ([\#8627](https://github.com/matrix-org/synapse/issues/8627))
- Fix handling of invalid group IDs to return a 400 rather than log an exception and return a 500. ([\#8628](https://github.com/matrix-org/synapse/issues/8628))
- Fix handling of User-Agent headers that are invalid UTF-8, which caused user agents of users to not get correctly recorded. ([\#8632](https://github.com/matrix-org/synapse/issues/8632))
- Fix a bug in the `joined_rooms` admin API if the user has never joined any rooms. The bug was introduced, along with the API, in v1.21.0. ([\#8643](https://github.com/matrix-org/synapse/issues/8643))
- Fix exception during handling multiple concurrent requests for remote media when using multiple media repositories. ([\#8682](https://github.com/matrix-org/synapse/issues/8682))
- Fix bug that prevented Synapse from recovering after losing connection to the database. ([\#8726](https://github.com/matrix-org/synapse/issues/8726))
- Fix bug where the `/_synapse/admin/v1/send_server_notice` API could send notices to non-notice rooms. ([\#8728](https://github.com/matrix-org/synapse/issues/8728))
- Fix PostgreSQL port script fails when DB has no backfilled events. Broke in v1.21.0. ([\#8729](https://github.com/matrix-org/synapse/issues/8729))
- Fix PostgreSQL port script to correctly handle foreign key constraints. Broke in v1.21.0. ([\#8730](https://github.com/matrix-org/synapse/issues/8730))
- Fix PostgreSQL port script so that it can be run again after a failure. Broke in v1.21.0. ([\#8755](https://github.com/matrix-org/synapse/issues/8755))


Improved Documentation
----------------------

- Instructions for Azure AD in the OpenID Connect documentation. Contributed by peterk. ([\#8582](https://github.com/matrix-org/synapse/issues/8582))
- Improve the sample configuration for single sign-on providers. ([\#8635](https://github.com/matrix-org/synapse/issues/8635))
- Fix the filepath of Dex's example config and the link to Dex's Getting Started guide in the OpenID Connect docs. ([\#8657](https://github.com/matrix-org/synapse/issues/8657))
- Note support for Python 3.9. ([\#8665](https://github.com/matrix-org/synapse/issues/8665))
- Minor updates to docs on running tests. ([\#8666](https://github.com/matrix-org/synapse/issues/8666))
- Interlink prometheus/grafana documentation. ([\#8667](https://github.com/matrix-org/synapse/issues/8667))
- Notes on SSO logins and media_repository worker. ([\#8701](https://github.com/matrix-org/synapse/issues/8701))
- Document experimental support for running multiple event persisters. ([\#8706](https://github.com/matrix-org/synapse/issues/8706))
- Add information regarding the various sources of, and expected contributions to, Synapse's documentation to `CONTRIBUTING.md`. ([\#8714](https://github.com/matrix-org/synapse/issues/8714))
- Migrate documentation `docs/admin_api/event_reports` to markdown. ([\#8742](https://github.com/matrix-org/synapse/issues/8742))
- Add some helpful hints to the README for new Synapse developers. Contributed by @chagai95. ([\#8746](https://github.com/matrix-org/synapse/issues/8746))


Internal Changes
----------------

- Optimise `/createRoom` with multiple invited users. ([\#8559](https://github.com/matrix-org/synapse/issues/8559))
- Implement and use an `@lru_cache` decorator. ([\#8595](https://github.com/matrix-org/synapse/issues/8595))
- Don't instansiate Requester directly. ([\#8614](https://github.com/matrix-org/synapse/issues/8614))
- Type hints for `RegistrationStore`. ([\#8615](https://github.com/matrix-org/synapse/issues/8615))
- Change schema to support access tokens belonging to one user but granting access to another. ([\#8616](https://github.com/matrix-org/synapse/issues/8616))
- Remove unused OPTIONS handlers. ([\#8621](https://github.com/matrix-org/synapse/issues/8621))
- Run `mypy` as part of the lint.sh script. ([\#8633](https://github.com/matrix-org/synapse/issues/8633))
- Correct Synapse's PyPI package name in the OpenID Connect installation instructions. ([\#8634](https://github.com/matrix-org/synapse/issues/8634))
- Catch exceptions during initialization of `password_providers`. Contributed by Nicolai Søborg. ([\#8636](https://github.com/matrix-org/synapse/issues/8636))
- Fix typos and spelling errors in the code. ([\#8639](https://github.com/matrix-org/synapse/issues/8639))
- Reduce number of OpenTracing spans started. ([\#8640](https://github.com/matrix-org/synapse/issues/8640), [\#8668](https://github.com/matrix-org/synapse/issues/8668), [\#8670](https://github.com/matrix-org/synapse/issues/8670))
- Add field `total` to device list in admin API. ([\#8644](https://github.com/matrix-org/synapse/issues/8644))
- Add more type hints to the application services code. ([\#8655](https://github.com/matrix-org/synapse/issues/8655), [\#8693](https://github.com/matrix-org/synapse/issues/8693))
- Tell Black to format code for Python 3.5. ([\#8664](https://github.com/matrix-org/synapse/issues/8664))
- Don't pull event from DB when handling replication traffic. ([\#8669](https://github.com/matrix-org/synapse/issues/8669))
- Abstract some invite-related code in preparation for landing knocking. ([\#8671](https://github.com/matrix-org/synapse/issues/8671), [\#8688](https://github.com/matrix-org/synapse/issues/8688))
- Clarify representation of events in logfiles. ([\#8679](https://github.com/matrix-org/synapse/issues/8679))
- Don't require `hiredis` package to be installed to run unit tests. ([\#8680](https://github.com/matrix-org/synapse/issues/8680))
- Fix typing info on cache call signature to accept `on_invalidate`. ([\#8684](https://github.com/matrix-org/synapse/issues/8684))
- Fail tests if they do not await coroutines. ([\#8690](https://github.com/matrix-org/synapse/issues/8690))
- Improve start time by adding an index to `e2e_cross_signing_keys.stream_id`. ([\#8694](https://github.com/matrix-org/synapse/issues/8694))
- Re-organize the structured logging code to separate the TCP transport handling from the JSON formatting. ([\#8697](https://github.com/matrix-org/synapse/issues/8697))
- Use Python 3.8 in Docker images by default. ([\#8698](https://github.com/matrix-org/synapse/issues/8698))
- Remove the "draft" status of the Room Details Admin API. ([\#8702](https://github.com/matrix-org/synapse/issues/8702))
- Improve the error returned when a non-string displayname or avatar_url is used when updating a user's profile. ([\#8705](https://github.com/matrix-org/synapse/issues/8705))
- Block attempts by clients to send server ACLs, or redactions of server ACLs, that would result in the local server being blocked from the room. ([\#8708](https://github.com/matrix-org/synapse/issues/8708))
- Add metrics the allow the local sysadmin to track 3PID `/requestToken` requests. ([\#8712](https://github.com/matrix-org/synapse/issues/8712))
- Consolidate duplicated lists of purged tables that are checked in tests. ([\#8713](https://github.com/matrix-org/synapse/issues/8713))
- Add some `mdui:UIInfo` element examples for `saml2_config` in the homeserver config. ([\#8718](https://github.com/matrix-org/synapse/issues/8718))
- Improve the error message returned when a remote server incorrectly sets the `Content-Type` header in response to a JSON request. ([\#8719](https://github.com/matrix-org/synapse/issues/8719))
- Speed up repeated state resolutions on the same room by caching event ID to auth event ID lookups. ([\#8752](https://github.com/matrix-org/synapse/issues/8752))


Synapse 1.22.1 (2020-10-30)
===========================

Bugfixes
--------

- Fix a bug where an appservice may not be forwarded events for a room it was recently invited to. Broke in v1.22.0. ([\#8676](https://github.com/matrix-org/synapse/issues/8676))
- Fix `Object of type frozendict is not JSON serializable` exceptions when using third-party event rules. Broke in v1.22.0. ([\#8678](https://github.com/matrix-org/synapse/issues/8678))


Synapse 1.22.0 (2020-10-27)
===========================

No significant changes.


Synapse 1.22.0rc2 (2020-10-26)
==============================

Bugfixes
--------

- Fix bugs where ephemeral events were not sent to appservices. Broke in v1.22.0rc1. ([\#8648](https://github.com/matrix-org/synapse/issues/8648), [\#8656](https://github.com/matrix-org/synapse/issues/8656))
- Fix `user_daily_visits` table to not have duplicate rows per user/device due to multiple user agents. Broke in v1.22.0rc1. ([\#8654](https://github.com/matrix-org/synapse/issues/8654))

Synapse 1.22.0rc1 (2020-10-22)
==============================

Features
--------

- Add a configuration option for always using the "userinfo endpoint" for OpenID Connect. This fixes support for some identity providers, e.g. GitLab. Contributed by Benjamin Koch. ([\#7658](https://github.com/matrix-org/synapse/issues/7658))
- Add ability for `ThirdPartyEventRules` modules to query and manipulate whether a room is in the public rooms directory. ([\#8292](https://github.com/matrix-org/synapse/issues/8292), [\#8467](https://github.com/matrix-org/synapse/issues/8467))
- Add support for olm fallback keys ([MSC2732](https://github.com/matrix-org/matrix-doc/pull/2732)). ([\#8312](https://github.com/matrix-org/synapse/issues/8312), [\#8501](https://github.com/matrix-org/synapse/issues/8501))
- Add support for running background tasks in a separate worker process. ([\#8369](https://github.com/matrix-org/synapse/issues/8369), [\#8458](https://github.com/matrix-org/synapse/issues/8458), [\#8489](https://github.com/matrix-org/synapse/issues/8489), [\#8513](https://github.com/matrix-org/synapse/issues/8513), [\#8544](https://github.com/matrix-org/synapse/issues/8544), [\#8599](https://github.com/matrix-org/synapse/issues/8599))
- Add support for device dehydration ([MSC2697](https://github.com/matrix-org/matrix-doc/pull/2697)). ([\#8380](https://github.com/matrix-org/synapse/issues/8380))
- Add support for [MSC2409](https://github.com/matrix-org/matrix-doc/pull/2409), which allows sending typing, read receipts, and presence events to appservices. ([\#8437](https://github.com/matrix-org/synapse/issues/8437), [\#8590](https://github.com/matrix-org/synapse/issues/8590))
- Change default room version to "6", per [MSC2788](https://github.com/matrix-org/matrix-doc/pull/2788). ([\#8461](https://github.com/matrix-org/synapse/issues/8461))
- Add the ability to send non-membership events into a room via the `ModuleApi`. ([\#8479](https://github.com/matrix-org/synapse/issues/8479))
- Increase default upload size limit from 10M to 50M. Contributed by @Akkowicz. ([\#8502](https://github.com/matrix-org/synapse/issues/8502))
- Add support for modifying event content in `ThirdPartyRules` modules. ([\#8535](https://github.com/matrix-org/synapse/issues/8535), [\#8564](https://github.com/matrix-org/synapse/issues/8564))


Bugfixes
--------

- Fix a longstanding bug where invalid ignored users in account data could break clients. ([\#8454](https://github.com/matrix-org/synapse/issues/8454))
- Fix a bug where backfilling a room with an event that was missing the `redacts` field would break. ([\#8457](https://github.com/matrix-org/synapse/issues/8457))
- Don't attempt to respond to some requests if the client has already disconnected. ([\#8465](https://github.com/matrix-org/synapse/issues/8465))
- Fix message duplication if something goes wrong after persisting the event. ([\#8476](https://github.com/matrix-org/synapse/issues/8476))
- Fix incremental sync returning an incorrect `prev_batch` token in timeline section, which when used to paginate returned events that were included in the incremental sync. Broken since v0.16.0. ([\#8486](https://github.com/matrix-org/synapse/issues/8486))
- Expose the `uk.half-shot.msc2778.login.application_service` to clients from the login API. This feature was added in v1.21.0, but was not exposed as a potential login flow. ([\#8504](https://github.com/matrix-org/synapse/issues/8504))
- Fix error code for `/profile/{userId}/displayname` to be `M_BAD_JSON`. ([\#8517](https://github.com/matrix-org/synapse/issues/8517))
- Fix a bug introduced in v1.7.0 that could cause Synapse to insert values from non-state `m.room.retention` events into the `room_retention` database table. ([\#8527](https://github.com/matrix-org/synapse/issues/8527))
- Fix not sending events over federation when using sharded event writers. ([\#8536](https://github.com/matrix-org/synapse/issues/8536))
- Fix a long standing bug where email notifications for encrypted messages were blank. ([\#8545](https://github.com/matrix-org/synapse/issues/8545))
- Fix increase in the number of `There was no active span...` errors logged when using OpenTracing. ([\#8567](https://github.com/matrix-org/synapse/issues/8567))
- Fix a bug that prevented errors encountered during execution of the `synapse_port_db` from being correctly printed. ([\#8585](https://github.com/matrix-org/synapse/issues/8585))
- Fix appservice transactions to only include a maximum of 100 persistent and 100 ephemeral events. ([\#8606](https://github.com/matrix-org/synapse/issues/8606))


Updates to the Docker image
---------------------------

- Added multi-arch support (arm64,arm/v7) for the docker images. Contributed by @maquis196. ([\#7921](https://github.com/matrix-org/synapse/issues/7921))
- Add support for passing commandline args to the synapse process. Contributed by @samuel-p. ([\#8390](https://github.com/matrix-org/synapse/issues/8390))


Improved Documentation
----------------------

- Update the directions for using the manhole with coroutines. ([\#8462](https://github.com/matrix-org/synapse/issues/8462))
- Improve readme by adding new shield.io badges. ([\#8493](https://github.com/matrix-org/synapse/issues/8493))
- Added note about docker in manhole.md regarding which ip address to bind to. Contributed by @Maquis196. ([\#8526](https://github.com/matrix-org/synapse/issues/8526))
- Document the new behaviour of the `allowed_lifetime_min` and `allowed_lifetime_max` settings in the room retention configuration. ([\#8529](https://github.com/matrix-org/synapse/issues/8529))


Deprecations and Removals
-------------------------

- Drop unused `device_max_stream_id` table. ([\#8589](https://github.com/matrix-org/synapse/issues/8589))


Internal Changes
----------------

- Check for unreachable code with mypy. ([\#8432](https://github.com/matrix-org/synapse/issues/8432))
- Add unit test for event persister sharding. ([\#8433](https://github.com/matrix-org/synapse/issues/8433))
- Allow events to be sent to clients sooner when using sharded event persisters. ([\#8439](https://github.com/matrix-org/synapse/issues/8439), [\#8488](https://github.com/matrix-org/synapse/issues/8488), [\#8496](https://github.com/matrix-org/synapse/issues/8496), [\#8499](https://github.com/matrix-org/synapse/issues/8499))
- Configure `public_baseurl` when using demo scripts. ([\#8443](https://github.com/matrix-org/synapse/issues/8443))
- Add SQL logging on queries that happen during startup. ([\#8448](https://github.com/matrix-org/synapse/issues/8448))
- Speed up unit tests when using PostgreSQL. ([\#8450](https://github.com/matrix-org/synapse/issues/8450))
- Remove redundant database loads of stream_ordering for events we already have. ([\#8452](https://github.com/matrix-org/synapse/issues/8452))
- Reduce inconsistencies between codepaths for membership and non-membership events. ([\#8463](https://github.com/matrix-org/synapse/issues/8463))
- Combine `SpamCheckerApi` with the more generic `ModuleApi`. ([\#8464](https://github.com/matrix-org/synapse/issues/8464))
- Additional testing for `ThirdPartyEventRules`. ([\#8468](https://github.com/matrix-org/synapse/issues/8468))
- Add `-d` option to `./scripts-dev/lint.sh` to lint files that have changed since the last git commit. ([\#8472](https://github.com/matrix-org/synapse/issues/8472))
- Unblacklist some sytests. ([\#8474](https://github.com/matrix-org/synapse/issues/8474))
- Include the log level in the phone home stats. ([\#8477](https://github.com/matrix-org/synapse/issues/8477))
- Remove outdated sphinx documentation, scripts and configuration. ([\#8480](https://github.com/matrix-org/synapse/issues/8480))
- Clarify error message when plugin config parsers raise an error. ([\#8492](https://github.com/matrix-org/synapse/issues/8492))
- Remove the deprecated `Handlers` object. ([\#8494](https://github.com/matrix-org/synapse/issues/8494))
- Fix a threadsafety bug in unit tests. ([\#8497](https://github.com/matrix-org/synapse/issues/8497))
- Add user agent to user_daily_visits table. ([\#8503](https://github.com/matrix-org/synapse/issues/8503))
- Add type hints to various parts of the code base. ([\#8407](https://github.com/matrix-org/synapse/issues/8407), [\#8505](https://github.com/matrix-org/synapse/issues/8505), [\#8507](https://github.com/matrix-org/synapse/issues/8507), [\#8547](https://github.com/matrix-org/synapse/issues/8547), [\#8562](https://github.com/matrix-org/synapse/issues/8562), [\#8609](https://github.com/matrix-org/synapse/issues/8609))
- Remove unused code from the test framework. ([\#8514](https://github.com/matrix-org/synapse/issues/8514))
- Apply some internal fixes to the `HomeServer` class to make its code more idiomatic and statically-verifiable. ([\#8515](https://github.com/matrix-org/synapse/issues/8515))
- Factor out common code between `RoomMemberHandler._locally_reject_invite` and `EventCreationHandler.create_event`. ([\#8537](https://github.com/matrix-org/synapse/issues/8537))
- Improve database performance by executing more queries without starting transactions. ([\#8542](https://github.com/matrix-org/synapse/issues/8542))
- Rename `Cache` to `DeferredCache`, to better reflect its purpose. ([\#8548](https://github.com/matrix-org/synapse/issues/8548))
- Move metric registration code down into `LruCache`. ([\#8561](https://github.com/matrix-org/synapse/issues/8561), [\#8591](https://github.com/matrix-org/synapse/issues/8591))
- Replace `DeferredCache` with the lighter-weight `LruCache` where possible. ([\#8563](https://github.com/matrix-org/synapse/issues/8563))
- Add virtualenv-generated folders to `.gitignore`. ([\#8566](https://github.com/matrix-org/synapse/issues/8566))
- Add `get_immediate` method to `DeferredCache`. ([\#8568](https://github.com/matrix-org/synapse/issues/8568))
- Fix mypy not properly checking across the codebase, additionally, fix a typing assertion error in `handlers/auth.py`. ([\#8569](https://github.com/matrix-org/synapse/issues/8569))
- Fix `synmark` benchmark runner. ([\#8571](https://github.com/matrix-org/synapse/issues/8571))
- Modify `DeferredCache.get()` to return `Deferred`s instead of `ObservableDeferred`s. ([\#8572](https://github.com/matrix-org/synapse/issues/8572))
- Adjust a protocol-type definition to fit `sqlite3` assertions. ([\#8577](https://github.com/matrix-org/synapse/issues/8577))
- Support macOS on the `synmark` benchmark runner. ([\#8578](https://github.com/matrix-org/synapse/issues/8578))
- Update `mypy` static type checker to 0.790. ([\#8583](https://github.com/matrix-org/synapse/issues/8583), [\#8600](https://github.com/matrix-org/synapse/issues/8600))
- Re-organize the structured logging code to separate the TCP transport handling from the JSON formatting. ([\#8587](https://github.com/matrix-org/synapse/issues/8587))
- Remove extraneous unittest logging decorators from unit tests. ([\#8592](https://github.com/matrix-org/synapse/issues/8592))
- Minor optimisations in caching code. ([\#8593](https://github.com/matrix-org/synapse/issues/8593), [\#8594](https://github.com/matrix-org/synapse/issues/8594))


Synapse 1.21.2 (2020-10-15)
===========================

Debian packages and Docker images have been rebuilt using the latest versions of dependency libraries, including authlib 0.15.1. Please see bugfixes below.

Security advisory
-----------------

* HTML pages served via Synapse were vulnerable to cross-site scripting (XSS)
  attacks. All server administrators are encouraged to upgrade.
  ([\#8444](https://github.com/matrix-org/synapse/pull/8444))
  ([CVE-2020-26891](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2020-26891))

  This fix was originally included in v1.21.0 but was missing a security advisory.

  This was reported by [Denis Kasak](https://github.com/dkasak).

Bugfixes
--------

- Fix rare bug where sending an event would fail due to a racey assertion. ([\#8530](https://github.com/matrix-org/synapse/issues/8530))
- An updated version of the authlib dependency is included in the Docker and Debian images to fix an issue using OpenID Connect. See [\#8534](https://github.com/matrix-org/synapse/issues/8534) for details.


Synapse 1.21.1 (2020-10-13)
===========================

This release fixes a regression in v1.21.0 that prevented debian packages from being built.
It is otherwise identical to v1.21.0.

Synapse 1.21.0 (2020-10-12)
===========================

No significant changes since v1.21.0rc3.

As [noted in
v1.20.0](https://github.com/matrix-org/synapse/blob/release-v1.21.0/CHANGES.md#synapse-1200-2020-09-22),
a future release will drop support for accessing Synapse's
[Admin API](https://github.com/matrix-org/synapse/tree/master/docs/admin_api) under the
`/_matrix/client/*` endpoint prefixes. At that point, the Admin API will only
be accessible under `/_synapse/admin`.


Synapse 1.21.0rc3 (2020-10-08)
==============================

Bugfixes
--------

- Fix duplication of events on high traffic servers, caused by PostgreSQL `could not serialize access due to concurrent update` errors. ([\#8456](https://github.com/matrix-org/synapse/issues/8456))


Internal Changes
----------------

- Add Groovy Gorilla to the list of distributions we build `.deb`s for. ([\#8475](https://github.com/matrix-org/synapse/issues/8475))


Synapse 1.21.0rc2 (2020-10-02)
==============================

Features
--------

- Convert additional templates from inline HTML to Jinja2 templates. ([\#8444](https://github.com/matrix-org/synapse/issues/8444))

Bugfixes
--------

- Fix a regression in v1.21.0rc1 which broke thumbnails of remote media. ([\#8438](https://github.com/matrix-org/synapse/issues/8438))
- Do not expose the experimental `uk.half-shot.msc2778.login.application_service` flow in the login API, which caused a compatibility problem with Element iOS. ([\#8440](https://github.com/matrix-org/synapse/issues/8440))
- Fix malformed log line in new federation "catch up" logic. ([\#8442](https://github.com/matrix-org/synapse/issues/8442))
- Fix DB query on startup for negative streams which caused long start up times. Introduced in [\#8374](https://github.com/matrix-org/synapse/issues/8374). ([\#8447](https://github.com/matrix-org/synapse/issues/8447))


Synapse 1.21.0rc1 (2020-10-01)
==============================

Features
--------

- Require the user to confirm that their password should be reset after clicking the email confirmation link. ([\#8004](https://github.com/matrix-org/synapse/issues/8004))
- Add an admin API `GET /_synapse/admin/v1/event_reports` to read entries of table `event_reports`. Contributed by @dklimpel. ([\#8217](https://github.com/matrix-org/synapse/issues/8217))
- Consolidate the SSO error template across all configuration. ([\#8248](https://github.com/matrix-org/synapse/issues/8248), [\#8405](https://github.com/matrix-org/synapse/issues/8405))
- Add a configuration option to specify a whitelist of domains that a user can be redirected to after validating their email or phone number. ([\#8275](https://github.com/matrix-org/synapse/issues/8275), [\#8417](https://github.com/matrix-org/synapse/issues/8417))
- Add experimental support for sharding event persister. ([\#8294](https://github.com/matrix-org/synapse/issues/8294), [\#8387](https://github.com/matrix-org/synapse/issues/8387), [\#8396](https://github.com/matrix-org/synapse/issues/8396), [\#8419](https://github.com/matrix-org/synapse/issues/8419))
- Add the room topic and avatar to the room details admin API. ([\#8305](https://github.com/matrix-org/synapse/issues/8305))
- Add an admin API for querying rooms where a user is a member. Contributed by @dklimpel. ([\#8306](https://github.com/matrix-org/synapse/issues/8306))
- Add `uk.half-shot.msc2778.login.application_service` login type to allow appservices to login. ([\#8320](https://github.com/matrix-org/synapse/issues/8320))
- Add a configuration option that allows existing users to log in with OpenID Connect. Contributed by @BBBSnowball and @OmmyZhang. ([\#8345](https://github.com/matrix-org/synapse/issues/8345))
- Add prometheus metrics for replication requests. ([\#8406](https://github.com/matrix-org/synapse/issues/8406))
- Support passing additional single sign-on parameters to the client. ([\#8413](https://github.com/matrix-org/synapse/issues/8413))
- Add experimental reporting of metrics on expensive rooms for state-resolution. ([\#8420](https://github.com/matrix-org/synapse/issues/8420))
- Add experimental prometheus metric to track numbers of "large" rooms for state resolutiom. ([\#8425](https://github.com/matrix-org/synapse/issues/8425))
- Add prometheus metrics to track federation delays. ([\#8430](https://github.com/matrix-org/synapse/issues/8430))


Bugfixes
--------

- Fix a bug in the media repository where remote thumbnails with the same size but different crop methods would overwrite each other. Contributed by @deepbluev7. ([\#7124](https://github.com/matrix-org/synapse/issues/7124))
- Fix inconsistent handling of non-existent push rules, and stop tracking the `enabled` state of removed push rules. ([\#7796](https://github.com/matrix-org/synapse/issues/7796))
- Fix a longstanding bug when storing a media file with an empty `upload_name`. ([\#7905](https://github.com/matrix-org/synapse/issues/7905))
- Fix messages not being sent over federation until an event is sent into the same room. ([\#8230](https://github.com/matrix-org/synapse/issues/8230), [\#8247](https://github.com/matrix-org/synapse/issues/8247), [\#8258](https://github.com/matrix-org/synapse/issues/8258), [\#8272](https://github.com/matrix-org/synapse/issues/8272), [\#8322](https://github.com/matrix-org/synapse/issues/8322))
- Fix a longstanding bug where files that could not be thumbnailed would result in an Internal Server Error. ([\#8236](https://github.com/matrix-org/synapse/issues/8236), [\#8435](https://github.com/matrix-org/synapse/issues/8435))
- Upgrade minimum version of `canonicaljson` to version 1.4.0, to fix an unicode encoding issue. ([\#8262](https://github.com/matrix-org/synapse/issues/8262))
- Fix longstanding bug which could lead to incomplete database upgrades on SQLite. ([\#8265](https://github.com/matrix-org/synapse/issues/8265))
- Fix stack overflow when stderr is redirected to the logging system, and the logging system encounters an error. ([\#8268](https://github.com/matrix-org/synapse/issues/8268))
- Fix a bug which cause the logging system to report errors, if `DEBUG` was enabled and no `context` filter was applied. ([\#8278](https://github.com/matrix-org/synapse/issues/8278))
- Fix edge case where push could get delayed for a user until a later event was pushed. ([\#8287](https://github.com/matrix-org/synapse/issues/8287))
- Fix fetching malformed events from remote servers. ([\#8324](https://github.com/matrix-org/synapse/issues/8324))
- Fix `UnboundLocalError` from occuring when appservices send a malformed register request. ([\#8329](https://github.com/matrix-org/synapse/issues/8329))
- Don't send push notifications to expired user accounts. ([\#8353](https://github.com/matrix-org/synapse/issues/8353))
- Fix a regression in v1.19.0 with reactivating users through the admin API. ([\#8362](https://github.com/matrix-org/synapse/issues/8362))
- Fix a bug where during device registration the length of the device name wasn't limited. ([\#8364](https://github.com/matrix-org/synapse/issues/8364))
- Include `guest_access` in the fields that are checked for null bytes when updating `room_stats_state`. Broke in v1.7.2. ([\#8373](https://github.com/matrix-org/synapse/issues/8373))
- Fix theoretical race condition where events are not sent down `/sync` if the synchrotron worker is restarted without restarting other workers. ([\#8374](https://github.com/matrix-org/synapse/issues/8374))
- Fix a bug which could cause errors in rooms with malformed membership events, on servers using sqlite. ([\#8385](https://github.com/matrix-org/synapse/issues/8385))
- Fix "Re-starting finished log context" warning when receiving an event we already had over federation. ([\#8398](https://github.com/matrix-org/synapse/issues/8398))
- Fix incorrect handling of timeouts on outgoing HTTP requests. ([\#8400](https://github.com/matrix-org/synapse/issues/8400))
- Fix a regression in v1.20.0 in the `synapse_port_db` script regarding the `ui_auth_sessions_ips` table. ([\#8410](https://github.com/matrix-org/synapse/issues/8410))
- Remove unnecessary 3PID registration check when resetting password via an email address. Bug introduced in v0.34.0rc2. ([\#8414](https://github.com/matrix-org/synapse/issues/8414))


Improved Documentation
----------------------

- Add `/_synapse/client` to the reverse proxy documentation. ([\#8227](https://github.com/matrix-org/synapse/issues/8227))
- Add note to the reverse proxy settings documentation about disabling Apache's mod_security2. Contributed by Julian Fietkau (@jfietkau). ([\#8375](https://github.com/matrix-org/synapse/issues/8375))
- Improve description of `server_name` config option in `homserver.yaml`. ([\#8415](https://github.com/matrix-org/synapse/issues/8415))


Deprecations and Removals
-------------------------

- Drop support for `prometheus_client` older than 0.4.0. ([\#8426](https://github.com/matrix-org/synapse/issues/8426))


Internal Changes
----------------

- Fix tests on distros which disable TLSv1.0. Contributed by @danc86. ([\#8208](https://github.com/matrix-org/synapse/issues/8208))
- Simplify the distributor code to avoid unnecessary work. ([\#8216](https://github.com/matrix-org/synapse/issues/8216))
- Remove the `populate_stats_process_rooms_2` background job and restore functionality to `populate_stats_process_rooms`. ([\#8243](https://github.com/matrix-org/synapse/issues/8243))
- Clean up type hints for `PaginationConfig`. ([\#8250](https://github.com/matrix-org/synapse/issues/8250), [\#8282](https://github.com/matrix-org/synapse/issues/8282))
- Track the latest event for every destination and room for catch-up after federation outage. ([\#8256](https://github.com/matrix-org/synapse/issues/8256))
- Fix non-user visible bug in implementation of `MultiWriterIdGenerator.get_current_token_for_writer`. ([\#8257](https://github.com/matrix-org/synapse/issues/8257))
- Switch to the JSON implementation from the standard library. ([\#8259](https://github.com/matrix-org/synapse/issues/8259))
- Add type hints to `synapse.util.async_helpers`. ([\#8260](https://github.com/matrix-org/synapse/issues/8260))
- Simplify tests that mock asynchronous functions. ([\#8261](https://github.com/matrix-org/synapse/issues/8261))
- Add type hints to `StreamToken` and `RoomStreamToken` classes. ([\#8279](https://github.com/matrix-org/synapse/issues/8279))
- Change `StreamToken.room_key` to be a `RoomStreamToken` instance. ([\#8281](https://github.com/matrix-org/synapse/issues/8281))
- Refactor notifier code to correctly use the max event stream position. ([\#8288](https://github.com/matrix-org/synapse/issues/8288))
- Use slotted classes where possible. ([\#8296](https://github.com/matrix-org/synapse/issues/8296))
- Support testing the local Synapse checkout against the [Complement homeserver test suite](https://github.com/matrix-org/complement/). ([\#8317](https://github.com/matrix-org/synapse/issues/8317))
- Update outdated usages of `metaclass` to python 3 syntax. ([\#8326](https://github.com/matrix-org/synapse/issues/8326))
- Move lint-related dependencies to package-extra field, update CONTRIBUTING.md to utilise this. ([\#8330](https://github.com/matrix-org/synapse/issues/8330), [\#8377](https://github.com/matrix-org/synapse/issues/8377))
- Use the `admin_patterns` helper in additional locations. ([\#8331](https://github.com/matrix-org/synapse/issues/8331))
- Fix test logging to allow braces in log output. ([\#8335](https://github.com/matrix-org/synapse/issues/8335))
- Remove `__future__` imports related to Python 2 compatibility. ([\#8337](https://github.com/matrix-org/synapse/issues/8337))
- Simplify `super()` calls to Python 3 syntax. ([\#8344](https://github.com/matrix-org/synapse/issues/8344))
- Fix bad merge from `release-v1.20.0` branch to `develop`. ([\#8354](https://github.com/matrix-org/synapse/issues/8354))
- Factor out a `_send_dummy_event_for_room` method. ([\#8370](https://github.com/matrix-org/synapse/issues/8370))
- Improve logging of state resolution. ([\#8371](https://github.com/matrix-org/synapse/issues/8371))
- Add type annotations to `SimpleHttpClient`. ([\#8372](https://github.com/matrix-org/synapse/issues/8372))
- Refactor ID generators to use `async with` syntax. ([\#8383](https://github.com/matrix-org/synapse/issues/8383))
- Add `EventStreamPosition` type. ([\#8388](https://github.com/matrix-org/synapse/issues/8388))
- Create a mechanism for marking tests "logcontext clean". ([\#8399](https://github.com/matrix-org/synapse/issues/8399))
- A pair of tiny cleanups in the federation request code. ([\#8401](https://github.com/matrix-org/synapse/issues/8401))
- Add checks on startup that PostgreSQL sequences are consistent with their associated tables. ([\#8402](https://github.com/matrix-org/synapse/issues/8402))
- Do not include appservice users when calculating the total MAU for a server. ([\#8404](https://github.com/matrix-org/synapse/issues/8404))
- Typing fixes for `synapse.handlers.federation`. ([\#8422](https://github.com/matrix-org/synapse/issues/8422))
- Various refactors to simplify stream token handling. ([\#8423](https://github.com/matrix-org/synapse/issues/8423))
- Make stream token serializing/deserializing async. ([\#8427](https://github.com/matrix-org/synapse/issues/8427))


Synapse 1.20.1 (2020-09-24)
===========================

Bugfixes
--------

- Fix a bug introduced in v1.20.0 which caused the `synapse_port_db` script to fail. ([\#8386](https://github.com/matrix-org/synapse/issues/8386))
- Fix a bug introduced in v1.20.0 which caused variables to be incorrectly escaped in Jinja2 templates. ([\#8394](https://github.com/matrix-org/synapse/issues/8394))


Synapse 1.20.0 (2020-09-22)
===========================

No significant changes since v1.20.0rc5.

Removal warning
---------------

Historically, the [Synapse Admin
API](https://github.com/matrix-org/synapse/tree/master/docs) has been
accessible under the `/_matrix/client/api/v1/admin`,
`/_matrix/client/unstable/admin`, `/_matrix/client/r0/admin` and
`/_synapse/admin` prefixes. In a future release, we will be dropping support
for accessing Synapse's Admin API using the `/_matrix/client/*` prefixes.

From that point, the Admin API will only be accessible under `/_synapse/admin`.
This makes it easier for homeserver admins to lock down external access to the
Admin API endpoints.

Synapse 1.20.0rc5 (2020-09-18)
==============================

In addition to the below, Synapse 1.20.0rc5 also includes the bug fix that was included in 1.19.3.

Features
--------

- Add flags to the `/versions` endpoint for whether new rooms default to using E2EE. ([\#8343](https://github.com/matrix-org/synapse/issues/8343))


Bugfixes
--------

- Fix rate limiting of federation `/send` requests. ([\#8342](https://github.com/matrix-org/synapse/issues/8342))
- Fix a longstanding bug where back pagination over federation could get stuck if it failed to handle a received event. ([\#8349](https://github.com/matrix-org/synapse/issues/8349))


Internal Changes
----------------

- Blacklist [MSC2753](https://github.com/matrix-org/matrix-doc/pull/2753) SyTests until it is implemented. ([\#8285](https://github.com/matrix-org/synapse/issues/8285))


Synapse 1.19.3 (2020-09-18)
===========================

Bugfixes
--------

- Partially mitigate bug where newly joined servers couldn't get past events in a room when there is a malformed event. ([\#8350](https://github.com/matrix-org/synapse/issues/8350))


Synapse 1.20.0rc4 (2020-09-16)
==============================

Synapse 1.20.0rc4 is identical to 1.20.0rc3, with the addition of the security fix that was included in 1.19.2.


Synapse 1.19.2 (2020-09-16)
===========================

Due to the issue below server admins are encouraged to upgrade as soon as possible.

Bugfixes
--------

- Fix joining rooms over federation that include malformed events. ([\#8324](https://github.com/matrix-org/synapse/issues/8324))


Synapse 1.20.0rc3 (2020-09-11)
==============================

Bugfixes
--------

- Fix a bug introduced in v1.20.0rc1 where the wrong exception was raised when invalid JSON data is encountered. ([\#8291](https://github.com/matrix-org/synapse/issues/8291))


Synapse 1.20.0rc2 (2020-09-09)
==============================

Bugfixes
--------

- Fix a bug introduced in v1.20.0rc1 causing some features related to notifications to misbehave following the implementation of unread counts. ([\#8280](https://github.com/matrix-org/synapse/issues/8280))


Synapse 1.20.0rc1 (2020-09-08)
==============================

Removal warning
---------------

Some older clients used a [disallowed character](https://matrix.org/docs/spec/client_server/r0.6.1#post-matrix-client-r0-register-email-requesttoken) (`:`) in the `client_secret` parameter of various endpoints. The incorrect behaviour was allowed for backwards compatibility, but is now being removed from Synapse as most users have updated their client. Further context can be found at [\#6766](https://github.com/matrix-org/synapse/issues/6766).

Features
--------

- Add an endpoint to query your shared rooms with another user as an implementation of [MSC2666](https://github.com/matrix-org/matrix-doc/pull/2666). ([\#7785](https://github.com/matrix-org/synapse/issues/7785))
- Iteratively encode JSON to avoid blocking the reactor. ([\#8013](https://github.com/matrix-org/synapse/issues/8013), [\#8116](https://github.com/matrix-org/synapse/issues/8116))
- Add support for shadow-banning users (ignoring any message send requests). ([\#8034](https://github.com/matrix-org/synapse/issues/8034), [\#8092](https://github.com/matrix-org/synapse/issues/8092), [\#8095](https://github.com/matrix-org/synapse/issues/8095), [\#8142](https://github.com/matrix-org/synapse/issues/8142), [\#8152](https://github.com/matrix-org/synapse/issues/8152), [\#8157](https://github.com/matrix-org/synapse/issues/8157), [\#8158](https://github.com/matrix-org/synapse/issues/8158), [\#8176](https://github.com/matrix-org/synapse/issues/8176))
- Use the default template file when its equivalent is not found in a custom template directory. ([\#8037](https://github.com/matrix-org/synapse/issues/8037), [\#8107](https://github.com/matrix-org/synapse/issues/8107), [\#8252](https://github.com/matrix-org/synapse/issues/8252))
- Add unread messages count to sync responses, as specified in [MSC2654](https://github.com/matrix-org/matrix-doc/pull/2654). ([\#8059](https://github.com/matrix-org/synapse/issues/8059), [\#8254](https://github.com/matrix-org/synapse/issues/8254), [\#8270](https://github.com/matrix-org/synapse/issues/8270), [\#8274](https://github.com/matrix-org/synapse/issues/8274))
- Optimise `/federation/v1/user/devices/` API by only returning devices with encryption keys. ([\#8198](https://github.com/matrix-org/synapse/issues/8198))


Bugfixes
--------

- Fix a memory leak by limiting the length of time that messages will be queued for a remote server that has been unreachable. ([\#7864](https://github.com/matrix-org/synapse/issues/7864))
- Fix `Re-starting finished log context PUT-nnnn` warning when event persistence failed. ([\#8081](https://github.com/matrix-org/synapse/issues/8081))
- Synapse now correctly enforces the valid characters in the `client_secret` parameter used in various endpoints. ([\#8101](https://github.com/matrix-org/synapse/issues/8101))
- Fix a bug introduced in v1.7.2 impacting message retention policies that would allow federated homeservers to dictate a retention period that's lower than the configured minimum allowed duration in the configuration file. ([\#8104](https://github.com/matrix-org/synapse/issues/8104))
- Fix a long-standing bug where invalid JSON would be accepted by Synapse. ([\#8106](https://github.com/matrix-org/synapse/issues/8106))
- Fix a bug introduced in Synapse v1.12.0 which could cause `/sync` requests to fail with a 404 if you had a very old outstanding room invite. ([\#8110](https://github.com/matrix-org/synapse/issues/8110))
- Return a proper error code when the rooms of an invalid group are requested. ([\#8129](https://github.com/matrix-org/synapse/issues/8129))
- Fix a bug which could cause a leaked postgres connection if synapse was set to daemonize. ([\#8131](https://github.com/matrix-org/synapse/issues/8131))
- Clarify the error code if a user tries to register with a numeric ID. This bug was introduced in v1.15.0. ([\#8135](https://github.com/matrix-org/synapse/issues/8135))
- Fix a bug where appservices with ratelimiting disabled would still be ratelimited when joining rooms. This bug was introduced in v1.19.0. ([\#8139](https://github.com/matrix-org/synapse/issues/8139))
- Fix logging in via OpenID Connect with a provider that uses integer user IDs. ([\#8190](https://github.com/matrix-org/synapse/issues/8190))
- Fix a longstanding bug where user directory updates could break when unexpected profile data was included in events. ([\#8223](https://github.com/matrix-org/synapse/issues/8223))
- Fix a longstanding bug where stats updates could break when unexpected profile data was included in events. ([\#8226](https://github.com/matrix-org/synapse/issues/8226))
- Fix slow start times for large servers by removing a table scan of the `users` table from startup code. ([\#8271](https://github.com/matrix-org/synapse/issues/8271))


Updates to the Docker image
---------------------------

- Fix builds of the Docker image on non-x86 platforms. ([\#8144](https://github.com/matrix-org/synapse/issues/8144))
- Added curl for healthcheck support and readme updates for the change. Contributed by @maquis196. ([\#8147](https://github.com/matrix-org/synapse/issues/8147))


Improved Documentation
----------------------

- Link to matrix-synapse-rest-password-provider in the password provider documentation. ([\#8111](https://github.com/matrix-org/synapse/issues/8111))
- Updated documentation to note that Synapse does not follow `HTTP 308` redirects due to an upstream library not supporting them. Contributed by Ryan Cole. ([\#8120](https://github.com/matrix-org/synapse/issues/8120))
- Explain better what GDPR-erased means when deactivating a user. ([\#8189](https://github.com/matrix-org/synapse/issues/8189))


Internal Changes
----------------

- Add filter `name` to the `/users` admin API, which filters by user ID or displayname. Contributed by Awesome Technologies Innovationslabor GmbH. ([\#7377](https://github.com/matrix-org/synapse/issues/7377), [\#8163](https://github.com/matrix-org/synapse/issues/8163))
- Reduce run times of some unit tests by advancing the reactor a fewer number of times. ([\#7757](https://github.com/matrix-org/synapse/issues/7757))
- Don't fail `/submit_token` requests on incorrect session ID if `request_token_inhibit_3pid_errors` is turned on. ([\#7991](https://github.com/matrix-org/synapse/issues/7991))
- Convert various parts of the codebase to async/await. ([\#8071](https://github.com/matrix-org/synapse/issues/8071), [\#8072](https://github.com/matrix-org/synapse/issues/8072), [\#8074](https://github.com/matrix-org/synapse/issues/8074), [\#8075](https://github.com/matrix-org/synapse/issues/8075), [\#8076](https://github.com/matrix-org/synapse/issues/8076), [\#8087](https://github.com/matrix-org/synapse/issues/8087), [\#8100](https://github.com/matrix-org/synapse/issues/8100), [\#8119](https://github.com/matrix-org/synapse/issues/8119), [\#8121](https://github.com/matrix-org/synapse/issues/8121), [\#8133](https://github.com/matrix-org/synapse/issues/8133), [\#8156](https://github.com/matrix-org/synapse/issues/8156), [\#8162](https://github.com/matrix-org/synapse/issues/8162), [\#8166](https://github.com/matrix-org/synapse/issues/8166), [\#8168](https://github.com/matrix-org/synapse/issues/8168), [\#8173](https://github.com/matrix-org/synapse/issues/8173), [\#8191](https://github.com/matrix-org/synapse/issues/8191), [\#8192](https://github.com/matrix-org/synapse/issues/8192), [\#8193](https://github.com/matrix-org/synapse/issues/8193), [\#8194](https://github.com/matrix-org/synapse/issues/8194), [\#8195](https://github.com/matrix-org/synapse/issues/8195), [\#8197](https://github.com/matrix-org/synapse/issues/8197), [\#8199](https://github.com/matrix-org/synapse/issues/8199), [\#8200](https://github.com/matrix-org/synapse/issues/8200), [\#8201](https://github.com/matrix-org/synapse/issues/8201), [\#8202](https://github.com/matrix-org/synapse/issues/8202), [\#8207](https://github.com/matrix-org/synapse/issues/8207), [\#8213](https://github.com/matrix-org/synapse/issues/8213), [\#8214](https://github.com/matrix-org/synapse/issues/8214))
- Remove some unused database functions. ([\#8085](https://github.com/matrix-org/synapse/issues/8085))
- Add type hints to various parts of the codebase. ([\#8090](https://github.com/matrix-org/synapse/issues/8090), [\#8127](https://github.com/matrix-org/synapse/issues/8127), [\#8187](https://github.com/matrix-org/synapse/issues/8187), [\#8241](https://github.com/matrix-org/synapse/issues/8241), [\#8140](https://github.com/matrix-org/synapse/issues/8140), [\#8183](https://github.com/matrix-org/synapse/issues/8183), [\#8232](https://github.com/matrix-org/synapse/issues/8232), [\#8235](https://github.com/matrix-org/synapse/issues/8235), [\#8237](https://github.com/matrix-org/synapse/issues/8237), [\#8244](https://github.com/matrix-org/synapse/issues/8244))
- Return the previous stream token if a non-member event is a duplicate. ([\#8093](https://github.com/matrix-org/synapse/issues/8093), [\#8112](https://github.com/matrix-org/synapse/issues/8112))
- Separate `get_current_token` into two since there are two different use cases for it. ([\#8113](https://github.com/matrix-org/synapse/issues/8113))
- Remove `ChainedIdGenerator`. ([\#8123](https://github.com/matrix-org/synapse/issues/8123))
- Reduce the amount of whitespace in JSON stored and sent in responses. ([\#8124](https://github.com/matrix-org/synapse/issues/8124))
- Update the test federation client to handle streaming responses. ([\#8130](https://github.com/matrix-org/synapse/issues/8130))
- Micro-optimisations to `get_auth_chain_ids`. ([\#8132](https://github.com/matrix-org/synapse/issues/8132))
- Refactor `StreamIdGenerator` and `MultiWriterIdGenerator` to have the same interface. ([\#8161](https://github.com/matrix-org/synapse/issues/8161))
- Add functions to `MultiWriterIdGen` used by events stream. ([\#8164](https://github.com/matrix-org/synapse/issues/8164), [\#8179](https://github.com/matrix-org/synapse/issues/8179))
- Fix tests that were broken due to the merge of 1.19.1. ([\#8167](https://github.com/matrix-org/synapse/issues/8167))
- Make `SlavedIdTracker.advance` have the same interface as `MultiWriterIDGenerator`. ([\#8171](https://github.com/matrix-org/synapse/issues/8171))
- Remove unused `is_guest` parameter from, and add safeguard to, `MessageHandler.get_room_data`. ([\#8174](https://github.com/matrix-org/synapse/issues/8174), [\#8181](https://github.com/matrix-org/synapse/issues/8181))
- Standardize the mypy configuration. ([\#8175](https://github.com/matrix-org/synapse/issues/8175))
- Refactor some of `LoginRestServlet`'s helper methods, and move them to `AuthHandler` for easier reuse. ([\#8182](https://github.com/matrix-org/synapse/issues/8182))
- Fix `wait_for_stream_position` to allow multiple waiters on same stream ID. ([\#8196](https://github.com/matrix-org/synapse/issues/8196))
- Make `MultiWriterIDGenerator` work for streams that use negative values. ([\#8203](https://github.com/matrix-org/synapse/issues/8203))
- Refactor queries for device keys and cross-signatures. ([\#8204](https://github.com/matrix-org/synapse/issues/8204), [\#8205](https://github.com/matrix-org/synapse/issues/8205), [\#8222](https://github.com/matrix-org/synapse/issues/8222), [\#8224](https://github.com/matrix-org/synapse/issues/8224), [\#8225](https://github.com/matrix-org/synapse/issues/8225), [\#8231](https://github.com/matrix-org/synapse/issues/8231), [\#8233](https://github.com/matrix-org/synapse/issues/8233), [\#8234](https://github.com/matrix-org/synapse/issues/8234))
- Fix type hints for functions decorated with `@cached`. ([\#8240](https://github.com/matrix-org/synapse/issues/8240))
- Remove obsolete `order` field from federation send queues. ([\#8245](https://github.com/matrix-org/synapse/issues/8245))
- Stop sub-classing from object. ([\#8249](https://github.com/matrix-org/synapse/issues/8249))
- Add more logging to debug slow startup. ([\#8264](https://github.com/matrix-org/synapse/issues/8264))
- Do not attempt to upgrade database schema on worker processes. ([\#8266](https://github.com/matrix-org/synapse/issues/8266), [\#8276](https://github.com/matrix-org/synapse/issues/8276))


Synapse 1.19.1 (2020-08-27)
===========================

No significant changes.


Synapse 1.19.1rc1 (2020-08-25)
==============================

Bugfixes
--------

- Fix a bug introduced in v1.19.0 where appservices with ratelimiting disabled would still be ratelimited when joining rooms. ([\#8139](https://github.com/matrix-org/synapse/issues/8139))
- Fix a bug introduced in v1.19.0 that would cause e.g. profile updates to fail due to incorrect application of rate limits on join requests. ([\#8153](https://github.com/matrix-org/synapse/issues/8153))


Synapse 1.19.0 (2020-08-17)
===========================

No significant changes since 1.19.0rc1.

Removal warning
---------------

As outlined in the [previous release](https://github.com/matrix-org/synapse/releases/tag/v1.18.0),
we are no longer publishing Docker images with the `-py3` tag suffix. On top of that, we have also removed the
`latest-py3` tag. Please see
[the announcement in the upgrade notes for 1.18.0](https://github.com/matrix-org/synapse/blob/develop/docs/upgrade.md#upgrading-to-v1180).


Synapse 1.19.0rc1 (2020-08-13)
==============================

Features
--------

- Add option to allow server admins to join rooms which fail complexity checks. Contributed by @lugino-emeritus. ([\#7902](https://github.com/matrix-org/synapse/issues/7902))
- Add an option to purge room or not with delete room admin endpoint (`POST /_synapse/admin/v1/rooms/<room_id>/delete`). Contributed by @dklimpel. ([\#7964](https://github.com/matrix-org/synapse/issues/7964))
- Add rate limiting to users joining rooms. ([\#8008](https://github.com/matrix-org/synapse/issues/8008))
- Add a `/health` endpoint to every configured HTTP listener that can be used as a health check endpoint by load balancers. ([\#8048](https://github.com/matrix-org/synapse/issues/8048))
- Allow login to be blocked based on the values of SAML attributes. ([\#8052](https://github.com/matrix-org/synapse/issues/8052))
- Allow guest access to the `GET /_matrix/client/r0/rooms/{room_id}/members` endpoint, according to MSC2689. Contributed by Awesome Technologies Innovationslabor GmbH. ([\#7314](https://github.com/matrix-org/synapse/issues/7314))


Bugfixes
--------

- Fix a bug introduced in Synapse v1.7.2 which caused inaccurate membership counts in the room directory. ([\#7977](https://github.com/matrix-org/synapse/issues/7977))
- Fix a long standing bug: 'Duplicate key value violates unique constraint "event_relations_id"' when message retention is configured. ([\#7978](https://github.com/matrix-org/synapse/issues/7978))
- Fix "no create event in auth events" when trying to reject invitation after inviter leaves. Bug introduced in Synapse v1.10.0. ([\#7980](https://github.com/matrix-org/synapse/issues/7980))
- Fix various comments and minor discrepencies in server notices code. ([\#7996](https://github.com/matrix-org/synapse/issues/7996))
- Fix a long standing bug where HTTP HEAD requests resulted in a 400 error. ([\#7999](https://github.com/matrix-org/synapse/issues/7999))
- Fix a long-standing bug which caused two copies of some log lines to be written when synctl was used along with a MemoryHandler logger. ([\#8011](https://github.com/matrix-org/synapse/issues/8011), [\#8012](https://github.com/matrix-org/synapse/issues/8012))


Updates to the Docker image
---------------------------

- We no longer publish Docker images with the `-py3` tag suffix, as [announced in the upgrade notes](https://github.com/matrix-org/synapse/blob/develop/docs/upgrade.md#upgrading-to-v1180). ([\#8056](https://github.com/matrix-org/synapse/issues/8056))


Improved Documentation
----------------------

- Document how to set up a client .well-known file and fix several pieces of outdated documentation. ([\#7899](https://github.com/matrix-org/synapse/issues/7899))
- Improve workers docs. ([\#7990](https://github.com/matrix-org/synapse/issues/7990), [\#8000](https://github.com/matrix-org/synapse/issues/8000))
- Fix typo in `docs/workers.md`. ([\#7992](https://github.com/matrix-org/synapse/issues/7992))
- Add documentation for how to undo a room shutdown. ([\#7998](https://github.com/matrix-org/synapse/issues/7998), [\#8010](https://github.com/matrix-org/synapse/issues/8010))


Internal Changes
----------------

- Reduce the amount of whitespace in JSON stored and sent in responses. Contributed by David Vo. ([\#7372](https://github.com/matrix-org/synapse/issues/7372))
- Switch to the JSON implementation from the standard library and bump the minimum version of the canonicaljson library to 1.2.0. ([\#7936](https://github.com/matrix-org/synapse/issues/7936), [\#7979](https://github.com/matrix-org/synapse/issues/7979))
- Convert various parts of the codebase to async/await. ([\#7947](https://github.com/matrix-org/synapse/issues/7947), [\#7948](https://github.com/matrix-org/synapse/issues/7948), [\#7949](https://github.com/matrix-org/synapse/issues/7949), [\#7951](https://github.com/matrix-org/synapse/issues/7951), [\#7963](https://github.com/matrix-org/synapse/issues/7963), [\#7973](https://github.com/matrix-org/synapse/issues/7973), [\#7975](https://github.com/matrix-org/synapse/issues/7975), [\#7976](https://github.com/matrix-org/synapse/issues/7976), [\#7981](https://github.com/matrix-org/synapse/issues/7981), [\#7987](https://github.com/matrix-org/synapse/issues/7987), [\#7989](https://github.com/matrix-org/synapse/issues/7989), [\#8003](https://github.com/matrix-org/synapse/issues/8003), [\#8014](https://github.com/matrix-org/synapse/issues/8014), [\#8016](https://github.com/matrix-org/synapse/issues/8016), [\#8027](https://github.com/matrix-org/synapse/issues/8027), [\#8031](https://github.com/matrix-org/synapse/issues/8031), [\#8032](https://github.com/matrix-org/synapse/issues/8032), [\#8035](https://github.com/matrix-org/synapse/issues/8035), [\#8042](https://github.com/matrix-org/synapse/issues/8042), [\#8044](https://github.com/matrix-org/synapse/issues/8044), [\#8045](https://github.com/matrix-org/synapse/issues/8045), [\#8061](https://github.com/matrix-org/synapse/issues/8061), [\#8062](https://github.com/matrix-org/synapse/issues/8062), [\#8063](https://github.com/matrix-org/synapse/issues/8063), [\#8066](https://github.com/matrix-org/synapse/issues/8066), [\#8069](https://github.com/matrix-org/synapse/issues/8069), [\#8070](https://github.com/matrix-org/synapse/issues/8070))
- Move some database-related log lines from the default logger to the database/transaction loggers. ([\#7952](https://github.com/matrix-org/synapse/issues/7952))
- Add a script to detect source code files using non-unix line terminators. ([\#7965](https://github.com/matrix-org/synapse/issues/7965), [\#7970](https://github.com/matrix-org/synapse/issues/7970))
- Log the SAML session ID during creation. ([\#7971](https://github.com/matrix-org/synapse/issues/7971))
- Implement new experimental push rules for some users. ([\#7997](https://github.com/matrix-org/synapse/issues/7997))
- Remove redundant and unreliable signature check for v1 Identity Service lookup responses. ([\#8001](https://github.com/matrix-org/synapse/issues/8001))
- Improve the performance of the register endpoint. ([\#8009](https://github.com/matrix-org/synapse/issues/8009))
- Reduce less useful output in the newsfragment CI step. Add a link to the changelog section of the contributing guide on error. ([\#8024](https://github.com/matrix-org/synapse/issues/8024))
- Rename storage layer objects to be more sensible. ([\#8033](https://github.com/matrix-org/synapse/issues/8033))
- Change the default log config to reduce disk I/O and storage for new servers. ([\#8040](https://github.com/matrix-org/synapse/issues/8040))
- Add an assertion on `prev_events` in `create_new_client_event`. ([\#8041](https://github.com/matrix-org/synapse/issues/8041))
- Add a comment to `ServerContextFactory` about the use of `SSLv23_METHOD`. ([\#8043](https://github.com/matrix-org/synapse/issues/8043))
- Log `OPTIONS` requests at `DEBUG` rather than `INFO` level to reduce amount logged at `INFO`. ([\#8049](https://github.com/matrix-org/synapse/issues/8049))
- Reduce amount of outbound request logging at `INFO` level. ([\#8050](https://github.com/matrix-org/synapse/issues/8050))
- It is no longer necessary to explicitly define `filters` in the logging configuration. (Continuing to do so is redundant but harmless.) ([\#8051](https://github.com/matrix-org/synapse/issues/8051))
- Add and improve type hints. ([\#8058](https://github.com/matrix-org/synapse/issues/8058), [\#8064](https://github.com/matrix-org/synapse/issues/8064), [\#8060](https://github.com/matrix-org/synapse/issues/8060), [\#8067](https://github.com/matrix-org/synapse/issues/8067))


Synapse 1.18.0 (2020-07-30)
===========================

Deprecation Warnings
--------------------

### Docker Tags with `-py3` Suffix

From 10th August 2020, we will no longer publish Docker images with the `-py3` tag suffix. The images tagged with the `-py3` suffix have been identical to the non-suffixed tags since release 0.99.0, and the suffix is obsolete.

On 10th August, we will remove the `latest-py3` tag. Existing per-release tags (such as `v1.18.0-py3`) will not be removed, but no new `-py3` tags will be added.

Scripts relying on the `-py3` suffix will need to be updated.


### TCP-based Replication

When setting up worker processes, we now recommend the use of a Redis server for replication. The old direct TCP connection method is deprecated and will be removed in a future release. See [docs/workers.md](https://github.com/matrix-org/synapse/blob/release-v1.18.0/docs/workers.md) for more details.


Improved Documentation
----------------------

- Update worker docs with latest enhancements. ([\#7969](https://github.com/matrix-org/synapse/issues/7969))


Synapse 1.18.0rc2 (2020-07-28)
==============================

Bugfixes
--------

- Fix an `AssertionError` exception introduced in v1.18.0rc1. ([\#7876](https://github.com/matrix-org/synapse/issues/7876))
- Fix experimental support for moving typing off master when worker is restarted, which is broken in v1.18.0rc1. ([\#7967](https://github.com/matrix-org/synapse/issues/7967))


Internal Changes
----------------

- Further optimise queueing of inbound replication commands. ([\#7876](https://github.com/matrix-org/synapse/issues/7876))


Synapse 1.18.0rc1 (2020-07-27)
==============================

Features
--------

- Include room states on invite events that are sent to application services. Contributed by @Sorunome. ([\#6455](https://github.com/matrix-org/synapse/issues/6455))
- Add delete room admin endpoint (`POST /_synapse/admin/v1/rooms/<room_id>/delete`). Contributed by @dklimpel. ([\#7613](https://github.com/matrix-org/synapse/issues/7613), [\#7953](https://github.com/matrix-org/synapse/issues/7953))
- Add experimental support for running multiple federation sender processes. ([\#7798](https://github.com/matrix-org/synapse/issues/7798))
- Add the option to validate the `iss` and `aud` claims for JWT logins. ([\#7827](https://github.com/matrix-org/synapse/issues/7827))
- Add support for handling registration requests across multiple client reader workers. ([\#7830](https://github.com/matrix-org/synapse/issues/7830))
- Add an admin API to list the users in a room. Contributed by Awesome Technologies Innovationslabor GmbH. ([\#7842](https://github.com/matrix-org/synapse/issues/7842))
- Allow email subjects to be customised through Synapse's configuration. ([\#7846](https://github.com/matrix-org/synapse/issues/7846))
- Add the ability to re-activate an account from the admin API. ([\#7847](https://github.com/matrix-org/synapse/issues/7847), [\#7908](https://github.com/matrix-org/synapse/issues/7908))
- Add experimental support for running multiple pusher workers. ([\#7855](https://github.com/matrix-org/synapse/issues/7855))
- Add experimental support for moving typing off master. ([\#7869](https://github.com/matrix-org/synapse/issues/7869), [\#7959](https://github.com/matrix-org/synapse/issues/7959))
- Report CPU metrics to prometheus for time spent processing replication commands. ([\#7879](https://github.com/matrix-org/synapse/issues/7879))
- Support oEmbed for media previews. ([\#7920](https://github.com/matrix-org/synapse/issues/7920))
- Abort federation requests where the client disconnects before the ratelimiter expires. ([\#7930](https://github.com/matrix-org/synapse/issues/7930))
- Cache responses to `/_matrix/federation/v1/state_ids` to reduce duplicated work. ([\#7931](https://github.com/matrix-org/synapse/issues/7931))


Bugfixes
--------

- Fix detection of out of sync remote device lists when receiving events from remote users. ([\#7815](https://github.com/matrix-org/synapse/issues/7815))
- Fix bug where Synapse fails to process an incoming event over federation if the server is missing too much of the event's auth chain. ([\#7817](https://github.com/matrix-org/synapse/issues/7817))
- Fix a bug causing Synapse to misinterpret the value `off` for `encryption_enabled_by_default_for_room_type` in its configuration file(s) if that value isn't surrounded by quotes. This bug was introduced in v1.16.0. ([\#7822](https://github.com/matrix-org/synapse/issues/7822))
- Fix bug where we did not always pass in `app_name` or `server_name` to email templates, including e.g. for registration emails. ([\#7829](https://github.com/matrix-org/synapse/issues/7829))
- Errors which occur while using the non-standard JWT login now return the proper error: `403 Forbidden` with an error code of `M_FORBIDDEN`. ([\#7844](https://github.com/matrix-org/synapse/issues/7844))
- Fix "AttributeError: 'str' object has no attribute 'get'" error message when applying per-room message retention policies. The bug was introduced in Synapse 1.7.0. ([\#7850](https://github.com/matrix-org/synapse/issues/7850))
- Fix a bug introduced in Synapse 1.10.0 which could cause a "no create event in auth events" error during room creation. ([\#7854](https://github.com/matrix-org/synapse/issues/7854))
- Fix a bug which allowed empty rooms to be rejoined over federation. ([\#7859](https://github.com/matrix-org/synapse/issues/7859))
- Fix 'Unable to find a suitable guest user ID' error when using multiple client_reader workers. ([\#7866](https://github.com/matrix-org/synapse/issues/7866))
- Fix a long standing bug where the tracing of async functions with opentracing was broken. ([\#7872](https://github.com/matrix-org/synapse/issues/7872), [\#7961](https://github.com/matrix-org/synapse/issues/7961))
- Fix "TypeError in `synapse.notifier`" exceptions. ([\#7880](https://github.com/matrix-org/synapse/issues/7880))
- Fix deprecation warning due to invalid escape sequences. ([\#7895](https://github.com/matrix-org/synapse/issues/7895))


Updates to the Docker image
---------------------------

- Base docker image on Debian Buster rather than Alpine Linux. Contributed by @maquis196. ([\#7839](https://github.com/matrix-org/synapse/issues/7839))


Improved Documentation
----------------------

- Provide instructions on using `register_new_matrix_user` via docker. ([\#7885](https://github.com/matrix-org/synapse/issues/7885))
- Change the sample config postgres user section to use `synapse_user` instead of `synapse` to align with the documentation. ([\#7889](https://github.com/matrix-org/synapse/issues/7889))
- Reorder database paragraphs to promote postgres over sqlite. ([\#7933](https://github.com/matrix-org/synapse/issues/7933))
- Update the dates of ACME v1's end of life in [`ACME.md`](https://github.com/matrix-org/synapse/blob/master/docs/ACME.md). ([\#7934](https://github.com/matrix-org/synapse/issues/7934))


Deprecations and Removals
-------------------------

- Remove unused `synapse_replication_tcp_resource_invalidate_cache` prometheus metric. ([\#7878](https://github.com/matrix-org/synapse/issues/7878))
- Remove Ubuntu Eoan from the list of `.deb` packages that we build as it is now end-of-life. Contributed by @gary-kim. ([\#7888](https://github.com/matrix-org/synapse/issues/7888))


Internal Changes
----------------

- Switch parts of the codebase from `simplejson` to the standard library `json`. ([\#7802](https://github.com/matrix-org/synapse/issues/7802))
- Add type hints to the http server code and remove an unused parameter. ([\#7813](https://github.com/matrix-org/synapse/issues/7813))
- Add type hints to synapse.api.errors module. ([\#7820](https://github.com/matrix-org/synapse/issues/7820))
- Ensure that calls to `json.dumps` are compatible with the standard library json. ([\#7836](https://github.com/matrix-org/synapse/issues/7836))
- Remove redundant `retry_on_integrity_error` wrapper for event persistence code. ([\#7848](https://github.com/matrix-org/synapse/issues/7848))
- Consistently use `db_to_json` to convert from database values to JSON objects. ([\#7849](https://github.com/matrix-org/synapse/issues/7849))
- Convert various parts of the codebase to async/await. ([\#7851](https://github.com/matrix-org/synapse/issues/7851), [\#7860](https://github.com/matrix-org/synapse/issues/7860), [\#7868](https://github.com/matrix-org/synapse/issues/7868), [\#7871](https://github.com/matrix-org/synapse/issues/7871), [\#7873](https://github.com/matrix-org/synapse/issues/7873), [\#7874](https://github.com/matrix-org/synapse/issues/7874), [\#7884](https://github.com/matrix-org/synapse/issues/7884), [\#7912](https://github.com/matrix-org/synapse/issues/7912), [\#7935](https://github.com/matrix-org/synapse/issues/7935), [\#7939](https://github.com/matrix-org/synapse/issues/7939), [\#7942](https://github.com/matrix-org/synapse/issues/7942), [\#7944](https://github.com/matrix-org/synapse/issues/7944))
- Add support for handling registration requests across multiple client reader workers. ([\#7853](https://github.com/matrix-org/synapse/issues/7853))
- Small performance improvement in typing processing. ([\#7856](https://github.com/matrix-org/synapse/issues/7856))
- The default value of `filter_timeline_limit` was changed from -1 (no limit) to 100. ([\#7858](https://github.com/matrix-org/synapse/issues/7858))
- Optimise queueing of inbound replication commands. ([\#7861](https://github.com/matrix-org/synapse/issues/7861))
- Add some type annotations to `HomeServer` and `BaseHandler`. ([\#7870](https://github.com/matrix-org/synapse/issues/7870))
- Clean up `PreserveLoggingContext`. ([\#7877](https://github.com/matrix-org/synapse/issues/7877))
- Change "unknown room version" logging from 'error' to 'warning'. ([\#7881](https://github.com/matrix-org/synapse/issues/7881))
- Stop using `device_max_stream_id` table and just use `device_inbox.stream_id`. ([\#7882](https://github.com/matrix-org/synapse/issues/7882))
- Return an empty body for OPTIONS requests. ([\#7886](https://github.com/matrix-org/synapse/issues/7886))
- Fix typo in generated config file. Contributed by @ThiefMaster. ([\#7890](https://github.com/matrix-org/synapse/issues/7890))
- Import ABC from `collections.abc` for Python 3.10 compatibility. ([\#7892](https://github.com/matrix-org/synapse/issues/7892))
- Remove unused functions `time_function`, `trace_function`, `get_previous_frames`
  and `get_previous_frame` from `synapse.logging.utils` module. ([\#7897](https://github.com/matrix-org/synapse/issues/7897))
- Lint the `contrib/` directory in CI and linting scripts, add `synctl` to the linting script for consistency with CI. ([\#7914](https://github.com/matrix-org/synapse/issues/7914))
- Use Element CSS and logo in notification emails when app name is Element. ([\#7919](https://github.com/matrix-org/synapse/issues/7919))
- Optimisation to /sync handling: skip serializing the response if the client has already disconnected. ([\#7927](https://github.com/matrix-org/synapse/issues/7927))
- When a client disconnects, don't log it as 'Error processing request'. ([\#7928](https://github.com/matrix-org/synapse/issues/7928))
- Add debugging to `/sync` response generation (disabled by default). ([\#7929](https://github.com/matrix-org/synapse/issues/7929))
- Update comments that refer to Deferreds for async functions. ([\#7945](https://github.com/matrix-org/synapse/issues/7945))
- Simplify error handling in federation handler. ([\#7950](https://github.com/matrix-org/synapse/issues/7950))


Synapse 1.17.0 (2020-07-13)
===========================

Synapse 1.17.0 is identical to 1.17.0rc1, with the addition of the fix that was included in 1.16.1.


Synapse 1.16.1 (2020-07-10)
===========================

In some distributions of Synapse 1.16.0, we incorrectly included a database migration which added a new, unused table. This release removes the redundant table.

Bugfixes
--------

- Drop table `local_rejections_stream` which was incorrectly added in Synapse 1.16.0. ([\#7816](https://github.com/matrix-org/synapse/issues/7816), [b1beb3ff5](https://github.com/matrix-org/synapse/commit/b1beb3ff5))


Synapse 1.17.0rc1 (2020-07-09)
==============================

Bugfixes
--------

- Fix inconsistent handling of upper and lower case in email addresses when used as identifiers for login, etc. Contributed by @dklimpel. ([\#7021](https://github.com/matrix-org/synapse/issues/7021))
- Fix "Tried to close a non-active scope!" error messages when opentracing is enabled. ([\#7732](https://github.com/matrix-org/synapse/issues/7732))
- Fix incorrect error message when database CTYPE was set incorrectly. ([\#7760](https://github.com/matrix-org/synapse/issues/7760))
- Fix to not ignore `set_tweak` actions in Push Rules that have no `value`, as permitted by the specification. ([\#7766](https://github.com/matrix-org/synapse/issues/7766))
- Fix synctl to handle empty config files correctly. Contributed by @kotovalexarian. ([\#7779](https://github.com/matrix-org/synapse/issues/7779))
- Fixes a long standing bug in worker mode where worker information was saved in the devices table instead of the original IP address and user agent. ([\#7797](https://github.com/matrix-org/synapse/issues/7797))
- Fix 'stuck invites' which happen when we are unable to reject a room invite received over federation. ([\#7804](https://github.com/matrix-org/synapse/issues/7804), [\#7809](https://github.com/matrix-org/synapse/issues/7809), [\#7810](https://github.com/matrix-org/synapse/issues/7810))


Updates to the Docker image
---------------------------

- Include libwebp in the Docker file to properly handle webp image uploads. ([\#7791](https://github.com/matrix-org/synapse/issues/7791))


Improved Documentation
----------------------

- Improve the documentation of the non-standard JSON web token login type. ([\#7776](https://github.com/matrix-org/synapse/issues/7776))
- Update doc links for caddy. Contributed by Nicolai Søborg. ([\#7789](https://github.com/matrix-org/synapse/issues/7789))


Internal Changes
----------------

- Refactor getting replication updates from database. ([\#7740](https://github.com/matrix-org/synapse/issues/7740))
- Send push notifications with a high or low priority depending upon whether they may generate user-observable effects. ([\#7765](https://github.com/matrix-org/synapse/issues/7765))
- Use symbolic names for replication stream names. ([\#7768](https://github.com/matrix-org/synapse/issues/7768))
- Add early returns to `_check_for_soft_fail`. ([\#7769](https://github.com/matrix-org/synapse/issues/7769))
- Fix up `synapse.handlers.federation` to pass mypy. ([\#7770](https://github.com/matrix-org/synapse/issues/7770))
- Convert the appserver handler to async/await. ([\#7775](https://github.com/matrix-org/synapse/issues/7775))
- Allow to use higher versions of prometheus_client <0.9.0 which are expected to introduce no breaking changes. Contributed by Oliver Kurz. ([\#7780](https://github.com/matrix-org/synapse/issues/7780))
- Update linting scripts and codebase to be compatible with `isort` v5. ([\#7786](https://github.com/matrix-org/synapse/issues/7786))
- Stop populating unused table `local_invites`. ([\#7793](https://github.com/matrix-org/synapse/issues/7793))
- Ensure that strings (not bytes) are passed into JSON serialization. ([\#7799](https://github.com/matrix-org/synapse/issues/7799))
- Switch from simplejson to the standard library json. ([\#7800](https://github.com/matrix-org/synapse/issues/7800))
- Add `signing_key` property to `HomeServer` to save code duplication. ([\#7805](https://github.com/matrix-org/synapse/issues/7805))
- Improve stacktraces from exceptions in background processes. ([\#7808](https://github.com/matrix-org/synapse/issues/7808))
- Fix various spelling errors in comments and log lines. ([\#7811](https://github.com/matrix-org/synapse/issues/7811))


Synapse 1.16.0 (2020-07-08)
===========================

No significant changes since 1.16.0rc2.

Note that this release deprecates the `m.login.jwt` login method, renaming it
to `org.matrix.login.jwt`, as `m.login.jwt` is not part of the Matrix spec.
Otherwise the behaviour is identical. Synapse will accept both names for now,
but this may change in a future release.

Synapse 1.16.0rc2 (2020-07-02)
==============================

Synapse 1.16.0rc2 includes the security fixes released with Synapse 1.15.2.
Please see [below](#synapse-1152-2020-07-02) for more details.

Improved Documentation
----------------------

- Update postgres image in example `docker-compose.yaml` to tag `12-alpine`. ([\#7696](https://github.com/matrix-org/synapse/issues/7696))


Internal Changes
----------------

- Add some metrics for inbound and outbound federation latencies: `synapse_federation_server_pdu_process_time` and `synapse_event_processing_lag_by_event`. ([\#7771](https://github.com/matrix-org/synapse/issues/7771))


Synapse 1.15.2 (2020-07-02)
===========================

Due to the two security issues highlighted below, server administrators are
encouraged to update Synapse. We are not aware of these vulnerabilities being
exploited in the wild.

Security advisory
-----------------

* A malicious homeserver could force Synapse to reset the state in a room to a
  small subset of the correct state. This affects all Synapse deployments which
  federate with untrusted servers. ([96e9afe6](https://github.com/matrix-org/synapse/commit/96e9afe62500310977dc3cbc99a8d16d3d2fa15c))
* HTML pages served via Synapse were vulnerable to clickjacking attacks. This
  predominantly affects homeservers with single-sign-on enabled, but all server
  administrators are encouraged to upgrade. ([ea26e9a9](https://github.com/matrix-org/synapse/commit/ea26e9a98b0541fc886a1cb826a38352b7599dbe))

  This was reported by [Quentin Gliech](https://sandhose.fr/).


Synapse 1.16.0rc1 (2020-07-01)
==============================

Features
--------

- Add an option to enable encryption by default for new rooms. ([\#7639](https://github.com/matrix-org/synapse/issues/7639))
- Add support for running multiple media repository workers. See [docs/workers.md](https://github.com/matrix-org/synapse/blob/release-v1.16.0/docs/workers.md) for instructions. ([\#7706](https://github.com/matrix-org/synapse/issues/7706))
- Media can now be marked as safe from quarantined. ([\#7718](https://github.com/matrix-org/synapse/issues/7718))
- Expand the configuration options for auto-join rooms. ([\#7763](https://github.com/matrix-org/synapse/issues/7763))


Bugfixes
--------

- Remove `user_id` from the response to `GET /_matrix/client/r0/presence/{userId}/status` to match the specification. ([\#7606](https://github.com/matrix-org/synapse/issues/7606))
- In worker mode, ensure that replicated data has not already been received. ([\#7648](https://github.com/matrix-org/synapse/issues/7648))
- Fix intermittent exception during startup, introduced in Synapse 1.14.0. ([\#7663](https://github.com/matrix-org/synapse/issues/7663))
- Include a user-agent for federation and well-known requests. ([\#7677](https://github.com/matrix-org/synapse/issues/7677))
- Accept the proper field (`phone`) for the `m.id.phone` identifier type. The legacy field of `number` is still accepted as a fallback. Bug introduced in v0.20.0. ([\#7687](https://github.com/matrix-org/synapse/issues/7687))
- Fix "Starting db txn 'get_completed_ui_auth_stages' from sentinel context" warning. The bug was introduced in 1.13.0. ([\#7688](https://github.com/matrix-org/synapse/issues/7688))
- Compare the URI and method during user interactive authentication (instead of the URI twice). Bug introduced in 1.13.0. ([\#7689](https://github.com/matrix-org/synapse/issues/7689))
- Fix a long standing bug where the response to the `GET room_keys/version` endpoint had the incorrect type for the `etag` field. ([\#7691](https://github.com/matrix-org/synapse/issues/7691))
- Fix logged error during device resync in opentracing. Broke in v1.14.0. ([\#7698](https://github.com/matrix-org/synapse/issues/7698))
- Do not break push rule evaluation when receiving an event with a non-string body. This is a long-standing bug. ([\#7701](https://github.com/matrix-org/synapse/issues/7701))
- Fixs a long standing bug which resulted in an exception: "TypeError: argument of type 'ObservableDeferred' is not iterable". ([\#7708](https://github.com/matrix-org/synapse/issues/7708))
- The `synapse_port_db` script no longer fails when the `ui_auth_sessions` table is non-empty. This bug has existed since v1.13.0. ([\#7711](https://github.com/matrix-org/synapse/issues/7711))
- Synapse will now fetch media from the proper specified URL (using the r0 prefix instead of the unspecified v1). ([\#7714](https://github.com/matrix-org/synapse/issues/7714))
- Fix the tables ignored by `synapse_port_db` to be in sync the current database schema. ([\#7717](https://github.com/matrix-org/synapse/issues/7717))
- Fix missing `Content-Length` on HTTP responses from the metrics handler. ([\#7730](https://github.com/matrix-org/synapse/issues/7730))
- Fix large state resolutions from stalling Synapse for seconds at a time. ([\#7735](https://github.com/matrix-org/synapse/issues/7735), [\#7746](https://github.com/matrix-org/synapse/issues/7746))


Improved Documentation
----------------------

- Spelling correction in sample_config.yaml. ([\#7652](https://github.com/matrix-org/synapse/issues/7652))
- Added instructions for how to use Keycloak via OpenID Connect to authenticate with Synapse. ([\#7659](https://github.com/matrix-org/synapse/issues/7659))
- Corrected misspelling of PostgreSQL. ([\#7724](https://github.com/matrix-org/synapse/issues/7724))


Deprecations and Removals
-------------------------

- Deprecate `m.login.jwt` login method in favour of `org.matrix.login.jwt`, as `m.login.jwt` is not part of the Matrix spec. ([\#7675](https://github.com/matrix-org/synapse/issues/7675))


Internal Changes
----------------

- Refactor getting replication updates from database. ([\#7636](https://github.com/matrix-org/synapse/issues/7636))
- Clean-up the login fallback code. ([\#7657](https://github.com/matrix-org/synapse/issues/7657))
- Increase the default SAML session expiry time to 15 minutes. ([\#7664](https://github.com/matrix-org/synapse/issues/7664))
- Convert the device message and pagination handlers to async/await. ([\#7678](https://github.com/matrix-org/synapse/issues/7678))
- Convert typing handler to async/await. ([\#7679](https://github.com/matrix-org/synapse/issues/7679))
- Require `parameterized` package version to be at least 0.7.0. ([\#7680](https://github.com/matrix-org/synapse/issues/7680))
- Refactor handling of `listeners` configuration settings. ([\#7681](https://github.com/matrix-org/synapse/issues/7681))
- Replace uses of `six.iterkeys`/`iteritems`/`itervalues` with `keys()`/`items()`/`values()`. ([\#7692](https://github.com/matrix-org/synapse/issues/7692))
- Add support for using `rust-python-jaeger-reporter` library to reduce jaeger tracing overhead. ([\#7697](https://github.com/matrix-org/synapse/issues/7697))
- Make Tox actions work on Debian 10. ([\#7703](https://github.com/matrix-org/synapse/issues/7703))
- Replace all remaining uses of `six` with native Python 3 equivalents. Contributed by @ilmari. ([\#7704](https://github.com/matrix-org/synapse/issues/7704))
- Fix broken link in sample config. ([\#7712](https://github.com/matrix-org/synapse/issues/7712))
- Speed up state res v2 across large state differences. ([\#7725](https://github.com/matrix-org/synapse/issues/7725))
- Convert directory handler to async/await. ([\#7727](https://github.com/matrix-org/synapse/issues/7727))
- Move `flake8` to the end of `scripts-dev/lint.sh` as it takes the longest and could cause the script to exit early. ([\#7738](https://github.com/matrix-org/synapse/issues/7738))
- Explain the "test" conditional requirement for dependencies is not all of the modules necessary to run the unit tests. ([\#7751](https://github.com/matrix-org/synapse/issues/7751))
- Add some metrics for inbound and outbound federation latencies: `synapse_federation_server_pdu_process_time` and `synapse_event_processing_lag_by_event`. ([\#7755](https://github.com/matrix-org/synapse/issues/7755))


Synapse 1.15.1 (2020-06-16)
===========================

Bugfixes
--------

- Fix a bug introduced in v1.15.0 that would crash Synapse on start when using certain password auth providers. ([\#7684](https://github.com/matrix-org/synapse/issues/7684))
- Fix a bug introduced in v1.15.0 which meant that some 3PID management endpoints were not accessible on the correct URL. ([\#7685](https://github.com/matrix-org/synapse/issues/7685))


Synapse 1.15.0 (2020-06-11)
===========================

No significant changes.


Synapse 1.15.0rc1 (2020-06-09)
==============================

Features
--------

- Advertise support for Client-Server API r0.6.0 and remove related unstable feature flags. ([\#6585](https://github.com/matrix-org/synapse/issues/6585))
- Add an option to disable autojoining rooms for guest accounts. ([\#6637](https://github.com/matrix-org/synapse/issues/6637))
- For SAML authentication, add the ability to pass email addresses to be added to new users' accounts via SAML attributes. Contributed by Christopher Cooper. ([\#7385](https://github.com/matrix-org/synapse/issues/7385))
- Add admin APIs to allow server admins to manage users' devices. Contributed by @dklimpel. ([\#7481](https://github.com/matrix-org/synapse/issues/7481))
- Add support for generating thumbnails for WebP images. Previously, users would see an empty box instead of preview image. Contributed by @WGH-. ([\#7586](https://github.com/matrix-org/synapse/issues/7586))
- Support the standardized `m.login.sso` user-interactive authentication flow. ([\#7630](https://github.com/matrix-org/synapse/issues/7630))


Bugfixes
--------

- Allow new users to be registered via the admin API even if the monthly active user limit has been reached. Contributed by @dklimpel. ([\#7263](https://github.com/matrix-org/synapse/issues/7263))
- Fix email notifications not being enabled for new users when created via the Admin API. ([\#7267](https://github.com/matrix-org/synapse/issues/7267))
- Fix str placeholders in an instance of `PrepareDatabaseException`. Introduced in Synapse v1.8.0. ([\#7575](https://github.com/matrix-org/synapse/issues/7575))
- Fix a bug in automatic user creation during first time login with `m.login.jwt`. Regression in v1.6.0. Contributed by @olof. ([\#7585](https://github.com/matrix-org/synapse/issues/7585))
- Fix a bug causing the cross-signing keys to be ignored when resyncing a device list. ([\#7594](https://github.com/matrix-org/synapse/issues/7594))
- Fix metrics failing when there is a large number of active background processes. ([\#7597](https://github.com/matrix-org/synapse/issues/7597))
- Fix bug where returning rooms for a group would fail if it included a room that the server was not in. ([\#7599](https://github.com/matrix-org/synapse/issues/7599))
- Fix duplicate key violation when persisting read markers. ([\#7607](https://github.com/matrix-org/synapse/issues/7607))
- Prevent an entire iteration of the device list resync loop from failing if one server responds with a malformed result. ([\#7609](https://github.com/matrix-org/synapse/issues/7609))
- Fix exceptions when fetching events from a remote host fails. ([\#7622](https://github.com/matrix-org/synapse/issues/7622))
- Make `synctl restart` start synapse if it wasn't running. ([\#7624](https://github.com/matrix-org/synapse/issues/7624))
- Pass device information through to the login endpoint when using the login fallback. ([\#7629](https://github.com/matrix-org/synapse/issues/7629))
- Advertise the `m.login.token` login flow when OpenID Connect is enabled. ([\#7631](https://github.com/matrix-org/synapse/issues/7631))
- Fix bug in account data replication stream. ([\#7656](https://github.com/matrix-org/synapse/issues/7656))


Improved Documentation
----------------------

- Update the OpenBSD installation instructions. ([\#7587](https://github.com/matrix-org/synapse/issues/7587))
- Advertise Python 3.8 support in `setup.py`. ([\#7602](https://github.com/matrix-org/synapse/issues/7602))
- Add a link to `#synapse:matrix.org` in the troubleshooting section of the README. ([\#7603](https://github.com/matrix-org/synapse/issues/7603))
- Clarifications to the admin api documentation. ([\#7647](https://github.com/matrix-org/synapse/issues/7647))


Internal Changes
----------------

- Convert the identity handler to async/await. ([\#7561](https://github.com/matrix-org/synapse/issues/7561))
- Improve query performance for fetching state from a PostgreSQL database. Contributed by @ilmari. ([\#7567](https://github.com/matrix-org/synapse/issues/7567))
- Speed up processing of federation stream RDATA rows. ([\#7584](https://github.com/matrix-org/synapse/issues/7584))
- Add comment to systemd example to show postgresql dependency. ([\#7591](https://github.com/matrix-org/synapse/issues/7591))
- Refactor `Ratelimiter` to limit the amount of expensive config value accesses. ([\#7595](https://github.com/matrix-org/synapse/issues/7595))
- Convert groups handlers to async/await. ([\#7600](https://github.com/matrix-org/synapse/issues/7600))
- Clean up exception handling in `SAML2ResponseResource`. ([\#7614](https://github.com/matrix-org/synapse/issues/7614))
- Check that all asynchronous tasks succeed and general cleanup of `MonthlyActiveUsersTestCase` and `TestMauLimit`. ([\#7619](https://github.com/matrix-org/synapse/issues/7619))
- Convert `get_user_id_by_threepid` to async/await. ([\#7620](https://github.com/matrix-org/synapse/issues/7620))
- Switch to upstream `dh-virtualenv` rather than our fork for Debian package builds. ([\#7621](https://github.com/matrix-org/synapse/issues/7621))
- Update CI scripts to check the number in the newsfile fragment. ([\#7623](https://github.com/matrix-org/synapse/issues/7623))
- Check if the localpart of a Matrix ID is reserved for guest users earlier in the registration flow, as well as when responding to requests to `/register/available`. ([\#7625](https://github.com/matrix-org/synapse/issues/7625))
- Minor cleanups to OpenID Connect integration. ([\#7628](https://github.com/matrix-org/synapse/issues/7628))
- Attempt to fix flaky test: `PhoneHomeStatsTestCase.test_performance_100`. ([\#7634](https://github.com/matrix-org/synapse/issues/7634))
- Fix typos of `m.olm.curve25519-aes-sha2` and `m.megolm.v1.aes-sha2` in comments, test files. ([\#7637](https://github.com/matrix-org/synapse/issues/7637))
- Convert user directory, state deltas, and stats handlers to async/await. ([\#7640](https://github.com/matrix-org/synapse/issues/7640))
- Remove some unused constants. ([\#7644](https://github.com/matrix-org/synapse/issues/7644))
- Fix type information on `assert_*_is_admin` methods. ([\#7645](https://github.com/matrix-org/synapse/issues/7645))
- Convert registration handler to async/await. ([\#7649](https://github.com/matrix-org/synapse/issues/7649))


Synapse 1.14.0 (2020-05-28)
===========================

No significant changes.


Synapse 1.14.0rc2 (2020-05-27)
==============================

Bugfixes
--------

- Fix cache config to not apply cache factor to event cache. Regression in v1.14.0rc1. ([\#7578](https://github.com/matrix-org/synapse/issues/7578))
- Fix bug where `ReplicationStreamer` was not always started when replication was enabled. Bug introduced in v1.14.0rc1. ([\#7579](https://github.com/matrix-org/synapse/issues/7579))
- Fix specifying individual cache factors for caches with special characters in their name. Regression in v1.14.0rc1. ([\#7580](https://github.com/matrix-org/synapse/issues/7580))


Improved Documentation
----------------------

- Fix the OIDC `client_auth_method` value in the sample config. ([\#7581](https://github.com/matrix-org/synapse/issues/7581))


Synapse 1.14.0rc1 (2020-05-26)
==============================

Features
--------

- Synapse's cache factor can now be configured in `homeserver.yaml` by the `caches.global_factor` setting. Additionally, `caches.per_cache_factors` controls the cache factors for individual caches. ([\#6391](https://github.com/matrix-org/synapse/issues/6391))
- Add OpenID Connect login/registration support. Contributed by Quentin Gliech, on behalf of [les Connecteurs](https://connecteu.rs). ([\#7256](https://github.com/matrix-org/synapse/issues/7256), [\#7457](https://github.com/matrix-org/synapse/issues/7457))
- Add room details admin endpoint. Contributed by Awesome Technologies Innovationslabor GmbH. ([\#7317](https://github.com/matrix-org/synapse/issues/7317))
- Allow for using more than one spam checker module at once. ([\#7435](https://github.com/matrix-org/synapse/issues/7435))
- Add additional authentication checks for `m.room.power_levels` event per [MSC2209](https://github.com/matrix-org/matrix-doc/pull/2209). ([\#7502](https://github.com/matrix-org/synapse/issues/7502))
- Implement room version 6 per [MSC2240](https://github.com/matrix-org/matrix-doc/pull/2240). ([\#7506](https://github.com/matrix-org/synapse/issues/7506))
- Add highly experimental option to move event persistence off master. ([\#7281](https://github.com/matrix-org/synapse/issues/7281), [\#7374](https://github.com/matrix-org/synapse/issues/7374), [\#7436](https://github.com/matrix-org/synapse/issues/7436), [\#7440](https://github.com/matrix-org/synapse/issues/7440), [\#7475](https://github.com/matrix-org/synapse/issues/7475), [\#7490](https://github.com/matrix-org/synapse/issues/7490), [\#7491](https://github.com/matrix-org/synapse/issues/7491), [\#7492](https://github.com/matrix-org/synapse/issues/7492), [\#7493](https://github.com/matrix-org/synapse/issues/7493), [\#7495](https://github.com/matrix-org/synapse/issues/7495), [\#7515](https://github.com/matrix-org/synapse/issues/7515), [\#7516](https://github.com/matrix-org/synapse/issues/7516), [\#7517](https://github.com/matrix-org/synapse/issues/7517), [\#7542](https://github.com/matrix-org/synapse/issues/7542))


Bugfixes
--------

- Fix a bug where event updates might not be sent over replication to worker processes after the stream falls behind. ([\#7384](https://github.com/matrix-org/synapse/issues/7384))
- Allow expired user accounts to log out their device sessions. ([\#7443](https://github.com/matrix-org/synapse/issues/7443))
- Fix a bug that would cause Synapse not to resync out-of-sync device lists. ([\#7453](https://github.com/matrix-org/synapse/issues/7453))
- Prevent rooms with 0 members or with invalid version strings from breaking group queries. ([\#7465](https://github.com/matrix-org/synapse/issues/7465))
- Workaround for an upstream Twisted bug that caused Synapse to become unresponsive after startup. ([\#7473](https://github.com/matrix-org/synapse/issues/7473))
- Fix Redis reconnection logic that can result in missed updates over replication if master reconnects to Redis without restarting. ([\#7482](https://github.com/matrix-org/synapse/issues/7482))
- When sending `m.room.member` events, omit `displayname` and `avatar_url` if they aren't set instead of setting them to `null`. Contributed by Aaron Raimist. ([\#7497](https://github.com/matrix-org/synapse/issues/7497))
- Fix incorrect `method` label on `synapse_http_matrixfederationclient_{requests,responses}` prometheus metrics. ([\#7503](https://github.com/matrix-org/synapse/issues/7503))
- Ignore incoming presence events from other homeservers if presence is disabled locally. ([\#7508](https://github.com/matrix-org/synapse/issues/7508))
- Fix a long-standing bug that broke the update remote profile background process. ([\#7511](https://github.com/matrix-org/synapse/issues/7511))
- Hash passwords as early as possible during password reset. ([\#7538](https://github.com/matrix-org/synapse/issues/7538))
- Fix bug where a local user leaving a room could fail under rare circumstances. ([\#7548](https://github.com/matrix-org/synapse/issues/7548))
- Fix "Missing RelayState parameter" error when using user interactive authentication with SAML for some SAML providers. ([\#7552](https://github.com/matrix-org/synapse/issues/7552))
- Fix exception `'GenericWorkerReplicationHandler' object has no attribute 'send_federation_ack'`, introduced in v1.13.0. ([\#7564](https://github.com/matrix-org/synapse/issues/7564))
- `synctl` now warns if it was unable to stop Synapse and will not attempt to start Synapse if nothing was stopped. Contributed by Romain Bouyé. ([\#6598](https://github.com/matrix-org/synapse/issues/6598))


Updates to the Docker image
---------------------------

- Update docker runtime image to Alpine v3.11. Contributed by @Starbix. ([\#7398](https://github.com/matrix-org/synapse/issues/7398))


Improved Documentation
----------------------

- Update information about mapping providers for SAML and OpenID. ([\#7458](https://github.com/matrix-org/synapse/issues/7458))
- Add additional reverse proxy example for Caddy v2. Contributed by Jeff Peeler. ([\#7463](https://github.com/matrix-org/synapse/issues/7463))
- Fix copy-paste error in `ServerNoticesConfig` docstring. Contributed by @ptman. ([\#7477](https://github.com/matrix-org/synapse/issues/7477))
- Improve the formatting of `reverse_proxy.md`. ([\#7514](https://github.com/matrix-org/synapse/issues/7514))
- Change the systemd worker service to check that the worker config file exists instead of silently failing. Contributed by David Vo. ([\#7528](https://github.com/matrix-org/synapse/issues/7528))
- Minor clarifications to the TURN docs. ([\#7533](https://github.com/matrix-org/synapse/issues/7533))


Internal Changes
----------------

- Add typing annotations in `synapse.federation`. ([\#7382](https://github.com/matrix-org/synapse/issues/7382))
- Convert the room handler to async/await. ([\#7396](https://github.com/matrix-org/synapse/issues/7396))
- Improve performance of `get_e2e_cross_signing_key`. ([\#7428](https://github.com/matrix-org/synapse/issues/7428))
- Improve performance of `mark_as_sent_devices_by_remote`. ([\#7429](https://github.com/matrix-org/synapse/issues/7429), [\#7562](https://github.com/matrix-org/synapse/issues/7562))
- Add type hints to the SAML handler. ([\#7445](https://github.com/matrix-org/synapse/issues/7445))
- Remove storage method `get_hosts_in_room` that is no longer called anywhere. ([\#7448](https://github.com/matrix-org/synapse/issues/7448))
- Fix some typos in the `notice_expiry` templates. ([\#7449](https://github.com/matrix-org/synapse/issues/7449))
- Convert the federation handler to async/await. ([\#7459](https://github.com/matrix-org/synapse/issues/7459))
- Convert the search handler to async/await. ([\#7460](https://github.com/matrix-org/synapse/issues/7460))
- Add type hints to `synapse.event_auth`. ([\#7505](https://github.com/matrix-org/synapse/issues/7505))
- Convert the room member handler to async/await. ([\#7507](https://github.com/matrix-org/synapse/issues/7507))
- Add type hints to room member handler. ([\#7513](https://github.com/matrix-org/synapse/issues/7513))
- Fix typing annotations in `tests.replication`. ([\#7518](https://github.com/matrix-org/synapse/issues/7518))
- Remove some redundant Python 2 support code. ([\#7519](https://github.com/matrix-org/synapse/issues/7519))
- All endpoints now respond with a 200 OK for `OPTIONS` requests. ([\#7534](https://github.com/matrix-org/synapse/issues/7534), [\#7560](https://github.com/matrix-org/synapse/issues/7560))
- Synapse now exports [detailed allocator statistics](https://doc.pypy.org/en/latest/gc_info.html#gc-get-stats) and basic GC timings as Prometheus metrics (`pypy_gc_time_seconds_total` and `pypy_memory_bytes`) when run under PyPy. Contributed by Ivan Shapovalov. ([\#7536](https://github.com/matrix-org/synapse/issues/7536))
- Remove Ubuntu Cosmic and Disco from the list of distributions which we provide `.deb`s for, due to end-of-life. ([\#7539](https://github.com/matrix-org/synapse/issues/7539))
- Make worker processes return a stubbed-out response to `GET /presence` requests. ([\#7545](https://github.com/matrix-org/synapse/issues/7545))
- Optimise some references to `hs.config`. ([\#7546](https://github.com/matrix-org/synapse/issues/7546))
- On upgrade room only send canonical alias once. ([\#7547](https://github.com/matrix-org/synapse/issues/7547))
- Fix some indentation inconsistencies in the sample config. ([\#7550](https://github.com/matrix-org/synapse/issues/7550))
- Include `synapse.http.site` in type checking. ([\#7553](https://github.com/matrix-org/synapse/issues/7553))
- Fix some test code to not mangle stacktraces, to make it easier to debug errors. ([\#7554](https://github.com/matrix-org/synapse/issues/7554))
- Refresh apt cache when building `dh_virtualenv` docker image. ([\#7555](https://github.com/matrix-org/synapse/issues/7555))
- Stop logging some expected HTTP request errors as exceptions. ([\#7556](https://github.com/matrix-org/synapse/issues/7556), [\#7563](https://github.com/matrix-org/synapse/issues/7563))
- Convert sending mail to async/await. ([\#7557](https://github.com/matrix-org/synapse/issues/7557))
- Simplify `reap_monthly_active_users`. ([\#7558](https://github.com/matrix-org/synapse/issues/7558))


Synapse 1.13.0 (2020-05-19)
===========================

This release brings some potential changes necessary for certain
configurations of Synapse:

* If your Synapse is configured to use SSO and have a custom
  `sso_redirect_confirm_template_dir` configuration option set, you will need
  to duplicate the new `sso_auth_confirm.html`, `sso_auth_success.html` and
  `sso_account_deactivated.html` templates into that directory.
* Synapse plugins using the `complete_sso_login` method of
  `synapse.module_api.ModuleApi` should instead switch to the async/await
  version, `complete_sso_login_async`, which includes additional checks. The
  former version is now deprecated.
* A bug was introduced in Synapse 1.4.0 which could cause the room directory
  to be incomplete or empty if Synapse was upgraded directly from v1.2.1 or
  earlier, to versions between v1.4.0 and v1.12.x.

Please review the [upgrade notes](docs/upgrade.md) for more details on these changes
and for general upgrade guidance.


Notice of change to the default `git` branch for Synapse
--------------------------------------------------------

With the release of Synapse 1.13.0, the default `git` branch for Synapse has
changed to `develop`, which is the development tip. This is more consistent with
common practice and modern `git` usage.

The `master` branch, which tracks the latest release, is still available. It is
recommended that developers and distributors who have scripts which run builds
using the default branch of Synapse should therefore consider pinning their
scripts to `master`.


Internal Changes
----------------

- Update the version of dh-virtualenv we use to build debs, and add focal to the list of target distributions. ([\#7526](https://github.com/matrix-org/synapse/issues/7526))


Synapse 1.13.0rc3 (2020-05-18)
==============================

Bugfixes
--------

- Hash passwords as early as possible during registration. ([\#7523](https://github.com/matrix-org/synapse/issues/7523))


Synapse 1.13.0rc2 (2020-05-14)
==============================

Bugfixes
--------

- Fix a long-standing bug which could cause messages not to be sent over federation, when state events with state keys matching user IDs (such as custom user statuses) were received. ([\#7376](https://github.com/matrix-org/synapse/issues/7376))
- Restore compatibility with non-compliant clients during the user interactive authentication process, fixing a problem introduced in v1.13.0rc1. ([\#7483](https://github.com/matrix-org/synapse/issues/7483))

Internal Changes
----------------

- Fix linting errors in new version of Flake8. ([\#7470](https://github.com/matrix-org/synapse/issues/7470))


Synapse 1.13.0rc1 (2020-05-11)
==============================

Features
--------

- Extend the `web_client_location` option to accept an absolute URL to use as a redirect. Adds a warning when running the web client on the same hostname as homeserver. Contributed by Martin Milata. ([\#7006](https://github.com/matrix-org/synapse/issues/7006))
- Set `Referrer-Policy` header to `no-referrer` on media downloads. ([\#7009](https://github.com/matrix-org/synapse/issues/7009))
- Add support for running replication over Redis when using workers. ([\#7040](https://github.com/matrix-org/synapse/issues/7040), [\#7325](https://github.com/matrix-org/synapse/issues/7325), [\#7352](https://github.com/matrix-org/synapse/issues/7352), [\#7401](https://github.com/matrix-org/synapse/issues/7401), [\#7427](https://github.com/matrix-org/synapse/issues/7427), [\#7439](https://github.com/matrix-org/synapse/issues/7439), [\#7446](https://github.com/matrix-org/synapse/issues/7446), [\#7450](https://github.com/matrix-org/synapse/issues/7450), [\#7454](https://github.com/matrix-org/synapse/issues/7454))
- Admin API `POST /_synapse/admin/v1/join/<roomIdOrAlias>` to join users to a room like `auto_join_rooms` for creation of users. ([\#7051](https://github.com/matrix-org/synapse/issues/7051))
- Add options to prevent users from changing their profile or associated 3PIDs. ([\#7096](https://github.com/matrix-org/synapse/issues/7096))
- Support SSO in the user interactive authentication workflow. ([\#7102](https://github.com/matrix-org/synapse/issues/7102), [\#7186](https://github.com/matrix-org/synapse/issues/7186), [\#7279](https://github.com/matrix-org/synapse/issues/7279), [\#7343](https://github.com/matrix-org/synapse/issues/7343))
- Allow server admins to define and enforce a password policy ([MSC2000](https://github.com/matrix-org/matrix-doc/issues/2000)). ([\#7118](https://github.com/matrix-org/synapse/issues/7118))
- Improve the support for SSO authentication on the login fallback page. ([\#7152](https://github.com/matrix-org/synapse/issues/7152), [\#7235](https://github.com/matrix-org/synapse/issues/7235))
- Always whitelist the login fallback in the SSO configuration if `public_baseurl` is set. ([\#7153](https://github.com/matrix-org/synapse/issues/7153))
- Admin users are no longer required to be in a room to create an alias for it. ([\#7191](https://github.com/matrix-org/synapse/issues/7191))
- Require admin privileges to enable room encryption by default. This does not affect existing rooms. ([\#7230](https://github.com/matrix-org/synapse/issues/7230))
- Add a config option for specifying the value of the Accept-Language HTTP header when generating URL previews. ([\#7265](https://github.com/matrix-org/synapse/issues/7265))
- Allow `/requestToken` endpoints to hide the existence (or lack thereof) of 3PID associations on the homeserver. ([\#7315](https://github.com/matrix-org/synapse/issues/7315))
- Add a configuration setting to tweak the threshold for dummy events. ([\#7422](https://github.com/matrix-org/synapse/issues/7422))


Bugfixes
--------

- Don't attempt to use an invalid sqlite config if no database configuration is provided. Contributed by @nekatak. ([\#6573](https://github.com/matrix-org/synapse/issues/6573))
- Fix single-sign on with CAS systems: pass the same service URL when requesting the CAS ticket and when calling the `proxyValidate` URL. Contributed by @Naugrimm. ([\#6634](https://github.com/matrix-org/synapse/issues/6634))
- Fix missing field `default` when fetching user-defined push rules. ([\#6639](https://github.com/matrix-org/synapse/issues/6639))
- Improve error responses when accessing remote public room lists. ([\#6899](https://github.com/matrix-org/synapse/issues/6899), [\#7368](https://github.com/matrix-org/synapse/issues/7368))
- Transfer alias mappings on room upgrade. ([\#6946](https://github.com/matrix-org/synapse/issues/6946))
- Ensure that a user interactive authentication session is tied to a single request. ([\#7068](https://github.com/matrix-org/synapse/issues/7068), [\#7455](https://github.com/matrix-org/synapse/issues/7455))
- Fix a bug in the federation API which could cause occasional "Failed to get PDU" errors. ([\#7089](https://github.com/matrix-org/synapse/issues/7089))
- Return the proper error (`M_BAD_ALIAS`) when a non-existant canonical alias is provided. ([\#7109](https://github.com/matrix-org/synapse/issues/7109))
- Fix a bug which meant that groups updates were not correctly replicated between workers. ([\#7117](https://github.com/matrix-org/synapse/issues/7117))
- Fix starting workers when federation sending not split out. ([\#7133](https://github.com/matrix-org/synapse/issues/7133))
- Ensure `is_verified` is a boolean in responses to `GET /_matrix/client/r0/room_keys/keys`. Also warn the user if they forgot the `version` query param. ([\#7150](https://github.com/matrix-org/synapse/issues/7150))
- Fix error page being shown when a custom SAML handler attempted to redirect when processing an auth response. ([\#7151](https://github.com/matrix-org/synapse/issues/7151))
- Avoid importing `sqlite3` when using the postgres backend. Contributed by David Vo. ([\#7155](https://github.com/matrix-org/synapse/issues/7155))
- Fix excessive CPU usage by `prune_old_outbound_device_pokes` job. ([\#7159](https://github.com/matrix-org/synapse/issues/7159))
- Fix a bug which could cause outbound federation traffic to stop working if a client uploaded an incorrect e2e device signature. ([\#7177](https://github.com/matrix-org/synapse/issues/7177))
- Fix a bug which could cause incorrect 'cyclic dependency' error. ([\#7178](https://github.com/matrix-org/synapse/issues/7178))
- Fix a bug that could cause a user to be invited to a server notices (aka System Alerts) room without any notice being sent. ([\#7199](https://github.com/matrix-org/synapse/issues/7199))
- Fix some worker-mode replication handling not being correctly recorded in CPU usage stats. ([\#7203](https://github.com/matrix-org/synapse/issues/7203))
- Do not allow a deactivated user to login via SSO. ([\#7240](https://github.com/matrix-org/synapse/issues/7240), [\#7259](https://github.com/matrix-org/synapse/issues/7259))
- Fix --help command-line argument. ([\#7249](https://github.com/matrix-org/synapse/issues/7249))
- Fix room publish permissions not being checked on room creation. ([\#7260](https://github.com/matrix-org/synapse/issues/7260))
- Reject unknown session IDs during user interactive authentication instead of silently creating a new session. ([\#7268](https://github.com/matrix-org/synapse/issues/7268))
- Fix a SQL query introduced in Synapse 1.12.0 which could cause large amounts of logging to the postgres slow-query log. ([\#7274](https://github.com/matrix-org/synapse/issues/7274))
- Persist user interactive authentication sessions across workers and Synapse restarts. ([\#7302](https://github.com/matrix-org/synapse/issues/7302))
- Fixed backwards compatibility logic of the first value of `trusted_third_party_id_servers` being used for `account_threepid_delegates.email`, which occurs when the former, deprecated option is set and the latter is not. ([\#7316](https://github.com/matrix-org/synapse/issues/7316))
- Fix a bug where event updates might not be sent over replication to worker processes after the stream falls behind. ([\#7337](https://github.com/matrix-org/synapse/issues/7337), [\#7358](https://github.com/matrix-org/synapse/issues/7358))
- Fix bad error handling that would cause Synapse to crash if it's provided with a YAML configuration file that's either empty or doesn't parse into a key-value map. ([\#7341](https://github.com/matrix-org/synapse/issues/7341))
- Fix incorrect metrics reporting for `renew_attestations` background task. ([\#7344](https://github.com/matrix-org/synapse/issues/7344))
- Prevent non-federating rooms from appearing in responses to federated `POST /publicRoom` requests when a filter was included. ([\#7367](https://github.com/matrix-org/synapse/issues/7367))
- Fix a bug which would cause the room durectory to be incorrectly populated if Synapse was upgraded directly from v1.2.1 or earlier to v1.4.0 or later. Note that this fix does not apply retrospectively; see the [upgrade notes](docs/upgrade.md#upgrading-to-v1130) for more information. ([\#7387](https://github.com/matrix-org/synapse/issues/7387))
- Fix bug in `EventContext.deserialize`. ([\#7393](https://github.com/matrix-org/synapse/issues/7393))


Improved Documentation
----------------------

- Update Debian installation instructions to recommend installing the `virtualenv` package instead of `python3-virtualenv`. ([\#6892](https://github.com/matrix-org/synapse/issues/6892))
- Improve the documentation for database configuration. ([\#6988](https://github.com/matrix-org/synapse/issues/6988))
- Improve the documentation of application service configuration files. ([\#7091](https://github.com/matrix-org/synapse/issues/7091))
- Update pre-built package name for FreeBSD. ([\#7107](https://github.com/matrix-org/synapse/issues/7107))
- Update postgres docs with login troubleshooting information. ([\#7119](https://github.com/matrix-org/synapse/issues/7119))
- Clean up INSTALL.md a bit. ([\#7141](https://github.com/matrix-org/synapse/issues/7141))
- Add documentation for running a local CAS server for testing. ([\#7147](https://github.com/matrix-org/synapse/issues/7147))
- Improve README.md by being explicit about public IP recommendation for TURN relaying. ([\#7167](https://github.com/matrix-org/synapse/issues/7167))
- Fix a small typo in the `metrics_flags` config option. ([\#7171](https://github.com/matrix-org/synapse/issues/7171))
- Update the contributed documentation on managing synapse workers with systemd, and bring it into the core distribution. ([\#7234](https://github.com/matrix-org/synapse/issues/7234))
- Add documentation to the `password_providers` config option. Add known password provider implementations to docs. ([\#7238](https://github.com/matrix-org/synapse/issues/7238), [\#7248](https://github.com/matrix-org/synapse/issues/7248))
- Modify suggested nginx reverse proxy configuration to match Synapse's default file upload size. Contributed by @ProCycleDev. ([\#7251](https://github.com/matrix-org/synapse/issues/7251))
- Documentation of media_storage_providers options updated to avoid misunderstandings. Contributed by Tristan Lins. ([\#7272](https://github.com/matrix-org/synapse/issues/7272))
- Add documentation on monitoring workers with Prometheus. ([\#7357](https://github.com/matrix-org/synapse/issues/7357))
- Clarify endpoint usage in the users admin api documentation. ([\#7361](https://github.com/matrix-org/synapse/issues/7361))


Deprecations and Removals
-------------------------

- Remove nonfunctional `captcha_bypass_secret` option from `homeserver.yaml`. ([\#7137](https://github.com/matrix-org/synapse/issues/7137))


Internal Changes
----------------

- Add benchmarks for LruCache. ([\#6446](https://github.com/matrix-org/synapse/issues/6446))
- Return total number of users and profile attributes in admin users endpoint. Contributed by Awesome Technologies Innovationslabor GmbH. ([\#6881](https://github.com/matrix-org/synapse/issues/6881))
- Change device list streams to have one row per ID. ([\#7010](https://github.com/matrix-org/synapse/issues/7010))
- Remove concept of a non-limited stream. ([\#7011](https://github.com/matrix-org/synapse/issues/7011))
- Move catchup of replication streams logic to worker. ([\#7024](https://github.com/matrix-org/synapse/issues/7024), [\#7195](https://github.com/matrix-org/synapse/issues/7195), [\#7226](https://github.com/matrix-org/synapse/issues/7226), [\#7239](https://github.com/matrix-org/synapse/issues/7239), [\#7286](https://github.com/matrix-org/synapse/issues/7286), [\#7290](https://github.com/matrix-org/synapse/issues/7290), [\#7318](https://github.com/matrix-org/synapse/issues/7318), [\#7326](https://github.com/matrix-org/synapse/issues/7326), [\#7378](https://github.com/matrix-org/synapse/issues/7378), [\#7421](https://github.com/matrix-org/synapse/issues/7421))
- Convert some of synapse.rest.media to async/await. ([\#7110](https://github.com/matrix-org/synapse/issues/7110), [\#7184](https://github.com/matrix-org/synapse/issues/7184), [\#7241](https://github.com/matrix-org/synapse/issues/7241))
- De-duplicate / remove unused REST code for login and auth. ([\#7115](https://github.com/matrix-org/synapse/issues/7115))
- Convert `*StreamRow` classes to inner classes. ([\#7116](https://github.com/matrix-org/synapse/issues/7116))
- Clean up some LoggingContext code. ([\#7120](https://github.com/matrix-org/synapse/issues/7120), [\#7181](https://github.com/matrix-org/synapse/issues/7181), [\#7183](https://github.com/matrix-org/synapse/issues/7183), [\#7408](https://github.com/matrix-org/synapse/issues/7408), [\#7426](https://github.com/matrix-org/synapse/issues/7426))
- Add explicit `instance_id` for USER_SYNC commands and remove implicit `conn_id` usage. ([\#7128](https://github.com/matrix-org/synapse/issues/7128))
- Refactored the CAS authentication logic to a separate class. ([\#7136](https://github.com/matrix-org/synapse/issues/7136))
- Run replication streamers on workers. ([\#7146](https://github.com/matrix-org/synapse/issues/7146))
- Add tests for outbound device pokes. ([\#7157](https://github.com/matrix-org/synapse/issues/7157))
- Fix device list update stream ids going backward. ([\#7158](https://github.com/matrix-org/synapse/issues/7158))
- Use `stream.current_token()` and remove `stream_positions()`. ([\#7172](https://github.com/matrix-org/synapse/issues/7172))
- Move client command handling out of TCP protocol. ([\#7185](https://github.com/matrix-org/synapse/issues/7185))
- Move server command handling out of TCP protocol. ([\#7187](https://github.com/matrix-org/synapse/issues/7187))
- Fix consistency of HTTP status codes reported in log lines. ([\#7188](https://github.com/matrix-org/synapse/issues/7188))
- Only run one background database update at a time. ([\#7190](https://github.com/matrix-org/synapse/issues/7190))
- Remove sent outbound device list pokes from the database. ([\#7192](https://github.com/matrix-org/synapse/issues/7192))
- Add a background database update job to clear out duplicate `device_lists_outbound_pokes`. ([\#7193](https://github.com/matrix-org/synapse/issues/7193))
- Remove some extraneous debugging log lines. ([\#7207](https://github.com/matrix-org/synapse/issues/7207))
- Add explicit Python build tooling as dependencies for the snapcraft build. ([\#7213](https://github.com/matrix-org/synapse/issues/7213))
- Add typing information to federation server code. ([\#7219](https://github.com/matrix-org/synapse/issues/7219))
- Extend room admin api (`GET /_synapse/admin/v1/rooms`) with additional attributes. ([\#7225](https://github.com/matrix-org/synapse/issues/7225))
- Unblacklist '/upgrade creates a new room' sytest for workers. ([\#7228](https://github.com/matrix-org/synapse/issues/7228))
- Remove redundant checks on `daemonize` from synctl. ([\#7233](https://github.com/matrix-org/synapse/issues/7233))
- Upgrade jQuery to v3.4.1 on fallback login/registration pages. ([\#7236](https://github.com/matrix-org/synapse/issues/7236))
- Change log line that told user to implement onLogin/onRegister fallback js functions to a warning, instead of an info, so it's more visible. ([\#7237](https://github.com/matrix-org/synapse/issues/7237))
- Correct the parameters of a test fixture. Contributed by Isaiah Singletary. ([\#7243](https://github.com/matrix-org/synapse/issues/7243))
- Convert auth handler to async/await. ([\#7261](https://github.com/matrix-org/synapse/issues/7261))
- Add some unit tests for replication. ([\#7278](https://github.com/matrix-org/synapse/issues/7278))
- Improve typing annotations in `synapse.replication.tcp.streams.Stream`. ([\#7291](https://github.com/matrix-org/synapse/issues/7291))
- Reduce log verbosity of url cache cleanup tasks. ([\#7295](https://github.com/matrix-org/synapse/issues/7295))
- Fix sample SAML Service Provider configuration. Contributed by @frcl. ([\#7300](https://github.com/matrix-org/synapse/issues/7300))
- Fix StreamChangeCache to work with multiple entities changing on the same stream id. ([\#7303](https://github.com/matrix-org/synapse/issues/7303))
- Fix an incorrect import in IdentityHandler. ([\#7319](https://github.com/matrix-org/synapse/issues/7319))
- Reduce logging verbosity for successful federation requests. ([\#7321](https://github.com/matrix-org/synapse/issues/7321))
- Convert some federation handler code to async/await. ([\#7338](https://github.com/matrix-org/synapse/issues/7338))
- Fix collation for postgres for unit tests. ([\#7359](https://github.com/matrix-org/synapse/issues/7359))
- Convert RegistrationWorkerStore.is_server_admin and dependent code to async/await. ([\#7363](https://github.com/matrix-org/synapse/issues/7363))
- Add an `instance_name` to `RDATA` and `POSITION` replication commands. ([\#7364](https://github.com/matrix-org/synapse/issues/7364))
- Thread through instance name to replication client. ([\#7369](https://github.com/matrix-org/synapse/issues/7369))
- Convert synapse.server_notices to async/await. ([\#7394](https://github.com/matrix-org/synapse/issues/7394))
- Convert synapse.notifier to async/await. ([\#7395](https://github.com/matrix-org/synapse/issues/7395))
- Fix issues with the Python package manifest. ([\#7404](https://github.com/matrix-org/synapse/issues/7404))
- Prevent methods in `synapse.handlers.auth` from polling the homeserver config every request. ([\#7420](https://github.com/matrix-org/synapse/issues/7420))
- Speed up fetching device lists changes when handling `/sync` requests. ([\#7423](https://github.com/matrix-org/synapse/issues/7423))
- Run group attestation renewal in series rather than parallel for performance. ([\#7442](https://github.com/matrix-org/synapse/issues/7442))


Synapse 1.12.4 (2020-04-23)
===========================

No significant changes.


Synapse 1.12.4rc1 (2020-04-22)
==============================

Features
--------

- Always send users their own device updates. ([\#7160](https://github.com/matrix-org/synapse/issues/7160))
- Add support for handling GET requests for `account_data` on a worker. ([\#7311](https://github.com/matrix-org/synapse/issues/7311))


Bugfixes
--------

- Fix a bug that prevented cross-signing with users on worker-mode synapses. ([\#7255](https://github.com/matrix-org/synapse/issues/7255))
- Do not treat display names as globs in push rules. ([\#7271](https://github.com/matrix-org/synapse/issues/7271))
- Fix a bug with cross-signing devices belonging to remote users who did not share a room with any user on the local homeserver. ([\#7289](https://github.com/matrix-org/synapse/issues/7289))

Synapse 1.12.3 (2020-04-03)
===========================

- Remove the the pin to Pillow 7.0 which was introduced in Synapse 1.12.2, and
correctly fix the issue with building the Debian packages. ([\#7212](https://github.com/matrix-org/synapse/issues/7212))

Synapse 1.12.2 (2020-04-02)
===========================

This release works around [an issue](https://github.com/matrix-org/synapse/issues/7208) with building the debian packages.

No other significant changes since 1.12.1.

Synapse 1.12.1 (2020-04-02)
===========================

No significant changes since 1.12.1rc1.


Synapse 1.12.1rc1 (2020-03-31)
==============================

Bugfixes
--------

- Fix starting workers when federation sending not split out. ([\#7133](https://github.com/matrix-org/synapse/issues/7133)). Introduced in v1.12.0.
- Avoid importing `sqlite3` when using the postgres backend. Contributed by David Vo. ([\#7155](https://github.com/matrix-org/synapse/issues/7155)). Introduced in v1.12.0rc1.
- Fix a bug which could cause outbound federation traffic to stop working if a client uploaded an incorrect e2e device signature. ([\#7177](https://github.com/matrix-org/synapse/issues/7177)). Introduced in v1.11.0.

Synapse 1.12.0 (2020-03-23)
===========================

Debian packages and Docker images are rebuilt using the latest versions of
dependency libraries, including Twisted 20.3.0. **Please see security advisory
below**.

Potential slow database update during upgrade
---------------------------------------------

Synapse 1.12.0 includes a database update which is run as part of the upgrade,
and which may take some time (several hours in the case of a large
server). Synapse will not respond to HTTP requests while this update is taking
place. For imformation on seeing if you are affected, and workaround if you
are, see the [upgrade notes](docs/upgrade.md#upgrading-to-v1120).

Security advisory
-----------------

Synapse may be vulnerable to request-smuggling attacks when it is used with a
reverse-proxy. The vulnerabilties are fixed in Twisted 20.3.0, and are
described in
[CVE-2020-10108](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2020-10108)
and
[CVE-2020-10109](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2020-10109).
For a good introduction to this class of request-smuggling attacks, see
https://portswigger.net/research/http-desync-attacks-request-smuggling-reborn.

We are not aware of these vulnerabilities being exploited in the wild, and
do not believe that they are exploitable with current versions of any reverse
proxies. Nevertheless, we recommend that all Synapse administrators ensure that
they have the latest versions of the Twisted library to ensure that their
installation remains secure.

* Administrators using the [`matrix.org` Docker
  image](https://hub.docker.com/r/matrixdotorg/synapse/) or the [Debian/Ubuntu
  packages from
  `matrix.org`](https://matrix-org.github.io/synapse/latest/setup/installation.html#matrixorg-packages)
  should ensure that they have version 1.12.0 installed: these images include
  Twisted 20.3.0.
* Administrators who have [installed Synapse from
  source](https://matrix-org.github.io/synapse/latest/setup/installation.html#installing-from-source)
  should upgrade Twisted within their virtualenv by running:
  ```sh
  <path_to_virtualenv>/bin/pip install 'Twisted>=20.3.0'
  ```
* Administrators who have installed Synapse from distribution packages should
  consult the information from their distributions.

The `matrix.org` Synapse instance was not vulnerable to these vulnerabilities.

Advance notice of change to the default `git` branch for Synapse
----------------------------------------------------------------

Currently, the default `git` branch for Synapse is `master`, which tracks the
latest release.

After the release of Synapse 1.13.0, we intend to change this default to
`develop`, which is the development tip. This is more consistent with common
practice and modern `git` usage.

Although we try to keep `develop` in a stable state, there may be occasions
where regressions creep in. Developers and distributors who have scripts which
run builds using the default branch of `Synapse` should therefore consider
pinning their scripts to `master`.


Synapse 1.12.0rc1 (2020-03-19)
==============================

Features
--------

- Changes related to room alias management ([MSC2432](https://github.com/matrix-org/matrix-doc/pull/2432)):
  - Publishing/removing a room from the room directory now requires the user to have a power level capable of modifying the canonical alias, instead of the room aliases. ([\#6965](https://github.com/matrix-org/synapse/issues/6965))
  - Validate the `alt_aliases` property of canonical alias events. ([\#6971](https://github.com/matrix-org/synapse/issues/6971))
  - Users with a power level sufficient to modify the canonical alias of a room can now delete room aliases. ([\#6986](https://github.com/matrix-org/synapse/issues/6986))
  - Implement updated authorization rules and redaction rules for aliases events, from [MSC2261](https://github.com/matrix-org/matrix-doc/pull/2261) and [MSC2432](https://github.com/matrix-org/matrix-doc/pull/2432). ([\#7037](https://github.com/matrix-org/synapse/issues/7037))
  - Stop sending m.room.aliases events during room creation and upgrade. ([\#6941](https://github.com/matrix-org/synapse/issues/6941))
  - Synapse no longer uses room alias events to calculate room names for push notifications. ([\#6966](https://github.com/matrix-org/synapse/issues/6966))
  - The room list endpoint no longer returns a list of aliases. ([\#6970](https://github.com/matrix-org/synapse/issues/6970))
  - Remove special handling of aliases events from [MSC2260](https://github.com/matrix-org/matrix-doc/pull/2260) added in v1.10.0rc1. ([\#7034](https://github.com/matrix-org/synapse/issues/7034))
- Expose the `synctl`, `hash_password` and `generate_config` commands in the snapcraft package. Contributed by @devec0. ([\#6315](https://github.com/matrix-org/synapse/issues/6315))
- Check that server_name is correctly set before running database updates. ([\#6982](https://github.com/matrix-org/synapse/issues/6982))
- Break down monthly active users by `appservice_id` and emit via Prometheus. ([\#7030](https://github.com/matrix-org/synapse/issues/7030))
- Render a configurable and comprehensible error page if something goes wrong during the SAML2 authentication process. ([\#7058](https://github.com/matrix-org/synapse/issues/7058), [\#7067](https://github.com/matrix-org/synapse/issues/7067))
- Add an optional parameter to control whether other sessions are logged out when a user's password is modified. ([\#7085](https://github.com/matrix-org/synapse/issues/7085))
- Add prometheus metrics for the number of active pushers. ([\#7103](https://github.com/matrix-org/synapse/issues/7103), [\#7106](https://github.com/matrix-org/synapse/issues/7106))
- Improve performance when making HTTPS requests to sygnal, sydent, etc, by sharing the SSL context object between connections. ([\#7094](https://github.com/matrix-org/synapse/issues/7094))


Bugfixes
--------

- When a user's profile is updated via the admin API, also generate a displayname/avatar update for that user in each room. ([\#6572](https://github.com/matrix-org/synapse/issues/6572))
- Fix a couple of bugs in email configuration handling. ([\#6962](https://github.com/matrix-org/synapse/issues/6962))
- Fix an issue affecting worker-based deployments where replication would stop working, necessitating a full restart, after joining a large room. ([\#6967](https://github.com/matrix-org/synapse/issues/6967))
- Fix `duplicate key` error which was logged when rejoining a room over federation. ([\#6968](https://github.com/matrix-org/synapse/issues/6968))
- Prevent user from setting 'deactivated' to anything other than a bool on the v2 PUT /users Admin API. ([\#6990](https://github.com/matrix-org/synapse/issues/6990))
- Fix py35-old CI by using native tox package. ([\#7018](https://github.com/matrix-org/synapse/issues/7018))
- Fix a bug causing `org.matrix.dummy_event` to be included in responses from `/sync`. ([\#7035](https://github.com/matrix-org/synapse/issues/7035))
- Fix a bug that renders UTF-8 text files incorrectly when loaded from media. Contributed by @TheStranjer. ([\#7044](https://github.com/matrix-org/synapse/issues/7044))
- Fix a bug that would cause Synapse to respond with an error about event visibility if a client tried to request the state of a room at a given token. ([\#7066](https://github.com/matrix-org/synapse/issues/7066))
- Repair a data-corruption issue which was introduced in Synapse 1.10, and fixed in Synapse 1.11, and which could cause `/sync` to return with 404 errors about missing events and unknown rooms. ([\#7070](https://github.com/matrix-org/synapse/issues/7070))
- Fix a bug causing account validity renewal emails to be sent even if the feature is turned off in some cases. ([\#7074](https://github.com/matrix-org/synapse/issues/7074))


Improved Documentation
----------------------

- Updated CentOS8 install instructions. Contributed by Richard Kellner. ([\#6925](https://github.com/matrix-org/synapse/issues/6925))
- Fix `POSTGRES_INITDB_ARGS` in the `contrib/docker/docker-compose.yml` example docker-compose configuration. ([\#6984](https://github.com/matrix-org/synapse/issues/6984))
- Change date in [INSTALL.md](./INSTALL.md#tls-certificates) for last date of getting TLS certificates to November 2019. ([\#7015](https://github.com/matrix-org/synapse/issues/7015))
- Document that the fallback auth endpoints must be routed to the same worker node as the register endpoints. ([\#7048](https://github.com/matrix-org/synapse/issues/7048))


Deprecations and Removals
-------------------------

- Remove the unused query_auth federation endpoint per [MSC2451](https://github.com/matrix-org/matrix-doc/pull/2451). ([\#7026](https://github.com/matrix-org/synapse/issues/7026))


Internal Changes
----------------

- Add type hints to `logging/context.py`. ([\#6309](https://github.com/matrix-org/synapse/issues/6309))
- Add some clarifications to `README.md` in the database schema directory. ([\#6615](https://github.com/matrix-org/synapse/issues/6615))
- Refactoring work in preparation for changing the event redaction algorithm. ([\#6874](https://github.com/matrix-org/synapse/issues/6874), [\#6875](https://github.com/matrix-org/synapse/issues/6875), [\#6983](https://github.com/matrix-org/synapse/issues/6983), [\#7003](https://github.com/matrix-org/synapse/issues/7003))
- Improve performance of v2 state resolution for large rooms. ([\#6952](https://github.com/matrix-org/synapse/issues/6952), [\#7095](https://github.com/matrix-org/synapse/issues/7095))
- Reduce time spent doing GC, by freezing objects on startup. ([\#6953](https://github.com/matrix-org/synapse/issues/6953))
- Minor perfermance fixes to `get_auth_chain_ids`. ([\#6954](https://github.com/matrix-org/synapse/issues/6954))
- Don't record remote cross-signing keys in the `devices` table. ([\#6956](https://github.com/matrix-org/synapse/issues/6956))
- Use flake8-comprehensions to enforce good hygiene of list/set/dict comprehensions. ([\#6957](https://github.com/matrix-org/synapse/issues/6957))
- Merge worker apps together. ([\#6964](https://github.com/matrix-org/synapse/issues/6964), [\#7002](https://github.com/matrix-org/synapse/issues/7002), [\#7055](https://github.com/matrix-org/synapse/issues/7055), [\#7104](https://github.com/matrix-org/synapse/issues/7104))
- Remove redundant `store_room` call from `FederationHandler._process_received_pdu`. ([\#6979](https://github.com/matrix-org/synapse/issues/6979))
- Update warning for incorrect database collation/ctype to include link to documentation. ([\#6985](https://github.com/matrix-org/synapse/issues/6985))
- Add some type annotations to the database storage classes. ([\#6987](https://github.com/matrix-org/synapse/issues/6987))
- Port `synapse.handlers.presence` to async/await. ([\#6991](https://github.com/matrix-org/synapse/issues/6991), [\#7019](https://github.com/matrix-org/synapse/issues/7019))
- Add some type annotations to the federation base & client classes. ([\#6995](https://github.com/matrix-org/synapse/issues/6995))
- Port `synapse.rest.keys` to async/await. ([\#7020](https://github.com/matrix-org/synapse/issues/7020))
- Add a type check to `is_verified` when processing room keys. ([\#7045](https://github.com/matrix-org/synapse/issues/7045))
- Add type annotations and comments to the auth handler. ([\#7063](https://github.com/matrix-org/synapse/issues/7063))


Synapse 1.11.1 (2020-03-03)
===========================

This release includes a security fix impacting installations using Single Sign-On (i.e. SAML2 or CAS) for authentication. Administrators of such installations are encouraged to upgrade as soon as possible.

The release also includes fixes for a couple of other bugs.

Bugfixes
--------

- Add a confirmation step to the SSO login flow before redirecting users to the redirect URL. ([b2bd54a2](https://github.com/matrix-org/synapse/commit/b2bd54a2e31d9a248f73fadb184ae9b4cbdb49f9), [65c73cdf](https://github.com/matrix-org/synapse/commit/65c73cdfec1876a9fec2fd2c3a74923cd146fe0b), [a0178df1](https://github.com/matrix-org/synapse/commit/a0178df10422a76fd403b82d2b2a4ed28a9a9d1e))
- Fixed set a user as an admin with the admin API `PUT /_synapse/admin/v2/users/<user_id>`. Contributed by @dklimpel. ([\#6910](https://github.com/matrix-org/synapse/issues/6910))
- Fix bug introduced in Synapse 1.11.0 which sometimes caused errors when joining rooms over federation, with `'coroutine' object has no attribute 'event_id'`. ([\#6996](https://github.com/matrix-org/synapse/issues/6996))


Synapse 1.11.0 (2020-02-21)
===========================

Improved Documentation
----------------------

- Small grammatical fixes to the ACME v1 deprecation notice. ([\#6944](https://github.com/matrix-org/synapse/issues/6944))


Synapse 1.11.0rc1 (2020-02-19)
==============================

Features
--------

- Admin API to add or modify threepids of user accounts. ([\#6769](https://github.com/matrix-org/synapse/issues/6769))
- Limit the number of events that can be requested by the backfill federation API to 100. ([\#6864](https://github.com/matrix-org/synapse/issues/6864))
- Add ability to run some group APIs on workers. ([\#6866](https://github.com/matrix-org/synapse/issues/6866))
- Reject device display names over 100 characters in length to prevent abuse. ([\#6882](https://github.com/matrix-org/synapse/issues/6882))
- Add ability to route federation user device queries to workers. ([\#6873](https://github.com/matrix-org/synapse/issues/6873))
- The result of a user directory search can now be filtered via the spam checker. ([\#6888](https://github.com/matrix-org/synapse/issues/6888))
- Implement new `GET /_matrix/client/unstable/org.matrix.msc2432/rooms/{roomId}/aliases` endpoint as per [MSC2432](https://github.com/matrix-org/matrix-doc/pull/2432). ([\#6939](https://github.com/matrix-org/synapse/issues/6939), [\#6948](https://github.com/matrix-org/synapse/issues/6948), [\#6949](https://github.com/matrix-org/synapse/issues/6949))
- Stop sending `m.room.alias` events wheng adding / removing aliases. Check `alt_aliases` in the latest `m.room.canonical_alias` event when deleting an alias. ([\#6904](https://github.com/matrix-org/synapse/issues/6904))
- Change the default power levels of invites, tombstones and server ACLs for new rooms. ([\#6834](https://github.com/matrix-org/synapse/issues/6834))

Bugfixes
--------

- Fixed third party event rules function `on_create_room`'s return value being ignored. ([\#6781](https://github.com/matrix-org/synapse/issues/6781))
- Allow URL-encoded User IDs on `/_synapse/admin/v2/users/<user_id>[/admin]` endpoints. Thanks to @NHAS for reporting. ([\#6825](https://github.com/matrix-org/synapse/issues/6825))
- Fix Synapse refusing to start if `federation_certificate_verification_whitelist` option is blank. ([\#6849](https://github.com/matrix-org/synapse/issues/6849))
- Fix errors from logging in the purge jobs related to the message retention policies support. ([\#6945](https://github.com/matrix-org/synapse/issues/6945))
- Return a 404 instead of 200 for querying information of a non-existant user through the admin API. ([\#6901](https://github.com/matrix-org/synapse/issues/6901))


Updates to the Docker image
---------------------------

- The deprecated "generate-config-on-the-fly" mode is no longer supported. ([\#6918](https://github.com/matrix-org/synapse/issues/6918))


Improved Documentation
----------------------

- Add details of PR merge strategy to contributing docs. ([\#6846](https://github.com/matrix-org/synapse/issues/6846))
- Spell out that the last event sent to a room won't be deleted by a purge. ([\#6891](https://github.com/matrix-org/synapse/issues/6891))
- Update Synapse's documentation to warn about the deprecation of ACME v1. ([\#6905](https://github.com/matrix-org/synapse/issues/6905), [\#6907](https://github.com/matrix-org/synapse/issues/6907), [\#6909](https://github.com/matrix-org/synapse/issues/6909))
- Add documentation for the spam checker. ([\#6906](https://github.com/matrix-org/synapse/issues/6906))
- Fix worker docs to point `/publicised_groups` API correctly. ([\#6938](https://github.com/matrix-org/synapse/issues/6938))
- Clean up and update docs on setting up federation. ([\#6940](https://github.com/matrix-org/synapse/issues/6940))
- Add a warning about indentation to generated configuration files. ([\#6920](https://github.com/matrix-org/synapse/issues/6920))
- Databases created using the compose file in contrib/docker will now always have correct encoding and locale settings. Contributed by Fridtjof Mund. ([\#6921](https://github.com/matrix-org/synapse/issues/6921))
- Update pip install directions in readme to avoid error when using zsh. ([\#6855](https://github.com/matrix-org/synapse/issues/6855))


Deprecations and Removals
-------------------------

- Remove `m.lazy_load_members` from `unstable_features` since lazy loading is in the stable Client-Server API version r0.5.0. ([\#6877](https://github.com/matrix-org/synapse/issues/6877))


Internal Changes
----------------

- Add type hints to `SyncHandler`. ([\#6821](https://github.com/matrix-org/synapse/issues/6821))
- Refactoring work in preparation for changing the event redaction algorithm. ([\#6823](https://github.com/matrix-org/synapse/issues/6823), [\#6827](https://github.com/matrix-org/synapse/issues/6827), [\#6854](https://github.com/matrix-org/synapse/issues/6854), [\#6856](https://github.com/matrix-org/synapse/issues/6856), [\#6857](https://github.com/matrix-org/synapse/issues/6857), [\#6858](https://github.com/matrix-org/synapse/issues/6858))
- Fix stacktraces when using `ObservableDeferred` and async/await. ([\#6836](https://github.com/matrix-org/synapse/issues/6836))
- Port much of `synapse.handlers.federation` to async/await. ([\#6837](https://github.com/matrix-org/synapse/issues/6837), [\#6840](https://github.com/matrix-org/synapse/issues/6840))
- Populate `rooms.room_version` database column at startup, rather than in a background update. ([\#6847](https://github.com/matrix-org/synapse/issues/6847))
- Reduce amount we log at `INFO` level. ([\#6833](https://github.com/matrix-org/synapse/issues/6833), [\#6862](https://github.com/matrix-org/synapse/issues/6862))
- Remove unused `get_room_stats_state` method. ([\#6869](https://github.com/matrix-org/synapse/issues/6869))
- Add typing to `synapse.federation.sender` and port to async/await. ([\#6871](https://github.com/matrix-org/synapse/issues/6871))
- Refactor `_EventInternalMetadata` object to improve type safety. ([\#6872](https://github.com/matrix-org/synapse/issues/6872))
- Add an additional entry to the SyTest blacklist for worker mode. ([\#6883](https://github.com/matrix-org/synapse/issues/6883))
- Fix the use of sed in the linting scripts when using BSD sed. ([\#6887](https://github.com/matrix-org/synapse/issues/6887))
- Add type hints to the spam checker module. ([\#6915](https://github.com/matrix-org/synapse/issues/6915))
- Convert the directory handler tests to use HomeserverTestCase. ([\#6919](https://github.com/matrix-org/synapse/issues/6919))
- Increase DB/CPU perf of `_is_server_still_joined` check. ([\#6936](https://github.com/matrix-org/synapse/issues/6936))
- Tiny optimisation for incoming HTTP request dispatch. ([\#6950](https://github.com/matrix-org/synapse/issues/6950))


Synapse 1.10.1 (2020-02-17)
===========================

Bugfixes
--------

- Fix a bug introduced in Synapse 1.10.0 which would cause room state to be cleared in the database if Synapse was upgraded direct from 1.2.1 or earlier to 1.10.0. ([\#6924](https://github.com/matrix-org/synapse/issues/6924))


Synapse 1.10.0 (2020-02-12)
===========================

**WARNING to client developers**: As of this release Synapse validates `client_secret` parameters in the Client-Server API as per the spec. See [\#6766](https://github.com/matrix-org/synapse/issues/6766) for details.

Updates to the Docker image
---------------------------

- Update the docker images to Alpine Linux 3.11. ([\#6897](https://github.com/matrix-org/synapse/issues/6897))


Synapse 1.10.0rc5 (2020-02-11)
==============================

Bugfixes
--------

- Fix the filtering introduced in 1.10.0rc3 to also apply to the state blocks returned by `/sync`. ([\#6884](https://github.com/matrix-org/synapse/issues/6884))

Synapse 1.10.0rc4 (2020-02-11)
==============================

This release candidate was built incorrectly and is superceded by 1.10.0rc5.

Synapse 1.10.0rc3 (2020-02-10)
==============================

Features
--------

- Filter out `m.room.aliases` from the CS API to mitigate abuse while a better solution is specced. ([\#6878](https://github.com/matrix-org/synapse/issues/6878))


Internal Changes
----------------

- Fix continuous integration failures with old versions of `pip`, which were introduced by a release of the `zipp` library. ([\#6880](https://github.com/matrix-org/synapse/issues/6880))


Synapse 1.10.0rc2 (2020-02-06)
==============================

Bugfixes
--------

- Fix an issue with cross-signing where device signatures were not sent to remote servers. ([\#6844](https://github.com/matrix-org/synapse/issues/6844))
- Fix to the unknown remote device detection which was introduced in 1.10.rc1. ([\#6848](https://github.com/matrix-org/synapse/issues/6848))


Internal Changes
----------------

- Detect unexpected sender keys on remote encrypted events and resync device lists. ([\#6850](https://github.com/matrix-org/synapse/issues/6850))


Synapse 1.10.0rc1 (2020-01-31)
==============================

Features
--------

- Add experimental support for updated authorization rules for aliases events, from [MSC2260](https://github.com/matrix-org/matrix-doc/pull/2260). ([\#6787](https://github.com/matrix-org/synapse/issues/6787), [\#6790](https://github.com/matrix-org/synapse/issues/6790), [\#6794](https://github.com/matrix-org/synapse/issues/6794))


Bugfixes
--------

- Warn if postgres database has a non-C locale, as that can cause issues when upgrading locales (e.g. due to upgrading OS). ([\#6734](https://github.com/matrix-org/synapse/issues/6734))
- Minor fixes to `PUT /_synapse/admin/v2/users` admin api. ([\#6761](https://github.com/matrix-org/synapse/issues/6761))
- Validate `client_secret` parameter using the regex provided by the Client-Server API, temporarily allowing `:` characters for older clients. The `:` character will be removed in a future release. ([\#6767](https://github.com/matrix-org/synapse/issues/6767))
- Fix persisting redaction events that have been redacted (or otherwise don't have a redacts key). ([\#6771](https://github.com/matrix-org/synapse/issues/6771))
- Fix outbound federation request metrics. ([\#6795](https://github.com/matrix-org/synapse/issues/6795))
- Fix bug where querying a remote user's device keys that weren't cached resulted in only returning a single device. ([\#6796](https://github.com/matrix-org/synapse/issues/6796))
- Fix race in federation sender worker that delayed sending of device updates. ([\#6799](https://github.com/matrix-org/synapse/issues/6799), [\#6800](https://github.com/matrix-org/synapse/issues/6800))
- Fix bug where Synapse didn't invalidate cache of remote users' devices when Synapse left a room. ([\#6801](https://github.com/matrix-org/synapse/issues/6801))
- Fix waking up other workers when remote server is detected to have come back online. ([\#6811](https://github.com/matrix-org/synapse/issues/6811))


Improved Documentation
----------------------

- Clarify documentation related to `user_dir` and `federation_reader` workers. ([\#6775](https://github.com/matrix-org/synapse/issues/6775))


Internal Changes
----------------

- Record room versions in the `rooms` table. ([\#6729](https://github.com/matrix-org/synapse/issues/6729), [\#6788](https://github.com/matrix-org/synapse/issues/6788), [\#6810](https://github.com/matrix-org/synapse/issues/6810))
- Propagate cache invalidates from workers to other workers. ([\#6748](https://github.com/matrix-org/synapse/issues/6748))
- Remove some unnecessary admin handler abstraction methods. ([\#6751](https://github.com/matrix-org/synapse/issues/6751))
- Add some debugging for media storage providers. ([\#6757](https://github.com/matrix-org/synapse/issues/6757))
- Detect unknown remote devices and mark cache as stale. ([\#6776](https://github.com/matrix-org/synapse/issues/6776), [\#6819](https://github.com/matrix-org/synapse/issues/6819))
- Attempt to resync remote users' devices when detected as stale. ([\#6786](https://github.com/matrix-org/synapse/issues/6786))
- Delete current state from the database when server leaves a room. ([\#6792](https://github.com/matrix-org/synapse/issues/6792))
- When a client asks for a remote user's device keys check if the local cache for that user has been marked as potentially stale. ([\#6797](https://github.com/matrix-org/synapse/issues/6797))
- Add background update to clean out left rooms from current state. ([\#6802](https://github.com/matrix-org/synapse/issues/6802), [\#6816](https://github.com/matrix-org/synapse/issues/6816))
- Refactoring work in preparation for changing the event redaction algorithm. ([\#6803](https://github.com/matrix-org/synapse/issues/6803), [\#6805](https://github.com/matrix-org/synapse/issues/6805), [\#6806](https://github.com/matrix-org/synapse/issues/6806), [\#6807](https://github.com/matrix-org/synapse/issues/6807), [\#6820](https://github.com/matrix-org/synapse/issues/6820))


Synapse 1.9.1 (2020-01-28)
==========================

Bugfixes
--------

- Fix bug where setting `mau_limit_reserved_threepids` config would cause Synapse to refuse to start. ([\#6793](https://github.com/matrix-org/synapse/issues/6793))


Synapse 1.9.0 (2020-01-23)
==========================

**WARNING**: As of this release, Synapse no longer supports versions of SQLite before 3.11, and will refuse to start when configured to use an older version. Administrators are recommended to migrate their database to Postgres (see instructions [here](docs/postgres.md)).

If your Synapse deployment uses workers, note that the reverse-proxy configurations for the `synapse.app.media_repository`, `synapse.app.federation_reader` and `synapse.app.event_creator` workers have changed, with the addition of a few paths (see the updated configurations [here](docs/workers.md#available-worker-applications)). Existing configurations will continue to work.


Improved Documentation
----------------------

- Fix endpoint documentation for the List Rooms admin API. ([\#6770](https://github.com/matrix-org/synapse/issues/6770))


Synapse 1.9.0rc1 (2020-01-22)
=============================

Features
--------

- Allow admin to create or modify a user. Contributed by Awesome Technologies Innovationslabor GmbH. ([\#5742](https://github.com/matrix-org/synapse/issues/5742))
- Add new quarantine media admin APIs to quarantine by media ID or by user who uploaded the media. ([\#6681](https://github.com/matrix-org/synapse/issues/6681), [\#6756](https://github.com/matrix-org/synapse/issues/6756))
- Add `org.matrix.e2e_cross_signing` to `unstable_features` in `/versions` as per [MSC1756](https://github.com/matrix-org/matrix-doc/pull/1756). ([\#6712](https://github.com/matrix-org/synapse/issues/6712))
- Add a new admin API to list and filter rooms on the server. ([\#6720](https://github.com/matrix-org/synapse/issues/6720))


Bugfixes
--------

- Correctly proxy HTTP errors due to API calls to remote group servers. ([\#6654](https://github.com/matrix-org/synapse/issues/6654))
- Fix media repo admin APIs when using a media worker. ([\#6664](https://github.com/matrix-org/synapse/issues/6664))
- Fix "CRITICAL" errors being logged when a request is received for a uri containing non-ascii characters. ([\#6682](https://github.com/matrix-org/synapse/issues/6682))
- Fix a bug where we would assign a numeric user ID if somebody tried registering with an empty username. ([\#6690](https://github.com/matrix-org/synapse/issues/6690))
- Fix `purge_room` admin API. ([\#6711](https://github.com/matrix-org/synapse/issues/6711))
- Fix a bug causing Synapse to not always purge quiet rooms with a low `max_lifetime` in their message retention policies when running the automated purge jobs. ([\#6714](https://github.com/matrix-org/synapse/issues/6714))
- Fix the `synapse_port_db` not correctly running background updates. Thanks @tadzik for reporting. ([\#6718](https://github.com/matrix-org/synapse/issues/6718))
- Fix changing password via user admin API. ([\#6730](https://github.com/matrix-org/synapse/issues/6730))
- Fix `/events/:event_id` deprecated API. ([\#6731](https://github.com/matrix-org/synapse/issues/6731))
- Fix monthly active user limiting support for worker mode, fixes [#4639](https://github.com/matrix-org/synapse/issues/4639). ([\#6742](https://github.com/matrix-org/synapse/issues/6742))
- Fix bug when setting `account_validity` to an empty block in the config. Thanks to @Sorunome for reporting. ([\#6747](https://github.com/matrix-org/synapse/issues/6747))
- Fix `AttributeError: 'NoneType' object has no attribute 'get'` in `hash_password` when configuration has an empty `password_config`. Contributed by @ivilata. ([\#6753](https://github.com/matrix-org/synapse/issues/6753))
- Fix the `docker-compose.yaml` overriding the entire `/etc` folder of the container. Contributed by Fabian Meyer. ([\#6656](https://github.com/matrix-org/synapse/issues/6656))


Improved Documentation
----------------------

- Fix a typo in the configuration example for purge jobs in the sample configuration file. ([\#6621](https://github.com/matrix-org/synapse/issues/6621))
- Add complete documentation of the message retention policies support. ([\#6624](https://github.com/matrix-org/synapse/issues/6624), [\#6665](https://github.com/matrix-org/synapse/issues/6665))
- Add some helpful tips about changelog entries to the GitHub pull request template. ([\#6663](https://github.com/matrix-org/synapse/issues/6663))
- Clarify the `account_validity` and `email` sections of the sample configuration. ([\#6685](https://github.com/matrix-org/synapse/issues/6685))
- Add more endpoints to the documentation for Synapse workers. ([\#6698](https://github.com/matrix-org/synapse/issues/6698))


Deprecations and Removals
-------------------------

- Synapse no longer supports versions of SQLite before 3.11, and will refuse to start when configured to use an older version. Administrators are recommended to migrate their database to Postgres (see instructions [here](docs/postgres.md)). ([\#6675](https://github.com/matrix-org/synapse/issues/6675))


Internal Changes
----------------

- Add `local_current_membership` table for tracking local user membership state in rooms. ([\#6655](https://github.com/matrix-org/synapse/issues/6655), [\#6728](https://github.com/matrix-org/synapse/issues/6728))
- Port `synapse.replication.tcp` to async/await. ([\#6666](https://github.com/matrix-org/synapse/issues/6666))
- Fixup `synapse.replication` to pass mypy checks. ([\#6667](https://github.com/matrix-org/synapse/issues/6667))
- Allow `additional_resources` to implement `IResource` directly. ([\#6686](https://github.com/matrix-org/synapse/issues/6686))
- Allow REST endpoint implementations to raise a `RedirectException`, which will redirect the user's browser to a given location. ([\#6687](https://github.com/matrix-org/synapse/issues/6687))
- Updates and extensions to the module API. ([\#6688](https://github.com/matrix-org/synapse/issues/6688))
- Updates to the SAML mapping provider API. ([\#6689](https://github.com/matrix-org/synapse/issues/6689), [\#6723](https://github.com/matrix-org/synapse/issues/6723))
- Remove redundant `RegistrationError` class. ([\#6691](https://github.com/matrix-org/synapse/issues/6691))
- Don't block processing of incoming EDUs behind processing PDUs in the same transaction. ([\#6697](https://github.com/matrix-org/synapse/issues/6697))
- Remove duplicate check for the `session` query parameter on the `/auth/xxx/fallback/web` Client-Server endpoint. ([\#6702](https://github.com/matrix-org/synapse/issues/6702))
- Attempt to retry sending a transaction when we detect a remote server has come back online, rather than waiting for a transaction to be triggered by new data. ([\#6706](https://github.com/matrix-org/synapse/issues/6706))
- Add `StateMap` type alias to simplify types. ([\#6715](https://github.com/matrix-org/synapse/issues/6715))
- Add a `DeltaState` to track changes to be made to current state during event persistence. ([\#6716](https://github.com/matrix-org/synapse/issues/6716))
- Add more logging around message retention policies support. ([\#6717](https://github.com/matrix-org/synapse/issues/6717))
- When processing a SAML response, log the assertions for easier configuration. ([\#6724](https://github.com/matrix-org/synapse/issues/6724))
- Fixup `synapse.rest` to pass mypy. ([\#6732](https://github.com/matrix-org/synapse/issues/6732), [\#6764](https://github.com/matrix-org/synapse/issues/6764))
- Fixup `synapse.api` to pass mypy. ([\#6733](https://github.com/matrix-org/synapse/issues/6733))
- Allow streaming cache 'invalidate all' to workers. ([\#6749](https://github.com/matrix-org/synapse/issues/6749))
- Remove unused CI docker compose files. ([\#6754](https://github.com/matrix-org/synapse/issues/6754))


Synapse 1.8.0 (2020-01-09)
==========================

**WARNING**: As of this release Synapse will refuse to start if the `log_file` config option is specified. Support for the option was removed in v1.3.0.


Bugfixes
--------

- Fix `GET` request on `/_synapse/admin/v2/users` endpoint. Contributed by Awesome Technologies Innovationslabor GmbH. ([\#6563](https://github.com/matrix-org/synapse/issues/6563))
- Fix incorrect signing of responses from the key server implementation. ([\#6657](https://github.com/matrix-org/synapse/issues/6657))


Synapse 1.8.0rc1 (2020-01-07)
=============================

Features
--------

- Add v2 APIs for the `send_join` and `send_leave` federation endpoints (as described in [MSC1802](https://github.com/matrix-org/matrix-doc/pull/1802)). ([\#6349](https://github.com/matrix-org/synapse/issues/6349))
- Add a develop script to generate full SQL schemas. ([\#6394](https://github.com/matrix-org/synapse/issues/6394))
- Add custom SAML username mapping functionality through an external provider plugin. ([\#6411](https://github.com/matrix-org/synapse/issues/6411))
- Automatically delete empty groups/communities. ([\#6453](https://github.com/matrix-org/synapse/issues/6453))
- Add option `limit_profile_requests_to_users_who_share_rooms` to prevent requirement of a local user sharing a room with another user to query their profile information. ([\#6523](https://github.com/matrix-org/synapse/issues/6523))
- Add an `export_signing_key` script to extract the public part of signing keys when rotating them. ([\#6546](https://github.com/matrix-org/synapse/issues/6546))
- Add experimental config option to specify multiple databases. ([\#6580](https://github.com/matrix-org/synapse/issues/6580))
- Raise an error if someone tries to use the `log_file` config option. ([\#6626](https://github.com/matrix-org/synapse/issues/6626))


Bugfixes
--------

- Prevent redacted events from being returned during message search. ([\#6377](https://github.com/matrix-org/synapse/issues/6377), [\#6522](https://github.com/matrix-org/synapse/issues/6522))
- Prevent error on trying to search a upgraded room when the server is not in the predecessor room. ([\#6385](https://github.com/matrix-org/synapse/issues/6385))
- Improve performance of looking up cross-signing keys. ([\#6486](https://github.com/matrix-org/synapse/issues/6486))
- Fix race which occasionally caused deleted devices to reappear. ([\#6514](https://github.com/matrix-org/synapse/issues/6514))
- Fix missing row in `device_max_stream_id` that could cause unable to decrypt errors after server restart. ([\#6555](https://github.com/matrix-org/synapse/issues/6555))
- Fix a bug which meant that we did not send systemd notifications on startup if acme was enabled. ([\#6571](https://github.com/matrix-org/synapse/issues/6571))
- Fix exception when fetching the `matrix.org:ed25519:auto` key. ([\#6625](https://github.com/matrix-org/synapse/issues/6625))
- Fix bug where a moderator upgraded a room and became an admin in the new room. ([\#6633](https://github.com/matrix-org/synapse/issues/6633))
- Fix an error which was thrown by the `PresenceHandler` `_on_shutdown` handler. ([\#6640](https://github.com/matrix-org/synapse/issues/6640))
- Fix exceptions in the synchrotron worker log when events are rejected. ([\#6645](https://github.com/matrix-org/synapse/issues/6645))
- Ensure that upgraded rooms are removed from the directory. ([\#6648](https://github.com/matrix-org/synapse/issues/6648))
- Fix a bug causing Synapse not to fetch missing events when it believes it has every event in the room. ([\#6652](https://github.com/matrix-org/synapse/issues/6652))


Improved Documentation
----------------------

- Document the Room Shutdown Admin API. ([\#6541](https://github.com/matrix-org/synapse/issues/6541))
- Reword sections of [docs/federate.md](docs/federate.md) that explained delegation at time of Synapse 1.0 transition. ([\#6601](https://github.com/matrix-org/synapse/issues/6601))
- Added the section 'Configuration' in [docs/turn-howto.md](docs/turn-howto.md). ([\#6614](https://github.com/matrix-org/synapse/issues/6614))


Deprecations and Removals
-------------------------

- Remove redundant code from event authorisation implementation. ([\#6502](https://github.com/matrix-org/synapse/issues/6502))
- Remove unused, undocumented `/_matrix/content` API. ([\#6628](https://github.com/matrix-org/synapse/issues/6628))


Internal Changes
----------------

- Add *experimental* support for multiple physical databases and split out state storage to separate data store. ([\#6245](https://github.com/matrix-org/synapse/issues/6245), [\#6510](https://github.com/matrix-org/synapse/issues/6510), [\#6511](https://github.com/matrix-org/synapse/issues/6511), [\#6513](https://github.com/matrix-org/synapse/issues/6513), [\#6564](https://github.com/matrix-org/synapse/issues/6564), [\#6565](https://github.com/matrix-org/synapse/issues/6565))
- Port sections of code base to async/await. ([\#6496](https://github.com/matrix-org/synapse/issues/6496), [\#6504](https://github.com/matrix-org/synapse/issues/6504), [\#6505](https://github.com/matrix-org/synapse/issues/6505), [\#6517](https://github.com/matrix-org/synapse/issues/6517), [\#6559](https://github.com/matrix-org/synapse/issues/6559), [\#6647](https://github.com/matrix-org/synapse/issues/6647), [\#6653](https://github.com/matrix-org/synapse/issues/6653))
- Remove `SnapshotCache` in favour of `ResponseCache`. ([\#6506](https://github.com/matrix-org/synapse/issues/6506))
- Silence mypy errors for files outside those specified. ([\#6512](https://github.com/matrix-org/synapse/issues/6512))
- Clean up some logging when handling incoming events over federation. ([\#6515](https://github.com/matrix-org/synapse/issues/6515))
- Test more folders against mypy. ([\#6534](https://github.com/matrix-org/synapse/issues/6534))
- Update `mypy` to new version. ([\#6537](https://github.com/matrix-org/synapse/issues/6537))
- Adjust the sytest blacklist for worker mode. ([\#6538](https://github.com/matrix-org/synapse/issues/6538))
- Remove unused `get_pagination_rows` methods from `EventSource` classes. ([\#6557](https://github.com/matrix-org/synapse/issues/6557))
- Clean up logs from the push notifier at startup. ([\#6558](https://github.com/matrix-org/synapse/issues/6558))
- Improve diagnostics on database upgrade failure. ([\#6570](https://github.com/matrix-org/synapse/issues/6570))
- Reduce the reconnect time when worker replication fails, to make it easier to catch up. ([\#6617](https://github.com/matrix-org/synapse/issues/6617))
- Simplify http handling by removing redundant `SynapseRequestFactory`. ([\#6619](https://github.com/matrix-org/synapse/issues/6619))
- Add a workaround for synapse raising exceptions when fetching the notary's own key from the notary. ([\#6620](https://github.com/matrix-org/synapse/issues/6620))
- Automate generation of the sample log config. ([\#6627](https://github.com/matrix-org/synapse/issues/6627))
- Simplify event creation code by removing redundant queries on the `event_reference_hashes` table. ([\#6629](https://github.com/matrix-org/synapse/issues/6629))
- Fix errors when `frozen_dicts` are enabled. ([\#6642](https://github.com/matrix-org/synapse/issues/6642))


Synapse 1.7.3 (2019-12-31)
==========================

This release fixes a long-standing bug in the state resolution algorithm.

Bugfixes
--------

- Fix exceptions caused by state resolution choking on malformed events. ([\#6608](https://github.com/matrix-org/synapse/issues/6608))


Synapse 1.7.2 (2019-12-20)
==========================

This release fixes some regressions introduced in Synapse 1.7.0 and 1.7.1.

Bugfixes
--------

- Fix a regression introduced in Synapse 1.7.1 which caused errors when attempting to backfill rooms over federation. ([\#6576](https://github.com/matrix-org/synapse/issues/6576))
- Fix a bug introduced in Synapse 1.7.0 which caused an error on startup when upgrading from versions before 1.3.0. ([\#6578](https://github.com/matrix-org/synapse/issues/6578))


Synapse 1.7.1 (2019-12-18)
==========================

This release includes several security fixes as well as a fix to a bug exposed by the security fixes. Administrators are encouraged to upgrade as soon as possible.

Security updates
----------------

- Fix a bug which could cause room events to be incorrectly authorized using events from a different room. ([\#6501](https://github.com/matrix-org/synapse/issues/6501), [\#6503](https://github.com/matrix-org/synapse/issues/6503), [\#6521](https://github.com/matrix-org/synapse/issues/6521), [\#6524](https://github.com/matrix-org/synapse/issues/6524), [\#6530](https://github.com/matrix-org/synapse/issues/6530), [\#6531](https://github.com/matrix-org/synapse/issues/6531))
- Fix a bug causing responses to the `/context` client endpoint to not use the pruned version of the event. ([\#6553](https://github.com/matrix-org/synapse/issues/6553))
- Fix a cause of state resets in room versions 2 onwards. ([\#6556](https://github.com/matrix-org/synapse/issues/6556), [\#6560](https://github.com/matrix-org/synapse/issues/6560))

Bugfixes
--------

- Fix a bug which could cause the federation server to incorrectly return errors when handling certain obscure event graphs. ([\#6526](https://github.com/matrix-org/synapse/issues/6526), [\#6527](https://github.com/matrix-org/synapse/issues/6527))

Synapse 1.7.0 (2019-12-13)
==========================

This release changes the default settings so that only local authenticated users can query the server's room directory. See the [upgrade notes](docs/upgrade.md#upgrading-to-v170) for details.

Support for SQLite versions before 3.11 is now deprecated. A future release will refuse to start if used with an SQLite version before 3.11.

Administrators are reminded that SQLite should not be used for production instances. Instructions for migrating to Postgres are available [here](docs/postgres.md). A future release of synapse will, by default, disable federation for servers using SQLite.

No significant changes since 1.7.0rc2.


Synapse 1.7.0rc2 (2019-12-11)
=============================

Bugfixes
--------

- Fix incorrect error message for invalid requests when setting user's avatar URL. ([\#6497](https://github.com/matrix-org/synapse/issues/6497))
- Fix support for SQLite 3.7. ([\#6499](https://github.com/matrix-org/synapse/issues/6499))
- Fix regression where sending email push would not work when using a pusher worker. ([\#6507](https://github.com/matrix-org/synapse/issues/6507), [\#6509](https://github.com/matrix-org/synapse/issues/6509))


Synapse 1.7.0rc1 (2019-12-09)
=============================

Features
--------

- Implement per-room message retention policies. ([\#5815](https://github.com/matrix-org/synapse/issues/5815), [\#6436](https://github.com/matrix-org/synapse/issues/6436))
- Add etag and count fields to key backup endpoints to help clients guess if there are new keys. ([\#5858](https://github.com/matrix-org/synapse/issues/5858))
- Add `/admin/v2/users` endpoint with pagination. Contributed by Awesome Technologies Innovationslabor GmbH. ([\#5925](https://github.com/matrix-org/synapse/issues/5925))
- Require User-Interactive Authentication for `/account/3pid/add`, meaning the user's password will be required to add a third-party ID to their account. ([\#6119](https://github.com/matrix-org/synapse/issues/6119))
- Implement the `/_matrix/federation/unstable/net.atleastfornow/state/<context>` API as drafted in MSC2314. ([\#6176](https://github.com/matrix-org/synapse/issues/6176))
- Configure privacy-preserving settings by default for the room directory. ([\#6355](https://github.com/matrix-org/synapse/issues/6355))
- Add ephemeral messages support by partially implementing [MSC2228](https://github.com/matrix-org/matrix-doc/pull/2228). ([\#6409](https://github.com/matrix-org/synapse/issues/6409))
- Add support for [MSC 2367](https://github.com/matrix-org/matrix-doc/pull/2367), which allows specifying a reason on all membership events. ([\#6434](https://github.com/matrix-org/synapse/issues/6434))


Bugfixes
--------

- Transfer non-standard power levels on room upgrade. ([\#6237](https://github.com/matrix-org/synapse/issues/6237))
- Fix error from the Pillow library when uploading RGBA images. ([\#6241](https://github.com/matrix-org/synapse/issues/6241))
- Correctly apply the event filter to the `state`, `events_before` and `events_after` fields in the response to `/context` requests. ([\#6329](https://github.com/matrix-org/synapse/issues/6329))
- Fix caching devices for remote users when using workers, so that we don't attempt to refetch (and potentially fail) each time a user requests devices. ([\#6332](https://github.com/matrix-org/synapse/issues/6332))
- Prevent account data syncs getting lost across TCP replication. ([\#6333](https://github.com/matrix-org/synapse/issues/6333))
- Fix bug: TypeError in `register_user()` while using LDAP auth module. ([\#6406](https://github.com/matrix-org/synapse/issues/6406))
- Fix an intermittent exception when handling read-receipts. ([\#6408](https://github.com/matrix-org/synapse/issues/6408))
- Fix broken guest registration when there are existing blocks of numeric user IDs. ([\#6420](https://github.com/matrix-org/synapse/issues/6420))
- Fix startup error when http proxy is defined. ([\#6421](https://github.com/matrix-org/synapse/issues/6421))
- Fix error when using synapse_port_db on a vanilla synapse db. ([\#6449](https://github.com/matrix-org/synapse/issues/6449))
- Fix uploading multiple cross signing signatures for the same user. ([\#6451](https://github.com/matrix-org/synapse/issues/6451))
- Fix bug which lead to exceptions being thrown in a loop when a cross-signed device is deleted. ([\#6462](https://github.com/matrix-org/synapse/issues/6462))
- Fix `synapse_port_db` not exiting with a 0 code if something went wrong during the port process. ([\#6470](https://github.com/matrix-org/synapse/issues/6470))
- Improve sanity-checking when receiving events over federation. ([\#6472](https://github.com/matrix-org/synapse/issues/6472))
- Fix inaccurate per-block Prometheus metrics. ([\#6491](https://github.com/matrix-org/synapse/issues/6491))
- Fix small performance regression for sending invites. ([\#6493](https://github.com/matrix-org/synapse/issues/6493))
- Back out cross-signing code added in Synapse 1.5.0, which caused a performance regression. ([\#6494](https://github.com/matrix-org/synapse/issues/6494))


Improved Documentation
----------------------

- Update documentation and variables in user contributed systemd reference file. ([\#6369](https://github.com/matrix-org/synapse/issues/6369), [\#6490](https://github.com/matrix-org/synapse/issues/6490))
- Fix link in the user directory documentation. ([\#6388](https://github.com/matrix-org/synapse/issues/6388))
- Add build instructions to the docker readme. ([\#6390](https://github.com/matrix-org/synapse/issues/6390))
- Switch Ubuntu package install recommendation to use python3 packages in INSTALL.md. ([\#6443](https://github.com/matrix-org/synapse/issues/6443))
- Write some docs for the quarantine_media api. ([\#6458](https://github.com/matrix-org/synapse/issues/6458))
- Convert CONTRIBUTING.rst to markdown (among other small fixes). ([\#6461](https://github.com/matrix-org/synapse/issues/6461))


Deprecations and Removals
-------------------------

- Remove admin/v1/users_paginate endpoint. Contributed by Awesome Technologies Innovationslabor GmbH. ([\#5925](https://github.com/matrix-org/synapse/issues/5925))
- Remove fallback for federation with old servers which lack the /federation/v1/state_ids API. ([\#6488](https://github.com/matrix-org/synapse/issues/6488))


Internal Changes
----------------

- Add benchmarks for structured logging and improve output performance. ([\#6266](https://github.com/matrix-org/synapse/issues/6266))
- Improve the performance of outputting structured logging. ([\#6322](https://github.com/matrix-org/synapse/issues/6322))
- Refactor some code in the event authentication path for clarity. ([\#6343](https://github.com/matrix-org/synapse/issues/6343), [\#6468](https://github.com/matrix-org/synapse/issues/6468), [\#6480](https://github.com/matrix-org/synapse/issues/6480))
- Clean up some unnecessary quotation marks around the codebase. ([\#6362](https://github.com/matrix-org/synapse/issues/6362))
- Complain on startup instead of 500'ing during runtime when `public_baseurl` isn't set when necessary. ([\#6379](https://github.com/matrix-org/synapse/issues/6379))
- Add a test scenario to make sure room history purges don't break `/messages` in the future. ([\#6392](https://github.com/matrix-org/synapse/issues/6392))
- Clarifications for the email configuration settings. ([\#6423](https://github.com/matrix-org/synapse/issues/6423))
- Add more tests to the blacklist when running in worker mode. ([\#6429](https://github.com/matrix-org/synapse/issues/6429))
- Refactor data store layer to support multiple databases in the future. ([\#6454](https://github.com/matrix-org/synapse/issues/6454), [\#6464](https://github.com/matrix-org/synapse/issues/6464), [\#6469](https://github.com/matrix-org/synapse/issues/6469), [\#6487](https://github.com/matrix-org/synapse/issues/6487))
- Port synapse.rest.client.v1 to async/await. ([\#6482](https://github.com/matrix-org/synapse/issues/6482))
- Port synapse.rest.client.v2_alpha to async/await. ([\#6483](https://github.com/matrix-org/synapse/issues/6483))
- Port SyncHandler to async/await. ([\#6484](https://github.com/matrix-org/synapse/issues/6484))

Synapse 1.6.1 (2019-11-28)
==========================

Security updates
----------------

This release includes a security fix ([\#6426](https://github.com/matrix-org/synapse/issues/6426), below). Administrators are encouraged to upgrade as soon as possible.

Bugfixes
--------

- Clean up local threepids from user on account deactivation. ([\#6426](https://github.com/matrix-org/synapse/issues/6426))
- Fix startup error when http proxy is defined. ([\#6421](https://github.com/matrix-org/synapse/issues/6421))


Synapse 1.6.0 (2019-11-26)
==========================

Bugfixes
--------

- Fix phone home stats reporting. ([\#6418](https://github.com/matrix-org/synapse/issues/6418))


Synapse 1.6.0rc2 (2019-11-25)
=============================

Bugfixes
--------

- Fix a bug which could cause the background database update hander for event labels to get stuck in a loop raising exceptions. ([\#6407](https://github.com/matrix-org/synapse/issues/6407))


Synapse 1.6.0rc1 (2019-11-20)
=============================

Features
--------

- Add federation support for cross-signing. ([\#5727](https://github.com/matrix-org/synapse/issues/5727))
- Increase default room version from 4 to 5, thereby enforcing server key validity period checks. ([\#6220](https://github.com/matrix-org/synapse/issues/6220))
- Add support for outbound http proxying via http_proxy/HTTPS_PROXY env vars. ([\#6238](https://github.com/matrix-org/synapse/issues/6238))
- Implement label-based filtering on `/sync` and `/messages` ([MSC2326](https://github.com/matrix-org/matrix-doc/pull/2326)). ([\#6301](https://github.com/matrix-org/synapse/issues/6301), [\#6310](https://github.com/matrix-org/synapse/issues/6310), [\#6340](https://github.com/matrix-org/synapse/issues/6340))


Bugfixes
--------

- Fix LruCache callback deduplication for Python 3.8. Contributed by @V02460. ([\#6213](https://github.com/matrix-org/synapse/issues/6213))
- Remove a room from a server's public rooms list on room upgrade. ([\#6232](https://github.com/matrix-org/synapse/issues/6232), [\#6235](https://github.com/matrix-org/synapse/issues/6235))
- Delete keys from key backup when deleting backup versions. ([\#6253](https://github.com/matrix-org/synapse/issues/6253))
- Make notification of cross-signing signatures work with workers. ([\#6254](https://github.com/matrix-org/synapse/issues/6254))
- Fix exception when remote servers attempt to join a room that they're not allowed to join. ([\#6278](https://github.com/matrix-org/synapse/issues/6278))
- Prevent errors from appearing on Synapse startup if `git` is not installed. ([\#6284](https://github.com/matrix-org/synapse/issues/6284))
- Appservice requests will no longer contain a double slash prefix when the appservice url provided ends in a slash. ([\#6306](https://github.com/matrix-org/synapse/issues/6306))
- Fix `/purge_room` admin API. ([\#6307](https://github.com/matrix-org/synapse/issues/6307))
- Fix the `hidden` field in the `devices` table for SQLite versions prior to 3.23.0. ([\#6313](https://github.com/matrix-org/synapse/issues/6313))
- Fix bug which casued rejected events to be persisted with the wrong room state. ([\#6320](https://github.com/matrix-org/synapse/issues/6320))
- Fix bug where `rc_login` ratelimiting would prematurely kick in. ([\#6335](https://github.com/matrix-org/synapse/issues/6335))
- Prevent the server taking a long time to start up when guest registration is enabled. ([\#6338](https://github.com/matrix-org/synapse/issues/6338))
- Fix bug where upgrading a guest account to a full user would fail when account validity is enabled. ([\#6359](https://github.com/matrix-org/synapse/issues/6359))
- Fix `to_device` stream ID getting reset every time Synapse restarts, which had the potential to cause unable to decrypt errors. ([\#6363](https://github.com/matrix-org/synapse/issues/6363))
- Fix permission denied error when trying to generate a config file with the docker image. ([\#6389](https://github.com/matrix-org/synapse/issues/6389))


Improved Documentation
----------------------

- Contributor documentation now mentions script to run linters. ([\#6164](https://github.com/matrix-org/synapse/issues/6164))
- Modify CAPTCHA_SETUP.md to update the terms `private key` and `public key` to `secret key` and `site key` respectively. Contributed by Yash Jipkate. ([\#6257](https://github.com/matrix-org/synapse/issues/6257))
- Update `INSTALL.md` Email section to talk about `account_threepid_delegates`. ([\#6272](https://github.com/matrix-org/synapse/issues/6272))
- Fix a small typo in `account_threepid_delegates` configuration option. ([\#6273](https://github.com/matrix-org/synapse/issues/6273))


Internal Changes
----------------

- Add a CI job to test the `synapse_port_db` script. ([\#6140](https://github.com/matrix-org/synapse/issues/6140), [\#6276](https://github.com/matrix-org/synapse/issues/6276))
- Convert EventContext to an attrs. ([\#6218](https://github.com/matrix-org/synapse/issues/6218))
- Move `persist_events` out from main data store. ([\#6240](https://github.com/matrix-org/synapse/issues/6240), [\#6300](https://github.com/matrix-org/synapse/issues/6300))
- Reduce verbosity of user/room stats. ([\#6250](https://github.com/matrix-org/synapse/issues/6250))
- Reduce impact of debug logging. ([\#6251](https://github.com/matrix-org/synapse/issues/6251))
- Expose some homeserver functionality to spam checkers. ([\#6259](https://github.com/matrix-org/synapse/issues/6259))
- Change cache descriptors to always return deferreds. ([\#6263](https://github.com/matrix-org/synapse/issues/6263), [\#6291](https://github.com/matrix-org/synapse/issues/6291))
- Fix incorrect comment regarding the functionality of an `if` statement. ([\#6269](https://github.com/matrix-org/synapse/issues/6269))
- Update CI to run `isort` over the `scripts` and `scripts-dev` directories. ([\#6270](https://github.com/matrix-org/synapse/issues/6270))
- Replace every instance of `logger.warn` method with `logger.warning` as the former is deprecated. ([\#6271](https://github.com/matrix-org/synapse/issues/6271), [\#6314](https://github.com/matrix-org/synapse/issues/6314))
- Port replication http server endpoints to async/await. ([\#6274](https://github.com/matrix-org/synapse/issues/6274))
- Port room rest handlers to async/await. ([\#6275](https://github.com/matrix-org/synapse/issues/6275))
- Remove redundant CLI parameters on CI's `flake8` step. ([\#6277](https://github.com/matrix-org/synapse/issues/6277))
- Port `federation_server.py` to async/await. ([\#6279](https://github.com/matrix-org/synapse/issues/6279))
- Port receipt and read markers to async/wait. ([\#6280](https://github.com/matrix-org/synapse/issues/6280))
- Split out state storage into separate data store. ([\#6294](https://github.com/matrix-org/synapse/issues/6294), [\#6295](https://github.com/matrix-org/synapse/issues/6295))
- Refactor EventContext for clarity. ([\#6298](https://github.com/matrix-org/synapse/issues/6298))
- Update the version of black used to 19.10b0. ([\#6304](https://github.com/matrix-org/synapse/issues/6304))
- Add some documentation about worker replication. ([\#6305](https://github.com/matrix-org/synapse/issues/6305))
- Move admin endpoints into separate files. Contributed by Awesome Technologies Innovationslabor GmbH. ([\#6308](https://github.com/matrix-org/synapse/issues/6308))
- Document the use of `lint.sh` for code style enforcement & extend it to run on specified paths only. ([\#6312](https://github.com/matrix-org/synapse/issues/6312))
- Add optional python dependencies and dependant binary libraries to snapcraft packaging. ([\#6317](https://github.com/matrix-org/synapse/issues/6317))
- Remove the dependency on psutil and replace functionality with the stdlib `resource` module. ([\#6318](https://github.com/matrix-org/synapse/issues/6318), [\#6336](https://github.com/matrix-org/synapse/issues/6336))
- Improve documentation for EventContext fields. ([\#6319](https://github.com/matrix-org/synapse/issues/6319))
- Add some checks that we aren't using state from rejected events. ([\#6330](https://github.com/matrix-org/synapse/issues/6330))
- Add continuous integration for python 3.8. ([\#6341](https://github.com/matrix-org/synapse/issues/6341))
- Correct spacing/case of various instances of the word "homeserver". ([\#6357](https://github.com/matrix-org/synapse/issues/6357))
- Temporarily blacklist the failing unit test PurgeRoomTestCase.test_purge_room. ([\#6361](https://github.com/matrix-org/synapse/issues/6361))


Synapse 1.5.1 (2019-11-06)
==========================

Features
--------

- Limit the length of data returned by url previews, to prevent DoS attacks. ([\#6331](https://github.com/matrix-org/synapse/issues/6331), [\#6334](https://github.com/matrix-org/synapse/issues/6334))


Synapse 1.5.0 (2019-10-29)
==========================

Security updates
----------------

This release includes a security fix ([\#6262](https://github.com/matrix-org/synapse/issues/6262), below). Administrators are encouraged to upgrade as soon as possible.

Bugfixes
--------

- Fix bug where room directory search was case sensitive. ([\#6268](https://github.com/matrix-org/synapse/issues/6268))


Synapse 1.5.0rc2 (2019-10-28)
=============================

Bugfixes
--------

- Update list of boolean columns in `synapse_port_db`. ([\#6247](https://github.com/matrix-org/synapse/issues/6247))
- Fix /keys/query API on workers. ([\#6256](https://github.com/matrix-org/synapse/issues/6256))
- Improve signature checking on some federation APIs. ([\#6262](https://github.com/matrix-org/synapse/issues/6262))


Internal Changes
----------------

- Move schema delta files to the correct data store. ([\#6248](https://github.com/matrix-org/synapse/issues/6248))
- Small performance improvement by removing repeated config lookups in room stats calculation. ([\#6255](https://github.com/matrix-org/synapse/issues/6255))


Synapse 1.5.0rc1 (2019-10-24)
==========================

Features
--------

- Improve quality of thumbnails for 1-bit/8-bit color palette images. ([\#2142](https://github.com/matrix-org/synapse/issues/2142))
- Add ability to upload cross-signing signatures. ([\#5726](https://github.com/matrix-org/synapse/issues/5726))
- Allow uploading of cross-signing keys. ([\#5769](https://github.com/matrix-org/synapse/issues/5769))
- CAS login now provides a default display name for users if a `displayname_attribute` is set in the configuration file. ([\#6114](https://github.com/matrix-org/synapse/issues/6114))
- Reject all pending invites for a user during deactivation. ([\#6125](https://github.com/matrix-org/synapse/issues/6125))
- Add config option to suppress client side resource limit alerting. ([\#6173](https://github.com/matrix-org/synapse/issues/6173))


Bugfixes
--------

- Return an HTTP 404 instead of 400 when requesting a filter by ID that is unknown to the server. Thanks to @krombel for contributing this! ([\#2380](https://github.com/matrix-org/synapse/issues/2380))
- Fix a bug where users could be invited twice to the same group. ([\#3436](https://github.com/matrix-org/synapse/issues/3436))
- Fix `/createRoom` failing with badly-formatted MXIDs in the invitee list. Thanks to @wener291! ([\#4088](https://github.com/matrix-org/synapse/issues/4088))
- Make the `synapse_port_db` script create the right indexes on a new PostgreSQL database. ([\#6102](https://github.com/matrix-org/synapse/issues/6102), [\#6178](https://github.com/matrix-org/synapse/issues/6178), [\#6243](https://github.com/matrix-org/synapse/issues/6243))
- Fix bug when uploading a large file: Synapse responds with `M_UNKNOWN` while it should be `M_TOO_LARGE` according to spec. Contributed by Anshul Angaria. ([\#6109](https://github.com/matrix-org/synapse/issues/6109))
- Fix user push rules being deleted from a room when it is upgraded. ([\#6144](https://github.com/matrix-org/synapse/issues/6144))
- Don't 500 when trying to exchange a revoked 3PID invite. ([\#6147](https://github.com/matrix-org/synapse/issues/6147))
- Fix transferring notifications and tags when joining an upgraded room that is new to your server. ([\#6155](https://github.com/matrix-org/synapse/issues/6155))
- Fix bug where guest account registration can wedge after restart. ([\#6161](https://github.com/matrix-org/synapse/issues/6161))
- Fix monthly active user reaping when reserved users are specified. ([\#6168](https://github.com/matrix-org/synapse/issues/6168))
- Fix `/federation/v1/state` endpoint not supporting newer room versions. ([\#6170](https://github.com/matrix-org/synapse/issues/6170))
- Fix bug where we were updating censored events as bytes rather than text, occaisonally causing invalid JSON being inserted breaking APIs that attempted to fetch such events. ([\#6186](https://github.com/matrix-org/synapse/issues/6186))
- Fix occasional missed updates in the room and user directories. ([\#6187](https://github.com/matrix-org/synapse/issues/6187))
- Fix tracing of non-JSON APIs, `/media`, `/key` etc. ([\#6195](https://github.com/matrix-org/synapse/issues/6195))
- Fix bug where presence would not get timed out correctly if a synchrotron worker is used and restarted. ([\#6212](https://github.com/matrix-org/synapse/issues/6212))
- synapse_port_db: Add 2 additional BOOLEAN_COLUMNS to be able to convert from database schema v56. ([\#6216](https://github.com/matrix-org/synapse/issues/6216))
- Fix a bug where the Synapse demo script blacklisted `::1` (ipv6 localhost) from receiving federation traffic. ([\#6229](https://github.com/matrix-org/synapse/issues/6229))


Updates to the Docker image
---------------------------

- Fix logging getting lost for the docker image. ([\#6197](https://github.com/matrix-org/synapse/issues/6197))


Internal Changes
----------------

- Update `user_filters` table to have a unique index, and non-null columns. Thanks to @pik for contributing this. ([\#1172](https://github.com/matrix-org/synapse/issues/1172), [\#6175](https://github.com/matrix-org/synapse/issues/6175), [\#6184](https://github.com/matrix-org/synapse/issues/6184))
- Allow devices to be marked as hidden, for use by features such as cross-signing.
  This adds a new field with a default value to the devices field in the database,
  and so the database upgrade may take a long time depending on how many devices
  are in the database. ([\#5759](https://github.com/matrix-org/synapse/issues/5759))
- Move lookup-related functions from RoomMemberHandler to IdentityHandler. ([\#5978](https://github.com/matrix-org/synapse/issues/5978))
- Improve performance of the public room list directory. ([\#6019](https://github.com/matrix-org/synapse/issues/6019), [\#6152](https://github.com/matrix-org/synapse/issues/6152), [\#6153](https://github.com/matrix-org/synapse/issues/6153), [\#6154](https://github.com/matrix-org/synapse/issues/6154))
- Edit header dicts docstrings in `SimpleHttpClient` to note that `str` or `bytes` can be passed as header keys. ([\#6077](https://github.com/matrix-org/synapse/issues/6077))
- Add snapcraft packaging information. Contributed by @devec0. ([\#6084](https://github.com/matrix-org/synapse/issues/6084), [\#6191](https://github.com/matrix-org/synapse/issues/6191))
- Kill off half-implemented password-reset via sms. ([\#6101](https://github.com/matrix-org/synapse/issues/6101))
- Remove `get_user_by_req` opentracing span and add some tags. ([\#6108](https://github.com/matrix-org/synapse/issues/6108))
- Drop some unused database tables. ([\#6115](https://github.com/matrix-org/synapse/issues/6115))
- Add env var to turn on tracking of log context changes. ([\#6127](https://github.com/matrix-org/synapse/issues/6127))
- Refactor configuration loading to allow better typechecking. ([\#6137](https://github.com/matrix-org/synapse/issues/6137))
- Log responder when responding to media request. ([\#6139](https://github.com/matrix-org/synapse/issues/6139))
- Improve performance of `find_next_generated_user_id` DB query. ([\#6148](https://github.com/matrix-org/synapse/issues/6148))
- Expand type-checking on modules imported by `synapse.config`. ([\#6150](https://github.com/matrix-org/synapse/issues/6150))
- Use Postgres ANY for selecting many values. ([\#6156](https://github.com/matrix-org/synapse/issues/6156))
- Add more caching to `_get_joined_users_from_context` DB query. ([\#6159](https://github.com/matrix-org/synapse/issues/6159))
- Add some metrics on the federation sender. ([\#6160](https://github.com/matrix-org/synapse/issues/6160))
- Add some logging to the rooms stats updates, to try to track down a flaky test. ([\#6167](https://github.com/matrix-org/synapse/issues/6167))
- Remove unused `timeout` parameter from `_get_public_room_list`. ([\#6179](https://github.com/matrix-org/synapse/issues/6179))
- Reject (accidental) attempts to insert bytes into postgres tables. ([\#6186](https://github.com/matrix-org/synapse/issues/6186))
- Make `version` optional in body of `PUT /room_keys/version/{version}`, since it's redundant. ([\#6189](https://github.com/matrix-org/synapse/issues/6189))
- Make storage layer responsible for adding device names to key, rather than the handler. ([\#6193](https://github.com/matrix-org/synapse/issues/6193))
- Port `synapse.rest.admin` module to use async/await. ([\#6196](https://github.com/matrix-org/synapse/issues/6196))
- Enforce that all boolean configuration values are lowercase in CI. ([\#6203](https://github.com/matrix-org/synapse/issues/6203))
- Remove some unused event-auth code. ([\#6214](https://github.com/matrix-org/synapse/issues/6214))
- Remove `Auth.check` method. ([\#6217](https://github.com/matrix-org/synapse/issues/6217))
- Remove `format_tap.py` script in favour of a perl reimplementation in Sytest's repo. ([\#6219](https://github.com/matrix-org/synapse/issues/6219))
- Refactor storage layer in preparation to support having multiple databases. ([\#6231](https://github.com/matrix-org/synapse/issues/6231))
- Remove some extra quotation marks across the codebase. ([\#6236](https://github.com/matrix-org/synapse/issues/6236))


Synapse 1.4.1 (2019-10-18)
==========================

No changes since 1.4.1rc1.


Synapse 1.4.1rc1 (2019-10-17)
=============================

Bugfixes
--------

- Fix bug where redacted events were sometimes incorrectly censored in the database, breaking APIs that attempted to fetch such events. ([\#6185](https://github.com/matrix-org/synapse/issues/6185), [5b0e9948](https://github.com/matrix-org/synapse/commit/5b0e9948eaae801643e594b5abc8ee4b10bd194e))

Synapse 1.4.0 (2019-10-03)
==========================

Bugfixes
--------

- Redact `client_secret` in server logs. ([\#6158](https://github.com/matrix-org/synapse/issues/6158))


Synapse 1.4.0rc2 (2019-10-02)
=============================

Bugfixes
--------

- Fix bug in background update that adds last seen information to the `devices` table, and improve its performance on Postgres. ([\#6135](https://github.com/matrix-org/synapse/issues/6135))
- Fix bad performance of censoring redactions background task. ([\#6141](https://github.com/matrix-org/synapse/issues/6141))
- Fix fetching censored redactions from DB, which caused APIs like initial sync to fail if it tried to include the censored redaction. ([\#6145](https://github.com/matrix-org/synapse/issues/6145))
- Fix exceptions when storing large retry intervals for down remote servers. ([\#6146](https://github.com/matrix-org/synapse/issues/6146))


Internal Changes
----------------

- Fix up sample config entry for `redaction_retention_period` option. ([\#6117](https://github.com/matrix-org/synapse/issues/6117))


Synapse 1.4.0rc1 (2019-09-26)
=============================

Note that this release includes significant changes around 3pid
verification. Administrators are reminded to review the [upgrade notes](docs/upgrade.md#upgrading-to-v140).

Features
--------

- Changes to 3pid verification:
  - Add the ability to send registration emails from the homeserver rather than delegating to an identity server. ([\#5835](https://github.com/matrix-org/synapse/issues/5835), [\#5940](https://github.com/matrix-org/synapse/issues/5940), [\#5993](https://github.com/matrix-org/synapse/issues/5993), [\#5994](https://github.com/matrix-org/synapse/issues/5994), [\#5868](https://github.com/matrix-org/synapse/issues/5868))
  - Replace `trust_identity_server_for_password_resets` config option with `account_threepid_delegates`, and make the `id_server` parameteter optional on `*/requestToken` endpoints, as per [MSC2263](https://github.com/matrix-org/matrix-doc/pull/2263). ([\#5876](https://github.com/matrix-org/synapse/issues/5876), [\#5969](https://github.com/matrix-org/synapse/issues/5969), [\#6028](https://github.com/matrix-org/synapse/issues/6028))
  - Switch to using the v2 Identity Service `/lookup` API where available, with fallback to v1. (Implements [MSC2134](https://github.com/matrix-org/matrix-doc/pull/2134) plus `id_access_token authentication` for v2 Identity Service APIs from [MSC2140](https://github.com/matrix-org/matrix-doc/pull/2140)). ([\#5897](https://github.com/matrix-org/synapse/issues/5897))
  - Remove `bind_email` and `bind_msisdn` parameters from `/register` ala [MSC2140](https://github.com/matrix-org/matrix-doc/pull/2140). ([\#5964](https://github.com/matrix-org/synapse/issues/5964))
  - Add `m.id_access_token` to `unstable_features` in `/versions` as per [MSC2264](https://github.com/matrix-org/matrix-doc/pull/2264). ([\#5974](https://github.com/matrix-org/synapse/issues/5974))
  - Use the v2 Identity Service API for 3PID invites. ([\#5979](https://github.com/matrix-org/synapse/issues/5979))
  - Add `POST /_matrix/client/unstable/account/3pid/unbind` endpoint from [MSC2140](https://github.com/matrix-org/matrix-doc/pull/2140) for unbinding a 3PID from an identity server without removing it from the homeserver user account. ([\#5980](https://github.com/matrix-org/synapse/issues/5980), [\#6062](https://github.com/matrix-org/synapse/issues/6062))
  - Use `account_threepid_delegate.email` and `account_threepid_delegate.msisdn` for validating threepid sessions. ([\#6011](https://github.com/matrix-org/synapse/issues/6011))
  - Allow homeserver to handle or delegate email validation when adding an email to a user's account. ([\#6042](https://github.com/matrix-org/synapse/issues/6042))
  - Implement new Client Server API endpoints `/account/3pid/add` and `/account/3pid/bind` as per [MSC2290](https://github.com/matrix-org/matrix-doc/pull/2290). ([\#6043](https://github.com/matrix-org/synapse/issues/6043))
  - Add an unstable feature flag for separate add/bind 3pid APIs. ([\#6044](https://github.com/matrix-org/synapse/issues/6044))
  - Remove `bind` parameter from Client Server POST `/account` endpoint as per [MSC2290](https://github.com/matrix-org/matrix-doc/pull/2290/). ([\#6067](https://github.com/matrix-org/synapse/issues/6067))
  - Add `POST /add_threepid/msisdn/submit_token` endpoint for proxying submitToken on an `account_threepid_handler`. ([\#6078](https://github.com/matrix-org/synapse/issues/6078))
  - Add `submit_url` response parameter to `*/msisdn/requestToken` endpoints. ([\#6079](https://github.com/matrix-org/synapse/issues/6079))
  - Add `m.require_identity_server` flag to /version's unstable_features. ([\#5972](https://github.com/matrix-org/synapse/issues/5972))
- Enhancements to OpenTracing support:
  - Make OpenTracing work in worker mode. ([\#5771](https://github.com/matrix-org/synapse/issues/5771))
  - Pass OpenTracing contexts between servers when transmitting EDUs. ([\#5852](https://github.com/matrix-org/synapse/issues/5852))
  - OpenTracing for device list updates. ([\#5853](https://github.com/matrix-org/synapse/issues/5853))
  - Add a tag recording a request's authenticated entity and corresponding servlet in OpenTracing. ([\#5856](https://github.com/matrix-org/synapse/issues/5856))
  - Add minimum OpenTracing for client servlets. ([\#5983](https://github.com/matrix-org/synapse/issues/5983))
  - Check at setup that OpenTracing is installed if it's enabled in the config. ([\#5985](https://github.com/matrix-org/synapse/issues/5985))
  - Trace replication send times. ([\#5986](https://github.com/matrix-org/synapse/issues/5986))
  - Include missing OpenTracing contexts in outbout replication requests. ([\#5982](https://github.com/matrix-org/synapse/issues/5982))
  - Fix sending of EDUs when OpenTracing is enabled with an empty whitelist. ([\#5984](https://github.com/matrix-org/synapse/issues/5984))
  - Fix invalid references to None while OpenTracing if the log context slips. ([\#5988](https://github.com/matrix-org/synapse/issues/5988), [\#5991](https://github.com/matrix-org/synapse/issues/5991))
  - OpenTracing for room and e2e keys. ([\#5855](https://github.com/matrix-org/synapse/issues/5855))
  - Add OpenTracing span over HTTP push processing. ([\#6003](https://github.com/matrix-org/synapse/issues/6003))
- Add an admin API to purge old rooms from the database. ([\#5845](https://github.com/matrix-org/synapse/issues/5845))
- Retry well-known lookups if we have recently seen a valid well-known record for the server. ([\#5850](https://github.com/matrix-org/synapse/issues/5850))
- Add support for filtered room-directory search requests over federation ([MSC2197](https://github.com/matrix-org/matrix-doc/pull/2197), in order to allow upcoming room directory query performance improvements. ([\#5859](https://github.com/matrix-org/synapse/issues/5859))
- Correctly retry all hosts returned from SRV when we fail to connect. ([\#5864](https://github.com/matrix-org/synapse/issues/5864))
- Add admin API endpoint for setting whether or not a user is a server administrator. ([\#5878](https://github.com/matrix-org/synapse/issues/5878))
- Enable cleaning up extremities with dummy events by default to prevent undue build up of forward extremities. ([\#5884](https://github.com/matrix-org/synapse/issues/5884))
- Add config option to sign remote key query responses with a separate key. ([\#5895](https://github.com/matrix-org/synapse/issues/5895))
- Add support for config templating. ([\#5900](https://github.com/matrix-org/synapse/issues/5900))
- Users with the type of "support" or "bot" are no longer required to consent. ([\#5902](https://github.com/matrix-org/synapse/issues/5902))
- Let synctl accept a directory of config files. ([\#5904](https://github.com/matrix-org/synapse/issues/5904))
- Increase max display name size to 256. ([\#5906](https://github.com/matrix-org/synapse/issues/5906))
- Add admin API endpoint for getting whether or not a user is a server administrator. ([\#5914](https://github.com/matrix-org/synapse/issues/5914))
- Redact events in the database that have been redacted for a week. ([\#5934](https://github.com/matrix-org/synapse/issues/5934))
- New prometheus metrics:
  - `synapse_federation_known_servers`: represents the total number of servers your server knows about (i.e. is in rooms with), including itself. Enable by setting `metrics_flags.known_servers` to True in the configuration.([\#5981](https://github.com/matrix-org/synapse/issues/5981))
  - `synapse_build_info`: exposes the Python version, OS version, and Synapse version of the running server. ([\#6005](https://github.com/matrix-org/synapse/issues/6005))
- Give appropriate exit codes when synctl fails. ([\#5992](https://github.com/matrix-org/synapse/issues/5992))
- Apply the federation blacklist to requests to identity servers. ([\#6000](https://github.com/matrix-org/synapse/issues/6000))
- Add `report_stats_endpoint` option to configure where stats are reported to, if enabled. Contributed by @Sorunome. ([\#6012](https://github.com/matrix-org/synapse/issues/6012))
- Add config option to increase ratelimits for room admins redacting messages. ([\#6015](https://github.com/matrix-org/synapse/issues/6015))
- Stop sending federation transactions to servers which have been down for a long time. ([\#6026](https://github.com/matrix-org/synapse/issues/6026))
- Make the process for mapping SAML2 users to matrix IDs more flexible. ([\#6037](https://github.com/matrix-org/synapse/issues/6037))
- Return a clearer error message when a timeout occurs when attempting to contact an identity server. ([\#6073](https://github.com/matrix-org/synapse/issues/6073))
- Prevent password reset's submit_token endpoint from accepting trailing slashes. ([\#6074](https://github.com/matrix-org/synapse/issues/6074))
- Return 403 on `/register/available` if registration has been disabled. ([\#6082](https://github.com/matrix-org/synapse/issues/6082))
- Explicitly log when a homeserver does not have the `trusted_key_servers` config field configured. ([\#6090](https://github.com/matrix-org/synapse/issues/6090))
- Add support for pruning old rows in `user_ips` table. ([\#6098](https://github.com/matrix-org/synapse/issues/6098))

Bugfixes
--------

- Don't create broken room when `power_level_content_override.users` does not contain `creator_id`. ([\#5633](https://github.com/matrix-org/synapse/issues/5633))
- Fix database index so that different backup versions can have the same sessions. ([\#5857](https://github.com/matrix-org/synapse/issues/5857))
- Fix Synapse looking for config options `password_reset_failure_template` and `password_reset_success_template`, when they are actually `password_reset_template_failure_html`, `password_reset_template_success_html`. ([\#5863](https://github.com/matrix-org/synapse/issues/5863))
- Fix stack overflow when recovering an appservice which had an outage. ([\#5885](https://github.com/matrix-org/synapse/issues/5885))
- Fix error message which referred to `public_base_url` instead of `public_baseurl`. Thanks to @aaronraimist for the fix! ([\#5909](https://github.com/matrix-org/synapse/issues/5909))
- Fix 404 for thumbnail download when `dynamic_thumbnails` is `false` and the thumbnail was dynamically generated. Fix reported by rkfg. ([\#5915](https://github.com/matrix-org/synapse/issues/5915))
- Fix a cache-invalidation bug for worker-based deployments. ([\#5920](https://github.com/matrix-org/synapse/issues/5920))
- Fix admin API for listing media in a room not being available with an external media repo. ([\#5966](https://github.com/matrix-org/synapse/issues/5966))
- Fix list media admin API always returning an error. ([\#5967](https://github.com/matrix-org/synapse/issues/5967))
- Fix room and user stats tracking. ([\#5971](https://github.com/matrix-org/synapse/issues/5971), [\#5998](https://github.com/matrix-org/synapse/issues/5998), [\#6029](https://github.com/matrix-org/synapse/issues/6029))
- Return a `M_MISSING_PARAM` if `sid` is not provided to `/account/3pid`. ([\#5995](https://github.com/matrix-org/synapse/issues/5995))
- `federation_certificate_verification_whitelist` now will not cause `TypeErrors` to be raised (a regression in 1.3). Additionally, it now supports internationalised domain names in their non-canonical representation. ([\#5996](https://github.com/matrix-org/synapse/issues/5996))
- Only count real users when checking for auto-creation of auto-join room. ([\#6004](https://github.com/matrix-org/synapse/issues/6004))
- Ensure support users can be registered even if MAU limit is reached. ([\#6020](https://github.com/matrix-org/synapse/issues/6020))
- Fix bug where login error was shown incorrectly on SSO fallback login. ([\#6024](https://github.com/matrix-org/synapse/issues/6024))
- Fix bug in calculating the federation retry backoff period. ([\#6025](https://github.com/matrix-org/synapse/issues/6025))
- Prevent exceptions being logged when extremity-cleanup events fail due to lack of user consent to the terms of service. ([\#6053](https://github.com/matrix-org/synapse/issues/6053))
- Remove POST method from password-reset `submit_token` endpoint until we implement `submit_url` functionality. ([\#6056](https://github.com/matrix-org/synapse/issues/6056))
- Fix logcontext spam on non-Linux platforms. ([\#6059](https://github.com/matrix-org/synapse/issues/6059))
- Ensure query parameters in email validation links are URL-encoded. ([\#6063](https://github.com/matrix-org/synapse/issues/6063))
- Fix a bug which caused SAML attribute maps to be overridden by defaults. ([\#6069](https://github.com/matrix-org/synapse/issues/6069))
- Fix the logged number of updated items for the `users_set_deactivated_flag` background update. ([\#6092](https://github.com/matrix-org/synapse/issues/6092))
- Add `sid` to `next_link` for email validation. ([\#6097](https://github.com/matrix-org/synapse/issues/6097))
- Threepid validity checks on msisdns should not be dependent on `threepid_behaviour_email`. ([\#6104](https://github.com/matrix-org/synapse/issues/6104))
- Ensure that servers which are not configured to support email address verification do not offer it in the registration flows. ([\#6107](https://github.com/matrix-org/synapse/issues/6107))


Updates to the Docker image
---------------------------

- Avoid changing `UID/GID` if they are already correct. ([\#5970](https://github.com/matrix-org/synapse/issues/5970))
- Provide `SYNAPSE_WORKER` envvar to specify python module. ([\#6058](https://github.com/matrix-org/synapse/issues/6058))


Improved Documentation
----------------------

- Convert documentation to markdown (from rst) ([\#5849](https://github.com/matrix-org/synapse/issues/5849))
- Update `INSTALL.md` to say that Python 2 is no longer supported. ([\#5953](https://github.com/matrix-org/synapse/issues/5953))
- Add developer documentation for using SAML2. ([\#6032](https://github.com/matrix-org/synapse/issues/6032))
- Add some notes on rolling back to v1.3.1. ([\#6049](https://github.com/matrix-org/synapse/issues/6049))
- Update the upgrade notes. ([\#6050](https://github.com/matrix-org/synapse/issues/6050))


Deprecations and Removals
-------------------------

- Remove shared-secret registration from `/_matrix/client/r0/register` endpoint. Contributed by Awesome Technologies Innovationslabor GmbH. ([\#5877](https://github.com/matrix-org/synapse/issues/5877))
- Deprecate the `trusted_third_party_id_servers` option. ([\#5875](https://github.com/matrix-org/synapse/issues/5875))


Internal Changes
----------------

- Lay the groundwork for structured logging output. ([\#5680](https://github.com/matrix-org/synapse/issues/5680))
- Retry well-known lookup before the cache expires, giving a grace period where the remote well-known can be down but we still use the old result. ([\#5844](https://github.com/matrix-org/synapse/issues/5844))
- Remove log line for debugging issue #5407. ([\#5860](https://github.com/matrix-org/synapse/issues/5860))
- Refactor the Appservice scheduler code. ([\#5886](https://github.com/matrix-org/synapse/issues/5886))
- Compatibility with v2 Identity Service APIs other than /lookup. ([\#5892](https://github.com/matrix-org/synapse/issues/5892), [\#6013](https://github.com/matrix-org/synapse/issues/6013))
- Stop populating some unused tables. ([\#5893](https://github.com/matrix-org/synapse/issues/5893), [\#6047](https://github.com/matrix-org/synapse/issues/6047))
- Add missing index on `users_in_public_rooms` to improve the performance of directory queries. ([\#5894](https://github.com/matrix-org/synapse/issues/5894))
- Improve the logging when we have an error when fetching signing keys. ([\#5896](https://github.com/matrix-org/synapse/issues/5896))
- Add support for database engine-specific schema deltas, based on file extension. ([\#5911](https://github.com/matrix-org/synapse/issues/5911))
- Update Buildkite pipeline to use plugins instead of buildkite-agent commands. ([\#5922](https://github.com/matrix-org/synapse/issues/5922))
- Add link in sample config to the logging config schema. ([\#5926](https://github.com/matrix-org/synapse/issues/5926))
- Remove unnecessary parentheses in return statements. ([\#5931](https://github.com/matrix-org/synapse/issues/5931))
- Remove unused `jenkins/prepare_sytest.sh` file. ([\#5938](https://github.com/matrix-org/synapse/issues/5938))
- Move Buildkite pipeline config to the pipelines repo. ([\#5943](https://github.com/matrix-org/synapse/issues/5943))
- Remove unnecessary return statements in the codebase which were the result of a regex run. ([\#5962](https://github.com/matrix-org/synapse/issues/5962))
- Remove left-over methods from v1 registration API. ([\#5963](https://github.com/matrix-org/synapse/issues/5963))
- Cleanup event auth type initialisation. ([\#5975](https://github.com/matrix-org/synapse/issues/5975))
- Clean up dependency checking at setup. ([\#5989](https://github.com/matrix-org/synapse/issues/5989))
- Update OpenTracing docs to use the unified `trace` method. ([\#5776](https://github.com/matrix-org/synapse/issues/5776))
- Small refactor of function arguments and docstrings in` RoomMemberHandler`. ([\#6009](https://github.com/matrix-org/synapse/issues/6009))
- Remove unused `origin` argument on `FederationHandler.add_display_name_to_third_party_invite`. ([\#6010](https://github.com/matrix-org/synapse/issues/6010))
- Add a `failure_ts` column to the `destinations` database table. ([\#6016](https://github.com/matrix-org/synapse/issues/6016), [\#6072](https://github.com/matrix-org/synapse/issues/6072))
- Clean up some code in the retry logic. ([\#6017](https://github.com/matrix-org/synapse/issues/6017))
- Fix the structured logging tests stomping on the global log configuration for subsequent tests. ([\#6023](https://github.com/matrix-org/synapse/issues/6023))
- Clean up the sample config for SAML authentication. ([\#6064](https://github.com/matrix-org/synapse/issues/6064))
- Change mailer logging to reflect Synapse doesn't just do chat notifications by email now. ([\#6075](https://github.com/matrix-org/synapse/issues/6075))
- Move last-seen info into devices table. ([\#6089](https://github.com/matrix-org/synapse/issues/6089))
- Remove unused parameter to `get_user_id_by_threepid`. ([\#6099](https://github.com/matrix-org/synapse/issues/6099))
- Refactor the user-interactive auth handling. ([\#6105](https://github.com/matrix-org/synapse/issues/6105))
- Refactor code for calculating registration flows. ([\#6106](https://github.com/matrix-org/synapse/issues/6106))


Synapse 1.3.1 (2019-08-17)
==========================

Features
--------

- Drop hard dependency on `sdnotify` python package. ([\#5871](https://github.com/matrix-org/synapse/issues/5871))


Bugfixes
--------

- Fix startup issue (hang on ACME provisioning) due to ordering of Twisted reactor startup. Thanks to @chrismoos for supplying the fix. ([\#5867](https://github.com/matrix-org/synapse/issues/5867))


Synapse 1.3.0 (2019-08-15)
==========================

Bugfixes
--------

- Fix 500 Internal Server Error on `publicRooms` when the public room list was
  cached. ([\#5851](https://github.com/matrix-org/synapse/issues/5851))


Synapse 1.3.0rc1 (2019-08-13)
==========================

Features
--------

- Use `M_USER_DEACTIVATED` instead of `M_UNKNOWN` for errcode when a deactivated user attempts to login. ([\#5686](https://github.com/matrix-org/synapse/issues/5686))
- Add sd_notify hooks to ease systemd integration and allows usage of Type=Notify. ([\#5732](https://github.com/matrix-org/synapse/issues/5732))
- Synapse will no longer serve any media repo admin endpoints when `enable_media_repo` is set to False in the configuration. If a media repo worker is used, the admin APIs relating to the media repo will be served from it instead. ([\#5754](https://github.com/matrix-org/synapse/issues/5754), [\#5848](https://github.com/matrix-org/synapse/issues/5848))
- Synapse can now be configured to not join remote rooms of a given "complexity" (currently, state events) over federation. This option can be used to prevent adverse performance on resource-constrained homeservers. ([\#5783](https://github.com/matrix-org/synapse/issues/5783))
- Allow defining HTML templates to serve the user on account renewal attempt when using the account validity feature. ([\#5807](https://github.com/matrix-org/synapse/issues/5807))


Bugfixes
--------

- Fix UISIs during homeserver outage. ([\#5693](https://github.com/matrix-org/synapse/issues/5693), [\#5789](https://github.com/matrix-org/synapse/issues/5789))
- Fix stack overflow in server key lookup code. ([\#5724](https://github.com/matrix-org/synapse/issues/5724))
- start.sh no longer uses deprecated cli option. ([\#5725](https://github.com/matrix-org/synapse/issues/5725))
- Log when we receive an event receipt from an unexpected origin. ([\#5743](https://github.com/matrix-org/synapse/issues/5743))
- Fix debian packaging scripts to correctly build sid packages. ([\#5775](https://github.com/matrix-org/synapse/issues/5775))
- Correctly handle redactions of redactions. ([\#5788](https://github.com/matrix-org/synapse/issues/5788))
- Return 404 instead of 403 when accessing /rooms/{roomId}/event/{eventId} for an event without the appropriate permissions. ([\#5798](https://github.com/matrix-org/synapse/issues/5798))
- Fix check that tombstone is a state event in push rules. ([\#5804](https://github.com/matrix-org/synapse/issues/5804))
- Fix error when trying to login as a deactivated user when using a worker to handle login. ([\#5806](https://github.com/matrix-org/synapse/issues/5806))
- Fix bug where user `/sync` stream could get wedged in rare circumstances. ([\#5825](https://github.com/matrix-org/synapse/issues/5825))
- The purge_remote_media.sh script was fixed. ([\#5839](https://github.com/matrix-org/synapse/issues/5839))


Deprecations and Removals
-------------------------

- Synapse now no longer accepts the `-v`/`--verbose`, `-f`/`--log-file`, or `--log-config` command line flags, and removes the deprecated `verbose` and `log_file` configuration file options. Users of these options should migrate their options into the dedicated log configuration. ([\#5678](https://github.com/matrix-org/synapse/issues/5678), [\#5729](https://github.com/matrix-org/synapse/issues/5729))
- Remove non-functional 'expire_access_token' setting. ([\#5782](https://github.com/matrix-org/synapse/issues/5782))


Internal Changes
----------------

- Make Jaeger fully configurable. ([\#5694](https://github.com/matrix-org/synapse/issues/5694))
- Add precautionary measures to prevent future abuse of `window.opener` in default welcome page. ([\#5695](https://github.com/matrix-org/synapse/issues/5695))
- Reduce database IO usage by optimising queries for current membership. ([\#5706](https://github.com/matrix-org/synapse/issues/5706), [\#5738](https://github.com/matrix-org/synapse/issues/5738), [\#5746](https://github.com/matrix-org/synapse/issues/5746), [\#5752](https://github.com/matrix-org/synapse/issues/5752), [\#5770](https://github.com/matrix-org/synapse/issues/5770), [\#5774](https://github.com/matrix-org/synapse/issues/5774), [\#5792](https://github.com/matrix-org/synapse/issues/5792), [\#5793](https://github.com/matrix-org/synapse/issues/5793))
- Improve caching when fetching `get_filtered_current_state_ids`. ([\#5713](https://github.com/matrix-org/synapse/issues/5713))
- Don't accept opentracing data from clients. ([\#5715](https://github.com/matrix-org/synapse/issues/5715))
- Speed up PostgreSQL unit tests in CI. ([\#5717](https://github.com/matrix-org/synapse/issues/5717))
- Update the coding style document. ([\#5719](https://github.com/matrix-org/synapse/issues/5719))
- Improve database query performance when recording retry intervals for remote hosts. ([\#5720](https://github.com/matrix-org/synapse/issues/5720))
- Add a set of opentracing utils. ([\#5722](https://github.com/matrix-org/synapse/issues/5722))
- Cache result of get_version_string to reduce overhead of `/version` federation requests. ([\#5730](https://github.com/matrix-org/synapse/issues/5730))
- Return 'user_type' in admin API user endpoints results. ([\#5731](https://github.com/matrix-org/synapse/issues/5731))
- Don't package the sytest test blacklist file. ([\#5733](https://github.com/matrix-org/synapse/issues/5733))
- Replace uses of returnValue with plain return, as returnValue is not needed on Python 3. ([\#5736](https://github.com/matrix-org/synapse/issues/5736))
- Blacklist some flakey tests in worker mode. ([\#5740](https://github.com/matrix-org/synapse/issues/5740))
- Fix some error cases in the caching layer. ([\#5749](https://github.com/matrix-org/synapse/issues/5749))
- Add a prometheus metric for pending cache lookups. ([\#5750](https://github.com/matrix-org/synapse/issues/5750))
- Stop trying to fetch events with event_id=None. ([\#5753](https://github.com/matrix-org/synapse/issues/5753))
- Convert RedactionTestCase to modern test style. ([\#5768](https://github.com/matrix-org/synapse/issues/5768))
- Allow looping calls to be given arguments. ([\#5780](https://github.com/matrix-org/synapse/issues/5780))
- Set the logs emitted when checking typing and presence timeouts to DEBUG level, not INFO. ([\#5785](https://github.com/matrix-org/synapse/issues/5785))
- Remove DelayedCall debugging from the test suite, as it is no longer required in the vast majority of Synapse's tests. ([\#5787](https://github.com/matrix-org/synapse/issues/5787))
- Remove some spurious exceptions from the logs where we failed to talk to a remote server. ([\#5790](https://github.com/matrix-org/synapse/issues/5790))
- Improve performance when making `.well-known` requests by sharing the SSL options between requests. ([\#5794](https://github.com/matrix-org/synapse/issues/5794))
- Disable codecov GitHub comments on PRs. ([\#5796](https://github.com/matrix-org/synapse/issues/5796))
- Don't allow clients to send tombstone events that reference the room it's sent in. ([\#5801](https://github.com/matrix-org/synapse/issues/5801))
- Deny redactions of events sent in a different room. ([\#5802](https://github.com/matrix-org/synapse/issues/5802))
- Deny sending well known state types as non-state events. ([\#5805](https://github.com/matrix-org/synapse/issues/5805))
- Handle incorrectly encoded query params correctly by returning a 400. ([\#5808](https://github.com/matrix-org/synapse/issues/5808))
- Handle pusher being deleted during processing rather than logging an exception. ([\#5809](https://github.com/matrix-org/synapse/issues/5809))
- Return 502 not 500 when failing to reach any remote server. ([\#5810](https://github.com/matrix-org/synapse/issues/5810))
- Reduce global pauses in the events stream caused by expensive state resolution during persistence. ([\#5826](https://github.com/matrix-org/synapse/issues/5826))
- Add a lower bound to well-known lookup cache time to avoid repeated lookups. ([\#5836](https://github.com/matrix-org/synapse/issues/5836))
- Whitelist history visbility sytests in worker mode tests. ([\#5843](https://github.com/matrix-org/synapse/issues/5843))


Synapse 1.2.1 (2019-07-26)
==========================

Security update
---------------

This release includes *four* security fixes:

- Prevent an attack where a federated server could send redactions for arbitrary events in v1 and v2 rooms. ([\#5767](https://github.com/matrix-org/synapse/issues/5767))
- Prevent a denial-of-service attack where cycles of redaction events would make Synapse spin infinitely. Thanks to `@lrizika:matrix.org` for identifying and responsibly disclosing this issue. ([0f2ecb961](https://github.com/matrix-org/synapse/commit/0f2ecb961))
- Prevent an attack where users could be joined or parted from public rooms without their consent. Thanks to @dylangerdaly for identifying and responsibly disclosing this issue. ([\#5744](https://github.com/matrix-org/synapse/issues/5744))
- Fix a vulnerability where a federated server could spoof read-receipts from
  users on other servers. Thanks to @dylangerdaly for identifying this issue too. ([\#5743](https://github.com/matrix-org/synapse/issues/5743))

Additionally, the following fix was in Synapse **1.2.0**, but was not correctly
identified during the original release:

- It was possible for a room moderator to send a redaction for an `m.room.create` event, which would downgrade the room to version 1. Thanks to `/dev/ponies` for identifying and responsibly disclosing this issue! ([\#5701](https://github.com/matrix-org/synapse/issues/5701))

Synapse 1.2.0 (2019-07-25)
==========================

No significant changes.


Synapse 1.2.0rc2 (2019-07-24)
=============================

Bugfixes
--------

- Fix a regression introduced in v1.2.0rc1 which led to incorrect labels on some prometheus metrics. ([\#5734](https://github.com/matrix-org/synapse/issues/5734))


Synapse 1.2.0rc1 (2019-07-22)
=============================

Security fixes
--------------

This update included a security fix which was initially incorrectly flagged as
a regular bug fix.

- It was possible for a room moderator to send a redaction for an `m.room.create` event, which would downgrade the room to version 1. Thanks to `/dev/ponies` for identifying and responsibly disclosing this issue! ([\#5701](https://github.com/matrix-org/synapse/issues/5701))

Features
--------

- Add support for opentracing. ([\#5544](https://github.com/matrix-org/synapse/issues/5544), [\#5712](https://github.com/matrix-org/synapse/issues/5712))
- Add ability to pull all locally stored events out of synapse that a particular user can see. ([\#5589](https://github.com/matrix-org/synapse/issues/5589))
- Add a basic admin command app to allow server operators to run Synapse admin commands separately from the main production instance. ([\#5597](https://github.com/matrix-org/synapse/issues/5597))
- Add `sender` and `origin_server_ts` fields to `m.replace`. ([\#5613](https://github.com/matrix-org/synapse/issues/5613))
- Add default push rule to ignore reactions. ([\#5623](https://github.com/matrix-org/synapse/issues/5623))
- Include the original event when asking for its relations. ([\#5626](https://github.com/matrix-org/synapse/issues/5626))
- Implement `session_lifetime` configuration option, after which access tokens will expire. ([\#5660](https://github.com/matrix-org/synapse/issues/5660))
- Return "This account has been deactivated" when a deactivated user tries to login. ([\#5674](https://github.com/matrix-org/synapse/issues/5674))
- Enable aggregations support by default ([\#5714](https://github.com/matrix-org/synapse/issues/5714))


Bugfixes
--------

- Fix 'utime went backwards' errors on daemonization. ([\#5609](https://github.com/matrix-org/synapse/issues/5609))
- Various minor fixes to the federation request rate limiter. ([\#5621](https://github.com/matrix-org/synapse/issues/5621))
- Forbid viewing relations on an event once it has been redacted. ([\#5629](https://github.com/matrix-org/synapse/issues/5629))
- Fix requests to the `/store_invite` endpoint of identity servers being sent in the wrong format. ([\#5638](https://github.com/matrix-org/synapse/issues/5638))
- Fix newly-registered users not being able to lookup their own profile without joining a room. ([\#5644](https://github.com/matrix-org/synapse/issues/5644))
- Fix bug in #5626 that prevented the original_event field from actually having the contents of the original event in a call to `/relations`. ([\#5654](https://github.com/matrix-org/synapse/issues/5654))
- Fix 3PID bind requests being sent to identity servers as `application/x-form-www-urlencoded` data, which is deprecated. ([\#5658](https://github.com/matrix-org/synapse/issues/5658))
- Fix some problems with authenticating redactions in recent room versions. ([\#5699](https://github.com/matrix-org/synapse/issues/5699), [\#5700](https://github.com/matrix-org/synapse/issues/5700), [\#5707](https://github.com/matrix-org/synapse/issues/5707))


Updates to the Docker image
---------------------------

- Base Docker image on a newer Alpine Linux version (3.8 -> 3.10). ([\#5619](https://github.com/matrix-org/synapse/issues/5619))
- Add missing space in default logging file format generated by the Docker image. ([\#5620](https://github.com/matrix-org/synapse/issues/5620))


Improved Documentation
----------------------

- Add information about nginx normalisation to reverse_proxy.rst. Contributed by @skalarproduktraum - thanks! ([\#5397](https://github.com/matrix-org/synapse/issues/5397))
- --no-pep517 should be --no-use-pep517 in the documentation to setup the development environment. ([\#5651](https://github.com/matrix-org/synapse/issues/5651))
- Improvements to Postgres setup instructions. Contributed by @Lrizika - thanks! ([\#5661](https://github.com/matrix-org/synapse/issues/5661))
- Minor tweaks to postgres documentation. ([\#5675](https://github.com/matrix-org/synapse/issues/5675))


Deprecations and Removals
-------------------------

- Remove support for the `invite_3pid_guest` configuration setting. ([\#5625](https://github.com/matrix-org/synapse/issues/5625))


Internal Changes
----------------

- Move logging code out of `synapse.util` and into `synapse.logging`. ([\#5606](https://github.com/matrix-org/synapse/issues/5606), [\#5617](https://github.com/matrix-org/synapse/issues/5617))
- Add a blacklist file to the repo to blacklist certain sytests from failing CI. ([\#5611](https://github.com/matrix-org/synapse/issues/5611))
- Make runtime errors surrounding password reset emails much clearer. ([\#5616](https://github.com/matrix-org/synapse/issues/5616))
- Remove dead code for persiting outgoing federation transactions. ([\#5622](https://github.com/matrix-org/synapse/issues/5622))
- Add `lint.sh` to the scripts-dev folder which will run all linting steps required by CI. ([\#5627](https://github.com/matrix-org/synapse/issues/5627))
- Move RegistrationHandler.get_or_create_user to test code. ([\#5628](https://github.com/matrix-org/synapse/issues/5628))
- Add some more common python virtual-environment paths to the black exclusion list. ([\#5630](https://github.com/matrix-org/synapse/issues/5630))
- Some counter metrics exposed over Prometheus have been renamed, with the old names preserved for backwards compatibility and deprecated. See `docs/metrics-howto.rst` for details. ([\#5636](https://github.com/matrix-org/synapse/issues/5636))
- Unblacklist some user_directory sytests. ([\#5637](https://github.com/matrix-org/synapse/issues/5637))
- Factor out some redundant code in the login implementation. ([\#5639](https://github.com/matrix-org/synapse/issues/5639))
- Update ModuleApi to avoid register(generate_token=True). ([\#5640](https://github.com/matrix-org/synapse/issues/5640))
- Remove access-token support from `RegistrationHandler.register`, and rename it. ([\#5641](https://github.com/matrix-org/synapse/issues/5641))
- Remove access-token support from `RegistrationStore.register`, and rename it. ([\#5642](https://github.com/matrix-org/synapse/issues/5642))
- Improve logging for auto-join when a new user is created. ([\#5643](https://github.com/matrix-org/synapse/issues/5643))
- Remove unused and unnecessary check for FederationDeniedError in _exception_to_failure. ([\#5645](https://github.com/matrix-org/synapse/issues/5645))
- Fix a small typo in a code comment. ([\#5655](https://github.com/matrix-org/synapse/issues/5655))
- Clean up exception handling around client access tokens. ([\#5656](https://github.com/matrix-org/synapse/issues/5656))
- Add a mechanism for per-test homeserver configuration in the unit tests. ([\#5657](https://github.com/matrix-org/synapse/issues/5657))
- Inline issue_access_token. ([\#5659](https://github.com/matrix-org/synapse/issues/5659))
- Update the sytest BuildKite configuration to checkout Synapse in `/src`. ([\#5664](https://github.com/matrix-org/synapse/issues/5664))
- Add a `docker` type to the towncrier configuration. ([\#5673](https://github.com/matrix-org/synapse/issues/5673))
- Convert `synapse.federation.transport.server` to `async`. Might improve some stack traces. ([\#5689](https://github.com/matrix-org/synapse/issues/5689))
- Documentation for opentracing. ([\#5703](https://github.com/matrix-org/synapse/issues/5703))


Synapse 1.1.0 (2019-07-04)
==========================

As of v1.1.0, Synapse no longer supports Python 2, nor Postgres version 9.4.
See the [upgrade notes](docs/upgrade.md#upgrading-to-v110) for more details.

This release also deprecates the use of environment variables to configure the
docker image. See the [docker README](https://github.com/matrix-org/synapse/blob/release-v1.1.0/docker/README.md#legacy-dynamic-configuration-file-support)
for more details.

No changes since 1.1.0rc2.


Synapse 1.1.0rc2 (2019-07-03)
=============================

Bugfixes
--------

- Fix regression in 1.1rc1 where OPTIONS requests to the media repo would fail. ([\#5593](https://github.com/matrix-org/synapse/issues/5593))
- Removed the `SYNAPSE_SMTP_*` docker container environment variables. Using these environment variables prevented the docker container from starting in Synapse v1.0, even though they didn't actually allow any functionality anyway. ([\#5596](https://github.com/matrix-org/synapse/issues/5596))
- Fix a number of "Starting txn from sentinel context" warnings. ([\#5605](https://github.com/matrix-org/synapse/issues/5605))


Internal Changes
----------------

- Update github templates. ([\#5552](https://github.com/matrix-org/synapse/issues/5552))


Synapse 1.1.0rc1 (2019-07-02)
=============================

As of v1.1.0, Synapse no longer supports Python 2, nor Postgres version 9.4.
See the [upgrade notes](docs/upgrade.md#upgrading-to-v110) for more details.

Features
--------

- Added possibilty to disable local password authentication. Contributed by Daniel Hoffend. ([\#5092](https://github.com/matrix-org/synapse/issues/5092))
- Add monthly active users to phonehome stats. ([\#5252](https://github.com/matrix-org/synapse/issues/5252))
- Allow expired user to trigger renewal email sending manually. ([\#5363](https://github.com/matrix-org/synapse/issues/5363))
- Statistics on forward extremities per room are now exposed via Prometheus. ([\#5384](https://github.com/matrix-org/synapse/issues/5384), [\#5458](https://github.com/matrix-org/synapse/issues/5458), [\#5461](https://github.com/matrix-org/synapse/issues/5461))
- Add --no-daemonize option to run synapse in the foreground, per issue #4130. Contributed by Soham Gumaste. ([\#5412](https://github.com/matrix-org/synapse/issues/5412), [\#5587](https://github.com/matrix-org/synapse/issues/5587))
- Fully support SAML2 authentication. Contributed by [Alexander Trost](https://github.com/galexrt) - thank you! ([\#5422](https://github.com/matrix-org/synapse/issues/5422))
- Allow server admins to define implementations of extra rules for allowing or denying incoming events. ([\#5440](https://github.com/matrix-org/synapse/issues/5440), [\#5474](https://github.com/matrix-org/synapse/issues/5474), [\#5477](https://github.com/matrix-org/synapse/issues/5477))
- Add support for handling pagination APIs on client reader worker. ([\#5505](https://github.com/matrix-org/synapse/issues/5505), [\#5513](https://github.com/matrix-org/synapse/issues/5513), [\#5531](https://github.com/matrix-org/synapse/issues/5531))
- Improve help and cmdline option names for --generate-config options. ([\#5512](https://github.com/matrix-org/synapse/issues/5512))
- Allow configuration of the path used for ACME account keys. ([\#5516](https://github.com/matrix-org/synapse/issues/5516), [\#5521](https://github.com/matrix-org/synapse/issues/5521), [\#5522](https://github.com/matrix-org/synapse/issues/5522))
- Add --data-dir and --open-private-ports options. ([\#5524](https://github.com/matrix-org/synapse/issues/5524))
- Split public rooms directory auth config in two settings, in order to manage client auth independently from the federation part of it. Obsoletes the "restrict_public_rooms_to_local_users" configuration setting. If "restrict_public_rooms_to_local_users" is set in the config, Synapse will act as if both new options are enabled, i.e. require authentication through the client API and deny federation requests. ([\#5534](https://github.com/matrix-org/synapse/issues/5534))
- The minimum TLS version used for outgoing federation requests can now be set with `federation_client_minimum_tls_version`. ([\#5550](https://github.com/matrix-org/synapse/issues/5550))
- Optimise devices changed query to not pull unnecessary rows from the database, reducing database load. ([\#5559](https://github.com/matrix-org/synapse/issues/5559))
- Add new metrics for number of forward extremities being persisted and number of state groups involved in resolution. ([\#5476](https://github.com/matrix-org/synapse/issues/5476))

Bugfixes
--------

- Fix bug processing incoming events over federation if call to `/get_missing_events` fails. ([\#5042](https://github.com/matrix-org/synapse/issues/5042))
- Prevent more than one room upgrade happening simultaneously on the same room. ([\#5051](https://github.com/matrix-org/synapse/issues/5051))
- Fix a bug where running synapse_port_db would cause the account validity feature to fail because it didn't set the type of the email_sent column to boolean. ([\#5325](https://github.com/matrix-org/synapse/issues/5325))
- Warn about disabling email-based password resets when a reset occurs, and remove warning when someone attempts a phone-based reset. ([\#5387](https://github.com/matrix-org/synapse/issues/5387))
- Fix email notifications for unnamed rooms with multiple people. ([\#5388](https://github.com/matrix-org/synapse/issues/5388))
- Fix exceptions in federation reader worker caused by attempting to renew attestations, which should only happen on master worker. ([\#5389](https://github.com/matrix-org/synapse/issues/5389))
- Fix handling of failures fetching remote content to not log failures as exceptions. ([\#5390](https://github.com/matrix-org/synapse/issues/5390))
- Fix a bug where deactivated users could receive renewal emails if the account validity feature is on. ([\#5394](https://github.com/matrix-org/synapse/issues/5394))
- Fix missing invite state after exchanging 3PID invites over federaton. ([\#5464](https://github.com/matrix-org/synapse/issues/5464))
- Fix intermittent exceptions on Apple hardware. Also fix bug that caused database activity times to be under-reported in log lines. ([\#5498](https://github.com/matrix-org/synapse/issues/5498))
- Fix logging error when a tampered event is detected. ([\#5500](https://github.com/matrix-org/synapse/issues/5500))
- Fix bug where clients could tight loop calling `/sync` for a period. ([\#5507](https://github.com/matrix-org/synapse/issues/5507))
- Fix bug with `jinja2` preventing Synapse from starting. Users who had this problem should now simply need to run `pip install matrix-synapse`. ([\#5514](https://github.com/matrix-org/synapse/issues/5514))
- Fix a regression where homeservers on private IP addresses were incorrectly blacklisted. ([\#5523](https://github.com/matrix-org/synapse/issues/5523))
- Fixed m.login.jwt using unregistred user_id and added pyjwt>=1.6.4 as jwt conditional dependencies. Contributed by Pau Rodriguez-Estivill. ([\#5555](https://github.com/matrix-org/synapse/issues/5555), [\#5586](https://github.com/matrix-org/synapse/issues/5586))
- Fix a bug that would cause invited users to receive several emails for a single 3PID invite in case the inviter is rate limited. ([\#5576](https://github.com/matrix-org/synapse/issues/5576))


Updates to the Docker image
---------------------------
- Add ability to change Docker containers [timezone](https://en.wikipedia.org/wiki/List_of_tz_database_time_zones) with the `TZ` variable. ([\#5383](https://github.com/matrix-org/synapse/issues/5383))
- Update docker image to use Python 3.7. ([\#5546](https://github.com/matrix-org/synapse/issues/5546))
- Deprecate the use of environment variables for configuration, and make the use of a static configuration the default. ([\#5561](https://github.com/matrix-org/synapse/issues/5561), [\#5562](https://github.com/matrix-org/synapse/issues/5562), [\#5566](https://github.com/matrix-org/synapse/issues/5566), [\#5567](https://github.com/matrix-org/synapse/issues/5567))
- Increase default log level for docker image to INFO. It can still be changed by editing the generated log.config file. ([\#5547](https://github.com/matrix-org/synapse/issues/5547))
- Send synapse logs to the docker logging system, by default. ([\#5565](https://github.com/matrix-org/synapse/issues/5565))
- Open the non-TLS port by default. ([\#5568](https://github.com/matrix-org/synapse/issues/5568))
- Fix failure to start under docker with SAML support enabled. ([\#5490](https://github.com/matrix-org/synapse/issues/5490))
- Use a sensible location for data files when generating a config file. ([\#5563](https://github.com/matrix-org/synapse/issues/5563))


Deprecations and Removals
-------------------------

- Python 2.7 is no longer a supported platform. Synapse now requires Python 3.5+ to run. ([\#5425](https://github.com/matrix-org/synapse/issues/5425))
- PostgreSQL 9.4 is no longer supported. Synapse requires Postgres 9.5+ or above for Postgres support. ([\#5448](https://github.com/matrix-org/synapse/issues/5448))
- Remove support for cpu_affinity setting. ([\#5525](https://github.com/matrix-org/synapse/issues/5525))


Improved Documentation
----------------------
- Improve README section on performance troubleshooting. ([\#4276](https://github.com/matrix-org/synapse/issues/4276))
- Add information about how to install and run `black` on the codebase to code_style.rst. ([\#5537](https://github.com/matrix-org/synapse/issues/5537))
- Improve install docs on choosing server_name. ([\#5558](https://github.com/matrix-org/synapse/issues/5558))


Internal Changes
----------------

- Add logging to 3pid invite signature verification. ([\#5015](https://github.com/matrix-org/synapse/issues/5015))
- Update example haproxy config to a more compatible setup. ([\#5313](https://github.com/matrix-org/synapse/issues/5313))
- Track deactivated accounts in the database. ([\#5378](https://github.com/matrix-org/synapse/issues/5378), [\#5465](https://github.com/matrix-org/synapse/issues/5465), [\#5493](https://github.com/matrix-org/synapse/issues/5493))
- Clean up code for sending federation EDUs. ([\#5381](https://github.com/matrix-org/synapse/issues/5381))
- Add a sponsor button to the repo. ([\#5382](https://github.com/matrix-org/synapse/issues/5382), [\#5386](https://github.com/matrix-org/synapse/issues/5386))
- Don't log non-200 responses from federation queries as exceptions. ([\#5383](https://github.com/matrix-org/synapse/issues/5383))
- Update Python syntax in contrib/ to Python 3. ([\#5446](https://github.com/matrix-org/synapse/issues/5446))
- Update federation_client dev script to support `.well-known` and work with python3. ([\#5447](https://github.com/matrix-org/synapse/issues/5447))
- SyTest has been moved to Buildkite. ([\#5459](https://github.com/matrix-org/synapse/issues/5459))
- Demo script now uses python3. ([\#5460](https://github.com/matrix-org/synapse/issues/5460))
- Synapse can now handle RestServlets that return coroutines. ([\#5475](https://github.com/matrix-org/synapse/issues/5475), [\#5585](https://github.com/matrix-org/synapse/issues/5585))
- The demo servers talk to each other again. ([\#5478](https://github.com/matrix-org/synapse/issues/5478))
- Add an EXPERIMENTAL config option to try and periodically clean up extremities by sending dummy events. ([\#5480](https://github.com/matrix-org/synapse/issues/5480))
- Synapse's codebase is now formatted by `black`. ([\#5482](https://github.com/matrix-org/synapse/issues/5482))
- Some cleanups and sanity-checking in the CPU and database metrics. ([\#5499](https://github.com/matrix-org/synapse/issues/5499))
- Improve email notification logging. ([\#5502](https://github.com/matrix-org/synapse/issues/5502))
- Fix "Unexpected entry in 'full_schemas'" log warning. ([\#5509](https://github.com/matrix-org/synapse/issues/5509))
- Improve logging when generating config files. ([\#5510](https://github.com/matrix-org/synapse/issues/5510))
- Refactor and clean up Config parser for maintainability. ([\#5511](https://github.com/matrix-org/synapse/issues/5511))
- Make the config clearer in that email.template_dir is relative to the Synapse's root directory, not the `synapse/` folder within it. ([\#5543](https://github.com/matrix-org/synapse/issues/5543))
- Update v1.0.0 release changelog to include more information about changes to password resets. ([\#5545](https://github.com/matrix-org/synapse/issues/5545))
- Remove non-functioning check_event_hash.py dev script. ([\#5548](https://github.com/matrix-org/synapse/issues/5548))
- Synapse will now only allow TLS v1.2 connections when serving federation, if it terminates TLS. As Synapse's allowed ciphers were only able to be used in TLSv1.2 before, this does not change behaviour. ([\#5550](https://github.com/matrix-org/synapse/issues/5550))
- Logging when running GC collection on generation 0 is now at the DEBUG level, not INFO. ([\#5557](https://github.com/matrix-org/synapse/issues/5557))
- Reduce the amount of stuff we send in the docker context. ([\#5564](https://github.com/matrix-org/synapse/issues/5564))
- Point the reverse links in the Purge History contrib scripts at the intended location. ([\#5570](https://github.com/matrix-org/synapse/issues/5570))


Synapse 1.0.0 (2019-06-11)
==========================

Bugfixes
--------

- Fix bug where attempting to send transactions with large number of EDUs can fail. ([\#5418](https://github.com/matrix-org/synapse/issues/5418))


Improved Documentation
----------------------

- Expand the federation guide to include relevant content from the MSC1711 FAQ ([\#5419](https://github.com/matrix-org/synapse/issues/5419))


Internal Changes
----------------

- Move password reset links to /_matrix/client/unstable namespace. ([\#5424](https://github.com/matrix-org/synapse/issues/5424))


Synapse 1.0.0rc3 (2019-06-10)
=============================

Security: Fix authentication bug introduced in 1.0.0rc1. Please upgrade to rc3 immediately


Synapse 1.0.0rc2 (2019-06-10)
=============================

Bugfixes
--------

- Remove redundant warning about key server response validation. ([\#5392](https://github.com/matrix-org/synapse/issues/5392))
- Fix bug where old keys stored in the database with a null valid until timestamp caused all verification requests for that key to fail. ([\#5415](https://github.com/matrix-org/synapse/issues/5415))
- Fix excessive memory using with default `federation_verify_certificates: true` configuration. ([\#5417](https://github.com/matrix-org/synapse/issues/5417))


Synapse 1.0.0rc1 (2019-06-07)
=============================

Features
--------

- Synapse now more efficiently collates room statistics. ([\#4338](https://github.com/matrix-org/synapse/issues/4338), [\#5260](https://github.com/matrix-org/synapse/issues/5260), [\#5324](https://github.com/matrix-org/synapse/issues/5324))
- Add experimental support for relations (aka reactions and edits). ([\#5220](https://github.com/matrix-org/synapse/issues/5220))
- Ability to configure default room version. ([\#5223](https://github.com/matrix-org/synapse/issues/5223), [\#5249](https://github.com/matrix-org/synapse/issues/5249))
- Allow configuring a range for the account validity startup job. ([\#5276](https://github.com/matrix-org/synapse/issues/5276))
- CAS login will now hit the r0 API, not the deprecated v1 one. ([\#5286](https://github.com/matrix-org/synapse/issues/5286))
- Validate federation server TLS certificates by default (implements [MSC1711](https://github.com/matrix-org/matrix-doc/blob/master/proposals/1711-x509-for-federation.md)). ([\#5359](https://github.com/matrix-org/synapse/issues/5359))
- Update /_matrix/client/versions to reference support for r0.5.0. ([\#5360](https://github.com/matrix-org/synapse/issues/5360))
- Add a script to generate new signing-key files. ([\#5361](https://github.com/matrix-org/synapse/issues/5361))
- Update upgrade and installation guides ahead of 1.0. ([\#5371](https://github.com/matrix-org/synapse/issues/5371))
- Replace the `perspectives` configuration section with `trusted_key_servers`, and make validating the signatures on responses optional (since TLS will do this job for us). ([\#5374](https://github.com/matrix-org/synapse/issues/5374))
- Add ability to perform password reset via email without trusting the identity server. **As a result of this PR, password resets will now be disabled on the default configuration.**

  Password reset emails are now sent from the homeserver by default, instead of the identity server. To enable this functionality, ensure `email` and `public_baseurl` config options are filled out.

  If you would like to re-enable password resets being sent from the identity server (warning: this is dangerous! See [#5345](https://github.com/matrix-org/synapse/pull/5345)), set `email.trust_identity_server_for_password_resets` to true. ([\#5377](https://github.com/matrix-org/synapse/issues/5377))
- Set default room version to v4. ([\#5379](https://github.com/matrix-org/synapse/issues/5379))


Bugfixes
--------

- Fixes client-server API not sending "m.heroes" to lazy-load /sync requests when a rooms name or its canonical alias are empty. Thanks to @dnaf for this work! ([\#5089](https://github.com/matrix-org/synapse/issues/5089))
- Prevent federation device list updates breaking when processing multiple updates at once. ([\#5156](https://github.com/matrix-org/synapse/issues/5156))
- Fix worker registration bug caused by ClientReaderSlavedStore being unable to see get_profileinfo. ([\#5200](https://github.com/matrix-org/synapse/issues/5200))
- Fix race when backfilling in rooms with worker mode. ([\#5221](https://github.com/matrix-org/synapse/issues/5221))
- Fix appservice timestamp massaging. ([\#5233](https://github.com/matrix-org/synapse/issues/5233))
- Ensure that server_keys fetched via a notary server are correctly signed. ([\#5251](https://github.com/matrix-org/synapse/issues/5251))
- Show the correct error when logging out and access token is missing. ([\#5256](https://github.com/matrix-org/synapse/issues/5256))
- Fix error code when there is an invalid parameter on /_matrix/client/r0/publicRooms ([\#5257](https://github.com/matrix-org/synapse/issues/5257))
- Fix error when downloading thumbnail with missing width/height parameter. ([\#5258](https://github.com/matrix-org/synapse/issues/5258))
- Fix schema update for account validity. ([\#5268](https://github.com/matrix-org/synapse/issues/5268))
- Fix bug where we leaked extremities when we soft failed events, leading to performance degradation. ([\#5274](https://github.com/matrix-org/synapse/issues/5274), [\#5278](https://github.com/matrix-org/synapse/issues/5278), [\#5291](https://github.com/matrix-org/synapse/issues/5291))
- Fix "db txn 'update_presence' from sentinel context" log messages. ([\#5275](https://github.com/matrix-org/synapse/issues/5275))
- Fix dropped logcontexts during high outbound traffic. ([\#5277](https://github.com/matrix-org/synapse/issues/5277))
- Fix a bug where it is not possible to get events in the federation format with the request `GET /_matrix/client/r0/rooms/{roomId}/messages`. ([\#5293](https://github.com/matrix-org/synapse/issues/5293))
- Fix performance problems with the rooms stats background update. ([\#5294](https://github.com/matrix-org/synapse/issues/5294))
- Fix noisy 'no key for server' logs. ([\#5300](https://github.com/matrix-org/synapse/issues/5300))
- Fix bug where a notary server would sometimes forget old keys. ([\#5307](https://github.com/matrix-org/synapse/issues/5307))
- Prevent users from setting huge displaynames and avatar URLs. ([\#5309](https://github.com/matrix-org/synapse/issues/5309))
- Fix handling of failures when processing incoming events where calling `/event_auth` on remote server fails. ([\#5317](https://github.com/matrix-org/synapse/issues/5317))
- Ensure that we have an up-to-date copy of the signing key when validating incoming federation requests. ([\#5321](https://github.com/matrix-org/synapse/issues/5321))
- Fix various problems which made the signing-key notary server time out for some requests. ([\#5333](https://github.com/matrix-org/synapse/issues/5333))
- Fix bug which would make certain operations (such as room joins) block for 20 minutes while attemoting to fetch verification keys. ([\#5334](https://github.com/matrix-org/synapse/issues/5334))
- Fix a bug where we could rapidly mark a server as unreachable even though it was only down for a few minutes. ([\#5335](https://github.com/matrix-org/synapse/issues/5335), [\#5340](https://github.com/matrix-org/synapse/issues/5340))
- Fix a bug where account validity renewal emails could only be sent when email notifs were enabled. ([\#5341](https://github.com/matrix-org/synapse/issues/5341))
- Fix failure when fetching batches of events during backfill, etc. ([\#5342](https://github.com/matrix-org/synapse/issues/5342))
- Add a new room version where the timestamps on events are checked against the validity periods on signing keys. ([\#5348](https://github.com/matrix-org/synapse/issues/5348), [\#5354](https://github.com/matrix-org/synapse/issues/5354))
- Fix room stats and presence background updates to correctly handle missing events. ([\#5352](https://github.com/matrix-org/synapse/issues/5352))
- Include left members in room summaries' heroes. ([\#5355](https://github.com/matrix-org/synapse/issues/5355))
- Fix `federation_custom_ca_list` configuration option. ([\#5362](https://github.com/matrix-org/synapse/issues/5362))
- Fix missing logcontext warnings on shutdown. ([\#5369](https://github.com/matrix-org/synapse/issues/5369))


Improved Documentation
----------------------

- Fix docs on resetting the user directory. ([\#5282](https://github.com/matrix-org/synapse/issues/5282))
- Fix notes about ACME in the MSC1711 faq. ([\#5357](https://github.com/matrix-org/synapse/issues/5357))


Internal Changes
----------------

- Synapse will now serve the experimental "room complexity" API endpoint. ([\#5216](https://github.com/matrix-org/synapse/issues/5216))
- The base classes for the v1 and v2_alpha REST APIs have been unified. ([\#5226](https://github.com/matrix-org/synapse/issues/5226), [\#5328](https://github.com/matrix-org/synapse/issues/5328))
- Simplifications and comments in do_auth. ([\#5227](https://github.com/matrix-org/synapse/issues/5227))
- Remove urllib3 pin as requests 2.22.0 has been released supporting urllib3 1.25.2. ([\#5230](https://github.com/matrix-org/synapse/issues/5230))
- Preparatory work for key-validity features. ([\#5232](https://github.com/matrix-org/synapse/issues/5232), [\#5234](https://github.com/matrix-org/synapse/issues/5234), [\#5235](https://github.com/matrix-org/synapse/issues/5235), [\#5236](https://github.com/matrix-org/synapse/issues/5236), [\#5237](https://github.com/matrix-org/synapse/issues/5237), [\#5244](https://github.com/matrix-org/synapse/issues/5244), [\#5250](https://github.com/matrix-org/synapse/issues/5250), [\#5296](https://github.com/matrix-org/synapse/issues/5296), [\#5299](https://github.com/matrix-org/synapse/issues/5299), [\#5343](https://github.com/matrix-org/synapse/issues/5343), [\#5347](https://github.com/matrix-org/synapse/issues/5347), [\#5356](https://github.com/matrix-org/synapse/issues/5356))
- Specify the type of reCAPTCHA key to use. ([\#5283](https://github.com/matrix-org/synapse/issues/5283))
- Improve sample config for monthly active user blocking. ([\#5284](https://github.com/matrix-org/synapse/issues/5284))
- Remove spurious debug from MatrixFederationHttpClient.get_json. ([\#5287](https://github.com/matrix-org/synapse/issues/5287))
- Improve logging for logcontext leaks. ([\#5288](https://github.com/matrix-org/synapse/issues/5288))
- Clarify that the admin change password API logs the user out. ([\#5303](https://github.com/matrix-org/synapse/issues/5303))
- New installs will now use the v54 full schema, rather than the full schema v14 and applying incremental updates to v54. ([\#5320](https://github.com/matrix-org/synapse/issues/5320))
- Improve docstrings on MatrixFederationClient. ([\#5332](https://github.com/matrix-org/synapse/issues/5332))
- Clean up FederationClient.get_events for clarity. ([\#5344](https://github.com/matrix-org/synapse/issues/5344))
- Various improvements to debug logging. ([\#5353](https://github.com/matrix-org/synapse/issues/5353))
- Don't run CI build checks until sample config check has passed. ([\#5370](https://github.com/matrix-org/synapse/issues/5370))
- Automatically retry buildkite builds (max twice) when an agent is lost. ([\#5380](https://github.com/matrix-org/synapse/issues/5380))


Synapse 0.99.5.2 (2019-05-30)
=============================

Bugfixes
--------

- Fix bug where we leaked extremities when we soft failed events, leading to performance degradation. ([\#5274](https://github.com/matrix-org/synapse/issues/5274), [\#5278](https://github.com/matrix-org/synapse/issues/5278), [\#5291](https://github.com/matrix-org/synapse/issues/5291))


Synapse 0.99.5.1 (2019-05-22)
=============================

0.99.5.1 supersedes 0.99.5 due to malformed debian changelog - no functional changes.

Synapse 0.99.5 (2019-05-22)
===========================

No significant changes.


Synapse 0.99.5rc1 (2019-05-21)
==============================

Features
--------

- Add ability to blacklist IP ranges for the federation client. ([\#5043](https://github.com/matrix-org/synapse/issues/5043))
- Ratelimiting configuration for clients sending messages and the federation server has been altered to match login ratelimiting. The old configuration names will continue working. Check the sample config for details of the new names. ([\#5181](https://github.com/matrix-org/synapse/issues/5181))
- Drop support for the undocumented /_matrix/client/v2_alpha API prefix. ([\#5190](https://github.com/matrix-org/synapse/issues/5190))
- Add an option to disable per-room profiles. ([\#5196](https://github.com/matrix-org/synapse/issues/5196))
- Stick an expiration date to any registered user missing one at startup if account validity is enabled. ([\#5204](https://github.com/matrix-org/synapse/issues/5204))
- Add experimental support for relations (aka reactions and edits). ([\#5209](https://github.com/matrix-org/synapse/issues/5209), [\#5211](https://github.com/matrix-org/synapse/issues/5211), [\#5203](https://github.com/matrix-org/synapse/issues/5203), [\#5212](https://github.com/matrix-org/synapse/issues/5212))
- Add a room version 4 which uses a new event ID format, as per [MSC2002](https://github.com/matrix-org/matrix-doc/pull/2002). ([\#5210](https://github.com/matrix-org/synapse/issues/5210), [\#5217](https://github.com/matrix-org/synapse/issues/5217))


Bugfixes
--------

- Fix image orientation when generating thumbnails (needs pillow>=4.3.0). Contributed by Pau Rodriguez-Estivill. ([\#5039](https://github.com/matrix-org/synapse/issues/5039))
- Exclude soft-failed events from forward-extremity candidates: fixes "No forward extremities left!" error. ([\#5146](https://github.com/matrix-org/synapse/issues/5146))
- Re-order stages in registration flows such that msisdn and email verification are done last. ([\#5174](https://github.com/matrix-org/synapse/issues/5174))
- Fix 3pid guest invites. ([\#5177](https://github.com/matrix-org/synapse/issues/5177))
- Fix a bug where the register endpoint would fail with M_THREEPID_IN_USE instead of returning an account previously registered in the same session. ([\#5187](https://github.com/matrix-org/synapse/issues/5187))
- Prevent registration for user ids that are too long to fit into a state key. Contributed by Reid Anderson. ([\#5198](https://github.com/matrix-org/synapse/issues/5198))
- Fix incompatibility between ACME support and Python 3.5.2. ([\#5218](https://github.com/matrix-org/synapse/issues/5218))
- Fix error handling for rooms whose versions are unknown. ([\#5219](https://github.com/matrix-org/synapse/issues/5219))


Internal Changes
----------------

- Make /sync attempt to return device updates for both joined and invited users. Note that this doesn't currently work correctly due to other bugs. ([\#3484](https://github.com/matrix-org/synapse/issues/3484))
- Update tests to consistently be configured via the same code that is used when loading from configuration files. ([\#5171](https://github.com/matrix-org/synapse/issues/5171), [\#5185](https://github.com/matrix-org/synapse/issues/5185))
- Allow client event serialization to be async. ([\#5183](https://github.com/matrix-org/synapse/issues/5183))
- Expose DataStore._get_events as get_events_as_list. ([\#5184](https://github.com/matrix-org/synapse/issues/5184))
- Make generating SQL bounds for pagination generic. ([\#5191](https://github.com/matrix-org/synapse/issues/5191))
- Stop telling people to install the optional dependencies by default. ([\#5197](https://github.com/matrix-org/synapse/issues/5197))


Synapse 0.99.4 (2019-05-15)
===========================

No significant changes.


Synapse 0.99.4rc1 (2019-05-13)
==============================

Features
--------

- Add systemd-python to the optional dependencies to enable logging to the systemd journal. Install with `pip install matrix-synapse[systemd]`. ([\#4339](https://github.com/matrix-org/synapse/issues/4339))
- Add a default .m.rule.tombstone push rule. ([\#4867](https://github.com/matrix-org/synapse/issues/4867))
- Add ability for password provider modules to bind email addresses to users upon registration. ([\#4947](https://github.com/matrix-org/synapse/issues/4947))
- Implementation of [MSC1711](https://github.com/matrix-org/matrix-doc/pull/1711) including config options for requiring valid TLS certificates for federation traffic, the ability to disable TLS validation for specific domains, and the ability to specify your own list of CA certificates. ([\#4967](https://github.com/matrix-org/synapse/issues/4967))
- Remove presence list support as per MSC 1819. ([\#4989](https://github.com/matrix-org/synapse/issues/4989))
- Reduce CPU usage starting pushers during start up. ([\#4991](https://github.com/matrix-org/synapse/issues/4991))
- Add a delete group admin API. ([\#5002](https://github.com/matrix-org/synapse/issues/5002))
- Add config option to block users from looking up 3PIDs. ([\#5010](https://github.com/matrix-org/synapse/issues/5010))
- Add context to phonehome stats. ([\#5020](https://github.com/matrix-org/synapse/issues/5020))
- Configure the example systemd units to have a log identifier of `matrix-synapse`
  instead of the executable name, `python`.
  Contributed by Christoph Müller. ([\#5023](https://github.com/matrix-org/synapse/issues/5023))
- Add time-based account expiration. ([\#5027](https://github.com/matrix-org/synapse/issues/5027), [\#5047](https://github.com/matrix-org/synapse/issues/5047), [\#5073](https://github.com/matrix-org/synapse/issues/5073), [\#5116](https://github.com/matrix-org/synapse/issues/5116))
- Add support for handling `/versions`, `/voip` and `/push_rules` client endpoints to client_reader worker. ([\#5063](https://github.com/matrix-org/synapse/issues/5063), [\#5065](https://github.com/matrix-org/synapse/issues/5065), [\#5070](https://github.com/matrix-org/synapse/issues/5070))
- Add a configuration option to require authentication on /publicRooms and /profile endpoints. ([\#5083](https://github.com/matrix-org/synapse/issues/5083))
- Move admin APIs to `/_synapse/admin/v1`. (The old paths are retained for backwards-compatibility, for now). ([\#5119](https://github.com/matrix-org/synapse/issues/5119))
- Implement an admin API for sending server notices. Many thanks to @krombel who provided a foundation for this work. ([\#5121](https://github.com/matrix-org/synapse/issues/5121), [\#5142](https://github.com/matrix-org/synapse/issues/5142))


Bugfixes
--------

- Avoid redundant URL encoding of redirect URL for SSO login in the fallback login page. Fixes a regression introduced in [#4220](https://github.com/matrix-org/synapse/pull/4220). Contributed by Marcel Fabian Krüger ("[zaugin](https://github.com/zauguin)"). ([\#4555](https://github.com/matrix-org/synapse/issues/4555))
- Fix bug where presence updates were sent to all servers in a room when a new server joined, rather than to just the new server. ([\#4942](https://github.com/matrix-org/synapse/issues/4942), [\#5103](https://github.com/matrix-org/synapse/issues/5103))
- Fix sync bug which made accepting invites unreliable in worker-mode synapses. ([\#4955](https://github.com/matrix-org/synapse/issues/4955), [\#4956](https://github.com/matrix-org/synapse/issues/4956))
- start.sh: Fix the --no-rate-limit option for messages and make it bypass rate limit on registration and login too. ([\#4981](https://github.com/matrix-org/synapse/issues/4981))
- Transfer related groups on room upgrade. ([\#4990](https://github.com/matrix-org/synapse/issues/4990))
- Prevent the ability to kick users from a room they aren't in. ([\#4999](https://github.com/matrix-org/synapse/issues/4999))
- Fix issue #4596 so synapse_port_db script works with --curses option on Python 3. Contributed by Anders Jensen-Waud <anders@jensenwaud.com>. ([\#5003](https://github.com/matrix-org/synapse/issues/5003))
- Clients timing out/disappearing while downloading from the media repository will now no longer log a spurious "Producer was not unregistered" message. ([\#5009](https://github.com/matrix-org/synapse/issues/5009))
- Fix "cannot import name execute_batch" error with postgres. ([\#5032](https://github.com/matrix-org/synapse/issues/5032))
- Fix disappearing exceptions in manhole. ([\#5035](https://github.com/matrix-org/synapse/issues/5035))
- Workaround bug in twisted where attempting too many concurrent DNS requests could cause it to hang due to running out of file descriptors. ([\#5037](https://github.com/matrix-org/synapse/issues/5037))
- Make sure we're not registering the same 3pid twice on registration. ([\#5071](https://github.com/matrix-org/synapse/issues/5071))
- Don't crash on lack of expiry templates. ([\#5077](https://github.com/matrix-org/synapse/issues/5077))
- Fix the ratelimiting on third party invites. ([\#5104](https://github.com/matrix-org/synapse/issues/5104))
- Add some missing limitations to room alias creation. ([\#5124](https://github.com/matrix-org/synapse/issues/5124), [\#5128](https://github.com/matrix-org/synapse/issues/5128))
- Limit the number of EDUs in transactions to 100 as expected by synapse. Thanks to @superboum for this work! ([\#5138](https://github.com/matrix-org/synapse/issues/5138))

Internal Changes
----------------

- Add test to verify threepid auth check added in #4435. ([\#4474](https://github.com/matrix-org/synapse/issues/4474))
- Fix/improve some docstrings in the replication code. ([\#4949](https://github.com/matrix-org/synapse/issues/4949))
- Split synapse.replication.tcp.streams into smaller files. ([\#4953](https://github.com/matrix-org/synapse/issues/4953))
- Refactor replication row generation/parsing. ([\#4954](https://github.com/matrix-org/synapse/issues/4954))
- Run `black` to clean up formatting on `synapse/storage/roommember.py` and `synapse/storage/events.py`. ([\#4959](https://github.com/matrix-org/synapse/issues/4959))
- Remove log line for password via the admin API. ([\#4965](https://github.com/matrix-org/synapse/issues/4965))
- Fix typo in TLS filenames in docker/README.md. Also add the '-p' commandline option to the 'docker run' example. Contributed by Jurrie Overgoor. ([\#4968](https://github.com/matrix-org/synapse/issues/4968))
- Refactor room version definitions. ([\#4969](https://github.com/matrix-org/synapse/issues/4969))
- Reduce log level of .well-known/matrix/client responses. ([\#4972](https://github.com/matrix-org/synapse/issues/4972))
- Add `config.signing_key_path` that can be read by `synapse.config` utility. ([\#4974](https://github.com/matrix-org/synapse/issues/4974))
- Track which identity server is used when binding a threepid and use that for unbinding, as per MSC1915. ([\#4982](https://github.com/matrix-org/synapse/issues/4982))
- Rewrite KeyringTestCase as a HomeserverTestCase. ([\#4985](https://github.com/matrix-org/synapse/issues/4985))
- README updates: Corrected the default POSTGRES_USER. Added port forwarding hint in TLS section. ([\#4987](https://github.com/matrix-org/synapse/issues/4987))
- Remove a number of unused tables from the database schema. ([\#4992](https://github.com/matrix-org/synapse/issues/4992), [\#5028](https://github.com/matrix-org/synapse/issues/5028), [\#5033](https://github.com/matrix-org/synapse/issues/5033))
- Run `black` on the remainder of `synapse/storage/`. ([\#4996](https://github.com/matrix-org/synapse/issues/4996))
- Fix grammar in get_current_users_in_room and give it a docstring. ([\#4998](https://github.com/matrix-org/synapse/issues/4998))
- Clean up some code in the server-key Keyring. ([\#5001](https://github.com/matrix-org/synapse/issues/5001))
- Convert SYNAPSE_NO_TLS Docker variable to boolean for user friendliness. Contributed by Gabriel Eckerson. ([\#5005](https://github.com/matrix-org/synapse/issues/5005))
- Refactor synapse.storage._base._simple_select_list_paginate. ([\#5007](https://github.com/matrix-org/synapse/issues/5007))
- Store the notary server name correctly in server_keys_json. ([\#5024](https://github.com/matrix-org/synapse/issues/5024))
- Rewrite Datastore.get_server_verify_keys to reduce the number of database transactions. ([\#5030](https://github.com/matrix-org/synapse/issues/5030))
- Remove extraneous period from copyright headers. ([\#5046](https://github.com/matrix-org/synapse/issues/5046))
- Update documentation for where to get Synapse packages. ([\#5067](https://github.com/matrix-org/synapse/issues/5067))
- Add workarounds for pep-517 install errors. ([\#5098](https://github.com/matrix-org/synapse/issues/5098))
- Improve logging when event-signature checks fail. ([\#5100](https://github.com/matrix-org/synapse/issues/5100))
- Factor out an "assert_requester_is_admin" function. ([\#5120](https://github.com/matrix-org/synapse/issues/5120))
- Remove the requirement to authenticate for /admin/server_version. ([\#5122](https://github.com/matrix-org/synapse/issues/5122))
- Prevent an exception from being raised in a IResolutionReceiver and use a more generic error message for blacklisted URL previews. ([\#5155](https://github.com/matrix-org/synapse/issues/5155))
- Run `black` on the tests directory. ([\#5170](https://github.com/matrix-org/synapse/issues/5170))
- Fix CI after new release of isort. ([\#5179](https://github.com/matrix-org/synapse/issues/5179))
- Fix bogus imports in unit tests. ([\#5154](https://github.com/matrix-org/synapse/issues/5154))


Synapse 0.99.3.2 (2019-05-03)
=============================

Internal Changes
----------------

- Ensure that we have `urllib3` <1.25, to resolve incompatibility with `requests`. ([\#5135](https://github.com/matrix-org/synapse/issues/5135))


Synapse 0.99.3.1 (2019-05-03)
=============================

Security update
---------------

This release includes two security fixes:

- Switch to using a cryptographically-secure random number generator for token strings, ensuring they cannot be predicted by an attacker. Thanks to @opnsec for identifying and responsibly disclosing this issue! ([\#5133](https://github.com/matrix-org/synapse/issues/5133))
- Blacklist 0.0.0.0 and :: by default for URL previews. Thanks to @opnsec for identifying and responsibly disclosing this issue too! ([\#5134](https://github.com/matrix-org/synapse/issues/5134))

Synapse 0.99.3 (2019-04-01)
===========================

No significant changes.


Synapse 0.99.3rc1 (2019-03-27)
==============================

Features
--------

- The user directory has been rewritten to make it faster, with less chance of falling behind on a large server. ([\#4537](https://github.com/matrix-org/synapse/issues/4537), [\#4846](https://github.com/matrix-org/synapse/issues/4846), [\#4864](https://github.com/matrix-org/synapse/issues/4864), [\#4887](https://github.com/matrix-org/synapse/issues/4887), [\#4900](https://github.com/matrix-org/synapse/issues/4900), [\#4944](https://github.com/matrix-org/synapse/issues/4944))
- Add configurable rate limiting to the /register endpoint. ([\#4735](https://github.com/matrix-org/synapse/issues/4735), [\#4804](https://github.com/matrix-org/synapse/issues/4804))
- Move server key queries to federation reader. ([\#4757](https://github.com/matrix-org/synapse/issues/4757))
- Add support for /account/3pid REST endpoint to client_reader worker. ([\#4759](https://github.com/matrix-org/synapse/issues/4759))
- Add an endpoint to the admin API for querying the server version. Contributed by Joseph Weston. ([\#4772](https://github.com/matrix-org/synapse/issues/4772))
- Include a default configuration file in the 'docs' directory. ([\#4791](https://github.com/matrix-org/synapse/issues/4791), [\#4801](https://github.com/matrix-org/synapse/issues/4801))
- Synapse is now permissive about trailing slashes on some of its federation endpoints, allowing zero or more to be present. ([\#4793](https://github.com/matrix-org/synapse/issues/4793))
- Add support for /keys/query and /keys/changes REST endpoints to client_reader worker. ([\#4796](https://github.com/matrix-org/synapse/issues/4796))
- Add checks to incoming events over federation for events evading auth (aka "soft fail"). ([\#4814](https://github.com/matrix-org/synapse/issues/4814))
- Add configurable rate limiting to the /login endpoint. ([\#4821](https://github.com/matrix-org/synapse/issues/4821), [\#4865](https://github.com/matrix-org/synapse/issues/4865))
- Remove trailing slashes from certain outbound federation requests. Retry if receiving a 404. Context: #3622. ([\#4840](https://github.com/matrix-org/synapse/issues/4840))
- Allow passing --daemonize flags to workers in the same way as with master. ([\#4853](https://github.com/matrix-org/synapse/issues/4853))
- Batch up outgoing read-receipts to reduce federation traffic. ([\#4890](https://github.com/matrix-org/synapse/issues/4890), [\#4927](https://github.com/matrix-org/synapse/issues/4927))
- Add option to disable searching the user directory. ([\#4895](https://github.com/matrix-org/synapse/issues/4895))
- Add option to disable searching of local and remote public room lists. ([\#4896](https://github.com/matrix-org/synapse/issues/4896))
- Add ability for password providers to login/register a user via 3PID (email, phone). ([\#4931](https://github.com/matrix-org/synapse/issues/4931))


Bugfixes
--------

- Fix a bug where media with spaces in the name would get a corrupted name. ([\#2090](https://github.com/matrix-org/synapse/issues/2090))
- Fix attempting to paginate in rooms where server cannot see any events, to avoid unnecessarily pulling in lots of redacted events. ([\#4699](https://github.com/matrix-org/synapse/issues/4699))
- 'event_id' is now a required parameter in federated state requests, as per the matrix spec. ([\#4740](https://github.com/matrix-org/synapse/issues/4740))
- Fix tightloop over connecting to replication server. ([\#4749](https://github.com/matrix-org/synapse/issues/4749))
- Fix parsing of Content-Disposition headers on remote media requests and URL previews. ([\#4763](https://github.com/matrix-org/synapse/issues/4763))
- Fix incorrect log about not persisting duplicate state event. ([\#4776](https://github.com/matrix-org/synapse/issues/4776))
- Fix v4v6 option in HAProxy example config. Contributed by Flakebi. ([\#4790](https://github.com/matrix-org/synapse/issues/4790))
- Handle batch updates in worker replication protocol. ([\#4792](https://github.com/matrix-org/synapse/issues/4792))
- Fix bug where we didn't correctly throttle sending of USER_IP commands over replication. ([\#4818](https://github.com/matrix-org/synapse/issues/4818))
- Fix potential race in handling missing updates in device list updates. ([\#4829](https://github.com/matrix-org/synapse/issues/4829))
- Fix bug where synapse expected an un-specced `prev_state` field on state events. ([\#4837](https://github.com/matrix-org/synapse/issues/4837))
- Transfer a user's notification settings (push rules) on room upgrade. ([\#4838](https://github.com/matrix-org/synapse/issues/4838))
- fix test_auto_create_auto_join_where_no_consent. ([\#4886](https://github.com/matrix-org/synapse/issues/4886))
- Fix a bug where hs_disabled_message was sometimes not correctly enforced. ([\#4888](https://github.com/matrix-org/synapse/issues/4888))
- Fix bug in shutdown room admin API where it would fail if a user in the room hadn't consented to the privacy policy. ([\#4904](https://github.com/matrix-org/synapse/issues/4904))
- Fix bug where blocked world-readable rooms were still peekable. ([\#4908](https://github.com/matrix-org/synapse/issues/4908))


Internal Changes
----------------

- Add a systemd setup that supports synapse workers. Contributed by Luca Corbatto. ([\#4662](https://github.com/matrix-org/synapse/issues/4662))
- Change from TravisCI to Buildkite for CI. ([\#4752](https://github.com/matrix-org/synapse/issues/4752))
- When presence is disabled don't send over replication. ([\#4757](https://github.com/matrix-org/synapse/issues/4757))
- Minor docstring fixes for MatrixFederationAgent. ([\#4765](https://github.com/matrix-org/synapse/issues/4765))
- Optimise EDU transmission for the federation_sender worker. ([\#4770](https://github.com/matrix-org/synapse/issues/4770))
- Update test_typing to use HomeserverTestCase. ([\#4771](https://github.com/matrix-org/synapse/issues/4771))
- Update URLs for riot.im icons and logos in the default notification templates. ([\#4779](https://github.com/matrix-org/synapse/issues/4779))
- Removed unnecessary $ from some federation endpoint path regexes. ([\#4794](https://github.com/matrix-org/synapse/issues/4794))
- Remove link to deleted title in README. ([\#4795](https://github.com/matrix-org/synapse/issues/4795))
- Clean up read-receipt handling. ([\#4797](https://github.com/matrix-org/synapse/issues/4797))
- Add some debug about processing read receipts. ([\#4798](https://github.com/matrix-org/synapse/issues/4798))
- Clean up some replication code. ([\#4799](https://github.com/matrix-org/synapse/issues/4799))
- Add some docstrings. ([\#4815](https://github.com/matrix-org/synapse/issues/4815))
- Add debug logger to try and track down #4422. ([\#4816](https://github.com/matrix-org/synapse/issues/4816))
- Make shutdown API send explanation message to room after users have been forced joined. ([\#4817](https://github.com/matrix-org/synapse/issues/4817))
- Update example_log_config.yaml. ([\#4820](https://github.com/matrix-org/synapse/issues/4820))
- Document the `generate` option for the docker image. ([\#4824](https://github.com/matrix-org/synapse/issues/4824))
- Fix check-newsfragment for debian-only changes. ([\#4825](https://github.com/matrix-org/synapse/issues/4825))
- Add some debug logging for device list updates to help with #4828. ([\#4828](https://github.com/matrix-org/synapse/issues/4828))
- Improve federation documentation, specifically .well-known support. Many thanks to @vaab. ([\#4832](https://github.com/matrix-org/synapse/issues/4832))
- Disable captcha registration by default in unit tests. ([\#4839](https://github.com/matrix-org/synapse/issues/4839))
- Add stuff back to the .gitignore. ([\#4843](https://github.com/matrix-org/synapse/issues/4843))
- Clarify what registration_shared_secret allows for. ([\#4844](https://github.com/matrix-org/synapse/issues/4844))
- Correctly log expected errors when fetching server keys. ([\#4847](https://github.com/matrix-org/synapse/issues/4847))
- Update install docs to explicitly state a full-chain (not just the top-level) TLS certificate must be provided to Synapse. This caused some people's Synapse ports to appear correct in a browser but still (rightfully so) upset the federation tester. ([\#4849](https://github.com/matrix-org/synapse/issues/4849))
- Move client read-receipt processing to federation sender worker. ([\#4852](https://github.com/matrix-org/synapse/issues/4852))
- Refactor federation TransactionQueue. ([\#4855](https://github.com/matrix-org/synapse/issues/4855))
- Comment out most options in the generated config. ([\#4863](https://github.com/matrix-org/synapse/issues/4863))
- Fix yaml library warnings by using safe_load. ([\#4869](https://github.com/matrix-org/synapse/issues/4869))
- Update Apache setup to remove location syntax. Thanks to @cwmke! ([\#4870](https://github.com/matrix-org/synapse/issues/4870))
- Reinstate test case that runs unit tests against oldest supported dependencies. ([\#4879](https://github.com/matrix-org/synapse/issues/4879))
- Update link to federation docs. ([\#4881](https://github.com/matrix-org/synapse/issues/4881))
- fix test_auto_create_auto_join_where_no_consent. ([\#4886](https://github.com/matrix-org/synapse/issues/4886))
- Use a regular HomeServerConfig object for unit tests rater than a Mock. ([\#4889](https://github.com/matrix-org/synapse/issues/4889))
- Add some notes about tuning postgres for larger deployments. ([\#4895](https://github.com/matrix-org/synapse/issues/4895))
- Add a config option for torture-testing worker replication. ([\#4902](https://github.com/matrix-org/synapse/issues/4902))
- Log requests which are simulated by the unit tests. ([\#4905](https://github.com/matrix-org/synapse/issues/4905))
- Allow newsfragments to end with exclamation marks. Exciting! ([\#4912](https://github.com/matrix-org/synapse/issues/4912))
- Refactor some more tests to use HomeserverTestCase. ([\#4913](https://github.com/matrix-org/synapse/issues/4913))
- Refactor out the state deltas portion of the user directory store and handler. ([\#4917](https://github.com/matrix-org/synapse/issues/4917))
- Fix nginx example in ACME doc. ([\#4923](https://github.com/matrix-org/synapse/issues/4923))
- Use an explicit dbname for postgres connections in the tests. ([\#4928](https://github.com/matrix-org/synapse/issues/4928))
- Fix `ClientReplicationStreamProtocol.__str__()`. ([\#4929](https://github.com/matrix-org/synapse/issues/4929))


Synapse 0.99.2 (2019-03-01)
===========================

Features
--------

- Added an HAProxy example in the reverse proxy documentation. Contributed by Benoît S. (“Benpro”). ([\#4541](https://github.com/matrix-org/synapse/issues/4541))
- Add basic optional sentry integration. ([\#4632](https://github.com/matrix-org/synapse/issues/4632), [\#4694](https://github.com/matrix-org/synapse/issues/4694))
- Transfer bans on room upgrade. ([\#4642](https://github.com/matrix-org/synapse/issues/4642))
- Add configurable room list publishing rules. ([\#4647](https://github.com/matrix-org/synapse/issues/4647))
- Support .well-known delegation when issuing certificates through ACME. ([\#4652](https://github.com/matrix-org/synapse/issues/4652))
- Allow registration and login to be handled by a worker instance. ([\#4666](https://github.com/matrix-org/synapse/issues/4666), [\#4670](https://github.com/matrix-org/synapse/issues/4670), [\#4682](https://github.com/matrix-org/synapse/issues/4682))
- Reduce the overhead of creating outbound federation connections over TLS by caching the TLS client options. ([\#4674](https://github.com/matrix-org/synapse/issues/4674))
- Add prometheus metrics for number of outgoing EDUs, by type. ([\#4695](https://github.com/matrix-org/synapse/issues/4695))
- Return correct error code when inviting a remote user to a room whose homeserver does not support the room version. ([\#4721](https://github.com/matrix-org/synapse/issues/4721))
- Prevent showing rooms to other servers that were set to not federate. ([\#4746](https://github.com/matrix-org/synapse/issues/4746))


Bugfixes
--------

- Fix possible exception when paginating. ([\#4263](https://github.com/matrix-org/synapse/issues/4263))
- The dependency checker now correctly reports a version mismatch for optional
  dependencies, instead of reporting the dependency missing. ([\#4450](https://github.com/matrix-org/synapse/issues/4450))
- Set CORS headers on .well-known requests. ([\#4651](https://github.com/matrix-org/synapse/issues/4651))
- Fix kicking guest users on guest access revocation in worker mode. ([\#4667](https://github.com/matrix-org/synapse/issues/4667))
- Fix an issue in the database migration script where the
  `e2e_room_keys.is_verified` column wasn't considered as
  a boolean. ([\#4680](https://github.com/matrix-org/synapse/issues/4680))
- Fix TaskStopped exceptions in logs when outbound requests time out. ([\#4690](https://github.com/matrix-org/synapse/issues/4690))
- Fix ACME config for python 2. ([\#4717](https://github.com/matrix-org/synapse/issues/4717))
- Fix paginating over federation persisting incorrect state. ([\#4718](https://github.com/matrix-org/synapse/issues/4718))


Internal Changes
----------------

- Run `black` to reformat user directory code. ([\#4635](https://github.com/matrix-org/synapse/issues/4635))
- Reduce number of exceptions we log. ([\#4643](https://github.com/matrix-org/synapse/issues/4643), [\#4668](https://github.com/matrix-org/synapse/issues/4668))
- Introduce upsert batching functionality in the database layer. ([\#4644](https://github.com/matrix-org/synapse/issues/4644))
- Fix various spelling mistakes. ([\#4657](https://github.com/matrix-org/synapse/issues/4657))
- Cleanup request exception logging. ([\#4669](https://github.com/matrix-org/synapse/issues/4669), [\#4737](https://github.com/matrix-org/synapse/issues/4737), [\#4738](https://github.com/matrix-org/synapse/issues/4738))
- Improve replication performance by reducing cache invalidation traffic. ([\#4671](https://github.com/matrix-org/synapse/issues/4671), [\#4715](https://github.com/matrix-org/synapse/issues/4715), [\#4748](https://github.com/matrix-org/synapse/issues/4748))
- Test against Postgres 9.5 as well as 9.4. ([\#4676](https://github.com/matrix-org/synapse/issues/4676))
- Run unit tests against python 3.7. ([\#4677](https://github.com/matrix-org/synapse/issues/4677))
- Attempt to clarify installation instructions/config. ([\#4681](https://github.com/matrix-org/synapse/issues/4681))
- Clean up gitignores. ([\#4688](https://github.com/matrix-org/synapse/issues/4688))
- Minor tweaks to acme docs. ([\#4689](https://github.com/matrix-org/synapse/issues/4689))
- Improve the logging in the pusher process. ([\#4691](https://github.com/matrix-org/synapse/issues/4691))
- Better checks on newsfragments. ([\#4698](https://github.com/matrix-org/synapse/issues/4698), [\#4750](https://github.com/matrix-org/synapse/issues/4750))
- Avoid some redundant work when processing read receipts. ([\#4706](https://github.com/matrix-org/synapse/issues/4706))
- Run `push_receipts_to_remotes` as background job. ([\#4707](https://github.com/matrix-org/synapse/issues/4707))
- Add prometheus metrics for number of badge update pushes. ([\#4709](https://github.com/matrix-org/synapse/issues/4709))
- Reduce pusher logging on startup ([\#4716](https://github.com/matrix-org/synapse/issues/4716))
- Don't log exceptions when failing to fetch remote server keys. ([\#4722](https://github.com/matrix-org/synapse/issues/4722))
- Correctly proxy exception in frontend_proxy worker. ([\#4723](https://github.com/matrix-org/synapse/issues/4723))
- Add database version to phonehome stats. ([\#4753](https://github.com/matrix-org/synapse/issues/4753))


Synapse 0.99.1.1 (2019-02-14)
=============================

Bugfixes
--------

- Fix "TypeError: '>' not supported" when starting without an existing certificate.
  Fix a bug where an existing certificate would be reprovisoned every day. ([\#4648](https://github.com/matrix-org/synapse/issues/4648))


Synapse 0.99.1 (2019-02-14)
===========================

Features
--------

- Include m.room.encryption on invites by default ([\#3902](https://github.com/matrix-org/synapse/issues/3902))
- Federation OpenID listener resource can now be activated even if federation is disabled ([\#4420](https://github.com/matrix-org/synapse/issues/4420))
- Synapse's ACME support will now correctly reprovision a certificate that approaches its expiry while Synapse is running. ([\#4522](https://github.com/matrix-org/synapse/issues/4522))
- Add ability to update backup versions ([\#4580](https://github.com/matrix-org/synapse/issues/4580))
- Allow the "unavailable" presence status for /sync.
  This change makes Synapse compliant with r0.4.0 of the Client-Server specification. ([\#4592](https://github.com/matrix-org/synapse/issues/4592))
- There is no longer any need to specify `no_tls`: it is inferred from the absence of TLS listeners ([\#4613](https://github.com/matrix-org/synapse/issues/4613), [\#4615](https://github.com/matrix-org/synapse/issues/4615), [\#4617](https://github.com/matrix-org/synapse/issues/4617), [\#4636](https://github.com/matrix-org/synapse/issues/4636))
- The default configuration no longer requires TLS certificates. ([\#4614](https://github.com/matrix-org/synapse/issues/4614))


Bugfixes
--------

- Copy over room federation ability on room upgrade. ([\#4530](https://github.com/matrix-org/synapse/issues/4530))
- Fix noisy "twisted.internet.task.TaskStopped" errors in logs ([\#4546](https://github.com/matrix-org/synapse/issues/4546))
- Synapse is now tolerant of the `tls_fingerprints` option being None or not specified. ([\#4589](https://github.com/matrix-org/synapse/issues/4589))
- Fix 'no unique or exclusion constraint' error ([\#4591](https://github.com/matrix-org/synapse/issues/4591))
- Transfer Server ACLs on room upgrade. ([\#4608](https://github.com/matrix-org/synapse/issues/4608))
- Fix failure to start when not TLS certificate was given even if TLS was disabled. ([\#4618](https://github.com/matrix-org/synapse/issues/4618))
- Fix self-signed cert notice from generate-config. ([\#4625](https://github.com/matrix-org/synapse/issues/4625))
- Fix performance of `user_ips` table deduplication background update ([\#4626](https://github.com/matrix-org/synapse/issues/4626), [\#4627](https://github.com/matrix-org/synapse/issues/4627))


Internal Changes
----------------

- Change the user directory state query to use a filtered call to the db instead of a generic one. ([\#4462](https://github.com/matrix-org/synapse/issues/4462))
- Reject federation transactions if they include more than 50 PDUs or 100 EDUs. ([\#4513](https://github.com/matrix-org/synapse/issues/4513))
- Reduce duplication of ``synapse.app`` code. ([\#4567](https://github.com/matrix-org/synapse/issues/4567))
- Fix docker upload job to push -py2 images. ([\#4576](https://github.com/matrix-org/synapse/issues/4576))
- Add port configuration information to ACME instructions. ([\#4578](https://github.com/matrix-org/synapse/issues/4578))
- Update MSC1711 FAQ to calrify .well-known usage ([\#4584](https://github.com/matrix-org/synapse/issues/4584))
- Clean up default listener configuration ([\#4586](https://github.com/matrix-org/synapse/issues/4586))
- Clarifications for reverse proxy docs ([\#4607](https://github.com/matrix-org/synapse/issues/4607))
- Move ClientTLSOptionsFactory init out of `refresh_certificates` ([\#4611](https://github.com/matrix-org/synapse/issues/4611))
- Fail cleanly if listener config lacks a 'port' ([\#4616](https://github.com/matrix-org/synapse/issues/4616))
- Remove redundant entries from docker config ([\#4619](https://github.com/matrix-org/synapse/issues/4619))
- README updates ([\#4621](https://github.com/matrix-org/synapse/issues/4621))


Synapse 0.99.0 (2019-02-05)
===========================

Synapse v0.99.x is a precursor to the upcoming Synapse v1.0 release. It contains foundational changes to room architecture and the federation security model necessary to support the upcoming r0 release of the Server to Server API.

Features
--------

- Synapse's cipher string has been updated to require ECDH key exchange. Configuring and generating dh_params is no longer required, and they will be ignored. ([\#4229](https://github.com/matrix-org/synapse/issues/4229))
- Synapse can now automatically provision TLS certificates via ACME (the protocol used by CAs like Let's Encrypt). ([\#4384](https://github.com/matrix-org/synapse/issues/4384), [\#4492](https://github.com/matrix-org/synapse/issues/4492), [\#4525](https://github.com/matrix-org/synapse/issues/4525), [\#4572](https://github.com/matrix-org/synapse/issues/4572), [\#4564](https://github.com/matrix-org/synapse/issues/4564), [\#4566](https://github.com/matrix-org/synapse/issues/4566), [\#4547](https://github.com/matrix-org/synapse/issues/4547), [\#4557](https://github.com/matrix-org/synapse/issues/4557))
- Implement MSC1708 (.well-known routing for server-server federation) ([\#4408](https://github.com/matrix-org/synapse/issues/4408), [\#4409](https://github.com/matrix-org/synapse/issues/4409), [\#4426](https://github.com/matrix-org/synapse/issues/4426), [\#4427](https://github.com/matrix-org/synapse/issues/4427), [\#4428](https://github.com/matrix-org/synapse/issues/4428), [\#4464](https://github.com/matrix-org/synapse/issues/4464), [\#4468](https://github.com/matrix-org/synapse/issues/4468), [\#4487](https://github.com/matrix-org/synapse/issues/4487), [\#4488](https://github.com/matrix-org/synapse/issues/4488), [\#4489](https://github.com/matrix-org/synapse/issues/4489), [\#4497](https://github.com/matrix-org/synapse/issues/4497), [\#4511](https://github.com/matrix-org/synapse/issues/4511), [\#4516](https://github.com/matrix-org/synapse/issues/4516), [\#4520](https://github.com/matrix-org/synapse/issues/4520), [\#4521](https://github.com/matrix-org/synapse/issues/4521), [\#4539](https://github.com/matrix-org/synapse/issues/4539), [\#4542](https://github.com/matrix-org/synapse/issues/4542), [\#4544](https://github.com/matrix-org/synapse/issues/4544))
- Search now includes results from predecessor rooms after a room upgrade. ([\#4415](https://github.com/matrix-org/synapse/issues/4415))
- Config option to disable requesting MSISDN on registration. ([\#4423](https://github.com/matrix-org/synapse/issues/4423))
- Add a metric for tracking event stream position of the user directory. ([\#4445](https://github.com/matrix-org/synapse/issues/4445))
- Support exposing server capabilities in CS API (MSC1753, MSC1804) ([\#4472](https://github.com/matrix-org/synapse/issues/4472), [81b7e7eed](https://github.com/matrix-org/synapse/commit/81b7e7eed323f55d6550e7a270a9dc2c4c7b0fe0)))
- Add support for room version 3 ([\#4483](https://github.com/matrix-org/synapse/issues/4483), [\#4499](https://github.com/matrix-org/synapse/issues/4499), [\#4515](https://github.com/matrix-org/synapse/issues/4515), [\#4523](https://github.com/matrix-org/synapse/issues/4523), [\#4535](https://github.com/matrix-org/synapse/issues/4535))
- Synapse will now reload TLS certificates from disk upon SIGHUP. ([\#4495](https://github.com/matrix-org/synapse/issues/4495), [\#4524](https://github.com/matrix-org/synapse/issues/4524))
- The matrixdotorg/synapse Docker images now use Python 3 by default. ([\#4558](https://github.com/matrix-org/synapse/issues/4558))

Bugfixes
--------

- Prevent users with access tokens predating the introduction of device IDs from creating spurious entries in the user_ips table. ([\#4369](https://github.com/matrix-org/synapse/issues/4369))
- Fix typo in ALL_USER_TYPES definition to ensure type is a tuple ([\#4392](https://github.com/matrix-org/synapse/issues/4392))
- Fix high CPU usage due to remote devicelist updates ([\#4397](https://github.com/matrix-org/synapse/issues/4397))
- Fix potential bug where creating or joining a room could fail ([\#4404](https://github.com/matrix-org/synapse/issues/4404))
- Fix bug when rejecting remote invites ([\#4405](https://github.com/matrix-org/synapse/issues/4405), [\#4527](https://github.com/matrix-org/synapse/issues/4527))
- Fix incorrect logcontexts after a Deferred was cancelled ([\#4407](https://github.com/matrix-org/synapse/issues/4407))
- Ensure encrypted room state is persisted across room upgrades. ([\#4411](https://github.com/matrix-org/synapse/issues/4411))
- Copy over whether a room is a direct message and any associated room tags on room upgrade. ([\#4412](https://github.com/matrix-org/synapse/issues/4412))
- Fix None guard in calling config.server.is_threepid_reserved ([\#4435](https://github.com/matrix-org/synapse/issues/4435))
- Don't send IP addresses as SNI ([\#4452](https://github.com/matrix-org/synapse/issues/4452))
- Fix UnboundLocalError in post_urlencoded_get_json ([\#4460](https://github.com/matrix-org/synapse/issues/4460))
- Add a timeout to filtered room directory queries. ([\#4461](https://github.com/matrix-org/synapse/issues/4461))
- Workaround for login error when using both LDAP and internal authentication. ([\#4486](https://github.com/matrix-org/synapse/issues/4486))
- Fix a bug where setting a relative consent directory path would cause a crash. ([\#4512](https://github.com/matrix-org/synapse/issues/4512))


Deprecations and Removals
-------------------------

- Synapse no longer generates self-signed TLS certificates when generating a configuration file. ([\#4509](https://github.com/matrix-org/synapse/issues/4509))


Improved Documentation
----------------------

- Update debian installation instructions ([\#4526](https://github.com/matrix-org/synapse/issues/4526))


Internal Changes
----------------

- Synapse will now take advantage of native UPSERT functionality in PostgreSQL 9.5+ and SQLite 3.24+. ([\#4306](https://github.com/matrix-org/synapse/issues/4306), [\#4459](https://github.com/matrix-org/synapse/issues/4459), [\#4466](https://github.com/matrix-org/synapse/issues/4466), [\#4471](https://github.com/matrix-org/synapse/issues/4471), [\#4477](https://github.com/matrix-org/synapse/issues/4477), [\#4505](https://github.com/matrix-org/synapse/issues/4505))
- Update README to use the new virtualenv everywhere ([\#4342](https://github.com/matrix-org/synapse/issues/4342))
- Add better logging for unexpected errors while sending transactions ([\#4368](https://github.com/matrix-org/synapse/issues/4368))
- Apply a unique index to the user_ips table, preventing duplicates. ([\#4370](https://github.com/matrix-org/synapse/issues/4370), [\#4432](https://github.com/matrix-org/synapse/issues/4432), [\#4434](https://github.com/matrix-org/synapse/issues/4434))
- Silence travis-ci build warnings by removing non-functional python3.6 ([\#4377](https://github.com/matrix-org/synapse/issues/4377))
- Fix a comment in the generated config file ([\#4387](https://github.com/matrix-org/synapse/issues/4387))
- Add ground work for implementing future federation API versions ([\#4390](https://github.com/matrix-org/synapse/issues/4390))
- Update dependencies on msgpack and pymacaroons to use the up-to-date packages. ([\#4399](https://github.com/matrix-org/synapse/issues/4399))
- Tweak codecov settings to make them less loud. ([\#4400](https://github.com/matrix-org/synapse/issues/4400))
- Implement server support for MSC1794 - Federation v2 Invite API ([\#4402](https://github.com/matrix-org/synapse/issues/4402))
- debian package: symlink to explicit python version ([\#4433](https://github.com/matrix-org/synapse/issues/4433))
- Add infrastructure to support different event formats ([\#4437](https://github.com/matrix-org/synapse/issues/4437), [\#4447](https://github.com/matrix-org/synapse/issues/4447), [\#4448](https://github.com/matrix-org/synapse/issues/4448), [\#4470](https://github.com/matrix-org/synapse/issues/4470), [\#4481](https://github.com/matrix-org/synapse/issues/4481), [\#4482](https://github.com/matrix-org/synapse/issues/4482), [\#4493](https://github.com/matrix-org/synapse/issues/4493), [\#4494](https://github.com/matrix-org/synapse/issues/4494), [\#4496](https://github.com/matrix-org/synapse/issues/4496), [\#4510](https://github.com/matrix-org/synapse/issues/4510), [\#4514](https://github.com/matrix-org/synapse/issues/4514))
- Generate the debian config during build ([\#4444](https://github.com/matrix-org/synapse/issues/4444))
- Clarify documentation for the `public_baseurl` config param ([\#4458](https://github.com/matrix-org/synapse/issues/4458), [\#4498](https://github.com/matrix-org/synapse/issues/4498))
- Fix quoting for allowed_local_3pids example config ([\#4476](https://github.com/matrix-org/synapse/issues/4476))
- Remove deprecated --process-dependency-links option from UPGRADE.rst ([\#4485](https://github.com/matrix-org/synapse/issues/4485))
- Make it possible to set the log level for tests via an environment variable ([\#4506](https://github.com/matrix-org/synapse/issues/4506))
- Reduce the log level of linearizer lock acquirement to DEBUG. ([\#4507](https://github.com/matrix-org/synapse/issues/4507))
- Fix code to comply with linting in PyFlakes 3.7.1. ([\#4519](https://github.com/matrix-org/synapse/issues/4519))
- Add some debug for membership syncing issues ([\#4538](https://github.com/matrix-org/synapse/issues/4538))
- Docker: only copy what we need to the build image ([\#4562](https://github.com/matrix-org/synapse/issues/4562))


Synapse 0.34.1.1 (2019-01-11)
=============================

This release fixes CVE-2019-5885 and is recommended for all users of Synapse 0.34.1.

This release is compatible with Python 2.7 and 3.5+. Python 3.7 is fully supported.

Bugfixes
--------

- Fix spontaneous logout on upgrade
  ([\#4374](https://github.com/matrix-org/synapse/issues/4374))


Synapse 0.34.1 (2019-01-09)
===========================

Internal Changes
----------------

- Add better logging for unexpected errors while sending transactions ([\#4361](https://github.com/matrix-org/synapse/issues/4361), [\#4362](https://github.com/matrix-org/synapse/issues/4362))


Synapse 0.34.1rc1 (2019-01-08)
==============================

Features
--------

- Special-case a support user for use in verifying behaviour of a given server. The support user does not appear in user directory or monthly active user counts. ([\#4141](https://github.com/matrix-org/synapse/issues/4141), [\#4344](https://github.com/matrix-org/synapse/issues/4344))
- Support for serving .well-known files ([\#4262](https://github.com/matrix-org/synapse/issues/4262))
- Rework SAML2 authentication ([\#4265](https://github.com/matrix-org/synapse/issues/4265), [\#4267](https://github.com/matrix-org/synapse/issues/4267))
- SAML2 authentication: Initialise user display name from SAML2 data ([\#4272](https://github.com/matrix-org/synapse/issues/4272))
- Synapse can now have its conditional/extra dependencies installed by pip. This functionality can be used by using `pip install matrix-synapse[feature]`, where feature is a comma separated list with the possible values `email.enable_notifs`, `matrix-synapse-ldap3`, `postgres`, `resources.consent`, `saml2`, `url_preview`, and `test`. If you want to install all optional dependencies, you can use "all" instead. ([\#4298](https://github.com/matrix-org/synapse/issues/4298), [\#4325](https://github.com/matrix-org/synapse/issues/4325), [\#4327](https://github.com/matrix-org/synapse/issues/4327))
- Add routes for reading account data. ([\#4303](https://github.com/matrix-org/synapse/issues/4303))
- Add opt-in support for v2 rooms ([\#4307](https://github.com/matrix-org/synapse/issues/4307))
- Add a script to generate a clean config file ([\#4315](https://github.com/matrix-org/synapse/issues/4315))
- Return server data in /login response ([\#4319](https://github.com/matrix-org/synapse/issues/4319))


Bugfixes
--------

- Fix contains_url check to be consistent with other instances in code-base and check that value is an instance of string. ([\#3405](https://github.com/matrix-org/synapse/issues/3405))
- Fix CAS login when username is not valid in an MXID ([\#4264](https://github.com/matrix-org/synapse/issues/4264))
- Send CORS headers for /media/config ([\#4279](https://github.com/matrix-org/synapse/issues/4279))
- Add 'sandbox' to CSP for media reprository ([\#4284](https://github.com/matrix-org/synapse/issues/4284))
- Make the new landing page prettier. ([\#4294](https://github.com/matrix-org/synapse/issues/4294))
- Fix deleting E2E room keys when using old SQLite versions. ([\#4295](https://github.com/matrix-org/synapse/issues/4295))
- The metric synapse_admin_mau:current previously did not update when config.mau_stats_only was set to True ([\#4305](https://github.com/matrix-org/synapse/issues/4305))
- Fixed per-room account data filters ([\#4309](https://github.com/matrix-org/synapse/issues/4309))
- Fix indentation in default config ([\#4313](https://github.com/matrix-org/synapse/issues/4313))
- Fix synapse:latest docker upload ([\#4316](https://github.com/matrix-org/synapse/issues/4316))
- Fix test_metric.py compatibility with prometheus_client 0.5. Contributed by Maarten de Vries <maarten@de-vri.es>. ([\#4317](https://github.com/matrix-org/synapse/issues/4317))
- Avoid packaging _trial_temp directory in -py3 debian packages ([\#4326](https://github.com/matrix-org/synapse/issues/4326))
- Check jinja version for consent resource ([\#4327](https://github.com/matrix-org/synapse/issues/4327))
- fix NPE in /messages by checking if all events were filtered out ([\#4330](https://github.com/matrix-org/synapse/issues/4330))
- Fix `python -m synapse.config` on Python 3. ([\#4356](https://github.com/matrix-org/synapse/issues/4356))


Deprecations and Removals
-------------------------

- Remove the deprecated v1/register API on Python 2. It was never ported to Python 3. ([\#4334](https://github.com/matrix-org/synapse/issues/4334))


Internal Changes
----------------

- Getting URL previews of IP addresses no longer fails on Python 3. ([\#4215](https://github.com/matrix-org/synapse/issues/4215))
- drop undocumented dependency on dateutil ([\#4266](https://github.com/matrix-org/synapse/issues/4266))
- Update the example systemd config to use a virtualenv ([\#4273](https://github.com/matrix-org/synapse/issues/4273))
- Update link to kernel DCO guide ([\#4274](https://github.com/matrix-org/synapse/issues/4274))
- Make isort tox check print diff when it fails ([\#4283](https://github.com/matrix-org/synapse/issues/4283))
- Log room_id in Unknown room errors ([\#4297](https://github.com/matrix-org/synapse/issues/4297))
- Documentation improvements for coturn setup. Contributed by Krithin Sitaram. ([\#4333](https://github.com/matrix-org/synapse/issues/4333))
- Update pull request template to use absolute links ([\#4341](https://github.com/matrix-org/synapse/issues/4341))
- Update README to not lie about required restart when updating TLS certificates ([\#4343](https://github.com/matrix-org/synapse/issues/4343))
- Update debian packaging for compatibility with transitional package ([\#4349](https://github.com/matrix-org/synapse/issues/4349))
- Fix command hint to generate a config file when trying to start without a config file ([\#4353](https://github.com/matrix-org/synapse/issues/4353))
- Add better logging for unexpected errors while sending transactions ([\#4358](https://github.com/matrix-org/synapse/issues/4358))


Synapse 0.34.0 (2018-12-20)
===========================

Synapse 0.34.0 is the first release to fully support Python 3. Synapse will now
run on Python versions 3.5 or 3.6 (as well as 2.7). Support for Python 3.7
remains experimental.

We recommend upgrading to Python 3, but make sure to read the [upgrade
notes](docs/upgrade.md#upgrading-to-v0340) when doing so.

Features
--------

- Add 'sandbox' to CSP for media reprository ([\#4284](https://github.com/matrix-org/synapse/issues/4284))
- Make the new landing page prettier. ([\#4294](https://github.com/matrix-org/synapse/issues/4294))
- Fix deleting E2E room keys when using old SQLite versions. ([\#4295](https://github.com/matrix-org/synapse/issues/4295))
- Add a welcome page for the client API port. Credit to @krombel! ([\#4289](https://github.com/matrix-org/synapse/issues/4289))
- Remove Matrix console from the default distribution ([\#4290](https://github.com/matrix-org/synapse/issues/4290))
- Add option to track MAU stats (but not limit people) ([\#3830](https://github.com/matrix-org/synapse/issues/3830))
- Add an option to enable recording IPs for appservice users ([\#3831](https://github.com/matrix-org/synapse/issues/3831))
- Rename login type `m.login.cas` to `m.login.sso` ([\#4220](https://github.com/matrix-org/synapse/issues/4220))
- Add an option to disable search for homeservers that may not be interested in it. ([\#4230](https://github.com/matrix-org/synapse/issues/4230))


Bugfixes
--------

- Pushrules can now again be made with non-ASCII rule IDs. ([\#4165](https://github.com/matrix-org/synapse/issues/4165))
- The media repository now no longer fails to decode UTF-8 filenames when downloading remote media. ([\#4176](https://github.com/matrix-org/synapse/issues/4176))
- URL previews now correctly decode non-UTF-8 text if the header contains a `<meta http-equiv="Content-Type"` header. ([\#4183](https://github.com/matrix-org/synapse/issues/4183))
- Fix an issue where public consent URLs had two slashes. ([\#4192](https://github.com/matrix-org/synapse/issues/4192))
- Fallback auth now accepts the session parameter on Python 3. ([\#4197](https://github.com/matrix-org/synapse/issues/4197))
- Remove riot.im from the list of trusted Identity Servers in the default configuration ([\#4207](https://github.com/matrix-org/synapse/issues/4207))
- fix start up failure when mau_limit_reserved_threepids set and db is postgres ([\#4211](https://github.com/matrix-org/synapse/issues/4211))
- Fix auto join failures for servers that require user consent ([\#4223](https://github.com/matrix-org/synapse/issues/4223))
- Fix exception caused by non-ascii event IDs ([\#4241](https://github.com/matrix-org/synapse/issues/4241))
- Pushers can now be unsubscribed from on Python 3. ([\#4250](https://github.com/matrix-org/synapse/issues/4250))
- Fix UnicodeDecodeError when postgres is configured to give non-English errors ([\#4253](https://github.com/matrix-org/synapse/issues/4253))


Internal Changes
----------------

- Debian packages utilising a virtualenv with bundled dependencies can now be built. ([\#4212](https://github.com/matrix-org/synapse/issues/4212))
- Disable pager when running git-show in CI ([\#4291](https://github.com/matrix-org/synapse/issues/4291))
- A coveragerc file has been added. ([\#4180](https://github.com/matrix-org/synapse/issues/4180))
- Add a GitHub pull request template and add multiple issue templates ([\#4182](https://github.com/matrix-org/synapse/issues/4182))
- Update README to reflect the fact that [\#1491](https://github.com/matrix-org/synapse/issues/1491) is fixed ([\#4188](https://github.com/matrix-org/synapse/issues/4188))
- Run the AS senders as background processes to fix warnings ([\#4189](https://github.com/matrix-org/synapse/issues/4189))
- Add some diagnostics to the tests to detect logcontext problems ([\#4190](https://github.com/matrix-org/synapse/issues/4190))
- Add missing `jpeg` package prerequisite for OpenBSD in README. ([\#4193](https://github.com/matrix-org/synapse/issues/4193))
- Add a note saying you need to manually reclaim disk space after using the Purge History API ([\#4200](https://github.com/matrix-org/synapse/issues/4200))
- More logcontext checking in unittests ([\#4205](https://github.com/matrix-org/synapse/issues/4205))
- Ignore `__pycache__` directories in the database schema folder ([\#4214](https://github.com/matrix-org/synapse/issues/4214))
- Add note to UPGRADE.rst about removing riot.im from list of trusted identity servers ([\#4224](https://github.com/matrix-org/synapse/issues/4224))
- Added automated coverage reporting to CI. ([\#4225](https://github.com/matrix-org/synapse/issues/4225))
- Garbage-collect after each unit test to fix logcontext leaks ([\#4227](https://github.com/matrix-org/synapse/issues/4227))
- add more detail to logging regarding "More than one row matched" error ([\#4234](https://github.com/matrix-org/synapse/issues/4234))
- Drop sent_transactions table ([\#4244](https://github.com/matrix-org/synapse/issues/4244))
- Add a basic .editorconfig ([\#4257](https://github.com/matrix-org/synapse/issues/4257))
- Update README.rst and UPGRADE.rst for Python 3. ([\#4260](https://github.com/matrix-org/synapse/issues/4260))
- Remove obsolete `verbose` and `log_file` settings from `homeserver.yaml` for Docker image. ([\#4261](https://github.com/matrix-org/synapse/issues/4261))


Synapse 0.33.9 (2018-11-19)
===========================

No significant changes.


Synapse 0.33.9rc1 (2018-11-14)
==============================

Features
--------

- Include flags to optionally add `m.login.terms` to the registration flow when consent tracking is enabled. ([\#4004](https://github.com/matrix-org/synapse/issues/4004), [\#4133](https://github.com/matrix-org/synapse/issues/4133), [\#4142](https://github.com/matrix-org/synapse/issues/4142), [\#4184](https://github.com/matrix-org/synapse/issues/4184))
- Support for replacing rooms with new ones ([\#4091](https://github.com/matrix-org/synapse/issues/4091), [\#4099](https://github.com/matrix-org/synapse/issues/4099), [\#4100](https://github.com/matrix-org/synapse/issues/4100), [\#4101](https://github.com/matrix-org/synapse/issues/4101))


Bugfixes
--------

- Fix exceptions when using the email mailer on Python 3. ([\#4095](https://github.com/matrix-org/synapse/issues/4095))
- Fix e2e key backup with more than 9 backup versions ([\#4113](https://github.com/matrix-org/synapse/issues/4113))
- Searches that request profile info now no longer fail with a 500. ([\#4122](https://github.com/matrix-org/synapse/issues/4122))
- fix return code of empty key backups ([\#4123](https://github.com/matrix-org/synapse/issues/4123))
- If the typing stream ID goes backwards (as on a worker when the master restarts), the worker's typing handler will no longer erroneously report rooms containing new typing events. ([\#4127](https://github.com/matrix-org/synapse/issues/4127))
- Fix table lock of device_lists_remote_cache which could freeze the application ([\#4132](https://github.com/matrix-org/synapse/issues/4132))
- Fix exception when using state res v2 algorithm ([\#4135](https://github.com/matrix-org/synapse/issues/4135))
- Generating the user consent URI no longer fails on Python 3. ([\#4140](https://github.com/matrix-org/synapse/issues/4140), [\#4163](https://github.com/matrix-org/synapse/issues/4163))
- Loading URL previews from the DB cache on Postgres will no longer cause Unicode type errors when responding to the request, and URL previews will no longer fail if the remote server returns a Content-Type header with the chartype in quotes. ([\#4157](https://github.com/matrix-org/synapse/issues/4157))
- The hash_password script now works on Python 3. ([\#4161](https://github.com/matrix-org/synapse/issues/4161))
- Fix noop checks when updating device keys, reducing spurious device list update notifications. ([\#4164](https://github.com/matrix-org/synapse/issues/4164))


Deprecations and Removals
-------------------------

- The disused and un-specced identicon generator has been removed. ([\#4106](https://github.com/matrix-org/synapse/issues/4106))
- The obsolete and non-functional /pull federation endpoint has been removed. ([\#4118](https://github.com/matrix-org/synapse/issues/4118))
- The deprecated v1 key exchange endpoints have been removed. ([\#4119](https://github.com/matrix-org/synapse/issues/4119))
- Synapse will no longer fetch keys using the fallback deprecated v1 key exchange method and will now always use v2. ([\#4120](https://github.com/matrix-org/synapse/issues/4120))


Internal Changes
----------------

- Fix build of Docker image with docker-compose ([\#3778](https://github.com/matrix-org/synapse/issues/3778))
- Delete unreferenced state groups during history purge ([\#4006](https://github.com/matrix-org/synapse/issues/4006))
- The "Received rdata" log messages on workers is now logged at DEBUG, not INFO. ([\#4108](https://github.com/matrix-org/synapse/issues/4108))
- Reduce replication traffic for device lists ([\#4109](https://github.com/matrix-org/synapse/issues/4109))
- Fix `synapse_replication_tcp_protocol_*_commands` metric label to be full command name, rather than just the first character ([\#4110](https://github.com/matrix-org/synapse/issues/4110))
- Log some bits about room creation ([\#4121](https://github.com/matrix-org/synapse/issues/4121))
- Fix `tox` failure on old systems ([\#4124](https://github.com/matrix-org/synapse/issues/4124))
- Add STATE_V2_TEST room version ([\#4128](https://github.com/matrix-org/synapse/issues/4128))
- Clean up event accesses and tests ([\#4137](https://github.com/matrix-org/synapse/issues/4137))
- The default logging config will now set an explicit log file encoding of UTF-8. ([\#4138](https://github.com/matrix-org/synapse/issues/4138))
- Add helpers functions for getting prev and auth events of an event ([\#4139](https://github.com/matrix-org/synapse/issues/4139))
- Add some tests for the HTTP pusher. ([\#4149](https://github.com/matrix-org/synapse/issues/4149))
- add purge_history.sh and purge_remote_media.sh scripts to contrib/ ([\#4155](https://github.com/matrix-org/synapse/issues/4155))
- HTTP tests have been refactored to contain less boilerplate. ([\#4156](https://github.com/matrix-org/synapse/issues/4156))
- Drop incoming events from federation for unknown rooms ([\#4165](https://github.com/matrix-org/synapse/issues/4165))


Synapse 0.33.8 (2018-11-01)
===========================

No significant changes.


Synapse 0.33.8rc2 (2018-10-31)
==============================

Bugfixes
--------

- Searches that request profile info now no longer fail with a 500. Fixes
  a regression in 0.33.8rc1. ([\#4122](https://github.com/matrix-org/synapse/issues/4122))


Synapse 0.33.8rc1 (2018-10-29)
==============================

Features
--------

- Servers with auto-join rooms will now automatically create those rooms when the first user registers ([\#3975](https://github.com/matrix-org/synapse/issues/3975))
- Add config option to control alias creation ([\#4051](https://github.com/matrix-org/synapse/issues/4051))
- The register_new_matrix_user script is now ported to Python 3. ([\#4085](https://github.com/matrix-org/synapse/issues/4085))
- Configure Docker image to listen on both ipv4 and ipv6. ([\#4089](https://github.com/matrix-org/synapse/issues/4089))


Bugfixes
--------

- Fix HTTP error response codes for federated group requests. ([\#3969](https://github.com/matrix-org/synapse/issues/3969))
- Fix issue where Python 3 users couldn't paginate /publicRooms ([\#4046](https://github.com/matrix-org/synapse/issues/4046))
- Fix URL previewing to work in Python 3.7 ([\#4050](https://github.com/matrix-org/synapse/issues/4050))
- synctl will use the right python executable to run worker processes ([\#4057](https://github.com/matrix-org/synapse/issues/4057))
- Manhole now works again on Python 3, instead of failing with a "couldn't match all kex parts" when connecting. ([\#4060](https://github.com/matrix-org/synapse/issues/4060), [\#4067](https://github.com/matrix-org/synapse/issues/4067))
- Fix some metrics being racy and causing exceptions when polled by Prometheus. ([\#4061](https://github.com/matrix-org/synapse/issues/4061))
- Fix bug which prevented email notifications from being sent unless an absolute path was given for `email_templates`. ([\#4068](https://github.com/matrix-org/synapse/issues/4068))
- Correctly account for cpu usage by background threads ([\#4074](https://github.com/matrix-org/synapse/issues/4074))
- Fix race condition where config defined reserved users were not being added to
  the monthly active user list prior to the homeserver reactor firing up ([\#4081](https://github.com/matrix-org/synapse/issues/4081))
- Fix bug which prevented backslashes being used in event field filters ([\#4083](https://github.com/matrix-org/synapse/issues/4083))


Internal Changes
----------------

- Add information about the [matrix-docker-ansible-deploy](https://github.com/spantaleev/matrix-docker-ansible-deploy) playbook ([\#3698](https://github.com/matrix-org/synapse/issues/3698))
- Add initial implementation of new state resolution algorithm ([\#3786](https://github.com/matrix-org/synapse/issues/3786))
- Reduce database load when fetching state groups ([\#4011](https://github.com/matrix-org/synapse/issues/4011))
- Various cleanups in the federation client code ([\#4031](https://github.com/matrix-org/synapse/issues/4031))
- Run the CircleCI builds in docker containers ([\#4041](https://github.com/matrix-org/synapse/issues/4041))
- Only colourise synctl output when attached to tty ([\#4049](https://github.com/matrix-org/synapse/issues/4049))
- Refactor room alias creation code ([\#4063](https://github.com/matrix-org/synapse/issues/4063))
- Make the Python scripts in the top-level scripts folders meet pep8 and pass flake8. ([\#4068](https://github.com/matrix-org/synapse/issues/4068))
- The README now contains example for the Caddy web server. Contributed by steamp0rt. ([\#4072](https://github.com/matrix-org/synapse/issues/4072))
- Add psutil as an explicit dependency ([\#4073](https://github.com/matrix-org/synapse/issues/4073))
- Clean up threading and logcontexts in pushers ([\#4075](https://github.com/matrix-org/synapse/issues/4075))
- Correctly manage logcontexts during startup to fix some "Unexpected logging context" warnings ([\#4076](https://github.com/matrix-org/synapse/issues/4076))
- Give some more things logcontexts ([\#4077](https://github.com/matrix-org/synapse/issues/4077))
- Clean up some bits of code which were flagged by the linter ([\#4082](https://github.com/matrix-org/synapse/issues/4082))


Synapse 0.33.7 (2018-10-18)
===========================

**Warning**: This release removes the example email notification templates from
`res/templates` (they are now internal to the python package). This should only
affect you if you (a) deploy your Synapse instance from a git checkout or a
github snapshot URL, and (b) have email notifications enabled.

If you have email notifications enabled, you should ensure that
`email.template_dir` is either configured to point at a directory where you
have installed customised templates, or leave it unset to use the default
templates.

Synapse 0.33.7rc2 (2018-10-17)
==============================

Features
--------

- Ship the example email templates as part of the package ([\#4052](https://github.com/matrix-org/synapse/issues/4052))

Bugfixes
--------

- Fix bug which made get_missing_events return too few events ([\#4045](https://github.com/matrix-org/synapse/issues/4045))


Synapse 0.33.7rc1 (2018-10-15)
==============================

Features
--------

- Add support for end-to-end key backup (MSC1687) ([\#4019](https://github.com/matrix-org/synapse/issues/4019))


Bugfixes
--------

- Fix bug in event persistence logic which caused 'NoneType is not iterable' ([\#3995](https://github.com/matrix-org/synapse/issues/3995))
- Fix exception in background metrics collection ([\#3996](https://github.com/matrix-org/synapse/issues/3996))
- Fix exception handling in fetching remote profiles ([\#3997](https://github.com/matrix-org/synapse/issues/3997))
- Fix handling of rejected threepid invites ([\#3999](https://github.com/matrix-org/synapse/issues/3999))
- Workers now start on Python 3. ([\#4027](https://github.com/matrix-org/synapse/issues/4027))
- Synapse now starts on Python 3.7. ([\#4033](https://github.com/matrix-org/synapse/issues/4033))


Internal Changes
----------------

- Log exceptions in looping calls ([\#4008](https://github.com/matrix-org/synapse/issues/4008))
- Optimisation for serving federation requests ([\#4017](https://github.com/matrix-org/synapse/issues/4017))
- Add metric to count number of non-empty sync responses ([\#4022](https://github.com/matrix-org/synapse/issues/4022))


Synapse 0.33.6 (2018-10-04)
===========================

Internal Changes
----------------

- Pin to prometheus_client<0.4 to avoid renaming all of our metrics ([\#4002](https://github.com/matrix-org/synapse/issues/4002))


Synapse 0.33.6rc1 (2018-10-03)
==============================

Features
--------

- Adding the ability to change MAX_UPLOAD_SIZE for the docker container variables. ([\#3883](https://github.com/matrix-org/synapse/issues/3883))
- Report "python_version" in the phone home stats ([\#3894](https://github.com/matrix-org/synapse/issues/3894))
- Always LL ourselves if we're in a room ([\#3916](https://github.com/matrix-org/synapse/issues/3916))
- Include eventid in log lines when processing incoming federation transactions ([\#3959](https://github.com/matrix-org/synapse/issues/3959))
- Remove spurious check which made 'localhost' servers not work ([\#3964](https://github.com/matrix-org/synapse/issues/3964))


Bugfixes
--------

- Fix problem when playing media from Chrome using direct URL (thanks @remjey!) ([\#3578](https://github.com/matrix-org/synapse/issues/3578))
- support registering regular users non-interactively with register_new_matrix_user script ([\#3836](https://github.com/matrix-org/synapse/issues/3836))
- Fix broken invite email links for self hosted riots ([\#3868](https://github.com/matrix-org/synapse/issues/3868))
- Don't ratelimit autojoins ([\#3879](https://github.com/matrix-org/synapse/issues/3879))
- Fix 500 error when deleting unknown room alias ([\#3889](https://github.com/matrix-org/synapse/issues/3889))
- Fix some b'abcd' noise in logs and metrics ([\#3892](https://github.com/matrix-org/synapse/issues/3892), [\#3895](https://github.com/matrix-org/synapse/issues/3895))
- When we join a room, always try the server we used for the alias lookup first, to avoid unresponsive and out-of-date servers. ([\#3899](https://github.com/matrix-org/synapse/issues/3899))
- Fix incorrect server-name indication for outgoing federation requests ([\#3907](https://github.com/matrix-org/synapse/issues/3907))
- Fix adding client IPs to the database failing on Python 3. ([\#3908](https://github.com/matrix-org/synapse/issues/3908))
- Fix bug where things occaisonally were not being timed out correctly. ([\#3910](https://github.com/matrix-org/synapse/issues/3910))
- Fix bug where outbound federation would stop talking to some servers when using workers ([\#3914](https://github.com/matrix-org/synapse/issues/3914))
- Fix some instances of ExpiringCache not expiring cache items ([\#3932](https://github.com/matrix-org/synapse/issues/3932), [\#3980](https://github.com/matrix-org/synapse/issues/3980))
- Fix out-of-bounds error when LLing yourself ([\#3936](https://github.com/matrix-org/synapse/issues/3936))
- Sending server notices regarding user consent now works on Python 3. ([\#3938](https://github.com/matrix-org/synapse/issues/3938))
- Fix exceptions from metrics handler ([\#3956](https://github.com/matrix-org/synapse/issues/3956))
- Fix error message for events with m.room.create missing from auth_events ([\#3960](https://github.com/matrix-org/synapse/issues/3960))
- Fix errors due to concurrent monthly_active_user upserts ([\#3961](https://github.com/matrix-org/synapse/issues/3961))
- Fix exceptions when processing incoming events over federation ([\#3968](https://github.com/matrix-org/synapse/issues/3968))
- Replaced all occurences of e.message with str(e). Contributed by Schnuffle ([\#3970](https://github.com/matrix-org/synapse/issues/3970))
- Fix lazy loaded sync in the presence of rejected state events ([\#3986](https://github.com/matrix-org/synapse/issues/3986))
- Fix error when logging incomplete HTTP requests ([\#3990](https://github.com/matrix-org/synapse/issues/3990))


Internal Changes
----------------

- Unit tests can now be run under PostgreSQL in Docker using ``test_postgresql.sh``. ([\#3699](https://github.com/matrix-org/synapse/issues/3699))
- Speed up calculation of typing updates for replication ([\#3794](https://github.com/matrix-org/synapse/issues/3794))
- Remove documentation regarding installation on Cygwin, the use of WSL is recommended instead. ([\#3873](https://github.com/matrix-org/synapse/issues/3873))
- Fix typo in README, synaspse -> synapse ([\#3897](https://github.com/matrix-org/synapse/issues/3897))
- Increase the timeout when filling missing events in federation requests ([\#3903](https://github.com/matrix-org/synapse/issues/3903))
- Improve the logging when handling a federation transaction ([\#3904](https://github.com/matrix-org/synapse/issues/3904), [\#3966](https://github.com/matrix-org/synapse/issues/3966))
- Improve logging of outbound federation requests ([\#3906](https://github.com/matrix-org/synapse/issues/3906), [\#3909](https://github.com/matrix-org/synapse/issues/3909))
- Fix the docker image building on python 3 ([\#3911](https://github.com/matrix-org/synapse/issues/3911))
- Add a regression test for logging failed HTTP requests on Python 3. ([\#3912](https://github.com/matrix-org/synapse/issues/3912))
- Comments and interface cleanup for on_receive_pdu ([\#3924](https://github.com/matrix-org/synapse/issues/3924))
- Fix spurious exceptions when remote http client closes conncetion ([\#3925](https://github.com/matrix-org/synapse/issues/3925))
- Log exceptions thrown by background tasks ([\#3927](https://github.com/matrix-org/synapse/issues/3927))
- Add a cache to get_destination_retry_timings ([\#3933](https://github.com/matrix-org/synapse/issues/3933), [\#3991](https://github.com/matrix-org/synapse/issues/3991))
- Automate pushes to docker hub ([\#3946](https://github.com/matrix-org/synapse/issues/3946))
- Require attrs 16.0.0 or later ([\#3947](https://github.com/matrix-org/synapse/issues/3947))
- Fix incompatibility with python3 on alpine ([\#3948](https://github.com/matrix-org/synapse/issues/3948))
- Run the test suite on the oldest supported versions of our dependencies in CI. ([\#3952](https://github.com/matrix-org/synapse/issues/3952))
- CircleCI now only runs merged jobs on PRs, and commit jobs on develop, master, and release branches. ([\#3957](https://github.com/matrix-org/synapse/issues/3957))
- Fix docstrings and add tests for state store methods ([\#3958](https://github.com/matrix-org/synapse/issues/3958))
- fix docstring for FederationClient.get_state_for_room ([\#3963](https://github.com/matrix-org/synapse/issues/3963))
- Run notify_app_services as a bg process ([\#3965](https://github.com/matrix-org/synapse/issues/3965))
- Clarifications in FederationHandler ([\#3967](https://github.com/matrix-org/synapse/issues/3967))
- Further reduce the docker image size ([\#3972](https://github.com/matrix-org/synapse/issues/3972))
- Build py3 docker images for docker hub too ([\#3976](https://github.com/matrix-org/synapse/issues/3976))
- Updated the installation instructions to point to the matrix-synapse package on PyPI. ([\#3985](https://github.com/matrix-org/synapse/issues/3985))
- Disable USE_FROZEN_DICTS for unittests by default. ([\#3987](https://github.com/matrix-org/synapse/issues/3987))
- Remove unused Jenkins and development related files from the repo. ([\#3988](https://github.com/matrix-org/synapse/issues/3988))
- Improve stacktraces in certain exceptions in the logs ([\#3989](https://github.com/matrix-org/synapse/issues/3989))


Synapse 0.33.5.1 (2018-09-25)
=============================

Internal Changes
----------------

- Fix incompatibility with older Twisted version in tests. Thanks @OlegGirko! ([\#3940](https://github.com/matrix-org/synapse/issues/3940))


Synapse 0.33.5 (2018-09-24)
===========================

No significant changes.


Synapse 0.33.5rc1 (2018-09-17)
==============================

Features
--------

- Python 3.5 and 3.6 support is now in beta. ([\#3576](https://github.com/matrix-org/synapse/issues/3576))
- Implement `event_format` filter param in `/sync` ([\#3790](https://github.com/matrix-org/synapse/issues/3790))
- Add synapse_admin_mau:registered_reserved_users metric to expose number of real reaserved users ([\#3846](https://github.com/matrix-org/synapse/issues/3846))


Bugfixes
--------

- Remove connection ID for replication prometheus metrics, as it creates a large number of new series. ([\#3788](https://github.com/matrix-org/synapse/issues/3788))
- guest users should not be part of mau total ([\#3800](https://github.com/matrix-org/synapse/issues/3800))
- Bump dependency on pyopenssl 16.x, to avoid incompatibility with recent Twisted. ([\#3804](https://github.com/matrix-org/synapse/issues/3804))
- Fix existing room tags not coming down sync when joining a room ([\#3810](https://github.com/matrix-org/synapse/issues/3810))
- Fix jwt import check ([\#3824](https://github.com/matrix-org/synapse/issues/3824))
- fix VOIP crashes under Python 3 (#3821) ([\#3835](https://github.com/matrix-org/synapse/issues/3835))
- Fix manhole so that it works with latest openssh clients ([\#3841](https://github.com/matrix-org/synapse/issues/3841))
- Fix outbound requests occasionally wedging, which can result in federation breaking between servers. ([\#3845](https://github.com/matrix-org/synapse/issues/3845))
- Show heroes if room name/canonical alias has been deleted ([\#3851](https://github.com/matrix-org/synapse/issues/3851))
- Fix handling of redacted events from federation ([\#3859](https://github.com/matrix-org/synapse/issues/3859))
-  ([\#3874](https://github.com/matrix-org/synapse/issues/3874))
- Mitigate outbound federation randomly becoming wedged ([\#3875](https://github.com/matrix-org/synapse/issues/3875))


Internal Changes
----------------

- CircleCI tests now run on the potential merge of a PR. ([\#3704](https://github.com/matrix-org/synapse/issues/3704))
- http/ is now ported to Python 3. ([\#3771](https://github.com/matrix-org/synapse/issues/3771))
- Improve human readable error messages for threepid registration/account update ([\#3789](https://github.com/matrix-org/synapse/issues/3789))
- Make /sync slightly faster by avoiding needless copies ([\#3795](https://github.com/matrix-org/synapse/issues/3795))
- handlers/ is now ported to Python 3. ([\#3803](https://github.com/matrix-org/synapse/issues/3803))
- Limit the number of PDUs/EDUs per federation transaction ([\#3805](https://github.com/matrix-org/synapse/issues/3805))
- Only start postgres instance for postgres tests on Travis CI ([\#3806](https://github.com/matrix-org/synapse/issues/3806))
- tests/ is now ported to Python 3. ([\#3808](https://github.com/matrix-org/synapse/issues/3808))
- crypto/ is now ported to Python 3. ([\#3822](https://github.com/matrix-org/synapse/issues/3822))
- rest/ is now ported to Python 3. ([\#3823](https://github.com/matrix-org/synapse/issues/3823))
- add some logging for the keyring queue ([\#3826](https://github.com/matrix-org/synapse/issues/3826))
- speed up lazy loading by 2-3x ([\#3827](https://github.com/matrix-org/synapse/issues/3827))
- Improved Dockerfile to remove build requirements after building reducing the image size. ([\#3834](https://github.com/matrix-org/synapse/issues/3834))
- Disable lazy loading for incremental syncs for now ([\#3840](https://github.com/matrix-org/synapse/issues/3840))
- federation/ is now ported to Python 3. ([\#3847](https://github.com/matrix-org/synapse/issues/3847))
- Log when we retry outbound requests ([\#3853](https://github.com/matrix-org/synapse/issues/3853))
- Removed some excess logging messages. ([\#3855](https://github.com/matrix-org/synapse/issues/3855))
- Speed up purge history for rooms that have been previously purged ([\#3856](https://github.com/matrix-org/synapse/issues/3856))
- Refactor some HTTP timeout code. ([\#3857](https://github.com/matrix-org/synapse/issues/3857))
- Fix running merged builds on CircleCI ([\#3858](https://github.com/matrix-org/synapse/issues/3858))
- Fix typo in replication stream exception. ([\#3860](https://github.com/matrix-org/synapse/issues/3860))
- Add in flight real time metrics for Measure blocks ([\#3871](https://github.com/matrix-org/synapse/issues/3871))
- Disable buffering and automatic retrying in treq requests to prevent timeouts. ([\#3872](https://github.com/matrix-org/synapse/issues/3872))
- mention jemalloc in the README ([\#3877](https://github.com/matrix-org/synapse/issues/3877))
- Remove unmaintained "nuke-room-from-db.sh" script ([\#3888](https://github.com/matrix-org/synapse/issues/3888))


Synapse 0.33.4 (2018-09-07)
===========================

Internal Changes
----------------

- Unignore synctl in .dockerignore to fix docker builds ([\#3802](https://github.com/matrix-org/synapse/issues/3802))


Synapse 0.33.4rc2 (2018-09-06)
==============================

Pull in security fixes from v0.33.3.1


Synapse 0.33.3.1 (2018-09-06)
=============================

SECURITY FIXES
--------------

- Fix an issue where event signatures were not always correctly validated ([\#3796](https://github.com/matrix-org/synapse/issues/3796))
- Fix an issue where server_acls could be circumvented for incoming events ([\#3796](https://github.com/matrix-org/synapse/issues/3796))


Internal Changes
----------------

- Unignore synctl in .dockerignore to fix docker builds ([\#3802](https://github.com/matrix-org/synapse/issues/3802))


Synapse 0.33.4rc1 (2018-09-04)
==============================

Features
--------

- Support profile API endpoints on workers ([\#3659](https://github.com/matrix-org/synapse/issues/3659))
- Server notices for resource limit blocking ([\#3680](https://github.com/matrix-org/synapse/issues/3680))
- Allow guests to use /rooms/:roomId/event/:eventId ([\#3724](https://github.com/matrix-org/synapse/issues/3724))
- Add mau_trial_days config param, so that users only get counted as MAU after N days. ([\#3749](https://github.com/matrix-org/synapse/issues/3749))
- Require twisted 17.1 or later (fixes [#3741](https://github.com/matrix-org/synapse/issues/3741)). ([\#3751](https://github.com/matrix-org/synapse/issues/3751))


Bugfixes
--------

- Fix error collecting prometheus metrics when run on dedicated thread due to threading concurrency issues ([\#3722](https://github.com/matrix-org/synapse/issues/3722))
- Fix bug where we resent "limit exceeded" server notices repeatedly ([\#3747](https://github.com/matrix-org/synapse/issues/3747))
- Fix bug where we broke sync when using limit_usage_by_mau but hadn't configured server notices ([\#3753](https://github.com/matrix-org/synapse/issues/3753))
- Fix 'federation_domain_whitelist' such that an empty list correctly blocks all outbound federation traffic ([\#3754](https://github.com/matrix-org/synapse/issues/3754))
- Fix tagging of server notice rooms ([\#3755](https://github.com/matrix-org/synapse/issues/3755), [\#3756](https://github.com/matrix-org/synapse/issues/3756))
- Fix 'admin_uri' config variable and error parameter to be 'admin_contact' to match the spec. ([\#3758](https://github.com/matrix-org/synapse/issues/3758))
- Don't return non-LL-member state in incremental sync state blocks ([\#3760](https://github.com/matrix-org/synapse/issues/3760))
- Fix bug in sending presence over federation ([\#3768](https://github.com/matrix-org/synapse/issues/3768))
- Fix bug where preserved threepid user comes to sign up and server is mau blocked ([\#3777](https://github.com/matrix-org/synapse/issues/3777))

Internal Changes
----------------

- Removed the link to the unmaintained matrix-synapse-auto-deploy project from the readme. ([\#3378](https://github.com/matrix-org/synapse/issues/3378))
- Refactor state module to support multiple room versions ([\#3673](https://github.com/matrix-org/synapse/issues/3673))
- The synapse.storage module has been ported to Python 3. ([\#3725](https://github.com/matrix-org/synapse/issues/3725))
- Split the state_group_cache into member and non-member state events (and so speed up LL /sync) ([\#3726](https://github.com/matrix-org/synapse/issues/3726))
- Log failure to authenticate remote servers as warnings (without stack traces) ([\#3727](https://github.com/matrix-org/synapse/issues/3727))
- The CONTRIBUTING guidelines have been updated to mention our use of Markdown and that .misc files have content. ([\#3730](https://github.com/matrix-org/synapse/issues/3730))
- Reference the need for an HTTP replication port when using the federation_reader worker ([\#3734](https://github.com/matrix-org/synapse/issues/3734))
- Fix minor spelling error in federation client documentation. ([\#3735](https://github.com/matrix-org/synapse/issues/3735))
- Remove redundant state resolution function ([\#3737](https://github.com/matrix-org/synapse/issues/3737))
- The test suite now passes on PostgreSQL. ([\#3740](https://github.com/matrix-org/synapse/issues/3740))
- Fix MAU cache invalidation due to missing yield ([\#3746](https://github.com/matrix-org/synapse/issues/3746))
- Make sure that we close db connections opened during init ([\#3764](https://github.com/matrix-org/synapse/issues/3764))


Synapse 0.33.3 (2018-08-22)
===========================

Bugfixes
--------

- Fix bug introduced in v0.33.3rc1 which made the ToS give a 500 error ([\#3732](https://github.com/matrix-org/synapse/issues/3732))


Synapse 0.33.3rc2 (2018-08-21)
==============================

Bugfixes
--------

- Fix bug in v0.33.3rc1 which caused infinite loops and OOMs ([\#3723](https://github.com/matrix-org/synapse/issues/3723))


Synapse 0.33.3rc1 (2018-08-21)
==============================

Features
--------

- Add support for the SNI extension to federation TLS connections. Thanks to @vojeroen! ([\#3439](https://github.com/matrix-org/synapse/issues/3439))
- Add /_media/r0/config ([\#3184](https://github.com/matrix-org/synapse/issues/3184))
- speed up /members API and add `at` and `membership` params as per MSC1227 ([\#3568](https://github.com/matrix-org/synapse/issues/3568))
- implement `summary` block in /sync response as per MSC688 ([\#3574](https://github.com/matrix-org/synapse/issues/3574))
- Add lazy-loading support to /messages as per MSC1227 ([\#3589](https://github.com/matrix-org/synapse/issues/3589))
- Add ability to limit number of monthly active users on the server ([\#3633](https://github.com/matrix-org/synapse/issues/3633))
- Support more federation endpoints on workers ([\#3653](https://github.com/matrix-org/synapse/issues/3653))
- Basic support for room versioning ([\#3654](https://github.com/matrix-org/synapse/issues/3654))
- Ability to disable client/server Synapse via conf toggle ([\#3655](https://github.com/matrix-org/synapse/issues/3655))
- Ability to whitelist specific threepids against monthly active user limiting ([\#3662](https://github.com/matrix-org/synapse/issues/3662))
- Add some metrics for the appservice and federation event sending loops ([\#3664](https://github.com/matrix-org/synapse/issues/3664))
- Where server is disabled, block ability for locked out users to read new messages ([\#3670](https://github.com/matrix-org/synapse/issues/3670))
- set admin uri via config, to be used in error messages where the user should contact the administrator ([\#3687](https://github.com/matrix-org/synapse/issues/3687))
- Synapse's presence functionality can now be disabled with the "use_presence" configuration option. ([\#3694](https://github.com/matrix-org/synapse/issues/3694))
- For resource limit blocked users, prevent writing into rooms ([\#3708](https://github.com/matrix-org/synapse/issues/3708))


Bugfixes
--------

- Fix occasional glitches in the synapse_event_persisted_position metric ([\#3658](https://github.com/matrix-org/synapse/issues/3658))
- Fix bug on deleting 3pid when using identity servers that don't support unbind API ([\#3661](https://github.com/matrix-org/synapse/issues/3661))
- Make the tests pass on Twisted < 18.7.0 ([\#3676](https://github.com/matrix-org/synapse/issues/3676))
- Don’t ship recaptcha_ajax.js, use it directly from Google ([\#3677](https://github.com/matrix-org/synapse/issues/3677))
- Fixes test_reap_monthly_active_users so it passes under postgres ([\#3681](https://github.com/matrix-org/synapse/issues/3681))
- Fix mau blocking calulation bug on login ([\#3689](https://github.com/matrix-org/synapse/issues/3689))
- Fix missing yield in synapse.storage.monthly_active_users.initialise_reserved_users ([\#3692](https://github.com/matrix-org/synapse/issues/3692))
- Improve HTTP request logging to include all requests ([\#3700](https://github.com/matrix-org/synapse/issues/3700))
- Avoid timing out requests while we are streaming back the response ([\#3701](https://github.com/matrix-org/synapse/issues/3701))
- Support more federation endpoints on workers ([\#3705](https://github.com/matrix-org/synapse/issues/3705), [\#3713](https://github.com/matrix-org/synapse/issues/3713))
- Fix "Starting db txn 'get_all_updated_receipts' from sentinel context" warning ([\#3710](https://github.com/matrix-org/synapse/issues/3710))
- Fix bug where `state_cache` cache factor ignored environment variables ([\#3719](https://github.com/matrix-org/synapse/issues/3719))


Deprecations and Removals
-------------------------

- The Shared-Secret registration method of the legacy v1/register REST endpoint has been removed. For a replacement, please see [the admin/register API documentation](https://github.com/matrix-org/synapse/blob/master/docs/admin_api/register_api.rst). ([\#3703](https://github.com/matrix-org/synapse/issues/3703))


Internal Changes
----------------

- The test suite now can run under PostgreSQL. ([\#3423](https://github.com/matrix-org/synapse/issues/3423))
- Refactor HTTP replication endpoints to reduce code duplication ([\#3632](https://github.com/matrix-org/synapse/issues/3632))
- Tests now correctly execute on Python 3. ([\#3647](https://github.com/matrix-org/synapse/issues/3647))
- Sytests can now be run inside a Docker container. ([\#3660](https://github.com/matrix-org/synapse/issues/3660))
- Port over enough to Python 3 to allow the sytests to start. ([\#3668](https://github.com/matrix-org/synapse/issues/3668))
- Update docker base image from alpine 3.7 to 3.8. ([\#3669](https://github.com/matrix-org/synapse/issues/3669))
- Rename synapse.util.async to synapse.util.async_helpers to mitigate async becoming a keyword on Python 3.7. ([\#3678](https://github.com/matrix-org/synapse/issues/3678))
- Synapse's tests are now formatted with the black autoformatter. ([\#3679](https://github.com/matrix-org/synapse/issues/3679))
- Implemented a new testing base class to reduce test boilerplate. ([\#3684](https://github.com/matrix-org/synapse/issues/3684))
- Rename MAU prometheus metrics ([\#3690](https://github.com/matrix-org/synapse/issues/3690))
- add new error type ResourceLimit ([\#3707](https://github.com/matrix-org/synapse/issues/3707))
- Logcontexts for replication command handlers ([\#3709](https://github.com/matrix-org/synapse/issues/3709))
- Update admin register API documentation to reference a real user ID. ([\#3712](https://github.com/matrix-org/synapse/issues/3712))


Synapse 0.33.2 (2018-08-09)
===========================

No significant changes.


Synapse 0.33.2rc1 (2018-08-07)
==============================

Features
--------

- add support for the lazy_loaded_members filter as per MSC1227 ([\#2970](https://github.com/matrix-org/synapse/issues/2970))
- add support for the include_redundant_members filter param as per MSC1227 ([\#3331](https://github.com/matrix-org/synapse/issues/3331))
- Add metrics to track resource usage by background processes ([\#3553](https://github.com/matrix-org/synapse/issues/3553), [\#3556](https://github.com/matrix-org/synapse/issues/3556), [\#3604](https://github.com/matrix-org/synapse/issues/3604), [\#3610](https://github.com/matrix-org/synapse/issues/3610))
- Add `code` label to `synapse_http_server_response_time_seconds` prometheus metric ([\#3554](https://github.com/matrix-org/synapse/issues/3554))
- Add support for client_reader to handle more APIs ([\#3555](https://github.com/matrix-org/synapse/issues/3555), [\#3597](https://github.com/matrix-org/synapse/issues/3597))
- make the /context API filter & lazy-load aware as per MSC1227 ([\#3567](https://github.com/matrix-org/synapse/issues/3567))
- Add ability to limit number of monthly active users on the server ([\#3630](https://github.com/matrix-org/synapse/issues/3630))
- When we fail to join a room over federation, pass the error code back to the client. ([\#3639](https://github.com/matrix-org/synapse/issues/3639))
- Add a new /admin/register API for non-interactively creating users. ([\#3415](https://github.com/matrix-org/synapse/issues/3415))


Bugfixes
--------

- Make /directory/list API return 404 for room not found instead of 400. Thanks to @fuzzmz! ([\#3620](https://github.com/matrix-org/synapse/issues/3620))
- Default inviter_display_name to mxid for email invites ([\#3391](https://github.com/matrix-org/synapse/issues/3391))
- Don't generate TURN credentials if no TURN config options are set ([\#3514](https://github.com/matrix-org/synapse/issues/3514))
- Correctly announce deleted devices over federation ([\#3520](https://github.com/matrix-org/synapse/issues/3520))
- Catch failures saving metrics captured by Measure, and instead log the faulty metrics information for further analysis. ([\#3548](https://github.com/matrix-org/synapse/issues/3548))
- Unicode passwords are now normalised before hashing, preventing the instance where two different devices or browsers might send a different UTF-8 sequence for the password. ([\#3569](https://github.com/matrix-org/synapse/issues/3569))
- Fix potential stack overflow and deadlock under heavy load ([\#3570](https://github.com/matrix-org/synapse/issues/3570))
- Respond with M_NOT_FOUND when profiles are not found locally or over federation. Fixes #3585 ([\#3585](https://github.com/matrix-org/synapse/issues/3585))
- Fix failure to persist events over federation under load ([\#3601](https://github.com/matrix-org/synapse/issues/3601))
- Fix updating of cached remote profiles ([\#3605](https://github.com/matrix-org/synapse/issues/3605))
- Fix 'tuple index out of range' error ([\#3607](https://github.com/matrix-org/synapse/issues/3607))
- Only import secrets when available (fix for py < 3.6) ([\#3626](https://github.com/matrix-org/synapse/issues/3626))


Internal Changes
----------------

- Remove redundant checks on who_forgot_in_room ([\#3350](https://github.com/matrix-org/synapse/issues/3350))
- Remove unnecessary event re-signing hacks ([\#3367](https://github.com/matrix-org/synapse/issues/3367))
- Rewrite cache list decorator ([\#3384](https://github.com/matrix-org/synapse/issues/3384))
- Move v1-only REST APIs into their own module. ([\#3460](https://github.com/matrix-org/synapse/issues/3460))
- Replace more instances of Python 2-only iteritems and itervalues uses. ([\#3562](https://github.com/matrix-org/synapse/issues/3562))
- Refactor EventContext to accept state during init ([\#3577](https://github.com/matrix-org/synapse/issues/3577))
- Improve Dockerfile and docker-compose instructions ([\#3543](https://github.com/matrix-org/synapse/issues/3543))
- Release notes are now in the Markdown format. ([\#3552](https://github.com/matrix-org/synapse/issues/3552))
- add config for pep8 ([\#3559](https://github.com/matrix-org/synapse/issues/3559))
- Merge Linearizer and Limiter ([\#3571](https://github.com/matrix-org/synapse/issues/3571), [\#3572](https://github.com/matrix-org/synapse/issues/3572))
- Lazily load state on master process when using workers to reduce DB consumption ([\#3579](https://github.com/matrix-org/synapse/issues/3579), [\#3581](https://github.com/matrix-org/synapse/issues/3581), [\#3582](https://github.com/matrix-org/synapse/issues/3582), [\#3584](https://github.com/matrix-org/synapse/issues/3584))
- Fixes and optimisations for resolve_state_groups ([\#3586](https://github.com/matrix-org/synapse/issues/3586))
- Improve logging for exceptions when handling PDUs ([\#3587](https://github.com/matrix-org/synapse/issues/3587))
- Add some measure blocks to persist_events ([\#3590](https://github.com/matrix-org/synapse/issues/3590))
- Fix some random logcontext leaks. ([\#3591](https://github.com/matrix-org/synapse/issues/3591), [\#3606](https://github.com/matrix-org/synapse/issues/3606))
- Speed up calculating state deltas in persist_event loop ([\#3592](https://github.com/matrix-org/synapse/issues/3592))
- Attempt to reduce amount of state pulled out of DB during persist_events ([\#3595](https://github.com/matrix-org/synapse/issues/3595))
- Fix a documentation typo in on_make_leave_request ([\#3609](https://github.com/matrix-org/synapse/issues/3609))
- Make EventStore inherit from EventFederationStore ([\#3612](https://github.com/matrix-org/synapse/issues/3612))
- Remove some redundant joins on event_edges.room_id ([\#3613](https://github.com/matrix-org/synapse/issues/3613))
- Stop populating events.content ([\#3614](https://github.com/matrix-org/synapse/issues/3614))
- Update the /send_leave path registration to use event_id rather than a transaction ID. ([\#3616](https://github.com/matrix-org/synapse/issues/3616))
- Refactor FederationHandler to move DB writes into separate functions ([\#3621](https://github.com/matrix-org/synapse/issues/3621))
- Remove unused field "pdu_failures" from transactions. ([\#3628](https://github.com/matrix-org/synapse/issues/3628))
- rename replication_layer to federation_client ([\#3634](https://github.com/matrix-org/synapse/issues/3634))
- Factor out exception handling in federation_client ([\#3638](https://github.com/matrix-org/synapse/issues/3638))
- Refactor location of docker build script. ([\#3644](https://github.com/matrix-org/synapse/issues/3644))
- Update CONTRIBUTING to mention newsfragments. ([\#3645](https://github.com/matrix-org/synapse/issues/3645))


Synapse 0.33.1 (2018-08-02)
===========================

SECURITY FIXES
--------------

- Fix a potential issue where servers could request events for rooms they have not joined. ([\#3641](https://github.com/matrix-org/synapse/issues/3641))
- Fix a potential issue where users could see events in private rooms before they joined. ([\#3642](https://github.com/matrix-org/synapse/issues/3642))

Synapse 0.33.0 (2018-07-19)
===========================

Bugfixes
--------

-   Disable a noisy warning about logcontexts. ([\#3561](https://github.com/matrix-org/synapse/issues/3561))

Synapse 0.33.0rc1 (2018-07-18)
==============================

Features
--------

-   Enforce the specified API for report\_event. ([\#3316](https://github.com/matrix-org/synapse/issues/3316))
-   Include CPU time from database threads in request/block metrics. ([\#3496](https://github.com/matrix-org/synapse/issues/3496), [\#3501](https://github.com/matrix-org/synapse/issues/3501))
-   Add CPU metrics for \_fetch\_event\_list. ([\#3497](https://github.com/matrix-org/synapse/issues/3497))
-   Optimisation to make handling incoming federation requests more efficient. ([\#3541](https://github.com/matrix-org/synapse/issues/3541))

Bugfixes
--------

-   Fix a significant performance regression in /sync. ([\#3505](https://github.com/matrix-org/synapse/issues/3505), [\#3521](https://github.com/matrix-org/synapse/issues/3521), [\#3530](https://github.com/matrix-org/synapse/issues/3530), [\#3544](https://github.com/matrix-org/synapse/issues/3544))
-   Use more portable syntax in our use of the attrs package, widening the supported versions. ([\#3498](https://github.com/matrix-org/synapse/issues/3498))
-   Fix queued federation requests being processed in the wrong order. ([\#3533](https://github.com/matrix-org/synapse/issues/3533))
-   Ensure that erasure requests are correctly honoured for publicly accessible rooms when accessed over federation. ([\#3546](https://github.com/matrix-org/synapse/issues/3546))

Misc
----

-   Refactoring to improve testability. ([\#3351](https://github.com/matrix-org/synapse/issues/3351), [\#3499](https://github.com/matrix-org/synapse/issues/3499))
-   Use `isort` to sort imports. ([\#3463](https://github.com/matrix-org/synapse/issues/3463), [\#3464](https://github.com/matrix-org/synapse/issues/3464), [\#3540](https://github.com/matrix-org/synapse/issues/3540))
-   Use parse and asserts from http.servlet. ([\#3534](https://github.com/matrix-org/synapse/issues/3534), [\#3535](https://github.com/matrix-org/synapse/issues/3535)).

Synapse 0.32.2 (2018-07-07)
===========================

Bugfixes
--------

-   Amend the Python dependencies to depend on attrs from PyPI, not attr ([\#3492](https://github.com/matrix-org/synapse/issues/3492))

Synapse 0.32.1 (2018-07-06)
===========================

Bugfixes
--------

-   Add explicit dependency on netaddr ([\#3488](https://github.com/matrix-org/synapse/issues/3488))

Changes in synapse v0.32.0 (2018-07-06)
=======================================

No changes since 0.32.0rc1

Synapse 0.32.0rc1 (2018-07-05)
==============================

Features
--------

-   Add blacklist & whitelist of servers allowed to send events to a room via `m.room.server_acl` event.
-   Cache factor override system for specific caches ([\#3334](https://github.com/matrix-org/synapse/issues/3334))
-   Add metrics to track appservice transactions ([\#3344](https://github.com/matrix-org/synapse/issues/3344))
-   Try to log more helpful info when a sig verification fails ([\#3372](https://github.com/matrix-org/synapse/issues/3372))
-   Synapse now uses the best performing JSON encoder/decoder according to your runtime (simplejson on CPython, stdlib json on PyPy). ([\#3462](https://github.com/matrix-org/synapse/issues/3462))
-   Add optional ip\_range\_whitelist param to AS registration files to lock AS IP access ([\#3465](https://github.com/matrix-org/synapse/issues/3465))
-   Reject invalid server names in federation requests ([\#3480](https://github.com/matrix-org/synapse/issues/3480))
-   Reject invalid server names in homeserver.yaml ([\#3483](https://github.com/matrix-org/synapse/issues/3483))

Bugfixes
--------

-   Strip access\_token from outgoing requests ([\#3327](https://github.com/matrix-org/synapse/issues/3327))
-   Redact AS tokens in logs ([\#3349](https://github.com/matrix-org/synapse/issues/3349))
-   Fix federation backfill from SQLite servers ([\#3355](https://github.com/matrix-org/synapse/issues/3355))
-   Fix event-purge-by-ts admin API ([\#3363](https://github.com/matrix-org/synapse/issues/3363))
-   Fix event filtering in get\_missing\_events handler ([\#3371](https://github.com/matrix-org/synapse/issues/3371))
-   Synapse is now stricter regarding accepting events which it cannot retrieve the prev\_events for. ([\#3456](https://github.com/matrix-org/synapse/issues/3456))
-   Fix bug where synapse would explode when receiving unicode in HTTP User-Agent header ([\#3470](https://github.com/matrix-org/synapse/issues/3470))
-   Invalidate cache on correct thread to avoid race ([\#3473](https://github.com/matrix-org/synapse/issues/3473))

Improved Documentation
----------------------

-   `doc/postgres.rst`: fix display of the last command block. Thanks to @ArchangeGabriel! ([\#3340](https://github.com/matrix-org/synapse/issues/3340))

Deprecations and Removals
-------------------------

-   Remove was\_forgotten\_at ([\#3324](https://github.com/matrix-org/synapse/issues/3324))

Misc
----

-   [\#3332](https://github.com/matrix-org/synapse/issues/3332), [\#3341](https://github.com/matrix-org/synapse/issues/3341), [\#3347](https://github.com/matrix-org/synapse/issues/3347), [\#3348](https://github.com/matrix-org/synapse/issues/3348), [\#3356](https://github.com/matrix-org/synapse/issues/3356), [\#3385](https://github.com/matrix-org/synapse/issues/3385), [\#3446](https://github.com/matrix-org/synapse/issues/3446), [\#3447](https://github.com/matrix-org/synapse/issues/3447), [\#3467](https://github.com/matrix-org/synapse/issues/3467), [\#3474](https://github.com/matrix-org/synapse/issues/3474)

Changes in synapse v0.31.2 (2018-06-14)
=======================================

SECURITY UPDATE: Prevent unauthorised users from setting state events in a room when there is no `m.room.power_levels` event in force in the room. (PR #3397)

Discussion around the Matrix Spec change proposal for this change can be followed at <https://github.com/matrix-org/matrix-doc/issues/1304>.

Changes in synapse v0.31.1 (2018-06-08)
=======================================

v0.31.1 fixes a security bug in the `get_missing_events` federation API where event visibility rules were not applied correctly.

We are not aware of it being actively exploited but please upgrade asap.

Bug Fixes:

-   Fix event filtering in get\_missing\_events handler (PR #3371)

Changes in synapse v0.31.0 (2018-06-06)
=======================================

Most notable change from v0.30.0 is to switch to the python prometheus library to improve system stats reporting. WARNING: this changes a number of prometheus metrics in a backwards-incompatible manner. For more details, see [docs/metrics-howto.rst](docs/metrics-howto.rst#removal-of-deprecated-metrics--time-based-counters-becoming-histograms-in-0310).

Bug Fixes:

-   Fix metric documentation tables (PR #3341)
-   Fix LaterGauge error handling (694968f)
-   Fix replication metrics (b7e7fd2)

Changes in synapse v0.31.0-rc1 (2018-06-04)
===========================================

Features:

-   Switch to the Python Prometheus library (PR #3256, #3274)
-   Let users leave the server notice room after joining (PR #3287)

Changes:

-   daily user type phone home stats (PR #3264)
-   Use iter\* methods for \_filter\_events\_for\_server (PR #3267)
-   Docs on consent bits (PR #3268)
-   Remove users from user directory on deactivate (PR #3277)
-   Avoid sending consent notice to guest users (PR #3288)
-   disable CPUMetrics if no /proc/self/stat (PR #3299)
-   Consistently use six\'s iteritems and wrap lazy keys/values in list() if they\'re not meant to be lazy (PR #3307)
-   Add private IPv6 addresses to example config for url preview blacklist (PR #3317) Thanks to @thegcat!
-   Reduce stuck read-receipts: ignore depth when updating (PR #3318)
-   Put python\'s logs into Trial when running unit tests (PR #3319)

Changes, python 3 migration:

-   Replace some more comparisons with six (PR #3243) Thanks to @NotAFile!
-   replace some iteritems with six (PR #3244) Thanks to @NotAFile!
-   Add batch\_iter to utils (PR #3245) Thanks to @NotAFile!
-   use repr, not str (PR #3246) Thanks to @NotAFile!
-   Misc Python3 fixes (PR #3247) Thanks to @NotAFile!
-   Py3 storage/\_base.py (PR #3278) Thanks to @NotAFile!
-   more six iteritems (PR #3279) Thanks to @NotAFile!
-   More Misc. py3 fixes (PR #3280) Thanks to @NotAFile!
-   remaining isintance fixes (PR #3281) Thanks to @NotAFile!
-   py3-ize state.py (PR #3283) Thanks to @NotAFile!
-   extend tox testing for py3 to avoid regressions (PR #3302) Thanks to @krombel!
-   use memoryview in py3 (PR #3303) Thanks to @NotAFile!

Bugs:

-   Fix federation backfill bugs (PR #3261)
-   federation: fix LaterGauge usage (PR #3328) Thanks to @intelfx!

Changes in synapse v0.30.0 (2018-05-24)
=======================================

\'Server Notices\' are a new feature introduced in Synapse 0.30. They provide a channel whereby server administrators can send messages to users on the server.

They are used as part of communication of the server policies (see `docs/consent_tracking.md`), however the intention is that they may also find a use for features such as \"Message of the day\".

This feature is specific to Synapse, but uses standard Matrix communication mechanisms, so should work with any Matrix client. For more details see `docs/server_notices.md`

Further Server Notices/Consent Tracking Support:

-   Allow overriding the server\_notices user\'s avatar (PR #3273)
-   Use the localpart in the consent uri (PR #3272)
-   Support for putting %(consent\_uri)s in messages (PR #3271)
-   Block attempts to send server notices to remote users (PR #3270)
-   Docs on consent bits (PR #3268)

Changes in synapse v0.30.0-rc1 (2018-05-23)
===========================================

Server Notices/Consent Tracking Support:

-   ConsentResource to gather policy consent from users (PR #3213)
-   Move RoomCreationHandler out of synapse.handlers.Handlers (PR #3225)
-   Infrastructure for a server notices room (PR #3232)
-   Send users a server notice about consent (PR #3236)
-   Reject attempts to send event before privacy consent is given (PR #3257)
-   Add a \'has\_consented\' template var to consent forms (PR #3262)
-   Fix dependency on jinja2 (PR #3263)

Features:

-   Cohort analytics (PR #3163, #3241, #3251)
-   Add lxml to docker image for web previews (PR #3239) Thanks to @ptman!
-   Add in flight request metrics (PR #3252)

Changes:

-   Remove unused update\_external\_syncs (PR #3233)
-   Use stream rather depth ordering for push actions (PR #3212)
-   Make purge\_history operate on tokens (PR #3221)
-   Don\'t support limitless pagination (PR #3265)

Bug Fixes:

-   Fix logcontext resource usage tracking (PR #3258)
-   Fix error in handling receipts (PR #3235)
-   Stop the transaction cache caching failures (PR #3255)

Changes in synapse v0.29.1 (2018-05-17)
=======================================

Changes:

-   Update docker documentation (PR #3222)

Changes in synapse v0.29.0 (2018-05-16)
=======================================

Not changes since v0.29.0-rc1

Changes in synapse v0.29.0-rc1 (2018-05-14)
===========================================

Notable changes, a docker file for running Synapse (Thanks to @kaiyou!) and a closed spec bug in the Client Server API. Additionally further prep for Python 3 migration.

Potentially breaking change:

-   Make Client-Server API return 401 for invalid token (PR #3161).

    This changes the Client-server spec to return a 401 error code instead of 403 when the access token is unrecognised. This is the behaviour required by the specification, but some clients may be relying on the old, incorrect behaviour.

    Thanks to @NotAFile for fixing this.

Features:

-   Add a Dockerfile for synapse (PR #2846) Thanks to @kaiyou!

Changes - General:

-   nuke-room-from-db.sh: added postgresql option and help (PR #2337) Thanks to @rubo77!
-   Part user from rooms on account deactivate (PR #3201)
-   Make \'unexpected logging context\' into warnings (PR #3007)
-   Set Server header in SynapseRequest (PR #3208)
-   remove duplicates from groups tables (PR #3129)
-   Improve exception handling for background processes (PR #3138)
-   Add missing consumeErrors to improve exception handling (PR #3139)
-   reraise exceptions more carefully (PR #3142)
-   Remove redundant call to preserve\_fn (PR #3143)
-   Trap exceptions thrown within run\_in\_background (PR #3144)

Changes - Refactors:

-   Refactor /context to reuse pagination storage functions (PR #3193)
-   Refactor recent events func to use pagination func (PR #3195)
-   Refactor pagination DB API to return concrete type (PR #3196)
-   Refactor get\_recent\_events\_for\_room return type (PR #3198)
-   Refactor sync APIs to reuse pagination API (PR #3199)
-   Remove unused code path from member change DB func (PR #3200)
-   Refactor request handling wrappers (PR #3203)
-   transaction\_id, destination defined twice (PR #3209) Thanks to @damir-manapov!
-   Refactor event storage to prepare for changes in state calculations (PR #3141)
-   Set Server header in SynapseRequest (PR #3208)
-   Use deferred.addTimeout instead of time\_bound\_deferred (PR #3127, #3178)
-   Use run\_in\_background in preference to preserve\_fn (PR #3140)

Changes - Python 3 migration:

-   Construct HMAC as bytes on py3 (PR #3156) Thanks to @NotAFile!
-   run config tests on py3 (PR #3159) Thanks to @NotAFile!
-   Open certificate files as bytes (PR #3084) Thanks to @NotAFile!
-   Open config file in non-bytes mode (PR #3085) Thanks to @NotAFile!
-   Make event properties raise AttributeError instead (PR #3102) Thanks to @NotAFile!
-   Use six.moves.urlparse (PR #3108) Thanks to @NotAFile!
-   Add py3 tests to tox with folders that work (PR #3145) Thanks to @NotAFile!
-   Don\'t yield in list comprehensions (PR #3150) Thanks to @NotAFile!
-   Move more xrange to six (PR #3151) Thanks to @NotAFile!
-   make imports local (PR #3152) Thanks to @NotAFile!
-   move httplib import to six (PR #3153) Thanks to @NotAFile!
-   Replace stringIO imports with six (PR #3154, #3168) Thanks to @NotAFile!
-   more bytes strings (PR #3155) Thanks to @NotAFile!

Bug Fixes:

-   synapse fails to start under Twisted \>= 18.4 (PR #3157)
-   Fix a class of logcontext leaks (PR #3170)
-   Fix a couple of logcontext leaks in unit tests (PR #3172)
-   Fix logcontext leak in media repo (PR #3174)
-   Escape label values in prometheus metrics (PR #3175, #3186)
-   Fix \'Unhandled Error\' logs with Twisted 18.4 (PR #3182) Thanks to @Half-Shot!
-   Fix logcontext leaks in rate limiter (PR #3183)
-   notifications: Convert next\_token to string according to the spec (PR #3190) Thanks to @mujx!
-   nuke-room-from-db.sh: fix deletion from search table (PR #3194) Thanks to @rubo77!
-   add guard for None on purge\_history api (PR #3160) Thanks to @krombel!

Changes in synapse v0.28.1 (2018-05-01)
=======================================

SECURITY UPDATE

-   Clamp the allowed values of event depth received over federation to be \[0, 2\^63 - 1\]. This mitigates an attack where malicious events injected with depth = 2\^63 - 1 render rooms unusable. Depth is used to determine the cosmetic ordering of events within a room, and so the ordering of events in such a room will default to using stream\_ordering rather than depth (topological\_ordering).

    This is a temporary solution to mitigate abuse in the wild, whilst a long term solution is being implemented to improve how the depth parameter is used.

    Full details at <https://docs.google.com/document/d/1I3fi2S-XnpO45qrpCsowZv8P8dHcNZ4fsBsbOW7KABI>

-   Pin Twisted to \<18.4 until we stop using the private \_OpenSSLECCurve API.

Changes in synapse v0.28.0 (2018-04-26)
=======================================

Bug Fixes:

-   Fix quarantine media admin API and search reindex (PR #3130)
-   Fix media admin APIs (PR #3134)

Changes in synapse v0.28.0-rc1 (2018-04-24)
===========================================

Minor performance improvement to federation sending and bug fixes.

(Note: This release does not include the delta state resolution implementation discussed in matrix live)

Features:

-   Add metrics for event processing lag (PR #3090)
-   Add metrics for ResponseCache (PR #3092)

Changes:

-   Synapse on PyPy (PR #2760) Thanks to @Valodim!
-   move handling of auto\_join\_rooms to RegisterHandler (PR #2996) Thanks to @krombel!
-   Improve handling of SRV records for federation connections (PR #3016) Thanks to @silkeh!
-   Document the behaviour of ResponseCache (PR #3059)
-   Preparation for py3 (PR #3061, #3073, #3074, #3075, #3103, #3104, #3106, #3107, #3109, #3110) Thanks to @NotAFile!
-   update prometheus dashboard to use new metric names (PR #3069) Thanks to @krombel!
-   use python3-compatible prints (PR #3074) Thanks to @NotAFile!
-   Send federation events concurrently (PR #3078)
-   Limit concurrent event sends for a room (PR #3079)
-   Improve R30 stat definition (PR #3086)
-   Send events to ASes concurrently (PR #3088)
-   Refactor ResponseCache usage (PR #3093)
-   Clarify that SRV may not point to a CNAME (PR #3100) Thanks to @silkeh!
-   Use str(e) instead of e.message (PR #3103) Thanks to @NotAFile!
-   Use six.itervalues in some places (PR #3106) Thanks to @NotAFile!
-   Refactor store.have\_events (PR #3117)

Bug Fixes:

-   Return 401 for invalid access\_token on logout (PR #2938) Thanks to @dklug!
-   Return a 404 rather than a 500 on rejoining empty rooms (PR #3080)
-   fix federation\_domain\_whitelist (PR #3099)
-   Avoid creating events with huge numbers of prev\_events (PR #3113)
-   Reject events which have lots of prev\_events (PR #3118)

Changes in synapse v0.27.4 (2018-04-13)
=======================================

Changes:

-   Update canonicaljson dependency (\#3095)

Changes in synapse v0.27.3 (2018-04-11)
======================================

Bug fixes:

-   URL quote path segments over federation (\#3082)

Changes in synapse v0.27.3-rc2 (2018-04-09)
===========================================

v0.27.3-rc1 used a stale version of the develop branch so the changelog overstates the functionality. v0.27.3-rc2 is up to date, rc1 should be ignored.

Changes in synapse v0.27.3-rc1 (2018-04-09)
===========================================

Notable changes include API support for joinability of groups. Also new metrics and phone home stats. Phone home stats include better visibility of system usage so we can tweak synpase to work better for all users rather than our own experience with matrix.org. Also, recording \'r30\' stat which is the measure we use to track overal growth of the Matrix ecosystem. It is defined as:-

Counts the number of native 30 day retained users, defined as:- \* Users who have created their accounts more than 30 days

:   -   Where last seen at most 30 days ago
    -   Where account creation and last\_seen are \> 30 days\"

Features:

-   Add joinability for groups (PR #3045)
-   Implement group join API (PR #3046)
-   Add counter metrics for calculating state delta (PR #3033)
-   R30 stats (PR #3041)
-   Measure time it takes to calculate state group ID (PR #3043)
-   Add basic performance statistics to phone home (PR #3044)
-   Add response size metrics (PR #3071)
-   phone home cache size configurations (PR #3063)

Changes:

-   Add a blurb explaining the main synapse worker (PR #2886) Thanks to @turt2live!
-   Replace old style error catching with \'as\' keyword (PR #3000) Thanks to @NotAFile!
-   Use .iter\* to avoid copies in StateHandler (PR #3006)
-   Linearize calls to \_generate\_user\_id (PR #3029)
-   Remove last usage of ujson (PR #3030)
-   Use simplejson throughout (PR #3048)
-   Use static JSONEncoders (PR #3049)
-   Remove uses of events.content (PR #3060)
-   Improve database cache performance (PR #3068)

Bug fixes:

-   Add room\_id to the response of rooms/{roomId}/join (PR #2986) Thanks to @jplatte!
-   Fix replication after switch to simplejson (PR #3015)
-   404 correctly on missing paths via NoResource (PR #3022)
-   Fix error when claiming e2e keys from offline servers (PR #3034)
-   fix tests/storage/test\_user\_directory.py (PR #3042)
-   use PUT instead of POST for federating groups/m.join\_policy (PR #3070) Thanks to @krombel!
-   postgres port script: fix state\_groups\_pkey error (PR #3072)

Changes in synapse v0.27.2 (2018-03-26)
=======================================

Bug fixes:

-   Fix bug which broke TCP replication between workers (PR #3015)

Changes in synapse v0.27.1 (2018-03-26)
=======================================

Meta release as v0.27.0 temporarily pointed to the wrong commit

Changes in synapse v0.27.0 (2018-03-26)
=======================================

No changes since v0.27.0-rc2

Changes in synapse v0.27.0-rc2 (2018-03-19)
===========================================

Pulls in v0.26.1

Bug fixes:

-   Fix bug introduced in v0.27.0-rc1 that causes much increased memory usage in state cache (PR #3005)

Changes in synapse v0.26.1 (2018-03-15)
=======================================

Bug fixes:

-   Fix bug where an invalid event caused server to stop functioning correctly, due to parsing and serializing bugs in ujson library (PR #3008)

Changes in synapse v0.27.0-rc1 (2018-03-14)
===========================================

The common case for running Synapse is not to run separate workers, but for those that do, be aware that synctl no longer starts the main synapse when using `-a` option with workers. A new worker file should be added with `worker_app: synapse.app.homeserver`.

This release also begins the process of renaming a number of the metrics reported to prometheus. See [docs/metrics-howto.rst](docs/metrics-howto.rst#block-and-response-metrics-renamed-for-0-27-0). Note that the v0.28.0 release will remove the deprecated metric names.

Features:

-   Add ability for ASes to override message send time (PR #2754)
-   Add support for custom storage providers for media repository (PR #2867, #2777, #2783, #2789, #2791, #2804, #2812, #2814, #2857, #2868, #2767)
-   Add purge API features, see [docs/admin\_api/purge\_history\_api.rst](docs/admin_api/purge_history_api.rst) for full details (PR #2858, #2867, #2882, #2946, #2962, #2943)
-   Add support for whitelisting 3PIDs that users can register. (PR #2813)
-   Add `/room/{id}/event/{id}` API (PR #2766)
-   Add an admin API to get all the media in a room (PR #2818) Thanks to @turt2live!
-   Add `federation_domain_whitelist` option (PR #2820, #2821)

Changes:

-   Continue to factor out processing from main process and into worker processes. See updated [docs/workers.rst](docs/workers.rst) (PR #2892 - \#2904, #2913, #2920 - \#2926, #2947, #2847, #2854, #2872, #2873, #2874, #2928, #2929, #2934, #2856, #2976 - \#2984, #2987 - \#2989, #2991 - \#2993, #2995, #2784)
-   Ensure state cache is used when persisting events (PR #2864, #2871, #2802, #2835, #2836, #2841, #2842, #2849)
-   Change the default config to bind on both IPv4 and IPv6 on all platforms (PR #2435) Thanks to @silkeh!
-   No longer require a specific version of saml2 (PR #2695) Thanks to @okurz!
-   Remove `verbosity`/`log_file` from generated config (PR #2755)
-   Add and improve metrics and logging (PR #2770, #2778, #2785, #2786, #2787, #2793, #2794, #2795, #2809, #2810, #2833, #2834, #2844, #2965, #2927, #2975, #2790, #2796, #2838)
-   When using synctl with workers, don\'t start the main synapse automatically (PR #2774)
-   Minor performance improvements (PR #2773, #2792)
-   Use a connection pool for non-federation outbound connections (PR #2817)
-   Make it possible to run unit tests against postgres (PR #2829)
-   Update pynacl dependency to 1.2.1 or higher (PR #2888) Thanks to @bachp!
-   Remove ability for AS users to call /events and /sync (PR #2948)
-   Use bcrypt.checkpw (PR #2949) Thanks to @krombel!

Bug fixes:

-   Fix broken `ldap_config` config option (PR #2683) Thanks to @seckrv!
-   Fix error message when user is not allowed to unban (PR #2761) Thanks to @turt2live!
-   Fix publicised groups GET API (singular) over federation (PR #2772)
-   Fix user directory when using `user_directory_search_all_users` config option (PR #2803, #2831)
-   Fix error on `/publicRooms` when no rooms exist (PR #2827)
-   Fix bug in quarantine\_media (PR #2837)
-   Fix url\_previews when no Content-Type is returned from URL (PR #2845)
-   Fix rare race in sync API when joining room (PR #2944)
-   Fix slow event search, switch back from GIST to GIN indexes (PR #2769, #2848)

Changes in synapse v0.26.0 (2018-01-05)
=======================================

No changes since v0.26.0-rc1

Changes in synapse v0.26.0-rc1 (2017-12-13)
===========================================

Features:

-   Add ability for ASes to publicise groups for their users (PR #2686)
-   Add all local users to the user\_directory and optionally search them (PR #2723)
-   Add support for custom login types for validating users (PR #2729)

Changes:

-   Update example Prometheus config to new format (PR #2648) Thanks to @krombel!
-   Rename redact\_content option to include\_content in Push API (PR #2650)
-   Declare support for r0.3.0 (PR #2677)
-   Improve upserts (PR #2684, #2688, #2689, #2713)
-   Improve documentation of workers (PR #2700)
-   Improve tracebacks on exceptions (PR #2705)
-   Allow guest access to group APIs for reading (PR #2715)
-   Support for posting content in federation\_client script (PR #2716)
-   Delete devices and pushers on logouts etc (PR #2722)

Bug fixes:

-   Fix database port script (PR #2673)
-   Fix internal server error on login with ldap\_auth\_provider (PR #2678) Thanks to @jkolo!
-   Fix error on sqlite 3.7 (PR #2697)
-   Fix OPTIONS on preview\_url (PR #2707)
-   Fix error handling on dns lookup (PR #2711)
-   Fix wrong avatars when inviting multiple users when creating room (PR #2717)
-   Fix 500 when joining matrix-dev (PR #2719)

Changes in synapse v0.25.1 (2017-11-17)
=======================================

Bug fixes:

-   Fix login with LDAP and other password provider modules (PR #2678). Thanks to @jkolo!

Changes in synapse v0.25.0 (2017-11-15)
=======================================

Bug fixes:

-   Fix port script (PR #2673)

Changes in synapse v0.25.0-rc1 (2017-11-14)
===========================================

Features:

-   Add is\_public to groups table to allow for private groups (PR #2582)
-   Add a route for determining who you are (PR #2668) Thanks to @turt2live!
-   Add more features to the password providers (PR #2608, #2610, #2620, #2622, #2623, #2624, #2626, #2628, #2629)
-   Add a hook for custom rest endpoints (PR #2627)
-   Add API to update group room visibility (PR #2651)

Changes:

-   Ignore \<noscript\> tags when generating URL preview descriptions (PR #2576) Thanks to @maximevaillancourt!
-   Register some /unstable endpoints in /r0 as well (PR #2579) Thanks to @krombel!
-   Support /keys/upload on /r0 as well as /unstable (PR #2585)
-   Front-end proxy: pass through auth header (PR #2586)
-   Allow ASes to deactivate their own users (PR #2589)
-   Remove refresh tokens (PR #2613)
-   Automatically set default displayname on register (PR #2617)
-   Log login requests (PR #2618)
-   Always return is\_public in the /groups/:group\_id/rooms API (PR #2630)
-   Avoid no-op media deletes (PR #2637) Thanks to @spantaleev!
-   Fix various embarrassing typos around user\_directory and add some doc. (PR #2643)
-   Return whether a user is an admin within a group (PR #2647)
-   Namespace visibility options for groups (PR #2657)
-   Downcase UserIDs on registration (PR #2662)
-   Cache failures when fetching URL previews (PR #2669)

Bug fixes:

-   Fix port script (PR #2577)
-   Fix error when running synapse with no logfile (PR #2581)
-   Fix UI auth when deleting devices (PR #2591)
-   Fix typo when checking if user is invited to group (PR #2599)
-   Fix the port script to drop NUL values in all tables (PR #2611)
-   Fix appservices being backlogged and not receiving new events due to a bug in notify\_interested\_services (PR #2631) Thanks to @xyzz!
-   Fix updating rooms avatar/display name when modified by admin (PR #2636) Thanks to @farialima!
-   Fix bug in state group storage (PR #2649)
-   Fix 500 on invalid utf-8 in request (PR #2663)

Changes in synapse v0.24.1 (2017-10-24)
=======================================

Bug fixes:

-   Fix updating group profiles over federation (PR #2567)

Changes in synapse v0.24.0 (2017-10-23)
=======================================

No changes since v0.24.0-rc1

Changes in synapse v0.24.0-rc1 (2017-10-19)
===========================================

Features:

-   Add Group Server (PR #2352, #2363, #2374, #2377, #2378, #2382, #2410, #2426, #2430, #2454, #2471, #2472, #2544)
-   Add support for channel notifications (PR #2501)
-   Add basic implementation of backup media store (PR #2538)
-   Add config option to auto-join new users to rooms (PR #2545)

Changes:

-   Make the spam checker a module (PR #2474)
-   Delete expired url cache data (PR #2478)
-   Ignore incoming events for rooms that we have left (PR #2490)
-   Allow spam checker to reject invites too (PR #2492)
-   Add room creation checks to spam checker (PR #2495)
-   Spam checking: add the invitee to user\_may\_invite (PR #2502)
-   Process events from federation for different rooms in parallel (PR #2520)
-   Allow error strings from spam checker (PR #2531)
-   Improve error handling for missing files in config (PR #2551)

Bug fixes:

-   Fix handling SERVFAILs when doing AAAA lookups for federation (PR #2477)
-   Fix incompatibility with newer versions of ujson (PR #2483) Thanks to @jeremycline!
-   Fix notification keywords that start/end with non-word chars (PR #2500)
-   Fix stack overflow and logcontexts from linearizer (PR #2532)
-   Fix 500 error when fields missing from power\_levels event (PR #2552)
-   Fix 500 error when we get an error handling a PDU (PR #2553)

Changes in synapse v0.23.1 (2017-10-02)
=======================================

Changes:

-   Make \'affinity\' package optional, as it is not supported on some platforms

Changes in synapse v0.23.0 (2017-10-02)
=======================================

No changes since v0.23.0-rc2

Changes in synapse v0.23.0-rc2 (2017-09-26)
===========================================

Bug fixes:

-   Fix regression in performance of syncs (PR #2470)

Changes in synapse v0.23.0-rc1 (2017-09-25)
===========================================

Features:

-   Add a frontend proxy worker (PR #2344)
-   Add support for event\_id\_only push format (PR #2450)
-   Add a PoC for filtering spammy events (PR #2456)
-   Add a config option to block all room invites (PR #2457)

Changes:

-   Use bcrypt module instead of py-bcrypt (PR #2288) Thanks to @kyrias!
-   Improve performance of generating push notifications (PR #2343, #2357, #2365, #2366, #2371)
-   Improve DB performance for device list handling in sync (PR #2362)
-   Include a sample prometheus config (PR #2416)
-   Document known to work postgres version (PR #2433) Thanks to @ptman!

Bug fixes:

-   Fix caching error in the push evaluator (PR #2332)
-   Fix bug where pusherpool didn\'t start and broke some rooms (PR #2342)
-   Fix port script for user directory tables (PR #2375)
-   Fix device lists notifications when user rejoins a room (PR #2443, #2449)
-   Fix sync to always send down current state events in timeline (PR #2451)
-   Fix bug where guest users were incorrectly kicked (PR #2453)
-   Fix bug talking to IPv6 only servers using SRV records (PR #2462)

Changes in synapse v0.22.1 (2017-07-06)
=======================================

Bug fixes:

-   Fix bug where pusher pool didn\'t start and caused issues when interacting with some rooms (PR #2342)

Changes in synapse v0.22.0 (2017-07-06)
=======================================

No changes since v0.22.0-rc2

Changes in synapse v0.22.0-rc2 (2017-07-04)
===========================================

Changes:

-   Improve performance of storing user IPs (PR #2307, #2308)
-   Slightly improve performance of verifying access tokens (PR #2320)
-   Slightly improve performance of event persistence (PR #2321)
-   Increase default cache factor size from 0.1 to 0.5 (PR #2330)

Bug fixes:

-   Fix bug with storing registration sessions that caused frequent CPU churn (PR #2319)

Changes in synapse v0.22.0-rc1 (2017-06-26)
===========================================

Features:

-   Add a user directory API (PR #2252, and many more)
-   Add shutdown room API to remove room from local server (PR #2291)
-   Add API to quarantine media (PR #2292)
-   Add new config option to not send event contents to push servers (PR #2301) Thanks to @cjdelisle!

Changes:

-   Various performance fixes (PR #2177, #2233, #2230, #2238, #2248, #2256, #2274)
-   Deduplicate sync filters (PR #2219) Thanks to @krombel!
-   Correct a typo in UPGRADE.rst (PR #2231) Thanks to @aaronraimist!
-   Add count of one time keys to sync stream (PR #2237)
-   Only store event\_auth for state events (PR #2247)
-   Store URL cache preview downloads separately (PR #2299)

Bug fixes:

-   Fix users not getting notifications when AS listened to that user\_id (PR #2216) Thanks to @slipeer!
-   Fix users without push set up not getting notifications after joining rooms (PR #2236)
-   Fix preview url API to trim long descriptions (PR #2243)
-   Fix bug where we used cached but unpersisted state group as prev group, resulting in broken state of restart (PR #2263)
-   Fix removing of pushers when using workers (PR #2267)
-   Fix CORS headers to allow Authorization header (PR #2285) Thanks to @krombel!

Changes in synapse v0.21.1 (2017-06-15)
=======================================

Bug fixes:

-   Fix bug in anonymous usage statistic reporting (PR #2281)

Changes in synapse v0.21.0 (2017-05-18)
=======================================

No changes since v0.21.0-rc3

Changes in synapse v0.21.0-rc3 (2017-05-17)
===========================================

Features:

-   Add per user rate-limiting overrides (PR #2208)
-   Add config option to limit maximum number of events requested by `/sync` and `/messages` (PR #2221) Thanks to @psaavedra!

Changes:

-   Various small performance fixes (PR #2201, #2202, #2224, #2226, #2227, #2228, #2229)
-   Update username availability checker API (PR #2209, #2213)
-   When purging, don\'t de-delta state groups we\'re about to delete (PR #2214)
-   Documentation to check synapse version (PR #2215) Thanks to @hamber-dick!
-   Add an index to event\_search to speed up purge history API (PR #2218)

Bug fixes:

-   Fix API to allow clients to upload one-time-keys with new sigs (PR #2206)

Changes in synapse v0.21.0-rc2 (2017-05-08)
===========================================

Changes:

-   Always mark remotes as up if we receive a signed request from them (PR #2190)

Bug fixes:

-   Fix bug where users got pushed for rooms they had muted (PR #2200)

Changes in synapse v0.21.0-rc1 (2017-05-08)
===========================================

Features:

-   Add username availability checker API (PR #2183)
-   Add read marker API (PR #2120)

Changes:

-   Enable guest access for the 3pl/3pid APIs (PR #1986)
-   Add setting to support TURN for guests (PR #2011)
-   Various performance improvements (PR #2075, #2076, #2080, #2083, #2108, #2158, #2176, #2185)
-   Make synctl a bit more user friendly (PR #2078, #2127) Thanks @APwhitehat!
-   Replace HTTP replication with TCP replication (PR #2082, #2097, #2098, #2099, #2103, #2014, #2016, #2115, #2116, #2117)
-   Support authenticated SMTP (PR #2102) Thanks @DanielDent!
-   Add a counter metric for successfully-sent transactions (PR #2121)
-   Propagate errors sensibly from proxied IS requests (PR #2147)
-   Add more granular event send metrics (PR #2178)

Bug fixes:

-   Fix nuke-room script to work with current schema (PR #1927) Thanks @zuckschwerdt!
-   Fix db port script to not assume postgres tables are in the public schema (PR #2024) Thanks @jerrykan!
-   Fix getting latest device IP for user with no devices (PR #2118)
-   Fix rejection of invites to unreachable servers (PR #2145)
-   Fix code for reporting old verify keys in synapse (PR #2156)
-   Fix invite state to always include all events (PR #2163)
-   Fix bug where synapse would always fetch state for any missing event (PR #2170)
-   Fix a leak with timed out HTTP connections (PR #2180)
-   Fix bug where we didn\'t time out HTTP requests to ASes (PR #2192)

Docs:

-   Clarify doc for SQLite to PostgreSQL port (PR #1961) Thanks @benhylau!
-   Fix typo in synctl help (PR #2107) Thanks @HarHarLinks!
-   `web_client_location` documentation fix (PR #2131) Thanks @matthewjwolff!
-   Update README.rst with FreeBSD changes (PR #2132) Thanks @feld!
-   Clarify setting up metrics (PR #2149) Thanks @encks!

Changes in synapse v0.20.0 (2017-04-11)
=======================================

Bug fixes:

-   Fix joining rooms over federation where not all servers in the room saw the new server had joined (PR #2094)

Changes in synapse v0.20.0-rc1 (2017-03-30)
===========================================

Features:

-   Add delete\_devices API (PR #1993)
-   Add phone number registration/login support (PR #1994, #2055)

Changes:

-   Use JSONSchema for validation of filters. Thanks @pik! (PR #1783)
-   Reread log config on SIGHUP (PR #1982)
-   Speed up public room list (PR #1989)
-   Add helpful texts to logger config options (PR #1990)
-   Minor `/sync` performance improvements. (PR #2002, #2013, #2022)
-   Add some debug to help diagnose weird federation issue (PR #2035)
-   Correctly limit retries for all federation requests (PR #2050, #2061)
-   Don\'t lock table when persisting new one time keys (PR #2053)
-   Reduce some CPU work on DB threads (PR #2054)
-   Cache hosts in room (PR #2060)
-   Batch sending of device list pokes (PR #2063)
-   Speed up persist event path in certain edge cases (PR #2070)

Bug fixes:

-   Fix bug where current\_state\_events renamed to current\_state\_ids (PR #1849)
-   Fix routing loop when fetching remote media (PR #1992)
-   Fix current\_state\_events table to not lie (PR #1996)
-   Fix CAS login to handle PartialDownloadError (PR #1997)
-   Fix assertion to stop transaction queue getting wedged (PR #2010)
-   Fix presence to fallback to last\_active\_ts if it beats the last sync time. Thanks @Half-Shot! (PR #2014)
-   Fix bug when federation received a PDU while a room join is in progress (PR #2016)
-   Fix resetting state on rejected events (PR #2025)
-   Fix installation issues in readme. Thanks @ricco386 (PR #2037)
-   Fix caching of remote servers\' signature keys (PR #2042)
-   Fix some leaking log context (PR #2048, #2049, #2057, #2058)
-   Fix rejection of invites not reaching sync (PR #2056)

Changes in synapse v0.19.3 (2017-03-20)
=======================================

No changes since v0.19.3-rc2

Changes in synapse v0.19.3-rc2 (2017-03-13)
===========================================

Bug fixes:

-   Fix bug in handling of incoming device list updates over federation.

Changes in synapse v0.19.3-rc1 (2017-03-08)
===========================================

Features:

-   Add some administration functionalities. Thanks to morteza-araby! (PR #1784)

Changes:

-   Reduce database table sizes (PR #1873, #1916, #1923, #1963)
-   Update contrib/ to not use syutil. Thanks to andrewshadura! (PR #1907)
-   Don\'t fetch current state when sending an event in common case (PR #1955)

Bug fixes:

-   Fix synapse\_port\_db failure. Thanks to Pneumaticat! (PR #1904)
-   Fix caching to not cache error responses (PR #1913)
-   Fix APIs to make kick & ban reasons work (PR #1917)
-   Fix bugs in the /keys/changes api (PR #1921)
-   Fix bug where users couldn\'t forget rooms they were banned from (PR #1922)
-   Fix issue with long language values in pushers API (PR #1925)
-   Fix a race in transaction queue (PR #1930)
-   Fix dynamic thumbnailing to preserve aspect ratio. Thanks to jkolo! (PR #1945)
-   Fix device list update to not constantly resync (PR #1964)
-   Fix potential for huge memory usage when getting device that have changed (PR #1969)

Changes in synapse v0.19.2 (2017-02-20)
=======================================

-   Fix bug with event visibility check in /context/ API. Thanks to Tokodomo for pointing it out! (PR #1929)

Changes in synapse v0.19.1 (2017-02-09)
=======================================

-   Fix bug where state was incorrectly reset in a room when synapse received an event over federation that did not pass auth checks (PR #1892)

Changes in synapse v0.19.0 (2017-02-04)
=======================================

No changes since RC 4.

Changes in synapse v0.19.0-rc4 (2017-02-02)
===========================================

-   Bump cache sizes for common membership queries (PR #1879)

Changes in synapse v0.19.0-rc3 (2017-02-02)
===========================================

-   Fix email push in pusher worker (PR #1875)
-   Make presence.get\_new\_events a bit faster (PR #1876)
-   Make /keys/changes a bit more performant (PR #1877)

Changes in synapse v0.19.0-rc2 (2017-02-02)
===========================================

-   Include newly joined users in /keys/changes API (PR #1872)

Changes in synapse v0.19.0-rc1 (2017-02-02)
===========================================

Features:

-   Add support for specifying multiple bind addresses (PR #1709, #1712, #1795, #1835). Thanks to @kyrias!
-   Add /account/3pid/delete endpoint (PR #1714)
-   Add config option to configure the Riot URL used in notification emails (PR #1811). Thanks to @aperezdc!
-   Add username and password config options for turn server (PR #1832). Thanks to @xsteadfastx!
-   Implement device lists updates over federation (PR #1857, #1861, #1864)
-   Implement /keys/changes (PR #1869, #1872)

Changes:

-   Improve IPv6 support (PR #1696). Thanks to @kyrias and @glyph!
-   Log which files we saved attachments to in the media\_repository (PR #1791)
-   Linearize updates to membership via PUT /state/ to better handle multiple joins (PR #1787)
-   Limit number of entries to prefill from cache on startup (PR #1792)
-   Remove full\_twisted\_stacktraces option (PR #1802)
-   Measure size of some caches by sum of the size of cached values (PR #1815)
-   Measure metrics of string\_cache (PR #1821)
-   Reduce logging verbosity (PR #1822, #1823, #1824)
-   Don\'t clobber a displayname or avatar\_url if provided by an m.room.member event (PR #1852)
-   Better handle 401/404 response for federation /send/ (PR #1866, #1871)

Fixes:

-   Fix ability to change password to a non-ascii one (PR #1711)
-   Fix push getting stuck due to looking at the wrong view of state (PR #1820)
-   Fix email address comparison to be case insensitive (PR #1827)
-   Fix occasional inconsistencies of room membership (PR #1836, #1840)

Performance:

-   Don\'t block messages sending on bumping presence (PR #1789)
-   Change device\_inbox stream index to include user (PR #1793)
-   Optimise state resolution (PR #1818)
-   Use DB cache of joined users for presence (PR #1862)
-   Add an index to make membership queries faster (PR #1867)

Changes in synapse v0.18.7 (2017-01-09)
=======================================

No changes from v0.18.7-rc2

Changes in synapse v0.18.7-rc2 (2017-01-07)
===========================================

Bug fixes:

-   Fix error in rc1\'s discarding invalid inbound traffic logic that was incorrectly discarding missing events

Changes in synapse v0.18.7-rc1 (2017-01-06)
===========================================

Bug fixes:

-   Fix error in \#PR 1764 to actually fix the nightmare \#1753 bug.
-   Improve deadlock logging further
-   Discard inbound federation traffic from invalid domains, to immunise against \#1753

Changes in synapse v0.18.6 (2017-01-06)
=======================================

Bug fixes:

-   Fix bug when checking if a guest user is allowed to join a room (PR #1772) Thanks to Patrik Oldsberg for diagnosing and the fix!

Changes in synapse v0.18.6-rc3 (2017-01-05)
===========================================

Bug fixes:

-   Fix bug where we failed to send ban events to the banned server (PR #1758)
-   Fix bug where we sent event that didn\'t originate on this server to other servers (PR #1764)
-   Fix bug where processing an event from a remote server took a long time because we were making long HTTP requests (PR #1765, PR #1744)

Changes:

-   Improve logging for debugging deadlocks (PR #1766, PR #1767)

Changes in synapse v0.18.6-rc2 (2016-12-30)
===========================================

Bug fixes:

-   Fix memory leak in twisted by initialising logging correctly (PR #1731)
-   Fix bug where fetching missing events took an unacceptable amount of time in large rooms (PR #1734)

Changes in synapse v0.18.6-rc1 (2016-12-29)
===========================================

Bug fixes:

-   Make sure that outbound connections are closed (PR #1725)

Changes in synapse v0.18.5 (2016-12-16)
=======================================

Bug fixes:

-   Fix federation /backfill returning events it shouldn\'t (PR #1700)
-   Fix crash in url preview (PR #1701)

Changes in synapse v0.18.5-rc3 (2016-12-13)
===========================================

Features:

-   Add support for E2E for guests (PR #1653)
-   Add new API appservice specific public room list (PR #1676)
-   Add new room membership APIs (PR #1680)

Changes:

-   Enable guest access for private rooms by default (PR #653)
-   Limit the number of events that can be created on a given room concurrently (PR #1620)
-   Log the args that we have on UI auth completion (PR #1649)
-   Stop generating refresh\_tokens (PR #1654)
-   Stop putting a time caveat on access tokens (PR #1656)
-   Remove unspecced GET endpoints for e2e keys (PR #1694)

Bug fixes:

-   Fix handling of 500 and 429\'s over federation (PR #1650)
-   Fix Content-Type header parsing (PR #1660)
-   Fix error when previewing sites that include unicode, thanks to kyrias (PR #1664)
-   Fix some cases where we drop read receipts (PR #1678)
-   Fix bug where calls to `/sync` didn\'t correctly timeout (PR #1683)
-   Fix bug where E2E key query would fail if a single remote host failed (PR #1686)

Changes in synapse v0.18.5-rc2 (2016-11-24)
===========================================

Bug fixes:

-   Don\'t send old events over federation, fixes bug in -rc1.

Changes in synapse v0.18.5-rc1 (2016-11-24)
===========================================

Features:

-   Implement \"event\_fields\" in filters (PR #1638)

Changes:

-   Use external ldap auth pacakge (PR #1628)
-   Split out federation transaction sending to a worker (PR #1635)
-   Fail with a coherent error message if /sync?filter= is invalid (PR #1636)
-   More efficient notif count queries (PR #1644)

Changes in synapse v0.18.4 (2016-11-22)
=======================================

Bug fixes:

-   Add workaround for buggy clients that the fail to register (PR #1632)

Changes in synapse v0.18.4-rc1 (2016-11-14)
===========================================

Changes:

-   Various database efficiency improvements (PR #1188, #1192)
-   Update default config to blacklist more internal IPs, thanks to Euan Kemp (PR #1198)
-   Allow specifying duration in minutes in config, thanks to Daniel Dent (PR #1625)

Bug fixes:

-   Fix media repo to set CORs headers on responses (PR #1190)
-   Fix registration to not error on non-ascii passwords (PR #1191)
-   Fix create event code to limit the number of prev\_events (PR #1615)
-   Fix bug in transaction ID deduplication (PR #1624)

Changes in synapse v0.18.3 (2016-11-08)
=======================================

SECURITY UPDATE

Explicitly require authentication when using LDAP3. This is the default on versions of `ldap3` above 1.0, but some distributions will package an older version.

If you are using LDAP3 login and have a version of `ldap3` older than 1.0 it is **CRITICAL to updgrade**.

Changes in synapse v0.18.2 (2016-11-01)
=======================================

No changes since v0.18.2-rc5

Changes in synapse v0.18.2-rc5 (2016-10-28)
===========================================

Bug fixes:

-   Fix prometheus process metrics in worker processes (PR #1184)

Changes in synapse v0.18.2-rc4 (2016-10-27)
===========================================

Bug fixes:

-   Fix `user_threepids` schema delta, which in some instances prevented startup after upgrade (PR #1183)

Changes in synapse v0.18.2-rc3 (2016-10-27)
===========================================

Changes:

-   Allow clients to supply access tokens as headers (PR #1098)
-   Clarify error codes for GET /filter/, thanks to Alexander Maznev (PR #1164)
-   Make password reset email field case insensitive (PR #1170)
-   Reduce redundant database work in email pusher (PR #1174)
-   Allow configurable rate limiting per AS (PR #1175)
-   Check whether to ratelimit sooner to avoid work (PR #1176)
-   Standardise prometheus metrics (PR #1177)

Bug fixes:

-   Fix incredibly slow back pagination query (PR #1178)
-   Fix infinite typing bug (PR #1179)

Changes in synapse v0.18.2-rc2 (2016-10-25)
===========================================

(This release did not include the changes advertised and was identical to RC1)

Changes in synapse v0.18.2-rc1 (2016-10-17)
===========================================

Changes:

-   Remove redundant event\_auth index (PR #1113)
-   Reduce DB hits for replication (PR #1141)
-   Implement pluggable password auth (PR #1155)
-   Remove rate limiting from app service senders and fix get\_or\_create\_user requester, thanks to Patrik Oldsberg (PR #1157)
-   window.postmessage for Interactive Auth fallback (PR #1159)
-   Use sys.executable instead of hardcoded python, thanks to Pedro Larroy (PR #1162)
-   Add config option for adding additional TLS fingerprints (PR #1167)
-   User-interactive auth on delete device (PR #1168)

Bug fixes:

-   Fix not being allowed to set your own state\_key, thanks to Patrik Oldsberg (PR #1150)
-   Fix interactive auth to return 401 from for incorrect password (PR #1160, #1166)
-   Fix email push notifs being dropped (PR #1169)

Changes in synapse v0.18.1 (2016-10-05)
=======================================

No changes since v0.18.1-rc1

Changes in synapse v0.18.1-rc1 (2016-09-30)
===========================================

Features:

-   Add total\_room\_count\_estimate to `/publicRooms` (PR #1133)

Changes:

-   Time out typing over federation (PR #1140)
-   Restructure LDAP authentication (PR #1153)

Bug fixes:

-   Fix 3pid invites when server is already in the room (PR #1136)
-   Fix upgrading with SQLite taking lots of CPU for a few days after upgrade (PR #1144)
-   Fix upgrading from very old database versions (PR #1145)
-   Fix port script to work with recently added tables (PR #1146)

Changes in synapse v0.18.0 (2016-09-19)
=======================================

The release includes major changes to the state storage database schemas, which significantly reduce database size. Synapse will attempt to upgrade the current data in the background. Servers with large SQLite database may experience degradation of performance while this upgrade is in progress, therefore you may want to consider migrating to using Postgres before upgrading very large SQLite databases

Changes:

-   Make public room search case insensitive (PR #1127)

Bug fixes:

-   Fix and clean up publicRooms pagination (PR #1129)

Changes in synapse v0.18.0-rc1 (2016-09-16)
===========================================

Features:

-   Add `only=highlight` on `/notifications` (PR #1081)
-   Add server param to /publicRooms (PR #1082)
-   Allow clients to ask for the whole of a single state event (PR #1094)
-   Add is\_direct param to /createRoom (PR #1108)
-   Add pagination support to publicRooms (PR #1121)
-   Add very basic filter API to /publicRooms (PR #1126)
-   Add basic direct to device messaging support for E2E (PR #1074, #1084, #1104, #1111)

Changes:

-   Move to storing state\_groups\_state as deltas, greatly reducing DB size (PR #1065)
-   Reduce amount of state pulled out of the DB during common requests (PR #1069)
-   Allow PDF to be rendered from media repo (PR #1071)
-   Reindex state\_groups\_state after pruning (PR #1085)
-   Clobber EDUs in send queue (PR #1095)
-   Conform better to the CAS protocol specification (PR #1100)
-   Limit how often we ask for keys from dead servers (PR #1114)

Bug fixes:

-   Fix /notifications API when used with `from` param (PR #1080)
-   Fix backfill when cannot find an event. (PR #1107)

Changes in synapse v0.17.3 (2016-09-09)
=======================================

This release fixes a major bug that stopped servers from handling rooms with over 1000 members.

Changes in synapse v0.17.2 (2016-09-08)
=======================================

This release contains security bug fixes. Please upgrade.

No changes since v0.17.2-rc1

Changes in synapse v0.17.2-rc1 (2016-09-05)
===========================================

Features:

-   Start adding store-and-forward direct-to-device messaging (PR #1046, #1050, #1062, #1066)

Changes:

-   Avoid pulling the full state of a room out so often (PR #1047, #1049, #1063, #1068)
-   Don\'t notify for online to online presence transitions. (PR #1054)
-   Occasionally persist unpersisted presence updates (PR #1055)
-   Allow application services to have an optional \'url\' (PR #1056)
-   Clean up old sent transactions from DB (PR #1059)

Bug fixes:

-   Fix None check in backfill (PR #1043)
-   Fix membership changes to be idempotent (PR #1067)
-   Fix bug in get\_pdu where it would sometimes return events with incorrect signature

Changes in synapse v0.17.1 (2016-08-24)
=======================================

Changes:

-   Delete old received\_transactions rows (PR #1038)
-   Pass through user-supplied content in /join/\$room\_id (PR #1039)

Bug fixes:

-   Fix bug with backfill (PR #1040)

Changes in synapse v0.17.1-rc1 (2016-08-22)
===========================================

Features:

-   Add notification API (PR #1028)

Changes:

-   Don\'t print stack traces when failing to get remote keys (PR #996)
-   Various federation /event/ perf improvements (PR #998)
-   Only process one local membership event per room at a time (PR #1005)
-   Move default display name push rule (PR #1011, #1023)
-   Fix up preview URL API. Add tests. (PR #1015)
-   Set `Content-Security-Policy` on media repo (PR #1021)
-   Make notify\_interested\_services faster (PR #1022)
-   Add usage stats to prometheus monitoring (PR #1037)

Bug fixes:

-   Fix token login (PR #993)
-   Fix CAS login (PR #994, #995)
-   Fix /sync to not clobber status\_msg (PR #997)
-   Fix redacted state events to include prev\_content (PR #1003)
-   Fix some bugs in the auth/ldap handler (PR #1007)
-   Fix backfill request to limit URI length, so that remotes don\'t reject the requests due to path length limits (PR #1012)
-   Fix AS push code to not send duplicate events (PR #1025)

Changes in synapse v0.17.0 (2016-08-08)
=======================================

This release contains significant security bug fixes regarding authenticating events received over federation. PLEASE UPGRADE.

This release changes the LDAP configuration format in a backwards incompatible way, see PR #843 for details.

Changes:

-   Add federation /version API (PR #990)
-   Make psutil dependency optional (PR #992)

Bug fixes:

-   Fix URL preview API to exclude HTML comments in description (PR #988)
-   Fix error handling of remote joins (PR #991)

Changes in synapse v0.17.0-rc4 (2016-08-05)
===========================================

Changes:

-   Change the way we summarize URLs when previewing (PR #973)
-   Add new `/state_ids/` federation API (PR #979)
-   Speed up processing of `/state/` response (PR #986)

Bug fixes:

-   Fix event persistence when event has already been partially persisted (PR #975, #983, #985)
-   Fix port script to also copy across backfilled events (PR #982)

Changes in synapse v0.17.0-rc3 (2016-08-02)
===========================================

Changes:

-   Forbid non-ASes from registering users whose names begin with \'\_\' (PR #958)
-   Add some basic admin API docs (PR #963)

Bug fixes:

-   Send the correct host header when fetching keys (PR #941)
-   Fix joining a room that has missing auth events (PR #964)
-   Fix various push bugs (PR #966, #970)
-   Fix adding emails on registration (PR #968)

Changes in synapse v0.17.0-rc2 (2016-08-02)
===========================================

(This release did not include the changes advertised and was identical to RC1)

Changes in synapse v0.17.0-rc1 (2016-07-28)
===========================================

This release changes the LDAP configuration format in a backwards incompatible way, see PR #843 for details.

Features:

-   Add purge\_media\_cache admin API (PR #902)
-   Add deactivate account admin API (PR #903)
-   Add optional pepper to password hashing (PR #907, #910 by KentShikama)
-   Add an admin option to shared secret registration (breaks backwards compat) (PR #909)
-   Add purge local room history API (PR #911, #923, #924)
-   Add requestToken endpoints (PR #915)
-   Add an /account/deactivate endpoint (PR #921)
-   Add filter param to /messages. Add \'contains\_url\' to filter. (PR #922)
-   Add device\_id support to /login (PR #929)
-   Add device\_id support to /v2/register flow. (PR #937, #942)
-   Add GET /devices endpoint (PR #939, #944)
-   Add GET /device/{deviceId} (PR #943)
-   Add update and delete APIs for devices (PR #949)

Changes:

-   Rewrite LDAP Authentication against ldap3 (PR #843 by mweinelt)
-   Linearize some federation endpoints based on (origin, room\_id) (PR #879)
-   Remove the legacy v0 content upload API. (PR #888)
-   Use similar naming we use in email notifs for push (PR #894)
-   Optionally include password hash in createUser endpoint (PR #905 by KentShikama)
-   Use a query that postgresql optimises better for get\_events\_around (PR #906)
-   Fall back to \'username\' if \'user\' is not given for appservice registration. (PR #927 by Half-Shot)
-   Add metrics for psutil derived memory usage (PR #936)
-   Record device\_id in client\_ips (PR #938)
-   Send the correct host header when fetching keys (PR #941)
-   Log the hostname the reCAPTCHA was completed on (PR #946)
-   Make the device id on e2e key upload optional (PR #956)
-   Add r0.2.0 to the \"supported versions\" list (PR #960)
-   Don\'t include name of room for invites in push (PR #961)

Bug fixes:

-   Fix substitution failure in mail template (PR #887)
-   Put most recent 20 messages in email notif (PR #892)
-   Ensure that the guest user is in the database when upgrading accounts (PR #914)
-   Fix various edge cases in auth handling (PR #919)
-   Fix 500 ISE when sending alias event without a state\_key (PR #925)
-   Fix bug where we stored rejections in the state\_group, persist all rejections (PR #948)
-   Fix lack of check of if the user is banned when handling 3pid invites (PR #952)
-   Fix a couple of bugs in the transaction and keyring code (PR #954, #955)

Changes in synapse v0.16.1-r1 (2016-07-08)
==========================================

THIS IS A CRITICAL SECURITY UPDATE.

This fixes a bug which allowed users\' accounts to be accessed by unauthorised users.

Changes in synapse v0.16.1 (2016-06-20)
=======================================

Bug fixes:

-   Fix assorted bugs in `/preview_url` (PR #872)
-   Fix TypeError when setting unicode passwords (PR #873)

Performance improvements:

-   Turn `use_frozen_events` off by default (PR #877)
-   Disable responding with canonical json for federation (PR #878)

Changes in synapse v0.16.1-rc1 (2016-06-15)
===========================================

Features: None

Changes:

-   Log requester for `/publicRoom` endpoints when possible (PR #856)
-   502 on `/thumbnail` when can\'t connect to remote server (PR #862)
-   Linearize fetching of gaps on incoming events (PR #871)

Bugs fixes:

-   Fix bug where rooms where marked as published by default (PR #857)
-   Fix bug where joining room with an event with invalid sender (PR #868)
-   Fix bug where backfilled events were sent down sync streams (PR #869)
-   Fix bug where outgoing connections could wedge indefinitely, causing push notifications to be unreliable (PR #870)

Performance improvements:

-   Improve `/publicRooms` performance(PR #859)

Changes in synapse v0.16.0 (2016-06-09)
=======================================

NB: As of v0.14 all AS config files must have an ID field.

Bug fixes:

-   Don\'t make rooms published by default (PR #857)

Changes in synapse v0.16.0-rc2 (2016-06-08)
===========================================

Features:

-   Add configuration option for tuning GC via `gc.set_threshold` (PR #849)

Changes:

-   Record metrics about GC (PR #771, #847, #852)
-   Add metric counter for number of persisted events (PR #841)

Bug fixes:

-   Fix \'From\' header in email notifications (PR #843)
-   Fix presence where timeouts were not being fired for the first 8h after restarts (PR #842)
-   Fix bug where synapse sent malformed transactions to AS\'s when retrying transactions (Commits 310197b, 8437906)

Performance improvements:

-   Remove event fetching from DB threads (PR #835)
-   Change the way we cache events (PR #836)
-   Add events to cache when we persist them (PR #840)

Changes in synapse v0.16.0-rc1 (2016-06-03)
===========================================

Version 0.15 was not released. See v0.15.0-rc1 below for additional changes.

Features:

-   Add email notifications for missed messages (PR #759, #786, #799, #810, #815, #821)
-   Add a `url_preview_ip_range_whitelist` config param (PR #760)
-   Add /report endpoint (PR #762)
-   Add basic ignore user API (PR #763)
-   Add an openidish mechanism for proving that you own a given user\_id (PR #765)
-   Allow clients to specify a server\_name to avoid \'No known servers\' (PR #794)
-   Add secondary\_directory\_servers option to fetch room list from other servers (PR #808, #813)

Changes:

-   Report per request metrics for all of the things using request\_handler (PR #756)
-   Correctly handle `NULL` password hashes from the database (PR #775)
-   Allow receipts for events we haven\'t seen in the db (PR #784)
-   Make synctl read a cache factor from config file (PR #785)
-   Increment badge count per missed convo, not per msg (PR #793)
-   Special case m.room.third\_party\_invite event auth to match invites (PR #814)

Bug fixes:

-   Fix typo in event\_auth servlet path (PR #757)
-   Fix password reset (PR #758)

Performance improvements:

-   Reduce database inserts when sending transactions (PR #767)
-   Queue events by room for persistence (PR #768)
-   Add cache to `get_user_by_id` (PR #772)
-   Add and use `get_domain_from_id` (PR #773)
-   Use tree cache for `get_linearized_receipts_for_room` (PR #779)
-   Remove unused indices (PR #782)
-   Add caches to `bulk_get_push_rules*` (PR #804)
-   Cache `get_event_reference_hashes` (PR #806)
-   Add `get_users_with_read_receipts_in_room` cache (PR #809)
-   Use state to calculate `get_users_in_room` (PR #811)
-   Load push rules in storage layer so that they get cached (PR #825)
-   Make `get_joined_hosts_for_room` use get\_users\_in\_room (PR #828)
-   Poke notifier on next reactor tick (PR #829)
-   Change CacheMetrics to be quicker (PR #830)

Changes in synapse v0.15.0-rc1 (2016-04-26)
===========================================

Features:

-   Add login support for Javascript Web Tokens, thanks to Niklas Riekenbrauck (PR #671,\#687)
-   Add URL previewing support (PR #688)
-   Add login support for LDAP, thanks to Christoph Witzany (PR #701)
-   Add GET endpoint for pushers (PR #716)

Changes:

-   Never notify for member events (PR #667)
-   Deduplicate identical `/sync` requests (PR #668)
-   Require user to have left room to forget room (PR #673)
-   Use DNS cache if within TTL (PR #677)
-   Let users see their own leave events (PR #699)
-   Deduplicate membership changes (PR #700)
-   Increase performance of pusher code (PR #705)
-   Respond with error status 504 if failed to talk to remote server (PR #731)
-   Increase search performance on postgres (PR #745)

Bug fixes:

-   Fix bug where disabling all notifications still resulted in push (PR #678)
-   Fix bug where users couldn\'t reject remote invites if remote refused (PR #691)
-   Fix bug where synapse attempted to backfill from itself (PR #693)
-   Fix bug where profile information was not correctly added when joining remote rooms (PR #703)
-   Fix bug where register API required incorrect key name for AS registration (PR #727)

Changes in synapse v0.14.0 (2016-03-30)
=======================================

No changes from v0.14.0-rc2

Changes in synapse v0.14.0-rc2 (2016-03-23)
===========================================

Features:

-   Add published room list API (PR #657)

Changes:

-   Change various caches to consume less memory (PR #656, #658, #660, #662, #663, #665)
-   Allow rooms to be published without requiring an alias (PR #664)
-   Intern common strings in caches to reduce memory footprint (\#666)

Bug fixes:

-   Fix reject invites over federation (PR #646)
-   Fix bug where registration was not idempotent (PR #649)
-   Update aliases event after deleting aliases (PR #652)
-   Fix unread notification count, which was sometimes wrong (PR #661)

Changes in synapse v0.14.0-rc1 (2016-03-14)
===========================================

Features:

-   Add event\_id to response to state event PUT (PR #581)
-   Allow guest users access to messages in rooms they have joined (PR #587)
-   Add config for what state is included in a room invite (PR #598)
-   Send the inviter\'s member event in room invite state (PR #607)
-   Add error codes for malformed/bad JSON in /login (PR #608)
-   Add support for changing the actions for default rules (PR #609)
-   Add environment variable SYNAPSE\_CACHE\_FACTOR, default it to 0.1 (PR #612)
-   Add ability for alias creators to delete aliases (PR #614)
-   Add profile information to invites (PR #624)

Changes:

-   Enforce user\_id exclusivity for AS registrations (PR #572)
-   Make adding push rules idempotent (PR #587)
-   Improve presence performance (PR #582, #586)
-   Change presence semantics for `last_active_ago` (PR #582, #586)
-   Don\'t allow `m.room.create` to be changed (PR #596)
-   Add 800x600 to default list of valid thumbnail sizes (PR #616)
-   Always include kicks and bans in full /sync (PR #625)
-   Send history visibility on boundary changes (PR #626)
-   Register endpoint now returns a refresh\_token (PR #637)

Bug fixes:

-   Fix bug where we returned incorrect state in /sync (PR #573)
-   Always return a JSON object from push rule API (PR #606)
-   Fix bug where registering without a user id sometimes failed (PR #610)
-   Report size of ExpiringCache in cache size metrics (PR #611)
-   Fix rejection of invites to empty rooms (PR #615)
-   Fix usage of `bcrypt` to not use `checkpw` (PR #619)
-   Pin `pysaml2` dependency (PR #634)
-   Fix bug in `/sync` where timeline order was incorrect for backfilled events (PR #635)

Changes in synapse v0.13.3 (2016-02-11)
=======================================

-   Fix bug where `/sync` would occasionally return events in the wrong room.

Changes in synapse v0.13.2 (2016-02-11)
=======================================

-   Fix bug where `/events` would fail to skip some events if there had been more events than the limit specified since the last request (PR #570)

Changes in synapse v0.13.1 (2016-02-10)
=======================================

-   Bump matrix-angular-sdk (matrix web console) dependency to 0.6.8 to pull in the fix for SYWEB-361 so that the default client can display HTML messages again(!)

Changes in synapse v0.13.0 (2016-02-10)
=======================================

This version includes an upgrade of the schema, specifically adding an index to the `events` table. This may cause synapse to pause for several minutes the first time it is started after the upgrade.

Changes:

-   Improve general performance (PR #540, #543. \#544, #54, #549, #567)
-   Change guest user ids to be incrementing integers (PR #550)
-   Improve performance of public room list API (PR #552)
-   Change profile API to omit keys rather than return null (PR #557)
-   Add `/media/r0` endpoint prefix, which is equivalent to `/media/v1/` (PR #595)

Bug fixes:

-   Fix bug with upgrading guest accounts where it would fail if you opened the registration email on a different device (PR #547)
-   Fix bug where unread count could be wrong (PR #568)

Changes in synapse v0.12.1-rc1 (2016-01-29)
===========================================

Features:

-   Add unread notification counts in `/sync` (PR #456)
-   Add support for inviting 3pids in `/createRoom` (PR #460)
-   Add ability for guest accounts to upgrade (PR #462)
-   Add `/versions` API (PR #468)
-   Add `event` to `/context` API (PR #492)
-   Add specific error code for invalid user names in `/register` (PR #499)
-   Add support for push badge counts (PR #507)
-   Add support for non-guest users to peek in rooms using `/events` (PR #510)

Changes:

-   Change `/sync` so that guest users only get rooms they\'ve joined (PR #469)
-   Change to require unbanning before other membership changes (PR #501)
-   Change default push rules to notify for all messages (PR #486)
-   Change default push rules to not notify on membership changes (PR #514)
-   Change default push rules in one to one rooms to only notify for events that are messages (PR #529)
-   Change `/sync` to reject requests with a `from` query param (PR #512)
-   Change server manhole to use SSH rather than telnet (PR #473)
-   Change server to require AS users to be registered before use (PR #487)
-   Change server not to start when ASes are invalidly configured (PR #494)
-   Change server to require ID and `as_token` to be unique for AS\'s (PR #496)
-   Change maximum pagination limit to 1000 (PR #497)

Bug fixes:

-   Fix bug where `/sync` didn\'t return when something under the leave key changed (PR #461)
-   Fix bug where we returned smaller rather than larger than requested thumbnails when `method=crop` (PR #464)
-   Fix thumbnails API to only return cropped thumbnails when asking for a cropped thumbnail (PR #475)
-   Fix bug where we occasionally still logged access tokens (PR #477)
-   Fix bug where `/events` would always return immediately for guest users (PR #480)
-   Fix bug where `/sync` unexpectedly returned old left rooms (PR #481)
-   Fix enabling and disabling push rules (PR #498)
-   Fix bug where `/register` returned 500 when given unicode username (PR #513)

Changes in synapse v0.12.0 (2016-01-04)
=======================================

-   Expose `/login` under `r0` (PR #459)

Changes in synapse v0.12.0-rc3 (2015-12-23)
===========================================

-   Allow guest accounts access to `/sync` (PR #455)
-   Allow filters to include/exclude rooms at the room level rather than just from the components of the sync for each room. (PR #454)
-   Include urls for room avatars in the response to `/publicRooms` (PR #453)
-   Don\'t set a identicon as the avatar for a user when they register (PR #450)
-   Add a `display_name` to third-party invites (PR #449)
-   Send more information to the identity server for third-party invites so that it can send richer messages to the invitee (PR #446)
-   Cache the responses to `/initialSync` for 5 minutes. If a client retries a request to `/initialSync` before the a response was computed to the first request then the same response is used for both requests (PR #457)
-   Fix a bug where synapse would always request the signing keys of remote servers even when the key was cached locally (PR #452)
-   Fix 500 when pagination search results (PR #447)
-   Fix a bug where synapse was leaking raw email address in third-party invites (PR #448)

Changes in synapse v0.12.0-rc2 (2015-12-14)
===========================================

-   Add caches for whether rooms have been forgotten by a user (PR #434)
-   Remove instructions to use `--process-dependency-link` since all of the dependencies of synapse are on PyPI (PR #436)
-   Parallelise the processing of `/sync` requests (PR #437)
-   Fix race updating presence in `/events` (PR #444)
-   Fix bug back-populating search results (PR #441)
-   Fix bug calculating state in `/sync` requests (PR #442)

Changes in synapse v0.12.0-rc1 (2015-12-10)
===========================================

-   Host the client APIs released as r0 by <https://matrix.org/docs/spec/r0.0.0/client_server.html> on paths prefixed by `/_matrix/client/r0`. (PR #430, PR #415, PR #400)
-   Updates the client APIs to match r0 of the matrix specification.
    -   All APIs return events in the new event format, old APIs also include the fields needed to parse the event using the old format for compatibility. (PR #402)
    -   Search results are now given as a JSON array rather than a JSON object (PR #405)
    -   Miscellaneous changes to search (PR #403, PR #406, PR #412)
    -   Filter JSON objects may now be passed as query parameters to `/sync` (PR #431)
    -   Fix implementation of `/admin/whois` (PR #418)
    -   Only include the rooms that user has left in `/sync` if the client requests them in the filter (PR #423)
    -   Don\'t push for `m.room.message` by default (PR #411)
    -   Add API for setting per account user data (PR #392)
    -   Allow users to forget rooms (PR #385)
-   Performance improvements and monitoring:
    -   Add per-request counters for CPU time spent on the main python thread. (PR #421, PR #420)
    -   Add per-request counters for time spent in the database (PR #429)
    -   Make state updates in the C+S API idempotent (PR #416)
    -   Only fire `user_joined_room` if the user has actually joined. (PR #410)
    -   Reuse a single http client, rather than creating new ones (PR #413)
-   Fixed a bug upgrading from older versions of synapse on postgresql (PR #417)

Changes in synapse v0.11.1 (2015-11-20)
=======================================

-   Add extra options to search API (PR #394)
-   Fix bug where we did not correctly cap federation retry timers. This meant it could take several hours for servers to start talking to ressurected servers, even when they were receiving traffic from them (PR #393)
-   Don\'t advertise login token flow unless CAS is enabled. This caused issues where some clients would always use the fallback API if they did not recognize all login flows (PR #391)
-   Change /v2 sync API to rename `private_user_data` to `account_data` (PR #386)
-   Change /v2 sync API to remove the `event_map` and rename keys in `rooms` object (PR #389)

Changes in synapse v0.11.0-r2 (2015-11-19)
==========================================

-   Fix bug in database port script (PR #387)

Changes in synapse v0.11.0-r1 (2015-11-18)
==========================================

-   Retry and fail federation requests more aggressively for requests that block client side requests (PR #384)

Changes in synapse v0.11.0 (2015-11-17)
=======================================

-   Change CAS login API (PR #349)

Changes in synapse v0.11.0-rc2 (2015-11-13)
===========================================

-   Various changes to /sync API response format (PR #373)
-   Fix regression when setting display name in newly joined room over federation (PR #368)
-   Fix problem where /search was slow when using SQLite (PR #366)

Changes in synapse v0.11.0-rc1 (2015-11-11)
===========================================

-   Add Search API (PR #307, #324, #327, #336, #350, #359)
-   Add \'archived\' state to v2 /sync API (PR #316)
-   Add ability to reject invites (PR #317)
-   Add config option to disable password login (PR #322)
-   Add the login fallback API (PR #330)
-   Add room context API (PR #334)
-   Add room tagging support (PR #335)
-   Update v2 /sync API to match spec (PR #305, #316, #321, #332, #337, #341)
-   Change retry schedule for application services (PR #320)
-   Change retry schedule for remote servers (PR #340)
-   Fix bug where we hosted static content in the incorrect place (PR #329)
-   Fix bug where we didn\'t increment retry interval for remote servers (PR #343)

Changes in synapse v0.10.1-rc1 (2015-10-15)
===========================================

-   Add support for CAS, thanks to Steven Hammerton (PR #295, #296)
-   Add support for using macaroons for `access_token` (PR #256, #229)
-   Add support for `m.room.canonical_alias` (PR #287)
-   Add support for viewing the history of rooms that they have left. (PR #276, #294)
-   Add support for refresh tokens (PR #240)
-   Add flag on creation which disables federation of the room (PR #279)
-   Add some room state to invites. (PR #275)
-   Atomically persist events when joining a room over federation (PR #283)
-   Change default history visibility for private rooms (PR #271)
-   Allow users to redact their own sent events (PR #262)
-   Use tox for tests (PR #247)
-   Split up syutil into separate libraries (PR #243)

Changes in synapse v0.10.0-r2 (2015-09-16)
==========================================

-   Fix bug where we always fetched remote server signing keys instead of using ones in our cache.
-   Fix adding threepids to an existing account.
-   Fix bug with invinting over federation where remote server was already in the room. (PR #281, SYN-392)

Changes in synapse v0.10.0-r1 (2015-09-08)
==========================================

-   Fix bug with python packaging

Changes in synapse v0.10.0 (2015-09-03)
=======================================

No change from release candidate.

Changes in synapse v0.10.0-rc6 (2015-09-02)
===========================================

-   Remove some of the old database upgrade scripts.
-   Fix database port script to work with newly created sqlite databases.

Changes in synapse v0.10.0-rc5 (2015-08-27)
===========================================

-   Fix bug that broke downloading files with ascii filenames across federation.

Changes in synapse v0.10.0-rc4 (2015-08-27)
===========================================

-   Allow UTF-8 filenames for upload. (PR #259)

Changes in synapse v0.10.0-rc3 (2015-08-25)
===========================================

-   Add `--keys-directory` config option to specify where files such as certs and signing keys should be stored in, when using `--generate-config` or `--generate-keys`. (PR #250)
-   Allow `--config-path` to specify a directory, causing synapse to use all \*.yaml files in the directory as config files. (PR #249)
-   Add `web_client_location` config option to specify static files to be hosted by synapse under `/_matrix/client`. (PR #245)
-   Add helper utility to synapse to read and parse the config files and extract the value of a given key. For example:

        $ python -m synapse.config read server_name -c homeserver.yaml
        localhost

    (PR #246)

Changes in synapse v0.10.0-rc2 (2015-08-24)
===========================================

-   Fix bug where we incorrectly populated the `event_forward_extremities` table, resulting in problems joining large remote rooms (e.g. `#matrix:matrix.org`)
-   Reduce the number of times we wake up pushers by not listening for presence or typing events, reducing the CPU cost of each pusher.

Changes in synapse v0.10.0-rc1 (2015-08-21)
===========================================

Also see v0.9.4-rc1 changelog, which has been amalgamated into this release.

General:

-   Upgrade to Twisted 15 (PR #173)
-   Add support for serving and fetching encryption keys over federation. (PR #208)
-   Add support for logging in with email address (PR #234)
-   Add support for new `m.room.canonical_alias` event. (PR #233)
-   Change synapse to treat user IDs case insensitively during registration and login. (If two users already exist with case insensitive matching user ids, synapse will continue to require them to specify their user ids exactly.)
-   Error if a user tries to register with an email already in use. (PR #211)
-   Add extra and improve existing caches (PR #212, #219, #226, #228)
-   Batch various storage request (PR #226, #228)
-   Fix bug where we didn\'t correctly log the entity that triggered the request if the request came in via an application service (PR #230)
-   Fix bug where we needlessly regenerated the full list of rooms an AS is interested in. (PR #232)
-   Add support for AS\'s to use v2\_alpha registration API (PR #210)

Configuration:

-   Add `--generate-keys` that will generate any missing cert and key files in the configuration files. This is equivalent to running `--generate-config` on an existing configuration file. (PR #220)
-   `--generate-config` now no longer requires a `--server-name` parameter when used on existing configuration files. (PR #220)
-   Add `--print-pidfile` flag that controls the printing of the pid to stdout of the demonised process. (PR #213)

Media Repository:

-   Fix bug where we picked a lower resolution image than requested. (PR #205)
-   Add support for specifying if a the media repository should dynamically thumbnail images or not. (PR #206)

Metrics:

-   Add statistics from the reactor to the metrics API. (PR #224, #225)

Demo Homeservers:

-   Fix starting the demo homeservers without rate-limiting enabled. (PR #182)
-   Fix enabling registration on demo homeservers (PR #223)

Changes in synapse v0.9.4-rc1 (2015-07-21)
==========================================

General:

-   Add basic implementation of receipts. (SPEC-99)
-   Add support for configuration presets in room creation API. (PR #203)
-   Add auth event that limits the visibility of history for new users. (SPEC-134)
-   Add SAML2 login/registration support. (PR #201. Thanks Muthu Subramanian!)
-   Add client side key management APIs for end to end encryption. (PR #198)
-   Change power level semantics so that you cannot kick, ban or change power levels of users that have equal or greater power level than you. (SYN-192)
-   Improve performance by bulk inserting events where possible. (PR #193)
-   Improve performance by bulk verifying signatures where possible. (PR #194)

Configuration:

-   Add support for including TLS certificate chains.

Media Repository:

-   Add Content-Disposition headers to content repository responses. (SYN-150)

Changes in synapse v0.9.3 (2015-07-01)
======================================

No changes from v0.9.3 Release Candidate 1.

Changes in synapse v0.9.3-rc1 (2015-06-23)
==========================================

General:

-   Fix a memory leak in the notifier. (SYN-412)
-   Improve performance of room initial sync. (SYN-418)
-   General improvements to logging.
-   Remove `access_token` query params from `INFO` level logging.

Configuration:

-   Add support for specifying and configuring multiple listeners. (SYN-389)

Application services:

-   Fix bug where synapse failed to send user queries to application services.

Changes in synapse v0.9.2-r2 (2015-06-15)
=========================================

Fix packaging so that schema delta python files get included in the package.

Changes in synapse v0.9.2 (2015-06-12)
======================================

General:

-   Use ultrajson for json (de)serialisation when a canonical encoding is not required. Ultrajson is significantly faster than simplejson in certain circumstances.
-   Use connection pools for outgoing HTTP connections.
-   Process thumbnails on separate threads.

Configuration:

-   Add option, `gzip_responses`, to disable HTTP response compression.

Federation:

-   Improve resilience of backfill by ensuring we fetch any missing auth events.
-   Improve performance of backfill and joining remote rooms by removing unnecessary computations. This included handling events we\'d previously handled as well as attempting to compute the current state for outliers.

Changes in synapse v0.9.1 (2015-05-26)
======================================

General:

-   Add support for backfilling when a client paginates. This allows servers to request history for a room from remote servers when a client tries to paginate history the server does not have - SYN-36
-   Fix bug where you couldn\'t disable non-default pushrules - SYN-378
-   Fix `register_new_user` script - SYN-359
-   Improve performance of fetching events from the database, this improves both initialSync and sending of events.
-   Improve performance of event streams, allowing synapse to handle more simultaneous connected clients.

Federation:

-   Fix bug with existing backfill implementation where it returned the wrong selection of events in some circumstances.
-   Improve performance of joining remote rooms.

Configuration:

-   Add support for changing the bind host of the metrics listener via the `metrics_bind_host` option.

Changes in synapse v0.9.0-r5 (2015-05-21)
=========================================

-   Add more database caches to reduce amount of work done for each pusher. This radically reduces CPU usage when multiple pushers are set up in the same room.

Changes in synapse v0.9.0 (2015-05-07)
======================================

General:

-   Add support for using a PostgreSQL database instead of SQLite. See [docs/postgres.rst](docs/postgres.rst) for details.
-   Add password change and reset APIs. See [Registration](https://github.com/matrix-org/matrix-doc/blob/master/specification/10_client_server_api.rst#registration) in the spec.
-   Fix memory leak due to not releasing stale notifiers - SYN-339.
-   Fix race in caches that occasionally caused some presence updates to be dropped - SYN-369.
-   Check server name has not changed on restart.
-   Add a sample systemd unit file and a logger configuration in contrib/systemd. Contributed Ivan Shapovalov.

Federation:

-   Add key distribution mechanisms for fetching public keys of unavailable remote homeservers. See [Retrieving Server Keys](https://github.com/matrix-org/matrix-doc/blob/6f2698/specification/30_server_server_api.rst#retrieving-server-keys) in the spec.

Configuration:

-   Add support for multiple config files.
-   Add support for dictionaries in config files.
-   Remove support for specifying config options on the command line, except for:
    -   `--daemonize` - Daemonize the homeserver.
    -   `--manhole` - Turn on the twisted telnet manhole service on the given port.
    -   `--database-path` - The path to a sqlite database to use.
    -   `--verbose` - The verbosity level.
    -   `--log-file` - File to log to.
    -   `--log-config` - Python logging config file.
    -   `--enable-registration` - Enable registration for new users.

Application services:

-   Reliably retry sending of events from Synapse to application services, as per [Application Services](https://github.com/matrix-org/matrix-doc/blob/0c6bd9/specification/25_application_service_api.rst#home-server---application-service-api) spec.
-   Application services can no longer register via the `/register` API, instead their configuration should be saved to a file and listed in the synapse `app_service_config_files` config option. The AS configuration file has the same format as the old `/register` request. See [docs/application\_services.rst](docs/application_services.rst) for more information.

Changes in synapse v0.8.1 (2015-03-18)
======================================

-   Disable registration by default. New users can be added using the command `register_new_matrix_user` or by enabling registration in the config.
-   Add metrics to synapse. To enable metrics use config options `enable_metrics` and `metrics_port`.
-   Fix bug where banning only kicked the user.

Changes in synapse v0.8.0 (2015-03-06)
======================================

General:

-   Add support for registration fallback. This is a page hosted on the server which allows a user to register for an account, regardless of what client they are using (e.g. mobile devices).
-   Added new default push rules and made them configurable by clients:
    -   Suppress all notice messages.
    -   Notify when invited to a new room.
    -   Notify for messages that don\'t match any rule.
    -   Notify on incoming call.

Federation:

-   Added per host server side rate-limiting of incoming federation requests.
-   Added a `/get_missing_events/` API to federation to reduce number of `/events/` requests.

Configuration:

-   Added configuration option to disable registration: `disable_registration`.
-   Added configuration option to change soft limit of number of open file descriptors: `soft_file_limit`.
-   Make `tls_private_key_path` optional when running with `no_tls`.

Application services:

-   Application services can now poll on the CS API `/events` for their events, by providing their application service `access_token`.
-   Added exclusive namespace support to application services API.

Changes in synapse v0.7.1 (2015-02-19)
======================================

-   Initial alpha implementation of parts of the Application Services API. Including:
    -   AS Registration / Unregistration
    -   User Query API
    -   Room Alias Query API
    -   Push transport for receiving events.
    -   User/Alias namespace admin control
-   Add cache when fetching events from remote servers to stop repeatedly fetching events with bad signatures.
-   Respect the per remote server retry scheme when fetching both events and server keys to reduce the number of times we send requests to dead servers.
-   Inform remote servers when the local server fails to handle a received event.
-   Turn off python bytecode generation due to problems experienced when upgrading from previous versions.

Changes in synapse v0.7.0 (2015-02-12)
======================================

-   Add initial implementation of the query auth federation API, allowing servers to agree on whether an event should be allowed or rejected.
-   Persist events we have rejected from federation, fixing the bug where servers would keep requesting the same events.
-   Various federation performance improvements, including:
    -   Add in memory caches on queries such as:

        > -   Computing the state of a room at a point in time, used for authorization on federation requests.
        > -   Fetching events from the database.
        > -   User\'s room membership, used for authorizing presence updates.

    -   Upgraded JSON library to improve parsing and serialisation speeds.

-   Add default avatars to new user accounts using pydenticon library.
-   Correctly time out federation requests.
-   Retry federation requests against different servers.
-   Add support for push and push rules.
-   Add alpha versions of proposed new CSv2 APIs, including `/sync` API.

Changes in synapse 0.6.1 (2015-01-07)
=====================================

-   Major optimizations to improve performance of initial sync and event sending in large rooms (by up to 10x)
-   Media repository now includes a Content-Length header on media downloads.
-   Improve quality of thumbnails by changing resizing algorithm.

Changes in synapse 0.6.0 (2014-12-16)
=====================================

-   Add new API for media upload and download that supports thumbnailing.
-   Replicate media uploads over multiple homeservers so media is always served to clients from their local homeserver. This obsoletes the \--content-addr parameter and confusion over accessing content directly from remote homeservers.
-   Implement exponential backoff when retrying federation requests when sending to remote homeservers which are offline.
-   Implement typing notifications.
-   Fix bugs where we sent events with invalid signatures due to bugs where we incorrectly persisted events.
-   Improve performance of database queries involving retrieving events.

Changes in synapse 0.5.4a (2014-12-13)
======================================

-   Fix bug while generating the error message when a file path specified in the config doesn\'t exist.

Changes in synapse 0.5.4 (2014-12-03)
=====================================

-   Fix presence bug where some rooms did not display presence updates for remote users.
-   Do not log SQL timing log lines when started with \"-v\"
-   Fix potential memory leak.

Changes in synapse 0.5.3c (2014-12-02)
======================================

-   Change the default value for the content\_addr option to use the HTTP listener, as by default the HTTPS listener will be using a self-signed certificate.

Changes in synapse 0.5.3 (2014-11-27)
=====================================

-   Fix bug that caused joining a remote room to fail if a single event was not signed correctly.
-   Fix bug which caused servers to continuously try and fetch events from other servers.

Changes in synapse 0.5.2 (2014-11-26)
=====================================

Fix major bug that caused rooms to disappear from peoples initial sync.

Changes in synapse 0.5.1 (2014-11-26)
=====================================

See UPGRADES.rst for specific instructions on how to upgrade.

-   Fix bug where we served up an Event that did not match its signatures.
-   Fix regression where we no longer correctly handled the case where a homeserver receives an event for a room it doesn\'t recognise (but is in.)

Changes in synapse 0.5.0 (2014-11-19)
=====================================

This release includes changes to the federation protocol and client-server API that is not backwards compatible.

This release also changes the internal database schemas and so requires servers to drop their current history. See UPGRADES.rst for details.

Homeserver:

-   Add authentication and authorization to the federation protocol. Events are now signed by their originating homeservers.
-   Implement the new authorization model for rooms.
-   Split out web client into a seperate repository: matrix-angular-sdk.
-   Change the structure of PDUs.
-   Fix bug where user could not join rooms via an alias containing 4-byte UTF-8 characters.
-   Merge concept of PDUs and Events internally.
-   Improve logging by adding request ids to log lines.
-   Implement a very basic room initial sync API.
-   Implement the new invite/join federation APIs.

Webclient:

-   The webclient has been moved to a seperate repository.

Changes in synapse 0.4.2 (2014-10-31)
=====================================

Homeserver:

-   Fix bugs where we did not notify users of correct presence updates.
-   Fix bug where we did not handle sub second event stream timeouts.

Webclient:

-   Add ability to click on messages to see JSON.
-   Add ability to redact messages.
-   Add ability to view and edit all room state JSON.
-   Handle incoming redactions.
-   Improve feedback on errors.
-   Fix bugs in mobile CSS.
-   Fix bugs with desktop notifications.

Changes in synapse 0.4.1 (2014-10-17)
=====================================

Webclient:

-   Fix bug with display of timestamps.

Changes in synpase 0.4.0 (2014-10-17)
=====================================

This release includes changes to the federation protocol and client-server API that is not backwards compatible.

The Matrix specification has been moved to a separate git repository: <http://github.com/matrix-org/matrix-doc>

You will also need an updated syutil and config. See UPGRADES.rst.

Homeserver:

-   Sign federation transactions to assert strong identity over federation.
-   Rename timestamp keys in PDUs and events from \'ts\' and \'hsob\_ts\' to \'origin\_server\_ts\'.

Changes in synapse 0.3.4 (2014-09-25)
=====================================

This version adds support for using a TURN server. See docs/turn-howto.rst on how to set one up.

Homeserver:

-   Add support for redaction of messages.
-   Fix bug where inviting a user on a remote homeserver could take up to 20-30s.
-   Implement a get current room state API.
-   Add support specifying and retrieving turn server configuration.

Webclient:

-   Add button to send messages to users from the home page.
-   Add support for using TURN for VoIP calls.
-   Show display name change messages.
-   Fix bug where the client didn\'t get the state of a newly joined room until after it has been refreshed.
-   Fix bugs with tab complete.
-   Fix bug where holding down the down arrow caused chrome to chew 100% CPU.
-   Fix bug where desktop notifications occasionally used \"Undefined\" as the display name.
-   Fix more places where we sometimes saw room IDs incorrectly.
-   Fix bug which caused lag when entering text in the text box.

Changes in synapse 0.3.3 (2014-09-22)
=====================================

Homeserver:

-   Fix bug where you continued to get events for rooms you had left.

Webclient:

-   Add support for video calls with basic UI.
-   Fix bug where one to one chats were named after your display name rather than the other person\'s.
-   Fix bug which caused lag when typing in the textarea.
-   Refuse to run on browsers we know won\'t work.
-   Trigger pagination when joining new rooms.
-   Fix bug where we sometimes didn\'t display invitations in recents.
-   Automatically join room when accepting a VoIP call.
-   Disable outgoing and reject incoming calls on browsers we don\'t support VoIP in.
-   Don\'t display desktop notifications for messages in the room you are non-idle and speaking in.

Changes in synapse 0.3.2 (2014-09-18)
=====================================

Webclient:

-   Fix bug where an empty \"bing words\" list in old accounts didn\'t send notifications when it should have done.

Changes in synapse 0.3.1 (2014-09-18)
=====================================

This is a release to hotfix v0.3.0 to fix two regressions.

Webclient:

-   Fix a regression where we sometimes displayed duplicate events.
-   Fix a regression where we didn\'t immediately remove rooms you were banned in from the recents list.

Changes in synapse 0.3.0 (2014-09-18)
=====================================

See UPGRADE for information about changes to the client server API, including breaking backwards compatibility with VoIP calls and registration API.

Homeserver:

-   When a user changes their displayname or avatar the server will now update all their join states to reflect this.
-   The server now adds \"age\" key to events to indicate how old they are. This is clock independent, so at no point does any server or webclient have to assume their clock is in sync with everyone else.
-   Fix bug where we didn\'t correctly pull in missing PDUs.
-   Fix bug where prev\_content key wasn\'t always returned.
-   Add support for password resets.

Webclient:

-   Improve page content loading.
-   Join/parts now trigger desktop notifications.
-   Always show room aliases in the UI if one is present.
-   No longer show user-count in the recents side panel.
-   Add up & down arrow support to the text box for message sending to step through your sent history.
-   Don\'t display notifications for our own messages.
-   Emotes are now formatted correctly in desktop notifications.
-   The recents list now differentiates between public & private rooms.
-   Fix bug where when switching between rooms the pagination flickered before the view jumped to the bottom of the screen.
-   Add bing word support.

Registration API:

-   The registration API has been overhauled to function like the login API. In practice, this means registration requests must now include the following: \'type\':\'m.login.password\'. See UPGRADE for more information on this.
-   The \'user\_id\' key has been renamed to \'user\' to better match the login API.
-   There is an additional login type: \'m.login.email.identity\'.
-   The command client and web client have been updated to reflect these changes.

Changes in synapse 0.2.3 (2014-09-12)
=====================================

Homeserver:

-   Fix bug where we stopped sending events to remote homeservers if a user from that homeserver left, even if there were some still in the room.
-   Fix bugs in the state conflict resolution where it was incorrectly rejecting events.

Webclient:

-   Display room names and topics.
-   Allow setting/editing of room names and topics.
-   Display information about rooms on the main page.
-   Handle ban and kick events in real time.
-   VoIP UI and reliability improvements.
-   Add glare support for VoIP.
-   Improvements to initial startup speed.
-   Don\'t display duplicate join events.
-   Local echo of messages.
-   Differentiate sending and sent of local echo.
-   Various minor bug fixes.

Changes in synapse 0.2.2 (2014-09-06)
=====================================

Homeserver:

-   When the server returns state events it now also includes the previous content.
-   Add support for inviting people when creating a new room.
-   Make the homeserver inform the room via m.room.aliases when a new alias is added for a room.
-   Validate m.room.power\_level events.

Webclient:

-   Add support for captchas on registration.
-   Handle m.room.aliases events.
-   Asynchronously send messages and show a local echo.
-   Inform the UI when a message failed to send.
-   Only autoscroll on receiving a new message if the user was already at the bottom of the screen.
-   Add support for ban/kick reasons.

Changes in synapse 0.2.1 (2014-09-03)
=====================================

Homeserver:

-   Added support for signing up with a third party id.
-   Add synctl scripts.
-   Added rate limiting.
-   Add option to change the external address the content repo uses.
-   Presence bug fixes.

Webclient:

-   Added support for signing up with a third party id.
-   Added support for banning and kicking users.
-   Added support for displaying and setting ops.
-   Added support for room names.
-   Fix bugs with room membership event display.

Changes in synapse 0.2.0 (2014-09-02)
=====================================

This update changes many configuration options, updates the database schema and mandates SSL for server-server connections.

Homeserver:

-   Require SSL for server-server connections.
-   Add SSL listener for client-server connections.
-   Add ability to use config files.
-   Add support for kicking/banning and power levels.
-   Allow setting of room names and topics on creation.
-   Change presence to include last seen time of the user.
-   Change url path prefix to /\_matrix/\...
-   Bug fixes to presence.

Webclient:

-   Reskin the CSS for registration and login.
-   Various improvements to rooms CSS.
-   Support changes in client-server API.
-   Bug fixes to VOIP UI.
-   Various bug fixes to handling of changes to room member list.

Changes in synapse 0.1.2 (2014-08-29)
=====================================

Webclient:

-   Add basic call state UI for VoIP calls.

Changes in synapse 0.1.1 (2014-08-29)
=====================================

Homeserver:

-   Fix bug that caused the event stream to not notify some clients about changes.

Changes in synapse 0.1.0 (2014-08-29)
=====================================

Presence has been reenabled in this release.

Homeserver:

-   Update client to server API, including:
    -   Use a more consistent url scheme.
    -   Provide more useful information in the initial sync api.
-   Change the presence handling to be much more efficient.
-   Change the presence server to server API to not require explicit polling of all users who share a room with a user.
-   Fix races in the event streaming logic.

Webclient:

-   Update to use new client to server API.
-   Add basic VOIP support.
-   Add idle timers that change your status to away.
-   Add recent rooms column when viewing a room.
-   Various network efficiency improvements.
-   Add basic mobile browser support.
-   Add a settings page.

Changes in synapse 0.0.1 (2014-08-22)
=====================================

Presence has been disabled in this release due to a bug that caused the homeserver to spam other remote homeservers.

Homeserver:

-   Completely change the database schema to support generic event types.
-   Improve presence reliability.
-   Improve reliability of joining remote rooms.
-   Fix bug where room join events were duplicated.
-   Improve initial sync API to return more information to the client.
-   Stop generating fake messages for room membership events.

Webclient:

-   Add tab completion of names.
-   Add ability to upload and send images.
-   Add profile pages.
-   Improve CSS layout of room.
-   Disambiguate identical display names.
-   Don\'t get remote users display names and avatars individually.
-   Use the new initial sync API to reduce number of round trips to the homeserver.
-   Change url scheme to use room aliases instead of room ids where known.
-   Increase longpoll timeout.

Changes in synapse 0.0.0 (2014-08-13)
=====================================

-   Initial alpha release
