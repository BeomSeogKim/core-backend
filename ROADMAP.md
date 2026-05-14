# CS Interview Learning Roadmap

> Claude가 관리하는 면접 우선 학습 로드맵. `/cs-learn` 으로 진행.
> `[ ]` 미학습 · `[→]` 진행중 · `[x]` 완료

### 학습 방향

- **빅테크 빈출 우선**: 각 Phase의 ★ 등급 순으로 진행
- **Resume 연결 우선**: 🔗 표시 항목은 프로젝트 면접에서 직접 등장 — 해당 Phase 도달 시 최우선 처리
- **Phase 8-9 주의**: Kotlin Coroutines + Spring WebFlux는 resume 4개 프로젝트에 직접 연결 (byteplus, display-widget, display-performance, user-event-pipeline, moloco-ad) — 등급 대비 우선도 높음
- **Cross-domain**: Phase 1→6 완료 후 OS-JVM-Spring 연결 학습 진행

### 참조 소스 기준

공식 문서 및 저명한 기술 블로그만 인용: Java Language Spec · OpenJDK · spring.io · kotlinlang.org · Martin Fowler · Baeldung · Netflix/Kakao/LINE Tech Blog

---

## Phase 1 — Java Core ★★★★★

- [x] OOP 4대 원칙 + SOLID            → `languages/java/OOP-4가지-특징.md`
- [ ] equals & hashCode               → `languages/java/equals와-hashCode.md`
- [ ] Collections Framework           → `languages/java/Collections-Framework.md`
- [ ] HashMap 내부 구조               → `languages/java/HashMap-HashTable-ConcurrentHashMap.md`
- [ ] Generics                        → `languages/java/제네릭.md`
- [ ] Stream / Lambda                 → `languages/java/Stream-Lambda.md`
- [ ] Exception 처리                  → `languages/java/Checked-vs-Unchecked-Exception.md`
- [ ] Immutable Object                → `languages/java/Immutable-Object.md`
- [ ] Reflection 🔗                   → `languages/java/리플렉션.md`
- [ ] Dynamic Proxy 🔗                → `languages/java/다이나믹-프록시.md`
- [ ] Annotation                      → `languages/java/Annotation.md`
- [ ] Static keyword                  → `languages/java/Static-vs-Non-static.md`
- [ ] String / StringBuffer / StringBuilder → `languages/java/String-심화.md`

## Phase 2 — JVM & Runtime ★★★★★

- [ ] JVM 구조 (Heap/Stack/Method)    → `languages/java/JVM.md`
- [ ] GC 알고리즘 (G1, ZGC) 🔗       → `languages/java/GC.md`
- [ ] ClassLoader 🔗                  → `languages/java/ClassLoader.md`
- [ ] Java Memory Model               → `languages/java/concurrency/volatile과-메모리-가시성.md`

## Phase 3 — Java Concurrency ★★★★★

- [ ] Thread 기초 + Monitor           → `languages/java/concurrency/모니터.md`
- [ ] synchronized vs Lock            → `languages/java/concurrency/synchronized-vs-Lock.md`
- [ ] ThreadPool 🔗                   → `languages/java/concurrency/ThreadPool과-실행-전략.md`
- [ ] CompletableFuture               → `languages/java/CompletableFuture.md`

## Phase 4 — Spring ★★★★★

- [ ] IoC / DI                        → `frameworks/spring/core/IoC-DI.md`
- [ ] Bean 생명주기                   → `frameworks/spring/core/Bean.md`
- [ ] AOP + Proxy 단점 🔗             → `frameworks/spring/core/AOP.md`
- [ ] @Transactional (전파/격리) 🔗   → `frameworks/spring/core/Transactional.md`
- [ ] Spring MVC + DispatcherServlet  → `frameworks/spring/web/Spring-MVC.md`
- [ ] Spring Boot 동작원리            → `frameworks/spring/core/Spring-Boot-동작원리.md`
- [ ] JPA N+1 문제와 해결책 🔗        → `frameworks/spring/jpa/JPA-N+1.md`
- [ ] Filter vs Interceptor           → `frameworks/spring/web/Filter-vs-Interceptor.md`

## Phase 5 — Database ★★★★★

- [ ] Index (B-Tree 구조) 🔗          → `database/인덱스.md`
- [ ] Transaction + ACID              → `database/트랜잭션.md`
- [ ] MVCC 🔗                         → `database/MVCC.md`
- [ ] 정규화                          → `database/정규화.md`
- [ ] Connection Pool 🔗              → `database/Connection-Pool.md`
- [ ] 실행계획 + 쿼리 최적화 🔗       → `database/실행계획.md`

## Phase 6 — OS & Network ★★★★☆

- [ ] Process vs Thread               → `computer-science/os/프로세스,스레드.md`
- [ ] Context Switching               → `computer-science/os/컨텍스트-스위칭.md`
- [ ] Synchronization (뮤텍스/세마포어) → `computer-science/os/동기화.md`
- [ ] Deadlock                        → `computer-science/os/데드락.md`
- [ ] Virtual Memory + Paging         → `computer-science/os/가상-메모리.md`
- [ ] HTTP (1.1 / 2.0 / 3.0)         → `computer-science/network/HTTP.md`
- [ ] TCP / UDP                       → `computer-science/network/TCP-UDP.md`
- [ ] OSI 7 Layer                     → `computer-science/network/OSI-7-Layer.md`
- [ ] CORS                            → `computer-science/network/CORS.md`

## Phase 7 — Design Patterns ★★★☆☆

- [ ] Singleton                       → `architecture/design-patterns/싱글톤-패턴.md`
- [ ] Strategy + Template Method      → `architecture/design-patterns/전략-패턴.md`
- [ ] Proxy Pattern                   → `architecture/design-patterns/프록시-패턴.md`
- [ ] Factory Pattern                 → `architecture/design-patterns/팩토리-패턴.md`

## Phase 8 — Kotlin ★★★★☆

> resume 연결 높음 (byteplus-recommendation, display-widget, display-performance)

- [ ] 코루틴 기초 🔗                  → `languages/kotlin/코루틴-기초.md`
- [ ] Dispatcher + 구조적 동시성 🔗   → `languages/kotlin/코루틴-Dispatcher.md`
- [ ] Flow (Cold / Hot Stream)        → `languages/kotlin/Flow.md`

## Phase 9 — Modern Java & Reactive ★★★★☆

> resume 연결 높음 (user-event-pipeline, moloco-ad, display-performance). 2025-2026 빅테크 빈출 상승 중.

- [ ] Virtual Thread (Java 21)        → `languages/java/Virtual-Thread.md`
- [ ] Spring WebFlux + Reactor 🔗     → `frameworks/spring/web/WebFlux-Coroutine-통합.md`
