package org.cgiar.ilri.lab;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import com.googlecode.tesseract.android.TessBaseAPI;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Bitmap.CompressFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.AsyncTask;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

class Preview extends SurfaceView implements SurfaceHolder.Callback 
{
	private static final String TAG = "Preview";

	SurfaceHolder mHolder;
	public Camera camera;
	private int heightOfCamera=-1;
	AutoFocusCallback autoFocusCallback;
	private ShutterCallback shutterCallback;
	private PictureCallback rawCallback;
	private PictureCallback jpegCallback;
	private double activeImageHeight=0.1;//ration of active height on a total image height
	private boolean idle=true;

	Preview(Context context) 
	{
		super(context);

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		shutterCallback=new ShutterCallback()
        {
			
			@Override
			public void onShutter() {
				Log.d("CAMERA", "shutter called");
				
			}
		};
		rawCallback=new PictureCallback()
		{
			
			@Override
			public void onPictureTaken(byte[] data, Camera camera) 
			{
				Log.d("CAMERA", "Picture taken");	
			}
		};
		jpegCallback=new PictureCallback()
		{
			
			@Override
			public void onPictureTaken(byte[] data, Camera camera)
			{
				try
				{
					if(idle)
					{
						Log.d("CAMERA", "thread not idle");
						idle=false;
						OCRHandler handler=new OCRHandler();
						handler.execute(data);
					}
					camera.startPreview();
					
				} catch (Exception e) 
				{
					e.printStackTrace();
				}
				finally
				{
				}
				Log.d("CAMERA", "onPictureTaken - jpeg");
				
			}
		};
		
		autoFocusCallback=new AutoFocusCallback() 
		{
			
			@Override
			public void onAutoFocus(boolean success, Camera camera)
			{
				if(success)
				{
					camera.takePicture(shutterCallback,null,jpegCallback);
				}
				else
				{
					autoFocus();//make sure the endless loop doesnt end
				}
			}
		};
	}

