package jp.kshoji.blemidi.sample;

import static jp.kshoji.blemidi.util.Constants.TAG;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import jp.kshoji.blemidi.central.BleMidiCentralProvider;
import jp.kshoji.blemidi.device.MidiInputDevice;
import jp.kshoji.blemidi.device.MidiOutputDevice;
import jp.kshoji.blemidi.listener.OnMidiDeviceAttachedListener;
import jp.kshoji.blemidi.listener.OnMidiDeviceDetachedListener;
import jp.kshoji.blemidi.listener.OnMidiInputEventListener;
import jp.kshoji.blemidi.listener.OnMidiScanStatusListener;
import jp.kshoji.blemidi.sample.util.SoundMaker;
import jp.kshoji.blemidi.sample.util.Tone;
import jp.kshoji.blemidi.util.BleUtils;



/**
 * Activity for BLE MIDI Central Application
 *
 * @author K.Shoji
 */
public class CentralActivity extends Activity {
    BleMidiCentralProvider bleMidiCentralProvider;

    MenuItem toggleScanMenu;
    ArrayList<ArrayList<Object> > completeCatchHistory = new ArrayList<>();
    ArrayList<String> catchHistory = new ArrayList<String>();
    ArrayList<String> previousSiteswaps = new ArrayList<String>();

    LinkedList<Double> consistency_score_history = new LinkedList<Double>();

    int catchCounter;
    String previousName;
    Hashtable<String,Long> lastCaughtTimes = new Hashtable<String,Long>();

    TextView counterTextView;
    TextView siteswapTextView;
    TextView consistencyTextView;
    ListView midiInputEventListView;

    boolean isScanning = false;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.central, menu);
        toggleScanMenu = menu.getItem(0);

        if (isScanning) {
            toggleScanMenu.setTitle(R.string.stop_scan);
        } else {
            toggleScanMenu.setTitle(R.string.start_scan);
        }

        return true;
    }

    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;

    private int scanDuration;
    private void startScanDeviceWithRequestingPermission(int duration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    scanDuration = duration;
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
                    return;
                }
            }
        }

        // already has permission
        bleMidiCentralProvider.startScanDevice(duration);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_toggle_scan:
                if (isScanning) {
                    bleMidiCentralProvider.stopScanDevice();
                } else {
                    startScanDeviceWithRequestingPermission(scanDuration);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (requestCode == PERMISSION_REQUEST_FINE_LOCATION) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    bleMidiCentralProvider.startScanDevice(0);
                }
            }
        }
    }

    public String getFullSiteswap(ArrayList reverseCatchHistory){
        String fullSiteswap = "";

        Log.d("TAG", "reverseCatchHistory " +reverseCatchHistory.subList(0, 10));
        for (int catchNum = 0; catchNum < 9; catchNum++){
            for (int nextCatch = catchNum+1; nextCatch < 10; nextCatch++){
                Log.d("TAG", "reverseCatchHistory.get(catchNum): " +reverseCatchHistory.get(catchNum));
                Log.d("TAG", "reverseCatchHistory.get(nextCatch): " +reverseCatchHistory.get(nextCatch));
                if (reverseCatchHistory.get(catchNum).equals(reverseCatchHistory.get(nextCatch))){
                    fullSiteswap = fullSiteswap + (nextCatch-catchNum);
                    Log.d("TAG", "fullSiteswap: " +fullSiteswap);
                    break;
                }

            }
        }
        return fullSiteswap;
    }

    static String getBaseSiteswap(String input){
        return input.replaceAll("^(.+?)\\1*$","$1");
    }

