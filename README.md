# Ieum-BE

Backend repository in Ieum

## 기술 스택

| 항목 | 버전 |
| --- | --- |
| Java | 21 (Gradle toolchain으로 고정) |
| Spring Boot | 4.1.1 |
| Gradle | 9.7.1 (wrapper 포함, 별도 설치 불필요) |
| Database | PostgreSQL + Flyway |
| 기타 | Spring Security, Spring Data JPA, Validation, Lombok |

---

## 최초 셋업

클론 직후에는 바로 실행되지 않습니다. **아래 3단계를 한 번만** 해주세요.

### 사전 준비

- **JDK 21** — Gradle toolchain이 Java 21을 요구합니다
- **PostgreSQL** — 로컬에 직접 설치 (Docker는 현재 사용하지 않습니다)

### 1단계. 데이터베이스 2개 생성

개발용 `ieum` 과 테스트용 `ieum_test` 를 만듭니다. 테스트가 데이터를 지우고 다시 만들기 때문에 **분리가 필수**입니다.

**Windows**

```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -d postgres -c "CREATE DATABASE ieum;" -c "CREATE DATABASE ieum_test;"
```

**macOS / Linux**

```bash
psql -U postgres -d postgres -c "CREATE DATABASE ieum;" -c "CREATE DATABASE ieum_test;"
```

`-d postgres` 는 생략하면 안 됩니다. `CREATE DATABASE` 를 실행하려면 이미 존재하는 다른 DB에 먼저 접속해야 합니다.

### 2단계. 환경변수 설정

DB 계정 정보는 **저장소에 커밋하지 않습니다.** 각자 로컬 환경변수로 넣어주세요.

| 변수 | 값 |
| --- | --- |
| `DB_USERNAME` | PostgreSQL 계정 (기본 설치면 `postgres`) |
| `DB_PASSWORD` | 그 계정의 비밀번호 |

**Windows** — 특수문자가 있는 비밀번호도 안전하게 저장됩니다 (`setx` 는 1024자 제한과 특수문자 문제가 있어 권하지 않습니다):

```powershell
[Environment]::SetEnvironmentVariable('DB_USERNAME','postgres','User')
[Environment]::SetEnvironmentVariable('DB_PASSWORD','<비밀번호>','User')
```

GUI를 선호하면 `Win + R` → `sysdm.cpl` → **고급** → **환경 변수** → **사용자 변수** → **새로 만들기**

**macOS / Linux** — `~/.zshrc` 또는 `~/.bashrc` 에 추가:

```bash
export DB_USERNAME=postgres
export DB_PASSWORD=<비밀번호>
```

> **설정 후 IDE와 터미널을 반드시 재시작하세요.** 이미 실행 중인 프로세스는 시작 시점의 환경을 들고 있어 새 값을 읽지 못합니다. 이것 때문에 "설정했는데 안 된다"가 가장 흔하게 발생합니다.

### 3단계. 실행 확인

```bash
./gradlew bootRun        # Windows: .\gradlew.bat bootRun
```

로그에 아래 두 줄이 보이면 성공입니다.

```
No active profile set, falling back to 1 default profile: "dev"
Started IeumApplication in N seconds
```

---

## 실행 / 테스트

| 작업 | 명령 |
| --- | --- |
| 앱 실행 (dev) | `./gradlew bootRun` |
| 전체 테스트 | `./gradlew test` |
| 특정 테스트 클래스 | `./gradlew test --tests 'com.ieum.IeumApplicationTests'` |
| 특정 테스트 메서드 | `./gradlew test --tests 'com.ieum.IeumApplicationTests.contextLoads'` |
| 빌드 (컴파일+테스트+jar) | `./gradlew build` |
| 실행 가능한 jar | `./gradlew bootJar` |

테스트 리포트: `build/reports/tests/test/index.html`

린터·포매터는 설정하지 않았습니다. `check` 는 `test` 와 같습니다.

---

## 설정 구조

처음 보면 의아할 수 있는 선택이 몇 가지 있어 이유를 함께 적습니다.

### 공통 `application.yaml` 이 없습니다

설정 파일은 **딱 두 개**입니다.

```
src/main/resources/
  application-dev.yaml    개발 환경
  application-prod.yaml   운영 환경
```

환경별 설정을 한 파일만 보면 전부 파악할 수 있게 하려고 공통 파일을 두지 않았습니다. 대신 **앱 이름·JPA·Flyway 같은 공통 설정이 양쪽에 중복**되어 있으니, 공통 설정을 바꿀 때는 두 파일을 모두 고쳐야 합니다.

### 기본 프로필은 `IeumApplication.main` 에 있습니다

공통 `application.yaml` 이 없으면 `spring.profiles.default` 를 적을 곳도 없습니다. 그래서 `main()` 에서 지정합니다.

```java
application.setDefaultProperties(Map.of("spring.profiles.default", "dev"));
```

Gradle이 아니라 코드에 둔 이유는 **모든 실행 경로를 커버**하기 위해서입니다. `bootRun`, `java -jar`, IntelliJ에서 클래스 직접 실행까지 전부 dev로 떨어지므로 IDE Run Configuration을 따로 손댈 필요가 없습니다. 우선순위가 가장 낮은 기본 속성이라 `SPRING_PROFILES_ACTIVE=prod` 가 항상 이깁니다.

### 테스트 DB는 yml에 없습니다

`build.gradle` 의 `test` 태스크가 지정합니다.

