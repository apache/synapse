<!--
SPDX-License-Identifier: Apache-2.0

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Threat Model — Apache Synapse

## §1 Header

- **Project:** Apache Synapse — a lightweight, high-performance **Enterprise Service Bus (ESB) / mediation
  engine**. It accepts messages over pluggable transports (HTTP/S, JMS, VFS, Mail, …), runs them through
  operator-defined **mediation sequences / proxy services** (mediators: XSLT, XQuery, script, filter,
  switch, send-to-endpoint, …), and routes/transforms them toward backend **endpoints** *(documented — README;
  source `org.apache.synapse.mediators`, `config.xml`)*.
- **Modelled against:** `apache/synapse` `master`/HEAD (2026-05-31).
- **Status:** **DRAFT — v0, not yet reviewed by the Synapse PMC.** Produced by the ASF Security team via the
  `threat-model-producer` rubric (<https://gist.github.com/potiuk/da14a826283038ddfe38cc9fe6310573>).
- **Reporting / version-binding / legend** as in the sibling models. **Draft confidence:** ~12 documented /
  0 maintainer / ~48 inferred. Each *(inferred)* routes to §14.

**Framing note (as for any framework):** Synapse is a *mediation engine*, not a finished application. The
**integration developer** authors the synapse configuration — sequences, mediators, scripts, XSLT/XQuery,
endpoints, and security policies. That configuration is **trusted input** (§3); the **inbound message from a
network client is the untrusted adversary input** (§7). Most properties are conditional on how the
integration is configured, so §9/§10 carry a lot of weight.

## §2 Scope and intended use

Intended use *(documented)*: deploy Synapse as a message broker/mediator in front of or between services —
clients send messages to a Synapse proxy/API; Synapse mediates (transform, route, secure, throttle) and
forwards to backend endpoints.

Caller roles:

- **Message client (untrusted)** — any peer that can send a message to a Synapse listener/proxy/API.
- **Backend endpoint** — a service Synapse calls; semi-trusted (its responses re-enter mediation).
- **Integration developer / operator** — authors the synapse config (mediation logic, scripts, XSLT,
  endpoints, secure-vault secrets, transport + WS-Security policy). **Trusted; out of model as adversary (§3).**

**Component-family table:**

| Family | Entry point | Touches outside process | In model? |
| --- | --- | --- | --- |
| Transport listeners | HTTP/S (NHTTP/passthrough), JMS, VFS, Mail | network / fs / mail | **Yes** |
| Mediation engine | sequences / proxy services / APIs | — | **Yes** |
| XML transform mediators | **XSLT**, **XQuery**, payload factory | XML; **external refs** | **Yes (high-value)** |
| Script mediators | JS/Groovy/… (operator-authored) | runs config code over message data | **Yes (data-in surface)** |
| Endpoints (outbound) | send/call mediators, address/WSDL/loadbalance | **network egress** | **Yes (SSRF surface)** |
| Eventing | WS-Eventing subscriptions | network | **Yes** |
| Secrets / secure-vault | encrypted config secrets | keystore | **Yes** |
| Samples / docs / build | `modules/documentation`, samples, tests | — | No → §3 |

## §3 Out of scope (explicit non-goals)

- **The integration developer / operator as adversary**, and the **synapse configuration** itself (sequences,
  scripts, XSLT/XQuery bodies, endpoint addresses, secrets). Config is authored by a trusted party; a script
  mediator running operator-authored code is not an adversary surface — the message *data flowing into* it is
  *(inferred)*.
- **Misconfiguration** (enabling external-entity resolution, routing to an attacker-derived endpoint without
  validation, disabling TLS) — Synapse provides the controls; using them is the operator's job (§10/§11).
- **Backend services** Synapse mediates to, and the message producers' own security.
- **Samples, documentation, and tests** *(inferred)*.
- **The underlying XML/crypto stacks** (the JAXP/StAX provider, Rampart/WSS4J) except as Synapse configures
  and invokes them.

## §4 Trust boundaries and data flow

The trust boundary is the **transport listener + the mediation entry**: bytes arriving on a listener are
untrusted until mediation (and any configured WS-Security/transport auth) has processed them *(inferred)*.

Trust transitions:

1. **Wire → message build:** the transport builds a message (SOAP/XML/JSON/binary). XML building is the
   XXE / entity-expansion / large-message DoS surface *(inferred — wave-1)*.
2. **Message → XSLT/XQuery mediator:** transforms may resolve external entities, `document()` / `doc()`
   references, or extension functions — an **XXE / SSRF / file-read** surface if external resolution is enabled
   *(inferred — `XSLTMediator`; high-value, §14)*.
3. **Message → script mediator:** operator-authored JS/Groovy runs with message data as input. The *code* is
   trusted (config); the risk is unsafe handling of message data inside it *(inferred)*.
4. **Message → endpoint resolution:** static endpoints are config (trusted); **dynamic / content-based
   routing** that derives an endpoint address from message content is an **SSRF** surface *(inferred)*.
5. **Endpoint response → mediation:** backend responses re-enter mediation as semi-trusted input.

**Reachability precondition:** a finding is in-model if reachable from an inbound message *before* the
mediation auth/validation the integration configured; a finding requiring a malicious **config** (script,
XSLT body, endpoint address chosen by the operator) is `OUT-OF-MODEL: trusted-input` (§3/§6).

## §5 Assumptions about the environment

- JVM host running the Synapse runtime; operator-managed `synapse.xml` config, keystores, and transport setup.
- Transports reachable per operator network config; TLS provided by the transport configuration *(inferred)*.
- Secrets via secure-vault are protected by an operator-managed keystore/password *(inferred)*.
- **What Synapse does to its host (*(inferred)* — wave-2):** binds transport listeners; opens **outbound**
  connections to configured (and possibly dynamically-resolved) endpoints; reads config + keystores; XSLT/
  XQuery may fetch external references if enabled. Not assumed to spawn host processes beyond configured
  command/script mediators.

## §5a Build-time and configuration variants

| Knob (names *(inferred)*) | Effect | Ruling needed |
| --- | --- | --- |
| XML secure-processing / DTD + external-entity resolution in builders & XSLT/XQuery | XXE / SSRF / file-read on inbound transforms | **Open (wave-1):** are external entities/`document()` off by default? |
| Message size / element-depth / streaming limits | XML/large-message DoS | **Open (wave-1)** |
| Dynamic / content-based endpoint resolution | SSRF if endpoint derived from message | Open — validated/allow-listed? |
| Transport TLS (HTTPS listener + outbound) | Confidentiality/integrity | Operator (§10) |
| WS-Security (Rampart) on a proxy | Message-level auth/sig/enc | Integration choice |
| Script-mediator languages enabled | Operator-code surface | Operator config |

## §6 Assumptions about inputs

| Entry point | Parameter | Attacker-controllable? | Caller/operator must enforce |
| --- | --- | --- | --- |
| transport listener | message body (SOAP/XML/JSON/binary), headers, SOAPAction | **yes** | XML limits; transport/WS-Security; size caps |
| XSLT/XQuery mediator | message payload (the transform *input*) | **yes** | disable external entity/`document()` resolution |
| script mediator | message payload passed to the script | **yes** | safe handling of message data in the script |
| dynamic endpoint | endpoint address *derived from message* (if used) | **yes (if configured)** | validate/allow-list resolved addresses |
| synapse config (sequences, scripts, XSLT, endpoints, secrets) | all | **no — operator-trusted** | never sourced from a message |

## §7 Adversary model

- **Primary adversary:** an untrusted client sending messages to a Synapse listener/proxy/API. Capabilities:
  craft SOAP/XML/JSON payloads (XXE, entity-expansion, oversized), drive content that influences XSLT/XQuery
  resolution, supply data that a dynamic route turns into an endpoint address (SSRF), or that a script
  mishandles.
- **Secondary:** a malicious backend endpoint returning hostile responses into mediation.
- **Goals:** XXE/file-read/SSRF via transforms or routing; XML/message DoS; bypass of a configured
  mediation-level auth; exfiltration of secrets reachable through a transform.
- **Out of model:** the integration developer/operator; the config (scripts, XSLT bodies, endpoint
  addresses); keystore/secret holders.

## §8 Security properties the project provides

*(Conditional on configuration; *(inferred)* pending §14.)*

1. **Robust message building/parsing.** Malformed/oversized inbound messages yield a fault, not memory
   corruption or unbounded resource use (subject to configured limits) *(inferred)*. *Symptom:* crash/hang/OOM
   from crafted input. *Severity:* high.
2. **Safe-by-default XML transforms.** XSLT/XQuery and message builders do not resolve external entities/
   `document()` against untrusted input unless explicitly enabled *(inferred — load-bearing; wave-1)*.
   *Symptom:* XXE read / SSRF / file disclosure via a transform. *Severity:* critical.
3. **Mediation-level security mechanisms.** When configured, transport security and WS-Security (Rampart)
   authenticate/sign/encrypt messages *(inferred)*. *Symptom:* accepted unauthenticated/forged message where
   policy required otherwise. *Severity:* critical.
4. **Secret protection.** Secure-vault keeps configured secrets encrypted at rest, not in plaintext config
   *(inferred)*. *Symptom:* plaintext secret exposure. *Severity:* high.
5. **Transport security support.** TLS on HTTPS listeners and outbound calls with cert validation when
   configured *(inferred)*. *Symptom:* MITM where TLS expected. *Severity:* high.

## §9 Security properties the project does NOT provide

- **No security without configuration** — a proxy with no transport/WS-Security and permissive transforms is
  only as protected as the integration wired it *(inferred)*.
- **No defence against the integration developer** — scripts, XSLT/XQuery bodies, and endpoint addresses are
  trusted config (§3).
- **No intrinsic SSRF protection for dynamic/content-based routing** — if an endpoint is derived from message
  content, validating it is the integration's job *(inferred)*.

**False friends:**

- *An XSLT/XQuery transform looks like pure data transformation but can read files / fetch URLs* via external
  entities, `document()`/`doc()`, or extension functions if external resolution is left enabled.
- *A script mediator looks sandboxed but runs with the engine's privileges* — it is operator code, not a
  security boundary for message data.
- *Content-based routing looks like internal plumbing but can become SSRF* when the route target is
  attacker-influenced.

**Well-known attack classes to keep in view:** XXE and XML entity-expansion DoS; SSRF via XSLT `document()`/
external entities and via dynamic endpoint resolution; oversized-message / streaming DoS; injection into a
downstream system via an unsanitized transform; secret exposure through an over-broad transform; XML
signature-wrapping where WS-Security is used (see the CXF/WSS4J model).

## §10 Downstream (integrator/operator) responsibilities

- **Keep external-entity / DTD / `document()` resolution disabled** in message builders and XSLT/XQuery on
  untrusted inbound paths; keep message-size/depth limits on.
- **Validate or allow-list** any endpoint address derived from message content (anti-SSRF).
- Configure transport TLS (with cert validation) and WS-Security where the integration requires
  authentication/integrity.
- Treat script/XSLT/XQuery mediator bodies as code you own; don't accept them from untrusted sources.
- Protect the secure-vault keystore/password; don't commit plaintext secrets.

## §11 Known misuse patterns

- Exposing a proxy with no transport/message security and assuming the ESB "is secure".
- Enabling external-entity / `document()` resolution in XSLT/XQuery over untrusted messages.
- Deriving an endpoint address from message content without validation (SSRF).
- Embedding secrets in plaintext config instead of secure-vault.
- Routing untrusted message content into a script mediator that then executes/concatenates it unsafely.

## §11a Known non-findings (recurring false positives)

*(v0 seed — the PMC will own the authoritative list — §14.)*

- **A script/XSLT/XQuery mediator "executes code"** — operator-authored config (§3/§9); not a finding unless a
  *default* path lets an untrusted message reach unsafe resolution.
- **XXE/SSRF reachable only when the operator enabled external resolution** — `OUT-OF-MODEL: non-default-build`
  unless the *default* resolves external entities (then `VALID` — wave-1).
- **SSRF via an endpoint address the operator configured statically** — trusted input (§6).
- **Findings in samples / documentation / tests** — out of scope (§3).
- **Use of a weak algorithm explicitly configured** in a WS-Security policy — integration choice.

## §12 Conditions that would change this model

- A change to default XML/transform external-resolution or size-limit posture.
- A new transport, mediator, or default that resolves untrusted references.
- Dynamic endpoint resolution becoming on/permissive by default.
- A change in secure-vault or WS-Security defaults.
- Any report not cleanly routable to a §13 disposition.

## §13 Triage dispositions

| Disposition | Meaning | Licensed by |
| --- | --- | --- |
| `VALID` | Violates a claimed property via an in-scope adversary/input in a default config. | §8, §6, §7 |
| `VALID-HARDENING` | No §8 property broken, but a §11 misuse warrants a safer default/guard. | §11 |
| `OUT-OF-MODEL: trusted-input` | Requires control of the synapse config (script/XSLT/endpoint/secret). | §6, §3 |
| `OUT-OF-MODEL: adversary-not-in-scope` | Requires operator/keystore capability. | §7, §3 |
| `OUT-OF-MODEL: unsupported-component` | Lands in samples/docs/tests. | §3 |
| `OUT-OF-MODEL: non-default-build` | Only when an insecure non-default transform/resolution option was enabled. | §5a |
| `BY-DESIGN: property-disclaimed` | Concerns a §9-disclaimed property (no security without config; scripts are operator code). | §9 |
| `KNOWN-NON-FINDING` | Matches a §11a entry. | §11a |
| `MODEL-GAP` | Routes to none of the above → revise the model. | §12 |

## §14 Open questions for the maintainers

**Wave 1 — transform/parse defaults (decide VALID-vs-misconfig; §5a/§8):**
1. By default, do the **message builders and XSLT/XQuery mediators disable DTD / external-entity / `document()`
   resolution** on untrusted inbound messages, so an XXE/SSRF-via-transform report against defaults is `VALID`?
   *Proposed:* external resolution off by default; enabling it is operator opt-in.
2. Are there **default message-size / element-depth / streaming limits** that bound XML/large-message DoS?
   *Proposed:* configurable limits; sensible defaults.

**Wave 2 — routing & scripts (§4/§9):**
3. Is **dynamic / content-based endpoint resolution** something an untrusted message can influence by default,
   and is the resolved address validated/allow-listed? *Proposed:* static endpoints are the norm; dynamic
   resolution is opt-in and the integration validates it (SSRF = integration responsibility).
4. Confirm **script / XSLT / XQuery mediator bodies are trusted config** (operator-authored), so "code
   execution in a mediator" is `OUT-OF-MODEL: trusted-input` rather than a framework finding. *Proposed:* yes.

**Wave 3 — secrets, WS-Security, §11a (§8/§11a):**
5. How does **secure-vault** protect secrets, and what does Synapse claim about secret exposure through
   transforms/logging? *Proposed:* encrypted at rest; avoid logging secrets.
6. What do scanners most often (re)report here that the PMC considers a **non-finding**? (Seeds §11a.)

**Meta:**
7. Confirm this model lives as root `THREAT_MODEL.md` referenced from a new `SECURITY.md`. *Proposed:* yes.

## §15 Machine-readable companion

Deferred for v0; a `threat-model.yaml` can later encode the §6 trust table, §2/§3 scoping, §8 rows, §9 false
friends, §11a non-findings, and §13 dispositions.
