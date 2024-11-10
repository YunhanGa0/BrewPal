data class ShareRecipe(
    val name: String,
    val description: String?,
    val steps: List<ShareStep>,
    val version: Int = 1
)

data class ShareStep(
    val type: String,
    val name: String,
    val duration: Int?,
    val weight: Float?
) 