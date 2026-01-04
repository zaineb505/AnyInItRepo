export interface User {
  id: number;
  name: string;
  email: string | null;
  avatarUrl: string | null;
  provider: string;
}
