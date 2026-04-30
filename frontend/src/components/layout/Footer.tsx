import { NavLink } from "@/components/NavLink";

const Footer = () => {
  return (
    <footer className="border-t border-border bg-background/95 backdrop-blur-sm">
      <div className="mx-auto max-w-7xl px-4 py-6 md:px-8">
        <div className="flex flex-col md:flex-row items-center justify-between gap-4">
          {/* Legal links */}
          <div className="flex items-center gap-6 text-sm">
            <NavLink
              to="/terms"
              className="text-muted-foreground hover:text-foreground transition-colors"
            >
              Terms of Service
            </NavLink>
            <NavLink
              to="/privacy"
              className="text-muted-foreground hover:text-foreground transition-colors"
            >
              Privacy Policy
            </NavLink>
          </div>

          {/* Copyright */}
          <p className="text-xs text-muted-foreground/60">
            © {new Date().getFullYear()} R8N. All rights reserved.
          </p>
        </div>

        {/* Protected Communication notice */}
        <div className="mt-4 pt-4 border-t border-border/50">
          <p className="text-[11px] text-muted-foreground/60 leading-relaxed max-w-2xl">
            <strong className="text-muted-foreground/80">Protected Communication:</strong> Reviews
            on R8N are private and only visible to users you manually approve. In accordance with
            German privacy standards, this constitutes private correspondence, not public broadcast.
          </p>
        </div>
      </div>
    </footer>
  );
};

export default Footer;