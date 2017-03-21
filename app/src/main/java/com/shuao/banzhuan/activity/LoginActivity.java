package com.shuao.banzhuan.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.shuao.banzhuan.R;
import com.shuao.banzhuan.data.Config;
import com.shuao.banzhuan.tools.JSONUtils;
import com.shuao.banzhuan.tools.MatchUtil;
import com.shuao.banzhuan.tools.OKClientManager;
import com.shuao.banzhuan.tools.SPUtils;
import com.shuao.banzhuan.tools.UiTools;
import com.shuao.banzhuan.view.LoadingDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by flyonthemap on 16/8/4.
 * Android 登录类的实现
 */
public class LoginActivity extends BaseActivity implements View.OnClickListener {
    private EditText et_account,et_pass; // 账号密码
    private Button btn_login,btn_register; // 登录注册button
    private Button bt_username_clear;
    private Button bt_pwd_clear;
    private Button bt_pwd_eye;
    private TextWatcher username_watcher;
    private TextWatcher password_watcher;
    private LoadingDialog loadingDialog;
    private String phoneNum ;
    private String password ;
    OKClientManager okClientManager;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case Config.LOGIN_SUCCESS:
                    loadingDialog.dismiss();
                    // 保存用户的登录状态，确保Activity启动时候的初始界面
                    Toast.makeText(UiTools.getContext(),getString(R.string.login_success),Toast.LENGTH_SHORT).show();
                    SPUtils.put(UiTools.getContext(),Config.IS_LOGIN,true);
                    Intent intent = new Intent(LoginActivity.this,MainActivity.class);
                    startActivity(intent);
                    finish();
                    break;
                // 账号或密码错误
                case Config.LOGIN_ERROR:
                    loadingDialog.dismiss();
                    Toast.makeText(UiTools.getContext(),getString(R.string.account_or_password_is_error),Toast.LENGTH_SHORT).show();
                    break;
                // 手机号为注册
                case Config.LOGIN_PHONE_UNREGISTER:
                    Toast.makeText(UiTools.getContext(),getString(R.string.phone_has_no_register),Toast.LENGTH_SHORT).show();
                    loadingDialog.dismiss();
                    break;
                // 因为服务器端登录失败
                case Config.LOAD_FAIL_FINISH:
                    loadingDialog.dismiss();
                    Toast.makeText(UiTools.getContext(), getString(R.string.login_fail),Toast.LENGTH_SHORT).show();
                default:
                    loadingDialog.dismiss();
                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Override
    protected void initView() {
        setContentView(R.layout.activity_login);
        et_account= (EditText) findViewById(R.id.username);
        et_pass = (EditText) findViewById(R.id.password);
        bt_username_clear = (Button)findViewById(R.id.bt_username_clear);
        bt_pwd_clear = (Button)findViewById(R.id.bt_pwd_clear);
        bt_pwd_eye = (Button)findViewById(R.id.bt_pwd_eye);
        bt_username_clear.setOnClickListener(this);
        bt_pwd_clear.setOnClickListener(this);
        bt_pwd_eye.setOnClickListener(this);
        initWatcher();
        et_account.addTextChangedListener(username_watcher);
        et_pass.addTextChangedListener(password_watcher);

        btn_login = (Button) findViewById(R.id.login);
        btn_register = (Button) findViewById(R.id.register);

        btn_login.setOnClickListener(this);
        btn_register.setOnClickListener(this);

        // 加载对话框的初始化
        loadingDialog = new LoadingDialog(this);

    }

    @Override
    protected void init() {
        okClientManager = OKClientManager.getOkManager();
    }

    //    初始化用户一键清除功能
    private void initWatcher() {
        username_watcher = new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
            public void afterTextChanged(Editable s) {
                if(s.toString().length() > 0){
                    bt_username_clear.setVisibility(View.VISIBLE);
                }else{
                    bt_username_clear.setVisibility(View.INVISIBLE);
                }
            }
        };

        password_watcher = new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
            public void afterTextChanged(Editable s) {
                // 当有字符输入时清除按钮可见
                if(s.toString().length()>0){
                    bt_pwd_clear.setVisibility(View.VISIBLE);
                }else{
                    bt_pwd_clear.setVisibility(View.INVISIBLE);
                }
            }
        };
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.login:
                //向服务器提交登录数据
                login();
                break;
            //一键清除之前的数据
            case R.id.bt_username_clear:
                et_account.setText("");
                break;
            case R.id.bt_pwd_clear:
                et_pass.setText("");
                break;
            case R.id.bt_pwd_eye:
                // 密码是否可见的设置
                if(et_pass.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)){
                    bt_pwd_eye.setBackgroundResource(R.drawable.eye_close);
                    et_pass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
                }else{
                    bt_pwd_eye.setBackgroundResource(R.drawable.eye_open);
                    et_pass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
                et_pass.setSelection(et_pass.getText().toString().length());
                break;
            case R.id.register:
                Intent launchRegisterActivity  = new Intent(LoginActivity.this,RegisterActivity.class);
                startActivity(launchRegisterActivity);
                finish();
                break;
        }
    }

    //  此方法中进行登录逻辑的处理
    private void login() {
        phoneNum = et_account.getText().toString();
        password = et_pass.getText().toString();
        if(!MatchUtil.isPhoneNum(phoneNum)){
            Toast.makeText(LoginActivity.this, R.string.input_true_phoneNum,Toast.LENGTH_SHORT).show();
        }else if(et_pass.getText().toString().equals("")){
            Toast.makeText(LoginActivity.this, R.string.input_password,Toast.LENGTH_SHORT).show();
        }else{
            // 显示对话框
            loadingDialog.show();

            Map<String,String> params = new HashMap<>();
            params.put(Config.PHONE,phoneNum);
            params.put(Config.PASSWORD,password);

            if(okClientManager != null)
                okClientManager.requestGetBySyn(Config.LOGIN_URI, params, new OKClientManager.LoadJsonString() {
                    @Override
                    public void onResponse(String res) {
                        if(res !=  null){
                            JSONObject result = JSONUtils.instanceJsonObject(res);
                            try {
                                switch (result.getInt("code")){
                                    case 0:
                                        handler.sendEmptyMessage(Config.LOGIN_SUCCESS);
                                        break;
                                    case 1:
                                        handler.sendEmptyMessage(Config.LOGIN_ERROR);
                                        break;
                                    case 2:
                                        handler.sendEmptyMessage(Config.LOGIN_PHONE_UNREGISTER);
                                        break;
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }else {
                            handler.sendEmptyMessageDelayed(Config.LOAD_FAIL_FINISH, 3000);
                        }
                    }
                });

        }
    }


}
