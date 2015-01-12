package learn2crack.weboauth2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ActivitySignInWithGPlus extends Activity {

	private static String CLIENT_ID = "900934157555-nc3rj366j7h83lmde1p44b3urabj6i05.apps.googleusercontent.com";
	// Use your own client id
	// private static String CLIENT_SECRET ="EEOeMFHQpLtaHDU4Rr8k-l3N";
	// Use your own client secret
	private static String REDIRECT_URI = "http://localhost";
	private static String GRANT_TYPE = "authorization_code";
	private static String TOKEN_URL = "https://accounts.google.com/o/oauth2/token";
	private static String OAUTH_URL = "https://accounts.google.com/o/oauth2/auth";
	private static String OAUTH_SCOPE = "https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile";
	// Change the Scope as you need
	WebView web;
	Button auth;
	SharedPreferences pref;
	TextView Access;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		pref = getSharedPreferences("AppPref", MODE_PRIVATE);
		Access = (TextView) findViewById(R.id.Access);
		auth = (Button) findViewById(R.id.auth);
		auth.setOnClickListener(new View.OnClickListener() {
			Dialog auth_dialog;

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				auth_dialog = new Dialog(ActivitySignInWithGPlus.this);
				auth_dialog.setContentView(R.layout.auth_dialog);
				web = (WebView) auth_dialog.findViewById(R.id.webv);
				web.getSettings().setJavaScriptEnabled(true);
				web.loadUrl(OAUTH_URL + "?redirect_uri=" + REDIRECT_URI
						+ "&response_type=code&client_id=" + CLIENT_ID
						+ "&scope=" + OAUTH_SCOPE);
				web.setWebViewClient(new WebViewClient() {

					boolean authComplete = false;
					Intent resultIntent = new Intent();

					@Override
					public void onPageStarted(WebView view, String url,
							Bitmap favicon) {
						super.onPageStarted(view, url, favicon);

					}

					String authCode;

					@Override
					public void onPageFinished(WebView view, String url) {
						super.onPageFinished(view, url);

						if (url.contains("?code=") && authComplete != true) {
							Uri uri = Uri.parse(url);
							authCode = uri.getQueryParameter("code");
							Log.i("", "CODE : " + authCode);
							authComplete = true;
							resultIntent.putExtra("code", authCode);
							ActivitySignInWithGPlus.this.setResult(
									Activity.RESULT_OK, resultIntent);
							setResult(Activity.RESULT_CANCELED, resultIntent);

							SharedPreferences.Editor edit = pref.edit();
							edit.putString("Code", authCode);
							edit.commit();
							auth_dialog.dismiss();
							new TokenGet().execute();
							Toast.makeText(getApplicationContext(),
									"Authorization Code is: " + authCode,
									Toast.LENGTH_SHORT).show();
						} else if (url.contains("error=access_denied")) {
							Log.i("", "ACCESS_DENIED_HERE");
							resultIntent.putExtra("code", authCode);
							authComplete = true;
							setResult(Activity.RESULT_CANCELED, resultIntent);
							Toast.makeText(getApplicationContext(),
									"Error Occured", Toast.LENGTH_SHORT).show();

							auth_dialog.dismiss();
						}
					}
				});
				auth_dialog.show();
				auth_dialog.setTitle("Authorize Learn2Crack");
				auth_dialog.setCancelable(true);
			}
		});
	}

	private class TokenGet extends AsyncTask<String, String, JSONObject> {
		private ProgressDialog pDialog;
		String Code;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(ActivitySignInWithGPlus.this);
			pDialog.setMessage("Contacting Google ...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(true);
			Code = pref.getString("Code", "");
			pDialog.show();
		}

		@Override
		protected JSONObject doInBackground(String... args) {
			GetAccessToken jParser = new GetAccessToken();
			JSONObject json = jParser.gettoken(TOKEN_URL, Code, CLIENT_ID,
					REDIRECT_URI, GRANT_TYPE);
			return json;
		}

		@Override
		protected void onPostExecute(JSONObject json) {
			pDialog.dismiss();
			if (json != null) {
				HttpURLConnection urlConnection = null;
				try {
					String tok = json.getString("access_token");
					String expire = json.getString("expires_in");
					String refresh = json.getString("refresh_token");

					Log.d("Token Access", tok);
					Log.d("Expire", expire);
					Log.d("Refresh", refresh);
					auth.setText("Authenticated");
					Access.setText("Access Token:" + tok + "\nExpires:"
							+ expire + "\nRefresh Token:" + refresh
							+ "\nemail id is");

					try {
						StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

						StrictMode.setThreadPolicy(policy);
						URL url = new URL(
								" https://www.googleapis.com/userinfo/v2/me");
						urlConnection = (HttpURLConnection) url
								.openConnection();
						urlConnection.setRequestProperty("Authorization",
								"Bearer " + tok);
						BufferedReader reader = new BufferedReader(
								new InputStreamReader(
										urlConnection.getInputStream()));
						StringBuilder sb = new StringBuilder();

						String line = null;
						while ((line = reader.readLine()) != null) {
							sb.append(line + "\n");
						}
						String result = sb.toString();

						if (!TextUtils.isEmpty(result)) {
							String email = new JSONObject(result)
									.getString("email");

							Log.i("hari", "email id is" + email);

						}

					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else {
				Toast.makeText(getApplicationContext(), "Network Error",
						Toast.LENGTH_SHORT).show();
				pDialog.dismiss();
			}
		}
	}

}
