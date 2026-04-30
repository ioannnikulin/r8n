import { useEffect, useRef, useState } from "react";
import { motion } from "framer-motion";
import { ArrowLeft, CloudUpload, Loader2, Trash2 } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import UserAvatar from "@/components/UserAvatar";
import { Button } from "@/components/ui/button";
import { toast } from "@/hooks/use-toast";
import {
  useDeleteMyAvatarMutation,
  useMe,
  useUpdateMyPublicProfileMutation,
  useUploadMyAvatarMutation,
  useUserAvatar,
  useUserProfile,
} from "@/lib/server-state/hooks/users";

const MAX_AVATAR_SIZE_BYTES = readPositiveIntegerEnv(
  "VITE_AVATAR_MAX_SIZE_BYTES",
  import.meta.env.VITE_AVATAR_MAX_SIZE_BYTES,
);
const MAX_AVATAR_SIZE_LABEL = formatFileSize(MAX_AVATAR_SIZE_BYTES);
const PROFILE_NAME_MAX_LENGTH = readPositiveIntegerEnv(
  "VITE_PROFILE_NAME_MAX_LENGTH",
  import.meta.env.VITE_PROFILE_NAME_MAX_LENGTH,
);
const PROFILE_ABOUT_MAX_LENGTH = readPositiveIntegerEnv(
  "VITE_PROFILE_ABOUT_MAX_LENGTH",
  import.meta.env.VITE_PROFILE_ABOUT_MAX_LENGTH,
);
const PROFILE_LOCATION_MAX_LENGTH = readPositiveIntegerEnv(
  "VITE_PROFILE_LOCATION_MAX_LENGTH",
  import.meta.env.VITE_PROFILE_LOCATION_MAX_LENGTH,
);
const ALLOWED_AVATAR_TYPES = new Set([
  "image/jpeg",
  "image/png",
  "image/webp",
]);

function readPositiveIntegerEnv(name: string, value: string | undefined): number {
  const parsedValue = Number(value);

  if (!Number.isInteger(parsedValue) || parsedValue <= 0) {
    throw new Error(`${name} must be a positive integer.`);
  }

  return parsedValue;
}

function formatFileSize(bytes: number): string {
  const megabytes = bytes / (1024 * 1024);
  if (Number.isInteger(megabytes)) {
    return `${megabytes}MB`;
  }

  const kilobytes = bytes / 1024;
  if (Number.isInteger(kilobytes)) {
    return `${kilobytes}KB`;
  }

  return `${bytes}B`;
}

function validateAvatarFile(file: File): string | undefined {
  if (!ALLOWED_AVATAR_TYPES.has(file.type)) {
    return "Please choose a PNG, JPEG, or WebP image.";
  }

  if (file.size > MAX_AVATAR_SIZE_BYTES) {
    return `Profile image must be ${MAX_AVATAR_SIZE_LABEL} or smaller.`;
  }

  return undefined;
}

