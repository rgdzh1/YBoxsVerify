package com.yey.vcodevy;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.method.TransformationMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.yey.vcodevy.widget.YPasswordTransformation;

import java.util.ArrayList;

public class YBoxsCustomInput extends LinearLayout {
    private final static String TAG = YBoxsCustomInput.class.getName();
    private int mBoxNum;
    private int mBoxMargin;
    private float mBoxTextSize;
    private int mBoxFocus;
    private int mBoxNotFcous;
    private boolean isPwd;
    private ArrayList<TextView> mTextViewList;
    private int mBoxTextColor;
    private int mInputIndex;//输入索引
    private int mBoxPwdDotSize;// 密文模式下,黑点是大还是小
    private boolean isTextBoldStyle;// 明文字体是否为粗体 true 粗体
    private boolean mInputComplete;
    private int mBoxHeight;
    private int mBoxWidth;
    private TransformationMethod mPwdTransformationMethod;
    //明文属性转为密文属性 handler
    private Handler mRefreshHandler = new Handler(Looper.getMainLooper());
    // 内容
    private StringBuffer mContentBuffer = new StringBuffer();

    private IVCodeBack mInputCallback;


    public YBoxsCustomInput(Context context) {
        this(context, null);
    }

    public YBoxsCustomInput(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public YBoxsCustomInput(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // 设置YBoxsCustomInput 水平居中
        this.setOrientation(HORIZONTAL);
        this.setGravity(Gravity.CENTER);
        initRes(context, attrs, defStyleAttr);
        initData();
    }


    /**
     * 解析属性值
     *
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    private void initRes(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.YBoxsVerify, defStyleAttr, 0);
        mBoxNum = typedArray.getInteger(R.styleable.YBoxsVerify_box_bum, 1);
        mBoxMargin = typedArray.getDimensionPixelSize(R.styleable.YBoxsVerify_box_margin, 6);
        mBoxTextSize = typedArray.getDimensionPixelSize(R.styleable.YBoxsVerify_box_text_size, 16);
        mBoxHeight = typedArray.getDimensionPixelSize(R.styleable.YBoxsVerify_box_height, 40);
        mBoxWidth = typedArray.getDimensionPixelSize(R.styleable.YBoxsVerify_box_width, 40);
        mBoxTextColor = typedArray.getColor(R.styleable.YBoxsVerify_box_text_color, getResources().getColor(R.color.vcvy_balck));
        mBoxFocus = typedArray.getResourceId(R.styleable.YBoxsVerify_box_focus, R.drawable.box_focus);
        mBoxNotFcous = typedArray.getResourceId(R.styleable.YBoxsVerify_box_not_focus, R.drawable.box_notfoucs);
        isPwd = typedArray.getBoolean(R.styleable.YBoxsVerify_box_pwd_model, false);
        mBoxPwdDotSize = typedArray.getInt(R.styleable.YBoxsVerify_box_pwd_dot_size, 0);
        isTextBoldStyle = typedArray.getBoolean(R.styleable.YBoxsVerify_box_text_style, false);
        typedArray.recycle();
    }

    /**
     * 初始化数据
     */
    private void initData() {
        // 设置黑色点大还是小
        setBoxPwdDotSize();
        // 创建TextViews
        creatTextViews();
    }

    /**
     * 创建TextView 添加到List与YBoxsCustomInput中
     */
    private void creatTextViews() {
        mTextViewList = new ArrayList<>();
        mTextViewList.clear();
        for (int i = 0; i < mBoxNum; i++) {
            //TODO 这里Text的大小与边距都没有设置, 在onLayout中去设置
            TextView mTextView = new TextView(getContext());
            mTextView.setTextColor(mBoxTextColor);
            //为Paint画笔设置大小, 不会在有适配问题
            mTextView.getPaint().setTextSize(mBoxTextSize);
            if (isTextBoldStyle) {
                mTextView.getPaint().setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
            }
            mTextView.setGravity(Gravity.CENTER);
            if (i == 0) {
                mTextView.setBackgroundResource(mBoxFocus);
            } else {
                mTextView.setBackgroundResource(mBoxNotFcous);
            }
            mTextViewList.add(mTextView);
            this.addView(mTextView);
        }
    }

    /**
     * 设置黑色圆点大小
     */
    private void setBoxPwdDotSize() {
        if (mBoxPwdDotSize == 0) {
            // 大
            mPwdTransformationMethod = YPasswordTransformation.getInstance();
        } else if (mBoxPwdDotSize == 1) {
            // 小
            mPwdTransformationMethod = PasswordTransformationMethod.getInstance();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        setTextViewMargin();
    }

    /**
     * 设置每个TextView之间的间距
     */
    private void setTextViewMargin() {
        for (int i = 0; i < mBoxNum; i++) {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(mBoxWidth, mBoxHeight);
            if (i != 0) {
                layoutParams.leftMargin = mBoxMargin;
            }
            TextView textView = mTextViewList.get(i);
            textView.setWidth(mBoxWidth);
            textView.setHeight(mBoxHeight);
            textView.setLayoutParams(layoutParams);
        }
    }

    /**
     * 手动输入内容
     *
     * @param mContent
     */
    public void inputContent(String mContent) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, mContent);
        }
        if (!TextUtils.isEmpty(mContent) && mInputIndex >= 0 && mInputIndex < mBoxNum && !mInputComplete) {
            //1.当EditText中有输入时,将该输入取出展示到mInputIndex对应的textview中
            //将当前的textview 置为not focus
            //2.mInputIndex加1之后对应的textview 背景置为focus
            //3.将EditText 数据清除
            final TextView notFouceTextView = mTextViewList.get(mInputIndex);
            notFouceTextView.setText(mContent);
            mContentBuffer.append(mContent);//添加内容
            //输入中回调
            if (mInputCallback != null) {
                mInputCallback.inputing(mContentBuffer.toString(), mInputIndex);
            }
            notFouceTextView.setBackgroundResource(mBoxNotFcous);
            if (isPwdStatus()) {
                mRefreshHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //将textview 属性改为密文属性, 延迟200毫秒从明文变为密文
                        notFouceTextView.setTransformationMethod(mPwdTransformationMethod);
                    }
                }, 200);
            }
            mInputIndex++;
            //当mIndex 值超出了索引最大值,将mInputIndex置为索引最大值,防止索引越界
            if (mInputIndex > mBoxNum - 1) {
                mInputIndex--;
                //当输入完成之后, 再输入的话就不让输入了
                mInputComplete = true;
                //输入完成回调
                if (mInputCallback != null) {
                    mInputCallback.inputComplete(mContentBuffer.toString());
                }
            }
            TextView fouceTextView = mTextViewList.get(mInputIndex);
            fouceTextView.setBackgroundResource(mBoxFocus);
        }
    }

    /**
     * 删除输入内容
     */
    public void deleteContent() {
        if (mInputIndex >= 0 && mInputIndex < mBoxNum) {
            //删除了一个空格, 此时可以再输入内容
            mInputComplete = false;
            TextView mLastText = mTextViewList.get(mBoxNum - 1);
            String mLastString = mLastText.getText().toString().trim();
            if (!TextUtils.isEmpty(mLastString)) {
                //此时输入完成,将最后一个textview内容删除,但是mInputIndex 不要进行减1
                //此时最后一个textview背景还是focus
                mLastText.setText("");
                if (isPwdStatus()) {
                    mLastText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                }
            } else {
                //当最后一个textview没有数据,此时就是没有输入完成删除
                //要进行两步操作 1.将mInputIndex对应的textview背景置为not focus
                //             2.将mInputIndex减1后对应的textview内容删除,并且将该textview 背景置为focus
                TextView fouceTextView = mTextViewList.get(mInputIndex);
                fouceTextView.setBackgroundResource(mBoxNotFcous);
                mInputIndex--;
                //若mInputIndex此时为0, 按删除按钮的时候mInputIndex减1就变成了-1,
                //会造成索引越界,将mInputIndex置为0,可以避免越界.
                if (mInputIndex < 0) {
                    mInputIndex = 0;
                }
                TextView notFouceTextView = mTextViewList.get(mInputIndex);
                notFouceTextView.setText("");
                notFouceTextView.setBackgroundResource(mBoxFocus);
                if (isPwdStatus()) {
                    notFouceTextView.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                }
            }
            if (mContentBuffer.length() != 0) {
                mContentBuffer.deleteCharAt(mInputIndex);//删除内容
                //输入中回调
                if (mInputCallback != null) {
                    mInputCallback.inputing(mContentBuffer.toString(), mInputIndex);
                }
            }
        }
    }

    /**
     * 获取框中的密码
     */
    public String getContent() {
        return mContentBuffer.toString();
    }

    /**
     * 显示或者隐藏输入内容
     */
    public void changeModel() {
        isPwd = !isPwd;
        for (int i = 0; i < mTextViewList.size(); i++) {
            final TextView textView = mTextViewList.get(i);
            if (isPwd) {
                textView.setTransformationMethod(mPwdTransformationMethod);
            } else {
                textView.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            }
        }
    }

    /**
     * @return 是否是密文状态 密文为true,明文为false
     */
    public boolean isPwdStatus() {
        return isPwd;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mRefreshHandler.removeCallbacksAndMessages(null);
    }

    public void setInputCallback(IVCodeBack callback) {
        this.mInputCallback = callback;
    }
}
