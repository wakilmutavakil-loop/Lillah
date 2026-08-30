package com.lillah.dhikr.data.guide

import androidx.compose.runtime.Immutable

enum class GuideIcon { Heart, Ring, Sunrise, Calendar, Feather, Chart, Beads }

@Immutable
sealed interface GuideBlock {
    @Immutable data class Heading(val text: String) : GuideBlock
    @Immutable data class Paragraph(val text: String) : GuideBlock
    @Immutable data class Bullets(val items: List<String>) : GuideBlock
    @Immutable data class Quote(val text: String, val attribution: String? = null) : GuideBlock
    @Immutable data class Tip(val text: String) : GuideBlock
    @Immutable data class ArabicLine(
        val arabic: String,
        val transliteration: String? = null,
        val meaning: String? = null,
    ) : GuideBlock
}

@Immutable
data class GuideArticle(
    val id: String,
    val title: String,
    val subtitle: String,
    val minutes: Int,
    val icon: GuideIcon,
    val accentIndex: Int,
    val blocks: List<GuideBlock>,
)

/**
 * The guidebook. Written to be read rather than skimmed for settings: each article explains one
 * idea, in plain English, in a couple of minutes.
 *
 * Content is Kotlin rather than XML strings so it can carry structure — headings, pull quotes,
 * Arabic lines — and still be handed to a translation layer later by swapping this object for a
 * locale-aware provider.
 */
object GuideContent {

