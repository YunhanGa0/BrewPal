import android.util.Base64
import com.omelan.cofi.share.model.Recipe
import com.omelan.cofi.share.model.Step
import com.omelan.cofi.share.model.toRecipe
import org.json.JSONObject

class RecipeShareUtils {
    fun decodeRecipe(jsonString: String): Pair<Recipe, List<Step>> {
        val jsonObject = JSONObject(jsonString)
        return jsonObject.toRecipe(withId = false)
    }

    fun encodeRecipe(recipe: Recipe, steps: List<Step>): String {
        return recipe.serialize(steps, withLastFinished = false).toString()
    }
} 
