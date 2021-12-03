package ece.wisc.intonationtuner;

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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import java.util.concurrent.Executor;

import ece.wisc.intonationtuner.databinding.FragmentSecondBinding;

public class SecondFragment extends Fragment {

    private FragmentSecondBinding binding;

    private AudioRecord myAudioRecorder;
    private AudioProcessingThread audioProcessingThread = new AudioProcessingThread();

    private Thread myThread;
    private Executor myExecutor;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentSecondBinding.inflate(inflater, container, false);
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

        myAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 45000, AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT, 1024);

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

        /*myExecutor = new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
        myExecutor.execute(new AudioProcessingThread());*/
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
            myAudioRecorder.release();
            myAudioRecorder = null;
        }

        super.onDestroyView();

        binding = null;

        Toast.makeText(getActivity().getApplicationContext(), "Recording Stopped",
                Toast.LENGTH_LONG).show();
    }

    class AudioProcessingThread implements Runnable {
        @Override
        public void run() {
            byte[] audioData = new byte[1024];

            Log.e("TEST", "Test");

            while (myAudioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                myAudioRecorder.read(audioData, 0 , 1024);
                int a = flwt(audioData);
                Log.e("TEST", "Test" + Integer.toString(a));
                //System.out.println("Test" + Integer.toString(a));
                SystemClock.sleep(500);
            }
        }
    }

}