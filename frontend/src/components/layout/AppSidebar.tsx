import { Home, Search, List, Bell, PenLine, ListPlus, LogOut, ShieldCheck, MessageSquare, Users } from "lucide-react";
import { useNavigate } from "react-router-dom";
import logo from "@/assets/logo.png";
import { NavLink } from "@/components/NavLink";
import UserAvatar from "@/components/UserAvatar";
import { useLocation } from "react-router-dom";
import { useLogoutMutation, useUnreadMessagesCount } from "@/lib/server-state";
import { useMe } from "@/lib/server-state/hooks/users";
import { useIncomingAccessRequests } from "@/lib/server-state/hooks/access-requests";
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from "@/components/ui/sidebar";
import { useSidebar } from "@/components/ui/use-sidebar";

const mainItems = [
  // TODO #195: Dashboard is not implemented yet — hide from nav until ready
  // { title: "Dashboard", url: "/", icon: Home },
  { title: "Discover", url: "/discover", icon: Search },
  { title: "My Lists", url: "/lists", icon: List },
  { title: "Requests", url: "/requests", icon: Bell },
  { title: "Messages", url: "/messages", icon: MessageSquare },
  { title: "Moderation", url: "/moderation/opinions", icon: ShieldCheck, roles: ["MODERATOR", "SUPPORT", "ADMIN"] },
  { title: "User Roles", url: "/moderation/roles", icon: Users, roles: ["ADMIN"] },
] as const;

export function AppSidebar() {
  const { state } = useSidebar();
  const collapsed = state === "collapsed";
  const unreadMessagesCount = useUnreadMessagesCount({ refetchInterval: 30_000 });
  const location = useLocation();
  const navigate = useNavigate();
  const logoutMutation = useLogoutMutation({
    onSettled: () => {
      navigate("/login", { replace: true });
    },
  });
  const { data: me } = useMe();
  const { data: pendingRequests } = useIncomingAccessRequests(
    { filters: { status: "SENT" }, pageable: { page: 0, size: 1, sort: [] } },
    { refetchInterval: 30_000 },
  );
  const pendingCount = pendingRequests?.total ?? 0;

  const isActive = (path: string) =>
    path === "/" ? location.pathname === "/" : location.pathname.startsWith(path);

  return (
    <Sidebar collapsible="icon" className="border-r border-sidebar-border">
      <SidebarContent className="pt-6">
        {/* Brand */}
        <div className={`mb-8 flex ${collapsed ? "justify-center px-1" : "px-5"}`}>
          {collapsed ? (
            <span className="text-lg font-bold text-sidebar-foreground tracking-tighter">R8N</span>
          ) : (
            <img src={logo} alt="R8N" className="h-12 w-auto object-contain" />
          )}
        </div>

        {/* Quick Actions */}
        {!collapsed && (
          <div className="px-4 mb-6 space-y-2">
            <NavLink
              to="/create"
              className="flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90"
              activeClassName=""
            >
              <PenLine className="h-4 w-4" />
              Write Review
            </NavLink>
            <NavLink
              to="/lists/create"
              className="flex items-center gap-2 rounded-xl border border-border bg-card px-4 py-2.5 text-sm font-medium text-foreground transition-colors hover:bg-muted"
              activeClassName=""
            >
              <ListPlus className="h-4 w-4" />
              Create List
            </NavLink>
          </div>
        )}

        <SidebarGroup>
          <SidebarGroupLabel className="text-[10px] uppercase tracking-widest text-muted-foreground/60 px-6">
            Menu
          </SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {mainItems.filter((item) => !("roles" in item) || item.roles.some((r) => me?.roles?.includes(r))).map((item) => (
                <SidebarMenuItem key={item.title}>
                  <SidebarMenuButton asChild>
                    <NavLink
                      to={item.url}
                      end={item.url === "/"}
                      className="mx-2 flex items-center gap-3 rounded-lg px-4 py-2 text-sm text-sidebar-foreground/70 transition-colors hover:bg-sidebar-accent"
                      activeClassName="bg-sidebar-accent text-sidebar-foreground font-medium"
                    >
                      <item.icon className="h-4 w-4 shrink-0" />
                      {!collapsed && <span>{item.title}</span>}
                      {item.title === "Requests" && !collapsed && pendingCount > 0 && (
                        <span className="ml-auto flex h-5 w-5 items-center justify-center rounded-full bg-accent text-[10px] font-mono font-semibold text-accent-foreground">
                          {pendingCount}
                        </span>
                      )}
                      {item.title === "Messages" && !collapsed && unreadMessagesCount > 0 && (
                        <span className="ml-auto flex h-5 w-5 items-center justify-center rounded-full bg-accent text-[10px] font-mono font-semibold text-accent-foreground">
                          {unreadMessagesCount}
                        </span>
                      )}
                    </NavLink>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>

        <SidebarGroup className="mt-auto">
          <SidebarGroupContent>
            <SidebarMenu>
              <SidebarMenuItem className="mx-2 flex items-center gap-1">
                <SidebarMenuButton asChild size="lg" tooltip={me?.name ?? "Profile"} className="min-w-0 flex-1">
                  <NavLink
                    to="/profile"
                    aria-label={me?.name ? `Open profile for ${me.name}` : "Open your profile"}
                    className="flex min-w-0 items-center gap-3 rounded-lg px-2 py-2 text-sm text-sidebar-foreground/80 transition-colors hover:bg-sidebar-accent"
                    activeClassName="bg-sidebar-accent text-sidebar-foreground font-medium"
                  >
                    <UserAvatar userId={me?.id} name={me?.name ?? "…"} size="sm" />
                    {!collapsed && (
                      <span className="min-w-0 flex-1 truncate">
                        {me?.name ?? "…"}
                      </span>
                    )}
                  </NavLink>
                </SidebarMenuButton>
                {!collapsed && (
                  <button
                    onClick={() => logoutMutation.mutate()}
                    disabled={logoutMutation.isPending}
                    className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg text-muted-foreground/60 transition-colors hover:bg-sidebar-accent hover:text-sidebar-foreground disabled:opacity-50"
                    title="Log out"
                    aria-label="Log out"
                  >
                    <LogOut className="h-4 w-4" />
                  </button>
                )}
              </SidebarMenuItem>
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>
    </Sidebar>
  );
}
