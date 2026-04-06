import { useState } from "react";
import { motion } from "framer-motion";
import { ArrowLeft } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { toast } from "@/hooks/use-toast";
import { cn } from "@/lib/utils";

const VISIBILITY_OPTIONS = [
  { value: "private", label: "Private", desc: "Only you can see this list" },
  { value: "searchable", label: "Searchable", desc: "Discoverable by others — they must request access to view" },
] as const;

type Visibility = typeof VISIBILITY_OPTIONS[number]["value"];

const CreateList = () => {
  const navigate = useNavigate();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [visibility, setVisibility] = useState<Visibility>("private");

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) {
      toast({ title: "Missing title", description: "Please give your list a name." });
      return;
    }
    toast({ title: "List created", description: `"${title}" has been created.` });
    navigate("/lists");
  };

  return (
    <div className="mx-auto max-w-2xl px-4 py-8 md:px-8 md:py-12">
      <Link
        to="/lists"
        className="mb-6 inline-flex items-center gap-1.5 text-xs text-muted-foreground hover:text-foreground transition-colors"
      >
        <ArrowLeft className="h-3 w-3" />
        Back to lists
      </Link>

      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
      >
        <h1 className="text-2xl md:text-3xl font-semibold tracking-tight text-foreground mb-1">
          Create a new list
        </h1>
        <p className="text-sm text-muted-foreground mb-8">
          Organize your reviews into themed collections.
        </p>

        <form onSubmit={handleSubmit} className="space-y-8">
          {/* Title */}
          <div>
            <label htmlFor="create-list-name" className="mb-1.5 block text-sm font-medium text-foreground">
              List name
            </label>
            <input
              id="create-list-name"
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g., Best espresso in Berlin, Top vacuums 2026..."
              className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm text-foreground placeholder:text-muted-foreground/60 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/40 transition-all"
            />
          </div>

          {/* Description */}
          <div>
            <label htmlFor="create-list-description" className="mb-1.5 block text-sm font-medium text-foreground">
              Description
            </label>
            <textarea
              id="create-list-description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="What's this list about? Help others understand your curation..."
              rows={3}
              className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm text-foreground placeholder:text-muted-foreground/60 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/40 transition-all resize-none leading-relaxed"
            />
          </div>

          {/* Visibility */}
          <div>
            <label className="mb-2 block text-sm font-medium text-foreground">Visibility</label>
            <div className="grid grid-cols-2 gap-3">
              {VISIBILITY_OPTIONS.map(({ value, label, desc }) => (
                <button
                  key={value}
                  type="button"
                  onClick={() => setVisibility(value)}
                  className={cn(
                    "flex flex-col items-start rounded-xl border p-4 text-left transition-all",
                    visibility === value
                      ? "border-primary bg-primary/5 ring-1 ring-primary/20"
                      : "border-border bg-card hover:border-primary/30"
                  )}
                >
                  <p className={cn("text-sm font-medium", visibility === value ? "text-foreground" : "text-muted-foreground")}>{label}</p>
                  <p className="text-[11px] text-muted-foreground/70 mt-0.5">{desc}</p>
                </button>
              ))}
            </div>
          </div>

          {/* Submit */}
          <div className="flex gap-3 pt-2">
            <Button type="submit" className="rounded-xl px-8">
              Create List
            </Button>
            <Button type="button" variant="ghost" onClick={() => navigate("/lists")} className="rounded-xl">
              Cancel
            </Button>
          </div>
        </form>
      </motion.div>
    </div>
  );
};

export default CreateList;
