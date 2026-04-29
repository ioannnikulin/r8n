import { Suspense, lazy } from "react";
import { QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { Toaster } from "@/components/ui/toaster";
import { TooltipProvider } from "@/components/ui/tooltip";
import RequireAuth from "@/components/auth/RequireAuth";
import RequireRole from "@/components/auth/RequireRole";
import AppLayout from "@/components/layout/AppLayout";
import { createQueryClient } from "@/lib/server-state";

const Index = lazy(() => import("./pages/Index"));
const Profile = lazy(() => import("./pages/Profile"));
const Supplier = lazy(() => import("./pages/Supplier"));
const OpinionList = lazy(() => import("./pages/OpinionList"));
const MyLists = lazy(() => import("./pages/MyLists"));
const Requests = lazy(() => import("./pages/Requests"));
const Messages = lazy(() => import("./pages/Messages"));
const Discover = lazy(() => import("./pages/Discover"));
const CreateReview = lazy(() => import("./pages/CreateReview"));
const CreateList = lazy(() => import("./pages/CreateList"));
const Settings = lazy(() => import("./pages/Settings"));
const EditProfile = lazy(() => import("./pages/EditProfile"));
const Login = lazy(() => import("./pages/Login"));
const CreateProfile = lazy(() => import("./pages/CreateProfile"));
const OpinionModeration = lazy(() => import("./pages/OpinionModeration"));
const RoleAssignment = lazy(() => import("./pages/RoleAssignment"));
const TermsOfService = lazy(() => import("./pages/TermsOfService"));
const PrivacyPolicy = lazy(() => import("./pages/PrivacyPolicy"));
const NotFound = lazy(() => import("./pages/NotFound"));

const queryClient = createQueryClient();

const routeLoadingFallback = (
  <div className="flex min-h-screen items-center justify-center text-sm text-muted-foreground">
    Loading...
  </div>
);

const App = () => (
  <QueryClientProvider client={queryClient}>
    <TooltipProvider>
      <Toaster />
      <Sonner />
      <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <Suspense fallback={routeLoadingFallback}>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/create-profile" element={<CreateProfile />} />
            <Route path="/terms" element={<TermsOfService />} />
            <Route path="/privacy" element={<PrivacyPolicy />} />
            <Route element={<RequireAuth />}>
              <Route element={<AppLayout />}>
                <Route index element={<Navigate to="/lists" replace />} />
                <Route path="/profile" element={<Profile />} />
                <Route path="/profile/:id" element={<Profile />} />
                <Route path="/supplier/:id" element={<Supplier />} />
                <Route path="/supplier" element={<Supplier />} />
                <Route path="/list/:id" element={<OpinionList />} />
                <Route path="/lists" element={<MyLists />} />
                <Route path="/requests" element={<Requests />} />
                <Route path="/messages" element={<Messages />} />
                <Route element={<RequireRole roles={["MODERATOR", "SUPPORT", "ADMIN"]} />}>
                  <Route path="/moderation/opinions" element={<OpinionModeration />} />
                </Route>
                <Route element={<RequireRole roles={["ADMIN"]} />}>
                  <Route path="/moderation/roles" element={<RoleAssignment />} />
                </Route>
                <Route path="/discover" element={<Discover />} />
                <Route path="/create" element={<CreateReview />} />
                <Route path="/lists/create" element={<CreateList />} />
                <Route path="/settings" element={<Settings />} />
                <Route path="/profile/edit" element={<EditProfile />} />
                <Route path="*" element={<NotFound />} />
              </Route>
            </Route>
          </Routes>
        </Suspense>
      </BrowserRouter>
    </TooltipProvider>
  </QueryClientProvider>
);

export default App;