//    public String getBaseSiteswap(String fullSiteswap){
//        String baseSiteswap = "";
//        for (int period = 1; period > fullSiteswap.length()-1; period++){
//
//        }
//        return baseSiteswap;
//    }

    public long getFormatttedSiteswap(String baseSiteswap){
        long largestNumber = Long.parseLong(baseSiteswap);
        for (int i = 0; i<baseSiteswap.length();i++){
            String begString = baseSiteswap.substring(0, i);
            String endString = baseSiteswap.substring(i, baseSiteswap.length());
            long possibleLargest = Long.parseLong(endString + begString);
            if (possibleLargest > largestNumber){
                largestNumber = possibleLargest;
            }
        }
        return largestNumber;
    }

    public boolean checkIfAllEqual(List<String> list) {
        for (String s : list) {
            if (!s.equals(list.get(0)))
                return false;
        }
        return true;
    }

    public String getDetectedSiteswap(){
        String detectedSiteswap = "????";
        long formattedSiteswap;
        ArrayList<String> reverseCatchHistory = new ArrayList<String>();
        ArrayList<Object> catchHistoryBallNames = new ArrayList<>();
        for (ArrayList<Object> s : completeCatchHistory)
            catchHistoryBallNames.add(s.get(0));
        ArrayList<String> strList = (ArrayList<String>)(ArrayList<?>)(catchHistoryBallNames);
        reverseCatchHistory.addAll(strList);
        //Log.d("TAG", "catchHistory: " +catchHistory);
        Collections.reverse(reverseCatchHistory);
        //Log.d("TAG", "catchHistory: " +catchHistory);
        //Log.d("TAG", "reverseCatchHistory: " +reverseCatchHistory);
        if (reverseCatchHistory.size() > 15){
            String fullSiteswap = getFullSiteswap(reverseCatchHistory);
            Log.d("TAG", "fullSiteswap: " +fullSiteswap);
            String baseSiteswap = getBaseSiteswap(fullSiteswap);
            Log.d("TAG", "baseSiteswap: " +baseSiteswap);
            formattedSiteswap = getFormatttedSiteswap(baseSiteswap);
            Log.d("TAG", "formattedSiteswap: " +formattedSiteswap);
            previousSiteswaps.add(Long.toString(formattedSiteswap));
            if (previousSiteswaps.size() > 10) {
                Log.d("TAG", "previousSiteswaps: " +previousSiteswaps.subList(previousSiteswaps.size() - 10, previousSiteswaps.size()));

                if (checkIfAllEqual(previousSiteswaps.subList(previousSiteswaps.size() - 10, previousSiteswaps.size()))) {
                    detectedSiteswap = Long.toString(formattedSiteswap);
                    Log.d("TAG", "detectedSiteswap: " + detectedSiteswap);
                }
            }
        }
        return detectedSiteswap;
    }



    public String getConsistencyScore(){
        String consistencyScore = "????";



        return consistencyScore;
    }

    // User interface
    final Handler midiInputEventHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (midiInputEventAdapter != null) {
                //String msgString = (String)msg.obj;
                //msgString.split("Pball");
                //msgString = msgString.substring(msgString.indexOf("ball:") + 1);
                //String ballName = msgString.substring(0, msgString.indexOf("Pball:"));

                midiInputEventAdapter.add((String)msg.obj);
                int childCount = midiInputEventListView.getChildCount();
                if (midiInputEventListView.getChildAt(childCount-1) != null) {

                    for (int i = 0; i < Math.min(childCount-1, childCount); i++) {
                        //Log.d("TAG", "i: "+i);
                        int our_index = midiInputEventListView.getCount() - (i+1) ;
                        //Log.d("TAG", "our_index: "+our_index);
                        //Log.d("TAG", "midiInputEventListView.getCount(): "+midiInputEventListView.getCount());
                        //Log.d("TAG", "midiInputEventListView.getChildCount(): "+midiInputEventListView.getChildCount());
                        //Toast.makeText(getBaseContext(), our_index, Toast.LENGTH_LONG).show();
                        String msgString = (String) (midiInputEventListView.getItemAtPosition(our_index));
                        //Log.d("TAG", "msgString: "+msgString);
                        //Toast.makeText(getBaseContext(), msgString, Toast.LENGTH_LONG).show();
                        if (msgString.contains("Gry")) {
                            midiInputEventListView.getChildAt((childCount-1)-i).setBackgroundColor(
                                    Color.parseColor("#0000FF"));
                        } else if (msgString.contains("Trq")) {
                            midiInputEventListView.getChildAt((childCount-1)-i).setBackgroundColor(
                                    Color.parseColor("#00FF00"));
                        } else if (msgString.contains("Pnc")) {
                            midiInputEventListView.getChildAt((childCount-1)-i).setBackgroundColor(
                                    Color.parseColor("#FF0000"));
                        }
                    }
                }

            }
            counterTextView.setText(String.valueOf(catchCounter));
            //siteswapTextView.setText(getDetectedSiteswap());
            consistencyTextView.setText(getConsistencyScore());
            // message handled successfully
            return true;
        }
    });

    final Handler midiOutputEventHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (midiOutputEventAdapter != null) {
                midiOutputEventAdapter.add((String)msg.obj);
            }

            // message handled successfully
            return true;
        }
    });

    final Handler midiOutputConnectionChangedHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.obj instanceof MidiOutputDevice) {
                MidiOutputDevice midiOutputDevice = (MidiOutputDevice) msg.obj;
                if (msg.arg1 == 0) {
                    connectedOutputDevicesAdapter.remove(midiOutputDevice);
                    connectedOutputDevicesAdapter.add(midiOutputDevice);
                    connectedOutputDevicesAdapter.notifyDataSetChanged();
                } else {
                    connectedOutputDevicesAdapter.remove(midiOutputDevice);
                    connectedOutputDevicesAdapter.notifyDataSetChanged();
                }
            }

            // message handled successfully
            return true;
        }
    });

    ArrayAdapter<String> midiInputEventAdapter;
    ArrayAdapter<String> midiOutputEventAdapter;
    private ToggleButton thruToggleButton;
    Spinner deviceSpinner;

    ArrayAdapter<MidiOutputDevice> connectedOutputDevicesAdapter;

    // Play sounds
    AudioTrack audioTrack;
    Timer timer;
    TimerTask timerTask;
    SoundMaker soundMaker;
    final Set<Tone> tones = new HashSet<>();
    int currentProgram = 0;

    /**
     * Choose device from spinner
     *
     * @return chosen {@link jp.kshoji.blemidi.device.MidiOutputDevice}
     */
    MidiOutputDevice getBleMidiOutputDeviceFromSpinner() {
        if (deviceSpinner != null && deviceSpinner.getSelectedItemPosition() >= 0 && connectedOutputDevicesAdapter != null && !connectedOutputDevicesAdapter.isEmpty()) {
            MidiOutputDevice device = connectedOutputDevicesAdapter.getItem(deviceSpinner.getSelectedItemPosition());
            if (device != null) {
                Set<MidiOutputDevice> midiOutputDevices = bleMidiCentralProvider.getMidiOutputDevices();

                if (midiOutputDevices.size() > 0) {
                    // returns the first one.
                    return (MidiOutputDevice) midiOutputDevices.toArray()[0];
                }
            }
        }
        return null;
    }



    OnMidiInputEventListener onMidiInputEventListener = new OnMidiInputEventListener() {
        @Override
        public void onMidiSystemExclusive(@NonNull MidiInputDevice sender, @NonNull byte[] systemExclusive) {
            midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "SystemExclusive from: " + sender.getDeviceName() + ", data:" + Arrays.toString(systemExclusive)));

            if (thruToggleButton != null && thruToggleButton.isChecked() && getBleMidiOutputDeviceFromSpinner() != null) {
                getBleMidiOutputDeviceFromSpinner().sendMidiSystemExclusive(systemExclusive);
                midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "SystemExclusive from: " + sender.getDeviceName() + ", data:" + Arrays.toString(systemExclusive)));
            }
        }

        @Override
        public void onMidiNoteOff(@NonNull MidiInputDevice sender, int channel, int note, int velocity) {
            midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "NoteOff from: " + sender.getDeviceName() + " channel: " + channel + ", note: " + note + ", velocity: " + velocity));

            if (thruToggleButton != null && thruToggleButton.isChecked() && getBleMidiOutputDeviceFromSpinner() != null) {
                getBleMidiOutputDeviceFromSpinner().sendMidiNoteOff(channel, note, velocity);
                midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "NoteOff from: " + sender.getDeviceName() + " channel: " + channel + ", note: " + note + ", velocity: " + velocity));
            }

            synchronized (tones) {
                Iterator<Tone> it = tones.iterator();
                while (it.hasNext()) {
                    Tone tone = it.next();
                    if (tone.getNote() == note) {
                        it.remove();
                    }
                }
            }
        }
        public Boolean throwIsValid(String ballName){
            Boolean isValid = true;
            Log.d("TAG", "throwIsValid: "+lastCaughtTimes);
            Log.d("TAG", "ballName: "+ballName);
            Log.d("TAG", "previousName1: "+previousName);
            //if (ballName.equals(previousName)){
                //Log.d("TAG", "previousName2: "+previousName);
                Date date = new Date();
                if (lastCaughtTimes.containsKey(ballName)){
                    Log.d("TAG", "ContainsKey: ");
                    if (date.getTime() - lastCaughtTimes.get(ballName) < 250){
                        Log.d("TAG", "date.getTime(): "+date.getTime());
                        Log.d("TAG", "lastCaughtTimes.get(ballName): "+lastCaughtTimes.get(ballName));
                        isValid = false;
                    }
                }
                lastCaughtTimes.put(ballName, date.getTime());
            //}

            return isValid;
        }

        public long getTimeSinceLastCatch(String ballName, long date){
            long timeSinceLastCatch = 0;
            ArrayList<ArrayList<Object> > copied_list = new ArrayList<>();
            copied_list.addAll(completeCatchHistory);
            for(ArrayList<Object> row : copied_list) {
                if (row.get(0).equals(ballName)){
                    timeSinceLastCatch = date - (Long)row.get(1);
                    //Log.d(TAG, "timeSinceLastCatch: "+timeSinceLastCatch);
                }
            }
            return timeSinceLastCatch;
        }
        long calculate_list_average(ArrayList<Long> given_list){
            long average = 0;
            long total = 0;
            for(int i = 0; i<given_list.size(); i++)
                total = total+given_list.get(i);
            average = total / given_list.size();
            return average;
        }

        double calculate_linkedlist_average(LinkedList<Double> given_list){
            double average = 0;
            if (given_list.size()>0) {
                double total = 0;
                for (int i = 0; i < given_list.size(); i++)
                    total = total + given_list.get(i);
                average = total / given_list.size();
            }
            return average;
        }

        long getAverageTimeSinceLastCatch(String ballName){
            long average = 0;
            ArrayList<Long> all_times = new ArrayList<>();
            ArrayList<ArrayList<Object> > copied_list = new ArrayList<>();
            copied_list.addAll(completeCatchHistory);
            for(ArrayList<Object> row : copied_list) {
                if (row.get(0).equals(ballName)){
                    all_times.add((Long)row.get(2));
                }
            }
            average = calculate_list_average(all_times);
            return average;
        }






        @Override
        public void onMidiNoteOn(@NonNull MidiInputDevice sender, int channel, int note, int velocity) {



            //if the same ball is caught twice within a small amount of time, then ignore the
            //  second time it was caught
            String ballName = sender.getDeviceName();
            if (throwIsValid(ballName)) {
                catchCounter++; //TODO maybe remove this and just use catchHistory
                previousName = ballName; //TODO maybe remove this and just use catchHistory
                Date date = new Date();
                long timeSinceLastCatch = getTimeSinceLastCatch(ballName, date.getTime());
                catchHistory.add(ballName);
                ArrayList<Object> this_catch_data = new ArrayList<>();
                this_catch_data.add(ballName);
                this_catch_data.add(date.getTime());
                this_catch_data.add(timeSinceLastCatch);
                this_catch_data.add(velocity);
                completeCatchHistory.add(this_catch_data);
                //Log.d(TAG, "addToCatchHistory: "+ballName);
                //Log.d(TAG, "catchHistoryXXX: "+catchHistory);
                String shortTimeString = String.valueOf(date.getTime());
                shortTimeString = shortTimeString.substring(5, shortTimeString.length());

                String shortBallName = ballName.replace("ODD","");
                long average_time_since_last_catch = getAverageTimeSinceLastCatch(ballName);
                if (consistency_score_history.size() > 10){
                    consistency_score_history.removeFirst();
                }
                if (average_time_since_last_catch > 0 ) {
                    double score = ((double)timeSinceLastCatch/average_time_since_last_catch)*100;
                    consistency_score_history.add(score);
                }
                Log.d(TAG, "consistency_score_history: "+consistency_score_history);

                double consistency_score = calculate_linkedlist_average(consistency_score_history);
                midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, shortBallName + shortTimeString +" "+ timeSinceLastCatch + " " +average_time_since_last_catch+ " "+ consistency_score + ", n: " + note + ", v: " + velocity));
                if (thruToggleButton != null && thruToggleButton.isChecked() && getBleMidiOutputDeviceFromSpinner() != null) {
                    getBleMidiOutputDeviceFromSpinner().sendMidiNoteOn(channel, note, velocity);
                    midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "NoteOn from: " + sender.getDeviceName() + " channel: " + channel + ", note: " + note + ", velocity: " + velocity));
                }
            }



