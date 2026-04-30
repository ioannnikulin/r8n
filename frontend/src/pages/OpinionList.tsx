import { useState, useCallback } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import { ArrowLeft, List, ChevronDown, Plus, Link2, GitMerge, Search, MoreHorizontal, Pencil, FolderInput, X, Trash2, Settings, Globe, Lock } from "lucide-react";
import UserAvatar from "@/components/UserAvatar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { QueryState } from "@/components/server-state/QueryState";
import { cn } from "@/lib/utils";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  useOpinionList,
  useLinkOpinionToListMutation,
  useLinkMyOpinionForSubjectToListMutation,
  useSyncOpinionListsMutation,
  useSearchOpinionLists,
  useUnlinkOpinionFromListMutation,
  useMyOpinionLists,
  useDeleteOpinionListMutation,
  useSetOpinionListPrivacyMutation,
  useMoveOpinionMutation,
  useRenameOpinionListMutation,
} from "@/lib/server-state/hooks/opinion-lists";
import {
  useCreateOpinionMutation,
  useUpdateOpinionMutation,
  useDeleteOpinionMutation,
} from "@/lib/server-state/hooks/opinions";
import { useFindSubjects } from "@/lib/server-state/hooks/subjects";
import { useMe } from "@/lib/server-state/hooks/users";
import type { OpinionRowDto, OpinionSummaryDto } from "@/lib/api/opinions";
import type { OpinionListSummaryDto } from "@/lib/api/opinion-lists";
import type { Uuid } from "@/lib/api/shared";

