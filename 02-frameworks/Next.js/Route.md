---
tags: [nextjs, routing]
status: completed
created: 2026-02-21
---

# Next.js 라우팅 시스템

## 핵심 개념

Next.js는 파일 시스템 기반 라우팅을 제공하며, **Pages Router**와 **App Router** 두 가지 방식을 지원한다.

| 라우터 | 디렉토리 | API 정의 방식 | 도입 버전 |
|--------|----------|--------------|-----------|
| Pages Router | `pages/` | API Routes | Next.js 9+ |
| App Router | `app/` | Route Handlers | Next.js 13+ |

> [!tip]
> Next.js 13 이후로는 **App Router**가 권장된다. Web API 표준 기반의 Request/Response 객체를 사용하고, HTTP 메서드를 함수명으로 분리하여 더 명확한 코드 구조를 제공한다.

## 동작 원리

### Pages Router - API Routes (Legacy)

디렉토리 구조가 곧 API 경로가 된다. 하나의 파일에서 `req.method`로 HTTP 메서드를 분기 처리한다.

```text
pages/
  ├── api/
  │   ├── users/
  │   │   ├── index.ts      → GET/POST /api/users
  │   │   └── [id].ts       → GET/PUT/DELETE /api/users/:id
  │   └── auth/
  │       └── login.ts      → POST /api/auth/login
```

### App Router - Route Handlers (권장)

`route.ts`라는 고정 파일명을 사용하며, HTTP 메서드별로 **Named Export 함수**를 분리한다.

```text
app/
  ├── api/
  │   ├── users/
  │   │   ├── route.ts      → GET/POST /api/users
  │   │   └── [id]/
  │   │       └── route.ts  → GET/PUT/DELETE /api/users/:id
```

> [!note]
> App Router에서는 **NextRequest/NextResponse**가 Web API 표준(`Request`, `Response`)을 확장한 객체이다. Pages Router의 `NextApiRequest/NextApiResponse`는 Node.js의 `IncomingMessage/ServerResponse`를 확장한 것이라 근본적으로 다르다.

### 주요 차이점

```
  ┌───────────────┬───────────────────────────┬─────────────────────────────┐
  │     항목      │ Pages Router (API Routes) │ App Router (Route Handlers) │
  ├───────────────┼───────────────────────────┼─────────────────────────────┤
  │ 파일명        │ 자유 (users.ts)           │ 고정 (route.ts)             │
  ├───────────────┼───────────────────────────┼─────────────────────────────┤
  │ Request 객체  │ NextApiRequest            │ NextRequest (Web API 기반)  │
  ├───────────────┼───────────────────────────┼─────────────────────────────┤
  │ Response 객체 │ NextApiResponse           │ NextResponse (Web API 기반) │
  ├───────────────┼───────────────────────────┼─────────────────────────────┤
  │ HTTP 메서드   │ req.method로 분기         │ 함수명으로 분리 (GET, POST) │
  └───────────────┴───────────────────────────┴─────────────────────────────┘
```

## 코드 예시

### Pages Router (Legacy)

```typescript
// pages/api/users/index.ts
import type { NextApiRequest, NextApiResponse } from 'next'

export default function handler(req: NextApiRequest, res: NextApiResponse) {
  if (req.method === 'GET') {
    res.status(200).json({ users: [] })
  } else if (req.method === 'POST') {
    res.status(201).json({ id: 1, ...req.body })
  }
}
```

### App Router (권장)

```typescript
// app/api/users/route.ts
import { NextRequest, NextResponse } from 'next/server'

export async function GET(request: NextRequest) {
  return NextResponse.json({ users: [] })
}

export async function POST(request: NextRequest) {
  const body = await request.json()
  return NextResponse.json({ id: 1, ...body }, { status: 201 })
}
```

> [!warning]
> App Router에서 `route.ts`와 `page.tsx`는 같은 디렉토리에 공존할 수 없다. API 엔드포인트는 반드시 별도 경로로 분리해야 한다.

## 관련 문서
- [[dev/06-computer-science/network/CORS|CORS]]
