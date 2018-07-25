package com.etzwallet.presenter.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.transition.AutoTransition;
import android.support.transition.TransitionManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.etzwallet.BuildConfig;
import com.etzwallet.R;
import com.etzwallet.presenter.customviews.BRButton;
import com.etzwallet.presenter.customviews.BRDialogView;
import com.etzwallet.presenter.customviews.BRKeyboard;
import com.etzwallet.presenter.customviews.BRLinearLayoutWithCaret;
import com.etzwallet.presenter.customviews.BRText;
import com.etzwallet.presenter.entities.CryptoRequest;
import com.etzwallet.tools.animation.BRAnimator;
import com.etzwallet.tools.animation.BRDialog;
import com.etzwallet.tools.animation.SlideDetector;
import com.etzwallet.tools.animation.SpringAnimator;
import com.etzwallet.tools.manager.BRClipboardManager;
import com.etzwallet.tools.manager.BRReportsManager;
import com.etzwallet.tools.manager.BRSharedPrefs;
import com.etzwallet.tools.manager.SendManager;
import com.etzwallet.tools.threads.executor.BRExecutor;
import com.etzwallet.tools.util.BRConstants;
import com.etzwallet.tools.util.CurrencyUtils;
import com.etzwallet.tools.util.Utils;
import com.etzwallet.wallet.WalletsMaster;
import com.etzwallet.wallet.util.CryptoUriParser;
import com.etzwallet.wallet.abstracts.BaseWalletManager;
import com.etzwallet.wallet.wallets.ethereum.WalletEthManager;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.regex.*;

import static com.etzwallet.wallet.util.CryptoUriParser.parseRequest;


/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/29/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class FragmentSend extends Fragment {
    private static final String TAG = FragmentSend.class.getName();
    public ScrollView backgroundLayout;
    public LinearLayout signalLayout;
    private BRKeyboard keyboard;
    private EditText addressEdit;
    private Button scan;
    private Button paste;
    private Button send;
    private EditText commentEdit;
    private EditText commentData;
    private EditText gasLimitIpt;
    private EditText gasPriceIpt;
    private LinearLayout commentDataView;
    private StringBuilder amountBuilder;
    private TextView isoText;
    private EditText amountEdit;
    private TextView balanceText;
    private TextView feeText;
    private Button advancedBtn;
    private BigDecimal curBalance;
    private String selectedIso;
    private Button isoButton;
    private int keyboardIndex;
    private LinearLayout keyboardLayout;
    private RelativeLayout advBtnView;

    private ImageButton close;
    private ConstraintLayout amountLayout;
//    private BRButton regular;
//    private BRButton economy;
//    private BRLinearLayoutWithCaret feeLayout;
    private LinearLayout feeLayout;
    private boolean feeButtonsShown = false;
//    private BRText feeDescription;
//    private BRText warningText;
    private boolean amountLabelOn = true;

    private static String savedMemo;
    private static String savedIso;
    private static String savedAmount;

    private boolean ignoreCleanup;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_send, container, false);
        backgroundLayout = rootView.findViewById(R.id.background_layout);
        signalLayout = rootView.findViewById(R.id.signal_layout);
        keyboard = rootView.findViewById(R.id.keyboard);
        keyboard.setBRButtonBackgroundResId(R.drawable.keyboard_white_button);
        keyboard.setBRKeyboardColor(R.color.white);
        isoText = rootView.findViewById(R.id.iso_text);
        addressEdit = rootView.findViewById(R.id.address_edit);
        scan = rootView.findViewById(R.id.scan);
        paste = rootView.findViewById(R.id.paste_button);
        send = rootView.findViewById(R.id.send_button);
        commentEdit = rootView.findViewById(R.id.comment_edit);
        commentData = rootView.findViewById(R.id.comment_data);
        gasLimitIpt = rootView.findViewById(R.id.gas_limit);
        gasPriceIpt = rootView.findViewById(R.id.gas_price);
        commentDataView = rootView.findViewById(R.id.comment_data_view);
        amountEdit = rootView.findViewById(R.id.amount_edit);
        balanceText = rootView.findViewById(R.id.balance_text);
        feeText = rootView.findViewById(R.id.fee_text);
        advancedBtn = rootView.findViewById(R.id.advanced_btn);
        isoButton = rootView.findViewById(R.id.iso_button);
        keyboardLayout = rootView.findViewById(R.id.keyboard_layout);
        amountLayout = rootView.findViewById(R.id.amount_layout);
        feeLayout = rootView.findViewById(R.id.adv_edit_text);
        advBtnView = rootView.findViewById(R.id.adv_btn_view);
