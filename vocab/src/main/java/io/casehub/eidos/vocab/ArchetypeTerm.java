package io.casehub.eidos.vocab;

import io.casehub.eidos.api.VocabularyMetadata;
import io.casehub.eidos.api.VocabularyTerm;

import java.util.Arrays;
import java.util.List;

@VocabularyMetadata(uri = "urn:casehub:vocab:archetype",
                    name = "Hartwell & Chen Archetypes", version = "1.0",
                    description = "48 sub-archetypes in 12 families from Archetypes in Branding (2012). "
                        + "Four motivation quadrants: Independence, Mastery, Belonging, Stability. "
                        + "Derived via set-intersection of personality framework values (MBTI, Enneagram, DISC, Belbin, Big Five, SDI).")
public enum ArchetypeTerm implements VocabularyTerm {

    // --- Caregiver family (Stability) ---

    ANGEL("angel", "Angel",
        "Serves without expectation of return; radiates unconditional acceptance",
        ArchetypeFamily.CAREGIVER,
        List.of("gentle", "serene", "selfless", "patient", "radiant", "forgiving", "compassionate"),
        List.of("aggressive", "cynical", "calculating", "ruthless")),

    GUARDIAN("guardian", "Guardian",
        "Shields others from harm; enforces safety through vigilance",
        ArchetypeFamily.CAREGIVER,
        List.of("vigilant", "steadfast", "protective", "disciplined", "reliable", "firm", "alert"),
        List.of("reckless", "chaotic", "negligent", "indifferent")),

    HEALER("healer", "Healer",
        "Mends what is broken — emotional, physical, or systemic",
        ArchetypeFamily.CAREGIVER,
        List.of("empathic", "restorative", "intuitive", "nurturing", "transformative", "patient", "holistic"),
        List.of("destructive", "callous", "impatient", "clinical")),

    SAMARITAN("samaritan", "Samaritan",
        "Practical help in the moment; shows up where needed",
        ArchetypeFamily.CAREGIVER,
        List.of("pragmatic", "responsive", "generous", "grounded", "warm", "community-minded", "tireless"),
        List.of("detached", "theoretical", "self-serving", "aloof")),

    // --- Creator family (Stability) ---

    ARTIST("artist", "Artist",
        "Creates from inner emotional truth; form matters as much as function",
        ArchetypeFamily.CREATOR,
        List.of("expressive", "sensitive", "authentic", "aesthetic", "emotional", "original", "intense"),
        List.of("conformist", "insensitive", "utilitarian")),

    ENTREPRENEUR("entrepreneur", "Entrepreneur",
        "Creates ventures and systems; sees market gaps and fills them",
        ArchetypeFamily.CREATOR,
        List.of("inventive", "opportunistic", "bold", "driven", "pragmatic", "adaptive", "ambitious"),
        List.of("passive", "risk-averse", "complacent")),

    STORYTELLER("storyteller", "Storyteller",
        "Shapes understanding through story; gives experience a structure",
        ArchetypeFamily.CREATOR,
        List.of("narrative", "eloquent", "engaging", "imaginative", "empathic", "evocative", "perceptive"),
        List.of("inarticulate", "literal", "dry", "monotone")),

    VISIONARY("visionary", "Visionary",
        "Imagines what does not yet exist; sees beyond current constraints",
        ArchetypeFamily.CREATOR,
        List.of("prophetic", "conceptual", "revolutionary", "far-sighted", "paradigm-shifting", "bold"),
        List.of("myopic", "conventional", "incremental", "timid")),

    // --- Everyman family (Belonging) ---

    ADVOCATE("advocate", "Advocate",
        "Speaks up for those who cannot; bridges power gaps",
        ArchetypeFamily.EVERYMAN,
        List.of("principled", "passionate", "persistent", "courageous", "empathic", "articulate", "just"),
        List.of("apathetic", "compliant", "self-interested")),

    NETWORKER("networker", "Networker",
        "Weaves relationships; knows who to connect to whom",
        ArchetypeFamily.EVERYMAN,
        List.of("gregarious", "perceptive", "resourceful", "warm", "strategic", "inclusive", "energetic"),
        List.of("isolated", "rigid", "secretive", "antisocial")),

    SERVANT("servant", "Servant",
        "Does the unglamorous work that holds things together",
        ArchetypeFamily.EVERYMAN,
        List.of("humble", "reliable", "devoted", "quiet", "essential", "steadfast", "thorough"),
        List.of("proud", "showy", "demanding", "self-promoting")),

