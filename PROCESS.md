# Process

This document describes the practices we use when developing and maintaining `apso`.

## Pull requests

We encourage contributions to be in the form of pull requests. We keep track of a [Changelog](CHANGELOG.md) but it's not
expected for pull requests to eagerly update it. The [Changelog](CHANGELOG.md) should instead be updated prior to a
[release](PROCESS.md#releasing).

## Versioning

We try to adopt _MAJOR.MINOR.PATCH_ [Semantic Versioning 2.0.0](https://semver.org/):

* _PATCH_ version Z (x.y.Z) is incremented if only backwards compatible bug fixes are introduced.
* _MINOR_ version Y (x.Y.z) is incremented if new, binary backwards compatible functionality is introduced to the public
  API. A source breaking but binary compatible change is allowed between _MINOR_ versions.
* _MAJOR_ version X (X.y.z) is incremented if new binary backward breaking changes are introduced.

## Releasing

### Updating the Changelog

Before relasing, create, if it doesn't exist yet, a [Changelog](CHANGELOG.md) entry for the version you're releasing, 
following the template.

It's recommended to open a PR with the Changelog changes so that they can be reviewed by someone else from the team.

### Releasing artifacts

TO DO
