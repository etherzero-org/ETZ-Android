package com.etzwallet.presenter.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.etzwallet.R;
import com.etzwallet.presenter.activities.intro.IntroActivity;
import com.etzwallet.presenter.activities.util.BRActivity;
import com.etzwallet.presenter.customviews.BRDialogView;
import com.etzwallet.presenter.interfaces.BROnSignalCompletion;
import com.etzwallet.tools.animation.BRAnimator;
import com.etzwallet.tools.animation.BRDialog;
import com.etzwallet.tools.animation.SpringAnimator;
import com.etzwallet.tools.manager.BRReportsManager;
import com.etzwallet.tools.manager.BRSharedPrefs;
import com.etzwallet.tools.security.AuthManager;
import com.etzwallet.tools.security.PostAuth;
import com.etzwallet.tools.security.SmartValidator;
import com.etzwallet.tools.util.Utils;
import com.etzwallet.wallet.WalletsMaster;
import com.etzwallet.wallet.wallets.bitcoin.BaseBitcoinWalletManager;
import com.etzwallet.wallet.wallets.ethereum.WalletEthManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InputWordsActivity extends BRActivity implements View.OnFocusChangeListener {
    private static final String TAG = InputWordsActivity.class.getName();
    private Button mNextButton;

    private static final int NUMBER_OF_WORDS = 12;
    private static final int LAST_WORD_INDEX = 11;

    public static final String EXTRA_UNLINK = "com.etzwallet.EXTRA_UNLINK";
    public static final String EXTRA_RESET_PIN = "com.etzwallet.EXTRA_RESET_PIN";

    private List<EditText> mEditWords = new ArrayList<>(NUMBER_OF_WORDS);

    private String mDebugPhrase;

    //will be true if this screen was called from the restore screen
    private boolean mIsRestoring = false;
    private boolean mIsResettingPin = false;
    private int fromCrate = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_words);
        String languageCode = Locale.getDefault().getLanguage();//手机语言
//        if (Utils.isEmulatorOrDebug(this)) {
//            mDebugPhrase = "こせき　ぎじにってい　けっこん　せつぞく　うんどう　ふこう　にっすう　こせい　きさま　なまみ　たきび　はかい";//japanese
//            mDebugPhrase = "video tiger report bid suspect taxi mail argue naive layer metal surface";//english
//            mDebugPhrase = "vocation triage capsule marchand onduler tibia illicite entier fureur minorer amateur lubie";//french
//            mDebugPhrase = "zorro turismo mezcla nicho morir chico blanco pájaro alba esencia roer repetir";//spanish
//            mDebugPhrase = "怨 贪 旁 扎 吹 音 决 廷 十 助 畜 怒";//chinese
//            mDebugPhrase = "aim lawn sniff tenant coffee smoke meat hockey glow try also angle";
//            mDebugPhrase = "oído largo pensar grúa vampiro nación tomar agitar mano azote tarea miedo";

//        }

        mNextButton = findViewById(R.id.send_button);
        fromCrate = getIntent().getIntExtra("from",0);
        Log.i("********", "fromCrate="+fromCrate);

        if (Utils.isUsingCustomInputMethod(this)) {
            BRDialog.showCustomDialog(this, getString(R.string.JailbreakWarnings_title), getString(R.string.Alert_customKeyboard_android),
                    getString(R.string.Button_ok), getString(R.string.JailbreakWarnings_close), new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            InputMethodManager imeManager = (InputMethodManager) getApplicationContext().getSystemService(INPUT_METHOD_SERVICE);
                            imeManager.showInputMethodPicker();
                            brDialogView.dismissWithAnimation();
                        }
                    }, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            brDialogView.dismissWithAnimation();
                        }
                    }, null, 0);
        }

