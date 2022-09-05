2. _**Investigate about public key**_ for repository: _--public-key PATH - Path to public key used to verify the registry_ (optional). [Mix.Tasks.Hex.Repo](https://hexdocs.pm/hex/Mix.Tasks.Hex.Repo.html). That will allow remove `.withEnv("HEX_UNSAFE_REGISTRY", "1")` and `.withEnv("HEX_NO_VERIFY_REPO_ORIGIN", "1")`from `HexITCase.init()` and smoke test in Artipie repository.

4. every time when publish artifact you need enter local machine password.  
Use token or auth with organization

5. when publish - make documentation

7. add support `layout:org`.     .setRepository("artipie") in UploadSlice should be org name

