package io.casehub.eidos.vocab;

public enum ArchetypeFamily {
    CAREGIVER(Quadrant.STABILITY, "Service, compassion, generosity"),
    CREATOR(Quadrant.STABILITY, "Innovation, expression, imagination"),
    EVERYMAN(Quadrant.BELONGING, "Belonging, empathy, realism"),
    EXPLORER(Quadrant.INDEPENDENCE, "Freedom, discovery, self-sufficiency"),
    HERO(Quadrant.MASTERY, "Mastery, courage, achievement"),
    INNOCENT(Quadrant.INDEPENDENCE, "Safety, optimism, simplicity"),
    JESTER(Quadrant.BELONGING, "Joy, spontaneity, humor"),
    LOVER(Quadrant.BELONGING, "Intimacy, passion, commitment"),
    MAGICIAN(Quadrant.MASTERY, "Transformation, vision, catalyst"),
    REBEL(Quadrant.MASTERY, "Liberation, disruption, revolution"),
    SAGE(Quadrant.INDEPENDENCE, "Knowledge, truth, understanding"),
    SOVEREIGN(Quadrant.STABILITY, "Control, order, leadership");

    public enum Quadrant {
        INDEPENDENCE, MASTERY, BELONGING, STABILITY
    }

    private final Quadrant quadrant;
    private final String drive;

    ArchetypeFamily(Quadrant quadrant, String drive) {
        this.quadrant = quadrant;
        this.drive = drive;
    }

    public Quadrant quadrant() { return quadrant; }
    public String drive() { return drive; }

    public String label() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}