const OpinionListPage = () => {
  const { id: listId } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [expandedItem, setExpandedItem] = useState<string | null>(null);
  const [linkDialogOpen, setLinkDialogOpen] = useState(false);
  const [syncDialogOpen, setSyncDialogOpen] = useState(false);
  const [publishedAfter, setPublishedAfter] = useState("");

  const { data, isLoading, isError, error, refetch } = useOpinionList(
    { listId: listId!, publishedAfter: publishedAfter ? new Date(publishedAfter).toISOString() : undefined },
    { enabled: !!listId },
  );

  const createOpinion = useCreateOpinionMutation();
  const linkOpinion = useLinkOpinionToListMutation();
  const unlinkOpinion = useUnlinkOpinionFromListMutation();
  const updateOpinion = useUpdateOpinionMutation();
  const deleteOpinion = useDeleteOpinionMutation();
  const deleteList = useDeleteOpinionListMutation();
  const setListPrivacy = useSetOpinionListPrivacyMutation();
  const moveOpinion = useMoveOpinionMutation();
  const renameList = useRenameOpinionListMutation();

  const me = useMe();
  const currentUserId = me.data?.id ?? null;
  const isListOwner = !!data && !!currentUserId && data.owner === currentUserId;

  const [editingOpinion, setEditingOpinion] = useState<OpinionRowDto | null>(null);
  const [movingOpinion, setMovingOpinion] = useState<OpinionRowDto | null>(null);
  const [deletingOpinionId, setDeletingOpinionId] = useState<Uuid | null>(null);
  const [deleteListConfirmOpen, setDeleteListConfirmOpen] = useState(false);
  const [renameDialogOpen, setRenameDialogOpen] = useState(false);

  const summaries = data?.opinionSummaries ?? [];

  const handleAdjustWeight = useCallback((opinionId: Uuid, weight: number) => {
    linkOpinion.mutate({ listId: listId!, opinionId, weight });
  }, [linkOpinion, listId]);

  const handleUnlinkFromList = useCallback((opinionId: Uuid) => {
    unlinkOpinion.mutate({ listId: listId!, opinionId });
  }, [unlinkOpinion, listId]);

  const handleDeleteForever = useCallback((opinionId: Uuid) => {
    setDeletingOpinionId(opinionId);
  }, []);

  const handleAddReview = useCallback(async (
    subjectId: Uuid,
    mark: number,
    subjective: string,
    objective: string,
  ) => {
    const opinion = await createOpinion.mutateAsync({
      subjectId,
      mark,
      subjective: [subjective],
      objective: [objective],
    });
    await linkOpinion.mutateAsync({ listId: listId!, opinionId: opinion.id });
  }, [createOpinion, linkOpinion, listId]);

  return (
    <div className="mx-auto max-w-5xl px-4 py-8 md:px-8 md:py-12">
      <Link
        to="/lists"
        className="mb-6 inline-flex items-center gap-1.5 text-xs text-muted-foreground hover:text-foreground transition-colors"
      >
        <ArrowLeft className="h-3 w-3" />
        Back to lists
      </Link>

      <QueryState
        isLoading={isLoading}
        isError={isError}
        error={error}
        isEmpty={false}
        onRetry={refetch}
      >
        <>
          <motion.div
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4 }}
            className="mb-8"
          >
            <div className="flex items-center gap-3 mb-2">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/8">
                <List className="h-5 w-5 text-primary" />
              </div>
              <h1 className="text-2xl md:text-3xl font-semibold tracking-tight text-foreground font-body flex-1">
                {!isListOwner && data?.ownerName ? `${data.ownerName} · ` : ""}{data?.listName}
              </h1>
              {isListOwner && data && (
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <button
                      type="button"
                      className="rounded-md p-2 text-muted-foreground hover:bg-muted/50 hover:text-foreground transition-colors"
                      aria-label="List settings"
                    >
                      <Settings className="h-4 w-4" />
                    </button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end" className="w-52">
                    <DropdownMenuItem onClick={() => setRenameDialogOpen(true)}>
                      <Pencil className="h-3.5 w-3.5 mr-2" />
                      Rename list
                    </DropdownMenuItem>
                    {data.privacy === "PRIVATE" ? (
                      <DropdownMenuItem
                        onClick={() => setListPrivacy.mutate({ listId: data.id, privacy: "SEARCHABLE" })}
                        disabled={setListPrivacy.isPending}
                      >
                        <Globe className="h-3.5 w-3.5 mr-2" />
                        Make searchable
                      </DropdownMenuItem>
                    ) : (
                      <DropdownMenuItem
                        onClick={() => setListPrivacy.mutate({ listId: data.id, privacy: "PRIVATE" })}
                        disabled={setListPrivacy.isPending}
                      >
                        <Lock className="h-3.5 w-3.5 mr-2" />
                        Make private
                      </DropdownMenuItem>
                    )}
                    <DropdownMenuSeparator />
                    <DropdownMenuItem
                      className="text-destructive focus:text-destructive"
                      disabled={deleteList.isPending}
                      onClick={() => setDeleteListConfirmOpen(true)}
                    >
                      <Trash2 className="h-3.5 w-3.5 mr-2" />
                      Delete list
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              )}
            </div>
            <p className="text-xs text-muted-foreground">
              {summaries.length} {summaries.length === 1 ? "subject" : "subjects"}
              {isListOwner && data && (
                <span className="ml-2 text-[10px] uppercase tracking-widest text-muted-foreground/60">
                  · {data.privacy === "PRIVATE" ? "Private" : "Searchable"}
                </span>
              )}
            </p>
          </motion.div>

          <div className="flex flex-wrap items-center gap-2 mb-6">
            <Button variant="default" size="sm" className="rounded-lg text-xs" asChild>
              <Link to={listId ? `/create?listId=${listId}` : "/create"}>
                <Plus className="mr-1 h-3 w-3" /> Write review
              </Link>
            </Button>
            <Button
              variant="outline"
              size="sm"
              className="rounded-lg text-xs"
              onClick={() => setLinkDialogOpen(true)}
            >
              <Link2 className="mr-1 h-3 w-3" /> Link existing
            </Button>
            <Button
              variant="outline"
              size="sm"
              className="rounded-lg text-xs"
              onClick={() => setSyncDialogOpen(true)}
            >
              <GitMerge className="mr-1 h-3 w-3" /> Sync list
            </Button>
            <div className="ml-auto flex items-center gap-2">
              <label className="text-[10px] font-medium text-muted-foreground whitespace-nowrap">Published after</label>
              <Input
                type="date"
                value={publishedAfter}
                onChange={(e) => setPublishedAfter(e.target.value)}
                className="h-8 w-36 text-xs"
              />
              {publishedAfter && (
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-8 px-2 text-xs"
                  onClick={() => setPublishedAfter("")}
                >
                  Clear
                </Button>
              )}
            </div>
          </div>

          <LinkOpinionDialog
            open={linkDialogOpen}
            onClose={() => setLinkDialogOpen(false)}
            listId={listId!}
          />
          <SyncListDialog
            open={syncDialogOpen}
            onClose={() => setSyncDialogOpen(false)}
            listId={listId!}
          />
          <EditOpinionDialog
            row={editingOpinion}
            isPending={updateOpinion.isPending}
            onClose={() => setEditingOpinion(null)}
            onSubmit={(opinionId, mark, subjective, objective) => {
              updateOpinion.mutate(
                { opinionId, mark, subjective: [subjective], objective: [objective] },
                { onSuccess: () => setEditingOpinion(null) },
              );
            }}
          />
          <MoveOpinionDialog
            row={movingOpinion}
            currentListId={listId!}
            isPending={moveOpinion.isPending}
            onClose={() => setMovingOpinion(null)}
            onSubmit={(opinionId, targetListId) => {
              moveOpinion.mutate(
                { fromListId: listId!, toListId: targetListId, opinionId },
                { onSuccess: () => setMovingOpinion(null) },
              );
            }}
          />
          <ConfirmDialog
            open={deletingOpinionId !== null}
            title="Delete opinion forever?"
            description="This cannot be undone. The opinion will be removed from every list and the database."
            confirmLabel="Delete forever"
            destructive
            isPending={deleteOpinion.isPending}
            onClose={() => setDeletingOpinionId(null)}
            onConfirm={() => {
              if (!deletingOpinionId) return;
              deleteOpinion.mutate(
                { opinionId: deletingOpinionId },
                { onSuccess: () => setDeletingOpinionId(null) },
              );
            }}
          />
          <ConfirmDialog
            open={deleteListConfirmOpen}
            title={data ? `Delete "${data.listName}"?` : "Delete list?"}
            description="This cannot be undone. All links and syncs from this list will be removed too."
            confirmLabel="Delete list"
            destructive
            isPending={deleteList.isPending}
            onClose={() => setDeleteListConfirmOpen(false)}
            onConfirm={() => {
              if (!data) return;
              deleteList.mutate(
                { listId: data.id },
                {
                  onSuccess: () => {
                    setDeleteListConfirmOpen(false);
                    navigate("/lists");
                  },
                },
              );
            }}
          />
          {data && (
            <RenameListDialog
              open={renameDialogOpen}
              currentName={data.listName}
              isPending={renameList.isPending}
              onClose={() => setRenameDialogOpen(false)}
              onSubmit={(name) => {
                renameList.mutate(
                  { listId: data.id, name },
                  { onSuccess: () => setRenameDialogOpen(false) },
                );
              }}
            />
          )}

          <motion.div
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3, delay: 0.1 }}
            className="rounded-2xl border border-border overflow-hidden bg-card"
          >
            {summaries.length === 0 ? (
              <p className="px-5 py-8 text-center text-sm text-muted-foreground">No subjects in this list yet.</p>
            ) : (
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border bg-muted/30">
                    <th className="px-4 py-3 text-left font-medium text-muted-foreground text-xs">Name</th>
                    <th className="px-4 py-3 text-left font-medium text-muted-foreground text-xs">My rating</th>
                    <th className="px-4 py-3 text-left font-medium text-muted-foreground text-xs">Weighted rating</th>
                    <th className="px-4 py-3 w-8"></th>
                  </tr>
                </thead>
                <tbody>
                  {summaries.map((summary) => (
                    <ItemRow
                      key={summary.subject}
                      listId={listId!}
                      summary={summary}
                      isExpanded={expandedItem === summary.subject}
                      onToggle={() => setExpandedItem(expandedItem === summary.subject ? null : summary.subject)}
                      onAdjustWeight={handleAdjustWeight}
                      onAddReview={handleAddReview}
                      isAdjustingWeight={linkOpinion.isPending}
                      isAddingReview={createOpinion.isPending || linkOpinion.isPending}
                      currentUserId={currentUserId}
                      isListOwner={isListOwner}
                      onEdit={setEditingOpinion}
                      onMove={setMovingOpinion}
                      onUnlink={handleUnlinkFromList}
                      onDeleteForever={handleDeleteForever}
                    />
                  ))}
                </tbody>
              </table>
            )}
          </motion.div>
        </>
      </QueryState>
    </div>
  );
};

