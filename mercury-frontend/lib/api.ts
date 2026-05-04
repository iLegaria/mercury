export const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json', ...init?.headers },
    ...init,
  });
  if (!res.ok) {
    const text = await res.text().catch(() => res.statusText);
    throw new ApiError(res.status, text);
  }
  const text = await res.text();
  return text ? JSON.parse(text) : (undefined as T);
}

export const api = {
  get:    <T>(path: string)                     => request<T>(path),
  post:   <T>(path: string, body: unknown)      => request<T>(path, { method: 'POST', body: JSON.stringify(body) }),
  patch:  <T>(path: string, body: unknown)      => request<T>(path, { method: 'PATCH', body: JSON.stringify(body) }),
  delete: <T>(path: string)                     => request<T>(path, { method: 'DELETE' }),
  postForm: <T>(path: string, form: FormData)   => fetch(`${BASE_URL}${path}`, { method: 'POST', body: form })
    .then(async res => {
      if (!res.ok) throw new ApiError(res.status, await res.text().catch(() => res.statusText));
      const text = await res.text();
      return text ? JSON.parse(text) as T : undefined as T;
    }),
};

export { ApiError };