//        feeDescription = rootView.findViewById(R.id.fee_description);
//        warningText = rootView.findViewById(R.id.warning_text);

//        regular = rootView.findViewById(R.id.left_button);
//        economy = rootView.findViewById(R.id.right_button);
        close = rootView.findViewById(R.id.close_button);
        BaseWalletManager wm = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());
        selectedIso = BRSharedPrefs.isCryptoPreferred(getActivity()) ? BRSharedPrefs.getPreferredFiatIso(getContext()) : wm.getIso();

        amountBuilder = new StringBuilder(0);
        setListeners();
        visibleAdvView();
        isoText.setText(getString(R.string.Send_amountLabel));
        isoText.setTextSize(18);
        isoText.setTextColor(getContext().getColor(R.color.light_gray));
        isoText.requestLayout();
        signalLayout.setOnTouchListener(new SlideDetector(getContext(), signalLayout));

        signalLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        updateText();

        showFeeSelectionButtons(feeButtonsShown);
        //弹出高级选项
        advancedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showKeyboard(false);
                feeButtonsShown = !feeButtonsShown;
                showFeeSelectionButtons(feeButtonsShown);
            }
        });
        keyboardIndex = signalLayout.indexOfChild(keyboardLayout);

//        ImageButton faq = rootView.findViewById(R.id.faq_button);
//
//        faq.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (!BRAnimator.isClickAllowed()) return;
//                Activity app = getActivity();
//                if (app == null) {
//                    Log.e(TAG, "onClick: app is null, can't start the webview with url: " + URL_SUPPORT);
//                    return;
//                }
//                BaseWalletManager wm = WalletsMaster.getInstance(app).getCurrentWallet(app);
//                BRAnimator.showSupportFragment(app, BRConstants.FAQ_SEND, wm);
//            }
//        });

        showKeyboard(false);
