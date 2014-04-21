package org.fablabsaigon.playitcool;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ReadCheckpointActivity
    extends Activity
{

    protected static final String CHECKPOINT_DATA = "checkpoint_data";

    protected static final String CHECKPOINT_TEMP = "checkpoint_temperature";

    protected static final String CHECKPOINT_HUM = "checkpoint_humidity";

    protected static final String CHECKPOINT_LAT = "checkpoint_latitude";

    protected static final String CHECKPOINT_LON = "checkpoint_longitude";

    private BluetoothDevice mmDevice;

    private BluetoothSocket mmSocket;

    private OutputStream mmOutputStream;

    private InputStream mmInputStream;

    private Thread workerThread;

    private byte[] readBuffer;

    private int readBufferPosition;

    private volatile boolean stopWorker;

    private String myRawData;

    private Button submitBtn;

    private Intent submitIntent;

    private String myLon, myLat;

    private String myTeam;

    private List<String> myTemps;

    private List<String> myHums;

    // private ProgressBar pb;

    private TextView numRec;

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_read_checkpoint_ );

        // pb = (ProgressBar) findViewById( R.id.progressBar );
        // pb.setVisibility( View.GONE );

        numRec = (TextView) findViewById( R.id.number_records );

        submitBtn = (Button) findViewById( R.id.button_stop_send );
        submitBtn.setEnabled( false );

        // Get the device from the intent
        Intent intent = getIntent();
        // mmDevice = (BluetoothDevice) intent
        // .getParcelableExtra(SelectCheckpointActivity.BLUETOOTH_DEVICE);
        myTeam = intent.getStringExtra( MainActivity.CHOSEN_TEAM );

        View view = this.findViewById( R.id.layout_team );

        if ( this.getString( R.string.team_engagers ).equalsIgnoreCase( myTeam ) )
        {
            view.setBackgroundColor( this.getResources().getColor( R.color.engagers ) );
        }
        else
        {
            view.setBackgroundColor( this.getResources().getColor( R.color.resilients ) );
        }

        TextView latestRecord = (TextView) this.findViewById( R.id.latest_data );

//        SimpleDateFormat simpleDateFormat = new SimpleDateFormat( "@k:m, MMMM dd, yyyy" );
//
//        latestRecord.setText( this.getString( R.string.latest_data,
//            simpleDateFormat.format( Calendar.getInstance().getTime() ) ) );
        // remove "The"
        myTeam = myTeam.replaceAll( "The ", "" );

        submitBtn.setOnClickListener( new OnClickListener()
        {

            @Override
            public void onClick( View v )
            {
                submitIntent = new Intent();
                submitIntent.setClass( getApplicationContext(), SendDataActivity.class );
                JSONObject data = new JSONObject();
                try
                {
                    data.put( "team", myTeam );
                    data.put( "temperature", new JSONArray( myTemps ) );
                    data.put( "humidity", new JSONArray( myHums ) );
                    data.put( "checkpoint_lat", myLat );
                    data.put( "checkpoint_lon", myLon );
                }
                catch ( JSONException e )
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                submitIntent.putExtra( CHECKPOINT_DATA, data.toString() );
                submitIntent.putExtra( MainActivity.CHOSEN_TEAM, myTeam );
                startActivity( submitIntent );
            }
        } );

        // send data to server
        SensorFetcher fetcher = new SensorFetcher();
        // pb.setVisibility( View.VISIBLE );
        fetcher.execute();

    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate( R.menu.read_checkpoint, menu );
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

    private class SensorFetcher
        extends AsyncTask<Void, Integer, String>
    {

        @Override
        protected String doInBackground( Void... arg0 )
        {
            // getDataFromSensor();
            myRawData = "Temp:[33,33];Hum:[55,55];Lat:10.81449;Lon:106.690518";
            return myRawData;
        }

        @Override
        protected void onPostExecute( String result )
        {
            // pb.setVisibility( View.GONE );
            parseDisplayData();
            submitBtn.setEnabled( true );
        }

        @Override
        protected void onProgressUpdate( Integer... progress )
        {
            // pb.setProgress( progress[0] );
        }
    }

    private void getDataFromSensor()
    {
        try
        {
            openBT();
            sendData();
            beginListenForData();
        }
        catch ( IOException ex )
        {
            Toast.makeText( getApplicationContext(), "Failed to get data from checkpoint", Toast.LENGTH_SHORT ).show();
        }

    }

    private void parseDisplayData()
    {
        String delims = ";[ ]*";
        String[] cutData = myRawData.split( delims );

        TextView temp_view = (TextView) findViewById( R.id.temperature_data );
        TextView hum_view = (TextView) findViewById( R.id.humidity_data );

        String temps = cutData[0].split( ":" )[1];
        String hums = cutData[1].split( ":" )[1];

        temps = temps.replaceAll( "\\[", "" ).replaceAll( "\\]", "" );
        hums = hums.replaceAll( "\\[", "" ).replaceAll( "\\]", "" );

        myTemps = Arrays.asList( temps.split( "\\s*,\\s*" ) );
        myHums = Arrays.asList( hums.split( "\\s*,\\s*" ) );

        myLat = cutData[2].split( ":" )[1];
        myLon = cutData[3].split( ":" )[1];

        temp_view.setText( myTemps.get( 0 ) + "°C" );
        hum_view.setText( myHums.get( 0 ) + "%" );

        // Display the number of records available
        numRec.setText( myTemps.size() + "" );

    }

    void openBT()
        throws IOException
    {
        UUID uuid = UUID.fromString( "00001101-0000-1000-8000-00805f9b34fb" ); // Standard
                                                                               // SerialPortService
                                                                               // ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord( uuid );

        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();

        Toast.makeText( getApplicationContext(), "Connected to checkpoint", Toast.LENGTH_LONG ).show();
    }

    void sendData()
        throws IOException
    {
        String msg = "1";
        mmOutputStream.write( msg.getBytes() );
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; // This is the ASCII code for a newline
                                   // character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread( new Runnable()
        {
            public void run()
            {
                while ( !Thread.currentThread().isInterrupted() && !stopWorker )
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if ( bytesAvailable > 0 )
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read( packetBytes );
                            for ( int i = 0; i < bytesAvailable; i++ )
                            {
                                byte b = packetBytes[i];
                                if ( b == delimiter )
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy( readBuffer, 0, encodedBytes, 0, encodedBytes.length );
                                    final String data = new String( encodedBytes, "US-ASCII" );
                                    readBufferPosition = 0;

                                    handler.post( new Runnable()
                                    {
                                        public void run()
                                        {
                                            myRawData = data;
                                            submitBtn.setEnabled( true );
                                        }
                                    } );
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch ( IOException ex )
                    {
                        stopWorker = true;
                    }
                }
            }
        } );

        workerThread.start();
    }

    void closeBT()
        throws IOException
    {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        // unpair();
    }

    void unpair()
    {
        // Unpair the device (not really needed)
        Method method;
        try
        {
            method = mmDevice.getClass().getMethod( "removeBond", (Class[]) null );
            method.invoke( mmDevice, (Object[]) null );
        }
        catch ( NoSuchMethodException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch ( Exception e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
