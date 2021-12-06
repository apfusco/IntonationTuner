package ece.wisc.intonationtuner;

import static ece.wisc.intonationtuner.MainActivity.flwt;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ece.wisc.intonationtuner.databinding.FragmentSecondBinding;

public class SecondFragment extends Fragment {

    private FragmentSecondBinding binding;

    private AudioRecord myAudioRecorder;

    private Thread myThread;

    private static final String[] names = {"c", "c#", "d", "d#", "e", "f", "f#", "g", "g#", "a", "a#", "b"};
    private static final double[] pitches = {32.705, 34.65, 36.71, 38.89, 41.205, 43.655, 46.25, 49.0, 51.915, 55.0, 58.27, 61.74};
    private static final double[] ratios = {1, 25.0/24, 9.0/8, 6.0/5, 5.0/4, 4.0/3, 45.0/32, 3.0/2, 8.0/5, 5.0/3, 9.0/5, 15.0/8};

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentSecondBinding.inflate(inflater, container, false);
        //binding.textviewSecond.setText("Test");
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(SecondFragment.this)
                        .navigate(R.id.action_SecondFragment_to_FirstFragment);
            }
        });
        List<String> keys = new ArrayList<String>();
        for (int i = 0; i < names.length; i++) {
            keys.add(names[i]);
        }
        /*keys.add("c");
        keys.add("c#");
        keys.add("d");
        keys.add("d#");
        keys.add("e");
        keys.add("f");
        keys.add("f#");
        keys.add("g");
        keys.add("g#");
        keys.add("a");
        keys.add("a#");
        keys.add("b");*/
        keys.add("Equal Temperament");

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getContext(), R.layout.support_simple_spinner_dropdown_item, keys);
        binding.keySpinner.setAdapter(dataAdapter);

        Log.e("TEST", "TESTLOG");

        // Request permission to record audio
        if (ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }

        myAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 45000, AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_FLOAT, 1024);

        try {
            // TODO
            myAudioRecorder.startRecording();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Toast.makeText(getActivity().getApplicationContext(), "Recording Started",
                Toast.LENGTH_LONG).show();

        myThread = new Thread(new AudioProcessingThread());
        myThread.start();


    }

    @Override
    public void onDestroyView() {
        //myThread.stop();

        if (myAudioRecorder != null) {
            myAudioRecorder.stop();
            while (myThread.isAlive()) {
                try {
                    myThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //binding.textviewSecond.removeCallbacks(myAudioThread);
            myAudioRecorder.release();
            myAudioRecorder = null;
        }

        super.onDestroyView();

        binding = null;

        Toast.makeText(getActivity().getApplicationContext(), "Recording Stopped",
                Toast.LENGTH_LONG).show();
    }

    private static int getPitch(String note) {
        final int a4 = 440;
        String noteName;
        int octave, n;
        boolean sharp;

        if (note.charAt(1) == '#') {
            sharp = true;
            noteName = note.substring(0, 2);
            octave = Integer.parseInt(note.substring(2));
        } else {
            sharp = false;
            noteName = note.substring(0, 1);
            octave = Integer.parseInt(note.substring(1));
        }
        n = Arrays.asList(names).indexOf(noteName);

        return (int) (pitches[n] * Math.pow(2, octave - 1));
    }

    private static String getNote(int freq) {
        final int a4 = 440;
        double c0 = a4 * Math.pow(2, -4.75);

        int h = (int)Math.round(12 * Math.log10(freq / c0) / Math.log10(2));
        int octave = h / 12;
        int n = h % 12;
        return names[n] + Integer.toString(octave);
    }

    class AudioProcessingThread implements Runnable {
        @Override
        public void run() {
            float[] audioData = new float[1024];

            Log.e("TEST", "Test");

            while (myAudioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                myAudioRecorder.read(audioData, 0 , 1024, AudioRecord.READ_BLOCKING);
                float pitch = flwt(audioData);
                Log.e("TEST", "Pitch: " + Float.toString(pitch));

                if (pitch != 0.0) {
                    //binding.textviewSecond.setText(getNote(Math.round(pitch)));
                    binding.textviewSecond.post(new Runnable() {
                        @Override
                        public void run() {
                            int expct_pitch = 0;
                            String note = getNote(Math.round(pitch));
                            if (binding != null) {
                                binding.textviewSecond.setText(note);
                                if (binding.keySpinner.getSelectedItem().equals("Equal Temperament")) {
                                    binding.textviewSecond2.setText(Integer.toString(getPitch(note) - Math.round(pitch)));
                                } else {
                                    int n;
                                    if (note.charAt(1) == '#') {
                                        n = Arrays.asList(names).indexOf(note.substring(0, 2)) - Arrays.asList(names).indexOf((String)binding.keySpinner.getSelectedItem());
                                    } else {
                                        n = Arrays.asList(names).indexOf(note.substring(0, 1)) - Arrays.asList(names).indexOf((String)binding.keySpinner.getSelectedItem());
                                    }
                                    int octave;
                                    int root_pitch;

                                    if (note.charAt(1) == '#') {
                                        octave = Integer.parseInt(note.substring(2));
                                    } else {
                                        octave = Integer.parseInt(note.substring(1));
                                    }

                                    if (n < 0) {
                                        n = names.length + n;
                                        octave--;
                                    }
                                    //n %= names.length;

                                    root_pitch = getPitch(((String)binding.keySpinner.getSelectedItem()) + Integer.toString(octave));
                                    expct_pitch = (int)Math.round((double)root_pitch * 1.0 * ratios[n]);
                                    binding.textviewSecond2.setText(Integer.toString(expct_pitch - Math.round(pitch)));
                                    //binding.textviewSecond2.setText(Integer.toString(expct_pitch) + " " + Integer.toString(Math.round(getPitch(note))) + " " + Integer.toString(Math.round(pitch)));
                                }
                            }
                        }
                    });
                } else {
                    //binding.textviewSecond.setText("Play a note");
                    binding.textviewSecond.post(new Runnable() {
                        @Override
                        public void run() {
                            if (binding != null) {
                                binding.textviewSecond.setText(R.string.play_note);
                                binding.textviewSecond2.setText(R.string.no_pitch);
                            }
                        }
                    });
                }
                SystemClock.sleep(50);
            }
        }
    }

}