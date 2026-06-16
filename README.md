# Spring MVC Static Analyzer 1

Spring MVC Static Analyzer 1 is a Java CLI tool that statically analyzes legacy Spring MVC projects and writes a readable text report showing call flows from Controller methods to Service and Repository/DAO/Mapper methods.

## Requirements

- Java 17
- Gradle

## Usage

```bash
./gradlew run --args="/path/to/legacy-project -o result.txt"
```

## Supported MVP Scope

- Java 8 or later source code
- Annotation-based Spring MVC
- `@Controller`
- `@RestController`
- `@Service`
- `@Repository`
- `@Autowired` field injection
- Constructor injection
- Public Controller methods as entry points

## Unsupported MVP Scope

- Struts
- XML bean definitions
- AOP behavior
- Reflection
- Dynamic proxies
- WebFlux
- Runtime analysis
- MyBatis Mapper XML and SQL tracing

Unsupported or unresolved calls are printed explicitly in the report.

## Output Example

```text
==================================================
UserController.createUser()
==================================================

UserController.createUser()
├─ UserService.validate()
│  └─ UserRepository.findByEmail()
└─ UserService.createUser()
   ├─ UserRepository.save()
   └─ unsupported: externalClient.send()
```

## Error Handling

The analyzer keeps useful partial results where possible. Broken Java files are recorded in the `Warnings` section instead of stopping the whole analysis.

Fatal input errors return exit code `1`. Fatal analysis errors return exit code `2`.
