package uk.co.ormand.teaseme;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

public class AndroidTeaseMeActivity extends Activity {
	private File objSDRoot;
	private File objPresFolder;
	private String strPresentationPath;
	private DocumentBuilderFactory objDocumentBuilderFactory;
	private DocumentBuilder objDocumentBuilder;
	private String strMediaDirectory;
	private Document objDocPresXML;
	private Element objPagesElement;
	private PageTimer tmrPageTimer;
	private Timer tmrTetronome;
	private WebView objWebView1;
	private TextView objCountText;
	private String strDelStyle;
	private String strDelTarget;
	private Boolean blnDebug;
	private int intHeightTop;
	private int intWidthTop;
	private int intWidthImage;
	private int intTextViewHeight;
	private int intRightHeight;
	private int intRightWidth;
	private int intBtnHeight;
	private LinearLayout objLayoutImage;
	private SoundPool soundPool;
	private int sound;
	private int MintFontSize;
	private Random rndGen;
	private String strFilePatern;
	private String strPasswordEntered;
	private String strPrefPassword;
	private List<String> Flags;
	private Boolean blnAutoSetPage;
	private String strDelaySet;
	private String strDelayUnSet;
	private String strSounds[];
	private int intSounds[];
	private int intSoundCount;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);

			setContentView(R.layout.main);

			rndGen = new Random();

			DisplayMetrics dm = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(dm);

			// objLayoutTop = (LinearLayout) findViewById(R.id.LayoutTop);
			intHeightTop = dm.heightPixels;
			intWidthTop = dm.widthPixels;

			objLayoutImage = (LinearLayout) findViewById(R.id.LayoutImage);
			ViewGroup.LayoutParams objLayoutParm = objLayoutImage
					.getLayoutParams();
			intWidthImage = intWidthTop / 2;
			objLayoutParm.width = intWidthImage;
			objLayoutImage.setLayoutParams(objLayoutParm);

			LinearLayout objLayoutRight = (LinearLayout) findViewById(R.id.LayoutRight);
			objLayoutParm = objLayoutRight.getLayoutParams();
			intRightWidth = intWidthTop - intWidthImage;
			objLayoutParm.width = intRightWidth;
			objLayoutRight.setLayoutParams(objLayoutParm);

			SharedPreferences sharedPrefs = PreferenceManager
					.getDefaultSharedPreferences(this);
			blnDebug = sharedPrefs.getBoolean("Debug", false);
			MintFontSize = Integer.parseInt(sharedPrefs.getString("FontSize",
					"10"));

			LinearLayout objLayoutTextTop = (LinearLayout) findViewById(R.id.LayoutTextTop);
			objLayoutParm = objLayoutTextTop.getLayoutParams();
			intTextViewHeight = intHeightTop / 20;
			objLayoutParm.height = intTextViewHeight;
			objLayoutTextTop.setLayoutParams(objLayoutParm);
			intRightHeight = intHeightTop - intTextViewHeight;
			float fltH = intRightHeight / 10;
			fltH = fltH * MintFontSize;
			fltH = fltH / 8;
			intBtnHeight = (int) fltH;

			// strBaseTitle = (String) getTitle();
			objWebView1 = (WebView) findViewById(R.id.webView1);
			objCountText = (TextView) findViewById(R.id.textView1);
			objSDRoot = Environment.getExternalStorageDirectory();
			strPresentationPath = objSDRoot.getAbsolutePath()
					+ "/Android/data/uk.co.ormand.teaseme/files/";
			objPresFolder = new File(strPresentationPath);

			// write .nomedia
			@SuppressWarnings("unused")
			boolean blnTry;
			if (!objPresFolder.exists()) {
				blnTry = objPresFolder.mkdirs();
			}
			File objNoMedia = new File(strPresentationPath + ".nomedia");
			if (!objNoMedia.exists()) {
				blnTry = objNoMedia.createNewFile();
			}

			strPrefPassword = sharedPrefs.getString("Password", "");
			if (!strPrefPassword.equals("")) {
				this.showDialog(DIALOG_PASSWORD_ENTER);
			}

			Flags = new ArrayList<String>();
		} catch (NumberFormatException e) {
			Log.e("onCreate",
					"NumberFormatException " + e.getLocalizedMessage());
		} catch (Exception e) {
			Log.e("onCreate", "Exception " + e.getLocalizedMessage());
		}
	}

	// onclick listener for dynamic buttons
	View.OnClickListener getOnClickDoSomething(final Button button) {
		return new View.OnClickListener() {
			public void onClick(View v) {
				try {
					String strTag;
					strTag = (String) button.getTag(R.string.TagSetFlags);
					if (!strTag.equals("")) {
						SetFlags(strTag);
					}
					strTag = (String) button.getTag(R.string.TagUnSetFlags);
					if (!strTag.equals("")) {
						UnsetFlags(strTag);
					}
					strTag = (String) button.getTag(R.string.TagPage);
					displayPage(strTag);
				} catch (Exception e) {
					Log.e("onClick", "Exception " + e.getLocalizedMessage());
				}
			}
		};
	}

	// Prefernces Menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, 0, 0, "Load");
		menu.add(Menu.NONE, 1, 0, "Preferences");
		menu.add(Menu.NONE, 2, 0, "Exit");
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		try {
			switch (item.getItemId()) {
			case 0:
				loadFileList();
				showDialog(DIALOG_LOAD_FILE);
				return true;
			case 2:
				finish();
				return true;
			case 1:
				startActivity(new Intent(this, QuickPrefsActivity.class));
				return true;
			}
		} catch (Exception e) {
			Log.e("onClick", "Exception " + e.getLocalizedMessage());
		}
		return false;

	}

	public void displayPage(String pageName) {
		String strId;
		Element elPage;
		Element elImage;
		Element elDelay;
		Element elButton;
		Element elMetronome;
		Element elAudio;
		Node elText;
		String strImage;
		String strHTML;
		String strDelSeconds;
		String strBtnTarget;
		String strBtnText;
		int intDelSeconds;
		int intPos1;
		int intPos2;
		int intPos3;
		int intMin;
		int intMax;
		int intDpLeft;
		int intRows;
		String strMin;
		String strMax;
		String strPre;
		String strPost;
		String strPageName;
		String strSet;
		String strTest;
		boolean blnTestSet;
		boolean blnTestNotSet;
		NodeList pageNodeList;
		NodeList tmpNodeList;
		LinearLayout btnLayoutRow = null;
		ViewGroup.LayoutParams layout;
		ImageView objImageView;
		VideoView objVideoView;
		String imgPath = null;

		try {
			if (tmrPageTimer != null) {
				tmrPageTimer.cancel();
			}
			if (tmrTetronome != null) {
				tmrTetronome.cancel();
				tmrTetronome = null;
			}

			soundPool.stop(intSoundStream);

			// handle random page
			strPageName = pageName;
			intPos1 = strPageName.indexOf("(");
			if (intPos1 > -1) {
				intPos2 = strPageName.indexOf("..", intPos1);
				if (intPos2 > -1) {
					intPos3 = strPageName.indexOf(")", intPos2);
					if (intPos3 > -1) {
						strMin = strPageName.substring(intPos1 + 1, intPos2);
						intMin = Integer.parseInt(strMin);
						strMax = strPageName.substring(intPos2 + 2, intPos3);
						intMax = Integer.parseInt(strMax);
						if (intPos1 > 0) {
							strPre = strPageName.substring(0, intPos1);
						} else {
							strPre = "";
						}
						strPost = strPageName.substring(intPos3 + 1);
						String[] strPageArray;
						strPageArray = new String[intMax];
						int intPageArrayCount = -1;
						// Check if we are allowed to display the pages
						for (int i = 1; i <= intMax; i++) {
							strPageName = strPre + i + strPost;
							if (AllowedToShowPage(strPageName)) {
								intPageArrayCount++;
								strPageArray[intPageArrayCount] = strPageName;
							}
						}
						int i1 = 0;
						if (intPageArrayCount > 0) {
							// show one of the allowed random pages
							Random r = new Random();
							i1 = r.nextInt(intPageArrayCount + 1);
						}
						strPageName = strPageArray[i1];
					}
				}
			}

			// set page
			if (blnAutoSetPage) {
				Flags.add(strPageName);
			}

			// display page
			pageNodeList = objPagesElement.getElementsByTagName("Page");
			// loop through till we find the page
			for (int i = 0; i < pageNodeList.getLength(); i++) {
				elPage = (Element) pageNodeList.item(i);
				strId = elPage.getAttribute("id");
				if (strId.equals(strPageName)) {
					// found the page so display it

					// do page set / unset
					strSet = elPage.getAttribute("set");
					if (!strSet.equals("")) {
						SetFlags(strSet);
					}
					strSet = elPage.getAttribute("unset");
					if (!strSet.equals("")) {
						UnsetFlags(strSet);
					}

					// can't have a video and an image
					// Video
					tmpNodeList = elPage.getElementsByTagName("Video");
					elImage = (Element) tmpNodeList.item(0);
					if (elImage != null) {
						strImage = elImage.getAttribute("id");
						imgPath = strPresentationPath + strMediaDirectory + "/"
								+ strImage;
						objVideoView = new VideoView(this);
						objLayoutImage.removeAllViews();
						objLayoutImage.addView(objVideoView);
						layout = objVideoView.getLayoutParams();
						layout.width = ViewGroup.LayoutParams.FILL_PARENT;
						layout.height = ViewGroup.LayoutParams.FILL_PARENT;
						objVideoView.setVideoURI(Uri.parse(imgPath));
						objVideoView.start();
					} else {
						// image
						tmpNodeList = elPage.getElementsByTagName("Image");
						elImage = (Element) tmpNodeList.item(0);
						strImage = elImage.getAttribute("id");
						int intSubDir = strImage.lastIndexOf("/");
						String strSubDir;
						if (intSubDir > -1) {
							strSubDir = "/"
									+ strImage.substring(0, intSubDir + 1);
							strImage = strImage.substring(intSubDir + 1);
						} else {
							strSubDir = "";
						}
						// String strSubDir
						// Handle wildcard *
						if (strImage.indexOf("*") > -1) {
							strFilePatern = strImage;
							// get the directory
							File f = new File(strPresentationPath
									+ strMediaDirectory + strSubDir);
							// wildcard filter class handles the filtering
							java.io.FileFilter WildCardfilter = new WildCardFileFilter();
							if (f.isDirectory()) {
								// return a list of matching files
								File[] children = f.listFiles(WildCardfilter);
								// return a random image
								int intFile = rndGen
										.nextInt(children.length);
								imgPath = strPresentationPath
										+ strMediaDirectory + strSubDir + "/"
										+ children[intFile].getName();
							}
						} else {
							// no wildcard so just use the file name
							imgPath = strPresentationPath + strMediaDirectory
									+ strSubDir + "/" + strImage;
						}
						// we create a new image view every time (we may have
						// displayed a video last time)
						objImageView = new ImageView(this);
						// decodeSampledBitmapFromFile will resize the image
						// before it gets to memory
						// we can load large images using memory efficiently
						// (and not run out of memory and crash the app)
						objImageView
								.setImageBitmap(decodeSampledBitmapFromFile(
										imgPath, intWidthImage, intHeightTop));
						// Clear the parent layout
						objLayoutImage.removeAllViews();
						// add the new image to it and set it to fill the
						// available area
						objLayoutImage.addView(objImageView);
						layout = objImageView.getLayoutParams();
						layout.width = ViewGroup.LayoutParams.FILL_PARENT;
						layout.height = ViewGroup.LayoutParams.FILL_PARENT;
						objImageView
								.setScaleType(ImageView.ScaleType.FIT_CENTER);
					}

					// text
					tmpNodeList = elPage.getElementsByTagName("Text");
					elText = tmpNodeList.item(0);
					strHTML = "<html><head><title/></head><body style=\"background-color:black; color:white; font-family: Tahoma; font-size:"
							+ MintFontSize
							+ "pt\">"
							+ getInnerXml(elText, true) + "</body></html>";
					objWebView1.loadData(strHTML, "text/html", null);

					// delay
					//
					strDelaySet = "";
					strDelayUnSet = "";
					tmpNodeList = elPage.getElementsByTagName("Delay");
					elDelay = (Element) tmpNodeList.item(0);

					if (elDelay != null) {
						// test to see if we need this delay
						blnTestSet = true;
						blnTestNotSet = true;
						strTest = elDelay.getAttribute("if-set");
						if (!strTest.equals("")) {
							blnTestSet = MatchesIfSetCondition(strTest);
						}
						strTest = elDelay.getAttribute("if-not-set");
						if (!strTest.equals("")) {
							blnTestNotSet = MatchesIfNotSetCondition(strTest);
						}

						if (blnTestSet && blnTestNotSet) {
							strDelSeconds = elDelay.getAttribute("seconds");
							strDelStyle = elDelay.getAttribute("style");
							strDelTarget = elDelay.getAttribute("target");
							// record any delay set / unset
							strSet = elDelay.getAttribute("set");
							if (!strSet.equals("")) {
								strDelaySet = strSet;
							}
							strSet = elDelay.getAttribute("unset");
							if (!strSet.equals("")) {
								strDelayUnSet = strSet;
							}
							// handle random Delay
							intPos1 = strDelSeconds.indexOf("(");
							intDelSeconds = 0;
							if (intPos1 > -1) {
								intPos2 = strDelSeconds.indexOf("..", intPos1);
								if (intPos2 > -1) {
									intPos3 = strDelSeconds.indexOf(")",
											intPos2);
									if (intPos3 > -1) {
										strMin = strDelSeconds.substring(
												intPos1 + 1, intPos2);
										intMin = Integer.parseInt(strMin);
										strMax = strDelSeconds.substring(
												intPos2 + 2, intPos3);
										intMax = Integer.parseInt(strMax);
										Random r = new Random();
										int i1 = r.nextInt(intMax - intMin)
												+ intMin;
										intDelSeconds = i1;
									}
								}
							} else {
								intDelSeconds = Integer.parseInt(strDelSeconds);
							}
							intDelSeconds = intDelSeconds * 1000;
							tmrPageTimer = new PageTimer(intDelSeconds, 1000);
							tmrPageTimer.start();
						}
					} else {
						objCountText.setText("");
					}

					// buttons
					// remove old buttons
					LinearLayout btnLayout = (LinearLayout) findViewById(R.id.btnLayout);
					btnLayout.removeAllViews();

					// add new buttons
					tmpNodeList = elPage.getElementsByTagName("Button");
					intDpLeft = intRightWidth;
					intRows = 1;
					btnLayoutRow = new LinearLayout(this);
					btnLayout.addView(btnLayoutRow);
					for (int i1 = tmpNodeList.getLength() - 1; i1 >= 0; i1--) {
						elButton = (Element) tmpNodeList.item(i1);

						if (elButton != null) {

							// test to see if we need this button
							blnTestSet = true;
							blnTestNotSet = true;
							strTest = elButton.getAttribute("if-set");
							if (!strTest.equals("")) {
								blnTestSet = MatchesIfSetCondition(strTest);
							} else {
								strTest = elButton.getAttribute("if-not-set");
								if (!strTest.equals("")) {
									blnTestNotSet = MatchesIfNotSetCondition(strTest);
								}
							}

							if (blnTestSet && blnTestNotSet) {

								strBtnTarget = elButton.getAttribute("target");

								StringBuffer buffer = new StringBuffer();
								NodeList childList = elButton.getChildNodes();
								for (int i11 = 0; i11 < childList.getLength(); i11++) {
									Node child = childList.item(i11);
									if (child.getNodeType() == Node.TEXT_NODE) {
										buffer.append(child.getNodeValue());
									}
								}
								strBtnText = buffer.toString();
								Button btnDynamic = new Button(this);
								btnDynamic.setText(strBtnText);
								btnDynamic.setTextSize(MintFontSize);
								int intBtnWidth = intBtnHeight
										+ (int) (intBtnHeight / 7)
										* strBtnText.length();
								if (intBtnWidth > intDpLeft) {
									intDpLeft = intRightWidth - intBtnWidth;
									intRows++;
									btnLayoutRow = new LinearLayout(this);
									btnLayout.addView(btnLayoutRow);
									layout = btnLayout.getLayoutParams();
									layout.height = intBtnHeight;
									btnLayout.setLayoutParams(layout);
								} else {
									intDpLeft = intDpLeft - intBtnWidth;
								}

								// record any button set / unset
								String strButtonSet;
								strSet = elButton.getAttribute("set");
								if (!strSet.equals("")) {
									btnDynamic.setTag(R.string.TagSetFlags,
											strSet);
								} else {
									btnDynamic.setTag(R.string.TagSetFlags, "");
								}
								strSet = elButton.getAttribute("unset");
								if (!strSet.equals("")) {
									btnDynamic.setTag(R.string.TagUnSetFlags,
											strSet);
								} else {
									btnDynamic.setTag(R.string.TagUnSetFlags,
											"");
								}

								btnDynamic.setTag(R.string.TagPage,
										strBtnTarget);
								btnDynamic
										.setOnClickListener(getOnClickDoSomething(btnDynamic));
								btnLayoutRow.addView(btnDynamic);
								layout = btnDynamic.getLayoutParams();
								layout.width = intBtnWidth;
								layout.height = intBtnHeight;
								btnDynamic.setLayoutParams(layout);
							}
						}
					}
					if (blnDebug) {
						// add a button to trigger the delay target
						if (elDelay != null) {
							Button btnDynamic = new Button(this);
							btnDynamic.setText("Delay");
							btnDynamic.setTextSize(MintFontSize);
							int intBtnWidth = intBtnHeight
									+ (int) (intBtnHeight / 7) * 5;
							if (intBtnWidth > intDpLeft) {
								intDpLeft = intRightWidth - intBtnWidth;
								intRows++;
								btnLayoutRow = new LinearLayout(this);
								btnLayout.addView(btnLayoutRow);
								layout = btnLayout.getLayoutParams();
								layout.height = intBtnHeight;
								btnLayout.setLayoutParams(layout);
							} else {
								intDpLeft = intDpLeft - intBtnWidth;
							}
							btnDynamic.setTag(strDelTarget);
							btnDynamic
									.setOnClickListener(getOnClickDoSomething(btnDynamic));
							btnLayoutRow.addView(btnDynamic);
							layout = btnDynamic.getLayoutParams();
							layout.width = intBtnWidth;
							layout.height = intBtnHeight;
							btnDynamic.setLayoutParams(layout);
						}
						TextView objDebugText = (TextView) findViewById(R.id.textViewDebug);
						objDebugText.setText(" " + strPageName);
					}
					layout = btnLayout.getLayoutParams();
					layout.height = intBtnHeight * intRows;
					btnLayout.setLayoutParams(layout);
					layout = objWebView1.getLayoutParams();
					int intWebViewHeight = intRightHeight
							- (intBtnHeight * intRows);
					layout.height = intWebViewHeight;
					objWebView1.setLayoutParams(layout);

					// Audio / Metronome
					tmpNodeList = elPage.getElementsByTagName("Metronome");
					elMetronome = (Element) tmpNodeList.item(0);
					if (elMetronome != null) {
						// Metronome
						String strbpm = elMetronome.getAttribute("bpm");
						int intbpm = Integer.parseInt(strbpm);
						intbpm = 60000 / intbpm;
						try {
							tmrTetronome = new Timer();
							tmrTetronome.schedule(new MetronomeTask(), intbpm,
									intbpm);
						} catch (IllegalArgumentException e) {
							Log.e("displayPage", "IllegalArgumentException "
									+ e.getLocalizedMessage());
						} catch (IllegalStateException e) {
							Log.e("displayPage",
									"IllegalStateException "
											+ e.getLocalizedMessage());
						} catch (Exception e) {
							Log.e("displayPage",
									"Exception " + e.getLocalizedMessage());
						}
					} else {
						// Audio
						// Audio is stored in an array of sounds populated when
						// we load the tease
						tmpNodeList = elPage.getElementsByTagName("Audio");
						elAudio = (Element) tmpNodeList.item(0);
						if (elAudio != null) {
							String strAudio = elAudio.getAttribute("id");
							for (i = 0; i < intSoundCount; i++) {
								if (strSounds[i].equals(strAudio)) {
									intSoundStream = soundPool.play(
											intSounds[i], 1.0f, 1.0f, 0, 0,
											1.0f);
								}
							}
						}
					}

					break;
				}
			}
		} catch (Exception e) {
			Log.e("displayPage", "Exception " + e.getLocalizedMessage());
		}
	}

	public void loadXML(String xmlFileName) {
		String strPreXMLPath;
		NodeList objNodeList;
		Element objMediaElement;
		Element objAutoset;
		NodeList objAudio;
		Element objElAudio;
		String strAudio;
		boolean blnLoaded;
		try {
			strPreXMLPath = strPresentationPath + xmlFileName;
			File preXMLFile = new File(strPreXMLPath);
			objDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
			objDocumentBuilder = objDocumentBuilderFactory.newDocumentBuilder();
			objDocPresXML = objDocumentBuilder.parse(preXMLFile);
			objDocPresXML.getDocumentElement().normalize();

			// AutoSetPageWhenSeen
			objNodeList = objDocPresXML
					.getElementsByTagName("AutoSetPageWhenSeen");
			objAutoset = (Element) objNodeList.item(0);
			blnAutoSetPage = Boolean.parseBoolean(objAutoset.getFirstChild()
					.getNodeValue());

			// Media directory
			objNodeList = objDocPresXML.getElementsByTagName("MediaDirectory");
			objMediaElement = (Element) objNodeList.item(0);
			strMediaDirectory = objMediaElement.getFirstChild().getNodeValue();

			// Node holding all pages
			objNodeList = objDocPresXML.getElementsByTagName("Pages");
			objPagesElement = (Element) objNodeList.item(0);

			// load all audio into a precompiled sound array
			objAudio = objPagesElement.getElementsByTagName("Audio");
			intSoundCount = 0;
			strSounds = new String[objAudio.getLength()];
			intSounds = new int[objAudio.getLength()];
			soundPool = null;
			soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
			sound = soundPool.load(this, R.raw.tick, 1);
			for (int i1 = objAudio.getLength() - 1; i1 >= 0; i1--) {
				objElAudio = (Element) objAudio.item(i1);
				strAudio = objElAudio.getAttribute("id");
				blnLoaded = false;
				for (int i2 = 0; i2 < intSoundCount; i2++) {
					if (strSounds[i2].equals(strAudio)) {
						blnLoaded = true;
						break;
					}
				}
				if (!blnLoaded) {
					strSounds[intSoundCount] = strAudio;
					intSounds[intSoundCount] = soundPool.load(
							strPresentationPath + strMediaDirectory + "/"
									+ strAudio, 1);
					intSoundCount++;
				}
			}
		} catch (ParserConfigurationException e) {
			Log.e("loadXML",
					"ParserConfigurationException " + e.getLocalizedMessage());
		} catch (SAXException e) {
			Log.e("loadXML", "SAXException " + e.getLocalizedMessage());
		} catch (IOException e) {
			Log.e("loadXML", "IOException " + e.getLocalizedMessage());
		} catch (Exception e) {
			Log.e("loadXML", "Exception " + e.getLocalizedMessage());
		}
	}

	private String getInnerXml(Node objXMLNode, boolean blnTopNode) {
		String strXML;
		Node objTmpElement;
		NodeList tmpNodeList;
		strXML = "";
		try {
			if (objXMLNode.getNodeType() == Node.ELEMENT_NODE) {
				if (!blnTopNode) {
					strXML = "<" + objXMLNode.getNodeName() + ">";
				}
				tmpNodeList = objXMLNode.getChildNodes();
				for (int i = 0; i < tmpNodeList.getLength(); i++) {
					objTmpElement = tmpNodeList.item(i);
					strXML = strXML + getInnerXml(objTmpElement, false);
				}
				if (!blnTopNode) {
					strXML = strXML + "</" + objXMLNode.getNodeName() + ">";
				}
			}
			if (objXMLNode.getNodeType() == Node.TEXT_NODE) {
				strXML = strXML + objXMLNode.getNodeValue();
			}
		} catch (Exception e) {
			Log.e("getInnerXml", "Exception " + e.getLocalizedMessage());
		}
		return strXML;
	}

	// delay timer
	class PageTimer extends CountDownTimer {
		public PageTimer(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
			Long tmpLong = (Long) millisInFuture / 1000;
			objCountText.setText(tmpLong.toString());
		}

		// display the target page
		@Override
		public void onFinish() {
			objCountText.setText("");
			// do set / unset
			if (!strDelaySet.equals("")) {
				SetFlags(strDelaySet);
			}
			if (!strDelayUnSet.equals("")) {
				UnsetFlags(strDelayUnSet);
			}
			displayPage(strDelTarget);
		}

		// update the clock
		// normal display minutes and seconds
		// secret display ??:??
		// hidden don't display
		@Override
		public void onTick(long millisUntilFinished) {
			try {
				if (strDelStyle.equals("normal")) {
					Long tmpLong = (Long) millisUntilFinished / 1000;
					Long tmpMinutes = (Long) tmpLong / 60;
					Long tmpSeconds = (Long) tmpLong - (tmpMinutes * 60);
					String strTime = tmpSeconds.toString();
					if (strTime.length() < 2) {
						strTime = "0" + strTime;
					}
					strTime = tmpMinutes.toString() + ":" + strTime;
					objCountText.setText(strTime);

				} else if (strDelStyle.equals("secret")) {
					objCountText.setText(R.string.timerSecret);
				} else {
					objCountText.setText("");
				}
			} catch (Exception e) {
				Log.e("PageTimer.onTick",
						"Exception " + e.getLocalizedMessage());
			}
		}
	}

	// to force landscape ignore orienation change or keyboard change
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// ignore orientation/keyboard change
		super.onConfigurationChanged(newConfig);
	}

	private int intSoundStream;

	// called from the metronome timer plays the tick
	private class MetronomeTask extends TimerTask {
		@Override
		public void run() {
			try {
				intSoundStream = soundPool.play(sound, 1.0f, 1.0f, 0, 0, 1.0f);
			} catch (Exception e) {
				Log.e("MetronomeTask.run",
						"Exception " + e.getLocalizedMessage());
			}
		}
	}

	// Get xml file
	private String[] mFileList;
	private String mChosenFile;
	private static final String FTYPE = ".xml";
	private static final int DIALOG_LOAD_FILE = 1000;
	private static final int DIALOG_PASSWORD_ENTER = 1001;

	private void loadFileList() {
		try {
			if (objPresFolder.exists()) {
				FilenameFilter filter = new FilenameFilter() {
					public boolean accept(File dir, String filename) {
						@SuppressWarnings("unused")
						File sel = new File(dir, filename);
						return filename.contains(FTYPE);
					}
				};
				mFileList = objPresFolder.list(filter);
			} else {
				mFileList = new String[0];
			}
		} catch (Exception e) {
			Log.e("loadFileList", "Exception " + e.getLocalizedMessage());
		}
	}

	private Dialog dialog = null;

	// private boolean blnReturn;;

	// File Load and Password dialogs
	protected Dialog onCreateDialog(int id) {
		try {
			AlertDialog.Builder builder = new Builder(this);
			switch (id) {
			case DIALOG_LOAD_FILE:
				builder.setTitle("Choose your file");
				if (mFileList == null) {
					dialog = builder.create();
					return dialog;
				}
				builder.setItems(mFileList,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								mChosenFile = mFileList[which];
								loadXML(mChosenFile);
								displayPage("start");
							}
						});
				dialog = builder.show();
				break;
			case DIALOG_PASSWORD_ENTER:
				dialog = new Dialog(this);
				dialog.setCancelable(false);
				dialog.setContentView(R.layout.passwordentry);
				dialog.setTitle("Enter Password");
				Button objButtonPwd = (Button) dialog
						.findViewById(R.id.btnPasswordOk);
				objButtonPwd.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						EditText edit = (EditText) dialog
								.findViewById(R.id.editTextPassword);
						String text = edit.getText().toString();
						dialog.dismiss();
						strPasswordEntered = text;
						if (!strPasswordEntered.equals(strPrefPassword)) {
							finish();
						}
					}
				});
				break;
			}
		} catch (Exception e) {
			Log.e("onCreateDialog", "Exception " + e.getLocalizedMessage());
		}
		return dialog;
	}

	// Wildecard filter
	private class WildCardFileFilter implements java.io.FileFilter {
		public boolean accept(File f) {
			try {
				String[] cards = strFilePatern.split("\\*");
				String text = f.getName();

				// Iterate over the cards.
				for (String card : cards) {
					int idx = text.indexOf(card);

					// Card not detected in the text.
					if (idx == -1) {
						return false;
					}

					// Move ahead, towards the right of the text.
					text = text.substring(idx + card.length());
				}

				return true;
			} catch (Exception e) {
				Log.e("WildCardFileFilter.accept",
						"Exception " + e.getLocalizedMessage());
				return false;
			}
		}
	}

	// calculate resize factor to fit images to the available screen size
	public static int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {
		int inSampleSize = 1;
		try {
			// Raw height and width of image
			final int height = options.outHeight;
			final int width = options.outWidth;

			if (height > reqHeight || width > reqWidth) {
				if (width > height) {
					inSampleSize = Math.round((float) height
							/ (float) reqHeight);
				} else {
					inSampleSize = Math.round((float) width / (float) reqWidth);
				}
			}
		} catch (Exception e) {
			Log.e("calculateInSampleSize",
					"Exception " + e.getLocalizedMessage());
		}
		return inSampleSize;
	}

	// return the image resized to the correct size to display
	public static Bitmap decodeSampledBitmapFromFile(String strFile,
			int reqWidth, int reqHeight) {
		try {

			// First decode with inJustDecodeBounds=true to check dimensions
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(strFile, options);

			// Calculate inSampleSize
			options.inSampleSize = calculateInSampleSize(options, reqWidth,
					reqHeight);

			// Decode bitmap with inSampleSize set
			options.inJustDecodeBounds = false;
			return BitmapFactory.decodeFile(strFile, options);
		} catch (Exception e) {
			Log.e("calculateInSampleSize",
					"Exception " + e.getLocalizedMessage());
			return null;
		}
	}

	// functions to handle set flags go here
	private void SetFlags(String flagNames) {
		String[] flags;
		flags = flagNames.split(",", -1);
		for (int i = 0; i < flags.length; i++) {
			if (!Flags.contains(flags[i])) {
				Flags.add(flags[i]);
			}
		}
	}

	private void UnsetFlags(String flagNames) {
		String[] flags;
		flags = flagNames.split(",", -1);
		for (int i = 0; i < flags.length; i++) {
			if (Flags.contains(flags[i]) && !flags[i].equals("")) {
				Flags.remove(flags[i]);
			}
		}
	}

	private boolean MatchesIfSetCondition(String condition) {
		boolean blnReturn = false;
		boolean blnAnd = false;
		boolean blnOr = false;
		String[] conditions;

		if (condition.indexOf("|") > -1) {
			blnOr = true;
			condition = condition.replace("|", ",");
			conditions = condition.split(",", -1);
			for (int i = 0; i < conditions.length; i++) {
				if (Flags.contains(conditions[i])) {
					blnReturn = true;
					break;
				}
			}
		}

		if (condition.indexOf("+") > -1) {
			blnAnd = true;
			blnReturn = true;
			condition = condition.replace("+", ",");
			conditions = condition.split(",", -1);
			for (int i = 0; i < conditions.length; i++) {
				if (!Flags.contains(conditions[i])) {
					blnReturn = false;
					break;
				}
			}
		}

		if (!blnAnd && !blnOr) {
			blnReturn = Flags.contains(condition);
		}

		return blnReturn;
	}

	private boolean MatchesIfNotSetCondition(String condition) {
		boolean blnReturn = false;
		boolean blnAnd = false;
		boolean blnOr = false;
		String[] conditions;

		if (condition.indexOf("+") > -1) {
			blnAnd = true;
			blnReturn = true;
			condition = condition.replace("+", ",");
			conditions = condition.split(",", -1);
			for (int i = 0; i < conditions.length; i++) {
				if (Flags.contains(conditions[i])) {
					blnReturn = false;
					break;
				}
			}
		}

		if (condition.indexOf("|") > -1) {
			blnOr = true;
			condition = condition.replace("|", ",");
			conditions = condition.split(",", -1);
			for (int i = 0; i < conditions.length; i++) {
				if (!Flags.contains(conditions[i])) {
					blnReturn = true;
					break;
				}
			}
		}

		if (!blnAnd && !blnOr) {
			blnReturn = !Flags.contains(condition);
		}

		return blnReturn;
	}

	private boolean AllowedToShowPage(String pageId) {
		NodeList pageNodeList = objPagesElement.getElementsByTagName("Page");
		Element elPage;
		String strTest;
		boolean blnCanShow = false;
		boolean blnSet = true;
		boolean blnNotSet = true;
		// loop through till we find the page
		for (int i = 0; i < pageNodeList.getLength(); i++) {
			elPage = (Element) pageNodeList.item(i);
			String strId = elPage.getAttribute("id");
			if (strId.equals(pageId)) {
				// found the page so check it
				strTest = elPage.getAttribute("if-set");
				if (!strTest.equals("")) {
					blnSet = MatchesIfSetCondition(strTest);
				}
				strTest = elPage.getAttribute("if-not-set");
				if (!strTest.equals("")) {
					blnNotSet = MatchesIfNotSetCondition(strTest);
				}
				if (blnSet && blnNotSet) {
					blnCanShow = MatchesIfNotSetCondition(pageId);
				} else {
					blnCanShow = false;
				}
				break;
			}
		}
		return blnCanShow;
	}

}
