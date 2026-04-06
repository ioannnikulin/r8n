import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { Toaster } from "@/components/ui/toaster";
import { TooltipProvider } from "@/components/ui/tooltip";
import AppLayout from "@/components/layout/AppLayout";
import Index from "./pages/Index";
import Profile from "./pages/Profile";
import Supplier from "./pages/Supplier";
import OpinionList from "./pages/OpinionList";
import MyLists from "./pages/MyLists";
import Requests from "./pages/Requests";
import Discover from "./pages/Discover";
import CreateReview from "./pages/CreateReview";
import CreateList from "./pages/CreateList";
import Settings from "./pages/Settings";
import EditProfile from "./pages/EditProfile";
import Login from "./pages/Login";
import CreateProfile from "./pages/CreateProfile";
import NotFound from "./pages/NotFound";

const queryClient = new QueryClient();

const App = () => (
  <QueryClientProvider client={queryClient}>
    <TooltipProvider>
      <Toaster />
      <Sonner />
      <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/create-profile" element={<CreateProfile />} />
          <Route element={<AppLayout />}>
            <Route index element={<Index />} />
            <Route path="/profile" element={<Profile />} />
            <Route path="/profile/:id" element={<Profile />} />
            <Route path="/supplier/:id" element={<Supplier />} />
            <Route path="/supplier" element={<Supplier />} />
            <Route path="/list/:id" element={<OpinionList />} />
            <Route path="/lists" element={<MyLists />} />
            <Route path="/requests" element={<Requests />} />
            <Route path="/discover" element={<Discover />} />
            <Route path="/create" element={<CreateReview />} />
            <Route path="/lists/create" element={<CreateList />} />
            <Route path="/settings" element={<Settings />} />
            <Route path="/profile/edit" element={<EditProfile />} />
            <Route path="*" element={<NotFound />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </TooltipProvider>
  </QueryClientProvider>
);

export default App;
