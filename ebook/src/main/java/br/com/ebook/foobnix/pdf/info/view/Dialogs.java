package br.com.ebook.foobnix.pdf.info.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Handler;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import br.com.ebook.foobnix.android.utils.Dips;
import br.com.ebook.foobnix.android.utils.IntegerResponse;
import br.com.ebook.foobnix.android.utils.LOG;
import br.com.ebook.foobnix.android.utils.TxtUtils;
import br.com.ebook.foobnix.android.utils.Vibro;
import br.com.ebook.foobnix.pdf.info.TintUtil;
import br.com.ebook.foobnix.pdf.info.wrapper.AppState;
import br.com.ebook.foobnix.pdf.info.wrapper.DocumentController;
import br.com.ebook.R;
import br.com.ebook.foobnix.sys.TempHolder;

public class Dialogs {

    public static AlertDialog loadingBook(Context c, final Runnable onCancel) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(c);
        View view = LayoutInflater.from(c).inflate(R.layout.dialog_loading_book, null, false);
        final TextView text = (TextView) view.findViewById(R.id.text1);

        ProgressBar pr = (ProgressBar) view.findViewById(R.id.progressBar1);
        TintUtil.setDrawableTint(pr.getIndeterminateDrawable().getCurrent(), TintUtil.color);

        ImageView image = (ImageView) view.findViewById(R.id.onCancel);
        TintUtil.setTintImageNoAlpha(image, TintUtil.color);
        image.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                LOG.d("loadingBook Cancel");
                onCancel.run();
            }
        });

        builder.setView(view);
        builder.setCancelable(false);

        AlertDialog dialog = builder.show();
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        return dialog;

    }

    public static void showDeltaPage(final FrameLayout anchor, final DocumentController controller, final int pageNumber, final Runnable reloadUI) {
        Vibro.vibrate();
        String txt = controller.getString(R.string.set_the_current_page_number);

        final AlertDialog.Builder builder = new AlertDialog.Builder(anchor.getContext());
        builder.setMessage(txt);

        final EditText edit = new EditText(anchor.getContext());
        edit.setInputType(InputType.TYPE_CLASS_NUMBER);
        edit.setMaxWidth(Dips.dpToPx(100));
        edit.setText("" + pageNumber);

        builder.setView(edit);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                try {
                    TempHolder.get().pageDelta = Integer.parseInt(edit.getText().toString()) - controller.getCurentPageFirst1();
                } catch (Exception e) {
                    TempHolder.get().pageDelta = 0;
                }
                reloadUI.run();
            }
        });
        builder.setNeutralButton(R.string.restore_defaults_short, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int id) {

                AlertDialogs.showOkDialog(controller.getActivity(), controller.getString(R.string.restore_defaults_full), new Runnable() {

                    @Override
                    public void run() {
                        TempHolder.get().pageDelta = 0;
                        reloadUI.run();
                    }
                });

            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int id) {
            }
        });
        AlertDialog create = builder.create();
        create.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                //Keyboards.hideNavigation(controller.getActivity());
            }
        });
        create.show();
    }

    public static void showContrastDialogByUrl(final Activity c, final Runnable action) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setCancelable(true);
        builder.setTitle(R.string.contrast_and_brightness);

        LinearLayout l = getBCView(c, action);
        builder.setView(l);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                if (action != null) {
                    action.run();
                }

            }
        });
        builder.show();
    }

    public static LinearLayout getBCView(final Activity c, final Runnable action) {
        LinearLayout l = new LinearLayout(c);
        l.setPadding(Dips.dpToPx(5), Dips.dpToPx(5), Dips.dpToPx(5), Dips.dpToPx(5));
        l.setOrientation(LinearLayout.VERTICAL);

        final Handler handler = new Handler();

        final Runnable actionWrapper = new Runnable() {

            @Override
            public void run() {
                handler.removeCallbacks(action);
                handler.postDelayed(action, 100);
            }
        };

        final CheckBox isEnableBC = new CheckBox(c);
        isEnableBC.setText(R.string.enable_contrast_and_brightness);
        isEnableBC.setChecked(AppState.get().isEnableBC);
        isEnableBC.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().isEnableBC = isChecked;
                actionWrapper.run();
            }
        });

        final CustomSeek contrastSeek = new CustomSeek(c);
        contrastSeek.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0));
        contrastSeek.init(0, 200, AppState.get().contrastImage);
        contrastSeek.setOnSeekChanged(new IntegerResponse() {

            @Override
            public boolean onResultRecive(int result) {
                AppState.get().contrastImage = result;
                isEnableBC.setChecked(true);
                actionWrapper.run();
                return false;
            }
        });

        final CustomSeek brightnesSeek = new CustomSeek(c);
        brightnesSeek.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0));
        brightnesSeek.init(-100, 100, AppState.get().brigtnessImage);
        brightnesSeek.setOnSeekChanged(new IntegerResponse() {

            @Override
            public boolean onResultRecive(int result) {
                AppState.get().brigtnessImage = result;
                isEnableBC.setChecked(true);
                actionWrapper.run();
                return false;
            }
        });

        final CheckBox bolderText = new CheckBox(c);
        bolderText.setText(R.string.make_text_bold);
        bolderText.setChecked(AppState.get().bolderTextOnImage);
        bolderText.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                AppState.get().bolderTextOnImage = isChecked;
                if (isChecked) {
                    isEnableBC.setChecked(true);
                }
                actionWrapper.run();
            }
        });

        TextView contrastText = new TextView(c);
        contrastText.setText(R.string.contrast);

        TextView brightnessText = new TextView(c);
        brightnessText.setText(R.string.brightness);

        l.addView(contrastText);
        l.addView(contrastSeek);
        l.addView(brightnessText);
        l.addView(brightnesSeek);
        l.addView(bolderText);
        l.addView(isEnableBC);

        TextView defaults = new TextView(c);
        defaults.setTextAppearance(c, R.style.textLinkStyle);
        defaults.setText(R.string.restore_defaults_short);
        TxtUtils.underlineTextView(defaults);
        defaults.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                AlertDialogs.showOkDialog(c, c.getString(R.string.restore_defaults_full), new Runnable() {

                    @Override
                    public void run() {
                        AppState.get().brigtnessImage = 0;
                        AppState.get().contrastImage = 0;
                        AppState.get().bolderTextOnImage = false;
                        isEnableBC.setChecked(AppState.get().bolderTextOnImage);

                        brightnesSeek.reset(AppState.get().brigtnessImage);
                        contrastSeek.reset(AppState.get().contrastImage);

                        actionWrapper.run();
                    }
                });

            }
        });

        l.addView(defaults);
        return l;
    }

}
