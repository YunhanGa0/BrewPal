import android.util.Base64
import com.omelan.cofi.share.model.Recipe
import com.omelan.cofi.share.model.Step
import com.omelan.cofi.share.model.toRecipe
import org.json.JSONObject
import java.nio.charset.Charset

class RecipeShareUtils {
    fun decodeRecipe(encodedString: String): Pair<Recipe, List<Step>> {
        val jsonString = String(Base64.decode(encodedString, Base64.DEFAULT), Charset.forName("UTF-8"))
        val jsonObject = JSONObject(jsonString)
        return jsonObject.toRecipe(withId = false)
    }

    fun encodeRecipe(recipe: Recipe, steps: List<Step>): String {
        val jsonString = recipe.serialize(steps, withLastFinished = false).toString()
        return Base64.encodeToString(jsonString.toByteArray(Charset.forName("UTF-8")), Base64.DEFAULT)
    }
} 
