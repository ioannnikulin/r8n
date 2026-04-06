import { useState, useRef, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { ArrowLeft, ImagePlus, Search, Plus, Building2, MapPin, Sparkles } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { toast } from "@/hooks/use-toast";
import { cn } from "@/lib/utils";

// Mock existing suppliers/locations in "DB"
const EXISTING_SUPPLIERS = [
  { id: "1", name: "Bonanza Coffee", type: "Café", keywords: ["coffee", "espresso", "flat white", "latte", "cappuccino", "americano"] },
  { id: "2", name: "Dyson", type: "Brand", keywords: ["vacuum", "hair dryer", "air purifier", "v15", "v12", "airwrap"] },
  { id: "3", name: "IKEA", type: "Shop", keywords: ["furniture", "shelf", "desk", "kallax", "billy", "malm"] },
  { id: "4", name: "The Barn", type: "Café", keywords: ["coffee", "espresso", "flat white", "filter", "pour over"] },
  { id: "5", name: "Patagonia", type: "Brand", keywords: ["jacket", "fleece", "outdoor", "backpack", "clothing"] },
  { id: "6", name: "Five Guys", type: "Restaurant", keywords: ["burger", "fries", "shake", "milkshake", "cheeseburger"] },
  { id: "7", name: "Ace & Tate", type: "Shop", keywords: ["glasses", "sunglasses", "eyewear", "frames"] },
];

/** Find suppliers whose name or keywords match the subject text */
const getSuggestedSuppliers = (subject: string) => {
  if (subject.length < 2) return [];
  const lower = subject.toLowerCase();
  const words = lower.split(/\s+/);
  return EXISTING_SUPPLIERS.filter((s) =>
    s.name.toLowerCase().includes(lower) ||
    words.some((w) => w.length >= 3 && s.keywords.some((k) => k.includes(w) || w.includes(k)))
  );
};

const RatingRow = ({
  label,
  description,
  value,
  onChange,
}: {
  label: string;
  description: string;
  value: number | null;
  onChange: (v: number) => void;
}) => (
  <div>
    <label className="mb-0.5 block text-sm font-medium text-foreground">{label}</label>
    <p className="mb-2 text-xs text-muted-foreground">{description}</p>
    <div className="flex gap-2">
      {Array.from({ length: 10 }, (_, i) => i + 1).map((num) => (
        <motion.button
          key={num}
          type="button"
          whileTap={{ scale: 0.9 }}
          onClick={() => onChange(num)}
          className={cn(
            "flex h-9 w-9 items-center justify-center rounded-lg border text-sm font-mono font-semibold transition-all",
            value === num
              ? "border-primary bg-primary text-primary-foreground"
              : value && num <= value
                ? "border-primary/30 bg-primary/10 text-primary"
                : "border-border bg-card text-muted-foreground hover:border-primary/30"
          )}
        >
          {num}
        </motion.button>
      ))}
    </div>
  </div>
);

interface LinkedSupplier {
  id: string;
  name: string;
  type: string;
  isNew?: boolean;
}

const SupplierSearch = ({
  value,
  onChange,
}: {
  value: LinkedSupplier | null;
  onChange: (s: LinkedSupplier | null) => void;
}) => {
  const [query, setQuery] = useState("");
  const [open, setOpen] = useState(false);
  const [newType, setNewType] = useState("");
  const [showCreateForm, setShowCreateForm] = useState(false);
  const wrapperRef = useRef<HTMLDivElement>(null);

  const filtered = query.length > 0
    ? EXISTING_SUPPLIERS.filter((s) =>
        s.name.toLowerCase().includes(query.toLowerCase())
      )
    : EXISTING_SUPPLIERS;

  const exactMatch = EXISTING_SUPPLIERS.some(
    (s) => s.name.toLowerCase() === query.toLowerCase()
  );

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target as Node)) {
        setOpen(false);
        setShowCreateForm(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  if (value) {
    return (
      <div className="flex items-center gap-3 rounded-xl border border-border bg-card px-4 py-3">
        <Building2 className="h-4 w-4 text-muted-foreground shrink-0" />
        <div className="flex-1">
          <p className="text-sm font-medium text-foreground">{value.name}</p>
          <p className="text-[11px] text-muted-foreground">{value.type}{value.isNew ? " · New" : ""}</p>
        </div>
        <button
          type="button"
          onClick={() => onChange(null)}
          className="text-xs text-muted-foreground hover:text-foreground transition-colors"
        >
          Change
        </button>
      </div>
    );
  }

  return (
    <div ref={wrapperRef} className="relative">
      <div className="relative">
        <Search className="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <input
          type="text"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setOpen(true);
            setShowCreateForm(false);
          }}
          onFocus={() => setOpen(true)}
          placeholder="Search restaurant, brand, shop..."
          className="w-full rounded-xl border border-border bg-card pl-10 pr-4 py-3 text-sm text-foreground placeholder:text-muted-foreground/60 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/40 transition-all"
        />
      </div>

      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, y: -4 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -4 }}
            className="absolute z-50 mt-1.5 w-full rounded-xl border border-border bg-card shadow-lg overflow-hidden"
          >
            <div className="max-h-48 overflow-y-auto">
              {filtered.map((s) => (
                <button
                  key={s.id}
                  type="button"
                  onClick={() => {
                    onChange(s);
                    setQuery("");
                    setOpen(false);
                  }}
                  className="flex w-full items-center gap-3 px-4 py-2.5 text-left hover:bg-muted/50 transition-colors"
                >
                  <Building2 className="h-4 w-4 text-muted-foreground shrink-0" />
                  <div>
                    <p className="text-sm text-foreground">{s.name}</p>
                    <p className="text-[11px] text-muted-foreground">{s.type}</p>
                  </div>
                </button>
              ))}
              {filtered.length === 0 && (
                <p className="px-4 py-3 text-xs text-muted-foreground">No results found</p>
              )}
            </div>

            {/* Create new option */}
            {!exactMatch && query.length > 0 && !showCreateForm && (
              <button
                type="button"
                onClick={() => setShowCreateForm(true)}
                className="flex w-full items-center gap-2 border-t border-border px-4 py-3 text-left text-sm font-medium text-primary hover:bg-muted/50 transition-colors"
              >
                <Plus className="h-4 w-4" />
                Create "{query}"
              </button>
            )}

            {showCreateForm && (
              <div className="border-t border-border p-3 space-y-2">
                <p className="text-xs font-medium text-foreground">Create: {query}</p>
                <div className="flex flex-wrap gap-1.5">
                  {["Restaurant", "Café", "Brand", "Shop", "Hotel", "Service"].map((t) => (
                    <button
                      key={t}
                      type="button"
                      onClick={() => setNewType(t)}
                      className={cn(
                        "rounded-full border px-2.5 py-1 text-[11px] font-medium transition-all",
                        newType === t
                          ? "border-primary bg-primary text-primary-foreground"
                          : "border-border text-muted-foreground hover:border-primary/30"
                      )}
                    >
                      {t}
                    </button>
                  ))}
                </div>
                <Button
                  type="button"
                  size="sm"
                  disabled={!newType}
                  onClick={() => {
                    onChange({ id: `new-${Date.now()}`, name: query, type: newType, isNew: true });
                    setQuery("");
                    setOpen(false);
                    setShowCreateForm(false);
                    setNewType("");
                  }}
                  className="w-full rounded-lg text-xs mt-1"
                >
                  <Plus className="h-3 w-3 mr-1" />
                  Add {newType || "supplier"}
                </Button>
              </div>
            )}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

