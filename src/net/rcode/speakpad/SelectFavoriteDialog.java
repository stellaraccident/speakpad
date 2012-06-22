package net.rcode.speakpad;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * Selects a favorite.
 * @author stella
 *
 */
public class SelectFavoriteDialog extends AbstractActivity {
  List<String> favorites;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.selectfavorite);

    String[] favoritesArray = getIntent().getStringArrayExtra("Favorites");
    if (favoritesArray == null) {
      favoritesArray = new String[0];
    }
    favorites = new ArrayList<String>(Arrays.asList(favoritesArray));

    refreshFavorites();
  }

  void refreshFavorites() {
    TableLayout favoriteTable = (TableLayout) findViewById(R.id.favoriteTable);
    favoriteTable.removeAllViews();
    
    int index = 1;
    for (String favorite: favorites) {
      addFavorite(favoriteTable, index++, favorite);
    }
  }
  
  private void addFavorite(TableLayout favoriteTable, int index, final String favorite) {
    TableRow row = new TableRow(this);
    
    TextView indexView = new TextView(this);
    indexView.setTextSize(20);
    indexView.setText(index + ".");
    indexView.setPadding(10, 10, 10, 10);
    row.addView(indexView);
    
    TextView tv = new TextView(this);
    tv.setTextSize(20);
    tv.setMaxLines(2);
    tv.setMaxEms(10);
    tv.setEllipsize(TruncateAt.END);
    tv.setText(favorite);
    row.addView(tv);
    
    Button delete = new Button(this);
    delete.setPadding(10, 5, 10, 5);
    delete.setBackgroundDrawable(getResources().getDrawable(android.R.drawable.ic_delete));
    row.addView(delete);
    
    favoriteTable.addView(row);
    
    row.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        selectFavorite(favorite);
      }
    });
    
    delete.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        removeFavorite(favorite);
      }
    });
  }

  protected void removeFavorite(String favorite) {
    Log.i(LOG_TAG, "Remove favorite " + favorite);
    favorites.remove(favorite);
    completeDialog(null, favorites);
  }

  protected void selectFavorite(String favorite) {
    Log.i(LOG_TAG, "Select favorite " + favorite);
    completeDialog(favorite, null);
  }
  
  void completeDialog(String selectedFavorite, List<String> updatedFavorites) {
    Intent result = new Intent();
    if (updatedFavorites != null) {
      String[] favoritesArray = updatedFavorites.toArray(new String[0]);
      result.putExtra("Favorites", favoritesArray);
    }
    if (selectedFavorite != null) {
      result.putExtra("SelectedFavorite", selectedFavorite);
    }
    
    setResult(Activity.RESULT_OK, result);
    finish();
  }
}
