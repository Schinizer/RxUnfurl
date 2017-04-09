package com.schinizer.rxunfurl.sample;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.EditText;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.adapters.FastItemAdapter;
import com.schinizer.rxunfurl.RxUnfurl;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;

    @BindView(R.id.editText)
    EditText editText;

    FastItemAdapter<UrlPreviewItem> fastAdapter = new FastItemAdapter<>();
    RxUnfurl rxUnfurl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        rxUnfurl = new RxUnfurl.Builder()
                .scheduler(Schedulers.io())
                .build();

        recyclerView.setAdapter(fastAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true));

        fastAdapter.withOnClickListener(new FastAdapter.OnClickListener<UrlPreviewItem>() {
            @Override
            public boolean onClick(View v, IAdapter<UrlPreviewItem> adapter, UrlPreviewItem item, int position) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.url));
                startActivity(intent);
                return true;
            }
        });
    }

    @OnClick(R.id.fab)
    void GeneratePreview() {
        UrlPreviewItem item = new UrlPreviewItem(URLUtil.guessUrl(editText.getText().toString()));
        UrlPreviewItemPresenter presenter = new UrlPreviewItemPresenter(rxUnfurl);
        presenter.setView(item);
        item.setPresenter(presenter);

        fastAdapter.add(0, item);
        recyclerView.scrollToPosition(0);
        editText.setText("");
    }
}