	public void surfaceCreated(SurfaceHolder holder) 
	{
		// The Surface has been created, acquire the camera and tell it where
		// to draw.
		camera = Camera.open();
		Log.d("CAMERA", "camera opened");
		try 
		{
			camera.setPreviewDisplay(holder);
			camera.setPreviewCallback(new PreviewCallback()
			{

				public void onPreviewFrame(byte[] data, Camera arg1)
				{
					/*FileOutputStream outStream = null;
					try 
					{
						outStream = new FileOutputStream(String.format(
								"/sdcard/%d.jpg", System.currentTimeMillis()));
						outStream.write(data);
						outStream.close();
						Log.d(TAG, "onPreviewFrame - wrote bytes: "
								+ data.length);
					} 
					catch (FileNotFoundException e) 
					{
						e.printStackTrace();
					}
					catch (IOException e)
					{
						e.printStackTrace();
					} 
					finally
					{
					}*/
					Preview.this.invalidate();
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) 
	{
		// Surface will be destroyed when we return, so stop the preview.
		// Because the CameraDevice object is not a shared resource, it's very
		// important to release it when the activity is paused.
		camera.stopPreview();
		camera = null;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// Now that the size is known, set up the camera parameters and begin
		// the preview.
		Camera.Parameters parameters = camera.getParameters();
		List<Camera.Size> previewSizes=parameters.getSupportedPreviewSizes();
		int previewWidth=0;
		int previewHeight=0;
		for (int i = 0; i < previewSizes.size(); i++)
		{	
			if(previewSizes.get(i).width>previewWidth)
			{
				previewHeight=previewSizes.get(i).height;
				previewWidth=previewSizes.get(i).width;
			}
		}
		List<Camera.Size> pictureSizes=parameters.getSupportedPictureSizes();
		int pictureHeight=0;
		int pictureWidth=0;
		for(int i=0; i<pictureSizes.size(); i++)
		{
			if(pictureSizes.get(i).width>pictureWidth)
			{
				pictureWidth=pictureSizes.get(i).width;
				pictureHeight=pictureSizes.get(i).height;
			}
		}
		parameters.setPreviewSize(previewWidth, previewHeight);
		parameters.setPictureSize(pictureWidth, pictureHeight);
		parameters.setPictureFormat(ImageFormat.JPEG);
		parameters.setJpegQuality(100);
		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
		Log.d("CAMERA", "preview size :"+String.valueOf(previewWidth)+" x "+String.valueOf(previewHeight));
		Log.d("CAMERA", "picture size :"+String.valueOf(pictureWidth)+" x "+String.valueOf(pictureHeight));
		heightOfCamera=previewWidth;
		camera.setParameters(parameters);
		camera.setDisplayOrientation(90);
		camera.startPreview();
		//autoFocus();
		//camera.autoFocus(autoFocusCallback);
	}
	
	public void autoFocus()
	{
		camera.autoFocus(autoFocusCallback);
	}
	
	public int getHeightOfCamera()
	{
		return heightOfCamera;
	}

	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);
		Paint p = new Paint(Color.RED);
		//Log.d(TAG, "draw");
		canvas.drawText("PREVIEW", canvas.getWidth() / 2,
				canvas.getHeight() / 2, p);
	}
	
	private class OCRHandler extends AsyncTask<byte[], Integer, String>
	{
		
		@Override
		protected void onPreExecute() 
		{
			super.onPreExecute();
			Toast.makeText(Preview.this.getContext(), "starting..", Toast.LENGTH_SHORT).show();
		}

		@Override
		protected void onProgressUpdate(Integer... values) 
		{
			super.onProgressUpdate(values);
			Toast.makeText(Preview.this.getContext(), "still working..", Toast.LENGTH_SHORT).show();
		}

		@Override
		protected String doInBackground(byte[]... data)
		{
			try
			{
				FileOutputStream outStream = new FileOutputStream(String.format(MainActivity.DATA_PATH+"%d.jpg", System.currentTimeMillis()));
				Log.d("CAMERA", "byte size = "+String.valueOf(data[0].length));
				Bitmap bitmap=BitmapFactory.decodeByteArray(data[0], 0, data[0].length);
				Matrix matrix=new Matrix();
				matrix.postRotate(90);
				Bitmap rotatedImage=Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
				int halfHeight=(int)rotatedImage.getHeight()/2;
				int croppedHeight=(int)(rotatedImage.getHeight()*activeImageHeight);
				int y=halfHeight-(int)(croppedHeight/2);
				Bitmap croppedImage=Bitmap.createBitmap(rotatedImage, 0, y, rotatedImage.getWidth(), croppedHeight);
				croppedImage.compress(CompressFormat.JPEG, 100, outStream);
				outStream.close();
				Log.d("CAMERA", "cropped image width = "+String.valueOf(croppedImage.getWidth()));
				if (!(new File(MainActivity.DATA_PATH + "tessdata/eng.traineddata")).exists()) 
				{
					try 
					{
						Log.d("CAMERA", "copying eng to file system");
						AssetManager assetManager = Preview.this.getContext().getAssets();
						InputStream in = assetManager.open("tessdata/eng.traineddata");
						//GZIPInputStream gin = new GZIPInputStream(in);
						OutputStream out = new FileOutputStream(MainActivity.DATA_PATH + "tessdata/eng.traineddata");
						// Transfer bytes from in to out
						byte[] buf = new byte[1024];
						int len;
						//while ((lenf = gin.read(buff)) > 0) {
						while ((len = in.read(buf)) > 0) {
							out.write(buf, 0, len);
						}
						in.close();
						//gin.close();
						out.close();

						Log.d("CAMERA", "Finished copying eng");
					} 
					catch (IOException e) 
					{
						Log.e("CAMERA", "Was unable to copy eng traineddata " + e.toString());
					}
				}
				else
				{
					Log.d("CAMERA", "eng already on sd");
				}
				Log.d("CAMERA", "Calling TessBaseAPI");
				TessBaseAPI baseAPI=new TessBaseAPI();
				baseAPI.setDebug(true);
				Log.d("CAMERA", "1");
				baseAPI.init(MainActivity.DATA_PATH, "eng");
				Log.d("CAMERA", "2");
				baseAPI.setImage(croppedImage);
				Log.d("CAMERA", "3");
				String result=baseAPI.getUTF8Text();
				Log.d("CAMERA", "4");
				baseAPI.end();
				Log.d("CAMERA", "TessBaseAPI finished analyzing");
				
				//result=result.replaceAll("\\s", "");//remove all whitespaces
				result=result.replaceAll("o", "0");//sometimes 0s are identified as os by the ocr lib
				result=result.replaceAll("[^0123456789]", "");//remove all none numbers
				
				return result;
				
			} catch (Exception e) 
			{
				e.printStackTrace();
				System.err.print(e.getMessage());
			}
			finally
			{
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result)
		{
			super.onPostExecute(result);
			if(result!=null)
			{
				Log.d("CAMERA", result);
				if(result.length()==16||result.length()==15)
				{
					Toast.makeText(Preview.this.getContext(), result+" nice!!", Toast.LENGTH_LONG).show();
				}
			}
			idle=true;
			autoFocus();
		}
	}
}