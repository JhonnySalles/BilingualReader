package br.com.ebook.foobnix.ui2.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Pair;
import android.view.View;
import android.widget.ProgressBar;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import br.com.ebook.foobnix.android.utils.AsyncTasks;
import br.com.ebook.foobnix.pdf.search.activity.msg.OpenDirMessage;
import br.com.ebook.foobnix.android.utils.Dips;
import br.com.ebook.foobnix.android.utils.LOG;
import br.com.ebook.foobnix.pdf.info.wrapper.AppState;
import br.com.ebook.foobnix.sys.TempHolder;

public abstract class UIFragment<T> extends Fragment {
    public static String INTENT_TINT_CHANGE = "INTENT_TINT_CHANGE";

    Handler handler;
    protected volatile ProgressBar progressBar;
    protected RecyclerView recyclerView;

    public abstract Pair<Integer, Integer> getNameAndIconRes();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        handler = new Handler();

    }

    @Override
    public void onDetach() {
        super.onDetach();
        handler.removeCallbacksAndMessages(null);
    }

    public boolean isBackPressed() {
        return false;
    }

    public abstract void notifyFragment();

    public abstract void resetFragment();

    int listHash = 0;

    public final void onSelectFragment() {
        if (getActivity() == null) {
            return;
        }
        if (listHash != TempHolder.listHash) {
            LOG.d("TempHolder.listHash", listHash, TempHolder.listHash);
            resetFragment();
            listHash = TempHolder.listHash;
        } else {
            notifyFragment();
        }
    }

    public List<T> prepareDataInBackground() {
        return null;
    }

    public void populateDataInUI(List<T> items) {

    }

    public void onTintChanged() {

    }

    public void sendNotifyTintChanged() {
        Intent itent = new Intent(INTENT_TINT_CHANGE);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(itent);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        TempHolder.listHash++;
        onSelectFragment();
    }

    @Subscribe
    public void onReviceOpenDir(OpenDirMessage msg) {
        onReviceOpenDir(msg.getPath());
    }

    public void onReviceOpenDir(String path) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (recyclerView != null) {
            try {
                recyclerView.setAdapter(null);
            } catch (Exception e) {
                LOG.e(e);
            }
        }
    }

    public void onTextRecive(String txt) {

    }

    public boolean isInProgress() {
        return progressBar != null && progressBar.getVisibility() == View.VISIBLE;
    }

    AsyncTask<Object, Object, List<T>> execute;

    public void populate() {

        if (isInProgress()) {
            AsyncTasks.toastPleaseWait(getActivity());
            return;
        }
        if (AsyncTasks.isFinished(execute)) {
            execute = new AsyncTask<Object, Object, List<T>>() {
                @Override
                protected List<T> doInBackground(Object... params) {
                    try {
                        LOG.d("UIFragment", "prepareDataInBackground");
                        return prepareDataInBackground();
                    } catch (Exception e) {
                        LOG.e(e);
                        return new ArrayList<T>();
                    }
                }

                @Override
                protected void onPreExecute() {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.VISIBLE);
                    }
                }

                ;

                @Override
                protected void onPostExecute(List<T> result) {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    if (getActivity() != null) {
                        try {
                            populateDataInUI(result);
                        } catch (Exception e) {
                            LOG.e(e);
                        }
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            LOG.d("SKIP task");
        }
    }

    public boolean onKeyDown(int keyCode) {
        if (recyclerView == null) {
            return false;
        }
        View childAt = recyclerView.getChildAt(0);
        if (childAt == null) {
            return false;
        }
        int size = childAt.getHeight() + childAt.getPaddingTop() + Dips.dpToPx(2);

        if (AppState.get().getNextKeys().contains(keyCode)) {
            recyclerView.scrollBy(0, size);
            return true;

        }
        if (AppState.get().getPrevKeys().contains(keyCode)) {
            recyclerView.scrollBy(0, size * -1);
            return true;
        }
        return false;
    }

}