    CITIZEN("citizen", "Citizen",
        "Upholds community standards; reliable contributor to shared goals",
        ArchetypeFamily.EVERYMAN,
        List.of("responsible", "dependable", "civic-minded", "fair", "consistent", "dutiful", "cooperative"),
        List.of("anarchic", "selfish", "unreliable", "reckless")),

    // --- Explorer family (Independence) ---

    ADVENTURER("adventurer", "Adventurer",
        "Pushes boundaries through direct experience; thrives on the unknown",
        ArchetypeFamily.EXPLORER,
        List.of("bold", "fearless", "restless", "physical", "daring", "energetic", "spontaneous"),
        List.of("timid", "sedentary", "cautious", "routine")),

    GENERALIST("generalist", "Generalist",
        "Knows enough about everything to bridge specialties",
        ArchetypeFamily.EXPLORER,
        List.of("versatile", "curious", "broad", "adaptable", "connecting", "resourceful", "eclectic"),
        List.of("narrow", "rigid", "specialist", "dogmatic")),

    PIONEER("pioneer", "Pioneer",
        "Goes where none have gone; opens paths for others to follow",
        ArchetypeFamily.EXPLORER,
        List.of("trailblazing", "determined", "visionary", "courageous", "independent", "first-mover"),
        List.of("follower", "cautious", "derivative", "timid")),

    SEEKER("seeker", "Seeker",
        "Explores meaning, truth, and identity rather than geography",
        ArchetypeFamily.EXPLORER,
        List.of("questioning", "philosophical", "introspective", "searching", "contemplative", "deep"),
        List.of("superficial", "certain", "unreflective", "shallow")),

    // --- Hero family (Mastery) ---

    ATHLETE("athlete", "Athlete",
        "Excellence through relentless practice; pushes personal limits",
        ArchetypeFamily.HERO,
        List.of("disciplined", "competitive", "focused", "resilient", "driven", "precise", "relentless"),
        List.of("lazy", "unfocused", "complacent", "undisciplined")),

    LIBERATOR("liberator", "Liberator",
        "Frees others from oppressive systems; fights for collective freedom",
        ArchetypeFamily.HERO,
        List.of("justice-driven", "courageous", "systemic", "emancipating", "principled", "bold"),
        List.of("oppressive", "complacent", "self-serving", "cowardly")),

    RESCUER("rescuer", "Rescuer",
        "Acts decisively when others are in danger; runs toward the fire",
        ArchetypeFamily.HERO,
        List.of("responsive", "brave", "decisive", "crisis-oriented", "selfless", "protective", "quick"),
        List.of("passive", "hesitant", "self-preserving", "slow")),

    WARRIOR("warrior", "Warrior",
        "Fights for a cause with discipline and determination",
        ArchetypeFamily.HERO,
        List.of("strategic", "courageous", "mission-focused", "disciplined", "tenacious", "honorable"),
        List.of("cowardly", "aimless", "undisciplined", "mercenary")),

    // --- Innocent family (Independence) ---

    CHILD("child", "Child",
        "Approaches experience with fresh eyes; pre-cynical engagement",
        ArchetypeFamily.INNOCENT,
        List.of("curious", "open", "spontaneous", "playful", "trusting", "wonder-filled", "unselfconscious"),
        List.of("jaded", "cynical", "calculating", "guarded")),

    DREAMER("dreamer", "Dreamer",
        "Lives partly in possibility; sustains hope through imagination",
        ArchetypeFamily.INNOCENT,
        List.of("imaginative", "gentle", "hopeful", "ethereal", "contemplative", "soft", "wistful"),
        List.of("harsh", "pragmatic", "cynical", "grounded")),

    IDEALIST("idealist", "Idealist",
        "Believes in what could be; holds the standard others have abandoned",
        ArchetypeFamily.INNOCENT,
        List.of("principled", "optimistic", "reform-minded", "earnest", "aspirational", "steadfast"),
        List.of("cynical", "nihilistic", "apathetic", "corrupt")),

    MUSE("muse", "Muse",
        "Sparks creativity in others; their presence unlocks potential",
        ArchetypeFamily.INNOCENT,
        List.of("inspiring", "catalytic", "luminous", "awakening", "magnetic", "ethereal", "enchanting"),
        List.of("dulling", "discouraging", "draining", "mundane")),

    // --- Jester family (Belonging) ---

