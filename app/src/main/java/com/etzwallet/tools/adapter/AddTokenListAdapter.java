package com.etzwallet.tools.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.etzwallet.R;
import com.etzwallet.presenter.customviews.BRText;
import com.etzwallet.presenter.customviews.MyLog;
import com.etzwallet.presenter.entities.TokenItem;
import com.etzwallet.tools.threads.executor.BRExecutor;
import com.platform.APIClient;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class AddTokenListAdapter extends RecyclerView.Adapter<AddTokenListAdapter.TokenItemViewHolder> {

    private Context mContext;
    private ArrayList<TokenItem> mTokens;
    private ArrayList<TokenItem> mBackupTokens;
    private static final String TAG = AddTokenListAdapter.class.getSimpleName();
    private OnTokenAddOrRemovedListener mListener;

    public AddTokenListAdapter(Context context, OnTokenAddOrRemovedListener listener) {

        this.mContext = context;
        this.mTokens = new ArrayList<>();
        this.mListener = listener;
        this.mBackupTokens = mTokens;
    }

    public void refreshList(ArrayList<TokenItem> list) {
        if (list.size() > 0) mTokens.clear();
        mTokens.addAll(list);
        mBackupTokens = mTokens;
        notifyDataSetChanged();
    }

    public interface OnTokenAddOrRemovedListener {

        void onTokenAdded(TokenItem token);

        void onTokenRemoved(TokenItem token);
    }


    @Override
    public void onBindViewHolder(final @NonNull AddTokenListAdapter.TokenItemViewHolder holder, final int position) {

        TokenItem item = mTokens.get(position);
        String tickerName = item.symbol.toLowerCase();

        if (tickerName.equals("1st")) {
            tickerName = "first";
        }

        String iconResourceName = tickerName;
//        int iconResourceId = mContext.getResources().getIdentifier(tickerName, "drawable", mContext.getPackageName());
        holder.name.setText(mTokens.get(position).name);
        holder.symbol.setText(mTokens.get(position).symbol);

        String iconName = mTokens.get(position).symbol.toLowerCase();
        String pathDir = mContext.getFilesDir().getAbsolutePath() + APIClient.ETZ_TOKEN_ICON_EXTRACTED;
        File imageDir = new File(pathDir);
        if (!imageDir.exists()) {
            imageDir.mkdir();
        }
        final File image = new File(imageDir, iconName + ".png");

        if (image.exists()) {
            MyLog.i("+++++++++++++文件");
            Picasso.get().load(image).into(holder.logo);
        } else {
            MyLog.i("+++++++++++++网络"+mTokens.get(position).image);
            Picasso.get().load(mTokens.get(position).image).into(holder.logo);
            MyLog.i(mTokens.get(position).image);
            downloadIcon(mTokens.get(position).image, image.getAbsolutePath());


        }



        holder.addRemoveButton.setText(mContext.getString(item.isAdded ? R.string.TokenList_remove : R.string.TokenList_add));
        holder.addRemoveButton.setBackground(mContext.getDrawable(item.isAdded ? R.drawable.remove_wallet_button : R.drawable.add_wallet_button));
        holder.addRemoveButton.setTextColor(mContext.getResources().getColor(item.isAdded ? R.color.red : R.color.dialog_button_positive));

        holder.addRemoveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Set button to "Remove"
                if (!mTokens.get(position).isAdded) {
                    mTokens.get(position).isAdded = true;
                    mListener.onTokenAdded(mTokens.get(position));
                }

                // Set button back to "Add"
                else {
                    mTokens.get(position).isAdded = false;
                    mListener.onTokenRemoved(mTokens.get(position));

                }

            }
        });


    }

    @Override
    public int getItemCount() {
        return mTokens.size();
    }

    @NonNull
    @Override
    public AddTokenListAdapter.TokenItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        View convertView = inflater.inflate(R.layout.token_list_item, parent, false);

        TokenItemViewHolder holder = new TokenItemViewHolder(convertView);
        holder.setIsRecyclable(false);

        return holder;
    }

    public class TokenItemViewHolder extends RecyclerView.ViewHolder {

        private ImageView logo;
        private BRText symbol;
        private BRText name;
        private Button addRemoveButton;

        public TokenItemViewHolder(View view) {
            super(view);

            logo = view.findViewById(R.id.token_icon);
            symbol = view.findViewById(R.id.token_ticker);
            name = view.findViewById(R.id.token_name);
            addRemoveButton = view.findViewById(R.id.add_remove_button);

            Typeface typeface = Typeface.createFromAsset(mContext.getAssets(), "fonts/CircularPro-Book.otf");
            addRemoveButton.setTypeface(typeface);
        }
    }

    public void resetFilter() {
        mTokens = mBackupTokens;
        notifyDataSetChanged();
    }

    public void filter(String query) {
        resetFilter();
        ArrayList<TokenItem> filteredList = new ArrayList<>();

        query = query.toLowerCase();

        for (TokenItem item : mTokens) {
            if (item.name.toLowerCase().contains(query) || item.symbol.toLowerCase().contains(query)) {
                filteredList.add(item);
            }
        }

        mTokens = filteredList;
        notifyDataSetChanged();

    }

    private void downloadIcon(String url, final String iconName) {
        //获得图片的地址

        //Target
        Target target = new Target() {

            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {

                File dcimFile = new File(iconName);

                FileOutputStream ostream = null;
                try {
                    ostream = new FileOutputStream(dcimFile);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, ostream);
                    ostream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {

            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        };

        Picasso.get().load(url).into(target);

    }

}
