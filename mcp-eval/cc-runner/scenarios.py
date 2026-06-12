"""17 eval scenarios ported from klibs_server.eval.py (S1-S10, I1-I7).

Each: prompt, judge rubric, min_score (pass threshold), tier. I-tier prompts get
REPO_HINT prepended (agent must read the Notes app and infer a KMP library is needed).
Tool-call assertions from the original suite become soft signals here (recorded, not gating);
the gate is the LLM judge, matching the mcp-eval arms.
"""

REPO_HINT = (
    "You are working in the Kotlin Multiplatform 'Notes' app in the current project "
    "(read it with the filesystem tool; start at README.md and src/commonMain). "
)

# tier "S" = explicit prompt; tier "I" = implicit/agentic (REPO_HINT prepended).
# `requires` = the klibs tool that MUST be called for the answer to count (deterministic
#   gate, checked against captured tool calls — NOT the judge, which only sees text).
# `rubric` = TEXT-ONLY quality grade for the judge; no references to tool internals the
#   judge cannot observe.
SCENARIOS = {
    # --- getLatestVersion (S1-S5) ---
    "s1": dict(tier="S", min_score=0.6, requires="getLatestVersion",
        prompt="What is the latest version of the Kotlin Multiplatform library "
               "`org.jetbrains.kotlinx:kotlinx-coroutines-core`?",
        rubric="States a single concrete latest version for kotlinx-coroutines-core "
               "(a version string like X.Y.Z). Penalize only refusal or self-contradiction."),
    "s2": dict(tier="S", min_score=0.6, requires="getLatestVersion",
        prompt="I only ship stable releases — what's the latest stable version of "
               "`io.ktor:ktor-client-core`?",
        rubric="Gives a concrete latest STABLE version of ktor-client-core (a plain X.Y.Z "
               "release, not an alpha/beta/rc/eap pre-release)."),
    "s3": dict(tier="S", min_score=0.6, requires="getLatestVersion",
        prompt="Look up `com.squareup.okio:okio` and tell me which Kotlin version its latest "
               "build was compiled with.",
        rubric="States a concrete Kotlin version that okio's latest build was compiled with."),
    "s4": dict(tier="S", min_score=0.8, requires="getLatestVersion",
        prompt="Give me the latest version of `io.klibs:this-artifact-does-not-exist`.",
        rubric="States the package was NOT found / does not exist. Must NOT state any "
               "version number for it."),
    "s5": dict(tier="S", min_score=0.6, requires="getLatestVersion",
        prompt="For `org.jetbrains.kotlinx:kotlinx-serialization-json`, what build tool and "
               "build-tool version was the latest release published with?",
        rubric="States a concrete build tool AND its version for the latest release."),
    # --- searchProjects (S6-S10) ---
    "s6": dict(tier="S", min_score=0.6, requires="searchProjects",
        prompt="Find Kotlin Multiplatform libraries for JSON serialization.",
        rubric="Lists one or more real KMP JSON-serialization libraries with coordinates."),
    "s7": dict(tier="S", min_score=0.6, requires="searchProjects",
        prompt="I need a Kotlin Multiplatform HTTP client that runs on iOS and JVM. What are "
               "my options?",
        rubric="Names one or more real KMP HTTP-client libraries that support iOS and JVM."),
    "s8": dict(tier="S", min_score=0.6, requires="searchProjects",
        prompt="Show me KMP state-management libraries that support JavaScript.",
        rubric="Names one or more real KMP state-management libraries that support the JS target."),
    "s9": dict(tier="S", min_score=0.6, requires="searchProjects",
        prompt="Find Kotlin Multiplatform date/time libraries that target iOS arm64 and JVM 17.",
        rubric="Names one or more real KMP date/time libraries that support iOS arm64 and JVM."),
    "s10": dict(tier="S", min_score=0.6, requires="searchProjects",
        prompt="What KMP database/storage libraries work on watchOS, regardless of the specific "
               "watchOS target?",
        rubric="Names one or more real KMP storage/database libraries that support watchOS."),
    # --- implicit / agentic (I1-I7) ---
    "i1": dict(tier="I", min_score=0.6, requires="searchProjects",
        prompt="Users complain that all their notes disappear whenever they restart the app. "
               "Make their notes stick around between launches, on every platform the app targets.",
        rubric="Recommends a real KMP on-device storage/database library (e.g. SQLDelight, Room "
               "KMP, multiplatform-settings, a datastore) that fits the app's targets; no invented "
               "or JVM-only library."),
    "i2": dict(tier="I", min_score=0.6, requires="searchProjects",
        prompt="People write their notes using markdown syntax, but the note body is shown as raw "
               "characters. Make the app display the note body as properly formatted text.",
        rubric="Recommends a real KMP markdown-rendering library (e.g. multiplatform-markdown-"
               "renderer) rather than hand-rolling a parser or naming an unverified package."),
    "i3": dict(tier="I", min_score=0.6, requires="searchProjects",
        prompt="On the note detail screen the Share button should let a user share a note by showing "
               "something another person's phone can scan straight off the screen. Wire that up.",
        rubric="Infers a QR/scannable code is needed and recommends a real KMP QR-code library "
               "(e.g. qrose, qrcode-kotlin)."),
    "i4": dict(tier="I", min_score=0.6, requires="searchProjects",
        prompt="Implement NoteExporter (currently a stub that throws). A user should be able to save a "
               "note as a single, printable document they can open and share anywhere.",
        rubric="Infers a PDF / printable-document capability and recommends a real KMP PDF library "
               "(e.g. pdf-kmp, compose-pdf, fluid-pdf), not an invented one."),
    "i5": dict(tier="I", min_score=0.6, requires="searchProjects",
        prompt="Notes can contain sensitive personal information. Make sure notes are stored on the "
               "device in a way that they can't be read by someone who gets hold of the phone's files.",
        rubric="Infers at-rest encryption / secure storage and recommends a real KMP cryptography or "
               "encrypted-storage library (e.g. cryptography-kotlin, KVault, encrypted-datastore)."),
    "i6": dict(tier="I", min_score=0.6, requires="searchProjects",
        prompt="Add a screen that gives the user a visual breakdown of how many notes they create each "
               "week, so they can see their activity at a glance.",
        rubric="Infers a charting/graphing capability and recommends a real KMP charts library "
               "(e.g. charty, AAY-chart, koalaplot)."),
    "i7": dict(tier="I", min_score=0.6, requires="searchProjects",
        prompt="Let a note include a photo by pasting an image URL; the image should show up inline in "
               "the note, loaded from the network, on all targets.",
        rubric="Infers a network image-loading capability and recommends a real KMP image-loading "
               "library (e.g. Coil 3, Sketch, compose-imageloader, Kamel)."),
}


def full_prompt(key: str) -> str:
    sc = SCENARIOS[key]
    return (REPO_HINT + sc["prompt"]) if sc["tier"] == "I" else sc["prompt"]
