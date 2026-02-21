# Next.js 라우팅 시스템

두가지 라우터

| 라우터          | 디렉토리   | API 정의 방식      | 도입 버전       |
|--------------|--------|----------------|-------------|
| Pages Router | pages/ | API Routes     | Next.js 9+  |
| App Router   | app/   | Route Handlers | Next.js 13+ |


Pages Router - API Routes (Legacy)

```text
pages/
  ├── api/
  │   ├── users/
  │   │   ├── index.ts      → GET/POST /api/users
  │   │   └── [id].ts       → GET/PUT/DELETE /api/users/:id
  │   └── auth/
  │       └── login.ts      → POST /api/auth/login
```

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


App Router - Route Handlers (권장)

```text
app/
  ├── api/
  │   ├── users/
  │   │   ├── route.ts      → GET/POST /api/users
  │   │   └── [id]/
  │   │       └── route.ts  → GET/PUT/DELETE /api/users/:id
```

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

주요 차이점

| 항목 | Pages Router (API Routes) | App Router (Route Handlers) |
| -- | -- | -- |
| 파일명 | 자유 (users.ts) | 고정 (routes.ts) |
| Request 객체 | | |
| | | |
| | | |

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