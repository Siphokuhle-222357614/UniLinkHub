package za.co.unilinkhub.moderation;

import org.springframework.stereotype.Component;
import za.co.unilinkhub.common.exception.ForbiddenException;
import za.co.unilinkhub.common.web.RejectionMessages;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What can't be sold on UniLinkHub. Listings and businesses are checked against this list when
 * they're created or edited, and anything that matches is refused with an explanation.
 *
 * This is a first line of defence, not a guarantee: a determined seller can misspell a word, so
 * students can also report a listing as "Restricted or illegal item" and admins can take any
 * listing down (see ListingModerationService). Matching is whole-word, and the ALLOWED phrases
 * stop everyday products that happen to contain a restricted word ("ginger beer", "glue gun")
 * from being blocked.
 */
@Component
public class RestrictedItemsPolicy {

    public record Category(String key, String label, String description, List<String> examples, List<String> terms) {
    }

    public record Violation(Category category, String matchedWord) {
    }

    public static final List<Category> CATEGORIES = List.of(
            new Category("ALCOHOL", "Alcohol",
                    "Any alcoholic drink - beer, wine, ciders, spirits or homemade brews.",
                    List.of("Beer", "Wine", "Vodka", "Ciders", "Shots"),
                    List.of("alcohol", "alcoholic drink", "liquor", "booze", "beer", "wine", "vodka", "whisky", "whiskey",
                            "brandy", "gin", "rum", "tequila", "cider", "champagne", "jagermeister", "hennessy")),
            new Category("DRUGS", "Drugs and illegal substances",
                    "Dagga/weed (including edibles), any illegal drug, or someone else's prescription medicine.",
                    List.of("Weed / dagga", "Tik", "Mandrax", "Prescription pills"),
                    List.of("drugs", "weed", "dagga", "marijuana", "cannabis", "ganja", "kush", "thc", "cocaine", "heroin",
                            "meth", "methamphetamine", "tik", "mandrax", "mandies", "ecstasy", "mdma", "lsd", "shrooms",
                            "magic mushrooms", "nyaope", "whoonga", "xanax", "codeine", "prescription pills",
                            "prescription medicine", "antibiotics")),
            new Category("WEAPONS", "Weapons and ammunition",
                    "Guns (real or replica), ammunition, explosives, knuckle dusters, flick knives and stun guns.",
                    List.of("Guns", "Ammunition", "Tasers", "Knuckle dusters"),
                    List.of("gun", "firearm", "pistol", "rifle", "shotgun", "revolver", "ammunition", "ammo", "bullets",
                            "grenade", "explosive", "explosives", "taser", "stun gun", "knuckle duster", "brass knuckles",
                            "switchblade", "flick knife")),
            new Category("TOBACCO", "Cigarettes, vapes and hubbly",
                    "Cigarettes, loose draws, vapes, e-cigarettes, hubbly/hookah and tobacco - residences are smoke-free.",
                    List.of("Cigarettes", "Vapes", "Hubbly"),
                    List.of("cigarette", "cigarettes", "cigs", "loose draw", "loose draws", "vape", "vapes", "vaping",
                            "e-cigarette", "e cigarette", "hubbly", "hookah", "shisha", "tobacco", "snuff")),
            new Category("ACADEMIC_DISHONESTY", "Cheating services",
                    "Doing someone's assignments, tests or exams for them, or selling answers. Tutoring is welcome - cheating isn't.",
                    List.of("\"I'll do your assignment\"", "Exam answers", "Ghostwriting"),
                    List.of("do your assignment", "do your assignments", "write your assignment", "write your assignments",
                            "complete your assignment", "complete your assignments", "assignment writing", "essay writing service",
                            "exam answers", "test answers", "leaked exam", "leaked paper", "ghostwriting", "ghost writing",
                            "write your exam", "write your test")),
            new Category("FRAUD", "Fake documents and stolen goods",
                    "Fake IDs, sick notes or certificates, forged documents, and anything stolen or counterfeit.",
                    List.of("Fake IDs", "Sick notes", "Stolen phones"),
                    List.of("fake id", "fake ids", "sick note", "sick notes", "doctor's note", "doctors note",
                            "medical certificate", "fake certificate", "forged", "stolen", "counterfeit", "fake designer")),
            new Category("ADULT", "Adult content and services",
                    "Sexual content or services of any kind.",
                    List.of("Explicit photos or videos", "Sexual services"),
                    // Not "escort": campus walk-you-home escort services are a legitimate thing to offer.
                    List.of("nudes", "onlyfans", "adult content", "sexual services", "sex work", "porn"))
    );

    /** Everyday phrases that contain a restricted word but aren't restricted. Removed before matching. */
    private static final List<String> ALLOWED = List.of(
            "ginger beer", "root beer", "alcohol-free", "alcohol free", "non-alcoholic", "non alcoholic",
            "wine gums", "wine gum", "wine red", "wine colour", "wine color", "wine-coloured", "wine coloured",
            "rum and raisin", "rum & raisin", "brandy snap", "brandy snaps", "apple cider vinegar", "cider vinegar",
            "glue gun", "glue guns", "hot glue gun", "heat gun", "staple gun", "spray gun", "massage gun", "water gun",
            "nerf gun", "toy gun", "bullet journal", "bullet points", "bullet point", "gun metal",
            "drug-free", "drug free", "stolen hearts"
    );

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public Optional<Violation> check(String... texts) {
        StringBuilder combined = new StringBuilder();
        for (String text : texts) {
            if (text != null) {
                combined.append(' ').append(text);
            }
        }
        String normalized = " " + WHITESPACE.matcher(combined.toString().toLowerCase(Locale.ROOT)
                .replace('’', '\'')).replaceAll(" ") + " ";
        for (String allowed : ALLOWED) {
            normalized = normalized.replace(allowed, " ");
        }
        for (Category category : CATEGORIES) {
            for (String term : category.terms()) {
                Matcher m = patternFor(term).matcher(normalized);
                if (m.find()) {
                    return Optional.of(new Violation(category, m.group().trim()));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * @param what what's being saved, phrased to follow "This ... can't be published", e.g. "listing".
     */
    public void requireAllowed(String what, String... texts) {
        check(texts).ifPresent(v -> {
            throw new ForbiddenException("This " + what + " can't be published because it looks like it offers "
                    + v.category().label().toLowerCase(Locale.ROOT) + " (we spotted \"" + v.matchedWord() + "\"). "
                    + "Selling alcohol, drugs, weapons and other restricted items isn't allowed on UniLinkHub or in CPUT residences. "
                    + "Anyone caught selling them can have their account suspended and be reported to CPUT residence management "
                    + "- and illegal items to the police. If this is a mistake, please reword it or contact an admin.",
                    RejectionMessages.CODE_RESTRICTED_ITEM);
        });
    }

    private static Pattern patternFor(String term) {
        String body = Pattern.quote(term).replace(" ", "\\E[\\s-]+\\Q");
        // Whole words only (so "gin" doesn't match "ginger"), with an optional plural ending.
        return Pattern.compile("(?<![a-z0-9])" + body + "(?:s|es)?(?![a-z0-9])");
    }
}
