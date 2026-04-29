import { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Search, Filter, MapPin, Calendar, BookOpen, User, X, Crosshair, Navigation, ChevronLeft, ChevronRight, List } from "lucide-react";
import OpinionListCard from "@/components/OpinionListCard";
import ReviewerAvatar from "@/components/ReviewerAvatar";
import { QueryState } from "@/components/server-state/QueryState";
import { Link } from "react-router-dom";
import { useSearchOpinionLists } from "@/lib/server-state/hooks/opinion-lists";
import { Slider } from "@/components/ui/slider";

const Discover = () => {
  const [query, setQuery] = useState("");
  const [showFilters, setShowFilters] = useState(false);
  
  // Pagination & Sorting state
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [sortProperty, setSortProperty] = useState<string>("listName");
  const [sortDirection, setSortDirection] = useState<"ASC" | "DESC">("ASC");

  // Advanced filters state
  const [authorName, setAuthorName] = useState("");
  const [subject, setSubject] = useState("");
  const [location, setLocation] = useState("");
  const [youngerThan, setYoungerThan] = useState("");

  // Lat-Long-Radius filters state
  const [latitude, setLatitude] = useState<number | "">("");
  const [longitude, setLongitude] = useState<number | "">("");
  const [radius, setRadius] = useState<number>(5000); // Default 5km
  const [useGeo, setUseGeo] = useState(false);

  // Reset page when filters change
  useEffect(() => {
    setPage(0);
  }, [query, authorName, subject, location, youngerThan, latitude, longitude, radius, useGeo, pageSize, sortProperty, sortDirection]);

  const hasGeoFilters = useGeo && latitude !== "" && longitude !== "";
  const hasAdvancedFilters = authorName || subject || location || youngerThan || hasGeoFilters;

  const filters = {
    findThisTextInAnyOfTheAbove: query.length >= 2 ? query : undefined,
    authorNameSubstring: authorName || undefined,
    containsSubjectSubstring: subject || undefined,
    locationFilter: (location || hasGeoFilters) ? { 
      containsLocationSubstring: location || undefined,
      latitude: hasGeoFilters ? (latitude as number) : undefined,
      longitude: hasGeoFilters ? (longitude as number) : undefined,
      radiusInMeters: hasGeoFilters ? radius : undefined,
    } : undefined,
    someOpinionsYoungerThan: youngerThan ? new Date(youngerThan).toISOString() : undefined,
  };

  const { data, isLoading, isError, error, refetch } = useSearchOpinionLists({
    filters: (query.length >= 2 || hasAdvancedFilters) ? filters : undefined,
    pageable: { 
      page, 
      size: pageSize,
      sort: [{ property: sortProperty, direction: sortDirection }]
    },
  });

  const lists = data?.items ?? [];
  const total = data?.total ?? 0;
  const totalPages = Math.ceil(total / pageSize);

  const clearFilters = () => {
    setAuthorName("");
    setSubject("");
    setLocation("");
    setYoungerThan("");
    setLatitude("");
    setLongitude("");
    setUseGeo(false);
    setPageSize(20);
    setSortProperty("listName");
    setSortDirection("ASC");
    setPage(0);
  };

  const handleGetCurrentLocation = () => {
    if (!navigator.geolocation) return;
    
    navigator.geolocation.getCurrentPosition((position) => {
      setLatitude(position.coords.latitude);
      setLongitude(position.coords.longitude);
      setUseGeo(true);
    }, (error) => {
      console.error("Error getting location:", error);
    });
  };

  const Pagination = () => {
    if (totalPages <= 1) return null;

    return (
      <div className="flex items-center justify-between mt-6 mb-4 px-1">
        <p className="text-xs text-muted-foreground">
          Showing <span className="font-medium text-foreground">{page * pageSize + 1}</span> to{" "}
          <span className="font-medium text-foreground">{Math.min((page + 1) * pageSize, total)}</span> of{" "}
          <span className="font-medium text-foreground">{total}</span> results
        </p>
        <div className="flex items-center gap-2">
          <button
            onClick={() => setPage(p => Math.max(0, p - 1))}
            disabled={page === 0}
            className="p-1.5 rounded-lg border border-border bg-card text-muted-foreground hover:text-foreground disabled:opacity-30 disabled:pointer-events-none transition-colors"
          >
            <ChevronLeft className="h-4 w-4" />
          </button>
          <span className="text-xs font-medium px-2">
            Page {page + 1} of {totalPages}
          </span>
          <button
            onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            className="p-1.5 rounded-lg border border-border bg-card text-muted-foreground hover:text-foreground disabled:opacity-30 disabled:pointer-events-none transition-colors"
          >
            <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      </div>
    );
  };

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 md:px-8 md:py-12">
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="mb-10"
      >
        <h1 className="text-2xl md:text-3xl font-semibold tracking-tight text-foreground mb-2">Discover</h1>
        <p className="text-sm text-muted-foreground">Find reviewers and lists worth following.</p>
      </motion.div>

      {/* Search & Filter Toggle */}
      <div className="flex flex-col gap-3 mb-6">
        <div className="relative flex-1">
          <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search everything..."
            className="w-full rounded-xl border border-border bg-card py-3 pl-11 pr-4 text-sm text-foreground placeholder:text-muted-foreground/60 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/40 transition-all"
          />
        </div>
        <button
          onClick={() => setShowFilters(!showFilters)}
          className={`flex items-center justify-center gap-2 rounded-xl border py-2.5 px-4 text-sm font-medium transition-all ${
            showFilters || hasAdvancedFilters
              ? "bg-primary/10 border-primary/30 text-primary"
              : "bg-card border-border text-muted-foreground hover:text-foreground"
          }`}
        >
          <Filter className="h-4 w-4" />
          {hasAdvancedFilters ? "Filters Active" : "Advanced Filters"}
        </button>
      </div>

      {/* Advanced Filters Panel */}
      <AnimatePresence>
        {showFilters && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: "auto" }}
            exit={{ opacity: 0, height: 0 }}
            className="overflow-hidden mb-8"
          >
            <div className="grid gap-4 p-5 rounded-2xl border border-border bg-card/50 sm:grid-cols-2">
              <div className="space-y-2">
                <label className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
                  <User className="h-3 w-3" /> Author Name
                </label>
                <input
                  type="text"
                  value={authorName}
                  onChange={(e) => setAuthorName(e.target.value)}
                  placeholder="e.g. Alex"
                  className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20"
                />
              </div>
              <div className="space-y-2">
                <label className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
                  <BookOpen className="h-3 w-3" /> Subject
                </label>
                <input
                  type="text"
                  value={subject}
                  onChange={(e) => setSubject(e.target.value)}
                  placeholder="e.g. Coffee"
                  className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20"
                />
              </div>
              <div className="space-y-2">
                <label className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
                  <MapPin className="h-3 w-3" /> Location
                </label>
                <input
                  type="text"
                  value={location}
                  onChange={(e) => setLocation(e.target.value)}
                  placeholder="e.g. Berlin"
                  className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20"
                />
              </div>
              <div className="space-y-2">
                <label className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
                  <Calendar className="h-3 w-3" /> Newer Than
                </label>
                <input
                  type="date"
                  value={youngerThan}
                  onChange={(e) => setYoungerThan(e.target.value)}
                  className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20"
                />
              </div>

              <div className="sm:col-span-2 pt-2 border-t border-border/50">
                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="space-y-2">
                    <label className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
                      <List className="h-3 w-3" /> Page Size
                    </label>
                    <select
                      value={pageSize}
                      onChange={(e) => setPageSize(Number(e.target.value))}
                      className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 appearance-none"
                    >
                      <option value={10}>10 per page</option>
                      <option value={20}>20 per page</option>
                      <option value={50}>50 per page</option>
                      <option value={100}>100 per page</option>
                    </select>
                  </div>
                  <div className="space-y-2">
                    <label className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
                      <List className="h-3 w-3" /> Sort By
                    </label>
                    <div className="flex gap-2">
                      <select
                        value={sortProperty}
                        onChange={(e) => setSortProperty(e.target.value)}
                        className="flex-1 rounded-lg border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 appearance-none"
                      >
                        <option value="listName">List Name</option>
                        <option value="ownerName">Owner Name</option>
                        <option value="opinionsCount">Opinions Count</option>
                      </select>
                      <select
                        value={sortDirection}
                        onChange={(e) => setSortDirection(e.target.value as "ASC" | "DESC")}
                        className="w-24 rounded-lg border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 appearance-none text-center"
                      >
                        <option value="ASC">ASC</option>
                        <option value="DESC">DESC</option>
                      </select>
                    </div>
                  </div>
                </div>
              </div>

              <div className="sm:col-span-2 pt-2 border-t border-border/50">
                <div className="flex items-center justify-between mb-3">
                  <label className="text-xs font-semibold text-foreground flex items-center gap-1.5">
                    <Navigation className="h-3 w-3" /> Geospatial Search
                  </label>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={handleGetCurrentLocation}
                      className="text-[10px] px-2 py-1 rounded bg-secondary text-secondary-foreground hover:bg-secondary/80 transition-colors flex items-center gap-1"
                    >
                      <Crosshair className="h-2.5 w-2.5" /> Use My Location
                    </button>
                    <input 
                      type="checkbox" 
                      id="use-geo" 
                      checked={useGeo} 
                      onChange={(e) => setUseGeo(e.target.checked)}
                      className="h-3 w-3 rounded border-gray-300 text-primary focus:ring-primary"
                    />
                    <label htmlFor="use-geo" className="text-[10px] font-medium cursor-pointer">Enable</label>
                  </div>
                </div>
                
                <div className={`grid gap-4 sm:grid-cols-3 transition-opacity ${useGeo ? 'opacity-100' : 'opacity-40 pointer-events-none'}`}>
                  <div className="space-y-1.5">
                    <span className="text-[10px] font-medium text-muted-foreground">Latitude</span>
                    <input
                      type="number"
                      step="any"
                      value={latitude}
                      onChange={(e) => setLatitude(e.target.value === "" ? "" : parseFloat(e.target.value))}
                      placeholder="e.g. 52.52"
                      className="w-full rounded-lg border border-border bg-background px-3 py-1.5 text-xs focus:outline-none focus:ring-2 focus:ring-primary/20"
                    />
                  </div>
                  <div className="space-y-1.5">
                    <span className="text-[10px] font-medium text-muted-foreground">Longitude</span>
                    <input
                      type="number"
                      step="any"
                      value={longitude}
                      onChange={(e) => setLongitude(e.target.value === "" ? "" : parseFloat(e.target.value))}
                      placeholder="e.g. 13.40"
                      className="w-full rounded-lg border border-border bg-background px-3 py-1.5 text-xs focus:outline-none focus:ring-2 focus:ring-primary/20"
                    />
                  </div>
                  <div className="space-y-1.5">
                    <div className="flex justify-between">
                      <span className="text-[10px] font-medium text-muted-foreground">Radius</span>
                      <span className="text-[10px] font-mono text-primary">{(radius / 1000).toFixed(1)} km</span>
                    </div>
                    <div className="pt-2 px-1">
                      <Slider
                        value={[radius]}
                        min={100}
                        max={50000}
                        step={100}
                        onValueChange={(vals) => setRadius(vals[0])}
                      />
                    </div>
                  </div>
                </div>
              </div>

              {hasAdvancedFilters && (
                <div className="sm:col-span-2 flex justify-end">
                  <button
                    onClick={clearFilters}
                    className="text-xs font-medium text-muted-foreground hover:text-destructive flex items-center gap-1 transition-colors"
                  >
                    <X className="h-3 w-3" /> Clear filters
                  </button>
                </div>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Lists */}
      <motion.section
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3, delay: 0.15 }}
      >
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold tracking-tight text-foreground">
            {query.length >= 2 || hasAdvancedFilters ? "Search Results" : "Latest Lists"}
          </h2>
        </div>

        <QueryState
          isLoading={isLoading}
          isError={isError}
          error={error}
          isEmpty={lists.length === 0}
          emptyMessage={query.length >= 2 || hasAdvancedFilters ? "No lists match your search." : "No lists found."}
          onRetry={refetch}
        >
          <Pagination />
          <div className="grid gap-4 sm:grid-cols-2">
            {lists.map((list) => (
              <Link key={list.listId} to={`/list/${list.listId}`}>
                <OpinionListCard
                  title={list.listName}
                  description=""
                  reviewCount={list.opinionsCount}
                  authorId={list.owner}
                  authorName={list.ownerName}
                  hasAccess={false}
                />
              </Link>
            ))}
          </div>
          <Pagination />
        </QueryState>
      </motion.section>
    </div>
  );
};

export default Discover;
