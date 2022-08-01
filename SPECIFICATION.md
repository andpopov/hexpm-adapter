[Specification](https://github.com/hexpm/specifications/blob/488fdb7e0d92c2149b7d21088621176d3ec76c8d/apiary.apib) + [online viewer](https://dillinger.io/)

[endpoints](https://github.com/hexpm/specifications/blob/main/endpoints.md#repository)

[hex man](https://medium.com/@toddresudek/hex-power-user-deb608e60935)

[Configure Hex](https://hexdocs.pm/hex/Mix.Tasks.Hex.Config.html) - `mix hex.config`

[Building custom Hex repositories](https://dashbit.co/blog/mix-hex-registry-build)

## Case for using Mix with dependency from another repository

_Mix is the building system for Elixir and Erlang like Maven for Java_

[install](https://elixir-lang.org/install.html) erlang and elixir

create new project
```shell
mix new kv --module KV
```

install hex
```shell
mix local.hex --force
```

**Not require** generate a private key for create hex registry
```shell
openssl genrsa -out private_key.pem
 ```

[self-hosted hex repo](https://hex.pm/docs/self_hosting)

**Not require** create hex registry and it will create `public_key`
```shell
mix hex.registry build public --name=my_repo --private-key=private_key.pem
```
[//]: # (todo расковырять как делать публичный ключ без создания registry через hex )

my_repo will contain my_artifact.tar

Add a dependency from Artipie repository(repo name is **_my_repo_**) in `mix.exs` in the `defp deps` function (https://hexdocs.pm/hex/Mix.Tasks.Hex.Repo.html):
```elixir
  defp deps do
    [
      {:my_artifact, "~> 0.4.0", repo: "my_repo"}
    ]
  end
```

add repo
```shell
mix hex.repo add my_repo http//:localhost:<artipie_port>
```

show all repositories
```shell
mix hex.repo list
```

get dependencies with lock version
```shell
mix deps.get
```

update dependencies bypass of locked version
```shell
mix hex.outdated
mix deps.update
mix deps.update --all
```

<hr>

### fetch dependencies work only with hex.registry???
```shell
mix hex.package fetch
mix hex.package fetch my_artifact 0.4.0 --repo=my_repo
```

<hr>

###  Use it to publish packages in private repo

$ HEX_API_URL=http://<HOST> HEX_API_KEY=<AUTH_KEY> mix hex.publish package

For publishing we have the /api/publish endpoint https://github.com/hexpm/hexpm/pull/674 + https://github.com/hexpm/hexpm/issues/489 + https://github.com/hexpm/hex/pull/665/files