const EditProfile = () => {
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const isProfileInitializedRef = useRef(false);
  const [name, setName] = useState("");
  const [about, setAbout] = useState("");
  const [location, setLocation] = useState("");
  const { data: me } = useMe();
  const { data: profile } = useUserProfile(me?.id ?? "");
  const { data: avatar } = useUserAvatar(me?.id ?? "", {
    enabled: !!me?.id,
    retry: false,
  });
  const uploadAvatarMutation = useUploadMyAvatarMutation({
    onSuccess: () => {
      toast({
        title: "Profile image updated",
        description: "Your new image has been saved.",
      });
    },
  });
  const deleteAvatarMutation = useDeleteMyAvatarMutation({
    onSuccess: () => {
      toast({
        title: "Profile image removed",
        description: "Your profile now shows initials again.",
      });
    },
  });
  const updateProfileMutation = useUpdateMyPublicProfileMutation({
    onSuccess: () => {
      toast({ title: "Profile updated", description: "Your changes have been saved." });
      navigate("/profile");
    },
  });
  const isAvatarMutating =
    uploadAvatarMutation.isPending || deleteAvatarMutation.isPending;
  const displayName = name.trim() || me?.name || "?";

  useEffect(() => {
    if (!profile || isProfileInitializedRef.current) {
      return;
    }

    setName(profile.name);
    setAbout(profile.about ?? "");
    setLocation(profile.location ?? "");
    isProfileInitializedRef.current = true;
  }, [profile]);

  const handleAvatarChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.currentTarget.files?.[0];
    event.currentTarget.value = "";

    if (!file) {
      return;
    }

    const validationError = validateAvatarFile(file);
    if (validationError) {
      toast({
        title: "Image not uploaded",
        description: validationError,
      });
      return;
    }

    uploadAvatarMutation.mutate({ file });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const normalizedName = name.trim();
    const normalizedAbout = about.trim();
    const normalizedLocation = location.trim();

    if (!normalizedName) {
      toast({ title: "Name required", description: "Please enter your display name." });
      return;
    }
    if (normalizedName.length > PROFILE_NAME_MAX_LENGTH) {
      toast({
        title: "Profile not saved",
        description: `Display name must be ${PROFILE_NAME_MAX_LENGTH} characters or fewer.`,
      });
      return;
    }
    if (normalizedAbout.length > PROFILE_ABOUT_MAX_LENGTH) {
      toast({
        title: "Profile not saved",
        description: `Bio must be ${PROFILE_ABOUT_MAX_LENGTH} characters or fewer.`,
      });
      return;
    }
    if (normalizedLocation.length > PROFILE_LOCATION_MAX_LENGTH) {
      toast({
        title: "Profile not saved",
        description: `Location must be ${PROFILE_LOCATION_MAX_LENGTH} characters or fewer.`,
      });
      return;
    }

    updateProfileMutation.mutate({
      about: normalizedAbout === "" ? null : normalizedAbout,
      location: normalizedLocation === "" ? null : normalizedLocation,
      name: normalizedName,
    });
  };

  return (
    <div className="mx-auto max-w-2xl px-4 py-8 md:px-8 md:py-12">
      <Link
        to="/profile"
        className="mb-6 inline-flex items-center gap-1.5 text-xs text-muted-foreground hover:text-foreground transition-colors"
      >
        <ArrowLeft className="h-3 w-3" />
        Back to profile
      </Link>

      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
      >
        <h1 className="text-2xl md:text-3xl font-semibold tracking-tight text-foreground mb-1">Edit Profile</h1>
        <p className="text-sm text-muted-foreground mb-8">Update your public profile information.</p>

        <form onSubmit={handleSubmit} className="space-y-8">
          {/* Avatar */}
          <div className="flex items-center gap-5">
            <div className="relative">
              <UserAvatar userId={me?.id} name={displayName} size="lg" />
              <input
                ref={fileInputRef}
                type="file"
                accept="image/png,image/jpeg,image/webp"
                className="hidden"
                onChange={handleAvatarChange}
              />
              <button
                type="button"
                aria-label="Upload profile image"
                disabled={!me?.id || isAvatarMutating}
                onClick={() => fileInputRef.current?.click()}
                className="absolute -bottom-1 -right-1 flex h-8 w-8 items-center justify-center rounded-full border-2 border-background bg-primary text-primary-foreground hover:bg-primary/90 transition-colors"
              >
                {uploadAvatarMutation.isPending ? (
                  <Loader2 className="h-3.5 w-3.5 animate-spin" />
                ) : (
                  <CloudUpload className="h-3.5 w-3.5" />
                )}
              </button>
            </div>
            <div>
              <p className="text-sm font-medium text-foreground">Profile image</p>
              <p className="text-xs text-muted-foreground">
                PNG, JPEG, or WebP. Max {MAX_AVATAR_SIZE_LABEL}.
              </p>
              {avatar && (
                <button
                  type="button"
                  disabled={isAvatarMutating}
                  onClick={() => deleteAvatarMutation.mutate()}
                  className="mt-2 inline-flex items-center gap-1.5 text-xs font-medium text-destructive hover:text-destructive/80 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {deleteAvatarMutation.isPending ? (
                    <Loader2 className="h-3 w-3 animate-spin" />
                  ) : (
                    <Trash2 className="h-3 w-3" />
                  )}
                  Remove image
                </button>
              )}
            </div>
          </div>

          {/* Name */}
          <div>
            <label htmlFor="profile-name" className="mb-1.5 block text-sm font-medium text-foreground">Display name</label>
            <input
              id="profile-name"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              maxLength={PROFILE_NAME_MAX_LENGTH}
              placeholder="Your name"
              className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm text-foreground placeholder:text-muted-foreground/60 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/40 transition-all"
            />
          </div>

          {/* Bio */}
          <div>
            <label htmlFor="profile-about" className="mb-1.5 block text-sm font-medium text-foreground">Bio</label>
            <textarea
              id="profile-about"
              value={about}
              onChange={(e) => setAbout(e.target.value)}
              maxLength={PROFILE_ABOUT_MAX_LENGTH}
              placeholder="Tell others what you care about in your reviews..."
              rows={3}
              className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm text-foreground placeholder:text-muted-foreground/60 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/40 transition-all resize-none leading-relaxed"
            />
            <p className="mt-1 text-[11px] text-muted-foreground/60">{about.length}/{PROFILE_ABOUT_MAX_LENGTH} characters</p>
          </div>

          {/* Location */}
          <div>
            <label htmlFor="profile-location" className="mb-1.5 block text-sm font-medium text-foreground">Location</label>
            <input
              id="profile-location"
              type="text"
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              maxLength={PROFILE_LOCATION_MAX_LENGTH}
              placeholder="e.g., Berlin, Germany"
              className="w-full rounded-xl border border-border bg-card px-4 py-3 text-sm text-foreground placeholder:text-muted-foreground/60 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary/40 transition-all"
            />
          </div>

          {/* Submit */}
          <div className="flex gap-3 pt-2">
            <Button type="submit" disabled={updateProfileMutation.isPending} className="rounded-xl px-8">
              {updateProfileMutation.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                "Save Profile"
              )}
            </Button>
            <Button type="button" variant="ghost" onClick={() => navigate("/profile")} className="rounded-xl">
              Cancel
            </Button>
          </div>
        </form>
      </motion.div>
    </div>
  );
};

export default EditProfile;
