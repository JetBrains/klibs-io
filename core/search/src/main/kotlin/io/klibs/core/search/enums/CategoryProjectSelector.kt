package io.klibs.core.search.enums

enum class CategoryProjectSelector(
    val categoryName: String,
    val ownerLogins: List<String>,
    val subtitle: String? = null,
) {
    JETBRAINS_AND_GOOGLE(
        categoryName = "JetBrains and Google",
        // AndroidX is Google's GitHub organization; the Google org itself does not publish Kotlin/Multiplatform libraries.
        ownerLogins = listOf("Kotlin", "androidx", "JetBrains"),
        subtitle = "Essential libraries from the JetBrains and Google ecosystem",
    );

    companion object {
        fun forCategoryName(name: String): CategoryProjectSelector? =
            entries.firstOrNull { it.categoryName == name }
    }
}
