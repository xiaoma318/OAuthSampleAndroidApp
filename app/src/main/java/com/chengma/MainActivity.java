package com.chengma;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chenm.testapp.R;
import com.github.scribejava.apis.TwitterApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity {

    private static OAuth1RequestToken requestToken;
    private final static OAuth10aService service = new ServiceBuilder()
            .apiKey(OAuthClient.API_KEY)
            .apiSecret(OAuthClient.API_SECRET)
            .callback(OAuthClient.CALLBACK)
            .build(TwitterApi.instance());

    private TextView info;
    private boolean isAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        info = (TextView) findViewById(R.id.twitterInfo);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isAuth){
                    Toast.makeText(MainActivity.this, "Already authorized", Toast.LENGTH_SHORT).show();
                }else{
                    new GetAuthUrl().execute();
                }
            }
        });

    }

    private class GetAuthUrl extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                requestToken = service.getRequestToken();
            } catch (IOException ex) {}

            return service.getAuthorizationUrl(requestToken);
        }

        @Override
        protected void onPostExecute(String result) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(result)));
        }
    }

    class OauthEnd extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            SharedPreferences settings = getSharedPreferences(OAuthClient.PREFS_NAME, 0);
            final SharedPreferences.Editor editor = settings.edit();
            final String verifier = params[0];
            final OAuth1AccessToken accessToken;

            try {
                accessToken = service.getAccessToken(requestToken, verifier);
                editor.putString("accessToken", accessToken.getToken());
                editor.putString("accessSecret", accessToken.getTokenSecret());
                editor.commit();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Handler mainHandler = new Handler(getMainLooper());
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    new GetUser().execute();
                }
            });
        }
    }

    class GetUser extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            SharedPreferences settings = getSharedPreferences(OAuthClient.PREFS_NAME, 0);
            OAuth1AccessToken newAccessToken = new OAuth1AccessToken(settings.getString(OAuthClient.ACCESS_TOKEN, null), settings.getString(OAuthClient.ACCESS_SECRET, null));
            final OAuthRequest request = new OAuthRequest(Verb.GET, OAuthClient.VERIFY_CREDENTIALS, service);
            service.signRequest(newAccessToken, request);
            Response response = request.send();
            String body = null;
            try {
                body = response.getBody();
                isAuth = true;
            } catch (IOException e) {
                e.printStackTrace();
                isAuth = false;
            }
            return body;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject myJson = new JSONObject(result);
                String screenName = myJson.optString("screen_name");
                String name = myJson.optString("name");
                String createdAt = myJson.optString("created_at");
                int follower = myJson.optInt("followers_count");
                int friends = myJson.optInt("friends_count");
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                String date = sdf.format(new Date(createdAt));

                info.setText(name + "@" + screenName + "\nCreated at: " + date + "\nFollower: " + follower + "\tFollowing: " + friends);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        final Uri uri = this.getIntent().getData();
        SharedPreferences settings = getSharedPreferences(OAuthClient.PREFS_NAME, 0);

        if (settings.getString("accessToken", null) != null && settings.getString("accessSecret", null) != null) {
            new GetUser().execute();
        } else {
            // if shared settings are not set / check whether the uri is valid to do an OAuth Dance
            if (uri != null && uri.toString().startsWith(OAuthClient.CALLBACK)) {
                Toast.makeText(this, "Authorized successfully", Toast.LENGTH_SHORT).show();
                String verifier = uri.getQueryParameter("oauth_verifier");
                new OauthEnd().execute(verifier);
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
