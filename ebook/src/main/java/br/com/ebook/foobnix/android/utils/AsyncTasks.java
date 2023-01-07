package br.com.ebook.foobnix.android.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import br.com.ebook.R;


public class AsyncTasks {

    public static boolean isFinished(AsyncTask task) {
        return task == null || task.getStatus() == AsyncTask.Status.FINISHED;
    }

    public static void toastPleaseWait(Context c) {
        if (c != null) {
            Toast.makeText(c, R.string.please_wait, Toast.LENGTH_SHORT).show();
        }
    }

}
