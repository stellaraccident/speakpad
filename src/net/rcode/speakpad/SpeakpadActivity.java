package net.rcode.speakpad;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SpeakpadActivity extends AbstractActivity implements OnClickListener, TextToSpeech.OnInitListener {
  public static final int AUDIO_STREAM = AudioManager.STREAM_MUSIC;
  
  // -- Controls
  boolean activityInitialized;
  TextToSpeech tts;
  Button btnClear;
  Button btnSpeak;
  Button btnShutUp;
  Button btnShowFavorites;
  EditText textMain;
  TextView textRecent;
  Button btnRecentForward;
  Button btnRecentBack;
  CheckBox cbRecentFavorite;
  View viewTopBar;
  
  // -- State
  List<String> recentsDisplay;
  int recentDisplayIndex;
  String recentDisplayCurrent;

  View.OnClickListener textRecentClickListener = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      if (recentDisplayCurrent != null) {
        textMain.setText(recentDisplayCurrent);
        textMain.setSelection(recentDisplayCurrent.length());
      }
    }
  };
  
  View.OnClickListener recentNavClickListener = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      if (v == btnRecentForward) {
        recentDisplayIndex += 1;
      } else if (v == btnRecentBack) {
        recentDisplayIndex -= 1;
      }
      
      refreshRecentsDisplay();
    }
  };
  
  CompoundButton.OnCheckedChangeListener recentFavoriteCheckedListener = new CompoundButton.OnCheckedChangeListener() {
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
      if (recentDisplayCurrent == null) {
        return;
      }
      
      if (isChecked) {
        Log.i(LOG_TAG, "Add favorite " + recentDisplayCurrent);
        addFavorite(recentDisplayCurrent);
      } else {
        Log.i(LOG_TAG, "Remove favorite " + recentDisplayCurrent);
        removeFavorite(recentDisplayCurrent);
      }
    }
  };
  
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    
    btnClear = (Button) findViewById(R.id.btnClear);
    btnSpeak = (Button) findViewById(R.id.btnSpeak);
    btnShutUp = (Button) findViewById(R.id.btnShutUp);
    btnShowFavorites = (Button) findViewById(R.id.btnShowFavorites);
    textMain = (EditText) findViewById(R.id.editMain);
    textRecent = (TextView) findViewById(R.id.textRecent);
    btnRecentForward = (Button) findViewById(R.id.btnRecentForward);
    btnRecentBack = (Button) findViewById(R.id.btnRecentBack);
    cbRecentFavorite = (CheckBox) findViewById(R.id.cbRecentFavorite);
    
    viewTopBar = findViewById(R.id.topBar);
    
    btnClear.setOnClickListener(this);
    btnSpeak.setOnClickListener(this);
    btnShutUp.setOnClickListener(this);
    btnShowFavorites.setOnClickListener(this);
    textRecent.setOnClickListener(textRecentClickListener);
    
    btnRecentForward.setOnClickListener(recentNavClickListener);
    btnRecentBack.setOnClickListener(recentNavClickListener);
    cbRecentFavorite.setOnCheckedChangeListener(recentFavoriteCheckedListener);
    
    btnSpeak.setEnabled(false);
    
    refreshRecentsDisplay();
    if (recentDisplayCurrent != null) {
      textMain.setText(recentDisplayCurrent);
      textMain.setSelection(recentDisplayCurrent.length());
    }
    activityInitialized = true;
    
    setVolumeControlStream(AUDIO_STREAM);
    initializeTextToSpeech();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (tts != null) {
      tts.shutdown();
      tts = null;
    }
  }
  
  @Override
  protected void onResume() {
    super.onResume();
    initializeTextToSpeech();
  }
  
  private void initializeTextToSpeech() {
    if (!activityInitialized || tts != null) {
      return;
    }
    
    // Initialize TTS.
    Intent checkIntent = new Intent();
    checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
    launchSubActivity(checkIntent, new ActivityResultCallback() {
      @Override
      public void onResult(int resultCode, Intent data) {
        if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
          tts = new TextToSpeech(SpeakpadActivity.this, SpeakpadActivity.this);
        } else {
          showToastMessage("Text to Speech Not Available. Installing.");
          // missing data, install it
          Intent installIntent = new Intent();
          installIntent.setAction(
              TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
          startActivity(installIntent);
        }
      }
    });
  }

  protected void showToastMessage(String string) {
    Toast toast = Toast.makeText(this, string, Toast.LENGTH_SHORT);
    toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
    toast.show();
  }

  @Override
  public void onClick(View v) {
    if (v == btnClear) {
      doClear();
    } else if (v == btnSpeak) {
      doSpeak();
    } else if (v == btnShutUp) {
      doShutUp();
    } else if (v == btnShowFavorites) {
      doShowFavorites();
    }
  }

  private void doShowFavorites() {
    Intent intent = new Intent(this, SelectFavoriteDialog.class);
    intent.putExtra("Favorites", getFavorites().toArray(new String[0]));
    launchSubActivity(intent, new ActivityResultCallback() {  
      @Override
      public void onResult(int resultCode, Intent data) {
        Log.i(LOG_TAG, "Returned from favorite selector");
        if (resultCode != Activity.RESULT_OK) {
          return;
        }
        
        if (data.hasExtra("Favorites")) {
          String[] favorites = data.getStringArrayExtra("Favorites");
          if (favorites != null) {
            setStringPreferences("Favorites", Arrays.asList(favorites));
          }
        }
        if (data.hasExtra("SelectedFavorite")) {
          String favorite = data.getStringExtra("SelectedFavorite");
          if (favorite != null) {
            doSpeak(favorite);
          }
        }
      }
    });
  }

  private void doShutUp() {
    if (tts != null) {
      tts.stop();
    }
  }

  private void doSpeak() {
    String text = textMain.getText().toString().trim();
    doSpeak(text);
    textMain.setText("");
  }

  private void doSpeak(String text) {
    if (text.length() == 0 || tts == null) {
      return;
    }
    
    HashMap<String, String> params = new HashMap<String, String>();
    params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AUDIO_STREAM));
    tts.speak(text.toString(), TextToSpeech.QUEUE_ADD, params);
    
    addRecent(text);
  }
  
  private void doClear() {
    String text = textMain.getText().toString().trim();
    if (text.length() > 0) {
      addRecent(text);
    }
    textMain.setText("");
  }

  @Override
  public void onInit(int status) {
    btnSpeak.setEnabled(true);
  }
  
  List<String> getStringPreferences(String name) {
    SharedPreferences prefs = getSharedPreferences(name, 0);
    ArrayList<String> list = new ArrayList<String>();
    int count = prefs.getInt("count", 0);
    for (int i = 0; i < count; i++) {
      String item = prefs.getString(String.valueOf(i), null);
      if (item != null) {
        list.add(item);
      }
    }
    
    return list;
  }
  
  void setStringPreferences(String name, List<String> strings) {
    SharedPreferences prefs = getSharedPreferences(name, 0);
    SharedPreferences.Editor editor = prefs.edit();
    editor.clear();
    
    editor.putInt("count", strings.size());
    for (int i = 0; i < strings.size(); i++) {
      editor.putString(String.valueOf(i), strings.get(i));
    }
    
    editor.commit();
  }
  
  List<String> getRecents() {
    return getStringPreferences("Recents");
  }
  
  void setRecents(List<String> recents) {
    setStringPreferences("Recents", recents);
  }
  
  List<String> getFavorites() {
    return getStringPreferences("Favorites");
  }
  
  void addFavorite(String favorite) {
    boolean changed = false;
    List<String> favorites = getFavorites();
    Iterator<String> iter = favorites.iterator();
    while (iter.hasNext()) {
      String item = iter.next();
      if (favorite.equalsIgnoreCase(item)) {
        iter.remove();
        changed = true;
      }
    }
    favorites.add(0, favorite);
    setStringPreferences("Favorites", favorites);
    
    if (changed) {
      refreshRecentsDisplay();
    }
  }
  
  void removeFavorite(String favorite) {
    boolean changed = false;
    List<String> favorites = getFavorites();
    Iterator<String> iter = favorites.iterator();
    while (iter.hasNext()) {
      String item = iter.next();
      if (favorite.equalsIgnoreCase(item)) {
        iter.remove();
        changed = true;
      }
    }
    
    setStringPreferences("Favorites", favorites);
    
    if (changed) {
      refreshRecentsDisplay();
    }
  }
  
  void addRecent(String text) {
    List<String> recents = getRecents();
    Iterator<String> iter = recents.iterator();
    while (iter.hasNext()) {
      String item = iter.next();
      if (text.equalsIgnoreCase(item)) {
        iter.remove();
      }
    }
    
    recents.add(0, text);
    
    while (recents.size() > 10) {
      recents.remove(recents.size() - 1);
    }
    
    setRecents(recents);
    recentDisplayIndex = 0;
    refreshRecentsDisplay();
  }

  private void refreshRecentsDisplay() {
    List<String> favorites = getFavorites();
    recentsDisplay = getRecents();
    
    if (recentDisplayIndex >= recentsDisplay.size()) {
      recentDisplayIndex = 0;
    } else if (recentDisplayIndex < 0) {
      recentDisplayIndex = recentsDisplay.size() - 1;
    }
    
    if (recentsDisplay.isEmpty()) {
      viewTopBar.setVisibility(View.GONE);
      recentDisplayCurrent = null;
    } else {
      viewTopBar.setVisibility(View.VISIBLE);
      String recentItem = recentsDisplay.get(recentDisplayIndex);
      recentDisplayCurrent = recentItem;
      textRecent.setText(recentItem);
      cbRecentFavorite.setChecked(favorites.contains(recentItem));
    }
  }
}