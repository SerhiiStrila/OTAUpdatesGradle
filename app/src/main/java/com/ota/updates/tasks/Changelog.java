package com.ota.updates.tasks;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.ota.updates.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import in.uncod.android.bypass.Bypass;

public class Changelog extends AsyncTask<Void, Void, String> {

    private static final String CHANGELOG = "Changelog.md";
    private static final String TAG = "AboutActivity.Changelog";
    private ProgressDialog mLoadingDialog;
    private File mChangelogFile;
    private Context mContext;
    private Activity mActivity;
    private String mTitle;
    private String mChangelog;
    private boolean mRemote;

    public Changelog(Activity activity,
            Context context,
            String dialogTitle,
            String changelog,
            boolean remote) {
        mContext = context;
        mActivity = activity;
        mChangelog = changelog;
        mTitle = dialogTitle;
        mRemote = remote;
    }

    @Override
    protected String doInBackground(Void... params) {
        if (mRemote) {
            try {
                InputStream input = null;

                URL url = new URL(mChangelog);
                URLConnection connection = url.openConnection();
                connection.connect();
                // download the file
                input = new BufferedInputStream(url.openStream());

                OutputStream output = mContext.openFileOutput(
                        CHANGELOG, Context.MODE_PRIVATE);

                byte data[] = new byte[1024];
                int count;
                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();

                // file finished downloading, parse it!

            } catch (Exception e) {
                Log.d(TAG, "Exception: " + e.getMessage());
            }

            InputStreamReader inputReader = null;
            String text = null;

            try {
                StringBuilder data = new StringBuilder();
                char tmp[] = new char[2048];
                int numRead;
                inputReader = new FileReader(mChangelogFile);
                while ((numRead = inputReader.read(tmp)) >= 0) {
                    data.append(tmp, 0, numRead);
                }
                text = data.toString();
            } catch (IOException e) {
                text = mContext.getString(R.string.changelog_error);
            } finally {
                try {
                    if (inputReader != null) {
                        inputReader.close();
                    }
                } catch (IOException e) {
                }
            }
            return text;
        } else {
            return null;
        }
    }

    @Override
    protected void onPreExecute() {

        // Show a loading/progress dialog while the search is being performed
        mLoadingDialog = new ProgressDialog(mContext);
        mLoadingDialog.setIndeterminate(true);
        mLoadingDialog.setCancelable(false);
        mLoadingDialog.setMessage(mContext.getResources().getString(R.string.loading));
        mLoadingDialog.show();

        // Delete any existing manifest file before we attempt to download a new one
        mChangelogFile = new File(mContext.getFilesDir().getPath(), CHANGELOG);
        if (mChangelogFile.exists()) {
            mChangelogFile.delete();
        }
    }

    @Override
    protected void onPostExecute(String result) {
        mLoadingDialog.cancel();
        showChangelogDialog(mRemote ? result : mChangelog);
        super.onPostExecute(result);
    }

    private void showChangelogDialog(String changelogText) {
        View view = mActivity.getLayoutInflater().inflate(R.layout.ota_changelog_layout, null);
        TextView changelog = view.findViewById(R.id.title);

        Bypass bypass = new Bypass(mContext, new Bypass.Options());
        CharSequence string = bypass.markdownToSpannable(changelogText, changelog, null);
        changelog.setText(string);

        Builder dialog = new Builder(mContext);
        dialog.setTitle(mTitle);
        dialog.setView(view);
        dialog.setPositiveButton(R.string.done, null);
        dialog.show();
    }

}