    CLOWN("clown", "Clown",
        "Uses physicality and absurdity to break tension",
        ArchetypeFamily.JESTER,
        List.of("physical", "absurdist", "disarming", "warm", "spontaneous", "surprising", "expressive"),
        List.of("serious", "restrained", "humorless", "stiff")),

    ENTERTAINER("entertainer", "Entertainer",
        "Commands attention; makes every interaction a show",
        ArchetypeFamily.JESTER,
        List.of("charismatic", "performative", "energetic", "engaging", "dazzling", "magnetic", "vibrant"),
        List.of("dull", "withdrawn", "monotone", "forgettable")),

    PROVOCATEUR("provocateur", "Provocateur",
        "Uses humor as a weapon; reveals truth through provocation",
        ArchetypeFamily.JESTER,
        List.of("subversive", "satirical", "boundary-testing", "sharp", "irreverent", "incisive", "daring"),
        List.of("compliant", "deferential", "timid", "conventional")),

    SHAPESHIFTER("shapeshifter", "Shapeshifter",
        "Shifts persona to match the moment; elusive and versatile",
        ArchetypeFamily.JESTER,
        List.of("adaptive", "chameleonic", "context-reading", "fluid", "versatile", "elusive", "perceptive"),
        List.of("rigid", "predictable", "transparent", "one-dimensional")),

    // --- Lover family (Belonging) ---

    COMPANION("companion", "Companion",
        "Faithful presence; intimacy through constancy and reliability",
        ArchetypeFamily.LOVER,
        List.of("loyal", "present", "steady", "devoted", "reliable", "warm", "grounding"),
        List.of("fickle", "absent", "unreliable", "cold")),

    HEDONIST("hedonist", "Hedonist",
        "Savours beauty, taste, texture; celebrates the physical world",
        ArchetypeFamily.LOVER,
        List.of("sensory", "pleasure-seeking", "appreciative", "aesthetic", "indulgent", "vivid", "passionate"),
        List.of("ascetic", "numb", "indifferent", "austere")),

    MATCHMAKER("matchmaker", "Matchmaker",
        "Sees potential connections between people; orchestrates unions",
        ArchetypeFamily.LOVER,
        List.of("connective", "perceptive", "harmonizing", "social", "intuitive", "generous", "celebratory"),
        List.of("divisive", "oblivious", "isolating", "jealous")),

    ROMANTIC("romantic", "Romantic",
        "Love as transcendent force; seeks the extraordinary in connection",
        ArchetypeFamily.LOVER,
        List.of("passionate", "idealizing", "intense", "devoted", "expressive", "yearning", "transformative"),
        List.of("detached", "cynical", "transactional", "cold")),

    // --- Magician family (Mastery) ---

    ALCHEMIST("alchemist", "Alchemist",
        "Turns base material into gold; transformation through hidden process",
        ArchetypeFamily.MAGICIAN,
        List.of("transformative", "mysterious", "process-oriented", "deep", "patient", "catalytic", "subtle"),
        List.of("superficial", "impatient", "transparent", "clumsy")),

    ENGINEER("engineer", "Engineer",
        "Designs and builds the systems that make transformation possible",
        ArchetypeFamily.MAGICIAN,
        List.of("systematic", "building", "precise", "methodical", "architectural", "elegant", "rigorous"),
        List.of("chaotic", "sloppy", "careless", "haphazard")),

    INNOVATOR("innovator", "Innovator",
        "Creates new categories; makes the impossible suddenly obvious",
        ArchetypeFamily.MAGICIAN,
        List.of("disruptive", "bold", "inventive", "unconventional", "energetic", "visionary", "daring"),
        List.of("conventional", "cautious", "derivative", "timid")),

    SCIENTIST("scientist", "Scientist",
        "Discovers truth through systematic experimentation and evidence",
        ArchetypeFamily.MAGICIAN,
        List.of("empirical", "hypothesis-driven", "rigorous", "methodical", "objective", "thorough", "precise"),
        List.of("sloppy", "biased", "careless", "dogmatic")),

    // --- Rebel family (Mastery) ---

    ACTIVIST("activist", "Activist",
        "Channels rebellion into organized movement for justice",
        ArchetypeFamily.REBEL,
        List.of("cause-driven", "organized", "passionate", "systemic", "persistent", "courageous", "vocal"),
        List.of("apathetic", "compliant", "self-interested", "silent")),

    GAMBLER("gambler", "Gambler",
        "Lives on the edge; bets big and accepts consequences",
        ArchetypeFamily.REBEL,
        List.of("risk-embracing", "instinctive", "high-stakes", "bold", "decisive", "fearless", "improvisational"),
        List.of("cautious", "calculating", "risk-averse", "predictable")),

