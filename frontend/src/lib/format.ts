const zar = new Intl.NumberFormat("en-ZA", { style: "currency", currency: "ZAR" });
const zarCompact = new Intl.NumberFormat("en-ZA", { style: "currency", currency: "ZAR", maximumFractionDigits: 0 });

export function formatPrice(price: number): string {
  return zar.format(price);
}

/** Whole rands, for chart axes and KPI tiles where cents are just noise. */
export function formatRand(amount: number): string {
  return zarCompact.format(amount);
}

export function relativeTime(iso: string | null | undefined): string {
  const then = iso ? new Date(iso).getTime() : NaN;
  if (Number.isNaN(then)) return "just now";
  const minutes = Math.round((Date.now() - then) / 60000);
  if (minutes < 1) return "just now";
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  return `${Math.round(hours / 24)}d ago`;
}

export function initials(name: string): string {
  return name
    .split(" ")
    .filter(Boolean)
    .map((part) => part.charAt(0))
    .join("")
    .slice(0, 2)
    .toUpperCase();
}