//        setButton(true);

        signalLayout.setLayoutTransition(BRAnimator.getDefaultTransition());

        return rootView;
    }

    //只在etz显示data输入框
    private void visibleAdvView(){
        final BaseWalletManager wm = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());
        if(wm.getIso().equalsIgnoreCase("ETZ")){
            commentDataView.setVisibility(View.VISIBLE);
        }else{
            commentDataView.setVisibility(View.GONE);
        }

        if(wm.getIso().equalsIgnoreCase("BTC")){
            advBtnView.setVisibility(View.GONE);
        }else{
            advBtnView.setVisibility(View.VISIBLE);
        }
    }

    //needed to fix the overlap bug
    private void hideShowAdvOption(){
        commentEdit.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    amountLayout.requestLayout();
                    return true;
                }
                return false;
            }
        });
    }





    private void setListeners() {
        amountEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BaseWalletManager wm = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());
                showKeyboard(true);
                showFeeSelectionButtons(false);
                hideShowAdvOption();
                if (amountLabelOn) { //only first time
                    amountLabelOn = false;
                    amountEdit.setHint("0");
                    amountEdit.setTextSize(24);
                    balanceText.setVisibility(View.VISIBLE);
                    advancedBtn.setVisibility(View.VISIBLE);
                    feeText.setVisibility(View.VISIBLE);
                    isoText.setTextColor(getContext().getColor(R.color.almost_black));
                    isoText.setText(CurrencyUtils.getSymbolByIso(getActivity(), selectedIso));
                    isoText.setTextSize(28);
                    final float scaleX = amountEdit.getScaleX();
                    amountEdit.setScaleX(0);

                    AutoTransition tr = new AutoTransition();
                    tr.setInterpolator(new OvershootInterpolator());
                    tr.addListener(new android.support.transition.Transition.TransitionListener() {
                        @Override
                        public void onTransitionStart(@NonNull android.support.transition.Transition transition) {

                        }

                        @Override
                        public void onTransitionEnd(@NonNull android.support.transition.Transition transition) {
                            amountEdit.requestLayout();
                            amountEdit.animate().setDuration(100).scaleX(scaleX);
                        }

                        @Override
                        public void onTransitionCancel(@NonNull android.support.transition.Transition transition) {

                        }

                        @Override
                        public void onTransitionPause(@NonNull android.support.transition.Transition transition) {

                        }

                        @Override
                        public void onTransitionResume(@NonNull android.support.transition.Transition transition) {

                        }
                    });

                    ConstraintSet set = new ConstraintSet();
                    set.clone(amountLayout);
                    TransitionManager.beginDelayedTransition(amountLayout, tr);

                    int px4 = Utils.getPixelsFromDps(getContext(), 4);
                    set.connect(balanceText.getId(), ConstraintSet.TOP, isoText.getId(), ConstraintSet.BOTTOM, px4);
                    set.connect(feeText.getId(), ConstraintSet.TOP, balanceText.getId(), ConstraintSet.BOTTOM, px4);
                    set.connect(feeText.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, px4);
                    set.connect(isoText.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, px4);
                    set.connect(isoText.getId(), ConstraintSet.BOTTOM, -1, ConstraintSet.TOP, -1);
                    set.applyTo(amountLayout);

                }

            }
        });





        commentEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                showKeyboard(!hasFocus);
            }
        });


        commentData.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                showKeyboard(!hasFocus);
            }
        });


        gasLimitIpt.setOnFocusChangeListener(new View.OnFocusChangeListener(){
            @Override
            public void onFocusChange(View v, boolean hasFous) {
                showKeyboard(!hasFous);
            }
        });
        gasLimitIpt.setInputType(EditorInfo.TYPE_CLASS_PHONE);

        gasPriceIpt.setOnFocusChangeListener(new View.OnFocusChangeListener(){
            @Override
            public void onFocusChange(View v, boolean hasFous) {
                showKeyboard(!hasFous);
            }
        });
        gasPriceIpt.setInputType(EditorInfo.TYPE_CLASS_PHONE);


        paste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                String theUrl = BRClipboardManager.getClipboard(getActivity());
                if (Utils.isNullOrEmpty(theUrl)) {
                    sayClipboardEmpty();
                    return;
                }
                showKeyboard(false);

                final BaseWalletManager wm = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());


                if (Utils.isEmulatorOrDebug(getActivity()) && BuildConfig.BITCOIN_TESTNET) {
                    theUrl = wm.decorateAddress(theUrl);
                }

                final CryptoRequest obj = parseRequest(getActivity(), theUrl);

                if (obj == null || Utils.isNullOrEmpty(obj.address)) {
                    sayInvalidClipboardData();
                    return;
                }
                if (Utils.isEmulatorOrDebug(getActivity())) {
                    Log.d(TAG, "Send Address -> " + obj.address);
                    Log.d(TAG, "Send Value -> " + obj.value);
                    Log.d(TAG, "Send Amount -> " + obj.amount);
                }

                if (obj.iso != null && !obj.iso.equalsIgnoreCase(wm.getIso())) {
                    sayInvalidAddress(); //invalid if the screen is Bitcoin and scanning BitcoinCash for instance
                    return;
                }


                if (wm.isAddressValid(obj.address)) {
                    final Activity app = getActivity();
                    if (app == null) {
                        Log.e(TAG, "paste onClick: app is null");
                        return;
                    }
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            if (wm.containsAddress(obj.address)) {
                                app.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        BRDialog.showCustomDialog(getActivity(), "", getResources().getString(R.string.Send_containsAddress),
                                                getResources().getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                                                    @Override
                                                    public void onClick(BRDialogView brDialogView) {
                                                        brDialogView.dismiss();
                                                    }
                                                }, null, null, 0);
                                        BRClipboardManager.putClipboard(getActivity(), "");
                                    }
                                });

                            } else if (wm.addressIsUsed(obj.address)) {
                                app.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        String title = String.format("%1$s addresses are intended for single use only.", wm.getName());
                                        BRDialog.showCustomDialog(getActivity(), title, getString(R.string.Send_UsedAddress_secondLIne),
                                                "Ignore", "Cancel", new BRDialogView.BROnClickListener() {
                                                    @Override
                                                    public void onClick(BRDialogView brDialogView) {
                                                        brDialogView.dismiss();
                                                        addressEdit.setText(wm.decorateAddress(obj.address));
                                                    }
                                                }, new BRDialogView.BROnClickListener() {
                                                    @Override
                                                    public void onClick(BRDialogView brDialogView) {
                                                        brDialogView.dismiss();
                                                    }
                                                }, null, 0);
                                    }
                                });

                            } else {
                                app.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.e(TAG, "run: " + wm.getIso());
                                        addressEdit.setText(wm.decorateAddress(obj.address));

                                    }
                                });
                            }
                        }
                    });

                } else {
                    sayInvalidClipboardData();
                }

            }
        });

        isoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (selectedIso.equalsIgnoreCase(BRSharedPrefs.getPreferredFiatIso(getContext()))) {
                    Activity app = getActivity();

                    String currentIos;

                    if( WalletsMaster.getInstance(app).getCurrentWallet(app).getIso()=="ETH"){
                        currentIos = "ETZ";
                    }else{
                        currentIos = WalletsMaster.getInstance(app).getCurrentWallet(app).getIso();
                    }

                    selectedIso = currentIos;
                } else {
                    selectedIso = BRSharedPrefs.getPreferredFiatIso(getContext());
                }
                updateText();

            }
        });

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                saveMetaData();
                BRAnimator.openScanner(getActivity(), BRConstants.SCANNER_REQUEST);

            }
        });

        //交易  点击发送按钮
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //not allowed now
                if (!BRAnimator.isClickAllowed()) return;
                WalletsMaster master = WalletsMaster.getInstance(getActivity());
                final BaseWalletManager wm = master.getCurrentWallet(getActivity());

