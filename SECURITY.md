# Security Policy

## Reporting a Vulnerability

Apache Synapse follows the [Apache Software Foundation security process](https://www.apache.org/security/).
Please report suspected vulnerabilities **privately** to `security@apache.org` (the Synapse PMC is reachable
at `private@synapse.apache.org`). Do **not** open public GitHub issues or pull requests for security reports.

## Threat Model

What Synapse treats as in/out of scope, the security properties it provides and disclaims (safe-by-default
XML transforms, mediation-level security, secret protection), the adversary model (the untrusted message
sender vs. the trusted integration configuration), and how findings are triaged are documented in
[THREAT_MODEL.md](./THREAT_MODEL.md).
