---
tags: [java, optional, null-safety, functional]
status: completed
created: 2026-04-14
---

# Optional

## 핵심 개념

**Optional\<T>**는 `null`이 될 수 있는 값을 명시적으로 감싸는 컨테이너 클래스다 (Java 8+). `NullPointerException`을 방지하고, "이 값은 없을 수 있다"는 의도를 API에 명확히 드러낸다.

## 동작 원리

### Optional이 해결하는 문제

```java
// Optional 없이 — null 체크가 곳곳에 흩어짐
User user = userRepo.findById(1L);
if (user != null) {
    Address address = user.getAddress();
    if (address != null) {
        String city = address.getCity();
        if (city != null) {
            // 드디어 사용...
        }
    }
}

// Optional로 — 체이닝으로 깔끔하게
String city = userRepo.findById(1L)
    .map(User::getAddress)
    .map(Address::getCity)
    .orElse("Unknown");
```

### Optional 생성

<div style="font-family: -apple-system, sans-serif; max-width: 520px; margin: 12px auto; font-size: 12px;">
<div style="display: flex; gap: 4px; flex-wrap: wrap;">
<div style="flex: 1; min-width: 150px; background: #27AE60; color: white; padding: 10px; border-radius: 6px;">
<div style="font-weight: bold;">of(T value)</div>
<div style="font-size: 11px; margin-top: 6px;">값이 <b>확실히 not null</b></div>
<div style="font-size: 11px;">null 넘기면 NPE 발생</div>
</div>
<div style="flex: 1; min-width: 150px; background: #3498DB; color: white; padding: 10px; border-radius: 6px;">
<div style="font-weight: bold;">ofNullable(T value)</div>
<div style="font-size: 11px; margin-top: 6px;">값이 <b>null일 수 있음</b></div>
<div style="font-size: 11px;">null이면 Optional.empty()</div>
</div>
<div style="flex: 1; min-width: 150px; background: #8E44AD; color: white; padding: 10px; border-radius: 6px;">
<div style="font-weight: bold;">empty()</div>
<div style="font-size: 11px; margin-top: 6px;"><b>비어있는</b> Optional</div>
<div style="font-size: 11px;">값 없음을 명시</div>
</div>
</div>
</div>

```java
Optional<String> opt1 = Optional.of("hello");      // 값 확실할 때
Optional<String> opt2 = Optional.ofNullable(null);  // null 가능할 때 → empty
Optional<String> opt3 = Optional.empty();           // 빈 Optional
```

### 값 꺼내기 — orElse vs orElseGet

```
                    값이 있으면?            값이 없으면?
get()               값 반환                NoSuchElementException (위험)
orElse(T)           값 반환                기본값 반환
orElseGet(Supplier) 값 반환                Supplier 실행 후 반환
orElseThrow()       값 반환                NoSuchElementException
orElseThrow(Ex)     값 반환                커스텀 예외 발생
```

> [!warning] orElse vs orElseGet 성능 차이
> `orElse()`는 값이 있든 없든 **항상** 인자 표현식을 실행한다. 비용이 큰 객체 생성이면 `orElseGet()`을 써야 한다.

```java
// orElse — findDefault()가 항상 실행됨 (값 있어도!)
user.orElse(findDefault());

// orElseGet — 값이 없을 때만 findDefault() 실행
user.orElseGet(() -> findDefault());
```

```
orElse(new Heavy()):
  값 있음: new Heavy() 실행됨 (버려짐)  ← 낭비!
  값 없음: new Heavy() 실행됨 (사용됨)

orElseGet(() -> new Heavy()):
  값 있음: Lambda 실행 안 됨            ← 효율적!
  값 없음: Lambda 실행됨 (사용됨)
```

### 주요 연산

```java
Optional<User> userOpt = findUser(1L);

// map — 변환 (값 없으면 empty 전파)
Optional<String> name = userOpt.map(User::getName);

// flatMap — Optional을 반환하는 함수에 사용 (중첩 방지)
Optional<String> city = userOpt
    .flatMap(User::getAddress)    // getAddress()가 Optional<Address> 반환
    .map(Address::getCity);

// filter — 조건 만족하면 유지, 아니면 empty
Optional<User> adult = userOpt.filter(u -> u.getAge() >= 18);

// ifPresent — 값이 있을 때만 실행
userOpt.ifPresent(u -> sendEmail(u));

// ifPresentOrElse (Java 9+) — 있을 때 / 없을 때 분기
userOpt.ifPresentOrElse(
    u -> sendEmail(u),
    () -> log.warn("User not found")
);
```

### Optional 안티패턴

```java
// ✗ 필드에 Optional 사용 — 직렬화 불가, 메모리 낭비
class User {
    Optional<String> nickname;  // 안티패턴
}

// ✗ 메서드 파라미터에 Optional — 불필요한 래핑
void process(Optional<String> name) { ... }  // 안티패턴

// ✗ get() 남용 — NPE를 Optional로 바꾼 것뿐
user.get();  // NoSuchElementException 위험

// ✓ Optional은 메서드 반환 타입에만 사용하는 것이 Best Practice
Optional<User> findById(Long id) { ... }
```

## 코드 예시

```java
// 실무 패턴 — Repository에서 Optional 반환
public Optional<User> findById(Long id) {
    User user = em.find(User.class, id);
    return Optional.ofNullable(user);
}

// Controller에서 처리
User user = userService.findById(id)
    .orElseThrow(() -> new NotFoundException("User not found: " + id));

// Stream + Optional 조합
List<String> emails = users.stream()
    .map(User::getEmail)
    .flatMap(Optional::stream)  // Java 9+ Optional → Stream 변환
    .collect(toList());
```

## 관련 문서

- [[Stream-Lambda]] — Optional과 Stream의 map/flatMap 연계
- [[Checked-vs-Unchecked-Exception]] — orElseThrow와 예외 처리
- [[제네릭]] — Optional\<T>의 제네릭 활용