const ItemRow = ({
  summary,
  isExpanded,
  onToggle,
  onAdjustWeight,
  onAddReview,
  isAdjustingWeight,
  isAddingReview,
  currentUserId,
  isListOwner,
  onEdit,
  onMove,
  onUnlink,
  onDeleteForever,
}: {
  summary: OpinionSummaryDto;
  isExpanded: boolean;
  onToggle: () => void;
  onAdjustWeight: (opinionId: Uuid, weight: number) => void;
  onAddReview: (subjectId: Uuid, mark: number, subjective: string, objective: string) => Promise<void>;
  isAdjustingWeight: boolean;
  isAddingReview: boolean;
  currentUserId: Uuid | null;
  isListOwner: boolean;
  onEdit: (row: OpinionRowDto) => void;
  onMove: (row: OpinionRowDto) => void;
  onUnlink: (opinionId: Uuid) => void;
  onDeleteForever: (opinionId: Uuid) => void;
}) => {
  const [showForm, setShowForm] = useState(false);
  const [subjective, setSubjective] = useState("");
  const [objective, setObjective] = useState("");
  const [rating, setRating] = useState("");

  const hasMyReview = summary.ownMark !== null;

  const handleSubmit = async () => {
    const ratingNum = parseFloat(rating);
    if (!subjective.trim() || !objective.trim() || isNaN(ratingNum) || ratingNum < 0 || ratingNum > 10) return;
    try {
      await onAddReview(summary.subject, ratingNum, subjective.trim(), objective.trim());
      setShowForm(false);
      setSubjective("");
      setObjective("");
      setRating("");
    } catch {
      // error surfaced via mutation meta errorTitle toast — keep form open so user can retry
    }
  };

  return (
    <>
      <tr
        className={cn(
          "border-b border-border last:border-0 cursor-pointer transition-colors hover:bg-muted/20",
          isExpanded && "bg-muted/20"
        )}
        onClick={onToggle}
      >
        <td className="px-4 py-3 font-medium text-foreground">
          {summary.subjectName}
          {summary.referentName && (
            <span className="text-muted-foreground font-normal"> @ {summary.referentName}</span>
          )}
        </td>
        <td className="px-4 py-3 font-mono text-foreground">
          {summary.ownMark !== null ? summary.ownMark.toFixed(1) : "—"}
        </td>
        <td className="px-4 py-3 font-mono font-semibold text-foreground">
          {summary.componentMark !== null ? summary.componentMark.toFixed(2) : "—"}
        </td>
        <td className="px-4 py-3">
          <ChevronDown
            className={cn(
              "h-4 w-4 text-muted-foreground transition-transform",
              isExpanded && "rotate-180"
            )}
          />
        </td>
      </tr>

      <AnimatePresence>
        {isExpanded && (
          <tr>
            <td colSpan={4} className="p-0">
              <motion.div
                initial={{ height: 0, opacity: 0 }}
                animate={{ height: "auto", opacity: 1 }}
                exit={{ height: 0, opacity: 0 }}
                transition={{ duration: 0.2 }}
                className="overflow-hidden"
              >
                <div className="border-t border-border bg-muted/10 px-4 py-4">
                  {!hasMyReview && (
                    <div className="flex justify-end mb-3">
                      <Button
                        variant="outline"
                        size="sm"
                        className="rounded-lg text-xs h-7"
                        onClick={(e) => { e.stopPropagation(); setShowForm(!showForm); }}
                      >
                        <Plus className="mr-1 h-3 w-3" /> Add my review
                      </Button>
                    </div>
                  )}

                  {summary.opinions.length > 0 && (
                    <div className="rounded-xl border border-border overflow-hidden bg-card">
                      <table className="w-full text-xs">
                        <thead>
                          <tr className="border-b border-border bg-muted/20">
                            <th className="px-3 py-2 text-left font-medium text-muted-foreground">User</th>
                            <th className="px-3 py-2 text-left font-medium text-muted-foreground">Subjective opinion</th>
                            <th className="px-3 py-2 text-left font-medium text-muted-foreground hidden sm:table-cell">Objective facts</th>
                            <th className="px-3 py-2 text-left font-medium text-muted-foreground">Rating</th>
                            <th className="px-3 py-2 text-left font-medium text-muted-foreground hidden md:table-cell">Trust</th>
                            <th className="px-3 py-2 text-left font-medium text-muted-foreground hidden lg:table-cell">Status</th>
                            <th className="px-3 py-2 w-8"></th>
                          </tr>
                        </thead>
                        <tbody>
                          {summary.opinions.map((ref) => (
                            <OpinionRow
                              key={ref.opinionId}
                              ref_={ref}
                              onWeightChange={onAdjustWeight}
                              isAdjustingWeight={isAdjustingWeight}
                              currentUserId={currentUserId}
                              isListOwner={isListOwner}
                              onEdit={onEdit}
                              onMove={onMove}
                              onUnlink={onUnlink}
                              onDeleteForever={onDeleteForever}
                            />
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}

                  <AnimatePresence>
                    {showForm && (
                      <motion.div
                        initial={{ height: 0, opacity: 0 }}
                        animate={{ height: "auto", opacity: 1 }}
                        exit={{ height: 0, opacity: 0 }}
                        transition={{ duration: 0.15 }}
                        className="overflow-hidden"
                      >
                        <div className="mt-3 rounded-xl border border-border bg-card p-4 space-y-3">
                          <p className="text-xs font-medium text-foreground">Add your review</p>
                          <div className="grid gap-3 sm:grid-cols-2">
                            <div>
                              <label className="text-[10px] font-medium text-muted-foreground mb-1 block">Subjective opinion</label>
                              <Input
                                placeholder="How did it taste?"
                                value={subjective}
                                onChange={(e) => setSubjective(e.target.value)}
                                className="h-8 text-xs"
                              />
                            </div>
                            <div>
                              <label className="text-[10px] font-medium text-muted-foreground mb-1 block">Objective facts</label>
                              <Input
                                placeholder="Temperature, roast, etc."
                                value={objective}
                                onChange={(e) => setObjective(e.target.value)}
                                className="h-8 text-xs"
                              />
                            </div>
                          </div>
                          <div className="flex items-end gap-3">
                            <div>
                              <label className="text-[10px] font-medium text-muted-foreground mb-1 block">Rating (0–10)</label>
                              <Input
                                type="number"
                                min={0}
                                max={10}
                                step={0.1}
                                placeholder="0.0"
                                value={rating}
                                onChange={(e) => setRating(e.target.value)}
                                className="h-8 w-20 text-xs font-mono"
                              />
                            </div>
                            <div className="flex gap-2">
                              <Button
                                size="sm"
                                className="h-8 rounded-lg text-xs"
                                disabled={isAddingReview}
                                onClick={handleSubmit}
                              >
                                Submit
                              </Button>
                              <Button
                                variant="ghost"
                                size="sm"
                                className="h-8 rounded-lg text-xs"
                                onClick={() => setShowForm(false)}
                              >
                                Cancel
                              </Button>
                            </div>
                          </div>
                        </div>
                      </motion.div>
                    )}
                  </AnimatePresence>
                </div>
              </motion.div>
            </td>
          </tr>
        )}
      </AnimatePresence>
    </>
  );
};

const OpinionRow = ({
  ref_,
  onWeightChange,
  isAdjustingWeight,
  currentUserId,
  isListOwner,
  onEdit,
  onMove,
  onUnlink,
  onDeleteForever,
}: {
  ref_: OpinionRowDto;
  onWeightChange: (opinionId: Uuid, weight: number) => void;
  isAdjustingWeight: boolean;
  currentUserId: Uuid | null;
  isListOwner: boolean;
  onEdit: (row: OpinionRowDto) => void;
  onMove: (row: OpinionRowDto) => void;
  onUnlink: (opinionId: Uuid) => void;
  onDeleteForever: (opinionId: Uuid) => void;
}) => {
  const isOwnOpinion = currentUserId !== null && ref_.owner === currentUserId;
  const showMenu = isOwnOpinion || isListOwner;
  return (
    <tr className="border-b border-border last:border-0">
      <td className="px-3 py-2.5 text-foreground font-medium">
        <div className="flex items-center gap-2">
          <UserAvatar userId={ref_.owner} name={ref_.ownerName} size="sm" />
          {ref_.ownerName}
        </div>
      </td>
      <td className="px-3 py-2.5 text-muted-foreground">{ref_.subjective.join(", ")}</td>
      <td className="px-3 py-2.5 text-muted-foreground hidden sm:table-cell">{ref_.objective.join(", ")}</td>
      <td className="px-3 py-2.5 font-mono font-medium text-foreground">
        {ref_.mark !== null ? ref_.mark.toFixed(1) : "—"}
      </td>
      <td className="px-3 py-1.5 font-mono text-muted-foreground hidden md:table-cell">
        <Input
          type="number"
          min={0}
          max={1}
          step={0.1}
          key={ref_.weight}
          defaultValue={ref_.weight}
          disabled={isAdjustingWeight}
          onChange={(e) => {
            const val = parseFloat(e.target.value);
            if (!isNaN(val)) onWeightChange(ref_.opinionId, Math.min(1, Math.max(0, val)));
          }}
          onClick={(e) => e.stopPropagation()}
          className="h-7 w-16 px-2 text-xs font-mono bg-transparent border-border"
        />
      </td>
      <td className="px-3 py-2.5 text-muted-foreground hidden lg:table-cell capitalize">
        {ref_.status.toLowerCase().replace("_", " ")}
      </td>
      <td className="px-3 py-2.5 w-8">
        {showMenu && (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button
                type="button"
                onClick={(e) => e.stopPropagation()}
                className="rounded-md p-1 text-muted-foreground hover:bg-muted/50 hover:text-foreground transition-colors"
                aria-label="Opinion actions"
              >
                <MoreHorizontal className="h-4 w-4" />
              </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-44">
              {isOwnOpinion && (
                <DropdownMenuItem onClick={() => onEdit(ref_)}>
                  <Pencil className="h-3.5 w-3.5 mr-2" />
                  Edit
                </DropdownMenuItem>
              )}
              {isListOwner && (
                <DropdownMenuItem onClick={() => onMove(ref_)}>
                  <FolderInput className="h-3.5 w-3.5 mr-2" />
                  Move to list…
                </DropdownMenuItem>
              )}
              {isListOwner && (
                <DropdownMenuItem onClick={() => onUnlink(ref_.opinionId)}>
                  <X className="h-3.5 w-3.5 mr-2" />
                  Remove from list
                </DropdownMenuItem>
              )}
              {isOwnOpinion && (
                <>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem
                    onClick={() => onDeleteForever(ref_.opinionId)}
                    className="text-destructive focus:text-destructive"
                  >
                    <Trash2 className="h-3.5 w-3.5 mr-2" />
                    Delete forever
                  </DropdownMenuItem>
                </>
              )}
            </DropdownMenuContent>
          </DropdownMenu>
        )}
      </td>
    </tr>
  );
};

const LinkOpinionDialog = ({
  open,
  onClose,
  listId,
}: {
  open: boolean;
  onClose: () => void;
  listId: Uuid;
}) => {
  const [subjectQuery, setSubjectQuery] = useState("");

  const importOpinion = useLinkMyOpinionForSubjectToListMutation({
    onSuccess: () => {
      setSubjectQuery("");
      onClose();
    },
  });

  const trimmedSubjectQuery = subjectQuery.trim();
  const { data: subjectsData, isLoading: isSubjectsLoading } = useFindSubjects(
    { query: trimmedSubjectQuery, pageable: { page: 0, size: 8, sort: [{ property: "name", direction: "ASC" }] } },
    { enabled: open && trimmedSubjectQuery.length >= 2 },
  );
  const subjectResults = subjectsData?.items ?? [];

  const handleImport = (subjectId: Uuid) => {
    importOpinion.mutate({ listId, subjectId });
  };

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Link existing opinion</DialogTitle>
        </DialogHeader>
        <div className="space-y-4 py-2">
          <div className="space-y-2">
            <label className="text-xs font-medium text-muted-foreground">Import my opinion by subject</label>
            <Input
              placeholder="Search subject name…"
              value={subjectQuery}
              onChange={(e) => setSubjectQuery(e.target.value)}
              className="text-xs"
            />
            {trimmedSubjectQuery.length >= 2 && (
              <div className="rounded-lg border border-border overflow-hidden">
                {isSubjectsLoading ? (
                  <p className="px-3 py-2 text-xs text-muted-foreground">Searching…</p>
                ) : subjectResults.length === 0 ? (
                  <p className="px-3 py-2 text-xs text-muted-foreground">No subjects found.</p>
                ) : (
                  <div className="max-h-40 overflow-y-auto divide-y divide-border">
                    {subjectResults.map((s) => (
                      <button
                        key={s.id}
                        type="button"
                        onClick={() => handleImport(s.id)}
                        className="w-full px-3 py-2 text-left text-xs hover:bg-muted/50 transition-colors"
                        disabled={importOpinion.isPending}
                      >
                        <span className="block text-foreground">{s.name}</span>
                        <span className="block text-muted-foreground text-[11px]">
                          {s.primaryReferent?.address ?? "No address"}
                        </span>
                      </button>
                    ))}
                  </div>
                )}
              </div>
            )}
            <p className="text-[11px] text-muted-foreground">
              Picks your latest opinion for that subject and links it here.
            </p>
          </div>
        </div>
        <DialogFooter>
          <Button variant="ghost" size="sm" onClick={onClose}>Cancel</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

const SyncListDialog = ({
  open,
  onClose,
  listId,
}: {
  open: boolean;
  onClose: () => void;
  listId: Uuid;
}) => {
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState<OpinionListSummaryDto | null>(null);
  const [weight, setWeight] = useState("1.0");

  const { data } = useSearchOpinionLists(
    { filters: query.length >= 2 ? { nameSubstring: query } : undefined, pageable: { page: 0, size: 10, sort: [] } },
    { enabled: open },
  );
  const results = (data?.items ?? []).filter((l) => l.listId !== listId);

  const syncList = useSyncOpinionListsMutation({
    onSuccess: () => {
      setQuery("");
      setSelected(null);
      setWeight("1.0");
      onClose();
    },
  });

  const handleSubmit = () => {
    if (!selected) return;
    const w = parseFloat(weight);
    syncList.mutate({ existingListId: listId, addedListId: selected.listId, weight: isNaN(w) ? 1.0 : w });
  };

  return (
    <Dialog open={open} onOpenChange={(v) => { if (!v) { setQuery(""); setSelected(null); onClose(); } }}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Sync with another list</DialogTitle>
        </DialogHeader>
        <div className="space-y-3 py-2">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Search lists by name…"
              value={query}
              onChange={(e) => { setQuery(e.target.value); setSelected(null); }}
              className="pl-9 text-xs"
            />
          </div>
          {results.length > 0 && (
            <div className="rounded-lg border border-border divide-y divide-border max-h-48 overflow-y-auto">
              {results.map((list) => (
                <button
                  key={list.listId}
                  type="button"
                  onClick={() => setSelected(list)}
                  className={cn(
                    "w-full px-3 py-2 text-left text-xs transition-colors hover:bg-muted/50",
                    selected?.listId === list.listId && "bg-muted font-medium",
                  )}
                >
                  <span className="block text-foreground">{list.listName}</span>
                  <span className="text-muted-foreground">{list.ownerName}</span>
                </button>
              ))}
            </div>
          )}
          {selected && (
            <div className="flex items-center gap-3">
              <label className="text-xs font-medium text-muted-foreground whitespace-nowrap">Weight</label>
              <Input
                type="number"
                min={0}
                max={1}
                step={0.1}
                value={weight}
                onChange={(e) => setWeight(e.target.value)}
                className="h-8 w-20 text-xs font-mono"
              />
            </div>
          )}
        </div>
        <DialogFooter>
          <Button variant="ghost" size="sm" onClick={() => { setQuery(""); setSelected(null); onClose(); }}>Cancel</Button>
          <Button
            size="sm"
            disabled={!selected || syncList.isPending}
            onClick={handleSubmit}
          >
            Sync
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

const EditOpinionDialog = ({
  row,
  isPending,
  onClose,
  onSubmit,
}: {
  row: OpinionRowDto | null;
  isPending: boolean;
  onClose: () => void;
  onSubmit: (opinionId: Uuid, mark: number, subjective: string, objective: string) => void;
}) => {
  const [mark, setMark] = useState("");
  const [subjective, setSubjective] = useState("");
  const [objective, setObjective] = useState("");
  const [hydratedFor, setHydratedFor] = useState<Uuid | null>(null);

  if (row && hydratedFor !== row.opinionId) {
    setMark(row.mark !== null ? String(row.mark) : "");
    setSubjective(row.subjective.join(", "));
    setObjective(row.objective.join(", "));
    setHydratedFor(row.opinionId);
  }

  const handleSubmit = () => {
    if (!row) return;
    const m = parseFloat(mark);
    if (isNaN(m) || m < 0 || m > 10) return;
    if (!subjective.trim() || !objective.trim()) return;
    onSubmit(row.opinionId, m, subjective.trim(), objective.trim());
  };

  return (
    <Dialog
      open={row !== null}
      onOpenChange={(v) => {
        if (!v) {
          setHydratedFor(null);
          onClose();
        }
      }}
    >
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Edit opinion</DialogTitle>
        </DialogHeader>
        <div className="space-y-3 py-2">
          <div className="space-y-1.5">
            <label className="text-xs font-medium text-muted-foreground">Subjective opinion</label>
            <Input value={subjective} onChange={(e) => setSubjective(e.target.value)} className="text-xs" />
          </div>
          <div className="space-y-1.5">
            <label className="text-xs font-medium text-muted-foreground">Objective facts</label>
            <Input value={objective} onChange={(e) => setObjective(e.target.value)} className="text-xs" />
          </div>
          <div className="space-y-1.5">
            <label className="text-xs font-medium text-muted-foreground">Rating (0–10)</label>
            <Input
              type="number"
              min={0}
              max={10}
              step={0.1}
              value={mark}
              onChange={(e) => setMark(e.target.value)}
              className="w-24 text-xs font-mono"
            />
          </div>
        </div>
        <DialogFooter>
          <Button variant="ghost" size="sm" onClick={onClose}>Cancel</Button>
          <Button size="sm" onClick={handleSubmit} disabled={isPending}>Save</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

const MoveOpinionDialog = ({
  row,
  currentListId,
  isPending,
  onClose,
  onSubmit,
}: {
  row: OpinionRowDto | null;
  currentListId: Uuid;
  isPending: boolean;
  onClose: () => void;
  onSubmit: (opinionId: Uuid, targetListId: Uuid) => Promise<void>;
}) => {
  const { data: listsData } = useMyOpinionLists({ pageable: { page: 0, size: 50 } }, { enabled: row !== null });
  const myLists = (listsData?.items ?? []).filter((l) => l.listId !== currentListId);
  const [targetListId, setTargetListId] = useState("");

  const handleSubmit = () => {
    if (!row || !targetListId) return;
    void onSubmit(row.opinionId, targetListId);
  };

  return (
    <Dialog
      open={row !== null}
      onOpenChange={(v) => {
        if (!v) {
          setTargetListId("");
          onClose();
        }
      }}
    >
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Move opinion to another list</DialogTitle>
        </DialogHeader>
        <div className="space-y-2 py-2">
          {myLists.length === 0 ? (
            <p className="text-xs text-muted-foreground">You don't own any other lists yet.</p>
          ) : (
            <select
              value={targetListId}
              onChange={(e) => setTargetListId(e.target.value)}
              className="w-full rounded-lg border border-border bg-card px-3 py-2 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/40 transition-all"
            >
              <option value="">Pick a list…</option>
              {myLists.map((l) => (
                <option key={l.listId} value={l.listId}>{l.listName}</option>
              ))}
            </select>
          )}
        </div>
        <DialogFooter>
          <Button variant="ghost" size="sm" onClick={onClose}>Cancel</Button>
          <Button size="sm" onClick={handleSubmit} disabled={!targetListId || isPending}>Move</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

const ConfirmDialog = ({
  open,
  title,
  description,
  confirmLabel,
  destructive,
  isPending,
  onClose,
  onConfirm,
}: {
  open: boolean;
  title: string;
  description?: string;
  confirmLabel: string;
  destructive?: boolean;
  isPending: boolean;
  onClose: () => void;
  onConfirm: () => void;
}) => (
  <Dialog open={open} onOpenChange={(v) => { if (!v) onClose(); }}>
    <DialogContent className="sm:max-w-sm">
      <DialogHeader>
        <DialogTitle>{title}</DialogTitle>
      </DialogHeader>
      {description && <p className="text-sm text-muted-foreground">{description}</p>}
      <DialogFooter>
        <Button variant="ghost" size="sm" onClick={onClose} disabled={isPending}>Cancel</Button>
        <Button
          size="sm"
          variant={destructive ? "destructive" : "default"}
          onClick={onConfirm}
          disabled={isPending}
        >
          {confirmLabel}
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
);

const RenameListDialog = ({
  open,
  currentName,
  isPending,
  onClose,
  onSubmit,
}: {
  open: boolean;
  currentName: string;
  isPending: boolean;
  onClose: () => void;
  onSubmit: (name: string) => void;
}) => {
  const [name, setName] = useState(currentName);
  const [hydratedFor, setHydratedFor] = useState<string | null>(null);

  if (open && hydratedFor !== currentName) {
    setName(currentName);
    setHydratedFor(currentName);
  }

  const handleSubmit = () => {
    const trimmed = name.trim();
    if (!trimmed || trimmed === currentName) return;
    onSubmit(trimmed);
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(v) => {
        if (!v) {
          setHydratedFor(null);
          onClose();
        }
      }}
    >
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Rename list</DialogTitle>
        </DialogHeader>
        <div className="py-2">
          <Input
            value={name}
            autoFocus
            maxLength={255}
            onChange={(e) => setName(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter" && !isPending) {
                e.preventDefault();
                handleSubmit();
              }
            }}
          />
        </div>
        <DialogFooter>
          <Button variant="ghost" size="sm" onClick={onClose}>Cancel</Button>
          <Button
            size="sm"
            onClick={handleSubmit}
            disabled={isPending || !name.trim() || name.trim() === currentName}
          >
            Save
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default OpinionListPage;