//                if(WalletsMaster.getInstance(getActivity()).isIsoErc20(getActivity(), wm.getIso())){
//                    Log.i(TAG, "onClick: istoken");
//                }else{
//                    Log.i(TAG, "onClick: isnotToken");
//                }
                        
                        
                //get the current wallet used
                if (wm == null) {
                    Log.e(TAG, "onClick: Wallet is null and it can't happen.");
                    BRReportsManager.reportBug(new NullPointerException("Wallet is null and it can't happen."), true);
                    return;
                }
                boolean allFilled = true;
                String rawAddress = addressEdit.getText().toString();
                String amountStr = amountBuilder.toString();
                String comment = commentEdit.getText().toString();
                String dataValue = commentData.getText().toString();
                String gasL = gasLimitIpt.getText().toString();
                String gasP = gasPriceIpt.getText().toString();



                dataValue = dataValue.toLowerCase();
                if(dataValue.length() == 0){
                    dataValue = "";
                }
                if(dataValue.startsWith("0x")){
                    dataValue = dataValue.substring(2);
                }
                String p = "^[a-z0-9]*$";
                String p1 = "^[0-9]*$";
                final Activity app = getActivity();

//                if(gasL.length() > 0){
//                    if(!Pattern.matches(p1,gasL)){
//                        BRDialog.showCustomDialog(app,app.getString(R.string.Alert_error),app.getString(R.string.GasLimit_invalid),app.getString(R.string.AccessibilityLabels_close),null,new BRDialogView.BROnClickListener() {
//                            @Override
//                            public void onClick(BRDialogView brDialogView) {
//                                brDialogView.dismiss();
//                            }
//                        },null,null,0);
//                    }
//                    return;
//                }
////
//                if(gasP.length() > 0){
//                    if(!Pattern.matches(p1,gasP)){
//                        BRDialog.showCustomDialog(app,app.getString(R.string.Alert_error),app.getString(R.string.GasPrice_invalid),app.getString(R.string.AccessibilityLabels_close),null,new BRDialogView.BROnClickListener() {
//                            @Override
//                            public void onClick(BRDialogView brDialogView) {
//                                brDialogView.dismiss();
//                            }
//                        },null,null,0);
//                    }
//                    return;
//                }


                if(dataValue.length() > 0){
                    if(!Pattern.matches(p, dataValue)){
                        BRDialog.showCustomDialog(app,app.getString(R.string.Alert_error),app.getString(R.string.Data_invalid),app.getString(R.string.AccessibilityLabels_close),null,new BRDialogView.BROnClickListener() {
                            @Override
                            public void onClick(BRDialogView brDialogView) {
                                brDialogView.dismiss();
                            }
                        },null,null,0);
                        return;
                    }
                }
                //inserted amount
                BigDecimal rawAmount = new BigDecimal(Utils.isNullOrEmpty(amountStr) || amountStr.equalsIgnoreCase(".") ? "0" : amountStr);
                //is the chosen ISO a crypto (could be a fiat currency)
                boolean isIsoCrypto = master.isIsoCrypto(getActivity(), selectedIso);

                BigDecimal cryptoAmount = isIsoCrypto ? wm.getSmallestCryptoForCrypto(getActivity(), rawAmount) : wm.getSmallestCryptoForFiat(getActivity(), rawAmount);

                CryptoRequest req = CryptoUriParser.parseRequest(getActivity(), rawAddress);
                if (req == null || Utils.isNullOrEmpty(req.address)) {
                    sayInvalidClipboardData();
                    return;
                }

                if (!wm.isAddressValid(req.address)) {

                    BRDialog.showCustomDialog(app, app.getString(R.string.Alert_error), app.getString(R.string.Send_noAddress),
                            app.getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                                @Override
                                public void onClick(BRDialogView brDialogView) {
                                    brDialogView.dismissWithAnimation();
                                }
                            }, null, null, 0);
                    return;
                }

                if (cryptoAmount.compareTo(BigDecimal.ZERO) < 0) {
                    allFilled = false;
                    SpringAnimator.failShakeAnimation(getActivity(), amountEdit);
                }

                if (cryptoAmount.compareTo(wm.getCachedBalance(getActivity())) > 0) {
                    allFilled = false;
                    SpringAnimator.failShakeAnimation(getActivity(), balanceText);
                    SpringAnimator.failShakeAnimation(getActivity(), feeText);
                }

                if (WalletsMaster.getInstance(getActivity()).isIsoErc20(getActivity(), wm.getIso())) {

                    BigDecimal rawFee = wm.getEstimatedFee(cryptoAmount, addressEdit.getText().toString());
                    BaseWalletManager ethWm = WalletEthManager.getInstance(app);
                    BigDecimal isoFee = isIsoCrypto ? rawFee : ethWm.getFiatForSmallestCrypto(app, rawFee, null);
                    BigDecimal b = ethWm.getCachedBalance(app);
                    if (isoFee.compareTo(b) > 0) {
                        if (allFilled) {
                            BigDecimal ethVal = ethWm.getCryptoForSmallestCrypto(app, isoFee);
//                            sayInsufficientEthereumForFee(app, ethVal.setScale(ethWm.getMaxDecimalPlaces(app), BRConstants.ROUNDING_MODE).toPlainString());
                            sayInsufficientEthereumForFee(app, "0.01ETZ");

                            allFilled = false;
                        }
                    }

                }
                if (allFilled) {
                    final CryptoRequest item = new CryptoRequest(null, false, comment, req.address, cryptoAmount,dataValue,gasL,gasP);
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            SendManager.sendTransaction(getActivity(), item, wm, null);
                        }
                    });
                }
            }
        });

        backgroundLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                getActivity().onBackPressed();
            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity app = getActivity();
                if (app != null)
                    app.getFragmentManager().popBackStack();
            }
        });


        addressEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))
                        || (actionId == EditorInfo.IME_ACTION_DONE) || (actionId == EditorInfo.IME_ACTION_NEXT)) {
                    Utils.hideKeyboard(getActivity());
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showKeyboard(true);
                            //键盘弹出  高级选项隐藏
                            hideShowAdvOption();
                        }
                    }, 500);

                }
                return false;
            }
        });

        addressEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                showKeyboard(!hasFocus);
            }
        });

        keyboard.addOnInsertListener(new BRKeyboard.OnInsertListener() {
            @Override
            public void onClick(String key) {
                handleClick(key);
            }
        });

