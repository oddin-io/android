package br.com.sienaidea.oddin.view;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import br.com.sienaidea.oddin.R;
import br.com.sienaidea.oddin.adapter.AdapterViewPager;
import br.com.sienaidea.oddin.fragment.ParticipantsFragment;
import br.com.sienaidea.oddin.fragment.ParticipantsOfflineFragment;
import br.com.sienaidea.oddin.fragment.ParticipantsOnlineFragment;
import br.com.sienaidea.oddin.retrofitModel.Enroll;
import br.com.sienaidea.oddin.retrofitModel.Instruction;
import br.com.sienaidea.oddin.retrofitModel.Person;
import br.com.sienaidea.oddin.server.HttpApi;
import br.com.sienaidea.oddin.server.Preference;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ParticipantsActivity extends AppCompatActivity {
    private static String TAB_POSITION = "TAB_POSITION";

    private FragmentManager fragmentManager = getSupportFragmentManager();
    private ParticipantsFragment mParticipantsFragment;
    private ParticipantsOnlineFragment mParticipantsOnlineFragment;
    private ParticipantsOfflineFragment mParticipantsOfflineFragment;

    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private AdapterViewPager mAdapterViewPager;
    private int mSelectedTabPosition;

    private Instruction mInstruction;
    private List<Enroll> mList = new ArrayList<>();
    private List<Person> mPersonList = new ArrayList<>();

    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_participants);

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

        mTabLayout = (TabLayout) findViewById(R.id.tab_participants);
        mViewPager = (ViewPager) findViewById(R.id.vp_participants);

        if (savedInstanceState != null) {
            mList = savedInstanceState.getParcelableArrayList(Person.TAG);
            mInstruction = savedInstanceState.getParcelable(Instruction.TAG);
            mViewPager.setCurrentItem(savedInstanceState.getInt(TAB_POSITION));
            setupViewPager(mViewPager);
        } else {
            if (getIntent() != null && getIntent().getExtras() != null && getIntent().getParcelableExtra(Instruction.TAG) != null) {
                mInstruction = getIntent().getParcelableExtra(Instruction.TAG);
                getParticipants();
            } else {
                Toast.makeText(this, R.string.toast_fails_to_start, Toast.LENGTH_LONG).show();
                finish();
            }
        }

        Toolbar mToolbar = (Toolbar) findViewById(R.id.tb_participants);
        mToolbar.setTitle(mInstruction.getLecture().getName());
        mToolbar.setSubtitle(R.string.toolbar_users);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupViewPager(final ViewPager viewPager) {
        mAdapterViewPager = new AdapterViewPager(fragmentManager);

        mParticipantsFragment = ParticipantsFragment.newInstance(getListParticipants());
        mParticipantsOnlineFragment = ParticipantsOnlineFragment.newInstance(getListParticipantsOnline());
        mParticipantsOfflineFragment = ParticipantsOfflineFragment.newInstance(getListParticipantsOffline());

        mAdapterViewPager.addFragment(mParticipantsFragment, getResources().getString(R.string.ALL));
        mAdapterViewPager.addFragment(mParticipantsOnlineFragment, getResources().getString(R.string.ONLINE));
        mAdapterViewPager.addFragment(mParticipantsOfflineFragment, getResources().getString(R.string.OFFLINE));

        viewPager.setAdapter(mAdapterViewPager);

        mTabLayout.setTabTextColors(ContextCompat.getColorStateList(this, R.xml.selector));
        mTabLayout.setupWithViewPager(viewPager);

        mProgressBar.setVisibility(View.GONE);
    }

    public void getParticipants() {
        // Retrofit setup
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(HttpApi.API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Service setup
        HttpApi.HttpBinService service = retrofit.create(HttpApi.HttpBinService.class);

        Call<List<Enroll>> call = service.Participants(getToken(getApplicationContext()), mInstruction.getId());

        // Asynchronously execute HTTP request
        call.enqueue(new Callback<List<Enroll>>() {
            /**
             * onResponse is called when any kind of response has been received.
             */
            @Override
            public void onResponse(Call<List<Enroll>> call, Response<List<Enroll>> response) {
                // isSuccess is true if response code => 200 and <= 300
                if (response.isSuccessful()) {
                    onRequestSuccess(response.body());
                } else onRequestFailure();
            }

            /**
             * onFailure gets called when the HTTP request didn't get through.
             * For instance if the URL is invalid / host not reachable
             */
            @Override
            public void onFailure(Call<List<Enroll>> call, Throwable t) {
                onRequestFailure();
            }
        });
    }

    private List<Person> getListParticipants() {
        return mPersonList;
    }

    private List<Person> getListParticipantsOnline() {
        List<Person> listAux = new ArrayList<>();

        for (Person person : mPersonList) {
            if (person.isOnline()) {
                listAux.add(person);
            }
        }

        return listAux;
    }

    private List<Person> getListParticipantsOffline() {
        List<Person> listAux = new ArrayList<>();

        for (Person person : mPersonList) {
            if (!person.isOnline()) {
                listAux.add(person);
            }
        }

        return listAux;
    }

    private String getToken(Context context) {
        Preference preference = new Preference();
        return preference.getToken(context);
    }

    private void onRequestSuccess(List<Enroll> list) {
        mList.clear();
        mPersonList.clear();
        mList = list;

        for (Enroll enroll:mList){
            mPersonList.add(enroll.getPerson());
        }

        mSelectedTabPosition = mTabLayout.getSelectedTabPosition();
        setupViewPager(mViewPager);
        mViewPager.setCurrentItem(mSelectedTabPosition);
    }

    private void onRequestFailure() {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(Instruction.TAG, mInstruction);
        outState.putParcelableArrayList(Person.TAG, (ArrayList<Person>) mPersonList);
        outState.putInt(TAB_POSITION, mTabLayout.getSelectedTabPosition());
        super.onSaveInstanceState(outState);
    }
}
