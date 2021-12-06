package ece.wisc.intonationtuner;

import static java.lang.Math.*;
import static ece.wisc.intonationtuner.MainActivity.flwt;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.concurrent.Executor;

import ece.wisc.intonationtuner.databinding.FragmentSecondBinding;

public class SecondFragment extends Fragment {

    private FragmentSecondBinding binding;

    private AudioRecord myAudioRecorder;

    private Thread myThread;

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
        final String[] name = {"c", "c#", "d", "d#", "e", "f", "f#", "g", "g#", "a", "a#", "b"};
        final double[] pitches = {32.705, 34.65, 36.71, 38.89, 41.205, 43.655, 46.25, 49.0, 51.915, 55.0, 58.27, 61.74};
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
        n = Arrays.asList(name).indexOf(noteName);

        return (int) (pitches[n] * Math.pow(2, octave - 1));
    }

    private static String getNote(int freq) {
        final int a4 = 440;
        double c0 = a4 * Math.pow(2, -4.75);
        final String[] name = {"c", "c#", "d", "d#", "e", "f", "f#", "g", "g#", "a", "a#", "b"};

        int h = (int)Math.round(12 * Math.log10(freq / c0) / Math.log10(2));
        int octave = h / 12;
        int n = h % 12;
        return name[n] + Integer.toString(octave);
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
                            String note = getNote(Math.round(pitch));
                            if (binding != null) {
                                binding.textviewSecond.setText(note);
                                binding.textviewSecond2.setText(Integer.toString(getPitch(note) - Math.round(pitch)));
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