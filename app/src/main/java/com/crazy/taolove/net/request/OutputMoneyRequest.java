package com.crazy.taolove.net.request;

import android.support.v4.util.ArrayMap;

import com.crazy.taolove.CSApplication;
import com.crazy.taolove.R;
import com.crazy.taolove.entity.OutputMoney;
import com.crazy.taolove.manager.AppManager;
import com.crazy.taolove.net.base.ResultPostExecute;
import com.crazy.taolove.utils.AESOperator;
import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;

/**
 * Created by Administrator on 2016/5/1.
 */
public class OutputMoneyRequest extends ResultPostExecute<String> {
    public void request(OutputMoney outputMoney) {
        ArrayMap<String, String> params = new ArrayMap<>(1);
        Gson gson = new Gson();
        try {
            params.put("outputmoney", AESOperator.getInstance().encrypt(gson.toJson(outputMoney)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Call<ResponseBody> call = AppManager.getUserService().outputMoney(AppManager.getClientUser().sessionId, params);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                if(response.isSuccessful()){
                    try {
                        parseJson(response.body().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        response.body().close();
                    }
                } else {
                    onErrorExecute(CSApplication.getInstance()
                            .getResources()
                            .getString(R.string.network_requests_error));
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                onErrorExecute(CSApplication.getInstance()
                        .getResources()
                        .getString(R.string.network_requests_error));
            }
        });
    }

    private void parseJson(String json) {
        try {
            onPostExecute("");
        } catch (Exception e) {
            onErrorExecute(CSApplication.getInstance().getResources()
                    .getString(R.string.data_parser_error));
        }
    }
}