//            synchronized (tones) {
//                if (velocity == 0) {
//                    Iterator<Tone> it = tones.iterator();
//                    while (it.hasNext()) {
//                        Tone tone = it.next();
//                        if (tone.getNote() == note) {
//                            it.remove();
//                        }
//                    }
//                } else {
//                    //catchCounter++;
//                    //counterTextView.setText("test2");
//                    //tv.setText(String.valueOf(number));
//
//                    //tones.add(new Tone(note, velocity / 127.0, currentProgram));
//                }
//            }
        }

        @Override
        public void onMidiPolyphonicAftertouch(@NonNull MidiInputDevice sender, int channel, int note, int pressure) {
            midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "PolyphonicAftertouch  from: " + sender.getDeviceName() + " channel: " + channel + ", note: " + note + ", pressure: " + pressure));

            if (thruToggleButton != null && thruToggleButton.isChecked() && getBleMidiOutputDeviceFromSpinner() != null) {
                getBleMidiOutputDeviceFromSpinner().sendMidiPolyphonicAftertouch(channel, note, pressure);
                midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "PolyphonicAftertouch from: " + sender.getDeviceName() + " channel: " + channel + ", note: " + note + ", pressure: " + pressure));
            }
        }

        @Override
        public void onMidiControlChange(@NonNull MidiInputDevice sender, int channel, int function, int value) {
            midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "ControlChange from: " + sender.getDeviceName() + ", channel: " + channel + ", function: " + function + ", value: " + value));

            if (thruToggleButton != null && thruToggleButton.isChecked() && getBleMidiOutputDeviceFromSpinner() != null) {
                getBleMidiOutputDeviceFromSpinner().sendMidiControlChange(channel, function, value);
                midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "ControlChange from: " + sender.getDeviceName() + ", channel: " + channel + ", function: " + function + ", value: " + value));
            }
        }

        @Override
        public void onMidiProgramChange(@NonNull MidiInputDevice sender, int channel, int program) {
            midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "ProgramChange from: " + sender.getDeviceName() + ", channel: " + channel + ", program: " + program));

            if (thruToggleButton != null && thruToggleButton.isChecked() && getBleMidiOutputDeviceFromSpinner() != null) {
                getBleMidiOutputDeviceFromSpinner().sendMidiProgramChange(channel, program);
                midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "ProgramChange from: " + sender.getDeviceName() + ", channel: " + channel + ", program: " + program));
            }

            currentProgram = program % Tone.FORM_MAX;
            synchronized (tones) {
                for (Tone tone : tones) {
                    tone.setForm(currentProgram);
                }
            }
        }

        @Override
        public void onMidiChannelAftertouch(@NonNull MidiInputDevice sender, int channel, int pressure) {
            midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "ChannelAftertouch from: " + sender.getDeviceName() + ", channel: " + channel + ", pressure: " + pressure));

            if (thruToggleButton != null && thruToggleButton.isChecked() && getBleMidiOutputDeviceFromSpinner() != null) {
                getBleMidiOutputDeviceFromSpinner().sendMidiChannelAftertouch(channel, pressure);
                midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "ChannelAftertouch from: " + sender.getDeviceName() + ", channel: " + channel + ", pressure: " + pressure));
            }
        }

        @Override
        public void onMidiPitchWheel(@NonNull MidiInputDevice sender, int channel, int amount) {
            midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "PitchWheel from: " + sender.getDeviceName() + ", channel: " + channel + ", amount: " + amount));

            if (thruToggleButton != null && thruToggleButton.isChecked() && getBleMidiOutputDeviceFromSpinner() != null) {
                getBleMidiOutputDeviceFromSpinner().sendMidiPitchWheel(channel, amount);
                midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "PitchWheel from: " + sender.getDeviceName() + ", channel: " + channel + ", amount: " + amount));
            }
        }

        @Override
        public void onMidiTimeCodeQuarterFrame(@NonNull MidiInputDevice sender, int timing) {
            midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "TimeCodeQuarterFrame from: " + sender.getDeviceName() + ", timing: " + timing));

            if (thruToggleButton != null && thruToggleButton.isChecked() && getBleMidiOutputDeviceFromSpinner() != null) {
                getBleMidiOutputDeviceFromSpinner().sendMidiTimeCodeQuarterFrame(timing);
                midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "TimeCodeQuarterFrame from: " + sender.getDeviceName() + ", timing: " + timing));
            }
        }

        @Override
        public void onMidiSongSelect(@NonNull MidiInputDevice sender, int song) {
            midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "SongSelect from: " + sender.getDeviceName() + ", song: " + song));

            if (thruToggleButton != null && thruToggleButton.isChecked() && getBleMidiOutputDeviceFromSpinner() != null) {
                getBleMidiOutputDeviceFromSpinner().sendMidiSongSelect(song);
                midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "SongSelect from: " + sender.getDeviceName() + ", song: " + song));
            }
        }

        @Override
        public void onMidiSongPositionPointer(@NonNull MidiInputDevice sender, int position) {
            midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "SongPositionPointer from: " + sender.getDeviceName() + ", position: " + position));

            if (thruToggleButton != null && thruToggleButton.isChecked() && getBleMidiOutputDeviceFromSpinner() != null) {
                getBleMidiOutputDeviceFromSpinner().sendMidiSongPositionPointer(position);
                midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "SongPositionPointer from: " + sender.getDeviceName() + ", position: " + position));
            }
        }

        @Override
        public void onMidiTuneRequest(@NonNull MidiInputDevice sender) {
            midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "TuneRequest from: " + sender.getDeviceName()));

            if (thruToggleButton != null && thruToggleButton.isChecked() && getBleMidiOutputDeviceFromSpinner() != null) {
                getBleMidiOutputDeviceFromSpinner().sendMidiTuneRequest();
                midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "TuneRequest from: " + sender.getDeviceName()));
            }
        }

        @Override
        public void onMidiTimingClock(@NonNull MidiInputDevice sender) {
            midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "TimingClock from: " + sender.getDeviceName()));

            if (thruToggleButton != null && thruToggleButton.isChecked() && getBleMidiOutputDeviceFromSpinner() != null) {
                getBleMidiOutputDeviceFromSpinner().sendMidiTimingClock();
                midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "TimingClock from: " + sender.getDeviceName()));
            }
        }

        @Override
        public void onMidiStart(@NonNull MidiInputDevice sender) {
            midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "Start from: " + sender.getDeviceName()));

            if (thruToggleButton != null && thruToggleButton.isChecked() && getBleMidiOutputDeviceFromSpinner() != null) {
                getBleMidiOutputDeviceFromSpinner().sendMidiStart();
                midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "Start from: " + sender.getDeviceName()));
            }
        }

        @Override
        public void onMidiContinue(@NonNull MidiInputDevice sender) {
            midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "Continue from: " + sender.getDeviceName()));

            if (thruToggleButton != null && thruToggleButton.isChecked() && getBleMidiOutputDeviceFromSpinner() != null) {
                getBleMidiOutputDeviceFromSpinner().sendMidiContinue();
                midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "Continue from: " + sender.getDeviceName()));
            }
        }

        @Override
        public void onMidiStop(@NonNull MidiInputDevice sender) {
            midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "Stop from: " + sender.getDeviceName()));

            if (thruToggleButton != null && thruToggleButton.isChecked() && getBleMidiOutputDeviceFromSpinner() != null) {
                getBleMidiOutputDeviceFromSpinner().sendMidiStop();
                midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "Stop from: " + sender.getDeviceName()));
            }
        }

        @Override
        public void onMidiActiveSensing(@NonNull MidiInputDevice sender) {
            midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "ActiveSensing from: " + sender.getDeviceName()));

            if (thruToggleButton != null && thruToggleButton.isChecked() && getBleMidiOutputDeviceFromSpinner() != null) {
                getBleMidiOutputDeviceFromSpinner().sendMidiActiveSensing();
                midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "ActiveSensing from: " + sender.getDeviceName()));
            }
        }

        @Override
        public void onMidiReset(@NonNull MidiInputDevice sender) {
            midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "Reset from: " + sender.getDeviceName()));

            if (thruToggleButton != null && thruToggleButton.isChecked() && getBleMidiOutputDeviceFromSpinner() != null) {
                getBleMidiOutputDeviceFromSpinner().sendMidiReset();
                midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "Reset from: " + sender.getDeviceName()));
            }
        }

        @Override
        public void onRPNMessage(@NonNull MidiInputDevice sender, int channel, int function, int value) {
            midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "RPN message from: " + sender.getDeviceName() + ", channel: " + channel + ", function: " + function + ", value: " + value));
        }

        @Override
        public void onNRPNMessage(@NonNull MidiInputDevice sender, int channel, int function, int value) {
            midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "NRPN message from: " + sender.getDeviceName() + ", channel: " + channel + ", function: " + function + ", value: " + value));
        }
    };

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        counterTextView = (TextView) findViewById(R.id.textView2);
        siteswapTextView = (TextView) findViewById(R.id.siteswaptv);
        consistencyTextView = (TextView) findViewById(R.id.consistencytv);
        //counterTextView.setText("0");

        midiInputEventListView = (ListView) findViewById(R.id.catchHistory);
        midiInputEventAdapter = new ArrayAdapter<>(this, R.layout.midi_event, R.id.midiEventDescriptionTextView);
        midiInputEventAdapter = new ArrayAdapter<>(this, R.layout.midi_event, R.id.midiEventDescriptionTextView);
        midiInputEventListView.setAdapter(midiInputEventAdapter);

        ListView midiOutputEventListView = (ListView) findViewById(R.id.runHistory);
        midiOutputEventAdapter = new ArrayAdapter<>(this, R.layout.midi_event, R.id.midiEventDescriptionTextView);
        midiOutputEventListView.setAdapter(midiOutputEventAdapter);

        deviceSpinner = (Spinner) findViewById(R.id.deviceNameSpinner);
        connectedOutputDevicesAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.simple_spinner_dropdown_item, android.R.id.text1, new ArrayList<MidiOutputDevice>());
        deviceSpinner.setAdapter(connectedOutputDevicesAdapter);

        View.OnTouchListener onToneButtonTouchListener = new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                MidiOutputDevice midiOutputDevice = getBleMidiOutputDeviceFromSpinner();
                if (midiOutputDevice == null) {
                    return false;
                }

                int note = 60 + Integer.parseInt((String) v.getTag());
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        midiOutputDevice.sendMidiNoteOn(0, note, 127);
                        midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "NoteOn to: " + midiOutputDevice.getDeviceName() + ", note: " + note + ", velocity: 127"));
                        break;
                    case MotionEvent.ACTION_UP:
                        midiOutputDevice.sendMidiNoteOff(0, note, 127);
                        midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "NoteOff to: " + midiOutputDevice.getDeviceName() + ", note: " + note + ", velocity: 127"));
                        break;
                    default:
                        // do nothing.
                        break;
                }
                return false;
            }
        };


        int whiteKeyColor = 0xFFFFFFFF;
        int blackKeyColor = 0xFF808080;


        soundMaker = SoundMaker.getInstance();
        final int bufferSize = AudioTrack.getMinBufferSize(soundMaker.getSamplingRate(), AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int timerRate = bufferSize * 1000 / soundMaker.getSamplingRate() / 2;
        final short[] wav = new short[bufferSize / 2];

        audioTrack = prepareAudioTrack(soundMaker.getSamplingRate());
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                if (soundMaker != null) {
                    synchronized (tones) {
                        for (int i = 0; i < wav.length; i++) {
                            wav[i] = (short) (soundMaker.makeWaveStream(tones) * 1024);
                        }
                    }
                    try {
                        if (audioTrack != null) {
                            audioTrack.write(wav, 0, wav.length);
                        }
                    } catch (IllegalStateException | NullPointerException e) {
                        // do nothing
                    }
                }
            }
        };
        timer.scheduleAtFixedRate(timerTask, 10, timerRate);

        Button disconnectButton = (Button) findViewById(R.id.disconnectButton);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MidiOutputDevice bleMidiOutputDeviceFromSpinner = getBleMidiOutputDeviceFromSpinner();
                if (bleMidiOutputDeviceFromSpinner != null) {
                    bleMidiCentralProvider.disconnectDevice(bleMidiOutputDeviceFromSpinner);
                }
            }
        });

        Button resetButton = (Button) findViewById(R.id.resetButton);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    catchCounter = 0;
                    counterTextView.setText(String.valueOf(catchCounter));
            }
        });

        if (!BleUtils.isBluetoothEnabled(this)) {
            BleUtils.enableBluetooth(this);
            return;
        }

        if (!BleUtils.isBleSupported(this)) {
            // display alert and exit
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Not supported");
            alertDialog.setMessage("Bluetooth LE is not supported on this device. The app will exit.");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                }
            });
            alertDialog.show();
        } else {
            setupCentralProvider();
        }
    }

    /**
     * Configure BleMidiCentralProvider instance
     */
    private void setupCentralProvider() {
        bleMidiCentralProvider = new BleMidiCentralProvider(this);

        bleMidiCentralProvider.setOnMidiDeviceAttachedListener(new OnMidiDeviceAttachedListener() {
            @Override
            public void onMidiInputDeviceAttached(@NonNull MidiInputDevice midiInputDevice) {
                midiInputDevice.setOnMidiInputEventListener(onMidiInputEventListener);
            }

            @Override
            public void onMidiOutputDeviceAttached(@NonNull MidiOutputDevice midiOutputDevice) {
                Message message = new Message();
                message.arg1 = 0;
                message.obj = midiOutputDevice;
                midiOutputConnectionChangedHandler.sendMessage(message);
            }
        });

        bleMidiCentralProvider.setOnMidiDeviceDetachedListener(new OnMidiDeviceDetachedListener() {
            @Override
            public void onMidiInputDeviceDetached(@NonNull MidiInputDevice midiInputDevice) {
                // do nothing
            }

            @Override
            public void onMidiOutputDeviceDetached(@NonNull MidiOutputDevice midiOutputDevice) {
                Message message = new Message();
                message.arg1 = 1;
                message.obj = midiOutputDevice;
                midiOutputConnectionChangedHandler.sendMessage(message);
            }
        });

        bleMidiCentralProvider.setOnMidiScanStatusListener(new OnMidiScanStatusListener() {
            @Override
            public void onMidiScanStatusChanged(boolean isScanning) {
                CentralActivity.this.isScanning = isScanning;
                if (toggleScanMenu != null) {
                    if (isScanning) {
                        toggleScanMenu.setTitle(R.string.stop_scan);
                    } else {
                        toggleScanMenu.setTitle(R.string.start_scan);
                    }
                }
            }
        });

        // scan devices for 30 seconds
        startScanDeviceWithRequestingPermission(30000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == BleUtils.REQUEST_CODE_BLUETOOTH_ENABLE) {
            if (!BleUtils.isBluetoothEnabled(this)) {
                // User selected NOT to use Bluetooth.
                // do nothing
                return;
            }

            if (!BleUtils.isBleSupported(this)) {
                // display alert and exit
                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle("Not supported");
                alertDialog.setMessage("Bluetooth LE is not supported on this device. The app will exit.");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                    }
                });
                alertDialog.show();
            } else {
                setupCentralProvider();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (bleMidiCentralProvider != null) {
            bleMidiCentralProvider.terminate();
        }

        if (timer != null) {
            try {
                timer.cancel();
                timer.purge();
            } catch (Throwable t) {
                // do nothing
            } finally {
                timer = null;
            }
        }
        if (audioTrack != null) {
            try {
                audioTrack.stop();
                audioTrack.flush();
                audioTrack.release();
            } catch (Throwable t) {
                // do nothing
            } finally {
                audioTrack = null;
            }
        }

        super.onDestroy();
    }

    /**
     * @param samplingRate sampling rate for playing
     * @return configured {@link android.media.AudioTrack} instance
     */
    private static AudioTrack prepareAudioTrack(int samplingRate) {
        AudioTrack result = new AudioTrack(AudioManager.STREAM_MUSIC, samplingRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, AudioTrack.getMinBufferSize(samplingRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT), AudioTrack.MODE_STREAM);
        result.setStereoVolume(1f, 1f);
        result.play();
        return result;
    }
}
