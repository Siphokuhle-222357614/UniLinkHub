import { ref } from "vue";
import { api, extractErrorMessage } from "@/lib/api";
import type { UploadedImage } from "@/lib/types";

const MAX_DIMENSION = 1600;

/**
 * Shrinks a photo in the browser before uploading: a 6 MB phone photo becomes ~300 KB, uploads
 * quickly on campus Wi-Fi, and iPhone HEIC photos get converted to a format every browser shows.
 */
async function resize(file: File): Promise<Blob> {
  const bitmap = await createImageBitmap(file);
  const scale = Math.min(1, MAX_DIMENSION / Math.max(bitmap.width, bitmap.height));
  const canvas = document.createElement("canvas");
  canvas.width = Math.round(bitmap.width * scale);
  canvas.height = Math.round(bitmap.height * scale);
  canvas.getContext("2d")!.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
  bitmap.close();
  const encode = (type: string) => new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, type, 0.85));
  // WebP keeps logo transparency; browsers that can't encode it (older Safari) fall back to JPEG.
  const webp = await encode("image/webp");
  if (webp && webp.type === "image/webp") return webp;
  const jpeg = await encode("image/jpeg");
  if (!jpeg) throw new Error("encode");
  return jpeg;
}

/** Resize + upload a photo; returns its URL, or null with `error` set to a plain-English reason. */
export function useImageUpload() {
  const uploading = ref(false);
  const error = ref("");

  async function upload(file: File | undefined | null): Promise<string | null> {
    if (!file) return null;
    error.value = "";
    if (!file.type.startsWith("image/") && !/\.(heic|heif)$/i.test(file.name)) {
      error.value = "That file isn't a photo. Please choose a JPG, PNG or WebP image.";
      return null;
    }
    uploading.value = true;
    try {
      let blob: Blob;
      try {
        blob = await resize(file);
      } catch {
        error.value = "We couldn't open that photo on this device. Try a JPG or PNG instead.";
        return null;
      }
      const form = new FormData();
      form.append("file", blob, blob.type === "image/webp" ? "photo.webp" : "photo.jpg");
      const { data } = await api.post<UploadedImage>("/images", form);
      return data.url;
    } catch (err) {
      error.value = extractErrorMessage(err);
      return null;
    } finally {
      uploading.value = false;
    }
  }

  return { upload, uploading, error };
}

export const IMAGE_ACCEPT = "image/jpeg,image/png,image/webp,image/heic,image/heif";
