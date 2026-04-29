import { useState, useRef, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { ArrowLeft, ImagePlus, Search, Plus, Building2, MapPin } from "lucide-react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { toast } from "@/hooks/use-toast";
import { cn } from "@/lib/utils";
import { useMyOpinionLists, useLinkOpinionToListMutation } from "@/lib/server-state/hooks/opinion-lists";
import { useCreateOpinionMutation } from "@/lib/server-state/hooks/opinions";
import { useCreateSubjectMutation, useFindSubjects, useSetPrimaryReferentMutation } from "@/lib/server-state/hooks/subjects";
import { useCreateReferentMutation, useFindReferents } from "@/lib/server-state/hooks/referents";
import type { Uuid } from "@/lib/api/shared";

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
  id: Uuid;
  name: string;
  address: string | null;
  latitude: number | null;
  longitude: number | null;
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
  const [showCreateForm, setShowCreateForm] = useState(false);
  const wrapperRef = useRef<HTMLDivElement>(null);

  const trimmedQuery = query.trim();
  const findReferents = useFindReferents(
    { query: trimmedQuery || undefined, pageable: { page: 0, size: 10 } },
    { enabled: open },
  );
  const createReferent = useCreateReferentMutation();

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
          {value.address && <p className="text-[11px] text-muted-foreground">{value.address}</p>}
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
              {findReferents.isLoading ? (
                <p className="px-4 py-3 text-xs text-muted-foreground">Searching…</p>
              ) : (findReferents.data?.items?.length ?? 0) === 0 ? (
                <p className="px-4 py-3 text-xs text-muted-foreground">No results found</p>
              ) : (
                <div className="py-1">
                  {(findReferents.data?.items ?? []).map((referent) => {
                    const address = referent.address;
                    return (
                      <button
                        key={referent.id}
                        type="button"
                        onClick={() => {
                          onChange({
                            id: referent.id,
                            name: referent.name,
                            address: referent.address,
                            latitude: referent.latitude,
                            longitude: referent.longitude,
                          });
                          setQuery("");
                          setOpen(false);
                          setShowCreateForm(false);
                        }}
                        className="flex w-full flex-col gap-0.5 px-4 py-2.5 text-left hover:bg-muted/40 transition-colors"
                      >
                        <span className="text-sm font-medium text-foreground">{referent.name}</span>
                        {address ? (
                          <span className="text-[11px] text-muted-foreground inline-flex items-center gap-1">
                            <MapPin className="h-3 w-3" />
                            {address}
                          </span>
                        ) : (
                          <span className="text-[11px] text-muted-foreground">No address</span>
                        )}
                      </button>
                    );
                  })}
                </div>
              )}
            </div>

            {trimmedQuery.length > 0 && !showCreateForm && (
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
              <div className="border-t border-border p-3">
                <Button
                  type="button"
                  size="sm"
                  disabled={createReferent.isPending}
                  onClick={async () => {
                    try {
                      const created = await createReferent.mutateAsync({
                        name: trimmedQuery,
                      });
                      onChange({
                        id: created.id,
                        name: created.name,
                        address: created.address,
                        latitude: created.latitude,
                        longitude: created.longitude,
                      });
                      setQuery("");
                      setOpen(false);
                      setShowCreateForm(false);
                    } catch {
                      // error surfaced via mutation meta errorTitle
                    }
                  }}
                  className="w-full rounded-lg text-xs"
                >
                  <Plus className="h-3 w-3 mr-1" />
                  Add "{trimmedQuery}"
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
  const [searchParams] = useSearchParams();
  const [subjectName, setSubjectName] = useState("");
  const [linkedSupplier, setLinkedSupplier] = useState<LinkedSupplier | null>(null);
  const [rating, setRating] = useState<number | null>(null);
  const [objectiveText, setObjectiveText] = useState("");
  const [subjectiveText, setSubjectiveText] = useState("");
  const [selectedList, setSelectedList] = useState("");

  const { data: listsData } = useMyOpinionLists({ pageable: { page: 0, size: 50 } });
  const myLists = listsData?.items ?? [];

  const createOpinion = useCreateOpinionMutation();
  const linkOpinion = useLinkOpinionToListMutation();
  const findSubjects = useFindSubjects(
    {
      query: subjectName.trim() || undefined,
      referentId: linkedSupplier?.id,
      pageable: { page: 0, size: 10, sort: [{ property: "name", direction: "ASC" }] },
    },
    { enabled: false },
  );
  const createSubject = useCreateSubjectMutation();
  const setPrimaryReferent = useSetPrimaryReferentMutation();

  const isSubmitting = createOpinion.isPending || linkOpinion.isPending;

  useEffect(() => {
    const listId = searchParams.get("listId");
    if (listId) {
      setSelectedList(listId);
    }
  }, [searchParams]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const trimmedSubject = subjectName.trim();
    if (!trimmedSubject) {
      toast({ title: "Missing subject", description: "Please fill in what you are reviewing." });
      return;
    }
    if (!linkedSupplier) {
      toast({ title: "Missing supplier", description: "Please link this review to a place or brand." });
      return;
    }
    if (!rating) {
      toast({ title: "Missing rating", description: "Please provide a rating." });
      return;
    }

    try {
      const results = await findSubjects.refetch();
      const existingMatch = results.data?.items.find(
        (s) => s.name.toLowerCase() === trimmedSubject.toLowerCase(),
      );

      const subject =
        existingMatch ??
        (await createSubject.mutateAsync({
          name: trimmedSubject,
          primaryReferentId: linkedSupplier.id,
          referentName: null,
          address: null,
          latitude: null,
          longitude: null,
        }));

      if (existingMatch && existingMatch.primaryReferent?.id !== linkedSupplier.id) {
        await setPrimaryReferent.mutateAsync({ subjectId: existingMatch.id, referentId: linkedSupplier.id });
      }

      const opinion = await createOpinion.mutateAsync({
        subjectId: subject.id,
        mark: rating,
        subjective: subjectiveText.trim() ? [subjectiveText.trim()] : undefined,
        objective: objectiveText.trim() ? [objectiveText.trim()] : undefined,
      });

      if (selectedList) {
        await linkOpinion.mutateAsync({ listId: selectedList, opinionId: opinion.id });
      }

      navigate("/");
    } catch {
      // errors surfaced via mutation meta errorTitle
    }
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
          </div>

          {/* Link Supplier / Location */}
          <div>
            <label className="mb-1 block text-sm font-medium text-foreground">
              <span className="flex items-center gap-1.5">
                <MapPin className="h-3.5 w-3.5 text-muted-foreground" />
                Link to a place or brand
              </span>
            </label>
            <p className="mb-2 text-xs text-muted-foreground">Required — connect this review to a restaurant, shop, or brand.</p>
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
                {myLists.map((l) => (
                  <option key={l.listId} value={l.listId}>{l.listName}</option>
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
            <Button type="submit" className="rounded-xl px-8" disabled={isSubmitting}>
              {isSubmitting ? "Saving…" : "Publish Review"}
            </Button>
            <Button type="button" variant="ghost" onClick={() => navigate("/")} className="rounded-xl" disabled={isSubmitting}>
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
