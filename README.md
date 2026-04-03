# adempiere-shw-zk

ZK UI for ADempiere with Systemhaus Westfalia (SHW) customizations.

**The central purpose of this repository is to ensure that SHW customizations take precedence
over the original ADempiere classes at runtime.** This applies to two levels:

- **ZK UI layer** — Java files placed in `zkwebui/WEB-INF/src/` replace the corresponding
  classes from the official ADempiere ZK UI. These are compiled directly into `WEB-INF/classes/`.
- **Base/model layer** — Customized ADempiere core classes (e.g., `MOrder`) come from
  `lsv-general` via `adempiere-shw`. The build extracts them into `WEB-INF/classes/` as well.

In both cases, `WEB-INF/classes/` always loads before `WEB-INF/lib/` (Java EE specification),
so SHW classes win over the ADempiere originals regardless of JAR naming or ordering.

The final output is a Docker image published to Docker Hub.

---

## Position in the Stack

```
┌──────────────────────────────────────────────────────────────────────┐
│                        adempiere-ui-gateway                          │
│  (docker-compose orchestration — nginx, Keycloak, Kafka, OpenSearch) │
│                                                                      │
│   ┌──────────────────────┐      ┌──────────────────────────────┐     │
│   │   adempiere-shw-zk   │      │  adempiere-grpc-server (SHW) │     │
│   │  ZK UI (this repo)   │      │  Backend for Vue UI          │     │
│   │  Jetty 10 / Java 17  │      └──────────────────────────────┘     │
│   └──────────┬───────────┘                                           │
│              │ JDBC                                                  │
│   ┌──────────▼───────────┐                                           │
│   │      PostgreSQL      │                                           │
│   └──────────────────────┘                                           │
└──────────────────────────────────────────────────────────────────────┘
```

### Dependency Chain

```
adempiere/zk-ui (official ADempiere ZK release)
    │  downloaded during build (refreshDependences task)
    │
    ▼
adempiere-shw  (Systemhaus Westfalia customization library)
    │
    ├── shw_libs/build.gradle declares:
    │     ├── org.adempiere:base          ← ADempiere core classes
    │     ├── com.shw:lsv-general         ← SHW customized ADempiere classes
    │     └── ... (all other dependencies)
    │
    │  publishes: com.shw:adempiere-shw.shw_libs:<version>
    ▼
adempiere-shw-zk  (this repository)
    │
    │  build.gradle pulls in adempiere-shw.shw_libs as single dependency,
    │  which transitively brings in all ADempiere + SHW classes.
    │  ZK-layer patches (12 Java files) are compiled directly here.
    │
    │  releaseZK task produces: adempiere-shw-zk.war
    ▼
Docker image: marcalwestf/adempiere-shw-zk:jetty-<version>
    │
    ▼
adempiere-ui-gateway  (deployment)
    ADEMPIERE_ZK_IMAGE in env_template.env points to this image
```

---

## How Customization Classes Take Precedence

A key challenge: both `lsv-general-<version>.jar` (SHW customizations) and
`base-<version>.jar` (ADempiere originals) **can** define classes with the same fully qualified
names (e.g., `org.compiere.model.MOrder`).  
Both JARs end up in `WEB-INF/lib/` of the WAR.

Jetty loads `WEB-INF/lib/` JARs in alphabetical order — `base-` loads before `lsv-general-`,
so without intervention the ADempiere originals would win.

**The fix** (in `build.gradle`):  
- the `into('.patch')` block extracts all classes from`lsv-general` into a staging directory `.patch/`.  
- The `patchZK` task then moves them into `WEB-INF/classes/`.  
- Per the Java EE specification, **`WEB-INF/classes/` is always loaded before `WEB-INF/lib/`**, regardless of alphabetical order.  
- **The SHW customizations win**.

```
Class loading order at runtime (Jetty):
  1. WEB-INF/classes/  ← lsv-general classes extracted here  ✓ wins
  2. WEB-INF/lib/*.jar ← base- and lsv-general JARs ignored for already-loaded classes
```

---

## Repository Structure

```
adempiere-shw-zk/
├── build.gradle                    Main build file — versions, tasks, patch mechanism
├── .github/workflows/publish.yml   CI/CD: builds WAR and publishes Docker image on release
├── client/src/main/java/
│   └── org/adempiere/version.properties  (populated during build with release metadata)
├── docker/
│   ├── AdempiereTemplate.properties     ADempiere configuration template
│   └── jetty/
│       ├── Dockerfile                   CI/CD image build (requires full build environment)
│       ├── Dockerfile.local_test        Local testing only — see Local Testing section
│       └── settings/
│           ├── adempiere.sh             Jetty startup script
│           └── jetty-ds.xml             JNDI datasource configuration
├── lib/                            External JARs not available via Maven
└── zkwebui/WEB-INF/src/            ZK UI patches (Java source, compiled into WAR)
```

