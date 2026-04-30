import { Search, Plus } from "lucide-react";
import { motion } from "framer-motion";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import OpinionListCard from "@/components/OpinionListCard";
import { QueryState } from "@/components/server-state/QueryState";
import { useMyOpinionLists } from "@/lib/server-state/hooks/opinion-lists";

function getGreeting() {
  const hour = new Date().getHours();
  if (hour < 12) return "Good morning";
  if (hour < 18) return "Good afternoon";
  return "Good evening";
}

// TODO #195: Dashboard is not connected — hidden from the sidebar nav until implementation is complete
const Dashboard = () => {
  const { data, isLoading, isError, error, refetch } = useMyOpinionLists({
    pageable: { page: 0, size: 3 },
  });

  const lists = data?.items ?? [];

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 md:px-8 md:py-12">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="mb-10"
      >
        <h1 className="font-body text-3xl md:text-4xl font-semibold text-foreground mb-2 tracking-tight">
          {getGreeting()}
        </h1>
      </motion.div>

      {/* Search */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3, delay: 0.1 }}
        className="mb-10"
      >
        <div className="relative">
          <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search items, suppliers, or reviewers..."
            className="w-full rounded-xl border border-border bg-card py-3 pl-11 pr-4 text-sm text-foreground placeholder:text-muted-foreground/60 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/40 transition-all"
          />
        </div>
      </motion.div>

      {/* My Lists */}
      <motion.section
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3, delay: 0.15 }}
        className="mb-12"
      >
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold tracking-tight text-foreground">Your Lists</h2>
          <Link to="/lists">
            <Button variant="ghost" size="sm" className="text-xs text-muted-foreground">
              View all
            </Button>
          </Link>
        </div>

        <QueryState
          isLoading={isLoading}
          isError={isError}
          error={error}
          isEmpty={false}
          onRetry={refetch}
        >
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {lists.map((list) => (
              <Link key={list.listId} to={`/list/${list.listId}`}>
                <OpinionListCard
                  id={list.listId}
                  title={list.listName}
                  description=""
                  reviewCount={list.opinionsCount}
                  authorId={list.owner}
                  authorName={list.ownerName}
                  hasAccess={true}
                />
              </Link>
            ))}
            <motion.div
              whileTap={{ scale: 0.98 }}
              className="flex cursor-pointer items-center justify-center rounded-2xl border-2 border-dashed border-border p-5 text-muted-foreground transition-colors hover:border-primary/30 hover:text-primary"
            >
              <Link to="/lists/create" className="flex items-center gap-2 text-sm font-medium">
                <Plus className="h-4 w-4" />
                New List
              </Link>
            </motion.div>
          </div>
        </QueryState>
      </motion.section>

      {/* Activity Feed — blocked until feed endpoint is available */}
      <motion.section
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3, delay: 0.2 }}
      >
        <h2 className="mb-4 text-lg font-semibold tracking-tight text-foreground">From Your Network</h2>
        <div className="rounded-2xl border border-dashed border-border py-12 text-center">
          <p className="text-sm text-muted-foreground">Network feed coming soon.</p>
        </div>
      </motion.section>

      {/* Legal footer */}
      <footer className="mt-16 border-t border-border pt-6 pb-4">
        <p className="text-[11px] text-muted-foreground/60 leading-relaxed max-w-2xl">
          <strong className="text-muted-foreground/80">Protected Communication:</strong> Reviews
          on R8N are private and only visible to users you manually approve. In accordance with
          German privacy standards, this constitutes private correspondence, not public broadcast.
        </p>
      </footer>
    </div>
  );
};

export default Dashboard;
