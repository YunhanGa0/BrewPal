# Development process

## 1. Recipe statistic page

*Commit: Implement time counting functions for different recipes, and make a statistic page.*

**Changed files:**

- `MainActivity`&`Destinations`: Import `RecipeTimesPage` and add `navController` of it.

- `RecipeItem`: To test if the `times` attribute has successfully added.

- `RecipeEdit`&`Recipe`: Add times where needed.

- `RecipeTimesPage`: New page that has statistic result of recipes usage.

- `RecipeDetails`: Implement the core logic of recipe usage counting.

  ``` 
  val t = recipe.times + 1
  db.recipeDao().updateRecipe(recipe.copy(times = t))
  ```

- `RecipeList`: Add goToTimes button that navigates to `RecipeTimesPage`.

- `AppDatabase`: Add attribute `times` in table, update db to version 6 by a `migration`.

  ```
  val MIGRATION_5_6 = object : Migration(5, 6) {
      override fun migrate(database: SupportSQLiteDatabase) {
          database.execSQL("ALTER TABLE Recipe ADD COLUMN times INTEGER NOT NULL DEFAULT 0")
      }
  }
  ```

- `PrepopulateData`: Add preset times for recipes to test the statistic page.

## 2. Debug on the statistic function

*Commit:* *Some debug on the statistic function.*

**Changed files:**

- `build.gradle`: Add dependency`implementation 'androidx.compose.ui:ui-graphics-android:1.6.8'`.
- `PieChart`: Add a pie chart on statistic page to better show the usage of different recipes.
- `RecipeTimesPage`: Some change about adding pie chart.

## 3. Jump from the statistic page to recipeDetail page

*Commit: Implement jump from the statistic page to recipeDetail page.*

**Changed files:**

- `MainActivity`: Add navigator to recipes.

  ```
  navigateToRecipe = { recipeId -> navController.navigate("recipe/$recipeId") },
  ```

- `RecipeTimesPage`: Set `onClick`.

## 4. The unfinished QRcode share recipe function 

*Commit: The QRcode share recipe function by far, haven't implement it yet.*

- `bulid.gradle`: Add dependencies mostly related to camera.
- `AndroidManifest.xml`: Add uses-permissions to camera, read, write and network.
- `MainActivity`: Import `ImportRecipePage` and add `navController` of it.

- `QRCodeScannerDialog`: The implementation of QR code scanner dialog

- `ImportRecipePage`: Page that can import shared recipe by text and QR code.
-  `RecipeDetails`：Change link button to my share button.
- `RecipeList`: Add import button.
- `BackupRestore`: Change the way of data restore.
