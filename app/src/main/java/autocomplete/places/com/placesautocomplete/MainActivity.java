package autocomplete.places.com.placesautocomplete;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {


    private int CUSTOM_AUTOCOMPLETE_REQUEST_CODE = 13;
    private TextView txtLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView picker= (ImageView)findViewById(R.id.btn_picker);
        picker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i= new Intent(MainActivity.this, PickLocationActivity.class);
                startActivityForResult(i, CUSTOM_AUTOCOMPLETE_REQUEST_CODE);
            }
        });

        txtLocation=(TextView)findViewById(R.id.location);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode ==  CUSTOM_AUTOCOMPLETE_REQUEST_CODE){
            if(data!=null) {
                // set the location recieved from picklocation activity
                txtLocation.setText(data.getStringExtra("Location Address"));

            }
        }
    }

}