---

## ZK UI Patches

This section lists the **actual SHW customizations at the ZK layer** — Java files that
replace the corresponding originals from the official ADempiere ZK UI.  
They are compiled directly into `WEB-INF/classes/`, which guarantees they load before the originals in
`WEB-INF/lib/`.  
Adding a file here is the correct way to customize any ZK UI class.

Current customizations:

| Class | Area |
|-------|------|
| `org/adempiere/webui/AdempiereWebUI.java` | Core UI initialization |
| `org/adempiere/webui/panel/AbstractADWindowPanel.java` | Window panel base |
| `org/adempiere/webui/panel/LoginPanel.java` | Login screen |
| `org/adempiere/webui/panel/InfoProductPanel.java` | Product info panel |
| `org/adempiere/webui/apps/ProcessPanel.java` | Process execution panel |
| `org/adempiere/webui/apps/form/WMerge.java` | Merge form |
| `org/adempiere/webui/session/SessionContextListener.java` | Session lifecycle |
| `org/adempiere/webui/session/SessionManager.java` | Session management |
| `org/adempiere/webui/grid/WBPartner.java` | Business partner grid |
| `org/eevolution/form/WBrowser.java` | Smart browser form |
| `org/eevolution/form/WHRActionNotice.java` | HR action notice form |
| `org/eevolution/grid/WBrowserListItemRenderer.java` | Browser list renderer |

### Adding a new ZK UI patch

Place the Java source file under `zkwebui/WEB-INF/src/` preserving its original package
structure, then commit and create a release. Example:

```
Patch for org.adempiere.webui.panel.AbstractADWindowPanel
→ zkwebui/WEB-INF/src/org/adempiere/webui/panel/AbstractADWindowPanel.java
```

### Adding a patch to a base ADempiere class

