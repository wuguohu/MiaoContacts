package com.polaris.miaocontacts;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.polaris.miaocontacts.data.ContactHelper;
import com.polaris.miaocontacts.data.ContactInfo;
import com.polaris.miaocontacts.dummy.DummyContent;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * An activity representing a list of Items. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link ItemDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class ItemListActivity extends AppCompatActivity {

    private static final String[] PERMISSION_CONTACTS = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.CALL_PHONE, Manifest.permission.GET_ACCOUNTS, Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS};

    private List<ContactInfo> mContactInfos = new ArrayList<>();

    private ProgressDialog mProgressDialog;

    private SimpleItemRecyclerViewAdapter mAdapter;
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(ItemListActivity.this).setMessage("确认清空联系人？").setPositiveButton("确认", new AlertDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deleteContacts();
                    }
                }).setNegativeButton("取消", new AlertDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                }).create().show();
            }
        });

        if (findViewById(R.id.item_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        setupRecyclerView();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, PERMISSION_CONTACTS[0]) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, PERMISSION_CONTACTS[1]) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, PERMISSION_CONTACTS[2]) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, PERMISSION_CONTACTS[3]) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, PERMISSION_CONTACTS[4]) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(PERMISSION_CONTACTS, 200);
            }else{
                getContacts();
            }
        }else{
            getContacts();
        }


    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.item_list);
        assert recyclerView != null;
        mAdapter = new SimpleItemRecyclerViewAdapter(this, mContactInfos, mTwoPane);
        recyclerView.setAdapter(mAdapter);
    }

    private void getContacts(){
//        setupRecyclerView();
        showProgress("加载中...");
        Observable.create(new ObservableOnSubscribe<List<ContactInfo>>() {
            @Override
            public void subscribe(ObservableEmitter<List<ContactInfo>> e) throws Exception {
                try {
                    List<ContactInfo> contactInfos = new ContactHelper().getContacts(ItemListActivity.this);
                    e.onNext(contactInfos);
                } catch (Exception e1) {
                    e.onError(e1);
                }
            }
        }).observeOn(
                AndroidSchedulers.mainThread()
        ).subscribeOn(
                Schedulers.io()
        ).subscribe(new Consumer<List<ContactInfo>>() {
            @Override
            public void accept(List<ContactInfo> contactInfos) throws Exception {
                cancelProgress();
                mContactInfos.clear();
                if(contactInfos != null){
                    mContactInfos.addAll(contactInfos);
                }

                if(mAdapter != null){
                    mAdapter.notifyDataSetChanged();
                }
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                cancelProgress();
                Toast.makeText(ItemListActivity.this, "联系人读取失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteContacts(){
        showProgress("处理中...");
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> e) throws Exception {
                try {
                    if(mContactInfos != null && mContactInfos.size() > 0){
                        ContactHelper helper = new ContactHelper();
                        for(ContactInfo info :mContactInfos){
                            helper.deleteContacts(ItemListActivity.this, info.getId());
                        }
                    }
                    e.onNext(0);
                } catch (Exception e1) {
                    e.onError(e1);
                }
            }
        }).observeOn(
                AndroidSchedulers.mainThread()
        ).subscribeOn(
                Schedulers.io()
        ).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer result) throws Exception {
                cancelProgress();
                getContacts();
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                cancelProgress();
                Toast.makeText(ItemListActivity.this, "联系人删除失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isGrant = true;
        for(int result : grantResults){
            if(result != PackageManager.PERMISSION_GRANTED){
                isGrant = false;
                break;
            }
        }

        if(isGrant){
            getContacts();
        }else{
            Toast.makeText(this, "没有获取到操作联系人相关权限，应用可能无法正常使用", Toast.LENGTH_SHORT).show();
        }
    }

    private void showProgress(String msg){
        cancelProgress();
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(msg);
        mProgressDialog.setCancelable(true);
        mProgressDialog.show();
    }

    private void cancelProgress(){
        if(mProgressDialog != null && mProgressDialog.isShowing()){
            mProgressDialog.dismiss();
        }
    }

    public static class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        private final ItemListActivity mParentActivity;
        private final List<ContactInfo> mValues;
        private final boolean mTwoPane;
        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ContactInfo item = (ContactInfo) view.getTag();
                if (mTwoPane) {
                    Bundle arguments = new Bundle();
                    arguments.putSerializable(ItemDetailFragment.ARG_ITEM_ID, item);
                    ItemDetailFragment fragment = new ItemDetailFragment();
                    fragment.setArguments(arguments);
                    mParentActivity.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.item_detail_container, fragment)
                            .commit();
                } else {
                    Context context = view.getContext();
                    Intent intent = new Intent(context, ItemDetailActivity.class);
                    intent.putExtra(ItemDetailFragment.ARG_ITEM_ID, item);

                    context.startActivity(intent);
                }
            }
        };

        SimpleItemRecyclerViewAdapter(ItemListActivity parent,
                                      List<ContactInfo> items,
                                      boolean twoPane) {
            mValues = items;
            mParentActivity = parent;
            mTwoPane = twoPane;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mContentView.setText(mValues.get(position).getName());
            holder.mContentId.setText(String.valueOf(position + 1));

            holder.itemView.setTag(mValues.get(position));
            holder.itemView.setOnClickListener(mOnClickListener);
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView mContentId;
            final TextView mContentView;

            ViewHolder(View view) {
                super(view);
                mContentId = (TextView) view.findViewById(R.id.id_text);
                mContentView = (TextView) view.findViewById(R.id.content);
            }
        }
    }


}