//        regular.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                setButton(true);
//            }
//        });
//        economy.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                setButton(false);
//            }
//        });
//        updateText();

    }

    private void showKeyboard(boolean b) {
        int curIndex = keyboardIndex;

        if (!b) {
            signalLayout.removeView(keyboardLayout);

        } else {
            Utils.hideKeyboard(getActivity());
            if (signalLayout.indexOfChild(keyboardLayout) == -1)
                signalLayout.addView(keyboardLayout, curIndex);
            else
                signalLayout.removeView(keyboardLayout);

        }
    }

    private void sayClipboardEmpty() {
        BRDialog.showCustomDialog(getActivity(), "", getResources().getString(R.string.Send_emptyPasteboard),
                getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        brDialogView.dismiss();
                    }
                }, null, null, 0);
    }

    private void sayInvalidClipboardData() {
        BRDialog.showCustomDialog(getActivity(), "", getResources().getString(R.string.Send_invalidAddressTitle),
                getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        brDialogView.dismiss();
                    }
                }, null, null, 0);
    }

    private void saySomethingWentWrong() {
        BRDialog.showCustomDialog(getActivity(), "", "Something went wrong.",
                getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {

                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        brDialogView.dismiss();
                    }
                }, null, null, 0);
    }

    private void sayInvalidAddress() {
        BRDialog.showCustomDialog(getActivity(), "", getResources().getString(R.string.Send_invalidAddressMessage),
                getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        brDialogView.dismiss();
                    }
                }, null, null, 0);
    }

    private void sayInsufficientEthereumForFee(final Activity app, String ethNeeded) {
        String message = String.format(app.getString(R.string.Send_insufficientGasMessage), ethNeeded);
        BRDialog.showCustomDialog(app, app.getString(R.string.Send_insufficientGasTitle), message, app.getString(R.string.Button_continueAction),
                app.getString(R.string.Button_cancel), new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        brDialogView.dismissWithAnimation();
                        app.getFragmentManager().popBackStack();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!app.isDestroyed())
                                    app.onBackPressed();
                            }
                        }, 1000);
                    }
                }, new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        brDialogView.dismissWithAnimation();
                    }
                }, null, 0);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ViewTreeObserver observer = signalLayout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (observer.isAlive())
                    observer.removeOnGlobalLayoutListener(this);
                BRAnimator.animateBackgroundDim(backgroundLayout, false);
                BRAnimator.animateSignalSlide(signalLayout, false, new BRAnimator.OnSlideAnimationEnd() {
                    @Override
                    public void onAnimationEnd() {

                    }
                });
            }
        });

    }

    @Override
    public void onStop() {
        super.onStop();
        BRAnimator.animateBackgroundDim(backgroundLayout, true);
        BRAnimator.animateSignalSlide(signalLayout, true, new BRAnimator.OnSlideAnimationEnd() {
            @Override
            public void onAnimationEnd() {
                if (getActivity() != null) {
                    try {
                        getActivity().getFragmentManager().popBackStack();
                    } catch (Exception ignored) {

                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMetaData();

    }

    @Override
    public void onPause() {
        super.onPause();
        Utils.hideKeyboard(getActivity());
        if (!ignoreCleanup) {
            savedIso = null;
            savedAmount = null;
            savedMemo = null;
        }
    }

    private void handleClick(String key) {
        if (key == null) {
            Log.e(TAG, "handleClick: key is null! ");
            return;
        }

        if (key.isEmpty()) {
            handleDeleteClick();
        } else if (Character.isDigit(key.charAt(0))) {
            handleDigitClick(Integer.parseInt(key.substring(0, 1)));
        } else if (key.charAt(0) == '.') {
            handleSeparatorClick();
        }
    }

    private void handleDigitClick(Integer dig) {
        String currAmount = amountBuilder.toString();
        String iso = selectedIso;
        BaseWalletManager wm = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());
        if (new BigDecimal(currAmount.concat(String.valueOf(dig))).compareTo(wm.getMaxAmount(getActivity())) <= 0) {
            //do not insert 0 if the balance is 0 now
            if (currAmount.equalsIgnoreCase("0")) amountBuilder = new StringBuilder("");
            if ((currAmount.contains(".") && (currAmount.length() - currAmount.indexOf(".") > CurrencyUtils.getMaxDecimalPlaces(getActivity(), iso))))
                return;
            amountBuilder.append(dig);
            updateText();
        }
    }

    private void handleSeparatorClick() {
        String currAmount = amountBuilder.toString();
        if (currAmount.contains(".") || CurrencyUtils.getMaxDecimalPlaces(getActivity(), selectedIso) == 0)
            return;
        amountBuilder.append(".");
        updateText();
    }

    private void handleDeleteClick() {
        String currAmount = amountBuilder.toString();
        if (currAmount.length() > 0) {
            amountBuilder.deleteCharAt(currAmount.length() - 1);
            updateText();
        }

    }

    private void updateText() {
        Activity app = getActivity();
        if (app == null) return;
        //selectedIso == "ETZ"
        String stringAmount = amountBuilder.toString();
        setAmount();
        BaseWalletManager wm = WalletsMaster.getInstance(app).getCurrentWallet(app);
        String balanceString;
        if (selectedIso == null)
            selectedIso = wm.getIso();
        //String iso = selectedIso;

        Log.i(TAG, "updateText: selectedIso==="+selectedIso);


        curBalance = wm.getCachedBalance(app);
        if (!amountLabelOn)
            isoText.setText(CurrencyUtils.getSymbolByIso(app, selectedIso));
        isoButton.setText(selectedIso);

        //is the chosen ISO a crypto (could be also a fiat currency)
        boolean isIsoCrypto = WalletsMaster.getInstance(app).isIsoCrypto(app, selectedIso);
        //对象的getInstance方法 --对于方法的引用
        Log.i(TAG, "updateTextBTC=="+WalletsMaster.getInstance(app).isIsoCrypto(app, "BTC"));
        Log.i(TAG, "updateTextETZ=="+WalletsMaster.getInstance(app).isIsoCrypto(app, "ETZ"));
        Log.i(TAG, "updateTextETH=="+WalletsMaster.getInstance(app).isIsoCrypto(app, "ETH"));

        boolean isWalletErc20 = WalletsMaster.getInstance(app).isIsoErc20(app, wm.getIso());
        BigDecimal inputAmount = new BigDecimal(Utils.isNullOrEmpty(stringAmount) || stringAmount.equalsIgnoreCase(".") ? "0" : stringAmount);

        //smallest crypto e.g. satoshis
        BigDecimal cryptoAmount = isIsoCrypto ? wm.getSmallestCryptoForCrypto(app, inputAmount) : wm.getSmallestCryptoForFiat(app, inputAmount);

        //wallet's balance for the selected ISO
        BigDecimal isoBalance = isIsoCrypto ? wm.getCryptoForSmallestCrypto(app, curBalance) : wm.getFiatForSmallestCrypto(app, curBalance, null);
        Log.i(TAG, "updateText isoBalance=="+isoBalance);

        if (isoBalance == null) isoBalance = BigDecimal.ZERO;

        BigDecimal rawFee = wm.getEstimatedFee(cryptoAmount, addressEdit.getText().toString());

        //get the fee for iso (dollars, bits, BTC..)
        BigDecimal isoFee = isIsoCrypto ? rawFee : wm.getFiatForSmallestCrypto(app, rawFee, null);

        //format the fee to the selected ISO
        String formattedFee = CurrencyUtils.getFormattedAmount(app, selectedIso, isoFee);

        if (isWalletErc20) {
            BaseWalletManager ethWm = WalletEthManager.getInstance(app);
            isoFee = isIsoCrypto ? rawFee : ethWm.getFiatForSmallestCrypto(app, rawFee, null);
            formattedFee = CurrencyUtils.getFormattedAmount(app, isIsoCrypto ? ethWm.getIso() : selectedIso, isoFee);
        }

        boolean isOverTheBalance = inputAmount.compareTo(isoBalance) > 0;

        if (isOverTheBalance) {
            balanceText.setTextColor(getContext().getColor(R.color.warning_color));
            feeText.setTextColor(getContext().getColor(R.color.warning_color));
            amountEdit.setTextColor(getContext().getColor(R.color.warning_color));

            if (!amountLabelOn)
                isoText.setTextColor(getContext().getColor(R.color.warning_color));
        } else {
            balanceText.setTextColor(getContext().getColor(R.color.light_gray));
            feeText.setTextColor(getContext().getColor(R.color.light_gray));
            amountEdit.setTextColor(getContext().getColor(R.color.almost_black));
            if (!amountLabelOn)
                isoText.setTextColor(getContext().getColor(R.color.almost_black));
        }
        //formattedBalance

        //isIsoCrypto == fasle
        String formattedBalance = CurrencyUtils.getFormattedAmount(app, selectedIso,
                isIsoCrypto ? wm.getSmallestCryptoForCrypto(app, isoBalance) : isoBalance);

        balanceString = String.format(getString(R.string.Send_balance), formattedBalance);
        balanceText.setText(balanceString);
        feeText.setText(String.format(getString(R.string.Send_fee), "0.00"));//formattedFee
        amountLayout.requestLayout();
    }

    public void setCryptoObject(final CryptoRequest obj) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (obj == null) {
                    Log.e(TAG, "setCryptoObject: obj is null");
                    return;
                }
                Activity app = getActivity();
                if (app == null) {
                    Log.e(TAG, "setCryptoObject: app is null");
                    return;
                }
                BaseWalletManager wm = WalletsMaster.getInstance(app).getCurrentWallet(app);
                if (obj.address != null && addressEdit != null) {
                    addressEdit.setText(wm.decorateAddress(obj.address.trim()));
                }
                if (obj.message != null && commentEdit != null) {
                    commentEdit.setText(obj.message);
                }
                if (obj.amount != null) {

                    BigDecimal satoshiAmount = obj.amount.multiply(new BigDecimal(100000000));
                    amountBuilder = new StringBuilder(wm.getFiatForSmallestCrypto(getActivity(), satoshiAmount, null).toPlainString());
                    updateText();
                } else {
                    // ETH request amount param is named `value`

                    if (obj.value != null) {

                        BigDecimal fiatAmount = wm.getFiatForSmallestCrypto(getActivity(), obj.value, null);
                        fiatAmount = fiatAmount.setScale(2, RoundingMode.HALF_EVEN);

                        amountBuilder = new StringBuilder(fiatAmount.toPlainString());
                        updateText();

                    }
                }
            }
        }, 500);

    }

    private void showFeeSelectionButtons(boolean b) {
        if (!b) {
            signalLayout.removeView(feeLayout);
        } else {
            signalLayout.addView(feeLayout, signalLayout.indexOfChild(amountLayout) + 1);

        }
    }

    private void setAmount() {
        String tmpAmount = amountBuilder.toString();
        int divider = tmpAmount.length();
        if (tmpAmount.contains(".")) {
            divider = tmpAmount.indexOf(".");
        }
        StringBuilder newAmount = new StringBuilder();
        for (int i = 0; i < tmpAmount.length(); i++) {
            newAmount.append(tmpAmount.charAt(i));
            if (divider > 3 && divider - 1 != i && divider > i && ((divider - i - 1) % 3 == 0)) {
                newAmount.append(",");
            }
        }
        amountEdit.setText(newAmount.toString());
    }

