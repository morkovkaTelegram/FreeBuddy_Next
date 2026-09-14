package io.github.razziell.freebuddynext.widget

/** Only acknowledge revision after every provider selected by this plan has been submitted. */
internal data class WidgetPublicationPlan(
    val revision: Long,
    val battery: Boolean,
    val anc: Boolean,
) {
    companion object {
        fun create(revision: Long, published: Long, batteryRevision: Long,
            ancRevision: Long, appearanceRevision: Long): WidgetPublicationPlan {
            val appearanceChanged = appearanceRevision > published
            return WidgetPublicationPlan(revision,
                battery = appearanceChanged || batteryRevision > published,
                anc = appearanceChanged || ancRevision > published,
            )
        }
    }
}
