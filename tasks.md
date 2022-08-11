1. Now we have test case for pull via Mix. We have to **_implement also for Rebar_** [Usage](https://hex.pm/docs/rebar3_usage). Mix and Rebar is the building system for Elixir and Erlang, like Maven and Gradle for Java

2. _**Investigate about public key**_ for repository: _--public-key PATH - Path to public key used to verify the registry_ (optional). [Mix.Tasks.Hex.Repo](https://hexdocs.pm/hex/Mix.Tasks.Hex.Repo.html). That allow remove `.withEnv("HEX_UNSAFE_REGISTRY", "1")` from `HexITCase.init()`

3. Content-Type - application/vnd.hex+<needed format> implement these type of headers:
   Custom media types are used in the API to let consumers choose the format of the data they wish to receive. This is done by adding one or more of the following types to the Accept header when you make a request.

The API supports two media types; JSON and Erlang. Hex media types look like this:

    application/vnd.hex[+format]

The following are the accepted media types:

* application/json
* application/vnd.hex+json
* application/vnd.hex+erlang

The erlang media type is a safe subset of erlang terms serialized with [`erlang:term_to_binary/1`](http://www.erlang.org/doc/man/erlang.html#term_to_binary-1). The only allowed terms are maps, lists, tuples, numbers, binaries and booleans. Atoms are strictly not allowed.

4. every time when publish artifact you need enter local machine password.  
Use token or auth with organization

5. when publish - make documentation