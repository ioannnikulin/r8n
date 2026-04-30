import { Plus } from "lucide-react";
import { motion } from "framer-motion";
import { Link } from "react-router-dom";
import OpinionListCard from "@/components/OpinionListCard";
import { QueryState } from "@/components/server-state/QueryState";
import { useMyOpinionLists } from "@/lib/server-state/hooks/opinion-lists";

const MyLists = () => {
  const { data, isLoading, isError, error, refetch } = useMyOpinionLists({
    pageable: { page: 0, size: 50 },
  });

  const lists = data?.items ?? [];

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 md:px-8 md:py-12">
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="mb-10"
      >
        <h1 className="font-body text-3xl md:text-4xl font-semibold text-foreground mb-2 tracking-tight">
          My Lists
        </h1>
        <p className="text-muted-foreground text-sm">
          {data ? `${data.total} lists` : ""}
        </p>
      </motion.div>

      {/* My Lists */}
      <motion.section
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3, delay: 0.1 }}
        className="mb-12"
      >
        <h2 className="mb-4 text-lg font-semibold tracking-tight text-foreground">Created by you</h2>
        <QueryState
          isLoading={isLoading}
          isError={isError}
          error={error}
          isEmpty={lists.length === 0}
          emptyMessage="You haven't created any lists yet."
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
              className="flex cursor-pointer items-center justify-center rounded-2xl border-2 border-dashed border-border p-5 text-muted-foreground transition-colors hover:border-primary/30 hover:text-primary min-h-[140px]"
            >
              <Link to="/lists/create" className="flex items-center gap-2 text-sm font-medium">
                <Plus className="h-4 w-4" />
                New List
              </Link>
            </motion.div>
          </div>
        </QueryState>
      </motion.section>

    </div>
  );
};

export default MyLists;
