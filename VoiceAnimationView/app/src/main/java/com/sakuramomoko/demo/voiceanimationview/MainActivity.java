package com.sakuramomoko.demo.voiceanimationview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.sakuramomoko.voiceanimationview.MovableVolumeAnimView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.left_status)
    MovableVolumeAnimView movableVolumeAnimView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        movableVolumeAnimView.start();
    }
}