    val articles: List<GuideArticle> = listOf(
        GuideArticle(
            id = "what-is-dhikr",
            title = "What is Dhikr?",
            subtitle = "The short answer, and why people keep returning to it",
            minutes = 3,
            icon = GuideIcon.Heart,
            accentIndex = 2,
            blocks = listOf(
                GuideBlock.Paragraph(
                    "Dhikr (ذِكْر) means remembrance. In practice it is the habit of returning " +
                        "attention to God through short phrases said quietly, often, and without " +
                        "ceremony — while walking, waiting, cooking, or lying awake."
                ),
                GuideBlock.Paragraph(
                    "It asks almost nothing of you. No particular place, no set time, no special " +
                        "state. That is the point: it fits into the parts of a day that would " +
                        "otherwise be empty."
                ),
                GuideBlock.Heading("The phrases people start with"),
                GuideBlock.ArabicLine(
                    arabic = "سُبْحَانَ اللَّهِ",
                    transliteration = "SubhanAllah",
                    meaning = "Glory be to Allah",
                ),
                GuideBlock.ArabicLine(
                    arabic = "الْحَمْدُ لِلَّهِ",
                    transliteration = "Alhamdulillah",
                    meaning = "All praise belongs to Allah",
                ),
                GuideBlock.ArabicLine(
                    arabic = "اللَّهُ أَكْبَرُ",
                    transliteration = "Allahu Akbar",
                    meaning = "Allah is the Greatest",
                ),
                GuideBlock.Paragraph(
                    "These three are commonly said thirty-three times each after prayer. You will " +
                        "find them in Everyday Tasbih, already set to those counts."
                ),
                GuideBlock.Heading("Counting is a tool, not the goal"),
                GuideBlock.Paragraph(
                    "Numbers help a habit take shape — they give it edges, and they make it " +
                        "obvious when a day has slipped past. But a hundred distracted " +
                        "repetitions are not the aim. If the count ever starts to matter more " +
                        "than the words, put the phone down and just say them."
                ),
                GuideBlock.Tip(
                    "This app is a counter and a record. It is not a scholarly reference — " +
                        "check anything you are unsure of against a source you trust."
                ),
            ),
        ),

        GuideArticle(
            id = "using-the-app",
            title = "Using this app",
            subtitle = "Everything you need in about two minutes",
            minutes = 2,
            icon = GuideIcon.Ring,
            accentIndex = 0,
            blocks = listOf(
                GuideBlock.Heading("The counter"),
                GuideBlock.Paragraph(
                    "Tap anywhere on the large circle to count. The ring fills one bead per " +
                        "repetition, and completing a round is marked with a short pulse and a " +
                        "bloom of light. The next tap starts the following round."
                ),
                GuideBlock.Bullets(
                    listOf(
                        "Press and hold the circle to undo the last count.",
                        "The arrow button also undoes one; the reset button clears the round in " +
                            "progress without touching what you have already recorded today.",
                        "The dial button changes how many repetitions make up one round.",
                    )
                ),
                GuideBlock.Heading("Switching dhikr"),
                GuideBlock.Paragraph(
                    "The row beneath the counter holds your adhkar. Tap one to switch to it; the " +
                        "round you were part-way through is kept exactly where you left it, so " +
                        "you can move between them freely."
                ),
                GuideBlock.Heading("Collections"),
                GuideBlock.Paragraph(
                    "The Adhkar tab holds Morning and Evening Adhkar, everyday tasbih, and " +
                        "anything you make yourself. Inside a collection, the plus button on " +
                        "each row counts one without leaving the list — which is what you want " +
                        "for adhkar said once or three times. Tapping the row itself opens the " +
                        "full counter."
                ),
                GuideBlock.Tip(
                    "Nothing needs a connection. Everything is stored on your device, and the " +
                        "app works the same in aeroplane mode."
                ),
            ),
        ),

        GuideArticle(
            id = "morning-evening",
            title = "Morning and Evening Adhkar",
            subtitle = "Two anchors, at either end of the day",
            minutes = 3,
            icon = GuideIcon.Sunrise,
            accentIndex = 3,
            blocks = listOf(
                GuideBlock.Paragraph(
                    "Adhkar as-Sabah (أذكار الصباح) and Adhkar al-Masa (أذكار المساء) are " +
                        "collections said in the morning and the evening. They are among the " +
                        "oldest daily habits in the tradition, and among the easiest to keep, " +
                        "because they attach themselves to two moments that arrive on their own."
                ),
                GuideBlock.Heading("When"),
                GuideBlock.Paragraph(
                    "The morning set is usually said after Fajr and before sunrise; the evening " +
                        "set after Asr and before sunset. Views differ on the exact windows, and " +
                        "most scholars are relaxed about it — said late is far better than not " +
                        "said."
                ),
                GuideBlock.Heading("What is in them"),
                GuideBlock.Paragraph(
                    "Ayat al-Kursi, the last three suras, Sayyid al-Istighfar, and a set of " +
                        "short supplications for protection and well-being. The two collections " +
                        "overlap heavily; the wording shifts between morning and evening in a " +
                        "few places, which is why they are listed separately here."
                ),
                GuideBlock.Heading("How the app tracks them"),
                GuideBlock.Bullets(
                    listOf(
                        "Each item shows its own target — once, three times, seven, a hundred.",
                        "An item is marked done when today's count reaches that target.",
                        "The collection is complete when every item is done, and it resets " +
                            "cleanly at midnight.",
                        "In the morning the app offers the morning set on the home screen, and " +
                            "the evening set from mid-afternoon. Once a set is finished for the " +
                            "day, it stops offering it.",
                    )
                ),
                GuideBlock.Tip(
                    "If a wording in these collections differs from the one you were taught, " +
                        "edit it. Every seeded dhikr is editable, and your edits are kept."
                ),
            ),
        ),

        GuideArticle(
            id = "consistency",
            title = "Consistency over intensity",
            subtitle = "Why the goals here are small on purpose",
            minutes = 3,
            icon = GuideIcon.Calendar,
            accentIndex = 4,
            blocks = listOf(
                GuideBlock.Quote(
                    text = "The most beloved of deeds to Allah are those done consistently, " +
                        "even if they are few.",
                    attribution = "Reported in Sahih al-Bukhari and Sahih Muslim",
                ),
                GuideBlock.Paragraph(
                    "This is the idea the whole app is built around. A hundred a day, kept for a " +
                        "year, is worth more than a thousand in one burst and then nothing."
                ),
                GuideBlock.Heading("Setting a goal you will actually meet"),
                GuideBlock.Paragraph(
                    "The daily goal starts at 100 — roughly three minutes. Set it low enough " +
                        "that a bad day still clears it. You can always do more; the goal is " +
                        "the floor, not the ceiling. Change it any time in Settings."
                ),
                GuideBlock.Heading("How streaks work here"),
                GuideBlock.Bullets(
                    listOf(
                        "A day counts toward your streak if you counted anything at all — " +
                            "meeting your goal is not required.",
                        "An untouched today never breaks a streak. The day is not over yet.",
                        "If a streak does end, your longest run is kept and still shown. " +
                            "Nothing is taken away from you.",
                    )
                ),
                GuideBlock.Paragraph(
                    "There is no leaderboard, no comparison with other people, and no " +
                        "notification designed to make you feel behind. Missing a day is not a " +
                        "failure — it is a day. Start again on the next one."
                ),
            ),
        ),

        GuideArticle(
            id = "custom-dhikr",
            title = "Making it your own",
            subtitle = "Custom adhkar and collections",
            minutes = 2,
            icon = GuideIcon.Feather,
            accentIndex = 1,
            blocks = listOf(
                GuideBlock.Heading("Adding a dhikr"),
                GuideBlock.Paragraph(
                    "Tap the plus at the end of the row beneath the counter, or open a " +
                        "collection and use the row at the bottom. Only a name is required — " +
                        "everything else is there if you want it."
                ),
                GuideBlock.Bullets(
                    listOf(
                        "Arabic text, transliteration and meaning are all optional, and each " +
                            "can be hidden globally in Settings.",
                        "Repetitions per round is the number the ring fills to.",
                        "A daily goal for a single dhikr is separate from your overall daily " +
                            "goal, and useful for one you want to keep up specifically.",
                        "The colour you pick follows the dhikr everywhere it appears.",
                    )
                ),
                GuideBlock.Heading("Collections"),
                GuideBlock.Paragraph(
                    "A collection is a set of adhkar you say together. Give it a name, pick " +
                        "artwork or use a photo of your own, and add adhkar to it. Deleting a " +
                        "collection keeps the adhkar inside it — they simply stop belonging to " +
                        "one."
                ),
                GuideBlock.Heading("Covers"),
                GuideBlock.Paragraph(
                    "Any collection can take a photo from your device as its cover. The image " +
                        "is copied into the app's own storage and scaled down, so it keeps " +
                        "working even if the original is moved or deleted, and it never leaves " +
                        "your phone."
                ),
                GuideBlock.Tip(
                    "Removing a dhikr also removes its counting history. Archiving keeps the " +
                        "history and simply takes it out of the way."
                ),
            ),
        ),

        GuideArticle(
            id = "progress",
            title = "How progress is measured",
            subtitle = "What each number on the Progress tab means",
            minutes = 3,
            icon = GuideIcon.Chart,
            accentIndex = 5,
            blocks = listOf(
                GuideBlock.Heading("Day"),
                GuideBlock.Paragraph(
                    "The ring shows today against your daily goal. Beneath it, every dhikr you " +
                        "counted today, largest first — useful for noticing that a set you " +
                        "meant to keep has quietly dropped off."
                ),
                GuideBlock.Heading("Week"),
                GuideBlock.Paragraph(
                    "Seven bars, starting on whichever day your device treats as the start of " +
                        "the week. The comparison is with your own previous week, and it is " +
                        "phrased as information rather than a verdict."
                ),
                GuideBlock.Heading("Month"),
                GuideBlock.Paragraph(
                    "A calendar where each day is shaded by how much was counted, against your " +
                        "own busiest day that month. Over a few months the shape of your habit " +
                        "becomes visible — including the gaps, which are worth seeing plainly."
                ),
                GuideBlock.Heading("Your garden"),
                GuideBlock.Paragraph(
                    "Lifetime totals are shown as something growing: a seed, then a sprout, a " +
                        "sapling, and onward. It only ever grows. There is no rank attached to " +
                        "it and nobody else's to compare it against."
                ),
                GuideBlock.Heading("Milestones"),
                GuideBlock.Paragraph(
                    "Twenty-one quiet markers — a first hundred, a first full week, a finished " +
                        "set of morning adhkar. Locked ones show how far along you are rather " +
                        "than hiding themselves, so the list reads as what is ahead. Once " +
                        "reached, none of them can be lost."
                ),
            ),
        ),

        GuideArticle(
            id = "tasbih",
            title = "The tasbih, briefly",
            subtitle = "Where the beads and the number thirty-three come from",
            minutes = 2,
            icon = GuideIcon.Beads,
            accentIndex = 2,
            blocks = listOf(
                GuideBlock.Paragraph(
                    "A tasbih (تَسْبِيح), also called a misbaha or subha, is a loop of beads " +
                        "used to keep count. Most have ninety-nine beads divided into three " +
                        "groups of thirty-three, with a larger leader bead marking the start."
                ),
                GuideBlock.Paragraph(
                    "The division comes from a well-known practice after prayer: thirty-three " +
                        "SubhanAllah, thirty-three Alhamdulillah, and thirty-three or " +
                        "thirty-four Allahu Akbar. Earlier generations counted on the fingers, " +
                        "and many still do."
                ),
                GuideBlock.Heading("Why a digital one"),
                GuideBlock.Paragraph(
                    "For the same reason as the beads: so the counting can happen in the " +
                        "background, and your attention can stay on the words. A phone is also " +
                        "simply the thing already in your hand."
                ),
                GuideBlock.Paragraph(
                    "The one thing beads have that a screen does not is texture. This app " +
                        "answers each tap with a short haptic pulse for the same reason — so " +
                        "the count can be felt, and eventually done without looking."
                ),
                GuideBlock.Tip(
                    "Turn on the counting sound in Settings if you would rather hear it, or " +
                        "enable the volume keys and count with the screen off."
                ),
            ),
        ),
    )

    fun find(id: String): GuideArticle? = articles.firstOrNull { it.id == id }
}
