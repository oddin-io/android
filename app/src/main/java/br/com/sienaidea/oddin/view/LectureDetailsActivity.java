package br.com.sienaidea.oddin.view;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

import net.opacapp.multilinecollapsingtoolbar.CollapsingToolbarLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import br.com.sienaidea.oddin.R;
import br.com.sienaidea.oddin.fragment.MaterialDisciplineFragment;
import br.com.sienaidea.oddin.model.Constants;
import br.com.sienaidea.oddin.model.Discipline;
import br.com.sienaidea.oddin.model.Material;
import br.com.sienaidea.oddin.retrofitModel.Instruction;
import br.com.sienaidea.oddin.retrofitModel.Profile;
import br.com.sienaidea.oddin.server.BossClient;
import br.com.sienaidea.oddin.server.HttpApi;
import br.com.sienaidea.oddin.util.CookieUtil;
import br.com.sienaidea.oddin.util.FileUtils;
import cz.msebera.android.httpclient.Header;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import static br.com.sienaidea.oddin.R.string.toast_fails_to_start;

public class LectureDetailsActivity extends AppCompatActivity {
    private static String URL_GET_MATERIAL;
    private static int REQUEST_CODE_MATERIAL = 2;
    private static final int REQUEST_PERMISSIONS_UPLOAD = 21;
    private static final int REQUEST_PERMISSIONS_DOWNLOAD = 12;

    private List<Material> mList = new ArrayList<>();
    private Material material;
    //private Discipline mDiscipline;

    private File mTempFile;
    private byte[] mBytes;
    private String mFileName, mimeType;
    private Uri returnUri;
    private FragmentManager fragmentManager = getSupportFragmentManager();
    private MaterialDisciplineFragment mMaterialDisciplineFragment;

    private int mPositionFragment;
    private Material mMaterialFragment;

