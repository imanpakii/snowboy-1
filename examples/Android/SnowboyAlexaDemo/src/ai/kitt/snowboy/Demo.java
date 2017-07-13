package ai.kitt.snowboy;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.gson.JsonElement;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.Map;

import ai.api.AIListener;
import ai.api.RequestExtras;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIResponse;
import ai.api.model.Result;
import ai.kitt.snowboy.audio.AudioDataSaver;
import ai.kitt.snowboy.audio.PlaybackThread;
import ai.kitt.snowboy.audio.RecordingThread;
import ai.kitt.snowboy.demo.R;


//public class Demo extends Activity implements RecognitionListener {
public class Demo extends Activity implements AIListener{
    //===================================================================================================   Variables   =========================================================================================================================
    private Button record_button;
    private Button play_button;
    private TextView log;
    private ScrollView logView;
    static String strLog = null;

    private int preVolume = -1;
    private static long activeTimes = 0;

    private RecordingThread recordingThread;
    private PlaybackThread playbackThread;

    private boolean mStopSerial;
    private ToggleButton mBtn_GPIO;
    private boolean mStopGPIO;
    private EditText mET_Write;
    private Button mBtn_WriteSerial;
    private ToggleButton mBtn_ReadSerial;
    private TextView mET_Read;
    private String mLine;

