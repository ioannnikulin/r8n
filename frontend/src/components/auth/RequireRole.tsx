import { Navigate, Outlet } from "react-router-dom";
import { useMe } from "@/lib/server-state/hooks/users";

type Props = {
  roles: string[];
};

const RequireRole = ({ roles }: Props) => {
  const { data: me, isLoading } = useMe();

  if (isLoading) return null;

  const hasRole = me?.roles?.some((r) => roles.includes(r)) ?? false;

  return hasRole ? <Outlet /> : <Navigate to="/" replace />;
};

export default RequireRole;
