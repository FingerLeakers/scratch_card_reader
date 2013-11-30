package org.cgiar.ilri.lab;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.googlecode.tesseract.android.TessBaseAPI;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

/**
 * This is the main activity (duh). Check Preview.java since that is where all the cool stuff is done. But do remember that everything starts here
 * 
 * "I'm trying to free your mind, Neo, but I can only show you the door. You're the one that has to walk through it."
 * 
 * @author Jason Rogena
 *
 */
public class MainActivity extends Activity implements OnClickListener
{
	public static final String DATA_PATH = Environment.getExternalStorageDirectory()+File.separator+"SCR"+File.separator;
	
	private Preview preview=null;
	private Camera camera;
	private FrameLayout previewFrameLayout;
	private ImageView focusAreaImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
    }
    
    @Override
	protected void onResume() 
    {
		super.onResume();
		stopService(new Intent(this, SenderService.class));
		focusAreaImg = (ImageView)this.findViewById(R.id.focus_area_img);
		if(preview==null)
		{
			RelativeLayout mainLayout=(RelativeLayout)this.findViewById(R.id.main_layout);
			View upperLimit=(View)this.findViewById(R.id.upper_limit);
			View lowerLimit=(View)this.findViewById(R.id.lower_limit);
			View leftLimit = (View)this.findViewById(R.id.left_limit);
			View rightLimit = (View)this.findViewById(R.id.right_limit);
			ImageView lastImageIV = (ImageView)this.findViewById(R.id.last_image);
			previewFrameLayout=(FrameLayout)this.findViewById(R.id.preview);
			preview=new Preview(this,mainLayout,previewFrameLayout,upperLimit,lowerLimit,leftLimit,rightLimit,lastImageIV);
	        previewFrameLayout.addView(preview);
	        previewFrameLayout.setOnClickListener(this);
	        //SampleSender sampleSender =new SampleSender();
	        //sampleSender.execute(this);
		}
		else{
			preview.resume();
		}
	}
    
    
    
	@Override
	protected void onPause() {
		preview.pause();
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
		startService(new Intent(this, SenderService.class));
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

	@Override
	public void onClick(View v) 
	{
		if(v==previewFrameLayout)
		{
			if(focusAreaImg.getVisibility() == ImageView.VISIBLE) {
				focusAreaImg.setVisibility(ImageView.GONE);
			}
			preview.autoFocus();
		}
	}
	
}