    String weaghtStr=null;
    //=================================================================================================   Class define  ===========================================================================================================
    SerialPort serialPort=new SerialPort();
    private static TextToSpeech textToSpeech;
    private AIService aiService;
    RequestExtras requestExtras;
//=======================================================================================================   Functions   ===========================================================================================================
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++   Handler for calling port update   ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    Runnable mRunnableSerial = new Runnable() {

        @Override
        public void run() {
            // TODO Auto-generated method stub

            try {
                BufferedReader br = new BufferedReader(new FileReader("/dev/ttySAC0"));
                while (!mStopSerial) {
                    while((mLine = br.readLine()) != null) {
                        //Log.e(TAG, mLine);
                        mHandler.sendEmptyMessage(0);
                    }
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
    private void readSerial()
    {
        float weaightDraft = 0;
        int wordCnt = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader("/dev/ttySAC0"));
            if((mLine = br.readLine()) != null) updateLog(mLine);

            //arr = new String(mLine, 0, mLine.);
            String[] arr2 = mLine.split(",");
            for (String ss : arr2) {
                if (arr2[wordCnt].contains("kg")) {
                    weaightDraft = Float.valueOf(arr2[wordCnt - 1]);
                    weaightDraft = Math.abs(weaightDraft);
                    weaghtStr = String.valueOf(weaightDraft);
                }
                wordCnt++;
            }

            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            //mET_Read.setText(mLine);
            updateLog(mLine);
        }
    };
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++   onCreate Methode   ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//---------------------------------------------------------------------------------------------------   Serial init   ---------------------------------------------------------------------------------------------------------
        // mET_Write = (EditText) findViewById(R.id.et_write);
        mET_Read = (TextView) findViewById(R.id.et_read);
        serialPort.superUserCall();
        serialPort.setBaudRate();
        mStopSerial = false;


        // serialPort.writeSerial(mET_Write.getText().toString());
        // mStopSerial = false;

//---------------------------------------------------------------------------------------------------   AI.KITT init   ---------------------------------------------------------------------------------------------------------

        setContentView(R.layout.main);
        setUI();

        setProperVolume();

        AppResCopy.copyResFromAssetsToSD(this);

        activeTimes = 0;
        recordingThread = new RecordingThread(handle, new AudioDataSaver());
        playbackThread = new PlaybackThread();
//---------------------------------------------------------------------------------------------------   AI API init   ---------------------------------------------------------------------------------------------------------
        final AIConfiguration config = new AIConfiguration("698ee05a08b14a4d8975fb4fc55b6f5c",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);
        aiService = AIService.getService(this, config);
        aiService.setListener(this);
        //  requestExtras = new RequestExtras(contexts, null);

//---------------------------------------------------------------------------------------------------   TTS init   ---------------------------------------------------------------------------------------------------------
        textToSpeech=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.US);
                }
            }
        });

    }
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++   onCreate Methode end  ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    void showToast(CharSequence msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void setUI() {
        record_button = (Button) findViewById(R.id.btn_test1);
        record_button.setOnClickListener(record_button_handle);
        record_button.setEnabled(true);

        play_button = (Button) findViewById(R.id.btn_test2);
        play_button.setOnClickListener(play_button_handle);
        play_button.setEnabled(true);

        log = (TextView)findViewById(R.id.log);
        logView = (ScrollView)findViewById(R.id.logView);
    }

    private void setMaxVolume() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        preVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        updateLog(" ----> preVolume = "+preVolume, "green");
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        updateLog(" ----> maxVolume = "+maxVolume, "green");
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        updateLog(" ----> currentVolume = "+currentVolume, "green");
    }

    private void setProperVolume() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        preVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        updateLog(" ----> preVolume = "+preVolume, "green");
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        updateLog(" ----> maxVolume = "+maxVolume, "green");
        int properVolume = (int) ((float) maxVolume * 1);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, properVolume, 0);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        updateLog(" ----> currentVolume = "+currentVolume, "green");
    }

    private void restoreVolume() {
        if(preVolume>=0) {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, preVolume, 0);
            updateLog(" ----> set preVolume = "+preVolume, "green");
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            updateLog(" ----> currentVolume = "+currentVolume, "green");
        }
    }

    private void startRecording() {
        recordingThread.startRecording();
        updateLog(" ----> recording started ...", "green");
        record_button.setText(R.string.btn1_stop);

    }

    private void stopRecording() {
        recordingThread.stopRecording();
        updateLog(" ----> recording stopped ", "green");
        record_button.setText(R.string.btn1_start);
        // mHandler.removeCallbacks(mRunnableSerial);
        // mHandler.removeMessages(0);
    }

    private void startPlayback() {
        updateLog(" ----> playback started ...", "green");
        play_button.setText(R.string.btn2_stop);
        // (new PcmPlayer()).playPCM();
        playbackThread.startPlayback();
    }

    private void stopPlayback() {
        updateLog(" ----> playback stopped ", "green");
        play_button.setText(R.string.btn2_start);
        playbackThread.stopPlayback();
    }

    private void sleep() {
        try { Thread.sleep(500);
        } catch (Exception e) {}
    }

    private OnClickListener record_button_handle = new OnClickListener() {
        // @Override
        public void onClick(View arg0) {
            if(record_button.getText().equals(getResources().getString(R.string.btn1_start))) {
                stopPlayback();
                sleep();
                startRecording();
            } else {
                stopRecording();
                sleep();
            }
        }
    };

    private OnClickListener play_button_handle = new OnClickListener() {
        // @Override
        public void onClick(View arg0) {
            if (play_button.getText().equals(getResources().getString(R.string.btn2_start))) {
                stopRecording();
                sleep();
                startPlayback();
                //  startRecognizeSpeech();
            } else {
                stopPlayback();
            }
        }
    };

    public Handler handle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            MsgEnum message = MsgEnum.getMsgEnum(msg.what);
            switch(message) {
                case MSG_ACTIVE:
                    activeTimes++;
                    recordingThread.stopRecording();
                    updateLog(" ----> Detected " + activeTimes + " times", "green");
                    // Toast.makeText(Demo.this, "Active "+activeTimes, Toast.LENGTH_SHORT).show();
                    showToast("Active "+activeTimes);
                    aiService.startListening(requestExtras);
                    //   startRecognizeSpeech();
                    break;
                case MSG_INFO:
                    updateLog(" ----> "+message);
                    break;
                case MSG_VAD_SPEECH:
                    updateLog(" ----> normal voice", "blue");
                    break;
                case MSG_VAD_NOSPEECH:
                    updateLog(" ----> no speech", "blue");
                    break;
                case MSG_ERROR:
                    updateLog(" ----> " + msg.toString(), "red");
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };

    public void updateLog(final String text) {

        log.post(new Runnable() {
            @Override
            public void run() {
                if (currLogLineNum >= MAX_LOG_LINE_NUM) {
                    int st = strLog.indexOf("<br>");
                    strLog = strLog.substring(st+4);
                } else {
                    currLogLineNum++;
                }
                String str = "<font color='white'>"+text+"</font>"+"<br>";
                strLog = (strLog == null || strLog.length() == 0) ? str : strLog + str;
                log.setText(Html.fromHtml(strLog));
            }
        });
        logView.post(new Runnable() {
            @Override
            public void run() {
                logView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    static int MAX_LOG_LINE_NUM = 200;
    static int currLogLineNum = 0;

    public void updateLog(final String text, final String color) {
        log.post(new Runnable() {
            @Override
            public void run() {
                if(currLogLineNum>=MAX_LOG_LINE_NUM) {
                    int st = strLog.indexOf("<br>");
                    strLog = strLog.substring(st+4);
                } else {
                    currLogLineNum++;
                }
                String str = "<font color='"+color+"'>"+text+"</font>"+"<br>";
                strLog = (strLog == null || strLog.length() == 0) ? str : strLog + str;
                log.setText(Html.fromHtml(strLog));
            }
        });
        logView.post(new Runnable() {
            @Override
            public void run() {
                logView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    private void emptyLog() {
        strLog = null;
        log.setText("");
    }
    //=======================================================================================AI API Class=====================================================================================
    public void onResult(final AIResponse response) {
        Result result = response.getResult();

        // Get parameters
        String parameterString = "";
        if (result.getParameters() != null && !result.getParameters().isEmpty()) {
            for (final Map.Entry<String, JsonElement> entry : result.getParameters().entrySet()) {
                parameterString += "(" + entry.getKey() + ", " + entry.getValue() + ") ";
            }
        }

        // Show results in TextView.
        String Speech=result.getFulfillment().getSpeech();
   /* updateLog("Query:" + result.getResolvedQuery() +
            "\nAction: " + result.getAction() +
            "\nParameters: " + parameterString+
            "\nResponse: " +Speech);*/
        updateLog(Speech,"RED");
        recordingThread.startRecording();
        textToSpeech.speak(Speech, TextToSpeech.QUEUE_FLUSH,null);

    }
    @Override
    public void onError(final AIError error) {
        updateLog(error.toString(),"yellow");
        recordingThread.startRecording();
        // resultTextView.setText(error.toString());
    }
    @Override
    public void onListeningStarted() {}

    @Override
    public void onListeningCanceled() {}

    @Override
    public void onListeningFinished() {}

    @Override
    public void onAudioLevel(final float level) {}
    /*
    private SpeechRecognizer recognizer;
    private Intent intent = null;
    private boolean TEST_ERROR = false; // Toggle to test errors
    private long then; // Reset each time startListening is called.
    boolean ReadTrigger=true;
    public void startRecognizeSpeech() {
       // new Thread(mRunnableSerial).start();
        if (TEST_ERROR || recognizer == null) {
            ReadTrigger=false;
            //Intent intent = RecognizerIntent.getVoiceDetailsIntent(getApplicationContext());
            intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "blar");
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString());
            recognizer=SpeechRecognizer.createSpeechRecognizer(Demo.this);
            recognizer.setRecognitionListener(Demo.this);
        }
        then = System.currentTimeMillis();
        recognizer.startListening(intent);
        readSerial();
        //	((TextView)findViewById(R.id.status)).setText("");
        //	((TextView)findViewById(R.id.sub_status)).setText("");
        //	findViewById(R.id.start_recognize).setEnabled(false);
    }
    @Override
     public void onDestroy() {
         restoreVolume();
        // recordingThread.stopRecording();
         super.onDestroy();
        recordingThread.startRecording();
        Log.i("SPEECH_TEST", "onDestroy");
     }
    @Override
    public void onReadyForSpeech(Bundle bundle) {
        Log.i("SPEECH_TEST", "onReadyforspeech");
    }
    @Override
    public void onBeginningOfSpeech() {
        Log.i("SPEECH_TEST", "onBeginningOfSpeech");
    }
    @Override
    public void onRmsChanged(float v) {
    }
    @Override
    public void onBufferReceived(byte[] bytes) {
        //new Thread(mRunnableSerial).stop();
      //  mHandler.removeCallbacks(mRunnableSerial);
        Log.i("SPEECH_TEST", "on buffer received");
        recordingThread.startRecording();
    }
    @Override
    public void onEndOfSpeech() {
        ReadTrigger=true;
        recordingThread.startRecording();
        Log.i("SPEECH_TEST", "on End of speech");
       // mHandler.removeCallbacks(mRunnableSerial);
        //new Thread(mRunnableSerial).interrupt();
    }
    @Override
    public void onError(int i) {
        updateLog("Error","red");
        Log.i("SPEECH_TEST", "onError:");
        recordingThread.startRecording();
    }
    @Override
    public void onResults(Bundle bundle) {
        FetchWeatherTask weatherTask=new FetchWeatherTask();
        Log.i("SPEECH_TEST", "onResults:");
        ArrayList<String> results2 = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        ReadTrigger=true;
        recordingThread.startRecording();
        //new Thread(mRunnableSerial).interrupt();
       // mHandler.removeCallbacks(mRunnableSerial);
        if (!TEST_ERROR) {
            //Log.i("SPEECH_TEST", "onResults: destroying recognizer");
            if (recognizer != null) {
                recognizer.cancel();
                recognizer.destroy();
                recognizer = null;
                TextView t = (TextView)findViewById(R.id.log);
                t.setText("");
                for (String s : results2) {
                    t.append(s + "\n");
                    //inputFooditem=Emission.getText().toString();
                //updateLog(results2,"blue");
                }
                weatherTask.execute(weaghtStr,results2.get(0));
            }
        }
    }
    @Override
    public void onPartialResults(Bundle bundle) {
        final ArrayList<String> partialData = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        FetchWeatherTask weatherTask=new FetchWeatherTask();
        if (partialData != null) {
            for (String partial : partialData) {
                if (partial.replaceAll("\\s", "").isEmpty()) {
                    Log.i("SPEECH_TEST", "onPartialResults: I'm an empty String?");
                }else {
                    updateLog(partialData.toString(),"yellow");
                    Log.i("SPEECH_TEST", "onPartialResults:"+partialData);
                    weatherTask.execute(weaghtStr,partialData.toString());
                }
            }
        }
    }
    @Override
    public void onEvent(int i, Bundle bundle) {
    }
    */
    //=======================================================================================AsyncTask Class=====================================================================================
    public class FetchWeatherTask extends AsyncTask<String, Void, String> {
        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
        @Override
        protected String doInBackground(String... params) {
            HttpURLConnection httpURLConnection = null;
            BufferedReader reader = null;          // so that they can be closed in the finally block.
            String forecastJsonStr = null;         // Will contain the raw JSON response as a string.
            double calories = 0;
            String strCal="";
            String[] testStr = new String[10];
            try {
                URL url = new URL("https://trackapi.nutritionix.com/v2/natural/nutrients"); //Enter URL here
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setReadTimeout(10000 /*milliseconds*/);
                httpURLConnection.setConnectTimeout(15000 /* milliseconds */);
                httpURLConnection.setRequestMethod("POST"); // here you are telling that it is a POST request, which can be changed into "PUT", "GET", "DELETE" etc.
                httpURLConnection.setRequestProperty("Content-Type", "application/json"); // here you are setting the `Content-Type` for the data you are sending which is `application/json`
                httpURLConnection.setRequestProperty("x-app-id", "c0e5dcec");
                httpURLConnection.setRequestProperty("x-app-key", "8eac25e42921ec9b90970649710e25f3");
                httpURLConnection.connect();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("query", params[0] + " kg of "+params[1]);

                DataOutputStream wr = new DataOutputStream(httpURLConnection.getOutputStream());
                wr.writeBytes(jsonObject.toString());
                wr.flush();
                wr.close();
                String json_response = "";
                InputStreamReader in = new InputStreamReader(httpURLConnection.getInputStream());
                BufferedReader br = new BufferedReader(in);
                String text = "";
                while ((text = br.readLine()) != null) {
                    json_response += text;
                }
                JSONObject niutrition = new JSONObject(json_response);
                JSONArray foods = niutrition.getJSONArray("foods");
                JSONObject arryJson = foods.getJSONObject(0);
                calories = arryJson.getDouble("nf_calories");
                strCal = String.valueOf(calories);
                testStr[0]="Calories="+strCal+" Kcal";
                for(int i=1;i<10;i++){
                    testStr[i]= String.valueOf(i);
                }


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
            }
            return testStr[0];
        }
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if(s!=null)
                updateLog(s,"pink");
            //caloriesShower.setText(s);
        }
    }

}