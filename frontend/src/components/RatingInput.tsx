import { cn } from "@/lib/utils";

export const RATING_MIN = 0;
export const RATING_MAX = 10;
export const RATING_STEP = 0.1;

const clampRating = (n: number) => {
  const clamped = Math.min(RATING_MAX, Math.max(RATING_MIN, n));
  return Math.round(clamped * 10) / 10;
};

interface RatingInputProps {
  value: number | null;
  onChange: (v: number | null) => void;
  ariaLabel?: string;
  className?: string;
}

const RatingInput = ({
  value,
  onChange,
  ariaLabel = "Rating",
  className,
}: RatingInputProps) => {
  const sliderValue = value ?? RATING_MIN;
  const handleNumberInput = (raw: string) => {
    if (raw === "") {
      onChange(null);
      return;
    }
    const parsed = parseFloat(raw);
    if (Number.isNaN(parsed)) return;
    onChange(clampRating(parsed));
  };
  return (
    <div className={cn("flex items-center gap-3", className)}>
      <input
        type="number"
        min={RATING_MIN}
        max={RATING_MAX}
        step={RATING_STEP}
        value={value ?? ""}
        onChange={(e) => handleNumberInput(e.target.value)}
        className="h-9 w-20 rounded-lg border border-border bg-card px-2 text-sm font-mono font-semibold text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20"
        aria-label={`${ariaLabel} (number)`}
      />
      <input
        type="range"
        min={RATING_MIN}
        max={RATING_MAX}
        step={RATING_STEP}
        value={sliderValue}
        onChange={(e) => onChange(clampRating(parseFloat(e.target.value)))}
        className="flex-1 accent-primary"
        aria-label={ariaLabel}
      />
    </div>
  );
};

export default RatingInput;