    MAVERICK("maverick", "Maverick",
        "Does things their own way; ignores rules not through malice but irrelevance",
        ArchetypeFamily.REBEL,
        List.of("independent", "convention-defying", "self-directed", "original", "unorthodox", "resourceful"),
        List.of("conformist", "obedient", "conventional", "dependent")),

    REFORMER("reformer", "Reformer",
        "Breaks what doesn't work to build something better; constructive rebellion",
        ArchetypeFamily.REBEL,
        List.of("principled", "improvement-focused", "constructive", "determined", "analytical", "bold"),
        List.of("destructive", "nihilistic", "careless", "aimless")),

    // --- Sage family (Independence) ---

    DETECTIVE("detective", "Detective",
        "Uncovers truth through systematic investigation and evidence",
        ArchetypeFamily.SAGE,
        List.of("analytical", "meticulous", "persistent", "methodical", "observant", "evidence-driven", "skeptical"),
        List.of("impulsive", "gullible", "reckless", "superficial")),

    MENTOR("mentor", "Mentor",
        "Shares accumulated wisdom to develop others' potential",
        ArchetypeFamily.SAGE,
        List.of("patient", "wise", "developmental", "guiding", "experienced", "nurturing", "insightful"),
        List.of("dismissive", "withholding", "impatient", "competitive")),

    SHAMAN("shaman", "Shaman",
        "Accesses insight from unconventional or unseen sources",
        ArchetypeFamily.SAGE,
        List.of("intuitive", "mystical", "deep", "liminal", "unconventional", "contemplative", "perceptive"),
        List.of("superficial", "conventional", "rigid", "literal")),

    TRANSLATOR("translator", "Translator",
        "Makes the complex accessible; bridges disciplines and audiences",
        ArchetypeFamily.SAGE,
        List.of("clear", "synthesizing", "accessible", "bridging", "articulate", "patient", "versatile"),
        List.of("obscure", "jargon-heavy", "narrow", "exclusive")),

    // --- Sovereign family (Stability) ---

    AMBASSADOR("ambassador", "Ambassador",
        "Represents and negotiates between groups; builds consensus",
        ArchetypeFamily.SOVEREIGN,
        List.of("diplomatic", "representative", "bridge-building", "composed", "strategic", "tactful", "gracious"),
        List.of("partisan", "divisive", "tactless", "aggressive")),

    JUDGE("judge", "Judge",
        "Weighs evidence and renders decisions; upholds standards impartially",
        ArchetypeFamily.SOVEREIGN,
        List.of("principled", "evaluative", "fair-minded", "impartial", "discerning", "deliberate", "authoritative"),
        List.of("biased", "impulsive", "corrupt", "indecisive")),

    PATRIARCH("patriarch", "Patriarch",
        "Provides structure and security through established authority",
        ArchetypeFamily.SOVEREIGN,
        List.of("protective", "authoritative", "legacy-building", "structured", "dependable", "grounding"),
        List.of("tyrannical", "controlling", "rigid", "overbearing")),

    RULER("ruler", "Ruler",
        "Takes charge and creates order; exercises power to build and maintain systems",
        ArchetypeFamily.SOVEREIGN,
        List.of("commanding", "systemic", "order-creating", "decisive", "strategic", "powerful", "responsible"),
        List.of("weak", "chaotic", "indecisive", "irresponsible"));

    public static final String URI = "urn:casehub:vocab:archetype";

    private final String value;
    private final String label;
    private final String description;
    private final ArchetypeFamily family;
    private final List<String> validAdj;
    private final List<String> invalidAdj;

    ArchetypeTerm(String value, String label, String description,
                  ArchetypeFamily family,
                  List<String> validAdj, List<String> invalidAdj) {
        this.value = value;
        this.label = label;
        this.description = description;
        this.family = family;
        this.validAdj = validAdj;
        this.invalidAdj = invalidAdj;
    }

    @Override public String value() { return value; }
    @Override public String label() { return label; }
    @Override public String description() { return description; }
    public ArchetypeFamily family() { return family; }
    public List<String> validAdjectives() { return validAdj; }
    public List<String> invalidAdjectives() { return invalidAdj; }

    public static List<ArchetypeTerm> byFamily(ArchetypeFamily family) {
        return Arrays.stream(values())
            .filter(t -> t.family == family)
            .toList();
    }
}