Base class patches (e.g., `org.compiere.model.MOrder`) belong in
[adempiere-shw](https://github.com/Systemhaus-Westfalia/adempiere-shw), not here.
The workflow is:

1. Add the patch to `adempiere-shw` and create a release there
2. Update `adempiereSHWRelease` in `build.gradle` of this project to the new version
3. Commit and create a release here

---

## Build System

### Version Variables (build.gradle)

| Variable | Current Value | Description |
|----------|---------------|-------------|
| `adempiereZKRelease` | `1.2.2` | Official ADempiere ZK UI release to base on |
| `adempiereSHWRelease` | `3.9.4.001-1.1.55` | SHW customization library version |

### Build Tasks

| Task | Description |
|------|-------------|
| `refreshDependences` | Downloads the official `zk-ui.war` from ADempiere GitHub releases |
| `build` | Compiles ZK patches and assembles the WAR |
| `patchZK` | Merges lsv-general classes into `WEB-INF/classes/` for correct precedence |
| `releaseZK` | Produces final `build/release/adempiere-shw-zk.war` |

### Maven Repositories

All repositories require GitHub credentials (`deployUsername` / `deployToken`):

- `https://maven.pkg.github.com/Systemhaus-Westfalia/adempiere-shw` — SHW packages
- `https://maven.pkg.github.com/erpya/Repository` — ERPyA packages
- `https://maven.pkg.github.com/adempiere/adempiere` — ADempiere packages

Credentials can be provided via `gradle.properties` or system properties:
```
deploy.user=<github-username>
deploy.token=<github-personal-access-token>
```

### Prerequisites

- Java 17 (Temurin or Zulu recommended)
- Gradle 8.x (no Gradle wrapper in this repo — use system Gradle or sdkman)
- GitHub personal access token with `read:packages` scope

---

## CI/CD Pipeline

Triggered automatically when a new GitHub Release is created.

```
Trigger: GitHub Release created
    │
    ▼
build-app job
    ├── Java 17 (Temurin)
    ├── Injects release metadata into version.properties
    └── gradle clean refreshDependences releaseZK
        └── produces: build/release/adempiere-shw-zk.war
    │
    ▼
build-publish-docker job
    ├── Downloads WAR artifact
    ├── Extracts WEB-INF/lib/*.jar → docker/jetty/zk-ui/lib/
    ├── docker build using docker/jetty/Dockerfile
    │   (multi-platform: linux/amd64, amd64/v2, arm64/v8)
    └── Pushes to Docker Hub:
        ├── marcalwestf/adempiere-shw-zk:jetty-<tag>
        └── marcalwestf/adempiere-shw-zk:jetty  (latest)
```

**Required GitHub Secrets:**

| Secret | Description |
|--------|-------------|
| `DEPLOY_USER` | GitHub username for Maven package access |
| `DEPLOY_TOKEN` | GitHub personal access token |
| `DOCKER_USERNAME` | Docker Hub username |
| `DOCKER_TOKEN` | Docker Hub access token |
| `DOCKER_REPO_ADEMPIERE_ZK` | Docker Hub repository (e.g., `marcalwestf/adempiere-shw-zk`) |

---

## Local Testing

Use `docker/jetty/Dockerfile.local_test` to build a test image locally without needing
the full CI/CD environment.  
It takes the current production image as base and swaps only the WAR, inheriting all lib/ext JARs.

> **Note:** `Dockerfile.local_test` is never used by CI/CD.  
> Docker ignores files with non-standard names unless explicitly passed with `-f`.  
> CI/CD uses `docker/jetty/Dockerfile`.

### Prerequisites

- `adempiere-shw` published to `mavenLocal()` at the expected version:
  ```bash
  # In adempiere-shw repository (requires Java 11):
  export ADEMPIERE_LIBRARY_VERSION=<adempiereSHWRelease value>
  gradle :adempiere-shw.shw_libs:publishToMavenLocal
  ```

### Build and deploy a test image

```bash
# Step 1 — build the WAR (Java 17, run from adempiere-shw-zk root)
gradle clean build && gradle releaseZK

# Step 2 — build the test image
#   Base image version is read from env_template.env — no hardcoded version needed
BASE=$(grep ADEMPIERE_ZK_IMAGE \
  <path-to-adempiere-ui-gateway>/docker-compose/env_template.env \
  | cut -d'"' -f2)
cp build/release/adempiere-shw-zk.war docker/jetty/zk-ui/zk-ui.war
docker build -f docker/jetty/Dockerfile.local_test \
  --build-arg BASE_IMAGE=${BASE} \
  -t ${BASE}_SHW .

# Step 3 — deploy
#   Set ADEMPIERE_ZK_IMAGE="${BASE}_SHW" in env_template.env, then:
cd <path-to-adempiere-ui-gateway>/docker-compose
bash stop-all.sh && bash start-all.sh
```

### Verify the correct classes are loaded

```bash
# MOrder.class must appear under WEB-INF/classes/ (not only inside a JAR)
docker exec adempiere-ui-gateway.zk \
  find / -name 'MOrder.class' 2>/dev/null

# Confirm it is the SHW version (contains isSplit references absent from ADempiere original)
docker exec adempiere-ui-gateway.zk \
  strings <path-from-above>/MOrder.class | grep -i isSplit
# Expected output: issplit  isSplitInvoice  isSplitWhenDifference
```

### Cleanup after testing

```bash
# Remove test image
docker rmi ${BASE}_SHW

# Revert env_template.env (remove _SHW suffix) and restart
cd <path-to-adempiere-ui-gateway>/docker-compose
bash stop-all.sh && bash start-all.sh

# Remove locally published shw_libs from mavenLocal
rm -rf ~/.m2/repository/com/shw/adempiere-shw.shw_libs/<version>/
```

---

## Integration with adempiere-ui-gateway

The ZK image is referenced in `env_template.env`:

```env
ADEMPIERE_ZK_IMAGE="marcalwestf/adempiere-shw-zk:jetty-<version>"
```

**Update procedure after a new release:**
1. Update `ADEMPIERE_ZK_IMAGE` in `env_template.env` to the new tag
2. Run `bash stop-all.sh && bash start-all.sh`

The ZK container connects to PostgreSQL via JDBC (configured by environment variables) and
shares a persistent files volume with other services for document storage.

---

## Runtime Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `ADEMPIERE_DB_TYPE` | `PostgreSQL` | Database type |
| `ADEMPIERE_DB_SERVER` | `localhost` | Database host |
| `ADEMPIERE_DB_PORT` | `5432` | Database port |
| `ADEMPIERE_DB_NAME` | `adempiere` | Database name |
| `ADEMPIERE_DB_USER` | `adempiere` | Database user |
| `ADEMPIERE_DB_PASSWORD` | `adempiere` | Database password |
| `ADEMPIERE_JAVA_OPTIONS` | `-Xms128m -Xmx1024m` | JVM options |
| `ADEMPIERE_HOME` | `/opt/Adempiere` | ADempiere installation path |

---

## Related Repositories

| Repository | Description |
|------------|-------------|
| [adempiere-shw](https://github.com/Systemhaus-Westfalia/adempiere-shw) | SHW customization library — patches to ADempiere core classes and dependency aggregation |
| [adempiere-ui-gateway](https://github.com/Systemhaus-Westfalia/adempiere-ui-gateway_SHW) | Docker Compose stack — orchestrates all services including this ZK image |
| [adempiere/zk-ui](https://github.com/adempiere/zk-ui) | Official ADempiere ZK UI — base for this project |