//    private void setButton(boolean isRegular) {
//        BaseWalletManager wallet = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());
//        String iso = wallet.getIso();
//        if (isRegular) {
//            BRSharedPrefs.putFavorStandardFee(getActivity(), iso, true);
//            regular.setTextColor(getContext().getColor(R.color.white));
//            regular.setBackground(getContext().getDrawable(R.drawable.b_half_left_blue));
//            economy.setTextColor(getContext().getColor(R.color.dark_blue));
//            economy.setBackground(getContext().getDrawable(R.drawable.b_half_right_blue_stroke));
////            feeDescription.setText(String.format(getString(R.string.FeeSelector_estimatedDeliver), getString(R.string.FeeSelector_regularTime)));
////            warningText.getLayoutParams().height = 0;
//        } else {
//            BRSharedPrefs.putFavorStandardFee(getActivity(), iso, false);
//            regular.setTextColor(getContext().getColor(R.color.dark_blue));
//            regular.setBackground(getContext().getDrawable(R.drawable.b_half_left_blue_stroke));
//            economy.setTextColor(getContext().getColor(R.color.white));
//            economy.setBackground(getContext().getDrawable(R.drawable.b_half_right_blue));
////            feeDescription.setText(String.format(getString(R.string.FeeSelector_estimatedDeliver), getString(R.string.FeeSelector_economyTime)));
////            warningText.getLayoutParams().height = LinearLayout.LayoutParams.WRAP_CONTENT;
//        }
////        warningText.requestLayout();
//        updateText();
//    }

    // from the link above
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks whether a hardware keyboard is available
        if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
            showKeyboard(true);
        } else if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
            showKeyboard(false);
        }
    }

    private void saveMetaData() {
        if (!commentEdit.getText().toString().isEmpty())
            savedMemo = commentEdit.getText().toString();
        if (!amountBuilder.toString().isEmpty())
            savedAmount = amountBuilder.toString();
        savedIso = selectedIso;
        ignoreCleanup = true;
    }

    private void loadMetaData() {
        ignoreCleanup = false;
        if (!Utils.isNullOrEmpty(savedMemo))
            commentEdit.setText(savedMemo);
        if (!Utils.isNullOrEmpty(savedIso))
            selectedIso = savedIso;
        if (!Utils.isNullOrEmpty(savedAmount)) {
            amountBuilder = new StringBuilder(savedAmount);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    amountEdit.performClick();
                    updateText();
                }
            }, 500);

        }
    }

}
