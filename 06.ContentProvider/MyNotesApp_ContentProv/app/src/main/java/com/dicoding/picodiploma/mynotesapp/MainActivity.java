package com.dicoding.picodiploma.mynotesapp;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;

import com.dicoding.picodiploma.mynotesapp.adapter.NoteAdapter;
import com.dicoding.picodiploma.mynotesapp.db.NoteHelper;
import com.dicoding.picodiploma.mynotesapp.entity.Note;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import static android.provider.BaseColumns._ID;
import static com.dicoding.picodiploma.mynotesapp.FormAddUpdateActivity.REQUEST_UPDATE;
import static com.dicoding.picodiploma.mynotesapp.db.DatabaseContract.NoteColumns.CONTENT_URI;
import static com.dicoding.picodiploma.mynotesapp.db.DatabaseContract.NoteColumns.DATE;
import static com.dicoding.picodiploma.mynotesapp.db.DatabaseContract.NoteColumns.DESCRIPTION;
import static com.dicoding.picodiploma.mynotesapp.db.DatabaseContract.NoteColumns.TITLE;


public class MainActivity extends AppCompatActivity
        implements View.OnClickListener,LoadNotesCallback {
    private RecyclerView rvNotes;
    private ProgressBar progressBar;

    private ArrayList<Note> listnotes = new ArrayList<Note>();
    private NoteAdapter adapter;
    private NoteHelper noteHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle("Notes");

        rvNotes = findViewById(R.id.rv_notes);
        rvNotes.setLayoutManager(new LinearLayoutManager(this));
        rvNotes.setHasFixedSize(true);
        noteHelper = NoteHelper.getInstance(getApplicationContext());

        noteHelper.open();

        progressBar = findViewById(R.id.progressbar);
        FloatingActionButton fabAdd = findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(this);

        adapter = new NoteAdapter(this);
        rvNotes.setAdapter(adapter);
        adapter.setListNotes(listnotes);
        new LoadNoteAsync(this,this).execute();

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.fab_add) {
            Intent intent = new Intent(MainActivity.this, FormAddUpdateActivity.class);
            startActivityForResult(intent, FormAddUpdateActivity.REQUEST_ADD);
        }
    }
    private ArrayList<Note> mapping(Cursor notes){
        ArrayList<Note> noteList = new ArrayList<Note>();
        while (notes.moveToNext()){
            int id = notes.getInt(notes.getColumnIndexOrThrow(_ID));
            String title = notes.getString(notes.getColumnIndexOrThrow(TITLE));
            String description = notes.getString(notes.getColumnIndexOrThrow(DESCRIPTION));
            String date = notes.getString(notes.getColumnIndexOrThrow(DATE));
            noteList.add(new Note(id,title,description,date));
        }

        return noteList;
    }

    @Override
    public void preExecute() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void postExecute(Cursor notes) {
        Cursor list = notes;
        listnotes = mapping(notes);
        progressBar.setVisibility(View.INVISIBLE);
        adapter.setListNotes(listnotes);
        if (list.getCount() == 0) {
            showSnackbarMessage("Tidak ada data saat ini");
        }
    }

    private static class LoadNoteAsync extends AsyncTask<Void, Void, Cursor> {

        private final WeakReference<Activity> activtiy;
        private final WeakReference<LoadNotesCallback> weakCallback;

        private LoadNoteAsync(Activity activity,LoadNotesCallback callback) {
            activtiy = new WeakReference<>(activity);
            weakCallback = new WeakReference<>(callback);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            weakCallback.get().preExecute();
        }

        @Override
        protected Cursor doInBackground(Void... voids) {
            return activtiy.get().getContentResolver().query(CONTENT_URI, null, null, null, null);
        }

        @Override
        protected void onPostExecute(Cursor notes) {
            super.onPostExecute(notes);
            weakCallback.get().postExecute(notes);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Akan dipanggil jika request codenya ADD
        if (requestCode == FormAddUpdateActivity.REQUEST_ADD) {
            if (resultCode == FormAddUpdateActivity.RESULT_ADD) {
                Note note = data.getParcelableExtra(FormAddUpdateActivity.EXTRA_NOTE);
                adapter.addItem(note);
                rvNotes.smoothScrollToPosition(adapter.getItemCount() - 1);
                showSnackbarMessage("Satu item berhasil ditambahkan");
            }
        }
        // Update dan Delete memiliki request code sama akan tetapi result codenya berbeda
        else if (requestCode == REQUEST_UPDATE) {
            /*
            Akan dipanggil jika result codenya UPDATE
            Semua data di load kembali dari awal
            */
            if (resultCode == FormAddUpdateActivity.RESULT_UPDATE) {
                Note note = data.getParcelableExtra(FormAddUpdateActivity.EXTRA_NOTE);
                int position = data.getIntExtra(FormAddUpdateActivity.EXTRA_POSITION, 0);
                adapter.updateItem(position, note);
                rvNotes.smoothScrollToPosition(position);
                showSnackbarMessage("Satu item berhasil diubah");
            }
            /*
            Akan dipanggil jika result codenya DELETE
            Delete akan menghapus data dari list berdasarkan dari position
            Dikarenakan menggunakan cursor maka data akan di load kembali
            */
            else if (resultCode == FormAddUpdateActivity.RESULT_DELETE) {
                int position = data.getIntExtra(FormAddUpdateActivity.EXTRA_POSITION, 0);
                adapter.removeItem(position);
                showSnackbarMessage("Satu item berhasil dihapus");
            }
        }
    }

    /**
     * Tampilkan snackbar
     *
     * @param message inputan message
     */
    private void showSnackbarMessage(String message) {
        Snackbar.make(rvNotes, message, Snackbar.LENGTH_SHORT).show();
    }
}