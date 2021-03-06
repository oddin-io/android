package br.com.sienaidea.oddin.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import br.com.sienaidea.oddin.R;
import br.com.sienaidea.oddin.adapter.AdapterMaterial;
import br.com.sienaidea.oddin.interfaces.RecyclerViewOnClickListenerOnLongPressListener;
import br.com.sienaidea.oddin.model.Constants;
import br.com.sienaidea.oddin.retrofitModel.Material;
import br.com.sienaidea.oddin.server.Preference;
import br.com.sienaidea.oddin.view.LectureDetailsActivity;

public class MaterialDisciplineFragment extends Fragment implements RecyclerViewOnClickListenerOnLongPressListener, View.OnClickListener {
    private RecyclerView mRecyclerView;
    private TextView mEmptyView;
    private List<Material> mList;
    private AdapterMaterial mAdapter;
    private Context mContext;

    public static final String TAG = MaterialDisciplineFragment.class.getName();

    @Override
    public void onAttach(Context context) {
        mContext = context;
        super.onAttach(context);
    }

    public static MaterialDisciplineFragment newInstance(List<Material> list) {

        MaterialDisciplineFragment fragment = new MaterialDisciplineFragment();

        Bundle args = new Bundle();
        args.putParcelableArrayList(Material.TAG, (ArrayList<Material>) list);
        fragment.setArguments(args);

        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment, container, false);
        mEmptyView = (TextView) view.findViewById(R.id.empty_view);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.rv_list);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addOnItemTouchListener(new RecyclerViewTouchListener(mContext, mRecyclerView, this));

        LinearLayoutManager llm = new LinearLayoutManager(mContext);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(llm);

        mList = getArguments().getParcelableArrayList(Material.TAG);
        if (mList != null) {
            mAdapter = new AdapterMaterial(mContext, mList);
            mRecyclerView.setAdapter(mAdapter);
            notifyDataSetChanged();
        }

        return view;
    }

    public void addItemPosition(int position, Material material) {
        mAdapter.addItemPosition(position, material);
        mRecyclerView.getLayoutManager().smoothScrollToPosition(mRecyclerView, null, position);
        checkState();
    }

    public void removeItem(int position){
        mList.remove(position);
        notifyDataSetChanged();
    }

    public void notifyDataSetChanged() {
        mAdapter.notifyDataSetChanged();
        checkState();
    }

    private void setEmpty(boolean isEmpty) {
        if (isEmpty) {
            mRecyclerView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
        }
    }

    private void checkState() {
        if (mList.isEmpty())
            setEmpty(true);
        else setEmpty(false);
    }

    @Override
    public void onClickListener(final int position) {
        final Material material = mAdapter.getMaterial(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.AppCompatAlertDialogStyle);
        builder.setTitle(material.getName());
        builder.setMessage(R.string.dialog_start_download);
        builder.setNegativeButton(R.string.dialog_cancel, null);
        builder.setPositiveButton(R.string.dialog_download, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ((LectureDetailsActivity) getActivity()).getMaterial(material);
            }
        });
        builder.show();
    }

    @Override
    public void onLongPressClickListener(final int position) {
        final Material material = mAdapter.getMaterial(position);

        Preference preference = new Preference();
        if (preference.getUserProfile(mContext) == Constants.INSTRUCTOR) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.AppCompatAlertDialogStyle);
            builder.setPositiveButton(R.string.dialog_delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ((LectureDetailsActivity) getActivity()).deleteMaterial(position, material);
                }
            });
            builder.setNegativeButton(R.string.dialog_cancel, null);
            builder.setTitle(material.getName());
            builder.setMessage(R.string.dialog_delete_material);
            builder.show();
        }
    }

    @Override
    public void onClick(View v) {
    }

    private static class RecyclerViewTouchListener implements RecyclerView.OnItemTouchListener {
        private Context mContext;
        private GestureDetector mGestureDetector;
        private RecyclerViewOnClickListenerOnLongPressListener mRecyclerViewOnClickListenerOnLongPressListener;

        public RecyclerViewTouchListener(Context c, final RecyclerView recyclerView, RecyclerViewOnClickListenerOnLongPressListener rvoclh) {
            mContext = c;
            mRecyclerViewOnClickListenerOnLongPressListener = rvoclh;

            mGestureDetector = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public void onLongPress(MotionEvent e) {
                    View itemView = recyclerView.findChildViewUnder(e.getX(), e.getY());

                    if (mRecyclerViewOnClickListenerOnLongPressListener != null && itemView != null) {
                        mRecyclerViewOnClickListenerOnLongPressListener.onLongPressClickListener(
                                recyclerView.getChildAdapterPosition(itemView));
                    }
                }

                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    View itemView = recyclerView.findChildViewUnder(e.getX(), e.getY());

                    if (mRecyclerViewOnClickListenerOnLongPressListener != null && itemView != null) {
                        mRecyclerViewOnClickListenerOnLongPressListener.onClickListener(
                                recyclerView.getChildAdapterPosition(itemView));
                    }
                    return true;
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
            mGestureDetector.onTouchEvent(e);
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean b) {
        }
    }
}
