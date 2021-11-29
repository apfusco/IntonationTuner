package ece.wisc.intonationtuner;

import static ece.wisc.intonationtuner.MainActivity.flwt;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import ece.wisc.intonationtuner.databinding.FragmentSecondBinding;

public class SecondFragment extends Fragment {

    private FragmentSecondBinding binding;

    private AudioRecord myAudioRecorder;
    private AudioProcessingThread audioProcessingThread = new AudioProcessingThread();

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

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if(myAudioRecorder != null) {
            myAudioRecorder.stop();
            myAudioRecorder.release();
            myAudioRecorder = null;
        }

        binding = null;

        Toast.makeText(getActivity().getApplicationContext(), "Recording Stopped",
                Toast.LENGTH_LONG).show();
    }

    class AudioProcessingThread implements Runnable {
        @Override
        public void run() {
            byte[] audioData = new byte[1024];

            while (myAudioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                myAudioRecorder.read(audioData, 0 , 1024);
                flwt();
                SystemClock.sleep(500);
            }
        }
    }

}