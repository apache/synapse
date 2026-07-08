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
- **Modelled against:** `apache/synapse` `master` @ `0b40f4d7d7777d109f167c0b4afa21afbfb10988` (2026-06-15),
  with dependency defaults verified against **Axis2 1.7.9** and **Axiom 1.2.22** (the versions pinned in the
  root `pom.xml`).
- **Status:** **v1 — source-verified **  Produced by the ASF Security
  team via the `threat-model-producer` rubric
  (<https://gist.github.com/potiuk/da14a826283038ddfe38cc9fe6310573>). 
  Reviewed by the Synapse PMC.
- **Reporting / version-binding / legend** as in the sibling models. **Confidence:** ~14 documented /
  0 maintainer / ~34 source-verified / ~12 inferred.

**Framing note (as for any framework):** Synapse is a *mediation engine*, not a finished application. The
**integration developer** authors the synapse configuration — sequences, mediators, scripts, XSLT/XQuery,
endpoints, and security policies. That configuration is **trusted input** (§3); the **inbound message from a
network client is the untrusted adversary input** (§7). Most properties are conditional on how the
integration is configured, so §9/§10 carry a lot of weight.

## §2 Scope and intended use

Intended use *(documented)*: deploy Synapse as a message broker/mediator in front of or between services —
clients send messages to a Synapse proxy/API; Synapse mediates (transform, route, secure, throttle) and
forwards to backend endpoints. In **message-mediation mode**, Synapse additionally acts as a
WS-Addressing-style router: a bare `<send/>` with no configured endpoint forwards to the message's implicit
`To` address *(source-verified — `SendMediator.mediate()`; see §9)*.

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
  *(source-verified — config is loaded from operator-controlled `synapse.xml`/registry, never from messages)*.
- **Misconfiguration** (enabling external-entity resolution, routing to an attacker-derived endpoint without
  validation, disabling TLS) — Synapse provides the controls; using them is the operator's job (§10/§11).
- **Backend services** Synapse mediates to, and the message producers' own security.
- **Samples, documentation, and tests** (`modules/samples`, `modules/documentation`, `src/test` trees).
- **The underlying XML/crypto stacks** (the JAXP/StAX provider, Rampart/WSS4J) except as Synapse configures
  and invokes them. Note however that two dependency *defaults* are load-bearing for §8 and are therefore
  version-bound in §1: Axis2's builder parser configuration and Axiom's `StAXParserConfiguration.SOAP`.

## §4 Trust boundaries and data flow

The trust boundary is the **transport listener + the mediation entry**: bytes arriving on a listener are
untrusted until mediation (and any configured WS-Security/transport auth) has processed them.

Trust transitions:

1. **Wire → message build:** the transport builds a message (SOAP/XML/JSON/binary).
   **Source-verified:** both the SOAP and the plain-XML (POX) build paths go through Axis2
   `BuilderUtil.createSOAPModelBuilder` / `createPOXBuilder`, which use Axiom
   `StAXParserConfiguration.SOAP` → `StAXDialect.disallowDoctypeDecl`. **A DOCTYPE declaration in an
   inbound message is rejected outright**, which blocks classic XXE *and* DTD-based entity-expansion
   (billion-laughs) by default. The passthrough HTTP transport additionally **defers building** the message
   body (`RelayUtils`/`DeferredMessageBuilder`): the envelope is only parsed when a mediator actually needs
   it, so pure-routing configurations never parse untrusted bodies at all. There are, however, **no
   built-in message-size or element-depth limits** in the NHTTP/passthrough transports (§9).
2. **Message → XSLT/XQuery mediator:** the transform *input* is the already-parsed AXIOM tree, so no DTD/XXE
   can be introduced at this stage (blocked at transition 1). The XSLT mediator creates its
   `TransformerFactory` with **JAXP defaults — `FEATURE_SECURE_PROCESSING` is *not* set** *(source-verified —
   `XSLTMediator`)*; stylesheet functions such as `document()` / `doc()` (XQuery via Saxon XQJ) therefore
   remain available. Because the stylesheet/query body is trusted config, this becomes an attacker surface
   only when the operator's stylesheet feeds *message-derived* strings into `document()`/`doc()` or extension
   functions — an integration responsibility (§9/§10), with a per-mediator hardening knob available
   (`<feature>`/`<attribute>` elements on `<xslt>` can enable secure-processing — §5a).
3. **Message → script mediator:** operator-authored JS/Groovy runs with message data as input. The *code* is
   trusted (config); the risk is unsafe handling of message data inside it.
4. **Message → endpoint resolution:** static endpoints are config (trusted). Two attacker-influenced routes
   exist: (a) **explicit dynamic/content-based routing** the integration builds (header/XPath-derived
   addresses), and (b) **implicit `To` routing** — `<send/>` with no endpoint forwards to the inbound
   message's `To` address *(source-verified — `SendMediator`)*. Both are **SSRF** surfaces the integration
   must constrain (§9/§10).
5. **Endpoint response → mediation:** backend responses re-enter mediation as semi-trusted input.

**Reachability precondition:** a finding is in-model if reachable from an inbound message *before* the
mediation auth/validation the integration configured; a finding requiring a malicious **config** (script,
XSLT body, endpoint address chosen by the operator) is `OUT-OF-MODEL: trusted-input` (§3/§6).

## §5 Assumptions about the environment

- JVM host running the Synapse runtime; operator-managed `synapse.xml` config, keystores, and transport setup.
- Transports reachable per operator network config; TLS provided by the transport configuration.
- Secrets via secure-vault are protected by an operator-managed keystore/password *(source-verified —
  `org.apache.synapse.securevault`: keystore-backed asymmetric/symmetric ciphers; `PasswordManager` resolves
  encrypted passwords at runtime)*.
- **What Synapse does to its host:** binds transport listeners; opens **outbound** connections to configured
  (and possibly message-derived — §4.4) endpoints; reads config + keystores; XSLT/XQuery may fetch external
  references if the trusted stylesheet requests them. Not assumed to spawn host processes beyond configured
  command/script mediators.
- Because there are no built-in message-size limits (§9), **deployment-level ingress controls** (reverse
  proxy / LB request-size caps, connection limits) are assumed for internet-facing listeners.

## §5a Build-time and configuration variants

| Knob | Effect | Ruling |
| --- | --- | --- |
| Inbound XML DOCTYPE handling | XXE / entity-expansion | **Resolved (source-verified):** DOCTYPE **rejected by default** on SOAP and POX build paths (`StAXParserConfiguration.SOAP`). No supported knob re-enables it per-service; a report reaching XXE through the default build path is `VALID`. |
| `<xslt>` `<feature>`/`<attribute>` elements | Per-mediator `TransformerFactory` features (e.g. `http://javax.xml.XMLConstants/feature/secure-processing`) | **Resolved (source-verified):** off by default (JAXP defaults apply); enabling secure-processing is an operator hardening opt-in. |
| Message size / element-depth limits | XML/large-message DoS | **Resolved (source-verified):** **no built-in limits** in NHTTP/passthrough. Passthrough streaming + deferred building bound the pure-routing path; parsing paths rely on deployment controls (§5, §10). Documented as a §9 disclaimed property. |
| Dynamic / content-based endpoint resolution; implicit `To` routing via bare `<send/>` | SSRF if destination derived from message | **Resolved (source-verified):** implicit `To` routing is designed-in for message-mediation mode; **no built-in allow-listing**. Validation is the integration's job (§9/§10). |
| Transport TLS (HTTPS listener + outbound) | Confidentiality/integrity | Operator (§10). |
| WS-Security (Rampart) on a proxy | Message-level auth/sig/enc | Integration choice. |
| Script-mediator languages enabled | Operator-code surface | Operator config. |

## §6 Assumptions about inputs

| Entry point | Parameter | Attacker-controllable? | Caller/operator must enforce |
| --- | --- | --- | --- |
| transport listener | message body (SOAP/XML/JSON/binary), headers, SOAPAction, `To`/request URI | **yes** | deployment size caps; transport/WS-Security |
| XSLT/XQuery mediator | message payload (the transform *input*) | **yes** | don't pass message data into `document()`/`doc()`/extensions; optionally enable secure-processing |
| script mediator | message payload passed to the script | **yes** | safe handling of message data in the script |
| dynamic endpoint / bare `<send/>` | destination derived from message (`To`, headers, XPath) | **yes (if configured / message-mediation mode)** | validate/allow-list resolved addresses |
| synapse config (sequences, scripts, XSLT, endpoints, secrets) | all | **no — operator-trusted** | never sourced from a message |

## §7 Adversary model

- **Primary adversary:** an untrusted client sending messages to a Synapse listener/proxy/API. Capabilities:
  craft SOAP/XML/JSON payloads (oversized, deeply nested, malformed), supply data that a trusted transform
  passes to external resolution, supply a `To`/routing value that mediation turns into an outbound
  destination (SSRF), or data that a script mishandles. (DOCTYPE-based payloads — XXE, billion-laughs — are
  rejected at the build boundary by default; §4.1.)
- **Secondary:** a malicious backend endpoint returning hostile responses into mediation.
- **Goals:** SSRF via routing or transform resolution; large-message/streaming DoS; bypass of a configured
  mediation-level auth; exfiltration of secrets reachable through a transform or log.
- **Out of model:** the integration developer/operator; the config (scripts, XSLT bodies, endpoint
  addresses); keystore/secret holders.

## §8 Security properties the project provides

1. **DOCTYPE-free inbound XML parsing.** Inbound SOAP and plain-XML messages that contain a Document Type
   Declaration are rejected at message-build time; XXE and DTD entity-expansion are therefore not reachable
   from the default inbound path *(source-verified — Axis2 `BuilderUtil` + Axiom
   `StAXParserConfiguration.SOAP`, versions pinned in §1)*. *Symptom of violation:* an entity defined via
   DOCTYPE in an inbound message is resolved/expanded on a default build path. *Severity:* critical.
2. **Robust message building/parsing.** Malformed inbound messages yield a fault, not memory corruption;
   the passthrough transport streams and defers parsing until a mediator needs the body *(source-verified —
   `RelayUtils`/`DeferredMessageBuilder`)*. **Resource bounding of parsed messages is NOT provided** — see
   §9. *Symptom:* crash or memory corruption (not mere resource exhaustion) from crafted input.
   *Severity:* high.
3. **Trusted-config-only transform code.** Stylesheets, XQuery bodies, and script bodies execute only from
   operator-loaded configuration, never from message content *(source-verified — mediator factories read
   these from config/registry only)*. *Symptom:* message content interpreted as stylesheet/query/script
   code. *Severity:* critical.
4. **Mediation-level security mechanisms.** When configured, transport security and WS-Security (Rampart)
   authenticate/sign/encrypt messages *(documented; enforcement is Rampart's — see the CXF/WSS4J sibling
   model for signature-wrapping classes)*. *Symptom:* accepted unauthenticated/forged message where policy
   required otherwise. *Severity:* critical.
5. **Secret protection.** Secure-vault keeps configured secrets encrypted at rest under a keystore-backed
   cipher, resolved to plaintext only in memory at use *(source-verified — `org.apache.synapse.securevault`)*.
   *Symptom:* plaintext secret in config at rest, or secret emitted to logs by the framework. *Severity:* high.
6. **Transport security support.** TLS on HTTPS listeners and outbound calls with cert validation when
   configured *(documented)*. *Symptom:* MITM where TLS expected. *Severity:* high.

## §9 Security properties the project does NOT provide

- **No security without configuration** — a proxy with no transport/WS-Security is only as protected as the
  integration wired it.
- **No built-in message-size / element-depth / entity-count limits** *(source-verified — none in the
  NHTTP/passthrough transports)*. Bounding oversized or deeply nested messages on parsing paths is a
  deployment responsibility (§5, §10). Resource-exhaustion reports against defaults are
  `BY-DESIGN: property-disclaimed`, not `VALID`, unless they show asymmetric amplification beyond input size.
- **No defence against the integration developer** — scripts, XSLT/XQuery bodies, and endpoint addresses are
  trusted config (§3).
- **No intrinsic SSRF protection for message-derived destinations** — this includes both explicit
  content-based routing *and* the designed-in implicit `To` routing of a bare `<send/>` in message-mediation
  mode *(source-verified — `SendMediator`)*. Validating/allow-listing destinations is the integration's job.
- **No secure-processing on XSLT/XQuery by default** — `FEATURE_SECURE_PROCESSING` is not set on the
  `TransformerFactory`; operators who pass message data into resolution-capable constructs must enable it
  per-mediator (§5a) or avoid the construct.

**False friends:**

- *An XSLT/XQuery transform looks like pure data transformation but can read files / fetch URLs* via
  `document()`/`doc()` or extension functions — reachable when the **trusted** stylesheet feeds
  message-derived strings into them (the message itself cannot introduce a DTD; §4.1/§4.2).
- *A script mediator looks sandboxed but runs with the engine's privileges* — it is operator code, not a
  security boundary for message data.
- *A bare `<send/>` looks like internal plumbing but is a client-steered router* — the destination is the
  inbound message's `To`.
- *Passthrough streaming looks like a DoS defence, but only for untouched bodies* — the moment a mediator
  reads the payload, the full message is built with no size bound.

**Well-known attack classes to keep in view:** SSRF via message-derived routing and via transform
resolution; oversized-message / deep-nesting DoS on parsing paths; injection into a downstream system via an
unsanitized transform; secret exposure through an over-broad transform or log mediator; XML
signature-wrapping where WS-Security is used (see the CXF/WSS4J model). (XXE / DTD entity-expansion is
default-blocked — §8.1 — and would signal a regression in the §1-pinned builder configuration.)

## §10 Downstream (integrator/operator) responsibilities

- **Cap request sizes and connection counts at the ingress** (LB/reverse proxy) for internet-facing
  listeners — Synapse does not bound message size (§9).
- **Validate or allow-list** any destination derived from message content, and avoid bare `<send/>` on
  untrusted traffic unless client-steered routing is intended and network-constrained (anti-SSRF).
- **Don't feed message-derived strings into `document()`/`doc()`/extension functions** in stylesheets and
  queries; where unavoidable, enable secure-processing on that mediator (§5a) and restrict egress.
- Configure transport TLS (with cert validation) and WS-Security where the integration requires
  authentication/integrity.
- Treat script/XSLT/XQuery mediator bodies as code you own; never accept them from untrusted sources.
- Protect the secure-vault keystore/password; don't commit plaintext secrets; keep secrets out of log
  mediators.
- **Do not downgrade the pinned Axis2/Axiom builder stack** or swap in a builder that permits DOCTYPE —
  §8.1 depends on it.

## §11 Known misuse patterns

- Exposing a proxy with no transport/message security and assuming the ESB "is secure".
- A bare `<send/>` (or content-derived endpoint) on an internet-facing service with unrestricted egress
  (SSRF).
- Stylesheets/queries that pass message data into `document()`/`doc()`/extension functions.
- Internet-facing listeners with no ingress size limits, where sequences build every message body.
- Embedding secrets in plaintext config instead of secure-vault; logging payloads that carry secrets.
- Routing untrusted message content into a script mediator that then executes/concatenates it unsafely.

## §11a Known non-findings (recurring false positives)

*(Seed list; the PMC owns the authoritative version)*

- **A script/XSLT/XQuery mediator "executes code"** — operator-authored config (§3/§8.3/§9).
- **"XXE in Synapse" reproduced only by re-enabling DOCTYPE support** (custom builder, modified Axiom/StAX
  config, or a non-pinned dependency) — `OUT-OF-MODEL: non-default-build`. Against the default build path
  (§8.1) it would be `VALID`.
- **SSRF via an endpoint address the operator configured statically** — trusted input (§6).
- **SSRF via bare `<send/>`/implicit `To` routing** — designed-in router semantics; disclaimed (§9) →
  `BY-DESIGN: property-disclaimed` (constraining it is §10). A report showing it bypasses an *explicitly
  configured* endpoint or allow-list would be `VALID`.
- **Resource exhaustion from oversized/deeply-nested messages within input-proportional bounds** —
  disclaimed (§9) → `BY-DESIGN: property-disclaimed`. Asymmetric amplification remains in-model under §8.2.
- **Findings in samples / documentation / tests** — out of scope (§3).
- **Use of a weak algorithm explicitly configured** in a WS-Security policy — integration choice.
- **`XMLInputFactory` without hardening in config/registry loaders** (`SynapseConfigUtils`,
  `SimpleURLRegistry`) — these parse operator-trusted configuration, not messages → `OUT-OF-MODEL:
  trusted-input`.

## §12 Conditions that would change this model

- Any change to the message-build parser configuration (Axis2/Axiom upgrade or replacement) that no longer
  rejects DOCTYPE by default — invalidates §8.1.
- Introduction of built-in message-size/depth limits — would move that row from §9 to §8.
- A new transport, mediator, or default that resolves untrusted references or derives destinations from
  messages.
- Setting (or deciding to set) `FEATURE_SECURE_PROCESSING` by default on transform mediators.
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
| `OUT-OF-MODEL: non-default-build` | Only when an insecure non-default parser/builder/resolution option was enabled. | §5a |
| `BY-DESIGN: property-disclaimed` | Concerns a §9-disclaimed property (size limits; implicit `To` routing; no security without config; scripts are operator code). | §9 |
| `KNOWN-NON-FINDING` | Matches a §11a entry. | §11a |
| `MODEL-GAP` | Routes to none of the above → revise the model. | §12 |


## §14 Machine-readable companion

Deferred; a `threat-model.yaml` can later encode the §6 trust table, §2/§3 scoping, §8 rows, §9 false
friends, §11a non-findings, and §13 dispositions.
