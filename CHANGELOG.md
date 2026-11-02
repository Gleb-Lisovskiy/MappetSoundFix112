# Changelog

## 1.0.0

* Initial release.
* Inject into `mchorse.mappet.client.SoundPack.getFile` and resolve custom
  sound paths case-insensitively when the exact file was not found.
* Forge configuration: `caseInsensitiveLookup`, `logResolvedFiles`.
* Deterministic resolution when several files differ only by case.
* Refuses `..` path components, so a lookup can never escape the sound folder.
* Unit tests for the resolver.
