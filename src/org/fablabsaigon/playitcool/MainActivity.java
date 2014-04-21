package org.fablabsaigon.playitcool;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

public class MainActivity
    extends Activity
{

    public static final String CHOSEN_TEAM = "org.fablabsaigon.playitcool.CHOSEN_TEAM";

    private Button team_engagers;

    private Button team_resilients;

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        team_engagers = (Button) findViewById( R.id.team_engagers );
        team_resilients = (Button) findViewById( R.id.team_resilients );

        team_engagers.setOnClickListener( new OnClickListener()
        {

            @Override
            public void onClick( View v )
            {
                Intent intent = new Intent( v.getContext(), SelectCheckpointActivity.class );
                String team = "Engagers";
                intent.putExtra( CHOSEN_TEAM, team );
                startActivity( intent );
            }
        } );

        team_resilients.setOnClickListener( new OnClickListener()
        {

            @Override
            public void onClick( View v )
            {
                Intent intent = new Intent( v.getContext(), SelectCheckpointActivity.class );
                String team = "Resilients";
                intent.putExtra( CHOSEN_TEAM, team );
                startActivity( intent );
            }
        } );

    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate( R.menu.main, menu );
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

}