//        ImageButton faq = findViewById(R.id.faq_button);
//
//        faq.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (!BRAnimator.isClickAllowed()) return;
//                BaseWalletManager wm = WalletsMaster.getInstance(InputWordsActivity.this).getCurrentWallet(InputWordsActivity.this);
//                BRAnimator.showSupportFragment(InputWordsActivity.this, BRConstants.FAQ_PAPER_KEY, wm);
//            }
//        });

        TextView title = findViewById(R.id.title);
        TextView description = findViewById(R.id.description);

        mEditWords.add((EditText) findViewById(R.id.word1));
        mEditWords.add((EditText) findViewById(R.id.word2));
        mEditWords.add((EditText) findViewById(R.id.word3));
        mEditWords.add((EditText) findViewById(R.id.word4));
        mEditWords.add((EditText) findViewById(R.id.word5));
        mEditWords.add((EditText) findViewById(R.id.word6));
        mEditWords.add((EditText) findViewById(R.id.word7));
        mEditWords.add((EditText) findViewById(R.id.word8));
        mEditWords.add((EditText) findViewById(R.id.word9));
        mEditWords.add((EditText) findViewById(R.id.word10));
        mEditWords.add((EditText) findViewById(R.id.word11));
        mEditWords.add((EditText) findViewById(R.id.word12));

        for (EditText editText : mEditWords) {
            editText.setOnFocusChangeListener(this);
            if(languageCode.equals("zh")){
                editText.setInputType(InputType.TYPE_TEXT_VARIATION_PHONETIC);
            }
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mIsRestoring = extras.getBoolean(EXTRA_UNLINK);
            mIsResettingPin = extras.getBoolean(EXTRA_RESET_PIN);
        }

        if(fromCrate==1){
            title.setText(getString(R.string.SecurityCenter_paperKeyTitle));
            description.setText(getString(R.string.ConfirmPaperPhrase_label));
        }else{
            if (mIsRestoring) {
                //change the labels
                title.setText(getString(R.string.MenuViewController_recoverButton));
                description.setText(getString(R.string.WipeWallet_instruction));
            } else if (mIsResettingPin) {
                //change the labels
                title.setText(getString(R.string.RecoverWallet_header_reset_pin));
                description.setText(getString(R.string.RecoverWallet_subheader_reset_pin));
            }
        }

        mEditWords.get(LAST_WORD_INDEX).setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    mNextButton.performClick();
                }
                return false;
            }
        });

        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick: next step1111");
                if (!BRAnimator.isClickAllowed()) return;
                final Activity app = InputWordsActivity.this;
                String phraseToCheck = getPhrase();//返回助记词的字符串
                if (Utils.isEmulatorOrDebug(app) && !Utils.isNullOrEmpty(mDebugPhrase)) {
                    phraseToCheck = mDebugPhrase;
                }
                if (phraseToCheck == null) {
                    return;
                }
                String cleanPhrase = SmartValidator.cleanPaperKey(app, phraseToCheck);
                Log.i(TAG, "phrase==cleanPhrase="+cleanPhrase);
                Log.i(TAG, "phrase==fromCrate=="+fromCrate);
                if (Utils.isNullOrEmpty(cleanPhrase)) {
                    BRReportsManager.reportBug(new NullPointerException("cleanPhrase is null or empty!"));
                    return;
                }
                if (SmartValidator.isPaperKeyValid(app, cleanPhrase)) {
                    //创建钱包  验证助记词  输入助记词
                    Utils.hideKeyboard(app);
                    clearWords();
                    if(fromCrate == 1){
                        if (SmartValidator.isPaperKeyCorrect(cleanPhrase, app)) {//验证创建的助记词
                            //执行恢复钱包代码
                            WalletsMaster m = WalletsMaster.getInstance(InputWordsActivity.this);
//                            m.wipeAll(InputWordsActivity.this);
                            PostAuth.getInstance().setCachedPaperKey(cleanPhrase);
                            //Disallow BTC and BCH sending.
                            BRSharedPrefs.putAllowSpend(app, BaseBitcoinWalletManager.BITCASH_SYMBOL, false);
                            BRSharedPrefs.putAllowSpend(app, BaseBitcoinWalletManager.BITCOIN_SYMBOL, false);
                            PostAuth.getInstance().onRecoverWalletAuth(app, false,true);
//

                            BRSharedPrefs.putPhraseWroteDown(InputWordsActivity.this, true);
                            BRAnimator.showBreadSignal(InputWordsActivity.this, getString(R.string.Alerts_paperKeySet), getString(R.string.Alerts_paperKeySetSubheader), R.drawable.ic_check_mark_white, new BROnSignalCompletion() {
                                @Override
                                public void onComplete() {
                                    BRAnimator.startBreadActivity(InputWordsActivity.this, false);
                                    overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
//
                                }
                            });
                            BRSharedPrefs.putFirstCreate(app, false);
//                            finishAffinity();
                        }else{
                            //助记词无效
                            BRDialog.showCustomDialog(app, "", getString(R.string.RecoverWallet_invalid),
                                    getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                                        @Override
                                        public void onClick(BRDialogView brDialogView) {
                                            brDialogView.dismissWithAnimation();
                                        }
                                    }, null, null, 0);
                        }
                    }else{
                        if (mIsRestoring || mIsResettingPin) {
                            if (SmartValidator.isPaperKeyCorrect(cleanPhrase, app)) {
                                if (mIsRestoring) {
                                    //切换钱包  输入助记词
                                    BRDialog.showCustomDialog(InputWordsActivity.this, getString(R.string.WipeWallet_alertTitle),
                                            getString(R.string.WipeWallet_alertMessage), getString(R.string.WipeWallet_wipe), getString(R.string.Button_cancel), new BRDialogView.BROnClickListener() {
                                                @Override
                                                public void onClick(BRDialogView brDialogView) {
                                                    brDialogView.dismissWithAnimation();
                                                    WalletsMaster m = WalletsMaster.getInstance(InputWordsActivity.this);
                                                    m.wipeWalletButKeystore(app);
                                                    m.wipeKeyStore(app);
                                                    Intent intent = new Intent(app, IntroActivity.class);
                                                    finalizeIntent(intent);
                                                }
                                            }, new BRDialogView.BROnClickListener() {
                                                @Override
                                                public void onClick(BRDialogView brDialogView) {
                                                    brDialogView.dismissWithAnimation();
                                                }
                                            }, null, 0);

                                } else {
                                    //导入钱包输入助记词
                                    Log.i("-----------", "onClick: next step22222");
                                    AuthManager.getInstance().setPinCode("", InputWordsActivity.this);
                                    Intent intent = new Intent(app, SetPinActivity.class);
                                    intent.putExtra("noPin", true);
                                    finalizeIntent(intent);
                                }


                            } else {
                                BRDialog.showCustomDialog(app, "", getString(R.string.RecoverWallet_invalid),
                                        getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                                            @Override
                                            public void onClick(BRDialogView brDialogView) {
                                                brDialogView.dismissWithAnimation();
                                            }
                                        }, null, null, 0);
                            }

                        } else {
                            Utils.hideKeyboard(app);
                            WalletsMaster m = WalletsMaster.getInstance(InputWordsActivity.this);
                            m.wipeAll(InputWordsActivity.this);
                            PostAuth.getInstance().setCachedPaperKey(cleanPhrase);
                            //Disallow BTC and BCH sending.
                            BRSharedPrefs.putAllowSpend(app, BaseBitcoinWalletManager.BITCASH_SYMBOL, false);
                            BRSharedPrefs.putAllowSpend(app, BaseBitcoinWalletManager.BITCOIN_SYMBOL, false);
                            PostAuth.getInstance().onRecoverWalletAuth(app, false,false);
                            WalletEthManager.setmInstance(null);//设为空重新获取新的地址
                        }

                    }


                } else {
                    BRDialog.showCustomDialog(app, "", getResources().getString(R.string.RecoverWallet_invalid),
                            getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                                @Override
                                public void onClick(BRDialogView brDialogView) {
                                    brDialogView.dismissWithAnimation();
                                }
                            }, null, null, 0);

                }
            }
        });

    }

    private void finalizeIntent(Intent intent) {
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
        startActivity(intent);
        if (!InputWordsActivity.this.isDestroyed()) finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    private String getPhrase() {
        boolean success = true;

        StringBuilder paperKeyStringBuilder = new StringBuilder();

        for (EditText editText : mEditWords) {
            String cleanedWords = clean(editText.getText().toString().toLowerCase());
            if (Utils.isNullOrEmpty(cleanedWords)) {
                SpringAnimator.failShakeAnimation(this, editText);
                success = false;
            } else {
                paperKeyStringBuilder.append(cleanedWords);
                paperKeyStringBuilder.append(' ');
            }
        }
        Log.i(TAG, "getPhrase: paperKeyStringBuilder=="+paperKeyStringBuilder.length());
        //remove the last space
        if(paperKeyStringBuilder.length() == 0 ){
            return null;
        }else{
            paperKeyStringBuilder.setLength(paperKeyStringBuilder.length() - 1);
        }


        String paperKey = paperKeyStringBuilder.toString();
        Log.i(TAG, "phrase==paperKey="+paperKey);
        if (!success) {
            return null;
        }

        //ensure the paper key is 12 words
        int numberOfWords = paperKey.split(" ").length;
        if (numberOfWords != NUMBER_OF_WORDS) {
            BRReportsManager.reportBug(new IllegalArgumentException("Paper key contains " + numberOfWords + " words"));
            return null;
        }

        return paperKey;
    }

    private String clean(String word) {
        return word.trim().replaceAll(" ", "");
    }

    private void clearWords() {
        for (EditText editText : mEditWords) {
            editText.setText("");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            validateWord((EditText) v);
        } else {
            ((EditText) v).setTextColor(getColor(R.color.light_gray));
        }
    }

    private void validateWord(EditText view) {
        String word = view.getText().toString();
        boolean valid = SmartValidator.isWordValid(this, word);
        view.setTextColor(getColor(valid ? R.color.light_gray : R.color.red_text));
        if (!valid) {
            SpringAnimator.failShakeAnimation(this, view);
        }
    }

}
