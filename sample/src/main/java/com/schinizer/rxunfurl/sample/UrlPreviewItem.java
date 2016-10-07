package com.schinizer.rxunfurl.sample;

import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mikepenz.fastadapter.items.AbstractItem;
import com.mikepenz.fastadapter.utils.ViewHolderFactory;
import com.schinizer.rxunfurl.model.PreviewData;
import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by tinkerbox on 24/8/16.
 */

class UrlPreviewItem extends AbstractItem<UrlPreviewItem, UrlPreviewItem.ViewHolder> implements UrlPreviewContract.View {

    private static final ViewHolderFactory<? extends ViewHolder> FACTORY = new ItemFactory();

    String url;
    private UrlPreviewContract.Presenter presenter;
    private ViewHolder viewHolder;

    UrlPreviewItem(String url) {
        this.url = url;
    }

    //The unique ID for this type of item
    @Override
    public int getType() {
        return R.id.fastadapter_urlpreview_item_id;
    }

    //The layout to be used for this type of item
    @Override
    public int getLayoutRes() {
        return R.layout.card_url_preview;
    }

    @Override
    public void populateView(PreviewData data) {
        if (viewHolder == null)
            return;

        if (data != null) {
            viewHolder.previewLayout.setVisibility(View.VISIBLE);
            viewHolder.message.setVisibility(View.GONE);

            Uri uri = Uri.parse(data.getUrl());

            if (!data.getTitle().isEmpty()) viewHolder.title.setText(data.getTitle());
            if (!data.getDescription().isEmpty()) viewHolder.content.setText(data.getDescription());
            if (!data.getUrl().isEmpty()) viewHolder.host.setText(uri.getHost().toUpperCase());

            if (data.getImages().size() != 0) {
                viewHolder.imageView.setVisibility(View.VISIBLE);
                Picasso.with(viewHolder.itemView.getContext())
                        .load(data.getImages().get(0).getSource())
                        .fit()
                        .centerCrop()
                        .into(viewHolder.imageView);
            } else {
                viewHolder.imageView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void setPresenter(UrlPreviewContract.Presenter presenter) {
        this.presenter = presenter;
    }

    //The logic to bind your data to the view
    @Override
    public void bindView(final ViewHolder viewHolder) {
        //call super so the selection is already handled for you
        super.bindView(viewHolder);
        this.viewHolder = viewHolder;

        viewHolder.message.setText(url);
        viewHolder.previewLayout.setVisibility(View.GONE);
        viewHolder.message.setVisibility(View.VISIBLE);

        presenter.GeneratePreview(url);
    }

    /**
     * return our ViewHolderFactory implementation here
     *
     * @return
     */
    @Override
    public ViewHolderFactory<? extends ViewHolder> getFactory() {
        return FACTORY;
    }

    /**
     * our ItemFactory implementation which creates the ViewHolder for our adapter.
     * It is highly recommended to implement a ViewHolderFactory as it is 0-1ms faster for ViewHolder creation,
     * and it is also many many times more efficient if you define custom listeners on views within your item.
     */
    private static class ItemFactory implements ViewHolderFactory<ViewHolder> {
        public ViewHolder create(View v) {
            return new ViewHolder(v);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.previewLayout)
        LinearLayout previewLayout;
        @BindView(R.id.imageView)
        ImageView imageView;
        @BindView(R.id.title)
        TextView title;
        @BindView(R.id.content)
        TextView content;
        @BindView(R.id.host)
        TextView host;
        @BindView(R.id.message)
        TextView message;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
