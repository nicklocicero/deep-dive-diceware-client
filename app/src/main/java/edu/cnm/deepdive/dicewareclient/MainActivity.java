package edu.cnm.deepdive.dicewareclient;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

  private ListView passphraseWords;
  private Button requestPassphrase;
  private ProgressBar progressSpinner;
  private DicewareService service;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    setupUI();
    setupServices();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.options, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_logout:
        signOut();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void signOut() {
    DicewareApplication application = DicewareApplication.getInstance();
    application.getSignInClient().signOut().addOnCompleteListener(this, (task) -> {
      application.setSignInAccount(null);
      Intent intent = new Intent(MainActivity.this, LoginActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(intent);
    });
  }

  private void setupUI() {
    passphraseWords = findViewById(R.id.passphrase_words);
    progressSpinner = findViewById(R.id.progress_spinner);
    requestPassphrase = findViewById(R.id.request_passphrase);
    requestPassphrase.setOnClickListener((v) -> new DicewareTask().execute());
  }

  private void setupServices() {
    Gson gson = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .create();
    service = new Retrofit.Builder()
        .baseUrl(getString(R.string.base_url))
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(DicewareService.class);
  }

  private class DicewareTask extends AsyncTask<Void, Void, String[]> {

    @Override
    protected void onPreExecute() {
      progressSpinner.setVisibility(View.VISIBLE);
    }

    @Override
    protected String[] doInBackground(Void... voids) {
      String[] passphrase = null;
      try {
        String token = DicewareApplication.getInstance().getSignInAccount().getIdToken();
        Response<String[]> response =
            service.get(getString(R.string.oauth2_header_format, token)).execute();
        if (response.isSuccessful()) {
          passphrase = response.body();
        }
      } catch (IOException e) {
        // Do nothing; passphrase is still null.
      } finally {
        if (passphrase == null) {
          cancel(true);
        }
      }
      return passphrase;
    }

    @Override
    protected void onCancelled(String[] strings) {
      progressSpinner.setVisibility(View.GONE);
    }

    @Override
    protected void onPostExecute(String[] passphrase) {
      ArrayAdapter<String> adapter =
          new ArrayAdapter<>(MainActivity.this, R.layout.passphrase_item, passphrase);
      passphraseWords.setAdapter(adapter);
      progressSpinner.setVisibility(View.GONE);
    }

  }

}