    //new
    private FloatingActionButton mFab;
    private Instruction mInstruction;
    private Profile mProfile;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecture_details);

        mFab = (FloatingActionButton) findViewById(R.id.fab);

        if (savedInstanceState != null) {
            mList = savedInstanceState.getParcelableArrayList("mList");
            mInstruction = savedInstanceState.getParcelable(Instruction.TAG);
            mProfile = savedInstanceState.getParcelable(Profile.TAG);
        } else {
            if (getIntent() != null && getIntent().getExtras() != null && getIntent().getParcelableExtra(Instruction.TAG) != null) {
                mInstruction = getIntent().getParcelableExtra(Instruction.TAG);
                mProfile = getIntent().getParcelableExtra(Profile.TAG);
                setupFab();
                //URL_GET_MATERIAL = "controller/instruction/" + mDiscipline.getInstruction_id() + "/material";
                getMaterial();

            } else {
                Toast.makeText(this, toast_fails_to_start, Toast.LENGTH_LONG).show();
                finish();
            }
        }

        Toolbar mToolbar = (Toolbar) findViewById(R.id.tb_discipline_details);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        CollapsingToolbarLayout mCollapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar_layout);
        mCollapsingToolbarLayout.setTitle(mInstruction.getLecture().getName());
    }

    private void setupFab() {
        if (mProfile.getProfile() == Constants.INSTRUCTOR) {
            mFab.setVisibility(View.VISIBLE);
            mFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //se uma das duas permissões não estiverem liberadas
                    if (ContextCompat.checkSelfPermission(LectureDetailsActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(LectureDetailsActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                        //verifica se já foi recusado a permissão de escrita
                        if (ActivityCompat.shouldShowRequestPermissionRationale(LectureDetailsActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            callDialog("É preciso a permission WRITE_EXTERNAL_STORAGE para SALVAR o conteudo em seu aparelho.", new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS_UPLOAD);
                            return;
                        }

                        //verifica se já foi recusado a permissão de leitura
                        if (ActivityCompat.shouldShowRequestPermissionRationale(LectureDetailsActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                            callDialog("É preciso a permission READ_EXTERNAL_STORAGE para LER o conteudo em seu aparelho.", new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS_UPLOAD);
                            return;
                        }

                        //caso nenhuma das duas permissões nunca estiverem sido negadas, será solicitado aqui
                        ActivityCompat.requestPermissions(LectureDetailsActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS_UPLOAD);

                    } else {
                        openFileManager();
                    }

                }
            });
        }
    }

    private void openFileManager() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE_MATERIAL);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.i("TAG", "onRequestPermissionsResult");
        switch (requestCode) {
            case REQUEST_PERMISSIONS_DOWNLOAD:
                for (int i = 0; i < permissions.length; i++) {

                    if (permissions[i].equalsIgnoreCase(Manifest.permission.READ_EXTERNAL_STORAGE) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        getMaterialContent(mPositionFragment, mMaterialFragment);
                        return;
                    } else if (permissions[i].equalsIgnoreCase(Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        getMaterialContent(mPositionFragment, mMaterialFragment);
                        return;
                    }

                }
                break;
            case REQUEST_PERMISSIONS_UPLOAD:
                for (int i = 0; i < permissions.length; i++) {

                    if (permissions[i].equalsIgnoreCase(Manifest.permission.READ_EXTERNAL_STORAGE) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        openFileManager();
                        return;
                    } else if (permissions[i].equalsIgnoreCase(Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        openFileManager();
                        return;
                    }

                }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_MATERIAL) {
                /*
                * Get the file's content URI from the incoming Intent,
                * then query the server app to get the file's display name
                * and size.
                */
                returnUri = result.getData();

                attemptUploadMateril();
            }
        }
    }

    private boolean checkUriPermissionGranted(Uri uri) {
        if ((checkUriPermission(uri, 0, 0, Intent.FLAG_GRANT_READ_URI_PERMISSION) == PackageManager.PERMISSION_GRANTED) &&
                (checkUriPermission(uri, 0, 0, Intent.FLAG_GRANT_WRITE_URI_PERMISSION) == PackageManager.PERMISSION_GRANTED)) {

            attemptUploadMateril();
            return true;

        } else
            return false;
    }

    private void attemptUploadMateril() {

        final EditText inputName = new EditText(LectureDetailsActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        inputName.setLayoutParams(lp);

        inputName.setText(FileUtils.getFileName(getApplicationContext(), returnUri));

        AlertDialog.Builder builder = new AlertDialog.Builder(LectureDetailsActivity.this, R.style.AppCompatAlertDialogStyle);
        builder.setTitle("Novo Material");
        builder.setView(inputName);
        builder.setNegativeButton("CANCELAR", null);
        builder.setPositiveButton("ENVIAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!TextUtils.isEmpty(inputName.getText())) {
                    mFileName = inputName.getText().toString();
                }
                uploadFile();
            }
        });
        builder.show();
    }

    private void uploadFile() {
//        MaterialDAO materialDAO = new MaterialDAO(getApplicationContext(), mInstruction, returnUri, mFileName);
//        materialDAO.createInstructionMaterial();
    }