```gradle
tasks.named('test') {
	// 테스트는 main() 을 거치지 않아 기본 프로필이 적용되지 않으므로 여기서 직접 지정한다.
	systemProperty 'spring.profiles.active', 'dev'
	// dev 와 같은 설정을 쓰되 데이터베이스만 분리한다.
	systemProperty 'spring.datasource.url', 'jdbc:postgresql://localhost:5432/ieum_test'
}
```

테스트는 `@SpringBootTest` 가 컨텍스트를 직접 만들기 때문에 `main()` 을 거치지 않습니다. 그래서 프로필을 여기서 다시 지정합니다. 테스트 DB를 바꾸려면 yml이 아니라 **이 파일**을 고쳐야 합니다.

### 스키마의 주인은 Flyway

`ddl-auto` 는 양쪽 프로필 모두 `validate` 입니다. Hibernate가 테이블을 만들지 않습니다.

스키마 변경은 `src/main/resources/db/migration/` 에 버전 파일로 추가하세요 (`V1__create_users.sql`, `V2__...`). 엔티티만 만들고 마이그레이션을 빼먹으면 **기동이 실패합니다** — 의도된 동작입니다.

### Testcontainers는 들어 있지만 현재 미사용

`TestcontainersConfiguration.java` 와 `TestIeumApplication.java`, testcontainers 의존성이 남아 있습니다. 나중에 Docker 기반으로 전환할 여지를 남겨둔 것이고, 평소 테스트는 이것들을 거치지 않습니다.

Docker로 돌리고 싶으면 `IeumApplicationTests` 에 `@Import(TestcontainersConfiguration.class)` 를 붙이거나 `./gradlew bootTestRun` 을 쓰면 됩니다. `@ServiceConnection` 이 `spring.datasource.*` 보다 우선하므로 dev 프로필이 켜져 있어도 컨테이너 DB로 붙습니다. (Docker 데몬이 필요합니다.)

---

## 트러블슈팅

실제로 겪은 것들입니다.

### `password authentication failed for user "${DB_USERNAME}"`

**환경변수가 설정되지 않았습니다.** 비밀번호가 틀린 게 아닙니다.

Spring Boot의 `@ConfigurationProperties` 바인딩은 해결하지 못한 플레이스홀더를 예외 없이 **문자열 그대로** 넘깁니다. 그래서 `${DB_USERNAME}` 이라는 이름의 계정으로 접속을 시도합니다. 오류 메시지에 `${...}` 가 보이면 환경변수 문제입니다.

설정했는데도 같은 오류가 나면 **IDE·터미널을 재시작**하세요.

### `Failed to configure a DataSource ... (no profiles are currently active)`

프로필이 활성화되지 않아 설정 파일을 하나도 읽지 못한 상태입니다. 로그 앞머리에 `[ieum]` 이 없다면 확실합니다.

현재는 `main()` 의 기본 프로필이 막아주지만, 테스트 설정을 건드렸거나 `SPRING_PROFILES_ACTIVE` 를 빈 값으로 덮어쓴 경우 발생할 수 있습니다.

### `Web server failed to start. Port 8080 was already in use.`

8080을 쓰는 다른 앱이 있습니다. **자기가 띄운 앱이 이미 돌고 있는 경우가 많습니다** — IDE의 Run/Services 탭을 확인하세요.

점유 프로세스 확인:

```powershell
# Windows
Get-NetTCPConnection -LocalPort 8080 -State Listen | Select-Object OwningProcess
```

```bash
# macOS / Linux
lsof -i :8080
```

다른 포트로 띄우려면:

```bash
./gradlew bootRun --args='--server.port=8099'
```

### 모든 요청이 401 Unauthorized

**정상 동작입니다.** `spring-boot-starter-security` 가 클래스패스에 있고 `SecurityFilterChain` 빈이 아직 없어서, Spring Boot 기본값이 모든 경로를 HTTP Basic 인증으로 잠급니다.

보안 필터가 컨트롤러보다 앞단에서 가로채므로 **존재하지 않는 주소도 404가 아니라 401** 이 돌아옵니다. 임시로 통과하려면 아이디 `user` 와 기동 로그의 비밀번호를 쓰세요 (실행할 때마 새로 생성됩니다).

```
Using generated security password: 0b0e55db-d744-424c-93aa-e0b7f747d1f6
```

인증을 통과하면 404가 보일 겁니다 — 아직 컨트롤러가 없기 때문입니다. 경로별 허용 규칙은 `SecurityFilterChain` 빈을 추가해 정의하면 됩니다.

---

## 배포 (prod)

`SPRING_PROFILES_ACTIVE=prod` 와 함께 아래 환경변수를 주입합니다.

| 변수 | 필수 | 설명 |
| --- | --- | --- |
| `DB_URL` | 필수 | JDBC URL |
| `DB_USERNAME` | 필수 | DB 계정 |
| `DB_PASSWORD` | 필수 | DB 비밀번호 |
| `DB_POOL_SIZE` | 선택 | Hikari 최대 커넥션 (기본 10) |
| `SERVER_PORT` | 선택 | 서버 포트 (기본 8080) |

```bash
./gradlew bootJar
SPRING_PROFILES_ACTIVE=prod \
DB_URL=jdbc:postgresql://<host>:5432/<db> \
DB_USERNAME=<user> \
DB_PASSWORD=<password> \
java -jar build/libs/ieum-0.0.1-SNAPSHOT.jar
```

prod 프로필은 SQL 로깅을 끄고, Flyway `clean` 을 영구 차단하며, graceful shutdown을 사용합니다.
