# Third party rules callbacks

Third party rules callbacks allow module developers to add extra checks to verify the
validity of incoming events. Third party event rules callbacks can be registered using
the module API's `register_third_party_rules_callbacks` method.

## Callbacks

The available third party rules callbacks are:

### `check_event_allowed`

_First introduced in Synapse v1.39.0_

```python
async def check_event_allowed(
    event: "synapse.events.EventBase",
    state_events: "synapse.types.StateMap",
) -> Tuple[bool, Optional[dict]]
```

**<span style="color:red">
This callback is very experimental and can and will break without notice. Module developers
are encouraged to implement `check_event_for_spam` from the spam checker category instead.
</span>**

Called when processing any incoming event, with the event and a `StateMap`
representing the current state of the room the event is being sent into. A `StateMap` is
a dictionary that maps tuples containing an event type and a state key to the
corresponding state event. For example retrieving the room's `m.room.create` event from
the `state_events` argument would look like this: `state_events.get(("m.room.create", ""))`.
The module must return a boolean indicating whether the event can be allowed.

Note that this callback function processes incoming events coming via federation
traffic (on top of client traffic). This means denying an event might cause the local
copy of the room's history to diverge from that of remote servers. This may cause
federation issues in the room. It is strongly recommended to only deny events using this
callback function if the sender is a local user, or in a private federation in which all
servers are using the same module, with the same configuration.

If the boolean returned by the module is `True`, it may also tell Synapse to replace the
event with new data by returning the new event's data as a dictionary. In order to do
that, it is recommended the module calls `event.get_dict()` to get the current event as a
dictionary, and modify the returned dictionary accordingly.

If `check_event_allowed` raises an exception, the module is assumed to have failed.
The event will not be accepted but is not treated as explicitly rejected, either.
An HTTP request causing the module check will likely result in a 500 Internal
Server Error.

When the boolean returned by the module is `False`, the event is rejected.
(Module developers should not use exceptions for rejection.)

Note that replacing the event only works for events sent by local users, not for events
received over federation.

If multiple modules implement this callback, they will be considered in order. If a
callback returns `True`, Synapse falls through to the next one. The value of the first
callback that does not return `True` will be used. If this happens, Synapse will not call
any of the subsequent implementations of this callback.

### `on_create_room`

_First introduced in Synapse v1.39.0_

```python
async def on_create_room(
    requester: "synapse.types.Requester",
    request_content: dict,
    is_requester_admin: bool,
) -> None
```

Called when processing a room creation request, with the `Requester` object for the user
performing the request, a dictionary representing the room creation request's JSON body
(see [the spec](https://matrix.org/docs/spec/client_server/latest#post-matrix-client-r0-createroom)
for a list of possible parameters), and a boolean indicating whether the user performing
the request is a server admin.

Modules can modify the `request_content` (by e.g. adding events to its `initial_state`),
or deny the room's creation by raising a `module_api.errors.SynapseError`.

If multiple modules implement this callback, they will be considered in order. If a
callback returns without raising an exception, Synapse falls through to the next one. The
room creation will be forbidden as soon as one of the callbacks raises an exception. If
this happens, Synapse will not call any of the subsequent implementations of this
callback.

### `check_threepid_can_be_invited`

_First introduced in Synapse v1.39.0_

```python
async def check_threepid_can_be_invited(
    medium: str,
    address: str,
    state_events: "synapse.types.StateMap",
) -> bool:
```

Called when processing an invite via a third-party identifier (i.e. email or phone number).
The module must return a boolean indicating whether the invite can go through.

If multiple modules implement this callback, they will be considered in order. If a
callback returns `True`, Synapse falls through to the next one. The value of the first
callback that does not return `True` will be used. If this happens, Synapse will not call
any of the subsequent implementations of this callback.

### `check_visibility_can_be_modified`

_First introduced in Synapse v1.39.0_

```python
async def check_visibility_can_be_modified(
    room_id: str,
    state_events: "synapse.types.StateMap",
    new_visibility: str,
) -> bool:
```

Called when changing the visibility of a room in the local public room directory. The
visibility is a string that's either "public" or "private". The module must return a
boolean indicating whether the change can go through.

If multiple modules implement this callback, they will be considered in order. If a
callback returns `True`, Synapse falls through to the next one. The value of the first
callback that does not return `True` will be used. If this happens, Synapse will not call
any of the subsequent implementations of this callback.

### `on_new_event`

_First introduced in Synapse v1.47.0_

```python
async def on_new_event(
    event: "synapse.events.EventBase",
    state_events: "synapse.types.StateMap",
) -> None:
```

Called after sending an event into a room. The module is passed the event, as well
as the state of the room _after_ the event. This means that if the event is a state event,
it will be included in this state.

Note that this callback is called when the event has already been processed and stored
into the room, which means this callback cannot be used to deny persisting the event. To
deny an incoming event, see [`check_event_for_spam`](spam_checker_callbacks.md#check_event_for_spam) instead.

If multiple modules implement this callback, Synapse runs them all in order.

## Example

The example below is a module that implements the third-party rules callback
`check_event_allowed` to censor incoming messages as dictated by a third-party service.

```python
from typing import Optional, Tuple

from synapse.module_api import ModuleApi

_DEFAULT_CENSOR_ENDPOINT = "https://my-internal-service.local/censor-event"

class EventCensorer:
    def __init__(self, config: dict, api: ModuleApi):
        self.api = api
        self._endpoint = config.get("endpoint", _DEFAULT_CENSOR_ENDPOINT)

        self.api.register_third_party_rules_callbacks(
            check_event_allowed=self.check_event_allowed,
        )

    async def check_event_allowed(
        self,
        event: "synapse.events.EventBase",
        state_events: "synapse.types.StateMap",
    ) -> Tuple[bool, Optional[dict]]:
        event_dict = event.get_dict()
        new_event_content = await self.api.http_client.post_json_get_json(
            uri=self._endpoint, post_json=event_dict,
        )
        event_dict["content"] = new_event_content
        return event_dict
```
