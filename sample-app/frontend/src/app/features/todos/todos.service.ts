export type Todo = { id: number; title: string; done: boolean };

type ApiSuccess<T> = { data: T };
type ApiError = {
  error: {
    code: string;
    message: string;
    details?: unknown;
  };
};

/**
 * Todos feature service.
 *
 * This is intentionally explicit: the service directly calls backend routes.
 */

async function readData<T>(res: Response): Promise<T> {
  if (res.status === 204) return undefined as T;

  // Some responses can legally be empty (or non-JSON), even when status != 204.
  // Using `text()` avoids "Unexpected end of JSON input".
  const raw = await res.text();
  if (!raw) {
    if (res.ok) return undefined as T;
    throw new Error(`${res.status} ${res.statusText}`.trim());
  }

  const contentType = res.headers.get("content-type") ?? "";
  if (!contentType.includes("application/json")) {
    if (res.ok) return raw as unknown as T;
    throw new Error(`${res.status} ${res.statusText}`.trim());
  }

  let json: ApiSuccess<T> | ApiError;
  try {
    json = JSON.parse(raw) as ApiSuccess<T> | ApiError;
  } catch {
    if (res.ok) {
      throw new Error("Invalid JSON received from server");
    }
    throw new Error(`${res.status} ${res.statusText}`.trim());
  }

  if (!res.ok) {
    const err = json as ApiError;
    throw new Error(`${err.error.code}: ${err.error.message}`);
  }

  return (json as ApiSuccess<T>).data;
}

export const todosService = {
  list: async () => {
    const res = await fetch("/api/todos");
    return readData<Todo[]>(res);
  },
  create: async (title: string) => {
    const res = await fetch("/api/todos", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ title }),
    });
    return readData<Todo>(res);
  },
  toggle: async (id: number) => {
    const res = await fetch(`/api/todos/${id}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
    });
    return readData<Todo>(res);
  },
  remove: async (id: number) => {
    const res = await fetch(`/api/todos/${id}`, { method: "DELETE" });
    return readData<void>(res);
  },
} as const;
