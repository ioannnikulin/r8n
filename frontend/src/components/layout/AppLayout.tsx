import { Outlet, useLocation } from "react-router-dom";
import { SidebarProvider, SidebarTrigger } from "@/components/ui/sidebar";
import { AppSidebar } from "@/components/layout/AppSidebar";
import MobileNav from "@/components/layout/MobileNav";
import Footer from "@/components/layout/Footer";

interface AppLayoutProps {
  children?: React.ReactNode;
}

const AppLayout = ({ children }: AppLayoutProps) => {
  const location = useLocation();
  const isMessagesPage = location.pathname === "/messages";

  return (
    <SidebarProvider>
      <div className="min-h-screen flex w-full">
        {/* Desktop sidebar */}
        <div className="hidden md:block">
          <AppSidebar />
        </div>

        <div className="flex-1 flex flex-col min-w-0">
          {/* Mobile header */}
          <header className="flex h-14 items-center justify-between border-b border-border px-4 md:hidden">
            <img src="/favicon.png" alt="R8N" className="h-8" />
            <SidebarTrigger className="md:hidden" />
          </header>

          {/* Desktop header */}
          <header className="hidden md:flex h-12 items-center border-b border-border px-4">
            <SidebarTrigger />
          </header>

          {/* Main content */}
          <main className={isMessagesPage ? "flex-1 min-h-0 overflow-hidden" : "flex-1 pb-20 md:pb-0"}>
            {children ?? <Outlet />}
          </main>

          {/* Footer */}
          {!isMessagesPage && <Footer />}
        </div>

        {/* Mobile bottom nav */}
        <MobileNav />
      </div>
    </SidebarProvider>
  );
};

export default AppLayout;
