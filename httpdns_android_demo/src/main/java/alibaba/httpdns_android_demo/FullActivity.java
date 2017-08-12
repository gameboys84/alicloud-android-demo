package alibaba.httpdns_android_demo;

import android.os.Bundle;
import android.os.Message;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.alibaba.sdk.android.httpdns.DegradationFilter;
import com.alibaba.sdk.android.httpdns.HttpDns;
import com.alibaba.sdk.android.httpdns.HttpDnsService;

import java.io.DataInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import android.os.Handler;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;


public class FullActivity extends AppCompatActivity {
    private static final String TAG = "HTTPDNS";
    private static HttpDnsService httpdns;
    public final static String accountID = "138545"; // "183611" personal; // "138545" comp;// "139450" test;
    public final static String secretKey = "b31d7172e979bd0144904028e4fe9450";

    private HttpURLConnection connHttp;
    private HttpsURLConnection connHttps;

    private EditText text;
    private TextView txtRsp;
    private Button btnHttpdns;

    private RadioButton radioHttpQpgame;
    private RadioButton radioHttpD;
    private RadioButton radioHttpM;
    private RadioButton radioHttpsD;
    private RadioButton radioHttpNotice;
    private RadioButton radioHttpIni;
    private RadioButton radioHttpsUpdate;
    private RadioGroup radioGroup;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full);
        getSupportActionBar().setTitle(R.string.full_scene);

        // 初始化httpdns
        httpdns = HttpDns.getService(getApplicationContext(), accountID);
        // 预解析热点域名

        ArrayList lstPreResolve = new ArrayList<>(Arrays.asList("m.taobao.com", "d.qpgame.com", "www.qpgame.com", "m.qpgame.com", "syver-api-wjr.cy9527.com"));
        final ArrayList lstDegrade = new ArrayList<>(Arrays.asList("m.taobao.com", "www.taobao.com", "d.qpgame.com"));
        httpdns.setPreResolveHosts(lstPreResolve);
        String txt = "Presolve:" + lstPreResolve.toString() + "\n\nDegrade:" + lstDegrade.toString();
        Log.i(TAG, txt);

        httpdns.setDegradationFilter(new DegradationFilter() {
            @Override
            public boolean shouldDegradeHttpDNS(String hostName) {
                // 此处可以自定义降级逻辑，例如www.taobao.com不使用HttpDNS解析
                // 参照HttpDNS API文档，当存在中间HTTP代理时，应选择降级，使用Local DNS
                for (Object host:lstDegrade) {
                //for (int i = 0; i < lstDegrade.size(); ++i) {
                    //String host = (String)lstDegrade.get(0);
                    if (hostName.equals(host.toString())) {
                        Log.i(TAG, String.format("===isDegrade=true, url=%s", hostName));
                        return true;
                    }
                }

                Log.i(TAG, String.format("===isDegrade=false, url=%s", hostName));
                return false;
            }
        });

        text = (EditText)findViewById(R.id.url);
        txtRsp = (TextView)findViewById(R.id.rsp);
        txtRsp.setText(txt);
        txtRsp.setMovementMethod(ScrollingMovementMethod.getInstance());

        radioHttpQpgame = (RadioButton)findViewById(R.id.radio_http_qpgame);
        radioHttpD = (RadioButton)findViewById(R.id.radio_http_d);
        radioHttpM = (RadioButton)findViewById(R.id.radio_http_m);
        radioHttpsD = (RadioButton)findViewById(R.id.radio_https_d);
        radioHttpNotice = (RadioButton)findViewById(R.id.radio_http_notice);
        radioHttpIni = (RadioButton)findViewById(R.id.radio_http_ini);
        radioHttpsUpdate = (RadioButton)findViewById(R.id.radio_https_update);
        radioGroup = (RadioGroup)findViewById(R.id.radio_group);
        radioGroup.setOnCheckedChangeListener(onCheckedChangeListener);

        btnHttpdns = (Button) findViewById(R.id.btn_httpdns);
        btnHttpdns.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();

                txtRsp.setText("");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        FullActivity.this.connectHttps(text.getText().toString());
                    }
                }).start();
            }
        });
    }

    private RadioGroup.OnCheckedChangeListener onCheckedChangeListener =
            new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
            if (checkedId == radioHttpQpgame.getId()) {
                text.setText(radioHttpQpgame.getText());
            } else if (checkedId == radioHttpD.getId()) {
                text.setText(radioHttpD.getText());
            } else if (checkedId == radioHttpM.getId()) {
                text.setText(radioHttpM.getText());
            } else if (checkedId == radioHttpsD.getId()) {
                text.setText(radioHttpsD.getText());
            } else if (checkedId == radioHttpNotice.getId()) {
                text.setText(radioHttpNotice.getText());
            } else if (checkedId == radioHttpIni.getId()) {
                text.setText(radioHttpIni.getText());
            } else if (checkedId == radioHttpsUpdate.getId()) {
                text.setText(radioHttpsUpdate.getText());
            }
        }
    };

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            txtRsp.setText(msg.getData().getString("text"));
        }
    };

    private void connectHttps(String originalUrl) {
        try {
            //String originalUrl = "https://m.taobao.com/?sprefer=sypc00";
            Log.d(TAG, String.format("connectHttps: %s", originalUrl));
            final URL url = new URL(originalUrl);

            //boolean isHttpsUrl = false;

            String rsp = "";
            if (originalUrl.startsWith("https")) {
                //isHttpsUrl = true;

                connHttps = (HttpsURLConnection) url.openConnection();
                // 同步接口获取IP
                String ip = httpdns.getIpByHostAsync(url.getHost());
                if (ip != null) {
                    // 通过HTTPDNS获取IP成功，进行URL替换和HOST头设置
                    Log.d(TAG, "Get IP: " + ip + " for host: " + url.getHost() + " from HTTPDNS successfully!");
                    String newUrl = originalUrl.replaceFirst(url.getHost(), ip);
                    Log.d(TAG, String.format("getIpByHTTPDNSAsync: %s, %s", originalUrl, newUrl));

                    rsp = String.format("Get IP:%s for host:%s\norgURL:%S newURL:%s\n", ip, url.getHost(), originalUrl, newUrl);

                    connHttps = (HttpsURLConnection) new URL(newUrl).openConnection();
                    // 设置HTTP请求头Host域
                    connHttps.setRequestProperty("Host", url.getHost());
                } else {
                    Log.i(TAG, "由于在降级策略中过滤了" + originalUrl + "，无法从HTTPDNS服务中获取对应域名的IP信息\n");
                    rsp += "由于在降级策略中过滤了" + originalUrl + "，无法从HTTPDNS服务中获取对应域名的IP信息\n";
                }

                if (url.getHost().equals("d.qpgame.com")) connHttps.setDoInput(false);

                connHttps.setHostnameVerifier(new HostnameVerifier() {
                    /*
                     * 关于这个接口的说明，官方有文档描述：
                     * This is an extended verification option that implementers can provide.
                     * It is to be used during a handshake if the URL's hostname does not match the
                     * peer's identification hostname.
                     *
                     * 使用HTTPDNS后URL里设置的hostname不是远程的主机名(如:m.taobao.com)，与证书颁发的域不匹配，
                     * Android HttpsURLConnection提供了回调接口让用户来处理这种定制化场景。
                     * 在确认HTTPDNS返回的源站IP与Session携带的IP信息一致后，您可以在回调方法中将待验证域名替换为原来的真实域名进行验证。
                     *
                     */
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        String host = connHttps.getRequestProperty("Host");
                        if (null == host) {
                            host = connHttps.getURL().getHost();
                        }
                        return HttpsURLConnection.getDefaultHostnameVerifier().verify(host, session);
                    }
                });
                //InputStream inputStream = connHttps.getInputStream();
                if (connHttps.getDoInput()) {
                    InputStream inputStream = connHttps.getInputStream();
                    DataInputStream dis = new DataInputStream(inputStream);
                    int len;
                    byte[] buff = new byte[4096];
                    StringBuilder response = new StringBuilder();
                    while ((len = dis.read(buff)) != -1) {
                        response.append(new String(buff, 0, len));
                    }
                    Log.d(TAG, "Response: " + response.toString());

                    rsp += response.toString() + "\n";
                }
                else {
                    rsp += "response null\n";
                    Log.d(TAG, "Response null");
                }
            } else {
                // 发送网络请求
                //String originalUrl = "http://www.aliyun.com";
                connHttp = (HttpURLConnection) url.openConnection();
                // 异步接口获取IP
                String ip = httpdns.getIpByHostAsync(url.getHost());

                if (ip != null) {
                    // 通过HTTPDNS获取IP成功，进行URL替换和HOST头设置
                    Log.d(TAG, "Get IP: " + ip + " for host: " + url.getHost() + " from HTTPDNS successfully!");
                    String newUrl = originalUrl.replaceFirst(url.getHost(), ip);
                    Log.d(TAG, String.format("getIpByHTTPDNSAsync: %s, %s", originalUrl, newUrl));

                    rsp = String.format("Get IP:%s for host:%s\norgURL:%S newURL:%s\n", ip, url.getHost(), originalUrl, newUrl);

                    connHttp = (HttpURLConnection) new URL(newUrl).openConnection();
                    // 设置HTTP请求头Host域
                    connHttp.setRequestProperty("Host", url.getHost());
                } else {
                    Log.i(TAG, "由于在降级策略中过滤了" + originalUrl + "，无法从HTTPDNS服务中获取对应域名的IP信息\n");
                    rsp += "由于在降级策略中过滤了" + originalUrl + "，无法从HTTPDNS服务中获取对应域名的IP信息\n";
                }

                if (url.getHost().equals("d.qpgame.com")) connHttp.setDoInput(false);

                //InputStream inputStream = connHttp.getInputStream();
                if (connHttp.getDoInput()) {
                    InputStream inputStream = connHttp.getInputStream();
                    DataInputStream dis = new DataInputStream(inputStream);
                    int len;
                    byte[] buff = new byte[4096];
                    StringBuilder response = new StringBuilder();
                    while ((len = dis.read(buff)) != -1) {
                        response.append(new String(buff, 0, len));
                    }
                    rsp += response.toString() + "\n";
                    Log.d(TAG, "Response: " + response.toString());
                } else {
                    rsp += "response null\n";
                    Log.d(TAG, "Response null");
                }


                // 允许返回过期的IP
                //httpdns.setExpiredIPEnabled(true);

                // 测试黑名单中的域名
                //ip = httpdns.getIpByHostAsync(url.getHost()); // ("www.taobao.com");
                //if (ip == null) {
                //    Log.d(TAG, "由于在降级策略中过滤了" + originalUrl + "，无法从HTTPDNS服务中获取对应域名的IP信息");
                //}
            }

            sendMessage(rsp);
            //txtRsp.setText(rsp);
        }
        //conn = isHttpsUrl ? (HttpsURLConnection) url.openConnection() : (HttpURLConnection) url.openConnection();
        catch (Exception e) {
            e.printStackTrace();
            sendMessage(e.toString());
            //txtRsp.setText(e.toString());
        } finally {

        }
    }

    private boolean sendMessage(String txt) {
        Message msg = new Message();
        //msg.what =
        Bundle bundle = new Bundle();
        bundle.putString("text", txt);
        msg.setData(bundle);

        return handler.sendMessage(msg);
    }

}
