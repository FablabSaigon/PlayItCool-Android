package org.fablabsaigon.playitcool;

import java.util.ArrayList;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SelectCheckpointActivity
    extends Activity
{

    public static final String BLUETOOTH_DEVICE = "org.fablabsaigon.playitcool.BLUETOOTH_DEVICE";

    public static final String SENSOR_NAME = "SeeedBTSlave";

    private static final int REQUEST_ENABLE_BT = 1;

    private Button findBtn;

    private Button checkinBtn;

    private BluetoothAdapter myBluetoothAdapter;

    private ListView myListView;

    private Set<BluetoothDevice> pairedDevices;

    private ArrayList<BluetoothDevice> btDevices;

    private ArrayAdapter<String> BTArrayAdapter;

    private BluetoothDevice mmDevice;

    private Intent readIntent;

    private String team;

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_select_checkpoint_ );

        // Get the team name from the intent
        Intent intent = getIntent();
        team = intent.getStringExtra( MainActivity.CHOSEN_TEAM );

        View view = this.findViewById( R.id.layout_team );

        if ( this.getString( R.string.team_engagers ).equalsIgnoreCase( team ) )
        {
            view.setBackgroundColor( this.getResources().getColor( R.color.engagers ) );
        }
        else
        {
            view.setBackgroundColor( this.getResources().getColor( R.color.resilients ) );
        }

        // Retrieve the text view
        TextView textView = (TextView) findViewById( R.id.team_selected );
        textView.setText( team );

        // Need to select checkpoint before enabling checkin
        checkinBtn = (Button) findViewById( R.id.button_checkin );
        checkinBtn.setEnabled( false );

        // create the arrayAdapter that contains the BTDevices, and set it to
        // the ListView
        myListView = (ListView) findViewById( R.id.list_checkpoint );
        BTArrayAdapter = new ArrayAdapter<String>( this, android.R.layout.simple_list_item_single_choice );

        btDevices = new ArrayList<BluetoothDevice>();
        myListView.setAdapter( BTArrayAdapter );
        myListView.setChoiceMode( ListView.CHOICE_MODE_SINGLE );
        myListView.setOnItemClickListener( new OnItemClickListener()
        {
            @Override
            public void onItemClick( AdapterView<?> l, View v, int position, long id )
            {
                checkinBtn.setEnabled( true );
                mmDevice = btDevices.get( position );

                ((CheckedTextView) v.findViewById( android.R.id.text1 )).setChecked( true );
            }
        } );

        // take an instance of BluetoothAdapter - Bluetooth radio
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if ( myBluetoothAdapter == null )
        {
            findBtn.setEnabled( false );
            Toast.makeText( getApplicationContext(), "Your device does not support Bluetooth", Toast.LENGTH_LONG )
                .show();
        }
        else
        {
            findBtn = (Button) findViewById( R.id.button_detect );
            findBtn.setOnClickListener( new OnClickListener()
            {

                @Override
                public void onClick( View v )
                {
                    listPairedDevices();
                    detectNewDevices();
                }
            } );

            checkinBtn.setOnClickListener( new OnClickListener()
            {

                @Override
                public void onClick( View v )
                {
                    readIntent = new Intent();
                    readIntent.setClass( getApplicationContext(), ReadCheckpointActivity.class );
                    readIntent.putExtra( SelectCheckpointActivity.BLUETOOTH_DEVICE, mmDevice );
                    readIntent.putExtra( MainActivity.CHOSEN_TEAM, team );
                    startActivity( readIntent );
                }
            } );

        }
    }

    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data )
    {
        // TODO Auto-generated method stub
        if ( requestCode == REQUEST_ENABLE_BT )
        {
            if ( myBluetoothAdapter.isEnabled() )
            {
                Toast.makeText( getApplicationContext(), "Bluetooth enabled", Toast.LENGTH_LONG ).show();
            }
            else
            {
                Toast.makeText( getApplicationContext(), "Bluetooth disabled", Toast.LENGTH_LONG ).show();
            }
        }
    }

    @Override
    protected void onDestroy()
    {
        // TODO Auto-generated method stub
        try
        {
            unregisterReceiver( bReceiver );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        super.onDestroy();

    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate( R.menu.connecting_to, menu );
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

    private void detectNewDevices()
    {
        if ( !myBluetoothAdapter.isEnabled() )
        {
            Intent turnOnIntent = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE );
            startActivityForResult( turnOnIntent, REQUEST_ENABLE_BT );
        }

        if ( myBluetoothAdapter.isDiscovering() )
        {
            // the button is pressed when it discovers, so cancel the discovery
            myBluetoothAdapter.cancelDiscovery();
        }
        else
        {
            myBluetoothAdapter.startDiscovery();
            Toast.makeText( getApplicationContext(), "Looking for new Sensor Checkpoints", Toast.LENGTH_LONG ).show();
            registerReceiver( bReceiver, new IntentFilter( BluetoothDevice.ACTION_FOUND ) );
            registerReceiver( bReceiver, new IntentFilter( BluetoothAdapter.ACTION_DISCOVERY_FINISHED ) );
        }
    }

    final BroadcastReceiver bReceiver = new BroadcastReceiver()
    {
        public void onReceive( Context context, Intent intent )
        {
            String action = intent.getAction();
            // When discovery finds a device
            if ( BluetoothDevice.ACTION_FOUND.equals( action ) )
            {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra( BluetoothDevice.EXTRA_DEVICE );
                addSensorCheckpoint( device );
            }
            // When discovery cycle finished
            if ( BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals( action ) )
            {
                if ( BTArrayAdapter == null || BTArrayAdapter.isEmpty() )
                {
                    Toast.makeText( getApplicationContext(), "No new Sensor Checkpoints found", Toast.LENGTH_LONG )
                        .show();
                }
            }
        }

    };

    private void listPairedDevices()
    {
        // get paired devices
        Toast.makeText( getApplicationContext(), "Looking for paired Sensor Checkpoints", Toast.LENGTH_LONG ).show();
        pairedDevices = myBluetoothAdapter.getBondedDevices();

        for ( BluetoothDevice device : pairedDevices )
        {
            addSensorCheckpoint( device );
        }

        if ( BTArrayAdapter == null || BTArrayAdapter.isEmpty() )
        {
            Toast.makeText( getApplicationContext(), "No paired Sensor Checkpoints found", Toast.LENGTH_LONG ).show();
        }

    }

    private void addSensorCheckpoint( BluetoothDevice device )
    {
        if ( device != null )
        {
            String device_name = device.getName();
            // if (device_name.equals(SelectCheckpointActivity.SENSOR_NAME)) {
            // check that the device hasn't been added already
            String mac_address = device.getAddress();
            if ( !btDevices.contains( device ) )
            {
                btDevices.add( device );
                // add the name and the MAC address of the object to the
                // arrayAdapter
                BTArrayAdapter.add( device_name + "\n" + mac_address + "\n bond state:" + device.getBondState() );
                BTArrayAdapter.notifyDataSetChanged();
            }
            // }
        }
    }

}
