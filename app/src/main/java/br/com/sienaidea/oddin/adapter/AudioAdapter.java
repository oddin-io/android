package br.com.sienaidea.oddin.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;

import java.io.IOException;
import java.util.List;

import br.com.sienaidea.oddin.R;
import br.com.sienaidea.oddin.model.Constants;
import br.com.sienaidea.oddin.retrofitModel.Answer;
import br.com.sienaidea.oddin.retrofitModel.Material;
import br.com.sienaidea.oddin.view.DoubtDetailsActivity;

public class AudioAdapter extends RecyclerView.Adapter<AudioAdapter.MyViewHolder> {
    private LayoutInflater mLayoutInflater;
    private List<Answer> mList;
    private Context mContext;
    private int mProfile;
    //Media Player
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private int startTime = 0;
    private int finalTime = 0;
    private Handler SeekHandler = new Handler();

    public static int oneTimeOnly = 0;

    public AudioAdapter(Context context, List<Answer> list, int profile) {
        mContext = context;
        this.mList = list;
        this.mProfile = profile;
        this.mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mLayoutInflater.inflate(R.layout.item_audio_view, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder myViewHolder, final int position) {
        final List<Material> materials = mList.get(position).getMaterials();

        for (final Material material : materials) {
            if (material.isDownloaded()) {
                myViewHolder.iv_player.setImageResource(R.drawable.ic_play);
                myViewHolder.iv_player.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            if (mediaPlayer.isPlaying()) {
                                mediaPlayer.pause();
                                myViewHolder.iv_player.setImageResource(R.drawable.ic_play);
                                return;
                            } else {
                                if (oneTimeOnly != 0) {
                                    myViewHolder.iv_player.setImageResource(R.drawable.ic_pause);
                                    mediaPlayer.start();
                                    return;
                                }
                            }

                            mediaPlayer.reset();
                            mediaPlayer.setDataSource(mContext, Uri.parse(material.getUrl()));
                            myViewHolder.seekBar.setClickable(false);
                            mediaPlayer.prepare();
                            mediaPlayer.start();
                            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mp) {
                                    Log.d("onCompletion", "onCompletion");
                                    myViewHolder.iv_player.setImageResource(R.drawable.ic_play);
                                    mediaPlayer.seekTo(0);
                                    oneTimeOnly = 0;
                                }
                            });

                            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(MediaPlayer mp) {
                                    myViewHolder.iv_player.setImageResource(R.drawable.ic_pause);
                                }
                            });

                            Runnable UpdateSongTime = new Runnable() {
                                public void run() {
                                    myViewHolder.seekBar.setProgress(mediaPlayer.getCurrentPosition());
                                    SeekHandler.postDelayed(this, 100);
                                }
                            };

                            finalTime = mediaPlayer.getDuration();
                            startTime = mediaPlayer.getCurrentPosition();

                            if (oneTimeOnly == 0) {
                                myViewHolder.seekBar.setMax(finalTime);
                                oneTimeOnly = 1;
                            }

                            //myViewHolder.seekBar.setMax(finalTime);
                            myViewHolder.seekBar.setProgress(startTime);

                            SeekHandler.postDelayed(UpdateSongTime, 100);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                myViewHolder.seekBar.setEnabled(true);
            }
            if (mProfile == Constants.INSTRUCTOR) {
                myViewHolder.iv_understand.setEnabled(false);
                myViewHolder.iv_understand.setClickable(false);
            } else {
                if (material.isDownloaded()) {
                    myViewHolder.iv_understand.setEnabled(true);
                    myViewHolder.iv_understand.setClickable(true);
                    myViewHolder.iv_understand.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.AppCompatAlertDialogStyle);
                            builder.setNegativeButton(R.string.dialog_cancel, null);
                            if (material.isAccepted()) {
//                        builder.setMessage(R.string.dialog_delete_accept_answer);
//                        builder.setPositiveButton(R.string.dialog_confirm, new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                ((DoubtDetailsActivity) mContext).acceptAnswer(answer);
//                            }
//                        });
//                        builder.show();
                            } else {
                                builder.setMessage(R.string.dialog_accept_answer);
                                builder.setPositiveButton(R.string.dialog_confirm, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ((DoubtDetailsActivity) mContext).acceptAnswer(mList.get(position));
                                    }
                                });
                                builder.show();
                            }
                        }
                    });
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public Material getMaterial(int position) {
        return mList.get(position).getMaterials().get(position);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private SeekBar seekBar;
        private ImageView iv_player, iv_understand;

        public MyViewHolder(View itemView) {
            super(itemView);
            seekBar = (SeekBar) itemView.findViewById(R.id.seekBar);
            seekBar.setEnabled(false);
            iv_player = (ImageView) itemView.findViewById(R.id.iv_player);
            iv_understand = (ImageView) itemView.findViewById(R.id.iv_understand);
            //durationTextView = (TextView) itemView.findViewById(R.id.tv_duration);
        }
    }
}