//    private void uploadtesteFile() {
//
//        mTempFile = createTempFile();
//
//        if (mTempFile != null) {
//            // Retrofit setup
//            Retrofit retrofit = new Retrofit.Builder()
//                    .baseUrl(HttpApi.API_URL)
//                    .build();
//            //.addConverterFactory(GsonConverterFactory.create())
//
//
//            // Service setup
//            HttpApi.HttpBinService service = retrofit.create(HttpApi.HttpBinService.class);
//
//            MediaType MEDIA_TYPE = MediaType.parse(mimeType);
//            //File file = new File(returnUri.getPath());
//            //RequestBody requestBody = RequestBody.create(MEDIA_TYPE, file);
//
//            RequestBody requestBody = RequestBody.create(MEDIA_TYPE, mTempFile);
//
//            // MultipartBody.Part is used to send also the actual file name
//            MultipartBody.Part body = MultipartBody.Part.createFormData("file", mTempFile.getName(), requestBody);
//
//
//            Call<Void> call = service.postMaterialDisciplineTeste(CookieUtil.getCookie(getApplicationContext()),
//                    String.valueOf(mDiscipline.getInstruction_id()),
//                    body);
//
//            // Asynchronously execute HTTP request
//            call.enqueue(new Callback<Void>() {
//                /**
//                 * onResponse is called when any kind of response has been received.
//                 */
//                @Override
//                public void onResponse(Call<Void> call, Response<Void> response) {
//                    // http response status code + headers
//                    //System.out.println("Response status code: " + response.code());
//
//                    // isSuccess is true if response code => 200 and <= 300
//                    if (!response.isSuccessful()) {
//                        // print response body if unsuccessful
//                        Toast.makeText(getApplicationContext(), "Não foi possível completar a requisição no servidor, tente novamente!", Toast.LENGTH_LONG).show();
//                        Log.d("RESPONSE", response.message());
//                        Log.d("RESPONSE", String.valueOf(response.code()));
//                        Log.d("RESPONSE", response.errorBody().toString());
//                        return;
//                    }
//                    Toast.makeText(getApplicationContext(), "Enviado!", Toast.LENGTH_LONG).show();
//                    loadMaterial();//SUBSTITUIT PELO RESULT
//                }
//
//                /**
//                 * onFailure gets called when the HTTP request didn't get through.
//                 * For instance if the URL is invalid / host not reachable
//                 */
//                @Override
//                public void onFailure(Call<Void> call, Throwable t) {
//                    Toast.makeText(getApplicationContext(), "Não foi possível enviar ao servidor!", Toast.LENGTH_LONG).show();
//                }
//            });
//        } else {
//            Toast.makeText(getApplicationContext(), "Não foi possivel gerar o arquivo temporário", Toast.LENGTH_LONG).show();
//        }
//    }

    private File createTempFile() {

        File root = Environment.getExternalStorageDirectory();

        File dirOddin = new File(root.getAbsolutePath() + "/Oddin");
        if (!dirOddin.exists()) {
            dirOddin.mkdir();
        }

        File dirDiscipline = new File(dirOddin.getAbsolutePath() + "/temporarios");
        if (!dirDiscipline.exists()) {
            dirDiscipline.mkdir();
        }

        final File file = new File(dirDiscipline.getAbsolutePath(), mFileName);
        try {

            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(mBytes);
            fileOutputStream.close();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return file;

        /*
        try {

            File tempFile = new File(getCacheDir(), mFileName);
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(mBytes);
            return tempFile;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        */


    }


    public void loadMaterial() {
        BossClient.get(URL_GET_MATERIAL, null, CookieUtil.getCookie(this), new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                try {
                    JSONArray materials = response.getJSONArray("materials");
                    Log.d("MATERIALS", materials.toString());

                    mList.clear();

                    for (int i = 0; i < materials.length(); i++) {
                        material = new Material();

                        material.setId(materials.getJSONObject(i).getInt("id"));
                        material.setName(materials.getJSONObject(i).getString("name"));
                        material.setMime(materials.getJSONObject(i).getString("mime"));

                        addListItem(material);
                    }

                    mMaterialDisciplineFragment = (MaterialDisciplineFragment) fragmentManager.findFragmentByTag(MaterialDisciplineFragment.TAG);
                    if (mMaterialDisciplineFragment != null) {
                        mMaterialDisciplineFragment.notifyDataSetChanged();
                    } else {
                        mMaterialDisciplineFragment = MaterialDisciplineFragment.newInstance(getListMaterial());
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        fragmentTransaction.add(R.id.rl_fragment_discipline_details, mMaterialDisciplineFragment, MaterialDisciplineFragment.TAG);
                        fragmentTransaction.commit();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                //mMaterialDisciplineFragment.swipeRefreshStop();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
            }
        });
    }

    public void getMaterial() {
        // TODO: 03/08/2016
    }

    public void attemptGetMaterialContent(int position, Material material) {

        mPositionFragment = position;
        mMaterialFragment = material;

        //se uma das duas permissões não estiverem liberadas
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            //verifica se já foi recusado a permissão de escrita
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                callDialog("É preciso permissão para SALVAR o conteudo em seu aparelho.", new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS_DOWNLOAD);
                return;
            }

            //verifica se já foi recusado a permissão de leitura
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                callDialog("É preciso permissão para LER o conteudo em seu aparelho.", new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS_DOWNLOAD);
                return;
            }

            //caso nenhuma das duas permissões nunca estiverem sido negadas, será solicitado aqui
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS_DOWNLOAD);

        } else {
            //e por fim, caso já tenha permiçoes, faça download
            getMaterialContent(mPositionFragment, mMaterialFragment);
        }

    }

    private void getMaterialContent(final int position, final Material material) {
        BossClient.get(URL_GET_MATERIAL + "/" + material.getId(), null, CookieUtil.getCookie(getApplicationContext()), new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                if (FileUtils.isExternalStorageWritable()) {
                    File root = Environment.getExternalStorageDirectory();

                    File dirOddin = new File(root.getAbsolutePath() + "/Oddin");
                    if (!dirOddin.exists()) {
                        dirOddin.mkdir();
                    }

                    File dirDiscipline = new File(dirOddin.getAbsolutePath() + "/" + mInstruction.getLecture().getName().trim());
                    if (!dirDiscipline.exists()) {
                        dirDiscipline.mkdir();
                    }

                    final File file = new File(dirDiscipline.getAbsolutePath(), material.getName().trim());
                    try {

                        FileOutputStream fileOutputStream = new FileOutputStream(file);
                        fileOutputStream.write(responseBody);
                        fileOutputStream.close();

                        AlertDialog.Builder builder =
                                new AlertDialog.Builder(LectureDetailsActivity.this, R.style.AppCompatAlertDialogStyle);
                        builder.setMessage("Material salvo em: " + file.getAbsolutePath());
                        builder.setPositiveButton("OK", null);
                        builder.setNegativeButton("ABRIR", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent newIntent = new Intent();
                                newIntent.setDataAndType(Uri.parse("file://" + file.getPath()), material.getMime());
                                newIntent.setAction(Intent.ACTION_VIEW);
                                try {
                                    startActivity(newIntent);
                                } catch (android.content.ActivityNotFoundException e) {
                                    Toast.makeText(getApplicationContext(), "Nenhum manipulador para este tipo de arquivo.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        builder.show();

                        material.setDownloaded(true);
                        mMaterialDisciplineFragment.downloadFinished(position, Uri.parse("file://" + file.getPath()));

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Não foi possível salvar o arquivo.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(getApplicationContext(), "Não foi possível fazer a requisição no servidor, tente novamente.", Toast.LENGTH_LONG).show();
            }
        });

    }

    private void callDialog(String message, final String[] permissions, final int requestCode) {

        AlertDialog.Builder builder = new AlertDialog.Builder(LectureDetailsActivity.this, R.style.AppCompatAlertDialogStyle);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.dialog_confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ActivityCompat.requestPermissions(LectureDetailsActivity.this, permissions, requestCode);
            }
        });
        builder.setNegativeButton(R.string.dialog_cancel, null);
        builder.show();

    }

    private void addListItem(Material material) {
        mList.add(material);
    }

    public List<Material> getListMaterial() {
        return mList;
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
        outState.putParcelableArrayList("mList", (ArrayList<Material>) mList);
        outState.putParcelable(Instruction.TAG, mInstruction);
        super.onSaveInstanceState(outState);
    }
}
