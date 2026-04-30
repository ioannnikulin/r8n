import { useMemo, useState } from "react";
import { motion } from "framer-motion";
import { Shield, ShieldOff, ShieldCheck, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import UserAvatar from "@/components/UserAvatar";
import {
  useUsersWithRoles,
  useAssignModeratorMutation,
  useRevokeModeratorMutation,
  useAssignSupportMutation,
  useRevokeSupportMutation,
  useAssignAdminMutation,
  useRevokeAdminMutation,
} from "@/lib/server-state/hooks/users";
import type { UserWithRolesDto } from "@/lib/api/users";


const RoleAssignment = () => {
  const { data: users = [], isLoading, isError } = useUsersWithRoles();
  const [pendingUserId, setPendingUserId] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState<"all" | "admin" | "support" | "moderator">("all");

  const assignModerator = useAssignModeratorMutation({ onSettled: () => setPendingUserId(null) });
  const revokeModerator = useRevokeModeratorMutation({ onSettled: () => setPendingUserId(null) });
  const assignSupport = useAssignSupportMutation({ onSettled: () => setPendingUserId(null) });
  const revokeSupport = useRevokeSupportMutation({ onSettled: () => setPendingUserId(null) });
  const assignAdmin = useAssignAdminMutation({ onSettled: () => setPendingUserId(null) });
  const revokeAdmin = useRevokeAdminMutation({ onSettled: () => setPendingUserId(null) });

  const handleModeratorToggle = (user: UserWithRolesDto) => {
    setPendingUserId(user.id);
    if (user.isModerator) {
      revokeModerator.mutate(user.id);
    } else {
      assignModerator.mutate(user.id);
    }
  };

  const handleSupportToggle = (user: UserWithRolesDto) => {
    setPendingUserId(user.id);
    if (user.isSupport) {
      revokeSupport.mutate(user.id);
    } else {
      assignSupport.mutate(user.id);
    }
  };

  const handleAdminToggle = (user: UserWithRolesDto) => {
    setPendingUserId(user.id);
    if (user.isAdmin) {
      revokeAdmin.mutate(user.id);
    } else {
      assignAdmin.mutate(user.id);
    }
  };

  const filteredUsers = useMemo(() => {
    const q = search.trim().toLowerCase();
    return users
      .filter((u) => {
        if (q && !u.name.toLowerCase().includes(q) && !u.email.toLowerCase().includes(q)) return false;
        if (roleFilter === "admin") return u.isAdmin;
        if (roleFilter === "support") return u.isSupport && !u.isAdmin;
        if (roleFilter === "moderator") return u.isModerator && !u.isSupport && !u.isAdmin;
        return true;
      })
      ;
  }, [users, search, roleFilter]);

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 md:px-8 md:py-12">
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="mb-8"
      >
        <h1 className="mb-2 text-3xl font-semibold tracking-tight text-foreground md:text-4xl">
          User Roles
        </h1>
        <p className="max-w-2xl text-sm text-muted-foreground">
          Assign or remove roles for users. Roles are inherited: Admin &gt; Support &gt; Moderator &gt; User.
        </p>
      </motion.div>

      {/* Role inheritance explanation */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3, delay: 0.05 }}
        className="mb-8 grid grid-cols-1 gap-3 sm:grid-cols-4"
      >
        {[
          {
            title: "User",
            description: "Can write reviews, manage their own lists, and send access requests.",
            color: "bg-muted/50 border-border",
          },
          {
            title: "Moderator",
            description: "Inherits User. Can also review and action opinions in the moderation queue.",
            color: "bg-muted/50 border-border",
          },
          {
            title: "Support",
            description: "Inherits Moderator. Can also read and reply to user support threads.",
            color: "bg-muted/50 border-border",
          },
          {
            title: "Admin",
            description: "Inherits Support. Can also assign roles and manage platform settings.",
            color: "bg-muted/50 border-border",
          },
        ].map((role) => (
          <div key={role.title} className={`rounded-xl border p-4 ${role.color}`}>
            <p className="mb-1 text-sm font-semibold text-foreground">{role.title}</p>
            <p className="text-xs text-muted-foreground">{role.description}</p>
          </div>
        ))}
      </motion.div>

      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3, delay: 0.08 }}
        className="mb-6 flex flex-wrap items-center gap-3"
      >
        <div className="relative max-w-sm flex-1">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="Search by name or email…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-9"
          />
        </div>
        <div className="flex gap-2">
          {(["all", "admin", "support", "moderator"] as const).map((r) => (
            <Button
              key={r}
              variant={roleFilter === r ? "default" : "outline"}
              size="sm"
              className="rounded-xl capitalize"
              onClick={() => setRoleFilter(r)}
            >
              {r === "all" ? "All roles" : r.charAt(0).toUpperCase() + r.slice(1)}
            </Button>
          ))}
        </div>
      </motion.div>

      {isLoading ? (
        <div className="py-16 text-center text-sm text-muted-foreground">Loading users…</div>
      ) : isError ? (
        <div className="py-16 text-center text-sm text-destructive">
          Failed to load users. You may not have permission to view this page.
        </div>
      ) : (
        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3, delay: 0.1 }}
          className="space-y-3"
        >
          {filteredUsers.map((user) => (
            <Card key={user.id} className="rounded-2xl border-border">
              <CardContent className="flex flex-wrap items-center gap-4 px-5 py-4 md:px-6">
                <UserAvatar userId={user.id} name={user.name} size="md" />

                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="text-sm font-medium text-foreground">{user.name}</p>
                    {(user.isAdmin || user.isSupport || user.isModerator) && (
                      <span className="text-[10px] font-medium text-muted-foreground">
                        {user.isAdmin ? "Admin" : user.isSupport ? "Support" : "Moderator"}
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-muted-foreground">{user.email}</p>
                </div>

                <div className="flex shrink-0 gap-2">
                  <Button
                    variant={user.isModerator ? "outline" : "default"}
                    size="sm"
                    className="rounded-xl"
                    disabled={pendingUserId === user.id || user.isSupport || user.isAdmin}
                    title={user.isAdmin || user.isSupport ? "Support and Admin already have Moderator permissions" : undefined}
                    onClick={() => handleModeratorToggle(user)}
                  >
                    {user.isModerator ? (
                      <>
                        <ShieldOff className="h-3.5 w-3.5" />
                        Remove moderator
                      </>
                    ) : (
                      <>
                        <Shield className="h-3.5 w-3.5" />
                        Make moderator
                      </>
                    )}
                  </Button>

                  <Button
                    variant={user.isSupport ? "outline" : "secondary"}
                    size="sm"
                    className="rounded-xl"
                    disabled={pendingUserId === user.id || user.isAdmin}
                    title={user.isAdmin ? "Admins already have Support permissions" : undefined}
                    onClick={() => handleSupportToggle(user)}
                  >
                    {user.isSupport ? (
                      <>
                        <ShieldOff className="h-3.5 w-3.5" />
                        Remove support
                      </>
                    ) : (
                      <>
                        <Shield className="h-3.5 w-3.5" />
                        Make support
                      </>
                    )}
                  </Button>

                  <Button
                    variant={user.isAdmin ? "outline" : "secondary"}
                    size="sm"
                    className="rounded-xl"
                    disabled={pendingUserId === user.id}
                    onClick={() => handleAdminToggle(user)}
                  >
                    {user.isAdmin ? (
                      <>
                        <ShieldOff className="h-3.5 w-3.5" />
                        Remove admin
                      </>
                    ) : (
                      <>
                        <ShieldCheck className="h-3.5 w-3.5" />
                        Make admin
                      </>
                    )}
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}

          {filteredUsers.length === 0 && (
            <Card className="rounded-2xl border-dashed border-border">
              <CardContent className="py-14 text-center">
                <p className="text-base font-medium text-foreground">
                  {search ? "No users match your search." : "No users found."}
                </p>
              </CardContent>
            </Card>
          )}
        </motion.div>
      )}
    </div>
  );
};

export default RoleAssignment;