const CreateReview = () => {
  const navigate = useNavigate();
  const [subjectName, setSubjectName] = useState("");
  const [linkedSupplier, setLinkedSupplier] = useState<LinkedSupplier | null>(null);
  const [rating, setRating] = useState<number | null>(null);
  const [objectiveText, setObjectiveText] = useState("");
  const [subjectiveText, setSubjectiveText] = useState("");
  const [selectedList, setSelectedList] = useState("");

  const lists = ["Best espresso in Berlin", "Top vacuums 2026", "Date night restaurants"];

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!subjectName || !rating) {
      toast({ title: "Missing fields", description: "Please fill in the item name and rating." });
      return;
    }
    toast({ title: "Review created", description: `Your review of ${subjectName} has been saved as private.` });
    navigate("/");
  };

  return (
    <div className="mx-auto max-w-2xl px-4 py-8 md:px-8 md:py-12">
      <Link
        to="/"
        className="mb-6 inline-flex items-center gap-1.5 text-xs text-muted-foreground hover:text-foreground transition-colors"
      >
        <ArrowLeft className="h-3 w-3" />
        Back
      </Link>

      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
      >
        <h1 className="text-2xl md:text-3xl font-semibold tracking-tight text-foreground mb-1">Write a Review</h1>
        <p className="text-sm text-muted-foreground mb-8">
          Your review will be private by default. Only approved users can read it.
        </p>

        <form onSubmit={handleSubmit} className="space-y-8">
          {/* Subject / Item Name */}
          <div>
            <label htmlFor="review-subject-name" className="mb-1.5 block text-sm font-medium text-foreground">
              What are you reviewing?
            </label>
            <input
              id="review-subject-name"
              type="text"
              value={subjectName}
              onChange={(e) => setSubjectName(e.target.value)}
              placeholder="e.g., Flat White, Dyson V15 Detect, Margherita Pizza..."
              className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm text-foreground placeholder:text-muted-foreground/60 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/40 transition-all"
            />

            {/* Pairing Suggestions */}
            <AnimatePresence>
              {subjectName.length >= 2 && !linkedSupplier && (() => {
                const suggestions = getSuggestedSuppliers(subjectName);
                if (suggestions.length === 0) return null;
                return (
                  <motion.div
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: "auto" }}
                    exit={{ opacity: 0, height: 0 }}
                    className="mt-2 overflow-hidden"
                  >
                    <div className="rounded-xl border border-primary/20 bg-primary/5 p-3">
                      <p className="text-[11px] font-medium text-muted-foreground mb-2 flex items-center gap-1.5">
                        <Sparkles className="h-3 w-3 text-primary" />
                        Suggested matches from your network
                      </p>
                      <div className="flex flex-wrap gap-2">
                        {suggestions.map((s) => (
                          <button
                            key={s.id}
                            type="button"
                            onClick={() => setLinkedSupplier(s)}
                            className="flex items-center gap-1.5 rounded-lg border border-border bg-card px-3 py-1.5 text-xs font-medium text-foreground hover:border-primary/40 hover:bg-primary/5 transition-all"
                          >
                            <Building2 className="h-3 w-3 text-muted-foreground" />
                            {s.name}
                            <span className="text-muted-foreground/60">· {s.type}</span>
                          </button>
                        ))}
                      </div>
                    </div>
                  </motion.div>
                );
              })()}
            </AnimatePresence>
          </div>

          {/* Link Supplier / Location */}
          <div>
            <label className="mb-1 block text-sm font-medium text-foreground">
              <span className="flex items-center gap-1.5">
                <MapPin className="h-3.5 w-3.5 text-muted-foreground" />
                Link to a place or brand
              </span>
            </label>
            <p className="mb-2 text-xs text-muted-foreground">Optional — connect this to a restaurant, shop, or brand.</p>
            <SupplierSearch value={linkedSupplier} onChange={setLinkedSupplier} />
          </div>


          {/* Rating */}
          <div className="space-y-5 rounded-2xl border border-border bg-card p-5">
            <RatingRow
              label="Rating"
              description="Your overall assessment on a scale of 1–10."
              value={rating}
              onChange={setRating}
            />
          </div>

          {/* Objective Description */}
          <div className="space-y-3 rounded-2xl border border-border bg-card p-5">
            <div className="flex items-center gap-2 border-b border-border pb-3">
              <div className="h-2 w-2 rounded-full bg-primary" />
              <h3 className="text-sm font-medium text-foreground">Objective Assessment</h3>
              <span className="ml-auto text-[10px] uppercase tracking-widest text-muted-foreground/60">Facts</span>
            </div>
            <div>
              <label htmlFor="review-objective-notes" className="mb-1.5 block text-sm font-medium text-foreground">
                Objective Notes
              </label>
              <textarea
                id="review-objective-notes"
                value={objectiveText}
                onChange={(e) => setObjectiveText(e.target.value)}
                placeholder="Factual observations: quality, speed, price, materials..."
                rows={3}
                className="w-full rounded-xl border border-border bg-background px-4 py-3 text-sm text-foreground placeholder:text-muted-foreground/60 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/40 transition-all resize-none leading-relaxed"
              />
            </div>
          </div>

          {/* Subjective Description */}
          <div className="space-y-3 rounded-2xl border border-border bg-card p-5">
            <div className="flex items-center gap-2 border-b border-border pb-3">
              <div className="h-2 w-2 rounded-full bg-accent" />
              <h3 className="text-sm font-medium text-foreground">Subjective Opinion</h3>
              <span className="ml-auto text-[10px] uppercase tracking-widest text-muted-foreground/60">Taste</span>
            </div>
            <div>
              <label htmlFor="review-subjective-opinion" className="mb-1.5 block text-sm font-medium text-foreground">
                Your Opinion
              </label>
              <textarea
                id="review-subjective-opinion"
                value={subjectiveText}
                onChange={(e) => setSubjectiveText(e.target.value)}
                placeholder="Your personal feelings, experience, and honest opinion..."
                rows={4}
                className="w-full rounded-xl border border-border bg-background px-4 py-3 text-sm text-foreground placeholder:text-muted-foreground/60 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/40 transition-all resize-none leading-relaxed"
              />
            </div>
          </div>

          {/* Photos */}
          <div>
            <label className="mb-1.5 block text-sm font-medium text-foreground">Photos</label>
            <div className="flex h-24 cursor-pointer items-center justify-center rounded-xl border-2 border-dashed border-border text-muted-foreground transition-colors hover:border-primary/30 hover:text-primary">
              <div className="flex items-center gap-2 text-sm">
                <ImagePlus className="h-4 w-4" />
                Add photos
              </div>
            </div>
          </div>

          {/* Add to list */}
          <div>
            <label htmlFor="review-selected-list" className="mb-1.5 block text-sm font-medium text-foreground">
              Add to List (optional)
            </label>
            <div className="flex gap-2">
              <select
                id="review-selected-list"
                value={selectedList}
                onChange={(e) => setSelectedList(e.target.value)}
                className="flex-1 rounded-xl border border-border bg-card px-4 py-3 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/40 transition-all"
              >
                <option value="">None</option>
                {lists.map((l) => (
                  <option key={l} value={l}>{l}</option>
                ))}
              </select>
              <Button
                type="button"
                variant="outline"
                onClick={() => navigate("/lists/create")}
                className="rounded-xl shrink-0 gap-1.5"
              >
                <Plus className="h-3.5 w-3.5" />
                New list
              </Button>
            </div>
          </div>

          {/* Submit */}
          <div className="flex gap-3 pt-2">
            <Button type="submit" className="rounded-xl px-8">
              Publish Review
            </Button>
            <Button type="button" variant="ghost" onClick={() => navigate("/")} className="rounded-xl">
              Cancel
            </Button>
          </div>
        </form>

        {/* Legal */}
        <div className="mt-8 rounded-xl bg-secondary/50 px-4 py-3">
          <p className="text-[11px] text-muted-foreground/70 leading-relaxed">
            <strong className="text-muted-foreground/90">Protected Communication:</strong> This review is private
            and only visible to users you manually approve. In accordance with German privacy standards, this
            constitutes private correspondence, not public broadcast.
          </p>
        </div>
      </motion.div>
    </div>
  );
};

export default CreateReview;
