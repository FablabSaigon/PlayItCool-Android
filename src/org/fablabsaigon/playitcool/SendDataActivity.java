package org.fablabsaigon.playitcool;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SendDataActivity
    extends Activity
{

    // private ProgressBar pb;

    private TextView score;

    private TextView congrats;

    private ImageButton button_close;
    
    private ProgressDialog dialog;

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_send_data_ );
        
        this.dialog = new ProgressDialog( this );
        this.dialog.setMessage( "Sending data..." );
        this.dialog.show();

        String myTeam = this.getIntent().getStringExtra( MainActivity.CHOSEN_TEAM );

        View view = this.findViewById( R.id.layout_team );

        if ( this.getString( R.string.team_engagers ).equalsIgnoreCase( myTeam ) )
        {
            view.setBackgroundColor( this.getResources().getColor( R.color.engagers ) );
        }
        else
        {
            view.setBackgroundColor( this.getResources().getColor( R.color.resilients ) );
        }

        // pb = (ProgressBar) findViewById( R.id.progressBar1 );
        // pb.setVisibility( View.GONE );
        congrats = (TextView) findViewById( R.id.congratulations );

        score = (TextView) findViewById( R.id.score );

        button_close = (ImageButton) findViewById( R.id.button_close );

        button_close.setOnClickListener( new OnClickListener()
        {

            @Override
            public void onClick( View v )
            {
                finish();
                System.exit( 0 );
            }
        } );

        // Get the location manager
        LocationManager locationManager = (LocationManager) getSystemService( LOCATION_SERVICE );
        Criteria criteria = new Criteria();
        String bestProvider = locationManager.getBestProvider( criteria, false );
        Location location = locationManager.getLastKnownLocation( bestProvider );

        // Get server data
        Bundle b = getIntent().getExtras();
        JSONObject myJSONdata = new JSONObject();

        try
        {
            // parse the JSON passed as a string.
            myJSONdata = new JSONObject( b.getString( ReadCheckpointActivity.CHECKPOINT_DATA ) );
            // adding mobile's location
            myJSONdata.put( "player_lon", location.getLatitude() );
            myJSONdata.put( "player_lat", location.getLongitude() );
        }
        catch ( JSONException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // send data to server
        PostFetcher fetcher = new PostFetcher();
        // pb.setVisibility( View.VISIBLE );
        fetcher.execute( myJSONdata );
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate( R.menu.send_data, menu );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if ( id == R.id.action_settings )
        {
            return true;
        }
        return super.onOptionsItemSelected( item );
    }

    private class DataResult
    {
        public String error;

        public String message;

        public String team;

        public String checkpoint_owner;

        public String point_scored;

        public List<TeamScore> teams;
    }

    private class TeamScore
    {
        public String teamname;

        public String score;
    }

    private void handleResult( DataResult res )
    {
        // pb.setVisibility( View.GONE );
        score.setText( "+" + res.point_scored );
//        score.setVisibility( View.VISIBLE );
//        congrats.setVisibility( View.VISIBLE );
//        button_close.setVisibility( View.VISIBLE );
        this.dialog.dismiss();
    }

    private class PostFetcher
        extends AsyncTask<JSONObject, Integer, DataResult>
    {
        private static final String TAG = "PostFetcher";

        public static final String SERVER_URL = "http://ec2-54-255-160-37.ap-southeast-1.compute.amazonaws.com/rest/add";

        @Override
        protected DataResult doInBackground( JSONObject... params )
        {
            DataResult results = null;
            try
            {
                // Create an HTTP client
                HttpClient client = new DefaultHttpClient();
                HttpPost post = new HttpPost( SERVER_URL );
                post.setHeader( "content-type", "application/json" );
                StringEntity post_entity = new StringEntity( params[0].toString() );
                post.setEntity( post_entity );

                // Perform the request and check the status code
                HttpResponse response = client.execute( post );
                StatusLine statusLine = response.getStatusLine();
                if ( statusLine.getStatusCode() == 200 )
                {
                    HttpEntity entity = response.getEntity();
                    InputStream content = entity.getContent();

                    try
                    {
                        // Read the server response and attempt to parse it as
                        // JSON
                        Reader reader = new InputStreamReader( content );

                        GsonBuilder gsonBuilder = new GsonBuilder();
                        gsonBuilder.setDateFormat( "M/d/yy hh:mm a" );
                        Gson gson = gsonBuilder.create();
                        results = gson.fromJson( reader, DataResult.class );
                        content.close();
                    }
                    catch ( Exception ex )
                    {
                        Log.e( TAG, "Failed to parse JSON due to: " + ex );
                    }
                }
                else
                {
                    Log.e( TAG, "Server responded with status code: " + statusLine.getStatusCode() );
                }
            }
            catch ( Exception ex )
            {
                Log.e( TAG, "Failed to send HTTP POST request due to: " + ex );
            }
            return results;
        }

        @Override
        protected void onPostExecute( DataResult result )
        {
            handleResult( result );
        }

        @Override
        protected void onProgressUpdate( Integer... progress )
        {
            // pb.setProgress( progress[0] );
        }
    }

}
